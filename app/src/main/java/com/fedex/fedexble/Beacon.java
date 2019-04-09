package com.fedex.fedexble;

import android.util.Log;

import java.util.Comparator;

public class Beacon {
    public String macAddr;
    public int RSSI;

    public Beacon(String macAddr, int RSSI) {
        this.macAddr = macAddr;
        this.RSSI = RSSI;
    }

    @Override
    public String toString() {
        return "MAC: " + macAddr + " RSSI: " + RSSI;
    }

    public static Comparator<Beacon> BeaconComparator = new Comparator<Beacon>() {

        public int compare(Beacon b1, Beacon b2) {
            int calc = Integer.compare(b2.RSSI, b1.RSSI);
            Log.d("compare", "Comparing " + b1 + " with " + b2);
            Log.d("compare", "Result: " + Integer.toString(calc));
            return calc;
        }
    };
}
