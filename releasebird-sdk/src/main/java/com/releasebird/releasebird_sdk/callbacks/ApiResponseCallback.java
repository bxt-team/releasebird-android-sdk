package com.releasebird.releasebird_sdk.callbacks;

import org.json.JSONObject;

public interface ApiResponseCallback {
    void onSuccess(JSONObject response);
    void onError(Exception e);
}
