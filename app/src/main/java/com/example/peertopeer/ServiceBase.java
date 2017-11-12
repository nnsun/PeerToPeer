package com.example.peertopeer;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.util.Log;

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


public class ServiceBase extends ConnectionLifecycleCallback {
    private static final String SERVICE_ID = "PeerToPeer";

    private PeersListActivity mActivity;

    public ServiceBase(PeersListActivity activity) {
        mActivity = activity;
    }

    public final EndpointDiscoveryCallback mEndpointDiscoveryCallback = new EndpointDiscoveryCallback() {
        @Override
        public void onEndpointFound(String endpointId, DiscoveredEndpointInfo discoveredEndpointInfo) {
            // An endpoint was found!
            Log.d("p2p_log", "Endpoint found: " + endpointId);
            mActivity.getDevices().add(endpointId);
        }

        @Override
        public void onEndpointLost(String endpointId) {
            // A previously discovered endpoint has gone away.
            Log.d("p2p_log", "Endpoint lost: " + endpointId);
            if (mActivity.getDevices().contains(endpointId)) {
                mActivity.getDevices().remove(endpointId);
            }
        }
    };

    public void startAdvertising(GoogleApiClient client, String name) {
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
        Nearby.Connections.acceptConnection(mActivity.getGoogleApiClient(), endpointId, new PayloadCallback() {
            @Override
            public void onPayloadReceived(String s, Payload payload) {
                Log.d("p2p_log", "Payload received");
                byte[] byteMessage = payload.asBytes();
                String message = new String(byteMessage);
                FileOperations.parseMessage(mActivity.getData(), message);
            }

            @Override
            public void onPayloadTransferUpdate(String s, PayloadTransferUpdate payloadTransferUpdate) {
                Log.d("p2p_log", "Payload transfer update");
            }
        });
    }

    @Override
    public void onConnectionResult(String endpointId, ConnectionResolution result) {
        if (result.getStatus().getStatusCode() == ConnectionsStatusCodes.STATUS_OK) {
            Log.d("p2p_log", "We are now connected!");
            mActivity.setConnectedDevice(endpointId);

            StringBuilder builder = new StringBuilder(512);
            for (int num : mActivity.getData().navigableKeySet()) {
                builder.append(num + " " + mActivity.getData().get(num) + "\n");
            }
            byte[] messageBytes = builder.toString().getBytes();
            FileOperations.nearbySend(mActivity.getGoogleApiClient(), messageBytes, endpointId);
        }
        else {
            Log.d("p2p_log", "Error: Connection was rejected");
        }
    }

    @Override
    public void onDisconnected(String endpointId) {
        // We've been disconnected from this endpoint. No more data can be
        // sent or received.
        Log.d("p2p_log", "Disconnected");
        mActivity.removeConnectedDevice(endpointId);
    }
}
