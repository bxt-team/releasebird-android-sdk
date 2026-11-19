package com.releasebird.releasebird_android;

import android.app.Application;

import com.releasebird.releasebird_sdk.Releasebird;

import java.util.HashMap;
import java.util.Map;

public class MainApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Releasebird.init("1cad2c1b6d7842fd937469ce3ac42ba2", this, true);

        Map<String, String> companyMap = new HashMap<>();
        companyMap.put("externalId", "1234");
        companyMap.put("company_name", "Example company");

        // Erstellen des Benutzer-Dictionarys
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("firstname", "John");
        userMap.put("lastname", "Doe");
        userMap.put("email", "johndoe@example.com");
        userMap.put("external_user_id", "3456");
        userMap.put("company", companyMap);

        Releasebird.getInstance().identify(userMap);

    }
}
