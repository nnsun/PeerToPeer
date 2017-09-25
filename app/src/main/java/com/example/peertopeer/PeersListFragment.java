package com.example.peertopeer;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.DnsSdServiceResponseListener;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.IOException;
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
    private ServiceBase mWifiService;

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothListener mBluetoothListener;

    private GossipData mData;
    public static List<String> mBluetoothDevices;

    private final String[] mColors = { "BLACK", "BLUE", "CYAN", "GREEN", "MAGENTA", "RED", "YELLOW" };
    private HashMap<String, String> mColorMap;

    public void setBluetoothArgs(BluetoothAdapter adapter) {
        mBluetoothAdapter = adapter;
        mBluetoothListener = new BluetoothListener(mBluetoothAdapter);
    }

    public void setWifiDirectArgs(WifiP2pManager manager, Channel channel, ServiceBase receiver) {
        mManager = manager;
        mChannel = channel;
        mWifiService = receiver;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mColorMap = new HashMap<>();
        mBluetoothDevices = new ArrayList<>();

        mWifiService.advertiseService(mBluetoothAdapter.getAddress());

        mBluetoothListener.mBluetoothServer.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        AsyncTask timer = new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] objects) {
                while (true) {
                    try {
                        Thread.sleep(10000);

                        if (mData == null) {
                            mData = GossipData.get(mBluetoothAdapter.getAddress());
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
                                socket = device.createInsecureRfcommSocketToServiceRecord(FileOperations.MY_UUID);
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
        discoverServices();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.peers_list_fragment, container, false);

        updateUI(view);

        return view;
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

        DnsSdServiceResponseListener servListener = new DnsSdServiceResponseListener() {
            @Override
            public void onDnsSdServiceAvailable(String address, String serviceType, WifiP2pDevice device) {
                Log.d("p2p_log", "Found a device: " + address);
                if (serviceType.startsWith(ServiceBase.SERVICE_TYPE)) {
                    if (!mBluetoothDevices.contains(address)) {
                        mBluetoothDevices.add(address);
                    }

                }
            }
        };

        mManager.setDnsSdResponseListeners(mChannel, servListener, null);

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
