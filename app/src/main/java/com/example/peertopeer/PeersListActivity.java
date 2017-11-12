package com.example.peertopeer;

import android.annotation.SuppressLint;
import android.app.Activity;
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
import com.google.android.gms.nearby.Nearby;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.UUID;


public class PeersListActivity extends Activity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    public GoogleApiClient mGoogleApiClient;
    private ServiceBase mServiceBase;

    private String mDeviceName;
    private String mConnectedDevice;

    private TreeMap<Integer, String> mData;

    public static List<String> mDevices;

    private final String[] mColors = { "BLACK", "BLUE", "CYAN", "GREEN", "MAGENTA", "RED", "YELLOW" };
    private HashMap<String, String> mColorMap;

    public GoogleApiClient getGoogleApiClient() {
        return mGoogleApiClient;
    }

    public void setConnectedDevice(String device) {
        mConnectedDevice = device;
    }

    public void removeConnectedDevice(String device) {
        if (mConnectedDevice.equals(device)) {
            mConnectedDevice = "";
        }
    }

    public TreeMap<Integer, String> getData() {
        return mData;
    }

    public List<String> getDevices() {
        return mDevices;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDeviceName = UUID.randomUUID().toString();

        mData = new TreeMap<>();
        Random random = new Random();
        for (int i = 0; i < 1; i++) {
            mData.put(random.nextInt(100) + 1, mDeviceName);
        }

        mGoogleApiClient = new GoogleApiClient.Builder(this).addConnectionCallbacks(this).
                addOnConnectionFailedListener(this).addApi(Nearby.CONNECTIONS_API).build();
        mGoogleApiClient.connect();
        mDevices = new ArrayList<>();
        mColorMap = new HashMap<>();

        mConnectedDevice = "";

        mServiceBase = new ServiceBase(this);

        setContentView(R.layout.main_activity);

        @SuppressLint("StaticFieldLeak") AsyncTask timer = new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] objects) {
                while (true) {
                    try {
                        Random random = new Random();

                        Thread.sleep(random.nextInt(10000) + 10000);

                        if (!mConnectedDevice.isEmpty()) {
                            Nearby.Connections.disconnectFromEndpoint(mGoogleApiClient, mConnectedDevice);
                        }

                        if (random.nextBoolean()) {
                            mData.put(random.nextInt(100) + 1, mDeviceName);
                        }

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                updateUI();
                            }
                        });

                        int numPeers = mDevices.size();

                        if (numPeers > 0) {
                            int randomPeer = random.nextInt(numPeers);

                            String peer = mDevices.get(randomPeer);

                            Log.d("p2p_log", "Attempting to connect to: " + peer);

                            Nearby.Connections.requestConnection(mGoogleApiClient, peer, peer, mServiceBase)
                                    .setResultCallback(new ResultCallback<com.google.android.gms.common.api.Status>() {
                                @Override
                                public void onResult(@NonNull com.google.android.gms.common.api.Status status) {
                                    if (status.isSuccess()) {
                                        Log.d("p2p_log", "Successfully connected");
                                    }
                                    else {
                                        Log.d("p2p_log", "Failed to connect, error code: " + status.getStatusCode());
                                        if (!mConnectedDevice.isEmpty()) {
                                            Nearby.Connections.disconnectFromEndpoint(mGoogleApiClient, mConnectedDevice);
                                        }
                                    }
                                }
                            });

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
        Nearby.Connections.stopAdvertising(mGoogleApiClient);
        Nearby.Connections.stopDiscovery(mGoogleApiClient);
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d("p2p_log", "GoogleApiClient connected");
        mServiceBase.startAdvertising(mGoogleApiClient, mDeviceName);
        mServiceBase.startDiscovery(mGoogleApiClient);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d("p2p_log", "Connection suspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d("p2p_log", "Connection failed: " + connectionResult.getErrorCode() + "  " + connectionResult.getErrorMessage());
        mGoogleApiClient.reconnect();
    }

    public void updateUI() {
        if (mData == null) {
            return;
        }

        TextView dataView = findViewById(R.id.data_view);

        String text = "Number of elements: " + mData.size() + "<br />";

        for (Map.Entry<Integer, String> entrySet : mData.entrySet()) {
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
