package com.example.peertopeer;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Strategy;


public class PeersListActivity extends SingleFragmentActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private WifiP2pManager mManager;
    private Channel mChannel;
    private ServiceBase mWifiDirectReceiver;

    private BluetoothAdapter mBluetoothAdapter;
    private final static int REQUEST_ENABLE_BT = 1;

    private PeersListFragment mFragment;

    public GoogleApiClient mGoogleApiClient;

    @Override
    public Fragment createFragment() {
        mManager = (WifiP2pManager)getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);
        mWifiDirectReceiver = new ServiceBase(mManager, mChannel, this);

        mFragment = new PeersListFragment();

        mGoogleApiClient = new GoogleApiClient.Builder(this).addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this).addApi(Nearby.CONNECTIONS_API).build();

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter != null) {
            if (!mBluetoothAdapter.isEnabled()) {
                Log.d("p2p_log", "Bluetooth is not enabled");
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }

        }
        else {
            Log.e("p2p_log", "Device does not support Bluetooth");
        }

        mFragment.setBluetoothArgs(mBluetoothAdapter);
        mFragment.setWifiDirectArgs(mManager, mChannel, mWifiDirectReceiver);
        return mFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mGoogleApiClient = new GoogleApiClient.Builder(this).addConnectionCallbacks(this).
                addOnConnectionFailedListener(this).addApi(Nearby.CONNECTIONS_API).build();
    }


    private final EndpointDiscoveryCallback mEndpointDiscoveryCallback =
            new EndpointDiscoveryCallback() {
                @Override
                public void onEndpointFound(
                        String endpointId, DiscoveredEndpointInfo discoveredEndpointInfo) {
                    // An endpoint was found!
//                    mBluetoothDevices.add(endpointId);

                    String name = android.os.Build.MODEL;
                    Nearby.Connections.requestConnection(mGoogleApiClient, name, endpointId, new ConnectionLifecycleCallback() {
                        @Override
                        public void onConnectionInitiated(
                                String endpointId, ConnectionInfo connectionInfo) {
                            // Automatically accept the connection on both sides.
                            Nearby.Connections.acceptConnection(
                                    mGoogleApiClient, endpointId, null);
                        }

                        @Override
                        public void onConnectionResult(String endpointId, ConnectionResolution result) {
                            switch (result.getStatus().getStatusCode()) {
                                case ConnectionsStatusCodes.STATUS_OK:
                                    // We're connected! Can now start sending and receiving data.
                                    Log.d("p2p_log", "Connection successful!");
                                    break;
                                case ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED:
                                    // The connection was rejected by one or both sides.
                                    Log.d("p2p_log", "Connection failed");
                                    break;
                            }
                        }

                        @Override
                        public void onDisconnected(String endpointId) {
                            // We've been disconnected from this endpoint. No more data can be
                            // sent or received.
                            Log.d("p2p_log", "Disconnected from endpoint");
                        }
                    }).setResultCallback(
                            new ResultCallback<Status>() {
                                @Override
                                public void onResult(@NonNull Status status) {
                                    if (status.isSuccess()) {
                                        // We successfully requested a connection. Now both sides
                                        // must accept before the connection is established.
                                    }
                                    else {
                                        // Nearby Connections failed to request the connection.
                                        Log.d("p2p_log", "Connection request failed");
                                    }
                                }
                            });
                }

                @Override
                public void onEndpointLost(String endpointId) {
                    // A previously discovered endpoint has gone away.
//                    mBluetoothDevices.remove(endpointId);
                }
            };


    private void startDiscovery() {
        Log.d("p2p_log", "Discovering services");

        Nearby.Connections.startDiscovery(mGoogleApiClient, "",
                mEndpointDiscoveryCallback, new DiscoveryOptions(Strategy.P2P_CLUSTER)).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        if (status.isSuccess()) {
                            // We're discovering!
                            Log.d("p2p_log", "Started discovery");
                        }
                        else {
                            // We were unable to start discovering.
                            Log.d("p2p_log", "Start discovery failed");
                        }
                    }
                });


    }


    @Override
    public void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }




}
