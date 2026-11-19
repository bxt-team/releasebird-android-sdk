package com.releasebird.releasebird_sdk;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.releasebird.releasebird_sdk.callbacks.ApiResponseCallback;

import org.json.JSONObject;

import java.util.Map;

/**
 * Entry point of the Releasebird SDK. Call {@link #init(String, Application)} once (typically in
 * {@code Application.onCreate}); everything else is a no-op with a log warning until then – the
 * SDK never throws into the host app.
 */
public class Releasebird {
    private static Releasebird instance;
    private static Application application;
    private static final String TAG = "RELEASEBIRD";
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private Releasebird() {
    }

    public static synchronized Releasebird getInstance() {
        if (instance == null) {
            instance = new Releasebird();
        }
        return instance;
    }

    private static boolean ready(String what) {
        if (application == null || ReleasebirdConfig.getInstance().getApiKey() == null) {
            Log.w(TAG, what + " ignored: call Releasebird.init(apiKey, application) first");
            return false;
        }
        return true;
    }

    /**
     * Identifies the current user. For verified identities pass the HMAC-SHA256 of the
     * {@code external_user_id}, computed on your server with the workspace's secret hash key.
     */
    public void identify(Map<String, Object> identify, String hash) {
        if (identify == null || !ready("identify")) {
            return;
        }
        try {
            Context context = application.getApplicationContext();
            new ApiManager(context).sendIdentifyCall(new AiValueManager(context).getAIValue(), new JSONObject(identify), hash);
        } catch (Exception e) {
            Log.e(TAG, "identify failed: " + e.getMessage());
        }
    }

    public void identify(Map<String, Object> identify) {
        identify(identify, null);
    }

    public static void init(String apiKey, Application application) {
        init(apiKey, application, true);
    }

    /** Forgets the identified user and starts a fresh anonymous session. */
    public void logout() {
        if (!ready("logout")) {
            return;
        }
        logout(application);
    }

    public void logout(Application application) {
        if (application == null) {
            Log.w(TAG, "logout ignored: no application");
            return;
        }
        try {
            SharedPreferences sharedPreferences = application.getApplicationContext().getSharedPreferences("releasebird", Context.MODE_PRIVATE);
            sharedPreferences.edit().remove("rbird_state").remove("ai").apply();
            new AiValueManager(application.getApplicationContext()).checkAndStoreAIValue();
            ReleasebirdButton.getInstance().changeBadge("0");
        } catch (Exception e) {
            Log.e(TAG, "logout failed: " + e.getMessage());
        }
    }

    public static void init(String apiKey, Application application, boolean showButton) {
        if (apiKey == null || apiKey.trim().isEmpty() || application == null) {
            Log.w(TAG, "init ignored: apiKey and application are required");
            return;
        }
        try {
            new AiValueManager(application.getApplicationContext()).checkAndStoreAIValue();
            ReleasebirdConfig.getInstance().setApiKey(apiKey.trim());
            Releasebird.application = application;
            // Track activities right away so the SDK knows where to show the button and the widget
            ReleasebirdActivityManager.getInstance().start(application);
            ReleasebirdButton.getInstance().setEnableButton(showButton);
            new ApiManager(application.getApplicationContext()).fetchWidgetSettingsFromAPI(new ApiResponseCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    try {
                        // Missing or unexpected fields must never crash the host app – fall back to defaults
                        ReleasebirdConfig config = ReleasebirdConfig.getInstance();
                        config.setWidgetX(response.optInt("spaceLeftRight", 20));
                        config.setWidgetY(response.optInt("spaceBottom", 20));
                        String bubble = response.isNull("chatBubbleUrl" + response.optString("launcher")) ? null
                                : response.optString("chatBubbleUrl" + response.optString("launcher"), null);
                        config.setChatBubble(bubble);
                        config.setLauncherPosition(response.optString("launcherPosition", "right"));
                        config.setLauncherColor(response.optString("launcherColor", null));
                        config.setSettingsLoaded(true);
                        ReleasebirdButton.getInstance().addLayoutToActivity(ActivityUtil.getCurrentActivity());
                        ReleasebirdButton.getInstance().showButton(showButton);
                    } catch (Exception e) {
                        Log.e(TAG, "Could not apply widget settings: " + e.getMessage());
                    }
                }

                @Override
                public void onError(Exception e) {
                    Log.e(TAG, "API error " + e.getMessage());
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "init failed: " + e.getMessage());
        }
    }

    /** Shows or hides the floating button. Hiding sticks until it is shown again. */
    public void showButton(boolean show) {
        showFeedbackButton(show);
    }

    public void showFeedbackButton(boolean show) {
        try {
            if (show) {
                ReleasebirdButton.getInstance().setEnableButton(true);
            }
            ReleasebirdButton.getInstance().setHiddenByApp(!show);
            ReleasebirdButton.getInstance().showButton(show);
        } catch (Exception ignore) {
        }
    }

    /** Opens the widget – for your own buttons or menu entries. */
    public void showWidget() {
        if (!ready("showWidget")) {
            return;
        }
        MAIN.post(new Runnable() {
            @Override
            public void run() {
                Activity activity = ActivityUtil.getCurrentActivity();
                if (activity == null) {
                    Log.w(TAG, "showWidget ignored: no visible activity");
                    return;
                }
                ReleasebirdActivityManager.getInstance().showFrame(activity);
            }
        });
    }
}
