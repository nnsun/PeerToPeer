package com.example.peertopeer;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BluetoothBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d("p2p_log", "In Broadcast receiver with action: " + action);
        if (BluetoothDevice.ACTION_FOUND.equals(action)) {

            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            String uuid = intent.getParcelableExtra(BluetoothDevice.EXTRA_UUID);

            if (uuid.equals(BluetoothOperations.MY_UUID)) {
                String deviceName = device.getName();
                Log.d("p2p_log", "Available to connect with " + deviceName);

                PeersListFragment.mBluetoothDevices.add(device);
            }
        }

    }
}
