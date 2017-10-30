package com.example.peertopeer;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Html;
import android.util.Log;
import android.widget.TextView;

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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;


public class PeersListActivity extends Activity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private WifiP2pManager mManager;
    private Channel mChannel;

    private BluetoothAdapter mBluetoothAdapter;
    private final static int REQUEST_ENABLE_BT = 1;

    public GoogleApiClient mGoogleApiClient;

    private ServiceBase mServiceBase;

    private BluetoothListener mBluetoothListener;
    private String mDeviceName;

    private GossipData mData;
    public static List<String> mBluetoothDevices;

    private final String[] mColors = { "BLACK", "BLUE", "CYAN", "GREEN", "MAGENTA", "RED", "YELLOW" };
    private HashMap<String, String> mColorMap;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter != null) {
            if (!mBluetoothAdapter.isEnabled()) {
                Log.d("p2p_log", "Bluetooth is not enabled");
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }

        mDeviceName = mBluetoothAdapter.getName();

        mBluetoothListener = new BluetoothListener(mBluetoothAdapter);

        mGoogleApiClient = new GoogleApiClient.Builder(this).addConnectionCallbacks(this).
                addOnConnectionFailedListener(this).addApi(Nearby.CONNECTIONS_API).build();

        mGoogleApiClient.connect();

        mServiceBase = new ServiceBase(this);

        setContentView(R.layout.main_activity);

        mColorMap = new HashMap<>();
        mBluetoothDevices = new ArrayList<>();

        mBluetoothListener.mBluetoothServer.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        Log.d("p2p_log", "Is connecting: " + mGoogleApiClient.isConnecting());

        AsyncTask timer = new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] objects) {
                while (true) {
                    try {
                        Thread.sleep(10000);

                        if (mData == null) {
                            mData = GossipData.get(mDeviceName);
                        }

                        Random random = new Random();
                        if (random.nextBoolean()) {
                            mData.add(random.nextInt(100) + 1, mDeviceName);
                        }


                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                updateUI();
                            }
                        });

                        int numPeers = mBluetoothDevices.size();

                        if (numPeers > 0) {
                            int randomPeer = random.nextInt(numPeers);

                            String peer = mBluetoothDevices.get(randomPeer);

                            Log.d("p2p_log", "Trying to connect to " + peer);

                            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice("E4:58:B8:E0:6D:15");

                            BluetoothSocket socket = null;

                            long start = System.currentTimeMillis();
                            try {
                                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                                // MY_UUID is the app's UUID string, also used in the server code.
                                socket = device.createInsecureRfcommSocketToServiceRecord(BluetoothListener.MY_UUID);

                            }
                            catch (IOException e) {
                                Log.e("p2p_log", "Socket's create() method failed", e);
                            }

                            try {
                                // Connect to the remote device through the socket. This call blocks
                                // until it succeeds or throws an exception.
                                socket.connect();
                                Log.d("p2p_log", "Successfully connected in " + (System.currentTimeMillis() - start) + " milliseconds");

                                FileOperations.bluetoothSendData(socket, mDeviceName);
//                                Log.d("p2p_log", "Successfully sent in " + (System.currentTimeMillis() - start) + " milliseconds");

                                FileOperations.bluetoothGetData(socket, mDeviceName);
//                                Log.d("p2p_log", "Successfully received in " + (System.currentTimeMillis() - start) + " milliseconds");
                            }
                            catch (IOException connectException) {
                                // Unable to connect
                                Log.d("p2p_log", "Bluetooth socket connect failed");
                                try {
                                    socket.close();
                                }
                                catch (IOException e) {
                                    Log.d("p2p_log", "Failed to close Bluetooth socket");
                                }
                            }

                        }
                    }
                    catch (InterruptedException e) {
                        Log.e("p2p_log", e.toString());
                        break;
                    }
                }
                return null;
            }
        };

        timer.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }


    public final EndpointDiscoveryCallback mEndpointDiscoveryCallback = new EndpointDiscoveryCallback() {
        @Override
        public void onEndpointFound(String endpointId, DiscoveredEndpointInfo discoveredEndpointInfo) {
            // An endpoint was found!
            Log.d("p2p_log", "Endpoint found: " + endpointId);
            mBluetoothDevices.add(endpointId);

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
            }).setResultCallback(new ResultCallback<Status>() {
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

    @Override
    public void onStart() {
        super.onStart();
        Log.d("p2p_log", "On start");
        if (!mGoogleApiClient.isConnected() && !mGoogleApiClient.isConnecting()) {
            mGoogleApiClient.connect();
        }
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
        Log.d("p2p_log", "GoogleApiClient connected");

        mServiceBase.startAdvertising(mGoogleApiClient, mDeviceName);
        mServiceBase.startDiscovery(mGoogleApiClient, mDeviceName);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d("p2p_log", "Connection suspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d("p2p_log", "Connection failed");
    }

    public void updateUI() {
        if (mData == null) {
            return;
        }

        TextView dataView = findViewById(R.id.data_view);
        TreeMap<Integer, String> dataset = mData.getData();

        String text = "Number of elements: " + dataset.size() + "<br />";

        for (Map.Entry<Integer, String> entrySet : dataset.entrySet()) {
            int num = entrySet.getKey();
            String name = entrySet.getValue();
            if (!mColorMap.containsKey(name)) {
                mColorMap.put(name, mColors[mColorMap.size() % mColors.length]);
            }
            text += "<font color=" + mColorMap.get(name) + ">" + num + "</font>\t";
        }
        dataView.setText(Html.fromHtml(text), TextView.BufferType.SPANNABLE);
    }
}
