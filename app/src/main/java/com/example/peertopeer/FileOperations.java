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

public class FileOperations {
    public static void sendImage(Intent data, Context context, Socket socket) {
        Uri uri = data.getData();

        try {
            if (socket == null) {
                Log.d("p2p_log", "Can't send: socket is null");
                return;
            }
            OutputStream stream = socket.getOutputStream();
            ContentResolver cr = context.getContentResolver();
            InputStream is = null;
            try {
                is = cr.openInputStream(Uri.parse(uri.toString()));
            }
            catch (FileNotFoundException e) {
                Log.e("p2p_log", e.toString());
            }
            copyFile(is, stream);
            Log.d("p2p_log", "Client: Data written");
        }
        catch (IOException e) {
            Toast.makeText(context, "Failed to send file: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            Log.e("p2p_log", e.getMessage());
        }

        Log.d("p2p_log", "Sent image");
    }

    public static int copyFile(InputStream inputStream, OutputStream outputStream) throws IOException {
        byte buf[] = new byte[16384];
        int size = 0;

        int len = inputStream.read(buf);
        while (len > 0) {
            size += len;
            outputStream.write(buf, 0, len);
            len = inputStream.read(buf);
            Log.d("p2p_log", "In while loop");
        }

        inputStream.close();
        outputStream.close();

        return size;
    }

    public static void getImage(Socket socket, Context context) throws IOException {
        final File f = new File(Environment.getExternalStorageDirectory() + "/"
                + context.getPackageName() + "/wifip2pshared-" + System.currentTimeMillis()
                + ".jpg");

        File dirs = new File(f.getParent());
        if (!dirs.exists())
            dirs.mkdirs();
        f.createNewFile();

        Log.d("p2p_log", "Trying to read");

        if (f.getAbsoluteFile() != null) {
            if (copyFile(socket.getInputStream(), new FileOutputStream(f)) > 0) {
                Intent intent = new Intent();
                intent.setAction(android.content.Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse("file://" + f.getAbsolutePath()), "image/*");
                context.startActivity(intent);
            }
        }
    }

    public static void sendData(Socket socket, String name) throws IOException {
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

    public static void getData(Socket socket, String deviceName) throws IOException {

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

    public static void bluetoothSendImage(Intent data, Context context, BluetoothSocket socket) {
        Uri uri = data.getData();

        try {
            if (socket == null) {
                Log.d("p2p_log", "Can't send: socket is null");
                return;
            }
            OutputStream stream = socket.getOutputStream();
            ContentResolver cr = context.getContentResolver();
            InputStream is = null;
            try {
                is = cr.openInputStream(Uri.parse(uri.toString()));
            }
            catch (FileNotFoundException e) {
                Log.e("p2p_log", e.toString());
            }
            bluetoothCopyFile(is, stream);
            Log.d("p2p_log", "Client: Data written");
        }
        catch (IOException e) {
            Toast.makeText(context, "Failed to send file: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            Log.e("p2p_log", e.getMessage());
        }

        Log.d("p2p_log", "Sent image");
    }

    public static int bluetoothCopyFile(InputStream inputStream, OutputStream outputStream) throws IOException {
        byte buf[] = new byte[16384];
        int size = 0;

        int len = inputStream.read(buf);
        while (len > 0) {
            size += len;
            outputStream.write(buf, 0, len);
            len = inputStream.read(buf);
            Log.d("p2p_log", "In while loop");
        }

        inputStream.close();
        outputStream.close();

        return size;
    }

    public static void bluetoothGetImage(BluetoothSocket socket, Context context) throws IOException {
        final File f = new File(Environment.getExternalStorageDirectory() + "/"
                + context.getPackageName() + "/wifip2pshared-" + System.currentTimeMillis()
                + ".jpg");

        File dirs = new File(f.getParent());
        if (!dirs.exists())
            dirs.mkdirs();
        f.createNewFile();

        Log.d("p2p_log", "Trying to read");

        if (f.getAbsoluteFile() != null) {
            if (bluetoothCopyFile(socket.getInputStream(), new FileOutputStream(f)) > 0) {
                Intent intent = new Intent();
                intent.setAction(android.content.Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse("file://" + f.getAbsolutePath()), "image/*");
                context.startActivity(intent);
            }
        }
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
