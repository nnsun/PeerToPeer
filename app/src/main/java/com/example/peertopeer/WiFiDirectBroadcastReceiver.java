package com.example.peertopeer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class WiFiDirectBroadcastReceiver extends BroadcastReceiver {

    private WifiP2pManager mManager;
    private Channel mChannel;
    private PeersListActivity mActivity;

    public String mDeviceName;

    public ServerSocket mServerSocket;
    public Socket mClientSocket;

    public WiFiDirectBroadcastReceiver(WifiP2pManager manager, Channel channel, PeersListActivity activity) {
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

        // Create the server socket
        try {
            mServerSocket = new ServerSocket(SocketOperations.WIFI_P2P_PORT);
        }
        catch (IOException e) {
            Log.e("p2p_log", "IOException on server socket create");
        }
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        String action = intent.getAction();

        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);

            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                // request available peers from the wifi p2p manager. This is an
                // asynchronous call and the calling activity is notified with a
                // callback on PeerListListener.onPeersAvailable()

            }
            else {
                // Wi-Fi P2P is not enabled
                Log.d("p2p_log", "Wi-Fi P2P is not enabled.");
            }

        }
        else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            Log.d("p2p_log", "P2P peers changed");
        }

        else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            Log.d("p2p_log", "P2P connection changed");

            if (mDeviceName == null || mDeviceName.isEmpty() || mDeviceName.equals("null")) {
                Log.d("p2p_log", "Device name is invalid, exiting");
                return;
            }

            mManager.requestConnectionInfo(mChannel, new WifiP2pManager.ConnectionInfoListener() {
                @Override
                public void onConnectionInfoAvailable(final WifiP2pInfo wifiP2pInfo) {

                    if (wifiP2pInfo.groupFormed) {
                        if (wifiP2pInfo.isGroupOwner) {
                            Log.d("p2p_log", "This is the server");

                            mClientSocket = null;
                            AsyncTask groupOwner = new AsyncTask() {

                                @Override
                                protected Object doInBackground(Object[] objects) {
                                    try {
                                        Log.d("p2p_log", "Trying to create sockets");
                                        mClientSocket = mServerSocket.accept();

                                        String name = SocketOperations.getName(mClientSocket);

                                        Log.d("p2p_log", "Socket connected to " + name);

                                        FileOperations.sendData(mClientSocket, mDeviceName);
                                        FileOperations.getData(mClientSocket, mDeviceName);

                                    } catch (Exception e) {
                                        for (StackTraceElement elem : e.getStackTrace()) {
                                            Log.e("p2p_log", e.getMessage());
                                        }
                                    }
                                    return null;
                                }

                            };

                            groupOwner.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                        }

                        else {
                            Log.d("p2p_log", "This is a client");

                            AsyncTask groupClient = new AsyncTask() {

                                @Override
                                protected Object doInBackground(Object[] objects) {
                                    try {
                                        Log.d("p2p_log", "Trying to connect sockets");
                                        InetAddress address = wifiP2pInfo.groupOwnerAddress;
                                        Socket mClientSocket = SocketOperations.createSocket(address);

                                        Log.d("p2p_log", "Socket connected!");

                                        SocketOperations.sendName(mClientSocket, mDeviceName);

                                        FileOperations.sendData(mClientSocket, mDeviceName);
                                        FileOperations.getData(mClientSocket, mDeviceName);

                                    }
                                    catch (Exception e) {
                                        Log.d("p2p_log", e.getMessage());
                                    }
                                    return null;
                                }

                            };

                            groupClient.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                        }
                    }

                }
            });

        }
        else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            Log.d("p2p_log", "P2P device changed");

            if (mDeviceName == null || mDeviceName.isEmpty() || mDeviceName.equals("null")) {
                WifiP2pDevice device = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
                if (device == null || device.deviceName.equals("null")) {
                    Log.d("p2p_log", "Unable to get device name.");
                    mDeviceName = "";
                }
                else {
                    mDeviceName = device.deviceName;
                    Log.d("p2p_log", "name: " + mDeviceName);
                }
            }



        }
    }

    public void disconnect() {
        if (mManager != null && mChannel != null) {
            mManager.requestGroupInfo(mChannel, new WifiP2pManager.GroupInfoListener() {
                @Override
                public void onGroupInfoAvailable(WifiP2pGroup group) {
                    if (group != null && mManager != null && mChannel != null
                            && group.isGroupOwner()) {
                        mManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {

                            @Override
                            public void onSuccess() {
                                Log.d("p2p_log", "removeGroup onSuccess");
                            }

                            @Override
                            public void onFailure(int reason) {
                                Log.d("p2p_log", "removeGroup onFailure -" + reason);
                            }
                        });
                    }
                }
            });
        }
    }

}
