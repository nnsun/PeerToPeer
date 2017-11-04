package com.example.peertopeer;

import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApi;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.Connections;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;

import java.util.List;
import java.util.TreeMap;

public class ServiceBase extends ConnectionLifecycleCallback {
    private static final String SERVICE_ID = "PeerToPeer";

    GoogleApiClient mGoogleApiClient;
    private List<String> mBluetoothDevices;
    private String mDeviceName;
    private TreeMap<Integer, String> mData;

    public ServiceBase(GoogleApiClient client, List<String> devices, String name, TreeMap<Integer, String> data) {
        super();
        mGoogleApiClient = client;
        mBluetoothDevices = devices;
        mDeviceName = name;
        mData = data;
    }

    public final EndpointDiscoveryCallback mEndpointDiscoveryCallback = new EndpointDiscoveryCallback() {
        @Override
        public void onEndpointFound(String endpointId, DiscoveredEndpointInfo discoveredEndpointInfo) {
            // An endpoint was found!
            Log.d("p2p_log", "Endpoint found: " + endpointId);
            mBluetoothDevices.add(endpointId);
        }

        @Override
        public void onEndpointLost(String endpointId) {
            // A previously discovered endpoint has gone away.
            mBluetoothDevices.remove(endpointId);
        }
    };

    public void startAdvertising(GoogleApiClient client, String name) {
        Log.d("p2p_log", "Device name: " + name);
        Nearby.Connections.startAdvertising(client, name, SERVICE_ID, this,
                    new AdvertisingOptions(Strategy.P2P_CLUSTER)).setResultCallback(
            new ResultCallback<Connections.StartAdvertisingResult>() {
                @Override
                public void onResult(@NonNull Connections.StartAdvertisingResult result) {
                    if (result.getStatus().isSuccess()) {
                        // We're advertising!
                        Log.d("p2p_log", "Now advertising");
                    }
                    else {
                        // We were unable to start advertising.
                        Status status = result.getStatus();
                        Log.d("p2p_log", "Failed to advertise: " + status.getStatusCode() + "  " + status.getStatusMessage());
                    }
                }
            });
    }

    public void startDiscovery(GoogleApiClient client) {
        Log.d("p2p_log", "Discovering services");

        Nearby.Connections.startDiscovery(client, SERVICE_ID, mEndpointDiscoveryCallback, new DiscoveryOptions(Strategy.P2P_CLUSTER)).
                setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        if (status.isSuccess()) {
                            // We're discovering!
                            Log.d("p2p_log", "Started discovery");
                        }
                        else {
                            // We were unable to start discovering.
                            Log.d("p2p_log", "Start discovery failed: " + status.getStatusCode() + "   " + status.getStatusMessage());
                        }
                    }
                });
    }


    @Override
    public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {
        // Automatically accept the connection on both sides.
        Nearby.Connections.acceptConnection(mGoogleApiClient, endpointId, new PayloadCallback() {
            @Override
            public void onPayloadReceived(String s, Payload payload) {
                Log.d("p2p_log", "Payload received");
                byte[] byteMessage = payload.asBytes();
                String message = new String(byteMessage);
                FileOperations.parseMessage(mData, message);
            }

            @Override
            public void onPayloadTransferUpdate(String s, PayloadTransferUpdate payloadTransferUpdate) {
                Log.d("p2p_log", "Payload transfer update");
            }
        });
    }

    @Override
    public void onConnectionResult(String endpointId, ConnectionResolution result) {
        switch (result.getStatus().getStatusCode()) {
            case ConnectionsStatusCodes.STATUS_OK:
                // We're connected! Can now start sending and receiving data.
                Log.d("p2p_log", "We are now connected!");
                break;
            case ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED:
                // The connection was rejected by one or both sides.
                Log.d("p2p_log", "The connection was rejected on one or both s.");
                break;
        }
    }

    @Override
    public void onDisconnected(String endpointId) {
        // We've been disconnected from this endpoint. No more data can be
        // sent or received.
        Log.d("p2p_log", "Disconnected");
    }
}
