package com.example.peertopeer;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.DhcpInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.DnsSdServiceResponseListener;
import android.net.wifi.p2p.WifiP2pManager.DnsSdTxtRecordListener;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import static android.content.ContentValues.TAG;


public class PeersListFragment extends Fragment {

    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private WiFiDirectBroadcastReceiver mWifiDirectReceiver;

    private BluetoothBroadcastReceiver mBluetoothReceiver;
    private IntentFilter mBluetoothIntentFilter;
    private BluetoothAdapter mBluetoothAdapter;
    private WifiManager mWifiManager;

    private GossipData mData;
    public static List<String> mBluetoothDevices;

    private final String[] mColors = { "BLACK", "BLUE", "CYAN", "GREEN", "MAGENTA", "RED", "YELLOW" };
    private HashMap<String, String> mColorMap;

    public void setBluetoothArgs(BluetoothAdapter adapter, BluetoothBroadcastReceiver receiver, IntentFilter intent) {
        mBluetoothReceiver = receiver;
        mBluetoothIntentFilter = intent;
        mBluetoothAdapter = adapter;
    }

    public void setWifiDirectArgs(WifiP2pManager manager, Channel channel, WiFiDirectBroadcastReceiver receiver) {
        mManager = manager;
        mChannel = channel;
        mWifiDirectReceiver = receiver;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getContext().getApplicationContext().registerReceiver(mBluetoothReceiver, mBluetoothIntentFilter);

        mColorMap = new HashMap<>();
        mBluetoothDevices = new ArrayList<>();
        BluetoothOperations.enableDiscoverability(getContext());

        mWifiDirectReceiver.advertiseService(mBluetoothAdapter.getAddress());

        AsyncTask timer = new AsyncTask() {

            @Override
            protected Object doInBackground(Object[] objects) {
                while (true) {
                    try {
                        Thread.sleep(10000);

                        if (mData == null) {
                            mData =GossipData.get(mBluetoothAdapter.getAddress());
                        }

                        Random random = new Random();
                        if (random.nextBoolean()) {
                            mData.add(random.nextInt(100) + 1, mBluetoothAdapter.getAddress());
                        }

                        if (getView() != null) {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    updateUI(getView());
                                }
                            });
                        }

                        discoverServices();

                        int numPeers = mBluetoothDevices.size();

                        if (numPeers > 0) {
                            int randomPeer = random.nextInt(numPeers);

                            String peer = mBluetoothDevices.get(randomPeer);

                            Log.d("p2p_log", "Trying to connect to " + peer);

                            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(peer);

                            BluetoothSocket socket = null;
                            try {
                                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                                // MY_UUID is the app's UUID string, also used in the server code.
                                socket = device.createInsecureRfcommSocketToServiceRecord(BluetoothOperations.MY_UUID);
                            }
                            catch (IOException e) {
                                Log.e("p2p_log", "Socket's create() method failed", e);
                            }

                            try {
                                // Connect to the remote device through the socket. This call blocks
                                // until it succeeds or throws an exception.
                                socket.connect();
                                Log.d("p2p_log", "Successfully connected");

                                FileOperations.bluetoothSendData(socket, mBluetoothAdapter.getAddress());
                                FileOperations.bluetoothGetData(socket, mBluetoothAdapter.getAddress());


                            }
                            catch (IOException connectException) {
                                // Unable to connect; close the socket and return.
                                Log.d("p2p_log", "Bluetooth socket connect failed, trying fallback...");

                                try {
                                    socket = (BluetoothSocket) peer.getClass().getMethod("createRfcommSocket", new Class[]{int.class}).invoke(peer, 1);
                                    socket.connect();
                                } catch (Exception e) {
                                    Log.e("p2p_log", "Fallback Bluetooth connect failed");
                                    Log.e("p2p_log", e.getMessage());
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

        timer.execute();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.peers_list_fragment, container, false);

        updateUI(view);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        getContext().getApplicationContext().registerReceiver(mBluetoothReceiver, mBluetoothIntentFilter);
    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            getActivity().unregisterReceiver(mWifiDirectReceiver);
            getActivity().unregisterReceiver(mBluetoothReceiver);
        }
        catch (Exception e) {

        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            getActivity().unregisterReceiver(mWifiDirectReceiver);
            getActivity().unregisterReceiver(mBluetoothReceiver);
        }
        catch (Exception e) {

        }
    }

    public void updateUI(View view) {
        if (mData == null) {
            return;
        }

        TextView dataView = view.findViewById(R.id.data_view);
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


    private void discoverServices() {
        Log.d("p2p_log", "Discovering services...");

        DnsSdTxtRecordListener txtListener = new DnsSdTxtRecordListener() {
            @Override
            public void onDnsSdTxtRecordAvailable(String s, Map<String, String> map, WifiP2pDevice wifiP2pDevice) {
                Log.d("p2p_log", "Found a device");

                String name = wifiP2pDevice.deviceName;
            }
        };

        DnsSdServiceResponseListener servListener = new DnsSdServiceResponseListener() {
            @Override
            public void onDnsSdServiceAvailable(String address, String serviceType, WifiP2pDevice device) {
                if (serviceType.startsWith(WiFiDirectBroadcastReceiver.SERVICE_TYPE)) {
                    if (!mBluetoothDevices.contains(address)) {
                        mBluetoothDevices.add(address);
                    }

                }
            }
        };

        mManager.setDnsSdResponseListeners(mChannel, servListener, txtListener);

        WifiP2pDnsSdServiceRequest serviceRequest = WifiP2pDnsSdServiceRequest.newInstance();
        mManager.addServiceRequest(mChannel, serviceRequest, new ActionListener() {
            @Override
            public void onSuccess() {
                Log.d("p2p_log", "Service request successfully added");

            }

            @Override
            public void onFailure(int i) {
                Log.d("p2p_log", "Service request add failed");
            }
        });

        mManager.discoverServices(mChannel, new ActionListener() {
            @Override
            public void onSuccess() {

            }

            @Override
            public void onFailure(int code) {
                // Command failed.  Check for P2P_UNSUPPORTED, ERROR, or BUSY
                Log.d("p2p_log", "Error discovering services. Reason: " + code);

                if (code == WifiP2pManager.P2P_UNSUPPORTED) {
                    Log.d(TAG, "P2P isn't supported on this device.");
                }
            }
        });
    }

}
