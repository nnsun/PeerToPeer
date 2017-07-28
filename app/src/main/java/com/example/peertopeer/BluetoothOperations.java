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

    public void startServer(BluetoothAdapter bluetoothAdapter) {
        AcceptThread server = new AcceptThread(bluetoothAdapter);
        server.start();
    }

    public void startClient(BluetoothDevice device, BluetoothAdapter bluetoothAdapter) {
        ConnectThread client = new ConnectThread(device, bluetoothAdapter);
        client.start();
    }

    // Probably should be elsewhere, but as for now just keeping it here
    // Listens for connections (server) and waits until found one
    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread(BluetoothAdapter bluetoothAdapter) {
            // Use a temporary object that is later assigned to mmServerSocket
            // because mmServerSocket is final.
            BluetoothServerSocket tmp = null;
            try {
                // MY_UUID is the app's UUID string, also used by the client code.
                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
            }
            catch (IOException e) {
                Log.e("p2p_log", "Socket's listen() method failed", e);
            }
            mmServerSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket = null;
            // Keep listening until exception occurs or a socket is returned.
            while (true) {
                try {
                    socket = mmServerSocket.accept();
                }
                catch (IOException e) {
                    Log.e("p2p_log", "Socket's accept() method failed", e);
                    break;
                }

                if (socket != null) {
                    // A connection was accepted. Perform work associated with
                    // the connection in a separate thread.
                    //manageMyConnectedSocket(socket);
                    try {
                        mmServerSocket.close();
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }

        // Closes the connect socket and causes the thread to finish.
        public void cancel() {
            try {
                mmServerSocket.close();
            }
            catch (IOException e) {
                Log.e("p2p_log", "Could not close the connect socket", e);
            }
        }
    }

    // Probably should be elsewhere as well, but gonna keep it here for now
    // Tries to make an outgoing connection (client)
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private final BluetoothAdapter mmBluetoothAdapter;

        public ConnectThread(BluetoothDevice device, BluetoothAdapter bluetoothAdapter) {
            // Use a temporary object that is later assigned to mmSocket
            // because mmSocket is final.
            BluetoothSocket tmp = null;
            mmDevice = device;
            mmBluetoothAdapter = bluetoothAdapter;

            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                // MY_UUID is the app's UUID string, also used in the server code.
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            }
            catch (IOException e) {
                Log.e("p2p_log", "Socket's create() method failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            mmBluetoothAdapter.cancelDiscovery();

            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket.connect();
            }
            catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                Log.d("p2p_log", "!!!Could not connect to the server!!!");
                try {
                    mmSocket.close();
                }
                catch (IOException closeException) {
                    Log.e("p2p_log", "Could not close the client socket", closeException);
                }
                return;
            }

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            // manageMyConnectedSocket(mmSocket);
        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
            }
            catch (IOException e) {
                Log.e("p2p_log", "Could not close the client socket", e);
            }
        }
    }

}