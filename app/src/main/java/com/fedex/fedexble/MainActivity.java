package com.fedex.fedexble;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
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

public class MainActivity extends AppCompatActivity {

    // Necessary API Objects
    KontaktCloud kontaktCloud;
    ProximitiesApi proximitiesApi;
    private ProximityManager proximityManager;

    public final static int PERMISSION_REQUEST_CODE = 1;

    // Repeating Task Handler
    Handler handler = new Handler();
    Runnable runnable;

    // Delay, in ms, for the repeating task
    int delay = 5000;

    // Dialog for User Interaction
    AlertDialog alertDialog;

    // List to Store Found Beacons
    private ArrayList<IBeaconDevice> beaconList = new ArrayList<IBeaconDevice>();

    // Variables to Store Current and Recent Locations
    private String currentLocation;
    private String lastFound, lastFound2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_main);

        // Necessary Location Permissions
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1001);
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1002);

        // Initialize API
        KontaktSDK.initialize("MPgFPqypKCZZcKxTAKrhsKoYykmKPQnP");
        kontaktCloud = KontaktCloudFactory.create();
        proximitiesApi = kontaktCloud.proximities();
        proximityManager = ProximityManagerFactory.create(this);
        proximityManager.setIBeaconListener(createIBeaconListener());
        proximityManager.setEddystoneListener(createEddystoneListener());

        // Start Scanning
//        startScanning();
        createEddystoneListener();
        createIBeaconListener();
        createIBeaconListener();

        // Repeating Handler Eevery "delay" seconds
//        handler.postDelayed(new Runnable() {
//            public void run() {
//                // print beacons to screen, this is not necessary but iseful for de-bugging
//                printBeacons(null);
//                // compute the logic based on currently stored beacon information
//                doLogic();
//                // wipe all current information and re-scan for distances
//                rescanForBeacons(null);
//
//                runnable = this;
//                handler.postDelayed(runnable, delay);
//            }
//        }, delay);

        // Initialize the AlertDialog (but do not show until necessary)
        alertDialog = new AlertDialog.Builder(MainActivity.this).create();
        alertDialog.setTitle("Switch Locations");
        alertDialog.setMessage("Sample Message");

//
//

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays( this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, PERMISSION_REQUEST_CODE);
        }
        else {
            showButtonBubble();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        startScanning();
    }

    @Override
    protected void onStop() {
        proximityManager.stopScanning();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        proximityManager.disconnect();
        proximityManager = null;
        super.onDestroy();
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



    public void startScanning() {
        proximityManager.connect(new OnServiceReadyListener() {
            @Override
            public void onServiceReady() {
                proximityManager.startScanning();
            }
        });
    }

    // every time a beacon is found, add to the list
    private IBeaconListener createIBeaconListener() {
        return new SimpleIBeaconListener() {
            @Override
            public void onIBeaconDiscovered(IBeaconDevice ibeacon, IBeaconRegion region) {
                beaconList.add(ibeacon);


//                System.out.println("All Currently Located Beacons:");
//                for (int i = 0; i < beaconList.size(); i++) {
//                    System.out.println("\t" + beaconList.get(i).getAddress() + " (" + lookUpBeaconName(beaconList.get(i).getAddress()) + "): " + beaconList.get(i).getDistance());
//                }
            }
        };
    }
    public void onServiceStartBtn(View v) {
        Intent intent = new Intent(getApplicationContext(), BackgroundService.class);
        intent.putExtra("command", "start");
        startService(intent); // run BackgroundService
    }
    // we do not use the Eddystone configuration, we use iBeacon
    // however it may be useful if EddyStone is needed and easily integrated
    private EddystoneListener createEddystoneListener() {
        return new SimpleEddystoneListener() {
            @Override
            public void onEddystoneDiscovered(IEddystoneDevice eddystone, IEddystoneNamespace namespace) {
                Log.i("Sample", "Eddystone discovered: " + eddystone.toString());
                System.out.println("Eddystone discovered: " + eddystone.toString());
            }
        };
    }


    // prints found beacon information to standard output, for debugging, sets some framing
    // information on the demonstration application screen
    public void printBeacons(View view) {

        System.out.println("\nALL CURRENTLY LOCATED BEACONS:");

        String closestName = "NONE FOUND";
        double closestDistance = -1;

        for (int i = 0; i < beaconList.size(); i++) {
            System.out.println("\t" + lookUpBeaconName(beaconList.get(i).getAddress()) + ": " +
                    beaconList.get(i).getDistance() + " TX: " + beaconList.get(i).getTxPower() +
                    " RSSI: " + beaconList.get(i).getRssi());

            if (closestDistance == -1 || beaconList.get(i).getDistance() < closestDistance) {
                closestName = lookUpBeaconName(beaconList.get(i).getAddress());
                closestDistance = beaconList.get(i).getDistance();
            }
        }
        System.out.println("Closest Beacon:");
        System.out.println("\t" + closestName + ": " + closestDistance);

        EditText textBox = (EditText) findViewById(R.id.editText5);
        textBox.setText(closestName);

    }

    // clear the beacon list and restart scanning for beacons
    public void rescanForBeacons(View view) {
        beaconList.clear();
        proximityManager.restartScanning();
    }

    // find the closest beacon and prompt if an appropriate location change was made
    public void doLogic() {
        lastFound2 = lastFound; // set the last found beacon (cycle t-1) to the second to last found beacon (cycle t-2)
        // find the beacon that is closest to the user for thiis cycle
        double closestDistance = -1;
        String closestName = "NONE FOUND";
        for (int i = 0; i < beaconList.size(); i++) {
            if (closestDistance == -1 || beaconList.get(i).getDistance() < closestDistance) {
                closestName = lookUpBeaconName(beaconList.get(i).getAddress());
                closestDistance = beaconList.get(i).getDistance();
            }
        }
        lastFound = closestName; // store the final last found beacon

        // finite state machine: reduces noise by requiring a change to be registered for teo consecutive cycles
        if (lastFound.equals(lastFound2) && !lastFound.equals(currentLocation)) {
            System.out.println("PROMPTING THE USER TO SWITCH LOCATIONS!!!");

            // prompt the user to change locations
            alertDialog.setMessage("It appears that you have moved locations.  Would you like to begin loading at: " + lastFound + "?");
            // if yes, set the current location to the new found and accepted location
            alertDialog.setButton(Dialog.BUTTON_POSITIVE, "Yes", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    currentLocation = lastFound;
                    EditText textBox = (EditText) findViewById(R.id.editText5);
                    textBox.setText(currentLocation);
                    Toast.makeText(getApplicationContext(), "You have changed locations to: " + currentLocation, Toast.LENGTH_SHORT).show();
                }
            });
            // if no, do nothing
            // TODO: remember this choice for future prompts
            alertDialog.setButton(Dialog.BUTTON_NEGATIVE, "No", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    Toast.makeText(getApplicationContext(), "You will remain at location: " + currentLocation, Toast.LENGTH_SHORT).show();
                }
            });
            alertDialog.show();
        }
    }

    public String lookUpBeaconName(String mac) {
        /*
            This function mocks what would be a database call to the FedEx production database,
            retrieving the gate that corresponds with the passed MAC address of a BLE beacon
            device.
         */
        switch (mac) {
            case "D1:80:8B:15:64:96":
                return "Gate A";
            case "EA:D4:ED:E9:5D:28":
                return "Gate B";
            case "C0:90:EB:0A:AC:C9":
                return "Gate C";
            default:
                System.out.println(mac);
                return "Unknown Beacon";
        }
    }
}

