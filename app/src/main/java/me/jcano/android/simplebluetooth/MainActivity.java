package me.jcano.android.simplebluetooth;

import me.jcano.android.simplebluetooth.service.SimpleBluetoothService;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {
    private final static int REQUEST_ENABLE_BT = 1;
    private final int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 2;
    private final static String TAG = "SimpleBluetoothActivity";
    private final BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private ListView mListView;
    private ArrayList<BluetoothDevice> mDevices = new ArrayList<>();
    private ArrayAdapter<BluetoothDevice> mDevicesArrayAdapter;
    private SimpleBluetoothService bluetoothService;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_paired_devices:
                    Log.i("NavigationItemListener", "paired_devices");
                    bluetoothService.cancelDiscovery();
                    mDevicesArrayAdapter.clear();
                    mDevicesArrayAdapter.addAll(bluetoothService.getPairedDevices());
                    Log.i(TAG, String.format("pairedDevices: %s", mDevices));
                    return true;
                case R.id.navigation_discover:
                    bluetoothService.cancelDiscovery();
                    mDevicesArrayAdapter.clear();
                    bluetoothService.discover();
                    Log.i("NavigationItemListener", "discover");
                    return true;
            }
            return false;
        }

    };

    private class SimpleBluetoothArrayAdapter extends ArrayAdapter<BluetoothDevice> {
        public SimpleBluetoothArrayAdapter(Context context, ArrayList<BluetoothDevice> devices) {
            super(context, 0, devices);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // Get the data item for this position
            BluetoothDevice device = getItem(position);
            // Check if an existing view is being reused, otherwise inflate the view
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.bluetooth_device, parent, false);
            }
            // Lookup view for data population
            TextView nameTextView = (TextView) convertView.findViewById(R.id.device_name);
            TextView addressTextView = (TextView) convertView.findViewById(R.id.device_address);
            // Populate the data into the template view using the data object
            final String deviceName;
            final String deviceAddress= device.getAddress();
            if (device.getName() == null)
                deviceName = "(no friendly name for this device)";
            else
                deviceName = device.getName();
            nameTextView.setText(deviceName);
            addressTextView.setText(deviceAddress);

            // Show menu when tapped
            ListView blueoothListView = mListView;
            blueoothListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    PopupMenu popup = new PopupMenu(view.getContext(), view);
                    popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            switch (item.getItemId()) {
                                case R.id.menu_connect:
                                    Log.i(TAG, "\"Connect\" menu button selected");
                                    return true;
                                case R.id.menu_forget:
                                    Log.i(TAG, "\"Forget\" menu button selected");
                                    return true;
                                default:
                                    return false;
                            }
                        }
                    });
                    popup.inflate(R.menu.bluetooth_paired_device_menu);
                    popup.show();
                }
            });

            // Return the completed view to render on screen
            return convertView;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mListView = (ListView) findViewById(R.id.list_view);
        // Create adapter for ListView
        mDevicesArrayAdapter = new SimpleBluetoothArrayAdapter(this, mDevices);
        mListView.setAdapter(mDevicesArrayAdapter);

        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

         bluetoothService = new SimpleBluetoothService(mBluetoothAdapter, mDevicesArrayAdapter);

        // Set up Bluetooth
        if (mBluetoothAdapter == null) {
            // TODO
            // Device does not support Bluetooth
            Log.e(TAG, "Device does not support Bluetooth.");
        }
        if (!mBluetoothAdapter.isEnabled()) {
            Log.i(TAG, "Bluetooth adapter not enabled");
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);

        // Register for broadcasts when a device is discovered.
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
        registerReceiver(bluetoothService.getReceiver(), filter);

        // Initially show paired devices
        mDevicesArrayAdapter.clear();
        mDevicesArrayAdapter.addAll(bluetoothService.getPairedDevices());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mBluetoothAdapter.cancelDiscovery();
//        mBluetoothAdapter.closeProfileProxy(BluetoothProfile.HEADSET, mBluetoothHeadset);

        // Don't forget to unregister the ACTION_FOUND receiver.
        unregisterReceiver(bluetoothService.getReceiver());
    }


    private void addPairedDevice(BluetoothDevice device) {
        Log.i(TAG, String.format("Adding device %s to pairedDevices", device.getAddress()));
        mDevicesArrayAdapter.add(device);
    }
}
