package com.releasebird.releasebird_sdk;

import android.content.Context;
import android.content.Intent;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ProcessLifecycleOwner;

public class ForegroundServiceManager implements LifecycleObserver {

    private static final String TAG = "ForegroundServiceManager";
    private final Context context;

    public ForegroundServiceManager(Context context) {
        this.context = context.getApplicationContext();
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onEnterForeground() {
        Intent serviceIntent = new Intent(context, PingService.class);
        context.startService(serviceIntent);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onEnterBackground() {
        Intent serviceIntent = new Intent(context, PingService.class);
        context.stopService(serviceIntent);
    }
}