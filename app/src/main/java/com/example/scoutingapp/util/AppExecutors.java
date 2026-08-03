package com.example.scoutingapp.util;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Simple shared background executor + main-thread dispatcher.
 * Used everywhere the original Kotlin code used `suspend fun` / coroutines,
 * since plain threads are simpler and sufficient for this app's needs.
 */
public final class AppExecutors {

    private static final ExecutorService BACKGROUND = Executors.newCachedThreadPool();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private AppExecutors() {}

    public static void runBackground(Runnable task) {
        BACKGROUND.execute(task);
    }

    public static void runOnMain(Runnable task) {
        MAIN.post(task);
    }
}
