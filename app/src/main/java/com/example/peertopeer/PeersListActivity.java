package com.example.peertopeer;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.support.v4.app.Fragment;
import android.util.Log;


public class PeersListActivity extends SingleFragmentActivity {

    WifiP2pManager mManager;
    Channel mChannel;
    ServiceBase mWifiDirectReceiver;

    BluetoothAdapter mBluetoothAdapter;
    private final static int REQUEST_ENABLE_BT = 1;

    PeersListFragment mFragment;

    @Override
    public Fragment createFragment() {
        mManager = (WifiP2pManager)getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);
        mWifiDirectReceiver = new ServiceBase(mManager, mChannel, this);

        mFragment = new PeersListFragment();


        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter != null) {
            if (!mBluetoothAdapter.isEnabled()) {
                Log.d("p2p_log", "Bluetooth is not enabled");
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }

        }
        else {
            Log.e("p2p_log", "Device does not support Bluetooth");
        }

        mFragment.setBluetoothArgs(mBluetoothAdapter);
        mFragment.setWifiDirectArgs(mManager, mChannel, mWifiDirectReceiver);
        return mFragment;
    }

}
