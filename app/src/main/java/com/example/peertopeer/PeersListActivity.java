package com.example.peertopeer;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.View;

import java.util.Set;


public class PeersListActivity extends SingleFragmentActivity {

    WifiP2pManager mManager;
    Channel mChannel;
    WiFiDirectBroadcastReceiver mWifiDirectReceiver;
    IntentFilter mWifiDirectIntentFilter;

    BluetoothAdapter mBluetoothAdapter;
    BluetoothBroadcastReceiver mBluetoothReceiver;
    IntentFilter mBluetoothIntentFilter;
    private final static int REQUEST_ENABLE_BT = 1;

    PeersListFragment mFragment;

    @Override
    public Fragment createFragment() {
        mManager = (WifiP2pManager)getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);
        mWifiDirectReceiver = new WiFiDirectBroadcastReceiver(mManager, mChannel, this);

        mWifiDirectIntentFilter = new IntentFilter();
        mWifiDirectIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mWifiDirectIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mWifiDirectIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mWifiDirectIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        mBluetoothIntentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Log.d("p2p_log", "Device does not support Bluetooth");
        }
        if (!mBluetoothAdapter.isEnabled()) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            startActivityForResult(discoverableIntent, REQUEST_ENABLE_BT);
        }
        else {
            Log.d("p2p_log", "Bluetooth enabled");
        }

        // Get a set of currently paired devices
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

        // If there are paired devices, add each one to the ArrayAdapter
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                Log.d("p2p_log", "Paired Device: " + deviceName + " MAC address: " + deviceHardwareAddress);
            }
        } else {
            Log.d("p2p_log", "No paired devices found.");
        }

        mFragment = new PeersListFragment();

        mFragment.setWifiDirectArgs(mManager, mChannel, mWifiDirectReceiver, mWifiDirectIntentFilter);
        mFragment.setBluetoothArgs(mBluetoothReceiver, mBluetoothIntentFilter);

        makeDiscoverable();

        return mFragment;
    }

    private void makeDiscoverable() {
        if (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    // Most likely needs to be in PeersListFragment as an onclick event
    public void discoverPeers() {
        // If we're already discovering, stop it
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }

        // Request discover from BluetoothAdapter
        mBluetoothAdapter.startDiscovery();
    }


}
