package com.example.peertopeer;

import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;


public class FileOperations {
    public static void bluetoothSendData(BluetoothSocket socket, String name) throws IOException {
        if (socket == null) {
            Log.d("p2p_log", "Can't send: socket is null");
            return;
        }

        OutputStream os = socket.getOutputStream();
        OutputStreamWriter osw = new OutputStreamWriter(os);
        BufferedWriter bw = new BufferedWriter(osw);

        GossipData gossipData = GossipData.get(name);
        bw.write(gossipData.mData.size() + "\n");

        for (int num : gossipData.mData.navigableKeySet()) {
            bw.write(num + " " + gossipData.mData.get(num) + "\n");
        }

        bw.flush();
    }

    public static void bluetoothGetData(BluetoothSocket socket, String deviceName) throws IOException {

        InputStream is = socket.getInputStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);

        String line = br.readLine();

        int numElements = Integer.parseInt(line);

        for (int i = 0; i < numElements; i++) {
            line = br.readLine();

            String[] split_line = line.split(" ", 2);

            int num = Integer.parseInt(split_line[0]);
            String peerName = split_line[1];
            GossipData gossipData = GossipData.get(deviceName);
            gossipData.mData.put(num, peerName);
        }
        is.close();
        socket.getOutputStream().close();
    }
}
