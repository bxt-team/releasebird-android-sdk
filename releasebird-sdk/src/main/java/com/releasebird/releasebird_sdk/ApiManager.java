package com.releasebird.releasebird_sdk;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import com.releasebird.releasebird_sdk.callbacks.ApiResponseCallback;
import com.releasebird.releasebird_sdk.callbacks.UnreadCountCallback;

import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import java.util.TimeZone;

public class ApiManager {

    private static final String TAG = "ApiManager";

    private static final String PREFS_NAME = "releasebird";

    private static Context context;

    /** Connect and read timeout for every call – a hanging request must not pile up threads. */
    private static final int TIMEOUT_MS = 15000;

    public ApiManager(Context context) {
        // application context only: this field is static
        ApiManager.context = context.getApplicationContext();
    }

    private static HttpURLConnection open(String urlString, String method) throws java.io.IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(urlString).openConnection();
        connection.setConnectTimeout(TIMEOUT_MS);
        connection.setReadTimeout(TIMEOUT_MS);
        connection.setRequestMethod(method);
        connection.setRequestProperty("Content-Type", "application/json");
        if ("POST".equals(method)) {
            connection.setDoOutput(true);
        }
        return connection;
    }

    public void sendIdentifyCall(String anonymousIdentifier, JSONObject stateIdentify, String hash) {
        new SendIdentifyTask(anonymousIdentifier, stateIdentify, hash).execute();
    }

    public void fetchWidgetSettingsFromAPI(ApiResponseCallback callback) {
        new FetchWidgetSettingsTask(callback).execute();
    }

    public void sendPingRequest() {
        new SendPingRequestTask().execute();
    }

    public void getUnreadCount(final UnreadCountCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject rbirdState = ReleasebirdConfig.getInstance().getRbirdState(context);
                    AiValueManager aiValueManager = new AiValueManager(context.getApplicationContext());
                    String urlString = String.format("%s/ewidget/unread", ReleasebirdConfig.getInstance().getApiUrl());
                    HttpURLConnection urlConnection = open(urlString, "GET");
                    urlConnection.setRequestProperty("apiKey", ReleasebirdConfig.getInstance().getApiKey());
                    if (rbirdState != null && rbirdState.optString("people", null) != null) {
                        urlConnection.setRequestProperty("peopleId", rbirdState.optString("people"));
                    }
                    urlConnection.setRequestProperty("ai", aiValueManager.getAIValue());

                    int responseCode = urlConnection.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                        BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                        String inputLine;
                        StringBuilder response = new StringBuilder();

                        while ((inputLine = in.readLine()) != null) {
                            response.append(inputLine);
                        }
                        in.close();

                        try {
                            JSONObject jsonResponse = new JSONObject(response.toString());
                            int count = jsonResponse.getInt("messageCount");
                            callback.onSuccess(count);
                        } catch (JSONException e) {
                            Log.e(TAG, "Parsing exception: " + e.getMessage());
                            callback.onError("Parsing exception: " + e.getMessage());
                        }
                    } else {
                        Log.e(TAG, "HTTP Error: " + responseCode);
                        callback.onError("HTTP Error: " + responseCode);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error: " + e.getMessage());
                    callback.onError("Error: " + e.getMessage());
                }
            }
        }).start();
    }


    private static class FetchWidgetSettingsTask extends AsyncTask<Void, Void, JSONObject> {
        private final String api = ReleasebirdConfig.getInstance().getApiUrl();
        private final String apiKey = ReleasebirdConfig.getInstance().getApiKey();
        private final ApiResponseCallback callback;
        private Exception exception;

        FetchWidgetSettingsTask(ApiResponseCallback callback) {
            this.callback = callback;
        }

        @Override
        protected JSONObject doInBackground(Void... voids) {
            String urlString = api + "/ewidget";
            try {
                HttpURLConnection connection = open(urlString, "GET");
                connection.setRequestProperty("apiKey", apiKey);

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String inputLine;
                    StringBuilder response = new StringBuilder();

                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();

                    return new JSONObject(response.toString());
                } else {
                    Log.e(TAG, "HTTP Error: " + responseCode);
                }
            } catch (Exception e) {
                this.exception = e;
                Log.e(TAG, "Error: " + e.getMessage(), e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(JSONObject jsonResponse) {
            if (callback != null) {
                if (exception != null) {
                    callback.onError(exception);
                } else if (jsonResponse != null) {
                    callback.onSuccess(jsonResponse);
                } else {
                    callback.onError(new Exception("Unknown error occurred"));
                }
            }
        }
    }

    private static class SendIdentifyTask extends AsyncTask<Void, Void, Void> {
        private final String anonymousIdentifier;
        private final JSONObject stateIdentify;
        private final String hash;
        private Exception exception;

        SendIdentifyTask(String anonymousIdentifier, JSONObject stateIdentify, String hash) {
            this.anonymousIdentifier = anonymousIdentifier;
            this.stateIdentify = stateIdentify;
            this.hash = hash;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            String urlString = ReleasebirdConfig.getInstance().getApiUrl() + "/ewidget/identify";
            try {
                HttpURLConnection connection = open(urlString, "POST");
                connection.setRequestProperty("timezone", getCurrentTimeZone());
                connection.setRequestProperty("apiKey", ReleasebirdConfig.getInstance().getApiKey());

                JSONObject jsonBody = wrapDictionaryWithProperties(stateIdentify);
                jsonBody.put("anonymousIdentifier", anonymousIdentifier);

                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream()));
                writer.write(jsonBody.toString());
                writer.flush();
                writer.close();

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String inputLine;
                    StringBuilder response = new StringBuilder();

                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();

                    JSONObject responseDict = new JSONObject(response.toString());
                    if (responseDict.optBoolean("valid", false)) {
                        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPreferences.edit();

                        stateIdentify.put("people", responseDict.getString("peopleId"));
                        if (hash != null) {
                            stateIdentify.put("hash", hash);
                        }

                        editor.putString("rbird_state", stateIdentify.toString());
                        editor.apply();
                    }
                } else {
                    Log.e(TAG, "HTTP Error: " + responseCode);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error: " + e.getMessage(), e);
                this.exception = e;
            }
            return null;
        }

    }

    private static class SendPingRequestTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            String urlString = ReleasebirdConfig.getInstance().getApiUrl() + "/ewidget/ping";
            try {
                HttpURLConnection connection = open(urlString, "POST");
                connection.setRequestProperty("apiKey", ReleasebirdConfig.getInstance().getApiKey());

                AiValueManager aiValueManager = new AiValueManager(context.getApplicationContext());
                JSONObject rbirdState = ReleasebirdConfig.getInstance().getRbirdState(context);
                if (rbirdState == null) {
                    return null;
                }
                String people = rbirdState.optString("people", null);
                if (people == null) {
                    return null;
                }
                connection.setRequestProperty("peopleId", people);
                connection.setRequestProperty("ai", aiValueManager.getAIValue());

                JSONObject jsonBody = wrapDictionaryWithProperties(rbirdState);

                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream()));
                writer.write(jsonBody.toString());
                writer.flush();
                writer.close();

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String inputLine;
                    StringBuilder response = new StringBuilder();

                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();

                } else {
                    Log.e(TAG, "HTTP Error: " + responseCode);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error: " + e.getMessage(), e);
            }
            return null;
        }
    }

    private static String getCurrentTimeZone() {
        return TimeZone.getDefault().getID();
    }

    private static JSONObject wrapDictionaryWithProperties(JSONObject stateIdentify) throws JSONException {
        JSONObject wrapper = new JSONObject();
        wrapper.put("properties", stateIdentify);
        return wrapper;
    }

}
