package com.example.peertopeer;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
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
import com.google.android.gms.nearby.Nearby;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;


public class PeersListActivity extends Activity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private BluetoothAdapter mBluetoothAdapter;
    private final static int REQUEST_ENABLE_BT = 1;

    public GoogleApiClient mGoogleApiClient;

    private ServiceBase mServiceBase;

    private String mDeviceName;

    private TreeMap<Integer, String> mData;

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

        Random random = new Random();
        for (int i = 0; i < 1; i++) {
            mData.put(random.nextInt(100) + 1, mDeviceName);
        }

        mGoogleApiClient = new GoogleApiClient.Builder(this).addConnectionCallbacks(this).
                addOnConnectionFailedListener(this).addApi(Nearby.CONNECTIONS_API).build();

        mGoogleApiClient.connect();

        mServiceBase = new ServiceBase(mGoogleApiClient, mBluetoothDevices, mDeviceName, mData);

        setContentView(R.layout.main_activity);

        mColorMap = new HashMap<>();
        mBluetoothDevices = new ArrayList<>();

//        mBluetoothListener.mBluetoothServer.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        Log.d("p2p_log", "Is connecting: " + mGoogleApiClient.isConnecting());

        AsyncTask timer = new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] objects) {
                while (true) {
                    try {
                        Thread.sleep(10000);

                        Random random = new Random();
                        if (random.nextBoolean()) {
                            mData.put(random.nextInt(100) + 1, mDeviceName);
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

                            StringBuilder builder = new StringBuilder(512);
                            for (int num : mData.navigableKeySet()) {
                                builder.append(num + " " + mData.get(num) + "\n");
                            }
                            byte[] messageBytes = builder.toString().getBytes();
                            FileOperations.nearbySend(mGoogleApiClient, messageBytes, peer);
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
