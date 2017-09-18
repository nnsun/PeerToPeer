package com.example.peertopeer;

import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;

import java.util.HashMap;

public class ServiceOperations {
    public void advertiseService(String address) {

        HashMap<String, String> record = new HashMap<>();
        record.put("available", "visible");

        WifiP2pDnsSdServiceInfo service = WifiP2pDnsSdServiceInfo.newInstance(address, WifiBase.SERVICE_TYPE, record);

        p2p.addLocalService(channel, service, new WifiP2pManager.ActionListener() {
            public void onSuccess() {
            }

            public void onFailure(int reason) {

            }
        });
    }

    public void stopAdvertiseService() {
        p2p.clearLocalServices(channel, new WifiP2pManager.ActionListener() {
            public void onSuccess() {

            }

            public void onFailure(int reason) {

            }
        });
    }
}
