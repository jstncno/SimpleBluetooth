package me.jcano.android.simplebluetooth;

import me.jcano.android.simplebluetooth.service.SimpleBluetoothService;
import me.jcano.android.simplebluetooth.service.threads.SimpleBluetoothConnectionThread;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
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
    private BluetoothHeadset mBluetoothHeadset;
    private SimpleBluetoothService bluetoothService;

    public enum Page { PAIRED_DEVICES, DISCOVERED_DEVICES }
    private Page currentPage;

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
                    currentPage = Page.PAIRED_DEVICES;
                    return true;
                case R.id.navigation_discover:
                    bluetoothService.cancelDiscovery();
                    mDevicesArrayAdapter.clear();
                    bluetoothService.discover();
                    Log.i("NavigationItemListener", "discover");
                    currentPage = Page.DISCOVERED_DEVICES;
                    return true;
            }
            return false;
        }

    };

    private BluetoothProfile.ServiceListener mProfileListener = new BluetoothProfile.ServiceListener() {
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            if (profile == BluetoothProfile.HEADSET) {
                mBluetoothHeadset = (BluetoothHeadset) proxy;
                Log.i("mProfileListener", "mBluetoothHeadset set!");
            }
        }
        public void onServiceDisconnected(int profile) {
            if (profile == BluetoothProfile.HEADSET) {
                mBluetoothHeadset = null;
            }
        }
    };

    private class SimpleBluetoothArrayAdapter extends ArrayAdapter<BluetoothDevice> {
        public SimpleBluetoothArrayAdapter(Context context, ArrayList<BluetoothDevice> devices) {
            super(context, 0, devices);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // Get the data item for this position
            final BluetoothDevice device = getItem(position);
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
        addListenersToListView();
        mListView.setAdapter(mDevicesArrayAdapter);

        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

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
        bluetoothService = new SimpleBluetoothService(mBluetoothAdapter, mDevicesArrayAdapter);

        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);
        mBluetoothAdapter.getProfileProxy(this, mProfileListener, BluetoothProfile.HEADSET);

        // Register for broadcasts when a device is discovered.
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
        registerReceiver(bluetoothService.getReceiver(), filter);

        // Initially show paired devices
        currentPage = Page.PAIRED_DEVICES;
        mDevicesArrayAdapter.clear();
        mDevicesArrayAdapter.addAll(bluetoothService.getPairedDevices());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mBluetoothAdapter.cancelDiscovery();
        mBluetoothAdapter.closeProfileProxy(BluetoothProfile.HEADSET, mBluetoothHeadset);
        bluetoothService.stopAllServices();

        // Don't forget to unregister the ACTION_FOUND receiver.
        unregisterReceiver(bluetoothService.getReceiver());
    }

    private void addListenersToListView() {
        // Show menu when tapped
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                PopupMenu popup = new PopupMenu(view.getContext(), view);
                final BluetoothDevice selectedDevice = mDevicesArrayAdapter.getItem(position);
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    String deviceName = selectedDevice.getName();
                    String deviceAddress = selectedDevice.getAddress();

                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.menu_connect:
                                Log.i(TAG, "\"Connect\" menu button selected");
                                mBluetoothAdapter.cancelDiscovery();
                                Log.i(TAG, String.format("Connecting to device %s %s", deviceAddress, deviceName));
                                bluetoothService.connect(selectedDevice);
                                return true;
                            case R.id.menu_forget:
                                Log.i(TAG, "\"Forget\" menu button selected");
                                // TODO
                                return true;
                            case R.id.menu_disconnect:
                                Log.i(TAG, "\"Disconnect\" menu button selected");
                                bluetoothService.disconnect();
                                return true;
                            case R.id.menu_pair:
                                Log.i(TAG, "\"Pair\" menu button selected");
                                mBluetoothAdapter.cancelDiscovery();
                                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(deviceAddress);
                                Log.i(TAG, String.format("Pairing with device %s %s", deviceAddress, deviceName));
                                bluetoothService.pairDevice(device);
                                return true;
                            default:
                                return false;
                        }
                    }
                });
                switch (currentPage) {
                    case DISCOVERED_DEVICES:
                        popup.inflate(R.menu.bluetooth_discovered_device_menu);
                        break;
                    case PAIRED_DEVICES:
                        popup.inflate(R.menu.bluetooth_paired_device_menu);
                        break;
                    default:
                        popup.inflate(R.menu.bluetooth_paired_device_menu);
                }
                setMenuButtons(popup.getMenu(), selectedDevice);
                popup.show();
            }
        });
    }

    private boolean setMenuButtons(Menu menu, BluetoothDevice device) {
        MenuItem connect = menu.findItem(R.id.menu_connect);
        MenuItem disconnect = menu.findItem(R.id.menu_disconnect);

        if (connect != null && disconnect != null) {
            BluetoothDevice connectedDevice = bluetoothService.getConnectedDevice();
            if (connectedDevice != null && device.getAddress() == connectedDevice.getAddress()) {
                Log.i(TAG, "Connect button disabled");
                disconnect.setEnabled(true);
                connect.setEnabled(false);
            } else {
                Log.i(TAG, "Disconnect button disabled");
                connect.setEnabled(true);
                disconnect.setEnabled(false);
            }
        }
        return true;
    }
}
