package com.example.peertopeer;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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

    public static void copyFile(InputStream inputStream, OutputStream outputStream) throws IOException {
        byte buf[] = new byte[16384];

        int len = inputStream.read(buf);
        while (len > 0) {
            outputStream.write(buf, 0, len);
            len = inputStream.read(buf);
        }

        inputStream.close();
        outputStream.close();
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
            copyFile(socket.getInputStream(), new FileOutputStream(f));
            Intent intent = new Intent();
            intent.setAction(android.content.Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse("file://" + f.getAbsolutePath()), "image/*");
            context.startActivity(intent);
        }
    }
}
