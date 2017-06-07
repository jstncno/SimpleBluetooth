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
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Adapter;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {
    private final static int REQUEST_ENABLE_BT = 1;
    private final int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 2;
    private final static String TAG = "SimpleBluetoothActivity";
    private final BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private ListView mListView;
    private ArrayList<String> devices = new ArrayList<>();
    private ArrayAdapter<String> devicesAdapter;

    private SimpleBluetoothService bluetoothService;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_paired_devices:
                    Log.i("NavigationItemListener", "paired_devices");
                    bluetoothService.cancelDiscovery();
                    devicesAdapter.clear();
                    devicesAdapter.addAll(bluetoothService.getNamesOfPairedDevices());
                    Log.i(TAG, String.format("pairedDevices: %s", devices));
                    return true;
                case R.id.navigation_discover:
                    bluetoothService.cancelDiscovery();
                    devicesAdapter.clear();
                    bluetoothService.discover();
                    Log.i("NavigationItemListener", "discover");
                    return true;
            }
            return false;
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mListView = (ListView) findViewById(R.id.list);
        // Create adapter for ListView
        devicesAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, devices);
        mListView.setAdapter(devicesAdapter);

        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

         bluetoothService = new SimpleBluetoothService(
                mBluetoothAdapter,
                devicesAdapter
        );

        // Set up Bluetooth
        if (mBluetoothAdapter == null) {
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

        // Show paired devices initially
        devicesAdapter.clear();
        devicesAdapter.addAll(bluetoothService.getNamesOfPairedDevices());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mBluetoothAdapter.cancelDiscovery();
//        mBluetoothAdapter.closeProfileProxy(BluetoothProfile.HEADSET, mBluetoothHeadset);

        // Don't forget to unregister the ACTION_FOUND receiver.
        unregisterReceiver(bluetoothService.getReceiver());
    }


    private void addPairedDevice(String deviceName) {
        Log.i(TAG, String.format("Adding %s to pairedDevices", deviceName));
        devicesAdapter.add(deviceName);
    }

}
