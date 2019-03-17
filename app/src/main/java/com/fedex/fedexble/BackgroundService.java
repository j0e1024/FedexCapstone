package com.fedex.fedexble;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class BackgroundService extends Service {
    private static final String TAG = "BackgroundService";

    public BackgroundService() {

    }

    @Override
    public void onCreate() {

        super.onCreate();
    }

    @Override
    public void onDestroy() {

        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent!=null){
            String command = intent.getStringExtra("command");
            if(command!=null){
                // compare string
                if(command.equals("start")){
                    PrintThread thread = new PrintThread();
                    thread.start();
                }
            }
        }
        /////////////////// Service Should be Here ///////////////////////////////


        return super.onStartCommand(intent, flags, startId);
    }

    // inner class
    class PrintThread extends Thread{
        public void run(){
            for(int i=0; i < 100; i++){
                Log.d(TAG, "#" + i + " 서비스에서 반복됨");
                try{
                    Thread.sleep(500);
                }catch(Exception e){
                    e.printStackTrace();
                }

            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }


}
