package com.fedex.fedexble;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.format.Time;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.kontakt.sdk.android.ble.connection.OnServiceReadyListener;
import com.kontakt.sdk.android.ble.manager.ProximityManager;
import com.kontakt.sdk.android.ble.manager.ProximityManagerFactory;
import com.kontakt.sdk.android.ble.manager.listeners.EddystoneListener;
import com.kontakt.sdk.android.ble.manager.listeners.IBeaconListener;
import com.kontakt.sdk.android.ble.manager.listeners.simple.SimpleEddystoneListener;
import com.kontakt.sdk.android.ble.manager.listeners.simple.SimpleIBeaconListener;
import com.kontakt.sdk.android.cloud.KontaktCloud;
import com.kontakt.sdk.android.cloud.KontaktCloudFactory;
import com.kontakt.sdk.android.cloud.api.ProximitiesApi;
import com.kontakt.sdk.android.common.KontaktSDK;
import com.kontakt.sdk.android.common.profile.IBeaconDevice;
import com.kontakt.sdk.android.common.profile.IBeaconRegion;
import com.kontakt.sdk.android.common.profile.IEddystoneDevice;
import com.kontakt.sdk.android.common.profile.IEddystoneNamespace;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class MainActivity extends AppCompatActivity {

    BluetoothManager btManager;
    BluetoothAdapter btAdapter;
    BluetoothLeScanner btScanner;
    Button ScanningButton;
    TextView peripheralTextView;
    private final static int REQUEST_ENABLE_BT = 1;
    public HashMap<String, Beacon> beaconResult;
    public TreeMap<String, Beacon> sorted;
    private long tStart;
    private long tEnd;
    private int beaconCounter = 0;

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
        sorted = new TreeMap<>(new BeaconComparator(beaconResult));

        if (btAdapter != null && !btAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent,REQUEST_ENABLE_BT);
        }

        // Make sure we have access coarse location enabled, if not, prompt the user to enable it
        //if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
//            builder.setPositiveButton(android.R.string.ok, null);
//            builder.show();
        //}
    }

    // Device scan callback.
    private ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            /////////////////////////////////////// Timer //////////////////////////
            tEnd = System.currentTimeMillis();
            long tDelta = tEnd - tStart;
            double elapsedSeconds = tDelta / 1000.0;

            //////////////////////////////////////// if past 10 sec ///////////////////
            if(elapsedSeconds > 30) {
                stopScanning();
            }

            if( result.getScanRecord().getManufacturerSpecificData(321) != null) {
                peripheralTextView.append("Mac : " + result.getDevice().getAddress() + "  | RSSI: " + result.getRssi() + " | Status: Found" +"\n" );
                if(beaconResult.containsKey(result.getDevice().getAddress())) {
                    beaconResult.remove(result.getDevice().getAddress());
                    peripheralTextView.append("Mac : " + result.getDevice().getAddress() + "  | RSSI: " + result.getRssi() + " | Status: Updating" +"\n" );
                    //Log.d("BLE Data",  " Updating " + " MAC : " + result.getDevice().getAddress());
                }
                beaconResult.put(result.getDevice().getAddress(), new Beacon(result.getDevice().getAddress(), result.getRssi(), 0));
                Log.d("result", beaconResult.get(result.getDevice().getAddress()).toString());

                // auto scroll for text view
                final int scrollAmount = peripheralTextView.getLayout().getLineTop(peripheralTextView.getLineCount()) - peripheralTextView.getHeight();
                // if there is no need to scroll, scrollAmount will be <=0
                if (scrollAmount > 0)
                    peripheralTextView.scrollTo(0, scrollAmount);
            }
        }
    };

    public void startScanning() {
        System.out.println("start scanning...");
        peripheralTextView.setText("");
        peripheralTextView.append("Start Scanning\n");
        ScanningButton.setText("Scanning...");
        ScanningButton.setEnabled(false);
        tStart = System.currentTimeMillis();
        beaconCounter = 0;
        beaconResult.clear();
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                btScanner.startScan(leScanCallback);
            }
        });
    }

    public void stopScanning() {
        Log.d("scanResult","stop scanning...");
        sorted.clear();
        sorted.putAll(beaconResult);
        Log.d("scanResult", "There are " + sorted.size() + " beacons found");
        for(Map.Entry<String,Beacon> entry: sorted.entrySet() ) {
            Log.d("scanResult", entry.getValue().toString());
        }

        peripheralTextView.append("Scan Stopped\n");
        ScanningButton.setText("Start Scan");
        ScanningButton.setEnabled(true);
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                btScanner.stopScan(leScanCallback);
            }
        });
    }
}

