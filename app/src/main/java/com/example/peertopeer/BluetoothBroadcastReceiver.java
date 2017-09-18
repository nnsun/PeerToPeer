package com.example.peertopeer;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

public class BluetoothBroadcastReceiver extends BroadcastReceiver {

    private static final String NAME = "BluetoothPeerToPeer";
    public static final UUID MY_UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");


    BluetoothAdapter mAdapter;
    BluetoothDevice mDevice;
    BluetoothServerSocket mServerSocket;



    public BluetoothBroadcastReceiver(BluetoothAdapter adapter) {
        super();

        mAdapter = adapter;

        try {
            mServerSocket = mAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        if (mServerSocket != null) {

            AsyncTask bluetoothServer = new AsyncTask() {

                @Override
                protected Object doInBackground(Object[] objects) {
                    BluetoothSocket clientSocket;
                    while (true) {
                        try {
                            clientSocket = mServerSocket.accept();

                            if (clientSocket == null) {
                                continue;
                            }

                            Log.d("p2p_log", "Connected to client");

                            FileOperations.bluetoothSendData(clientSocket, mAdapter.getAddress());
                            FileOperations.bluetoothGetData(clientSocket, mAdapter.getAddress());
                        }
                        catch (IOException e) {
                            Log.e("p2p_log", "Socket's accept() method failed", e);
                        }
                    }
                }
            };

            bluetoothServer.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }

    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d("p2p_log", "In Broadcast receiver with action: " + action);
//        if (BluetoothDevice.ACTION_FOUND.equals(action)) {
//
//            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
//
//            String deviceName = device.getName();
//            Log.d("p2p_log", "Available to connect with " + deviceName);
//
//            PeersListFragment.mBluetoothDevices.add(device);
//        }

    }
}
