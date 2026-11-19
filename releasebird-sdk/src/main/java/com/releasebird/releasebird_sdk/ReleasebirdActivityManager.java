package com.releasebird.releasebird_sdk;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.releasebird.releasebird_sdk.callbacks.UnreadCountCallback;

import java.lang.ref.WeakReference;

/**
 * Follows the host app's activities: remembers the one in front, attaches the floating button to
 * it and refreshes the unread badge. Registered exactly once per process.
 */
public class ReleasebirdActivityManager {
    private static final String TAG = "RELEASEBIRD";
    private static ReleasebirdActivityManager instance;
    private Application application;
    private Application.ActivityLifecycleCallbacks callbacks;
    private WeakReference<Activity> currentActivity = new WeakReference<>(null);

    private ReleasebirdActivityManager() {
    }

    public static synchronized ReleasebirdActivityManager getInstance() {
        if (instance == null) {
            instance = new ReleasebirdActivityManager();
        }
        return instance;
    }

    /** The activity currently in front, or {@code null} if the SDK has not seen one resume yet. */
    Activity getCurrentActivity() {
        Activity activity = currentActivity.get();
        return activity != null && !activity.isFinishing() && !activity.isDestroyed() ? activity : null;
    }

    public void showFrame(Activity activity) {
        if (activity == null) {
            return;
        }
        try {
            activity.startActivity(new Intent(activity, ReleasebirdMainActivity.class));
        } catch (Exception exp) {
            Log.e(TAG, "Could not open the widget: " + exp.getMessage());
        }
    }

    public synchronized void start(Application application) {
        if (application == null || callbacks != null) {
            // already registered – a second init must not double every request
            return;
        }
        this.application = application;
        final ApiManager apiManager = new ApiManager(application);
        callbacks = new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle bundle) {
            }

            @Override
            public void onActivityStarted(@NonNull Activity activity) {
            }

            @Override
            public void onActivityResumed(@NonNull Activity activity) {
                currentActivity = new WeakReference<>(activity);
                if (!ReleasebirdConfig.getInstance().isSettingsLoaded()) {
                    return;
                }
                ReleasebirdButton.getInstance().addLayoutToActivity(activity);
                refreshUnread(apiManager);
            }

            @Override
            public void onActivityPaused(@NonNull Activity activity) {
            }

            @Override
            public void onActivityStopped(@NonNull Activity activity) {
                ReleasebirdButton.getInstance().setVisible();
            }

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle bundle) {
            }

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {
                if (currentActivity.get() == activity) {
                    currentActivity = new WeakReference<>(null);
                }
            }
        };
        application.registerActivityLifecycleCallbacks(callbacks);
    }

    private void refreshUnread(ApiManager apiManager) {
        apiManager.getUnreadCount(new UnreadCountCallback() {
            @Override
            public void onSuccess(int messageCount) {
                ReleasebirdButton.getInstance().changeBadge("" + messageCount);
            }

            @Override
            public void onError(String errorMessage) {
                Log.d(TAG, "Unread count not available: " + errorMessage);
            }
        });
    }

    public synchronized void stop() {
        if (this.application != null && callbacks != null) {
            this.application.unregisterActivityLifecycleCallbacks(callbacks);
            callbacks = null;
        }
    }
}
