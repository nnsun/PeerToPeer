package com.example.peertopeer;

import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApi;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.Connections;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.Strategy;

import java.util.HashMap;

public class ServiceBase extends ConnectionLifecycleCallback {
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

    public void advertiseService(GoogleApiClient client, String address) {
        Log.d("p2p_log", "Bluetooth MAC address: " + address);
        Nearby.Connections.startAdvertising(client, address, address, this,
                    new AdvertisingOptions(Strategy.P2P_CLUSTER)).setResultCallback(
            new ResultCallback<Connections.StartAdvertisingResult>() {
                @Override
                public void onResult(@NonNull Connections.StartAdvertisingResult result) {
                    if (result.getStatus().isSuccess()) {
                        // We're advertising!
                    }
                    else {
                        // We were unable to start advertising.
                    }
                }
            });

    }

    @Override
    public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {
        // Automatically accept the connection on both sides.
        Nearby.Connections.acceptConnection(mActivity.mGoogleApiClient, endpointId, null);
    }

    @Override
    public void onConnectionResult(String endpointId, ConnectionResolution result) {
        switch (result.getStatus().getStatusCode()) {
            case ConnectionsStatusCodes.STATUS_OK:
                // We're connected! Can now start sending and receiving data.
                break;
            case ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED:
                // The connection was rejected by one or both sides.
                break;
        }
    }

    @Override
    public void onDisconnected(String endpointId) {
        // We've been disconnected from this endpoint. No more data can be
        // sent or received.
    }
}
