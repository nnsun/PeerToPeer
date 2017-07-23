package com.example.peertopeer;

import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

public class SocketOperations {

    public static final int WIFI_P2P_PORT = 6003;

    public static Socket createSocket(InetAddress address) throws IOException {
        Socket socket = new Socket();
        socket.bind(null);
        socket.connect(new InetSocketAddress(address, WIFI_P2P_PORT), 1000);
        return socket;
    }

    public static void sendName(Socket socket, String deviceName) throws IOException {
        OutputStream os = socket.getOutputStream();
        OutputStreamWriter osw = new OutputStreamWriter(os);
        BufferedWriter bw = new BufferedWriter(osw);

        String sendMessage = deviceName + "\n";
        bw.write(sendMessage);
        bw.flush();
    }

    public static String getName(Socket socket) throws IOException {
        InputStream is = socket.getInputStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        return br.readLine();
    }
}
