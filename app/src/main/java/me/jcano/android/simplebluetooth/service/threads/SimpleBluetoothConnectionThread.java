package me.jcano.android.simplebluetooth.service.threads;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.nfc.Tag;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by justincano on 6/6/17.
 */

public class SimpleBluetoothConnectionThread extends Thread {
    private final BluetoothAdapter mBluetoothAdapter;
    private final BluetoothSocket mmSocket;
    private final BluetoothDevice mmDevice;
    private final UUID mUUID;
    private boolean _isRunning = false;

    private final String TAG = "SB/ConnectionThread";

    public SimpleBluetoothConnectionThread(
            BluetoothAdapter adapter,
            BluetoothDevice device,
            UUID uuid) {
        // Use a temporary object that is later assigned to mmSocket
        // because mmSocket is final.
        BluetoothSocket tmp = null;
        mBluetoothAdapter = adapter;
        mmDevice = device;
        mUUID = uuid;

        try {
            // Get a BluetoothSocket to connect with the given BluetoothDevice.
            // MY_UUID is the app's UUID string, also used in the server code.
            tmp = device.createRfcommSocketToServiceRecord(mUUID);
            Log.i(TAG, "mmSocket created!");
        } catch (IOException e) {
            Log.e(TAG, "Socket's create() method failed", e);
        }
        mmSocket = tmp;
    }

    public void run() {
        // Cancel discovery because it otherwise slows down the connection.
        mBluetoothAdapter.cancelDiscovery();

        Log.i(TAG, "Running...");
        _isRunning = true;
        Log.i(TAG, String.format("Attempting to connect to device %s %s", mmDevice.getName(), mmDevice.getAddress()));
        try {
            // Connect to the remote device through the socket. This call blocks
            // until it succeeds or throws an exception.
            mmSocket.connect();
        } catch (IOException connectException) {
            // Unable to connect; close the socket and return.
            Log.e(TAG, "Unable to connect; closing the socket and returning.", connectException);
            try {
                mmSocket.close();
            } catch (IOException closeException) {
                Log.e(TAG, "Could not close the client socket", closeException);
            }
            _isRunning = false;
            return;
        }
    }

    // Closes the client socket and causes the thread to finish.
    public void cancel() {
        _isRunning = false;
        try {
            mmSocket.close();
            Log.i(TAG, "mmSocket closed");
        } catch (IOException e) {
            Log.e(TAG, "Could not close the client socket", e);
        }
    }

    public boolean isRunning() {
        return mmSocket.isConnected();
    }
}