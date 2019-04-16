package com.fedex.fedexble;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;

public class ButtonBubbleService extends Service {

    private View buttonBubbleView;
    private WindowManager windowManager;
    public ArrayList<Beacon> sorted;
    static NotificationCompat.Builder notification;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    @Override
    public void onCreate() {
        super.onCreate();
        sorted = new ArrayList<>();
        notification = new NotificationCompat.Builder(this);
        notification.setAutoCancel(true);
        buttonBubbleView = LayoutInflater.from(this).inflate(R.layout.button_bubble, null);
        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.RIGHT;
        params.x = 0;
        params.y = 100;
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        windowManager.addView(buttonBubbleView, params);
        ImageView bubbleImage = buttonBubbleView.findViewById(R.id.button_image);
        bubbleImage.setOnTouchListener(
                new View.OnTouchListener() {
                    private int initialX;
                    private int initialY;
                    private float touchX;
                    private float touchY;
                    private int lastAction;
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        if(event.getAction() == MotionEvent.ACTION_DOWN) {
                            initialX = params.x;
                            initialY = params.y;
                            touchX = event.getRawX();
                            touchY = event.getRawY();
                            lastAction = event.getAction();
                            return true;
                        }
                        if(event.getAction() == MotionEvent.ACTION_UP) {
                            if(lastAction == MotionEvent.ACTION_DOWN) {
                                if(sorted.size() > 0) {
                                    Log.d("BLE", "Currently Connected to : " + MainActivity.sorted.get(0).macAddr);
                                    Toast.makeText(getApplicationContext(), "Connected to : " + MainActivity.sorted.get(0).macAddr, Toast.LENGTH_SHORT).show();
                                }
                                MainActivity.startScanning();

//                                Button button = new Button(ButtonBubbleService.this);
//                                RelativeLayout layout = buttonBubbleView.findViewById(R.id.button_bubble);
//                                button.setOnClickListener(new View.OnClickListener() {
//                                    @Override
//                                    public void onClick(View view) {
//                                        stopSelf();
//
//                                    }
//                                });
                            }
                            lastAction = event.getAction();
                            return true;
                        }
//                        if(event.getAction() ==  MotionEvent.ACTION_MOVE) {
//                            params.x = initialX + (int) (event.getRawX() + touchX);
//                            params.y = initialY + (int) (event.getRawY() - touchY);
//                            windowManager.updateViewLayout(buttonBubbleView, params);
//                            lastAction = event.getAction();
//                            return true;
//                        }
                        return false;
                    }
                }
        );
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if( buttonBubbleView != null) {
            windowManager.removeView(buttonBubbleView);
        }
    }
}
