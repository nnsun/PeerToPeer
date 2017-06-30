package com.example.peertopeer;

import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class Sockets implements Runnable {
    @Override
    public void run() {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);

        try {
            Log.d("p2p_log", "Trying to create sockets");

            ServerSocket serverSocket = new ServerSocket(6003);
            Socket client = serverSocket.accept();

            Log.d("p2p_log", "Sockets created");
        }
        catch(IOException e) {
            Log.d("p2p_log", "IO Exception :(");
            return;
        }
    }
}
