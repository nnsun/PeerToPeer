package com.example.peertopeer;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.DnsSdServiceResponseListener;
import android.net.wifi.p2p.WifiP2pManager.DnsSdTxtRecordListener;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.AsyncTask;
import android.os.Build;
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
import java.io.InputStream;
import java.io.OutputStream;
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
    private IntentFilter mWifiDirectIntentFilter;

    private BluetoothBroadcastReceiver mBluetoothReceiver;
    private IntentFilter mBluetoothIntentFilter;
    private BluetoothAdapter mBluetoothAdapter;

    private GossipData mData;
    public static List<BluetoothDevice> mBluetoothDevices;
    private static Map<String, String> mWifiDirectDevices;

    private final String[] mColors = { "BLACK", "BLUE", "CYAN", "GREEN", "MAGENTA", "RED", "YELLOW" };
    private HashMap<String, String> mColorMap;

    public void setWifiDirectArgs(WifiP2pManager manager, Channel channel, WiFiDirectBroadcastReceiver receiver, IntentFilter filter) {
        mManager = manager;
        mChannel = channel;
        mWifiDirectReceiver = receiver;
        mWifiDirectIntentFilter = filter;
    }

    public void setBluetoothArgs(BluetoothAdapter adapter, BluetoothBroadcastReceiver receiver, IntentFilter intent) {
        mBluetoothReceiver = receiver;
        mBluetoothIntentFilter = intent;
        mBluetoothAdapter = adapter;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getContext().getApplicationContext().registerReceiver(mWifiDirectReceiver, mWifiDirectIntentFilter);

        mWifiDirectDevices = new HashMap<>();

        mManager.removeGroup(mChannel, null);

        mManager.createGroup(mChannel, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                Log.d("p2p_log", "Create group success");
            }

            @Override
            public void onFailure(int i) {
                Log.d("p2p_log", "Create group failure");
            }
        });


        mColorMap = new HashMap<>();
        mBluetoothDevices = new ArrayList<>();
        BluetoothOperations.enableDiscoverability(getContext());

        AsyncTask timer = new AsyncTask() {

            @Override
            protected Object doInBackground(Object[] objects) {
                while (true) {
                    try {
                        Thread.sleep(10000);

                        if (mData == null && mWifiDirectReceiver != null) {
                            mData = GossipData.get(mWifiDirectReceiver.mDeviceName);
                        }

                        Random random = new Random();
                        if (random.nextBoolean()) {
                            mData.add(random.nextInt(100) + 1, mWifiDirectReceiver.mDeviceName);
                        }

                        if (getView() != null) {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    updateUI(getView());
                                }
                            });
                        }

                        if (mWifiDirectReceiver.mClientSocket != null && !mWifiDirectReceiver.mClientSocket.isClosed()) {
                            mWifiDirectReceiver.mClientSocket.close();
                        }

                        discoverServices();

                        int numPeers;
                        int numWifiDirectPeers = mWifiDirectDevices.size();

                        if (mBluetoothReceiver.mServerSocket != null) {
                            numPeers = numWifiDirectPeers + mBluetoothDevices.size();
                        }
                        else {
                            numPeers = numWifiDirectPeers;
                        }

                        if (numPeers > 0) {
                            int randomPeer = random.nextInt(numPeers);

                            Log.d("p2p_log", randomPeer + "    " + mWifiDirectDevices.size());

                            // connecting to a Wi-Fi Direct peer
                            if (randomPeer < numWifiDirectPeers) {
                                String peer = new ArrayList<>(mWifiDirectDevices.keySet()).get(randomPeer);

                                WifiConfiguration config = new WifiConfiguration();
                                config.SSID = String.format("\"%s\"", peer);
                                config.preSharedKey = String.format("\"%s\"", mWifiDirectDevices.get(peer));
                                Log.d("p2p_log", "Trying to connect to: " + peer);

                                WifiManager wifiManager = (WifiManager)getContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);

                                Log.d("p2p_log", "Adding network...");
                                int id = wifiManager.addNetwork(config);
                                if (id == -1) {
                                    Log.d("p2p_log", "Error adding network");
                                }
                                else {
                                    Log.d("p2p_log", "Added network! Id: " + id);

                                    boolean connectSuccess = wifiManager.enableNetwork(id, true);

                                    if (connectSuccess) {
                                        Log.d("p2p_log", "Successfully connected!");
                                    }
                                    else {
                                        Log.d("p2p_log", "Failure to connect");
                                    }

                                }
                            }

                            else if (!mBluetoothAdapter.isDiscovering()) {
                                final BluetoothDevice peer = mBluetoothDevices.get(randomPeer - numWifiDirectPeers);

                                Log.d("p2p_log", "Trying to connect to " + peer.getName());

                                BluetoothSocket mmSocket = null;
                                try {
                                    // Get a BluetoothSocket to connect with the given BluetoothDevice.
                                    // MY_UUID is the app's UUID string, also used in the server code.
                                    mmSocket = peer.createRfcommSocketToServiceRecord(BluetoothOperations.MY_UUID);
                                }
                                catch (IOException e) {
                                    Log.e("p2p_log", "Socket's create() method failed", e);
                                }


                                mBluetoothAdapter.cancelDiscovery();
                                try {
                                    // Connect to the remote device through the socket. This call blocks
                                    // until it succeeds or throws an exception.
                                    mmSocket.connect();
                                    Log.d("p2p_log", "Successfully connected");

                                    FileOperations.bluetoothSendData(mmSocket, mBluetoothAdapter.getAddress());
                                    FileOperations.bluetoothGetData(mmSocket, mBluetoothAdapter.getAddress());


                                }
                                catch (IOException connectException) {
                                    // Unable to connect; close the socket and return.
                                    Log.d("p2p_log", "Bluetooth socket connect failed, trying fallback...");

                                    try {
                                        mmSocket = (BluetoothSocket)peer.getClass().getMethod("createRfcommSocket", new Class[] {int.class}).invoke(peer, 1);
                                        mmSocket.connect();
                                    }
                                    catch (Exception e) {
                                        Log.e("p2p_log", "Fallback Bluetooth connect failed");
                                        Log.e("p2p_log", e.getMessage());
                                    }
                                }
                            }


                        }
                    }
                    catch (Exception e) {
                        for (StackTraceElement elem : e.getStackTrace()) {
                            Log.e("p2p_log", elem.toString());
                        }
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

        Button bluetoothDiscover = view.findViewById(R.id.bluetooth_discover_button);
        bluetoothDiscover.setText("Discover Bluetooth peers");
        bluetoothDiscover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("p2p_log", "Bluetooth discovery button pressed");
                BluetoothOperations.discover(mBluetoothAdapter);
            }
        });

        updateUI(view);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        getContext().getApplicationContext().registerReceiver(mWifiDirectReceiver, mWifiDirectIntentFilter);
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

            mManager.removeGroup(mChannel, null);
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
                String[] delimited = s.split(":");
                String ssid = delimited[1];
                String passphrase = delimited[2];
                String rawAddr = delimited[3];
                String inetAddr = rawAddr.split("._presence._tcp.local.")[0];

                String name = wifiP2pDevice.deviceName;

                mWifiDirectDevices.put(ssid, passphrase);
            }
        };

        DnsSdServiceResponseListener servListener = new DnsSdServiceResponseListener() {
            @Override
            public void onDnsSdServiceAvailable(String instanceName, String registrationType,
                                                WifiP2pDevice resourceType) {

                // Update the device name with the human-friendly version from
                // the DnsTxtRecord, assuming one arrived.
                resourceType.deviceName = mWifiDirectDevices
                        .containsKey(resourceType.deviceAddress) ? mWifiDirectDevices
                        .get(resourceType.deviceAddress) : resourceType.deviceName;

            }
        };

        mManager.setDnsSdResponseListeners(mChannel, servListener, txtListener);

        WifiP2pDnsSdServiceRequest serviceRequest = WifiP2pDnsSdServiceRequest.newInstance();
        mManager.addServiceRequest(mChannel, serviceRequest, new ActionListener() {
            @Override
            public void onSuccess() {

            }

            @Override
            public void onFailure(int i) {
                Log.d("p2p_log", "Service request add failed");
            }
        });

        mManager.discoverServices(mChannel, new ActionListener() {
            @Override
            public void onSuccess() {
                // Success!
            }

            @Override
            public void onFailure(int code) {
                // Command failed.  Check for P2P_UNSUPPORTED, ERROR, or BUSY
                if (code == WifiP2pManager.P2P_UNSUPPORTED) {
                    Log.d(TAG, "P2P isn't supported on this device.");
                }
            }
        });
    }

}
