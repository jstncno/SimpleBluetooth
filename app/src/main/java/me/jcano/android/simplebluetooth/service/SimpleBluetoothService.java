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

import me.jcano.android.simplebluetooth.service.threads.SimpleBluetoothConnectionThread;

/**
 * Created by justincano on 6/6/17.
 */

interface BluetoothService {
    boolean discover();
    boolean cancelDiscovery();
    Set<BluetoothDevice> getPairedDevices();
    boolean pairDevice(BluetoothDevice device);
    void connect(BluetoothDevice device);
}

public class SimpleBluetoothService implements BluetoothService {
    private final String TAG = "SimpleBluetoothService";
    private final UUID SPP_UUID = UUID.fromString("00001108-0000-1000-8000-00805F9B34FB");

    private final BluetoothAdapter mBluetoothAdapter;
    private ArrayAdapter<BluetoothDevice> mDevicesArrayAdapter;
    private SimpleBluetoothConnectionThread mBluetoothConnectThread;

    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        private final String TAG = "SBS/BroadcastReceiver";

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.i(TAG, action);
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                mDevicesArrayAdapter.add(device);
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                Log.i(TAG, String.format("Discovered device %s with address %s", deviceName, deviceHardwareAddress));
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Log.i(TAG, "Bluetooth discovery finished");
            }
        }
    };

    public SimpleBluetoothService(
            BluetoothAdapter adapter,
            ArrayAdapter<BluetoothDevice> devicesArrayAdapter) {
        mBluetoothAdapter = adapter;
        mDevicesArrayAdapter = devicesArrayAdapter;
    }

    /**
     * @return true if discovery initialization is successful, false otherwise
     */
    public boolean discover() {
        if (mBluetoothAdapter.isDiscovering())
            mBluetoothAdapter.cancelDiscovery();
        return mBluetoothAdapter.startDiscovery();
    }

    /**
     * @return true if discovery cancellation is successful, false otherwise
     */
    public boolean cancelDiscovery() {
        return mBluetoothAdapter.cancelDiscovery();
    }

    /**
     * @return true if device pairing is successful
     */
    public boolean pairDevice(BluetoothDevice device) {
        if (mBluetoothAdapter.isDiscovering())
            mBluetoothAdapter.cancelDiscovery();
        return device.createBond();
    }

    /**
     * @return A set of paired devices
     */
    public Set<BluetoothDevice> getPairedDevices() {
        if (mBluetoothAdapter.isDiscovering())
            mBluetoothAdapter.cancelDiscovery();
        return mBluetoothAdapter.getBondedDevices();
    }

    public void connect(BluetoothDevice device) {
        if (mBluetoothAdapter.isDiscovering())
            mBluetoothAdapter.cancelDiscovery();
        if (mBluetoothConnectThread != null)
            mBluetoothConnectThread.cancel();
        mBluetoothConnectThread = new SimpleBluetoothConnectionThread(
                mBluetoothAdapter,
                device,
                SPP_UUID
        );
        Log.i(TAG, device.getName());
        Log.i(TAG, device.getAddress());
        mBluetoothConnectThread.run();
    }

    public BroadcastReceiver getReceiver() { return mReceiver; }

    public void stopAllServices() {
        mBluetoothAdapter.cancelDiscovery();
        mBluetoothConnectThread.cancel();
    }

    public boolean hasOpenConnection() { return mBluetoothConnectThread.isRunning(); }

    public void closeConnection() { if (mBluetoothConnectThread != null) mBluetoothConnectThread.cancel(); }

}
