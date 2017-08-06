package com.example.peertopeer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
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

public class WiFiDirectBroadcastReceiver extends BroadcastReceiver {

    private WifiP2pManager mManager;
    private Channel mChannel;
    private PeersListActivity mActivity;

    public String mDeviceName;
    private String mNetworkName;
    private String mPassphrase;

    public ServerSocket mServerSocket;
    public Socket mClientSocket;

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
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        String action = intent.getAction();

        if (action.equals(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)) {
            mManager.requestGroupInfo(mChannel, new WifiP2pManager.GroupInfoListener() {

                @Override
                public void onGroupInfoAvailable(WifiP2pGroup wifiP2pGroup) {
                    mNetworkName = wifiP2pGroup.getNetworkName();
                    mPassphrase = wifiP2pGroup.getPassphrase();
                }
            });


            mManager.requestConnectionInfo(mChannel, new WifiP2pManager.ConnectionInfoListener() {
                @Override
                public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {

                    String inetAddress = wifiP2pInfo.groupOwnerAddress.getHostAddress();
                    String instance = "NI:" + mNetworkName + ":" + mPassphrase + ":" + inetAddress;

                    Map<String, String> record = new HashMap<>();
                    record.put("available", "visible");

                    WifiP2pDnsSdServiceInfo service = WifiP2pDnsSdServiceInfo.newInstance(instance, "_presence._tcp", record);

                    mManager.addLocalService(mChannel, service, new WifiP2pManager.ActionListener() {
                        @Override
                        public void onSuccess() {

                        }

                        @Override
                        public void onFailure(int i) {

                        }
                    });
                }
            });

        }
        else if (action.equals(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION) ||
                action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
            Log.d("p2p_log", "Network state change received");
        }
    }

}
