package com.example.peertopeer;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
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
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;


public class PeersListFragment extends Fragment {

    protected static final int CHOOSE_FILE_RESULT_CODE = 20;
    private RecyclerView mPeerRecyclerView;
    private PeerAdapter mAdapter;

    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private WiFiDirectBroadcastReceiver mReceiver;

    private IntentFilter mIntentFilter;

    private Socket mCurrentSocket;

    public void setNetworkArguments(WifiP2pManager manager, Channel channel, WiFiDirectBroadcastReceiver receiver, IntentFilter filter) {
        mManager = manager;
        mChannel = channel;
        mReceiver = receiver;
        mIntentFilter = filter;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (mCurrentSocket == null) {
            Log.d("p2p_log", "mCurrentSocket is null");
            return null;
        }

        View view = inflater.inflate(R.layout.fragment_peer_list, container, false);

        mPeerRecyclerView = view.findViewById(R.id.peer_recycler_view);
        mPeerRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        if (mAdapter == null) {
            mAdapter = new PeerAdapter(new WifiP2pDeviceList());
            mPeerRecyclerView.setAdapter(mAdapter);
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().registerReceiver(mReceiver, mIntentFilter);
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(mReceiver);
    }

    public void updateUI(WifiP2pDeviceList peers) {

        if (mAdapter == null) {
            mAdapter = new PeerAdapter(peers);
            mPeerRecyclerView.setAdapter(mAdapter);
        }
        else {
            mAdapter.mPeers = peers;
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        // User has picked an image. Transfer it to group owner i.e peer using
        // FileTransferService.
        Uri uri = data.getData();
//        TextView statusText = (TextView) mPeerRecyclerView.findViewById(R.id.status_text);
//        statusText.setText("Sending: " + uri);
        Log.d("p2p_log", "Intent----------- " + uri);
        Context context = getActivity();
        try {
            OutputStream stream = mCurrentSocket.getOutputStream();
            ContentResolver cr = context.getContentResolver();
            InputStream is = null;
            try {
                is = cr.openInputStream(Uri.parse(uri.toString()));
            } catch (FileNotFoundException e) {
                Log.d("p2p_log", e.toString());
            }
            copyFile(is, stream);
            Log.d("p2p_log", "Client: Data written");
        } catch (IOException e) {
            Toast.makeText(context, "Failed to send file: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            Log.e("p2p_log", e.getMessage());
        }
    }

    public static boolean copyFile(InputStream inputStream, OutputStream out) {
        byte buf[] = new byte[1024];
        int len;
        try {
            while ((len = inputStream.read(buf)) != -1) {
                out.write(buf, 0, len);

            }
            out.close();
            inputStream.close();
        } catch (IOException e) {
            Log.d("p2p_log", e.toString());
            return false;
        }
        return true;
    }


    private class PeerHolder extends RecyclerView.ViewHolder {

        private TextView mDeviceTextView;
        private Button mConnectButton;
        private Button mSendButton;

        private WifiP2pDevice mPeer;


        public PeerHolder(View itemView) {
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
                mConnectButton.setText("Connect");
                mSendButton.setVisibility(View.GONE);

                mConnectButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        WifiP2pConfig config = new WifiP2pConfig();
                        config.deviceAddress = mPeer.deviceAddress;
                        mManager.connect(mChannel, config, new ActionListener() {
                            @Override
                            public void onSuccess() {
                                Log.d("p2p_log", "Successfully connected.");
                            }

                            @Override
                            public void onFailure(int i) {
                                Log.d("p2p_log", "Failed to connect.");
                            }
                        });
                    }
                });

                mSendButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (mReceiver.mClientSocket == null) {
                            // the device is the group owner
                            mCurrentSocket = mReceiver.mClientSockets.get(mPeer.deviceName);
                        }
                        else {
                            // the device is a client
                            mCurrentSocket = mReceiver.mClientSocket;
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

                mConnectButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (mReceiver.mClientSocket == null) {
                            try {
                                mReceiver.mClientSockets.get(mPeer.deviceName).close();
                            }
                            catch (Exception e) {
                                Log.d("p2p_log", e.toString());
                            }
                        }
                        else {
                            try {
                                mReceiver.mClientSocket.close();
                            }
                            catch (Exception e) {
                                Log.d("p2p_log", e.toString());
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

    private class PeerAdapter extends RecyclerView.Adapter<PeerHolder> {
        public WifiP2pDeviceList mPeers;

        public PeerAdapter(WifiP2pDeviceList peers) {
            mPeers = peers;
        }

        @Override
        public PeerHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
            View view = layoutInflater.inflate(R.layout.list_item_peer, parent, false);
            return new PeerHolder(view);
        }

        @Override
        public void onBindViewHolder(PeerHolder holder, int position) {
            WifiP2pDevice peer = new ArrayList<>(mPeers.getDeviceList()).get(position);
            holder.bindPeer(peer);
        }

        @Override
        public int getItemCount() {
            return mPeers.getDeviceList().size();
        }

    }

}
