package com.fedex.fedexble;

public class Beacon {
    public String macAddr;
    public int RSSI;
    public int batteryLevel;

    public Beacon(String macAddr, int RSSI,  int batteryLevel) {
        this.macAddr = macAddr;
        this.RSSI = RSSI;
        this.batteryLevel = batteryLevel;
    }

    @Override
    public String toString() {
        return "MAC: " + macAddr + " RSSI: " + RSSI + " Battery Level: " + batteryLevel;
    }
}
