package com.example.peertopeer;

import android.content.Context;

import java.util.HashSet;
import java.util.Random;

public class GossipData {

    private static GossipData sGossipData;

    public static HashSet<Integer> mData;

    public static GossipData get() {
        if (sGossipData == null) {
            sGossipData = new GossipData();
        }
        return sGossipData;
    }

    private GossipData() {
        mData = new HashSet<>();

        Random random = new Random();
        for (int i = 0; i < 1; i++) {
            mData.add(random.nextInt(100) + 1);
        }
    }

    public HashSet<Integer> getData() {
        return mData;
    }

    public void add(int n) {
        mData.add(n);
    }
}
