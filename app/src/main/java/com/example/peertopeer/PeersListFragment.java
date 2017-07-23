package com.example.peertopeer;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;


public class PeersListFragment extends Fragment {

    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private WiFiDirectBroadcastReceiver mWifiDirectReceiver;
    private IntentFilter mWifiDirectIntentFilter;

    private GossipData mData;

    public void setWifiDirectArgs(WifiP2pManager manager, Channel channel, WiFiDirectBroadcastReceiver receiver, IntentFilter filter) {
        mManager = manager;
        mChannel = channel;
        mWifiDirectReceiver = receiver;
        mWifiDirectIntentFilter = filter;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mData = GossipData.get();

        AsyncTask timer = new AsyncTask() {

            @Override
            protected Object doInBackground(Object[] objects) {
                while (true) {
                    try {

                        Random random = new Random();
                        if (random.nextBoolean()) {
                            mData.add(random.nextInt(100) + 1);
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

                                if (wifiP2pGroup.getClientList().size() == 0) {
                                    mManager.requestPeers(mChannel, new WifiP2pManager.PeerListListener() {
                                        @Override
                                        public void onPeersAvailable(WifiP2pDeviceList peers) {

                                            Random random = new Random();
                                            int numPeers = peers.getDeviceList().size();
                                            if (numPeers > 0) {
                                                WifiP2pDevice peer = new ArrayList<>(peers.getDeviceList()).get(random.nextInt(numPeers));

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

                                        }
                                    });
                                }
                            }
                        });

                        Thread.sleep(10000);

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

        updateUI(view);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().registerReceiver(mWifiDirectReceiver, mWifiDirectIntentFilter);
//        getActivity().registerReceiver(mBluetoothReceiver, mBluetoothIntentFilter);
    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            getActivity().unregisterReceiver(mWifiDirectReceiver);
//            getActivity().unregisterReceiver(mBluetoothReceiver);
        }
        catch (Exception e) {

        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            getActivity().unregisterReceiver(mWifiDirectReceiver);
//            getActivity().unregisterReceiver(mBluetoothReceiver);
        }
        catch (Exception e) {

        }
    }

    public void updateUI(View view) {
        TextView dataView = view.findViewById(R.id.data_view);
        HashSet<Integer> dataset = mData.getData();

        dataView.setText("Number of elements: " + dataset.size() + "\n");

        for (int num : dataset) {
            dataView.append(num + "\t");
        }
    }


}
