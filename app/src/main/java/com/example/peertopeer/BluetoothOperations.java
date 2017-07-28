package com.example.peertopeer;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

public class BluetoothOperations {

    private static final int MESSAGE_READ = 0;
    private static final int MESSAGE_WRITE = 1;
    private static final int MESSAGE_TOAST = 2;

    // Name for the SDP record when creating server socket
    private static final String NAME = "BluetoothPeerToPeer";

    // Unique UUID for this application
    public static final UUID MY_UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");

    public static void findPairedDevices(BluetoothAdapter bluetoothAdapter) {
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        if (pairedDevices.size() > 0) {
            // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                // Check if these devices are in the global array
                // If not, put them under bluetooth availability
            }
        }
    }

    public static void enableDiscoverability(Context context) {
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0);
        context.startActivity(discoverableIntent);
    }

    public static void discover(BluetoothAdapter bluetoothAdapter) {
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        boolean success = bluetoothAdapter.startDiscovery();
        if (success)
            Log.d("p2p_log", "Discovery Started :)");
        else
            Log.d("p2p_log", "Discovery Failed :(");
    }

}