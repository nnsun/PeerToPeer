package com.example.peertopeer;

import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
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
import java.util.List;


public class PeersListFragment extends Fragment {

    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private WiFiDirectBroadcastReceiver mWifiDirectReceiver;
    private IntentFilter mWifiDirectIntentFilter;

    private BluetoothBroadcastReceiver mBluetoothReceiver;
    private IntentFilter mBluetoothIntentFilter;

    private Socket mCurrentSocket;

    protected static final int CHOOSE_FILE_RESULT_CODE = 20;
    private RecyclerView mWifiP2pRecyclerView;
    private WifiP2pAdapter mWifiP2pAdapter;
    private RecyclerView mBluetoothRecyclerView;
    private BluetoothAdapter mBluetoothAdapter;

    public void setWifiDirectArgs(WifiP2pManager manager, Channel channel, WiFiDirectBroadcastReceiver receiver, IntentFilter filter) {
        mManager = manager;
        mChannel = channel;
        mWifiDirectReceiver = receiver;
        mWifiDirectIntentFilter = filter;
    }

    public void setBluetoothArgs(BluetoothBroadcastReceiver receiver, IntentFilter filter) {
        mBluetoothReceiver = receiver;
        mBluetoothIntentFilter = filter;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.peers_list_fragment, container, false);

        mWifiP2pRecyclerView = view.findViewById(R.id.wifi_p2p_list_recycler_view);
        mWifiP2pRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

//        mBluetoothRecyclerView = view.findViewById(R.id.bluetooth_list_recyler_view);
//        mBluetoothRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        if (mWifiP2pAdapter == null) {
            mWifiP2pAdapter = new WifiP2pAdapter(new WifiP2pDeviceList());
            mWifiP2pRecyclerView.setAdapter(mWifiP2pAdapter);
        }

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

    public void updateUI(WifiP2pDeviceList peers) {
        if (mWifiP2pRecyclerView != null) {

            for (WifiP2pDevice dev : peers.getDeviceList()) {
                Log.d("p2p_log", dev.deviceName + " " + dev.isGroupOwner());
            }

            int numChildren = mWifiP2pAdapter.getItemCount();
            Log.d("p2p_log", "Num children: " + numChildren);
            if (mWifiDirectReceiver.mClientSockets.size() == 0 && mWifiDirectReceiver.mClientSocket != null) {
                for (int i = 0; i < numChildren; i++) {
                    WifiP2pHolder holder = (WifiP2pHolder)mWifiP2pRecyclerView.findViewHolderForAdapterPosition(i);


                    // if the device is not a group owner and is already connected to a device,
                    // disable the connect button
                    if (holder != null && holder.mConnectButton != null && holder.mConnectButton.getText().equals("Connect")) {
                        holder.mConnectButton.setEnabled(false);
                    }

                }
            }

            if (mWifiP2pAdapter == null) {
                mWifiP2pAdapter = new WifiP2pAdapter(peers);
                mWifiP2pRecyclerView.setAdapter(mWifiP2pAdapter);
            }
            else {
                mWifiP2pAdapter.mPeers = peers;
                mWifiP2pAdapter.notifyDataSetChanged();
            }

            Log.d("p2p_log", "In updateUI");


        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User has picked an image. Transfer it to group owner i.e peer using FileTransferService.
        FileOperations.sendImage(data, getActivity(), mCurrentSocket);
    }
    

    private class WifiP2pHolder extends RecyclerView.ViewHolder {

        private TextView mDeviceTextView;
        public Button mConnectButton;
        private Button mSendButton;

        private WifiP2pDevice mPeer;


        public WifiP2pHolder(View itemView) {
            super(itemView);

            mDeviceTextView = itemView.findViewById(R.id.list_item_peer_device_text_view);
            mConnectButton = itemView.findViewById(R.id.list_item_peer_connect_button);
            mSendButton = itemView.findViewById(R.id.list_item_peer_send_button);
        }

        public void bindPeer(WifiP2pDevice peer) {
            mPeer = peer;

            mDeviceTextView.setText(mPeer.deviceName);
            mSendButton.setText("Send image");

            if (mPeer.status != WifiP2pDevice.CONNECTED) {
                // if this device is not connected to this peer

                mConnectButton.setText("Connect");
                mSendButton.setVisibility(View.GONE);

                mConnectButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Log.d("p2p_log", "Connect button clicked");
                        WifiP2pConfig config = new WifiP2pConfig();
                        config.deviceAddress = mPeer.deviceAddress;
                        mManager.connect(mChannel, config, new ActionListener() {
                            @Override
                            public void onSuccess() {
                                Log.d("p2p_log", "Successfully connected.");
                            }

                            @Override
                            public void onFailure(int i) {
                                Log.d("p2p_log", "Failed to connect. Reason: " + i);
                            }
                        });
                    }
                });

                mSendButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (mWifiDirectReceiver.mClientSocket == null) {
                            // the device is the group owner
                            mCurrentSocket = mWifiDirectReceiver.mClientSockets.get(mPeer.deviceName);
                        }
                        else {
                            // the device is a client
                            mCurrentSocket = mWifiDirectReceiver.mClientSocket;
                        }

                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.setType("image/*");
                        startActivityForResult(intent, CHOOSE_FILE_RESULT_CODE);
                    }
                });


            }

            else {
                mSendButton.setVisibility(View.VISIBLE);
                mConnectButton.setText("Disconnect");
                mConnectButton.setEnabled(true);

                mConnectButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Log.d("p2p_log", "Connect button clicked");
                        if (mWifiDirectReceiver.mClientSocket == null) {
                            try {
                                mWifiDirectReceiver.mClientSockets.get(mPeer.deviceName).close();
                            }
                            catch (Exception e) {
                                Log.e("p2p_log", e.toString());
                            }
                        }
                        else {
                            try {
                                mWifiDirectReceiver.mClientSocket.close();
                            }
                            catch (Exception e) {
                                Log.e("p2p_log", e.toString());
                            }
                        }

                        mManager.removeGroup(mChannel, new ActionListener() {
                            @Override
                            public void onSuccess() {

                            }

                            @Override
                            public void onFailure(int i) {

                            }
                        });

                    }
                });
            }
        }
    }

    private class WifiP2pAdapter extends RecyclerView.Adapter<WifiP2pHolder> {
        public WifiP2pDeviceList mPeers;

        public WifiP2pAdapter(WifiP2pDeviceList peers) {
            mPeers = peers;
        }

        @Override
        public WifiP2pHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
            View view = layoutInflater.inflate(R.layout.list_item_peer, parent, false);
            return new WifiP2pHolder(view);
        }

        @Override
        public void onBindViewHolder(WifiP2pHolder holder, int position) {
            WifiP2pDevice peer = new ArrayList<>(mPeers.getDeviceList()).get(position);
            holder.bindPeer(peer);
        }

        @Override
        public int getItemCount() {
            return mPeers.getDeviceList().size();
        }

    }

    private class BluetoothHolder extends RecyclerView.ViewHolder {
        public BluetoothHolder(View itemView) {
            super(itemView);
        }
    }

    private class BluetoothAdapter extends RecyclerView.Adapter<BluetoothHolder> {

        @Override
        public BluetoothHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return null;
        }

        @Override
        public void onBindViewHolder(BluetoothHolder holder, int position) {

        }

        @Override
        public int getItemCount() {
            return 0;
        }
    }


    }
