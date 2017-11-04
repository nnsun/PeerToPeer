package com.example.peertopeer;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.Payload;

import java.util.TreeMap;


public class FileOperations {

    public static void parseMessage(TreeMap<Integer, String> data, String message) {
        String[] elements = message.split("\n");
        for (String element : elements) {
            String[] datum = element.split(" ", 2);
            data.put(Integer.parseInt(datum[0]), datum[1]);
        }
    }

    public static void nearbySend(GoogleApiClient client, byte[] payload, String endpointId) {
        Payload data = Payload.fromBytes(payload);
        Nearby.Connections.sendPayload(client, endpointId, data);
    }
}
