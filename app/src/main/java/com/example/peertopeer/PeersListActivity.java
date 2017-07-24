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

        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Log.d("p2p_log", "Device does not support Bluetooth");
        }

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        mFragment = new PeersListFragment();
        mFragment.setWifiDirectArgs(mManager, mChannel, mWifiDirectReceiver, mWifiDirectIntentFilter);
//        mFragment.setBluetoothArgs(mBluetoothAdapter, new BluetoothBroadcastReceiver(), mBluetoothIntentFilter);
        return mFragment;
    }

}
