package com.releasebird.releasebird_sdk;

import android.app.Activity;

import com.releasebird.releasebird_sdk.callbacks.GetActivityCallback;

import java.lang.reflect.Field;
import java.util.Map;


class ActivityUtil {
    public static Activity getCurrentActivity() {
        GetActivityCallback activityCallback =  ReleasebirdConfig.getInstance().getGetActivityCallback();
        if(activityCallback != null) {
            return activityCallback.getActivity();
        }
        // Preferred: the activity the SDK saw resume. The reflection below only covers the time
        // before the first resume after init (hidden API – may stop working on future Android versions).
        Activity tracked = ReleasebirdActivityManager.getInstance().getCurrentActivity();
        if (tracked != null) {
            return tracked;
        }
        try {
            Class activityThreadClass = Class.forName("android.app.ActivityThread");
            Object activityThread = activityThreadClass.getMethod("currentActivityThread").invoke(null);
            Field activitiesField = activityThreadClass.getDeclaredField("mActivities");
            activitiesField.setAccessible(true);
            Map<Object, Object> activities = (Map<Object, Object>) activitiesField.get(activityThread);
            if (activities == null)
                return null;
            for (Object activityRecord : activities.values()) {
                Class activityRecordClass = activityRecord.getClass();
                Field pausedField = activityRecordClass.getDeclaredField("paused");
                pausedField.setAccessible(true);
                if (!pausedField.getBoolean(activityRecord)) {
                    Field activityField = activityRecordClass.getDeclaredField("activity");
                    activityField.setAccessible(true);
                    Activity activity = (Activity) activityField.get(activityRecord);
                    return activity;
                }
            }
        } catch (Exception e) {
        }

        return null;
    }
}
