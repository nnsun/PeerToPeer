package com.example.peertopeer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;


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
                Log.d("p2p_log", "Find peers successful");
            }

            @Override
            public void onFailure(int reasonCode) {
                Log.d("p2p_log", "Find peers failed. Reason: " + reasonCode);
            }
        });
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            Log.d("p2p_log", "State changed to " + state);
            //PeerListListener myPeerListListener = connectToPeer();

            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                // request available peers from the wifi p2p manager. This is an
                // asynchronous call and the calling activity is notified with a
                // callback on PeerListListener.onPeersAvailable()
                mActivity.setIsWifiP2pEnabled(true);
//                if (mManager != null) {
//                    mManager.requestPeers(mChannel, myPeerListListener);
//                }
            }
            else {
                // Wi-Fi P2P is not enabled
                mActivity.setIsWifiP2pEnabled(false);
                Log.d("p2p_log", "Wi-Fi P2P is not enabled.");
                mActivity.resetData();
            }

        }
        else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            // request available peers from the wifi p2p manager. This is an
            // asynchronous call and the calling activity is notified with a
            // callback on PeerListListener.onPeersAvailable()

            //PeerListListener myPeerListListener = connectToPeer();
            Log.d("p2p_log", "P2P peers changed");
            if (mManager != null) {
                mManager.requestPeers(mChannel, (PeerListListener) mActivity.getFragmentManager()
                        .findFragmentById(R.id.frag_list));
            }

        }
        else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            if (mManager == null)
                return;
            Log.d("p2p_log", "P2P connection changed");
            NetworkInfo networkInfo = (NetworkInfo) intent
                    .getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

            if (networkInfo.isConnected()) {

                // we are connected with the other device, request connection
                // info to find group owner IP

                DeviceDetailFragment fragment = (DeviceDetailFragment) mActivity
                        .getFragmentManager().findFragmentById(R.id.frag_detail);
                mManager.requestConnectionInfo(mChannel, fragment);
            } else {
                // It's a disconnect
                mActivity.resetData();
            }
            //PeerListListener myPeerListListener = connectToPeer();
            //mManager.requestPeers(mChannel, myPeerListListener);

        }
        else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            Log.d("p2p_log", "P2P device changed");
            DeviceListFragment fragment = (DeviceListFragment) mActivity.getFragmentManager()
                    .findFragmentById(R.id.frag_list);
            fragment.updateThisDevice((WifiP2pDevice) intent.getParcelableExtra(
                    WifiP2pManager.EXTRA_WIFI_P2P_DEVICE));

        }
    }

    private PeerListListener connectToPeer() {
        return new PeerListListener() {
                    @Override
                    public void onPeersAvailable(WifiP2pDeviceList peers) {
                        Log.d("p2p_log", "Number of peers found: " + Integer.toString(peers.getDeviceList().size()));

                        for (WifiP2pDevice peer : peers.getDeviceList()) {
                            Log.d("p2p_log", peer.toString());
                        }

                        if (peers.getDeviceList().size() == 1) {
                            Log.d("p2p_log", "Attempting to connect");
                            Thread makeConnection = new Thread(new Connection(peers, mManager, mChannel));
                            makeConnection.start();

//                            WifiP2pDevice device = (WifiP2pDevice)peers.getDeviceList().toArray()[0];
//                            WifiP2pConfig config = new WifiP2pConfig();
//                            config.deviceAddress = device.deviceAddress;
//                            mManager.connect(mChannel, config, new ActionListener() {
//
//                                @Override
//                                public void onSuccess() {
//                                    Log.d("p2p_log", "Connected to device!");
//
//                                    //Try creating a new thread for connecting
//                                    Thread server_thread = new Thread(new Sockets());
//                                    server_thread.start();
//
//                                    Client client_thread = new Client();
//                                    client_thread.execute();
//                                }
//
//                                @Override
//                                public void onFailure(int reason) {
//                                    Log.d("p2p_log", "Failed to connect. Reason: " + reason);
//
//                                }
//                            });
                        }
                    }
                };
    }

}
