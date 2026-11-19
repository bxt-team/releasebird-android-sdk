package com.releasebird.releasebird_sdk;

import android.content.Context;
import android.content.SharedPreferences;

import com.releasebird.releasebird_sdk.callbacks.GetActivityCallback;

import org.json.JSONException;
import org.json.JSONObject;

public class ReleasebirdConfig {

    private static ReleasebirdConfig instance;

    private String apiKey;

    private String apiUrl = "https://api.releasebird.com/papi";
    //private String apiUrl = "http://10.0.2.2:8040/papi";

    private String widgetContentUrl = "https://wcontent.releasebird.com";
    //private String widgetContentUrl = "http://10.0.2.2:4001";

    private String chatBubble;

    private String launcherPosition;

    private String launcherColor;

    private int widgetX;

    private int widgetY;
    private boolean settingsLoaded;

    private static final String PREFS_NAME = "releasebird";

    private static final String RBIRD_STATE_KEY = "rbird_state";

    private GetActivityCallback getActivityCallback;

    private ReleasebirdConfig() {

    }

    public static ReleasebirdConfig getInstance() {
        if (instance == null) {
            instance = new ReleasebirdConfig();
        }
        return instance;
    }

    public boolean isSettingsLoaded() {
        return settingsLoaded;
    }

    public void setSettingsLoaded(boolean settingsLoaded) {
        this.settingsLoaded = settingsLoaded;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public GetActivityCallback getGetActivityCallback() {
        return getActivityCallback;
    }

    public void setGetActivityCallback(GetActivityCallback getActivityCallback) {
        this.getActivityCallback = getActivityCallback;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public String getWidgetContentUrl() {
        return widgetContentUrl;
    }

    public void setWidgetContentUrl(String widgetContentUrl) {
        this.widgetContentUrl = widgetContentUrl;
    }

    public String getChatBubble() {
        return chatBubble;
    }

    public void setChatBubble(String chatBubble) {
        this.chatBubble = chatBubble;
    }

    public int getWidgetX() {
        return widgetX;
    }

    public void setWidgetX(int widgetX) {
        this.widgetX = widgetX;
    }

    public int getWidgetY() {
        return widgetY;
    }

    public void setWidgetY(int widgetY) {
        this.widgetY = widgetY;
    }

    public String getLauncherPosition() {
        return launcherPosition;
    }

    public void setLauncherPosition(String launcherPosition) {
        this.launcherPosition = launcherPosition;
    }

    public String getLauncherColor() {
        return launcherColor;
    }

    public void setLauncherColor(String launcherColor) {
        this.launcherColor = launcherColor;
    }

    public JSONObject getRbirdState(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String rbirdStateString = sharedPreferences.getString(RBIRD_STATE_KEY, null);

        if (rbirdStateString != null) {
            try {
                return new JSONObject(rbirdStateString);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return null;
    }
}
