package com.fedex.fedexble;

import java.util.Comparator;
import java.util.Map;

public class BeaconComparator implements Comparator<String> {
    Map<String, Beacon> base;

    public BeaconComparator(Map<String, Beacon> base) {
        this.base = base;
    }

    @Override
    public int compare(String b1, String b2) {
        return base.get(b1).RSSI < base.get(b2).RSSI ? base.get(b1).RSSI : base.get(b2).RSSI;
    }
}
