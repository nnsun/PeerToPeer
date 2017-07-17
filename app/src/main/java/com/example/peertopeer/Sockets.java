package com.example.peertopeer;

import android.os.StrictMode;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class Sockets implements Runnable {
    @Override
    public void run() {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();

        StrictMode.setThreadPolicy(policy);
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);

        try {
            Log.d("p2p_log", "Trying to create sockets");

            ServerSocket serverSocket = new ServerSocket(8888);
            Socket client = serverSocket.accept();

            Log.d("p2p_log", "Sockets created");

            while (true) {
                //Reading the message from the client
                InputStream is = client.getInputStream();
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);
                String number = br.readLine();
                System.out.println("Message received from client is "+number);

                //Multiplying the number by 2 and forming the return message
                String returnMessage;
                try {
                    int numberInIntFormat = Integer.parseInt(number);
                    int returnValue = numberInIntFormat*2;
                    returnMessage = String.valueOf(returnValue) + "\n";
                }
                catch(NumberFormatException e) {
                    //Input was not a number. Sending proper message back to client.
                    returnMessage = "Please send a proper number\n";
                }

                Log.d("p2p_log", "Return message: " + returnMessage);
            }
        }
        catch(IOException e) {
            Log.d("p2p_log", "IO Exception :(");
            return;
        }


    }
}
