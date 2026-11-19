package com.releasebird.releasebird_sdk;

import android.content.Context;
import android.content.SharedPreferences;

import java.security.SecureRandom;

/**
 * The anonymous identifier of this installation. It is the only thing that protects an anonymous
 * visitor's conversations, so it comes from a cryptographically secure generator.
 */
public class AiValueManager {

    private static final String PREFS_NAME = "releasebird";
    private static final String AI_KEY = "ai";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final Context context;

    public AiValueManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public synchronized void checkAndStoreAIValue() {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (sharedPreferences.getString(AI_KEY, null) == null) {
            // commit: the value must be readable by the very next call
            sharedPreferences.edit().putString(AI_KEY, generateRandomString()).commit();
        }
    }

    public String getAIValue() {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String value = sharedPreferences.getString(AI_KEY, null);
        if (value == null) {
            checkAndStoreAIValue();
            value = sharedPreferences.getString(AI_KEY, null);
        }
        return value;
    }

    private String generateRandomString() {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        int length = 60;
        StringBuilder randomString = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            randomString.append(characters.charAt(RANDOM.nextInt(characters.length())));
        }
        return randomString.toString();
    }
}
