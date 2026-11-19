package com.releasebird.releasebird_sdk;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PingService extends Service {

    private static final String TAG = "MyScheduledService";
    private ScheduledExecutorService scheduler;

    @Override
    public void onCreate() {
        super.onCreate();
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
               ApiManager apiManager = new ApiManager(getApplicationContext());
               apiManager.sendPingRequest();
            }
        }, 0, 30, TimeUnit.SECONDS);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}