package com.example.peertopeer;

import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

public class Connection implements Runnable{
    private WifiP2pManager mManager;
    private Channel mChannel;
    private WifiP2pDeviceList peers;

    public Connection (WifiP2pDeviceList peerList, WifiP2pManager manager, Channel channel) {
        mManager = manager;
        mChannel = channel;
        peers = peerList;
    }

    public void run() {
        WifiP2pDevice device = (WifiP2pDevice)peers.getDeviceList().toArray()[0];
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;
        mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                Log.d("p2p_log", "Connected to device!");

                //Try creating a new thread for connecting
                Thread server_thread = new Thread(new Sockets());
                server_thread.start();

                Connection.Client client_thread = new Connection.Client();
                client_thread.execute();
            }

            @Override
            public void onFailure(int reason) {
                Log.d("p2p_log", "Failed to connect. Reason: " + reason);

            }
        });
    }

    private class Client extends AsyncTask<Void, Void, String> {
        String host;
        int port;
        int len;
        byte buf[]  = new byte[1024];

        @Override
        protected String doInBackground(Void... params) {
            /**
             * Create a client socket with the host,
             * port, and timeout information.
             */
            mManager.requestConnectionInfo(mChannel, new WifiP2pManager.ConnectionInfoListener() {
                @Override
                public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
                    InetAddress address = wifiP2pInfo.groupOwnerAddress;
                    Socket socket = new Socket();
                    try {
                        socket.bind(null);
                        socket.connect(new InetSocketAddress(address, 6003), 500);
                    }
                    catch(Exception e) {
                        Log.d("p2p_log", "Exception: " + e.getClass().getCanonicalName());
                    }
                    /**
                     * Clean up any open sockets when done
                     * transferring or if an exception occurred.
                     */
                    finally {
                        if (socket != null) {
                            if (socket.isConnected()) {
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                    //catch logic
                                }
                            }
                        }
                    }
                }
            });
            return null;
        }
    }
}
