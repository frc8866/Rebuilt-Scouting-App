package com.example.scoutingapp.util;

/** Generic async result callback, delivered on the main thread. Replaces suspend fun / Result<T>. */
public interface Callback<T> {
    void onSuccess(T result);
    void onError(Exception e);
}
