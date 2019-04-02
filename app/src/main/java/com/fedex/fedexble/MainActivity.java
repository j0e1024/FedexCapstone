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

import java.util.ArrayList;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    BluetoothManager btManager;
    BluetoothAdapter btAdapter;
    BluetoothLeScanner btScanner;
    Button ScanningButton;
    TextView peripheralTextView;
    private final static int REQUEST_ENABLE_BT = 1;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    public static final ScanResult[] beacons = new ScanResult[10];
    public static final int[] beaconIDs = new int[10];
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

            tEnd = System.currentTimeMillis();
            long tDelta = tEnd - tStart;
            double elapsedSeconds = tDelta / 1000.0;
            if(beaconCounter == 10  || elapsedSeconds > 10) {
                //Log.d("BLE Data",  "Result : " +  beaconIDs[0] + ", " +  beaconIDs[1] + ", " +  beaconIDs[2] );
                stopScanning();
            }

            if( result.getScanRecord().getManufacturerSpecificData(321) != null) {
                Log.d("BLE Data",  "RSSI : " +  result.getRssi() + " || Result : " + result);
                peripheralTextView.append("Device ID: " + result.getDevice() + "  | RSSI: " + result.getRssi() + "\n");
                // 여기에 같은거 다른 스트랭스 체크해서 넣어야됨
                if(!Arrays.asList(beaconIDs).contains(result.getDevice())) {
                    beacons[beaconCounter] = result;
                    //beaconIDs[beaconCounter] = result.getDevice();
                    for (int i = 0; i < beaconCounter; i++) {
                        if (beacons[i].getRssi() < beacons[i + 1].getRssi()) {
                            ScanResult temp = beacons[i];
                            int idTemp = beaconIDs[i];
                            beaconIDs[i] = beaconIDs[i + 1];
                            beacons[i] = beacons[i + 1];
                            beaconIDs[i + 1] = idTemp;
                            beacons[i + 1] = temp;
                        }
                    }
                    Toast.makeText(getApplicationContext(), "You have changed locations to: " + result.getDevice(), Toast.LENGTH_SHORT).show();
                    beaconCounter++;
                }
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
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                btScanner.startScan(leScanCallback);
            }
        });
    }

    public void stopScanning() {
        System.out.println("stop scanning...");
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

