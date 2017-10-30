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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.Connections;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Strategy;

import java.io.IOException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;


public class PeersListFragment extends Fragment {
    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private ServiceBase mWifiService;

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothListener mBluetoothListener;

    private String mBluetoothAddress;
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

        mBluetoothAddress = mBluetoothAdapter.getAddress();
        if (mBluetoothAddress.equals("02:00:00:00:00:00")) {
            List<NetworkInterface> all = null;
            try {
                all = Collections.list(NetworkInterface.getNetworkInterfaces());
            } catch (SocketException e) {
                e.printStackTrace();
            }
            for (NetworkInterface nif : all) {

                byte[] macBytes = new byte[0];
                try {
                    macBytes = nif.getHardwareAddress();
                } catch (SocketException e) {
                    e.printStackTrace();
                }
                if (macBytes == null) {
                    continue;
                }

                StringBuilder res1 = new StringBuilder();
                for (byte b : macBytes) {
                    res1.append(Integer.toHexString(b & 0xFF) + ":");
                }

                if (res1.length() > 0) {
                    res1.deleteCharAt(res1.length() - 1);
                }
                Log.d("p2p_log", nif.getName() + " " + res1.toString());
            }
        }

        mBluetoothListener.mBluetoothServer.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        AsyncTask timer = new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] objects) {
                while (true) {
                    try {
                        Thread.sleep(10000);

                        if (mData == null) {
                            mData = GossipData.get(mBluetoothAddress);
                        }

                        Random random = new Random();
                        if (random.nextBoolean()) {
                            mData.add(random.nextInt(100) + 1, mBluetoothAddress);
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

                                FileOperations.bluetoothSendData(socket, mBluetoothAddress);
//                                Log.d("p2p_log", "Successfully sent in " + (System.currentTimeMillis() - start) + " milliseconds");

                                FileOperations.bluetoothGetData(socket, mBluetoothAddress);
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

}
