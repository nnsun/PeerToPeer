package com.example.peertopeer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;


public class WiFiDirectBroadcastReceiver extends BroadcastReceiver {

    private WifiP2pManager mManager;
    private Channel mChannel;
    private PeersListActivity mActivity;

    public PeerListListener mPeerListListener;

    ServerSocket mServerSocket;

    HashMap<String, Socket> clientSockets;

    public WiFiDirectBroadcastReceiver(WifiP2pManager manager, Channel channel, PeersListActivity activity) {
        super();

        mManager = manager;
        mChannel = channel;
        mActivity = activity;

        clientSockets = new HashMap<>();


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

        mPeerListListener = new PeerListListener() {
            @Override
            public void onPeersAvailable(WifiP2pDeviceList peers) {
                Log.d("p2p_log", "Number of peers found: " + Integer.toString(peers.getDeviceList().size()));

                mActivity.mFragment.updateUI(peers);
            }
        };

        // Create the server socket
        try {
            mServerSocket = new ServerSocket(6003);
        }
        catch (IOException e) {
            Log.d("p2p_log", "IOException on server socket create");
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);

            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                // request available peers from the wifi p2p manager. This is an
                // asynchronous call and the calling activity is notified with a
                // callback on PeerListListener.onPeersAvailable()

                mManager.requestPeers(mChannel, mPeerListListener);

            }
            else {
                // Wi-Fi P2P is not enabled
                Log.d("p2p_log", "Wi-Fi P2P is not enabled.");
            }

        }
        else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            Log.d("p2p_log", "P2P peers changed");

            mManager.requestPeers(mChannel, mPeerListListener);

        }
        else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            Log.d("p2p_log", "P2P connection changed");

            mManager.requestPeers(mChannel, mPeerListListener);

            mManager.requestConnectionInfo(mChannel, new WifiP2pManager.ConnectionInfoListener() {
                @Override
                public void onConnectionInfoAvailable(final WifiP2pInfo wifiP2pInfo) {
                    if (wifiP2pInfo.groupFormed) {
                        if (wifiP2pInfo.isGroupOwner) {
                            AsyncTask groupOwner = new AsyncTask() {

                                @Override
                                protected Object doInBackground(Object[] objects) {
                                    try {
                                        Log.d("p2p_log", "Trying to create sockets");

                                        Socket clientSocket = mServerSocket.accept();

                                        Log.d("p2p_log", "Sockets created");
                                    }
                                    catch (IOException e) {
                                        Log.d("p2p_log", "IOException");
                                    }
                                    return null;
                                }

                            };

                            groupOwner.execute();
                        }

                        else {
                            AsyncTask groupClient = new AsyncTask() {

                                @Override
                                protected Object doInBackground(Object[] objects) {
                                    try {
                                        Log.d("p2p_log", "Trying to connect sockets");
                                        InetAddress address = wifiP2pInfo.groupOwnerAddress;
                                        Socket socket = new Socket();
                                        socket.bind(null);
                                        socket.connect(new InetSocketAddress(address, 6003), 500);
                                        Log.d("p2p_log", "Sockets connected!");

                                    }
                                    catch (IOException e) {
                                        Log.d("p2p_log", "IOException");
                                    }
                                    return null;
                                }

                            };

                            groupClient.execute();
                        }
                    }
                }
            });

        }
        else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            Log.d("p2p_log", "P2P device changed");

            mManager.requestPeers(mChannel, mPeerListListener);

        }
    }

}
