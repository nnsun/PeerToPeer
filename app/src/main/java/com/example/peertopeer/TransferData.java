package com.example.peertopeer;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * A service that process each file transfer request i.e Intent by opening a
 * socket connection with the WiFi Direct Group Owner and writing the file
 */
public class TransferData extends IntentService {

    private static final int SOCKET_TIMEOUT = 5000;
    public static final String ACTION_SEND_FILE = "com.example.peertopeer.SEND_FILE";
    public static final String EXTRAS_FILE_PATH = "file_url";
    public static final String EXTRAS_GROUP_OWNER_ADDRESS = "go_host";
    public static final String EXTRAS_GROUP_OWNER_PORT = "go_port";

    public TransferData(String name) {
        super(name);
    }

    public TransferData() {
        super("TransferData");
    }

    /*
     * (non-Javadoc)
     * @see android.app.IntentService#onHandleIntent(android.content.Intent)
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d("p2p_log", "In onHandleIntent! Tank teh lort!");

        Context context = getApplicationContext();
        if (intent.getAction().equals(ACTION_SEND_FILE)) {
            String fileUri = intent.getExtras().getString(EXTRAS_FILE_PATH);
            String host = intent.getExtras().getString(EXTRAS_GROUP_OWNER_ADDRESS);
            Socket socket = new Socket();
            int port = intent.getExtras().getInt(EXTRAS_GROUP_OWNER_PORT);

            try {
                Log.d("p2p_log", "Opening client socket - ");
                Toast.makeText(context, "Opening client socket...", Toast.LENGTH_SHORT).show();
                socket.bind(null);
                Toast.makeText(context, "Binding client socket...", Toast.LENGTH_SHORT).show();
                socket.connect((new InetSocketAddress(host, port)), SOCKET_TIMEOUT);

                Log.d("p2p_log", "Client socket - " + socket.isConnected());
                Toast.makeText(context, "Client socket - " + socket.isConnected(),
                        Toast.LENGTH_LONG).show();

                OutputStream stream = socket.getOutputStream();
                ContentResolver cr = context.getContentResolver();
                InputStream is = null;
                try {
                    is = cr.openInputStream(Uri.parse(fileUri));
                } catch (FileNotFoundException e) {
                    Log.d("p2p_log", e.toString());
                }
                DeviceDetailFragment.copyFile(is, stream);
                Log.d("p2p_log", "Client: Data written");
            } catch (IOException e) {
                Toast.makeText(context, "Failed to open client socket: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
                Log.e("p2p_log", e.getMessage());
            } finally {
                if (socket != null) {
                    if (socket.isConnected()) {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            // Give up
                            e.printStackTrace();
                        }
                    }
                }
            }

        }
    }
}