package com.example.peertopeer;

import java.util.Random;
import java.util.TreeMap;

public class GossipData {

    private static GossipData sGossipData;

    public static TreeMap<Integer, String> mData;

    public static GossipData get(String name) {
        if (sGossipData == null) {
            sGossipData = new GossipData(name);
        }
        return sGossipData;
    }

    private GossipData(String name) {
        mData = new TreeMap<>();

        Random random = new Random();
        for (int i = 0; i < 1; i++) {
            mData.put(random.nextInt(100) + 1, name);
        }
    }

    public TreeMap<Integer, String> getData() {
        return mData;
    }

    public void add(int n, String name) {
        mData.put(n, name);
    }
}
