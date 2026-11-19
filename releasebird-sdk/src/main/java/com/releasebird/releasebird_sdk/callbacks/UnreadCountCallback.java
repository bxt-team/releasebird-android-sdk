package com.releasebird.releasebird_sdk.callbacks;

public interface UnreadCountCallback {
    void onSuccess(int messageCount);
    void onError(String errorMessage);
}
