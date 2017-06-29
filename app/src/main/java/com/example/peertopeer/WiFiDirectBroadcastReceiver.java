package com.example.peertopeer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.net.wifi.p2p.WifiP2pDevice;
import android.util.Log;


public class WiFiDirectBroadcastReceiver extends BroadcastReceiver {

    private WifiP2pManager mManager;
    private Channel mChannel;
    private MainActivity mActivity;

    public WiFiDirectBroadcastReceiver(WifiP2pManager manager, Channel channel, MainActivity activity) {
        super();
        mManager = manager;
        mChannel = channel;
        mActivity = activity;

        mManager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d("DiscoverPeers", "Find peers successful");
            }

            @Override
            public void onFailure(int reasonCode) {
                Log.d("DiscoverPeers", "Find peers failed. Reason: " + reasonCode);
            }
        });
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);

            PeerListListener myPeerListListener = new PeerListListener() {
                @Override
                public void onPeersAvailable(WifiP2pDeviceList peers) {
                    Log.d("DiscoverPeers", "Number of peers found: " + Integer.toString(peers.getDeviceList().size()));

                    for (WifiP2pDevice peer : peers.getDeviceList()) {
                        Log.d("DiscoverPeers", peer.toString());
                    }

                    if (peers.getDeviceList().size() == 1) {
                        Log.d("DiscoverPeers", "Attempting to connect");

                        WifiP2pDevice device = (WifiP2pDevice)peers.getDeviceList().toArray()[0];
                        WifiP2pConfig config = new WifiP2pConfig();
                        config.deviceAddress = device.deviceAddress;
                        mManager.connect(mChannel, config, new ActionListener() {

                            @Override
                            public void onSuccess() {
                                Log.d("DiscoverPeers", "Connected to device!");
                            }

                            @Override
                            public void onFailure(int reason) {
                                Log.d("DiscoverPeers", "Failed to connect. Reason: " + reason);

                            }
                        });
                    }
                }
            };

            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                // request available peers from the wifi p2p manager. This is an
                // asynchronous call and the calling activity is notified with a
                // callback on PeerListListener.onPeersAvailable()
                if (mManager != null) {
                    mManager.requestPeers(mChannel, myPeerListListener);
                }
            }
            else {
                // Wi-Fi P2P is not enabled
                Log.d("DiscoverPeers", "Wi-Fi P2P is not enabled.");
                System.exit(-1);
            }
        }
        else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            Log.d("DiscoverPeers", "P2P peers changed");
        }
        else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            Log.d("DiscoverPeers", "P2P connection changed");
        }
        else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            Log.d("DiscoverPeers", "P2P device changed");
        }
    }


}
