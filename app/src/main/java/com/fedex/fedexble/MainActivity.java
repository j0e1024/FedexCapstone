package com.fedex.fedexble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.provider.Settings;
import android.net.Uri;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class MainActivity extends AppCompatActivity {

    BluetoothManager btManager;
    BluetoothAdapter btAdapter;
    static BluetoothLeScanner btScanner;
    static Button ScanningButton;
    static TextView peripheralTextView;
    private final static int REQUEST_ENABLE_BT = 1;
    public final static int PERMISSION_REQUEST_CODE = 1;
    public static HashMap<String, Beacon> beaconResult;
    public static ArrayList<Beacon> sorted;
    private static long tStart;
    private static long tEnd;
    //private int beaconCounter = 0;
    private static Lock lock;
    private static boolean done;

//    private static MainActivity mInstance= null;
//    protected MainActivity(){}
//    public static synchronized MainActivity getInstance() {
//        if(null == mInstance){
//            mInstance = new MainActivity();
//        }
//        return mInstance;
//    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        peripheralTextView = (TextView) findViewById(R.id.PeripheralTextView);
        peripheralTextView.setMovementMethod(new ScrollingMovementMethod());

        ScanningButton = (Button) findViewById(R.id.ScanButton);
        ScanningButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startScanning();
            }
        });


        btManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
        btAdapter = btManager.getAdapter();
        btScanner = btAdapter.getBluetoothLeScanner();
        beaconResult = new HashMap<>();
        sorted = new ArrayList<>();

        lock = new ReentrantLock();

        if (btAdapter != null && !btAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent,REQUEST_ENABLE_BT);
        }

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays( this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, PERMISSION_REQUEST_CODE);
        }
        else {
            showButtonBubble();
        }

        // Make sure we have access coarse location enabled, if not, prompt the user to enable it
        //if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
//            builder.setPositiveButton(android.R.string.ok, null);
//            builder.show();
        //}
    }
    protected  void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode,resultCode,data);

        if(requestCode == PERMISSION_REQUEST_CODE) {
            if(resultCode == RESULT_OK) {
                showButtonBubble();
            }
        }
    }
    public void showButtonBubble() {
        startService(new Intent(MainActivity.this, ButtonBubbleService.class));
    }

    // Device scan callback.
    private static ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            /////////////////////////////////////// Timer //////////////////////////
            tEnd = System.currentTimeMillis();
            long tDelta = tEnd - tStart;
            double elapsedSeconds = tDelta / 1000.0;

            //////////////////////////////////////// if past 10 sec ///////////////////
            if(elapsedSeconds > 15) {
                stopScanning();
            }
            if(done) return;

            if( result.getScanRecord().getManufacturerSpecificData(321) != null) {
                Beacon input = new Beacon(result.getDevice().getAddress(), result.getRssi());
                if(beaconResult.containsKey(result.getDevice().getAddress())) {
                    beaconResult.remove(result.getDevice().getAddress());
                    peripheralTextView.append(input + " | Status: Updating" +"\n" );
                } else {
                    peripheralTextView.append(input + " | Status: Found" +"\n" );
                }
                beaconResult.put(input.macAddr, input);
                Log.d("result", input.toString());

                // auto scroll for text view
                final int scrollAmount = peripheralTextView.getLayout().getLineTop(peripheralTextView.getLineCount()) - peripheralTextView.getHeight();
                // if there is no need to scroll, scrollAmount will be <=0
                if (scrollAmount > 0)
                    peripheralTextView.scrollTo(0, scrollAmount);
            }
        }
    };

    public static void startScanning() {
        Log.d("scanBegin", "start scanning...");
        done = false;
        peripheralTextView.setText("");
        peripheralTextView.append("Start Scanning\n");
        ScanningButton.setText("Scanning...");
        ScanningButton.setEnabled(false);
        tStart = System.currentTimeMillis();
        beaconResult.clear();
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                btScanner.startScan(leScanCallback);
            }
        });
    }

    public static void stopScanning() {
        if(done || !lock.tryLock()) {
            return;
        }
        done = true;
        lock.unlock();


        Log.d("scanResult","stop scanning...");
        sorted.clear();


        for(Map.Entry<String,Beacon> entry: beaconResult.entrySet() ) {
            sorted.add(entry.getValue());
        }
        Collections.sort(sorted, Beacon.BeaconComparator);

        Log.d("scanResult", "There are " + sorted.size() + " beacons found:");
        Log.d("scanResult", "" + sorted);
        if(sorted.size() != 0) {
            Log.d("scanResult", "The closest beacon is: " + sorted.get(0));
            ButtonBubbleService.showToast();
            peripheralTextView.append("Scan Stopped\n");
            peripheralTextView.append("Result: " + sorted.get(0) + "\n");
            ScanningButton.setText("Start Scan");
            ScanningButton.setEnabled(true);
        }
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                btScanner.stopScan(leScanCallback);
            }
        });
    }
}

