package com.example.peertopeer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class WiFiDirectBroadcastReceiver extends BroadcastReceiver implements WifiP2pManager.GroupInfoListener,
        WifiP2pManager.ConnectionInfoListener {

    private WifiP2pManager mManager;
    private Channel mChannel;
    private PeersListActivity mActivity;

    public String mDeviceName;
    private String mNetworkName;
    private String mPassphrase;

    public ServerSocket mServerSocket;
    public Socket mClientSocket;

    private boolean mBroadcasted;

    public WiFiDirectBroadcastReceiver(WifiP2pManager manager, Channel channel, PeersListActivity activity) {
        super();

        mManager = manager;
        mChannel = channel;
        mActivity = activity;

        // Create the server socket
        try {
            mServerSocket = new ServerSocket(SocketOperations.WIFI_P2P_PORT);
        }
        catch (IOException e) {
            Log.e("p2p_log", "IOException on server socket create");
        }

        AsyncTask acceptThread = new AsyncTask() {

            @Override
            protected Object doInBackground(Object[] objects) {
                while (true) {
                    try {
                        mClientSocket = mServerSocket.accept();
                        Log.d("p2p_log", "Acting as server");

                        FileOperations.sendData(mClientSocket, mDeviceName);
                        FileOperations.getData(mClientSocket, mDeviceName);

                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            }
        };

        acceptThread.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        String action = intent.getAction();

//        if (action.equals(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)) {
//            mManager.requestGroupInfo(mChannel, this);
//            mManager.requestConnectionInfo(mChannel, this);
//        }
        if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
            Log.d("p2p_log", "Network state change received");
            NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            if (info.isConnected()) {
                Log.d("p2p_log", "Is connected to network");
            }
            else {
                Log.d("p2p_log", "Isn't connected to network");
            }
        }
        else if (action.equals(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION)) {
            Log.d("p2p_log", "Supplicant connection change received");
        }
    }

    @Override
    public void onGroupInfoAvailable(WifiP2pGroup wifiP2pGroup) {
//        if (wifiP2pGroup == null) {
//            mManager.requestGroupInfo(mChannel, this);
//            return;
//        }
//        mNetworkName = wifiP2pGroup.getNetworkName();
//        mPassphrase = wifiP2pGroup.getPassphrase();
//        mDeviceName = wifiP2pGroup.getOwner().deviceName;
    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
//        if (wifiP2pInfo == null || wifiP2pInfo.groupOwnerAddress == null) {
//            mManager.requestConnectionInfo(mChannel, this);
//            return;
//        }
//
//        if (wifiP2pInfo.isGroupOwner && mNetworkName != null && !mNetworkName.isEmpty()) {
//
//            if (mBroadcasted) {
//                return;
//            }
//
//            mBroadcasted = true;
//
//            Log.d("p2p_log", "Is group owner");
//
//            String inetAddress = wifiP2pInfo.groupOwnerAddress.getHostAddress();
//            String instance = "NI:" + mNetworkName + ":" + mPassphrase + ":" + inetAddress;
//
//            Map<String, String> record = new HashMap<>();
//            record.put("available", "visible");
//
//            WifiP2pDnsSdServiceInfo service = WifiP2pDnsSdServiceInfo.newInstance(instance, "_presence._tcp", record);
//
//            mManager.addLocalService(mChannel, service, new WifiP2pManager.ActionListener() {
//                @Override
//                public void onSuccess() {
//                    Log.d("p2p_log", "Added local service");
//                }
//
//                @Override
//                public void onFailure(int i) {
//                    Log.d("p2p_log", "Error adding local service");
//                }
//            });
//
//            mManager.requestGroupInfo(mChannel, this);
//        }
    }
}
