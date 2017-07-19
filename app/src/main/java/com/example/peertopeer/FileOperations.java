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
    public static void sendImage(int requestCode, int resultCode, Intent data, Context context, Socket socket) {
        Uri uri = data.getData();
        Log.d("p2p_log", "Intent----------- " + uri);

        try {
            if (socket == null) {
                Log.d("p2p_log", "mCurrentSocket is null");
            }
            OutputStream stream = socket.getOutputStream();
            ContentResolver cr = context.getContentResolver();
            InputStream is = null;
            try {
                is = cr.openInputStream(Uri.parse(uri.toString()));
            }
            catch (FileNotFoundException e) {
                Log.d("p2p_log", e.toString());
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

    public static boolean copyFile(InputStream inputStream, OutputStream outputStream) {
        byte buf[] = new byte[1024];
        int len;
        try {
            while ((len = inputStream.read(buf)) != -1) {
                outputStream.write(buf, 0, len);

            }
            outputStream.close();
            inputStream.close();
        }
        catch (IOException e) {
            Log.d("p2p_log", e.toString());
            return false;
        }
        return true;
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
