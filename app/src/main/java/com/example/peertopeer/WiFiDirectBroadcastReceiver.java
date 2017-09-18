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
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class WiFiDirectBroadcastReceiver extends BroadcastReceiver {

    public static final String SERVICE_TYPE = "_P2P_demo._tcp";

    private WifiP2pManager mManager;
    private Channel mChannel;
    private PeersListActivity mActivity;


    public WiFiDirectBroadcastReceiver(WifiP2pManager manager, Channel channel, PeersListActivity activity) {
        super();

        mManager = manager;
        mChannel = channel;
        mActivity = activity;
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {

    }

    public void advertiseService(String address) {
        HashMap<String, String> record = new HashMap<>();
        record.put("available", "visible");

        WifiP2pDnsSdServiceInfo service = WifiP2pDnsSdServiceInfo.newInstance(address, SERVICE_TYPE, record);

        mManager.addLocalService(mChannel, service, new WifiP2pManager.ActionListener() {
            public void onSuccess() {
                Log.d("p2p_log", "Successfully added service");
            }

            public void onFailure(int reason) {
                Log.d("p2p_log", "Add local service failed. Error code: " + reason);
            }
        });
    }

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
