package me.jcano.android.simplebluetooth.service;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.ArrayAdapter;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Created by justincano on 6/6/17.
 */

interface BluetoothService {
    boolean discover();
    boolean cancelDiscovery();
    Set<BluetoothDevice> getPairedDevices();
    boolean pairDevice(BluetoothDevice device);
    // TODO
    // void connect(BluetoothDevice device);
}

public class SimpleBluetoothService implements BluetoothService {
    private final String TAG = "SimpleBluetoothService";
    private final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private final BluetoothAdapter mBluetoothAdapter;
    private ArrayAdapter<String> mDevicesAdapter;

    // Create a BroadcastReceiver for ACTION_FOUND.
    public final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        private final String TAG = "BluetoothPair.mReceiver";

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.i(TAG, action);
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                Log.i(TAG, String.format("Discovered device %s with address %s", deviceName, deviceHardwareAddress));
                if (deviceName == null)
                    mDevicesAdapter.add(String.format("%s %s", deviceHardwareAddress, "unknown"));
                else
                    mDevicesAdapter.add(String.format("%s %s", deviceHardwareAddress, deviceName));
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Log.i(TAG, "Bluetooth discovery finished");
            }
        }
    };

    public SimpleBluetoothService(
            BluetoothAdapter adapter,
            ArrayAdapter<String> devicesAdapter) {
        mBluetoothAdapter = adapter;
        mDevicesAdapter = devicesAdapter;
    }

    /**
     * @return true if discovery initialization is successful, false otherwise
     */
    public boolean discover() {
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }
        return mBluetoothAdapter.startDiscovery();
    }

    public boolean cancelDiscovery() {
        return mBluetoothAdapter.cancelDiscovery();
    }

    public boolean pairDevice(BluetoothDevice device) {
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }
        return device.createBond();
    }

    public Set<BluetoothDevice> getPairedDevices() {
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }
        return mBluetoothAdapter.getBondedDevices();
    }

    public Set<String> getNamesOfPairedDevices() {
        Log.i(TAG, "Finding names of paired devices...");
        Set<String> names = new HashSet<>();
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }
        for (BluetoothDevice device : mBluetoothAdapter.getBondedDevices()) {
            if (device.getName() == null)
                names.add("unknown");
            else
                names.add(device.getName());
        }
        return names;
    }

}