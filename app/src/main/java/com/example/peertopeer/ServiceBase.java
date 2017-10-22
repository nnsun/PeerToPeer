package com.example.peertopeer;

import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.util.Log;

import java.util.HashMap;

public class ServiceBase {
    public static final String SERVICE_TYPE = "_P2P_demo._tcp";

    private WifiP2pManager mManager;
    private Channel mChannel;
    private PeersListActivity mActivity;


    public ServiceBase(WifiP2pManager manager, Channel channel, PeersListActivity activity) {
        super();

        mManager = manager;
        mChannel = channel;
        mActivity = activity;
    }

    public void advertiseService(String address) {
        Log.d("p2p_log", "Bluetooth MAC address: " + address);
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
}
