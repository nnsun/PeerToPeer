package com.example.peertopeer;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
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

        mColorMap = new HashMap<>();
        mBluetoothDevices = new ArrayList<>();
        BluetoothOperations.enableDiscoverability(getContext());

        AsyncTask timer = new AsyncTask() {

            @Override
            protected Object doInBackground(Object[] objects) {
                while (true) {
                    try {
                        Thread.sleep(10000);

                        if (mWifiDirectReceiver.mDeviceName == null || mWifiDirectReceiver.mDeviceName.isEmpty() || mWifiDirectReceiver.mDeviceName.equals("null")) {
                            continue;
                        }
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

                        mWifiDirectReceiver.disconnect();

                        mManager.discoverPeers(mChannel, null);

                        mManager.requestGroupInfo(mChannel, new WifiP2pManager.GroupInfoListener() {

                            @Override
                            public void onGroupInfoAvailable(WifiP2pGroup wifiP2pGroup) {

                                if (wifiP2pGroup == null) {
                                    return;
                                }

                                if (wifiP2pGroup.getClientList().size() == 0) {
                                    mManager.requestPeers(mChannel, new WifiP2pManager.PeerListListener() {
                                        @Override
                                        public void onPeersAvailable(WifiP2pDeviceList peers) {

                                            Random random = new Random();
                                            int numPeers;

                                            if (mBluetoothReceiver.mServerSocket != null) {
                                                numPeers = peers.getDeviceList().size() + mBluetoothDevices.size();
                                            }
                                            else {
                                                numPeers = peers.getDeviceList().size();
                                            }

                                            if (numPeers > 0) {
                                                int randomPeer = random.nextInt(numPeers);
                                                int numWifiDirectPeers = peers.getDeviceList().size();

                                                // connecting to a Wi-Fi Direct peer
                                                if (randomPeer < numWifiDirectPeers) {
                                                    WifiP2pDevice peer = new ArrayList<>(peers.getDeviceList()).get(randomPeer);

                                                    WifiP2pConfig config = new WifiP2pConfig();
                                                    config.deviceAddress = peer.deviceAddress;
                                                    mManager.connect(mChannel, config, new ActionListener() {
                                                        @Override
                                                        public void onSuccess() {
                                                            Log.d("p2p_log", "Successfully connected");
                                                        }

                                                        @Override
                                                        public void onFailure(int i) {
                                                            Log.d("p2p_log", "Failed to connect. Reason: " + i);
                                                            mManager.cancelConnect(mChannel, null);
                                                        }
                                                    });
                                                }

                                                // Connect to a Bluetooth peer if it is not currently discovering peers
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
                                    });
                                }
                            }
                        });

                    } catch (Exception e) {
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
        getActivity().registerReceiver(mWifiDirectReceiver, mWifiDirectIntentFilter);
        getActivity().registerReceiver(mBluetoothReceiver, mBluetoothIntentFilter);
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


}
