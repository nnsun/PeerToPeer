package com.example.peertopeer;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

public class BluetoothListener {
    private static final String NAME = "BluetoothPeerToPeer";
    public static final UUID MY_UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");

    BluetoothAdapter mAdapter;
    BluetoothServerSocket mServerSocket;

    AsyncTask mBluetoothServer;

    public BluetoothListener(BluetoothAdapter adapter) {
        super();

        mAdapter = adapter;

        try {
            mServerSocket = adapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        if (mServerSocket != null) {

            mBluetoothServer = new AsyncTask() {

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
        }
    }


}
