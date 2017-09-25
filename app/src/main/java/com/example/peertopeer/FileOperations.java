package com.example.peertopeer;

import android.app.Activity;
import android.bluetooth.BluetoothSocket;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.UUID;

public class FileOperations {

    public static final UUID MY_UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");


    public static void sendData(Socket socket, String name) throws IOException {
        if (socket == null) {
            Log.e("p2p_log", "Can't send: socket is null");
            return;
        }

        Log.d("p2p_log", "Sending data");

        OutputStream os = socket.getOutputStream();
        OutputStreamWriter osw = new OutputStreamWriter(os);
        BufferedWriter bw = new BufferedWriter(osw);

        GossipData gossipData = GossipData.get(name);
        bw.write(gossipData.mData.size() + "\n");

        for (int num : gossipData.mData.navigableKeySet()) {
            bw.write(num + " " + gossipData.mData.get(num) + "\n");
            Log.d("p2p_log", "Wrote: " + num + " " + gossipData.mData.get(num));
        }

        bw.flush();

        Log.d("p2p_log", "Finished sending data");
    }

    public static void getData(Socket socket, String deviceName) throws IOException {
        if (socket == null) {
            Log.e("p2p_log", "Can't receive data: socket is null");
            return;
        }

        InputStream is = socket.getInputStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);

        String line = br.readLine();
        Log.d("p2p_log", "Getting data");

        int numElements = 0;

        try {
            numElements = Integer.parseInt(line);
            Log.d("p2p_log", "Trying to read " + numElements + " items");
        }
        catch (NumberFormatException e) {
            Log.d("p2p_log", "Failed to get a valid integer, received: " + line);
        }


        for (int i = 0; i < numElements; i++) {
            line = br.readLine();

            Log.d("p2p_log", "Got: " + line);

            String[] split_line = line.split(" ", 2);

            int num = Integer.parseInt(split_line[0]);
            String peerName = split_line[1];
            GossipData gossipData = GossipData.get(deviceName);
            gossipData.mData.put(num, peerName);

        }

        Log.d("p2p_log", "Finished getting data");

        is.close();
        socket.getOutputStream().close();
    }

    public static void bluetoothSendData(BluetoothSocket socket, String name) throws IOException {
        if (socket == null) {
            Log.d("p2p_log", "Can't send: socket is null");
            return;
        }

        Log.d("p2p_log", "Sending data");

        OutputStream os = socket.getOutputStream();
        OutputStreamWriter osw = new OutputStreamWriter(os);
        BufferedWriter bw = new BufferedWriter(osw);

        GossipData gossipData = GossipData.get(name);
        bw.write(gossipData.mData.size() + "\n");

        for (int num : gossipData.mData.navigableKeySet()) {
            bw.write(num + " " + gossipData.mData.get(num) + "\n");
            Log.d("p2p_log", "Wrote: " + num + " " + gossipData.mData.get(num));
        }

        bw.flush();

        Log.d("p2p_log", "Finished sending data");

    }

    public static void bluetoothGetData(BluetoothSocket socket, String deviceName) throws IOException {

        InputStream is = socket.getInputStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);

        String line = br.readLine();
        Log.d("p2p_log", "Getting data");

        int numElements = Integer.parseInt(line);

        for (int i = 0; i < numElements; i++) {
            line = br.readLine();

            Log.d("p2p_log", "Got: " + line);

            String[] split_line = line.split(" ", 2);

            int num = Integer.parseInt(split_line[0]);
            String peerName = split_line[1];
            GossipData gossipData = GossipData.get(deviceName);
            gossipData.mData.put(num, peerName);

        }

        Log.d("p2p_log", "Finished getting data");

        is.close();
        socket.getOutputStream().close();
    }
}
