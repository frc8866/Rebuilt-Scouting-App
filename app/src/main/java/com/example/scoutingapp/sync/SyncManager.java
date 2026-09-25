package com.example.scoutingapp.sync;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.util.Log;

import com.example.scoutingapp.data.config.ScoutPosition;
import com.example.scoutingapp.data.repository.ScheduleCache;
import com.example.scoutingapp.data.scout.PendingUploadStore;
import com.example.scoutingapp.data.supabase.PostgrestClient;
import com.example.scoutingapp.domain.TableResolver;
import com.example.scoutingapp.util.AppExecutors;
import com.example.scoutingapp.util.Callback;

import org.json.JSONObject;

import java.util.List;

/**
 * Drains PendingUploadStore to Supabase.
 *
 * Auto-triggers whenever the device gains a validated Wi-Fi connection - scouting itself
 * never waits on this (see PendingUploadStore / ScoutViewModel.submitData), it only decides
 * when queued matches actually leave the device. A manual "Sync Now" action is also exposed
 * for peace of mind and works over any connection type, not just Wi-Fi.
 */
public final class SyncManager {

    private static final String TAG = "SyncManager";

    private static final PostgrestClient client = new PostgrestClient();
    private static volatile boolean syncInProgress = false;
    private static ConnectivityManager.NetworkCallback wifiCallback;

    private SyncManager() {}

    /** Call once (from Application.onCreate). Registers a listener that flushes the pending
     *  upload queue automatically whenever a validated Wi-Fi connection becomes available. */
    public static synchronized void registerAutoSync(Context context) {
        if (wifiCallback != null) return; // already registered
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return;

        NetworkRequest request = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                .build();

        wifiCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                Log.d(TAG, "Wi-Fi connected - flushing pending match uploads");
                flushNow(null);
            }
        };

        try {
            cm.registerNetworkCallback(request, wifiCallback);
        } catch (Exception e) {
            Log.e(TAG, "Failed to register Wi-Fi auto-sync callback: " + e.getMessage());
        }
    }

    public static boolean hasAnyConnection(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        Network network = cm.getActiveNetwork();
        if (network == null) return false;
        NetworkCapabilities caps = cm.getNetworkCapabilities(network);
        return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    public static final class SyncResult {
        public final int uploaded;
        public final int remaining;
        public final String error;

        public SyncResult(int uploaded, int remaining, String error) {
            this.uploaded = uploaded;
            this.remaining = remaining;
            this.error = error;
        }
    }

    /**
     * Attempts to upload every queued match, in submission order, stopping at the first
     * failure (treated as "connection dropped / never had one"). Safe to call often and
     * opportunistically - a no-op if a flush is already running or the queue is empty.
     * callback (may be null) is delivered on the main thread.
     */
    public static void flushNow(Callback<SyncResult> callback) {
        if (syncInProgress) {
            if (callback != null) {
                AppExecutors.runOnMain(() -> callback.onSuccess(new SyncResult(0, PendingUploadStore.size(), null)));
            }
            return;
        }
        List<PendingUploadStore.PendingUpload> queued = PendingUploadStore.getAll();
        if (queued.isEmpty()) {
            if (callback != null) AppExecutors.runOnMain(() -> callback.onSuccess(new SyncResult(0, 0, null)));
            return;
        }

        syncInProgress = true;
        AppExecutors.runBackground(() -> {
            int uploaded = 0;
            String lastError = null;
            for (PendingUploadStore.PendingUpload item : queued) {
                try {
                    client.insert(item.tableName, new JSONObject(item.payloadJson));
                    PendingUploadStore.removeById(item.id);
                    // We just proved we have connectivity, so it's safe to force a fresh
                    // authoritative re-fetch of scouted IDs next time they're needed.
                    ScheduleCache.invalidateScoutedIds(item.competitionKey, positionFromTableString(item.scoutingPosition));
                    uploaded++;
                } catch (Exception e) {
                    lastError = e.getMessage();
                    Log.w(TAG, "Upload failed for match " + item.matchNumber + ": " + e.getMessage());
                    break; // assume connectivity dropped again; leave the rest queued for next trigger
                }
            }
            syncInProgress = false;

            int finalUploaded = uploaded;
            int remaining = PendingUploadStore.size();
            String finalError = lastError;
            if (callback != null) {
                AppExecutors.runOnMain(() -> callback.onSuccess(new SyncResult(finalUploaded, remaining, finalError)));
            }
        });
    }

    private static ScoutPosition positionFromTableString(String posStr) {
        for (ScoutPosition p : ScoutPosition.values()) {
            if (TableResolver.positionString(p).equals(posStr)) return p;
        }
        return ScoutPosition.RED_1; // unreachable in practice; invalidation is best-effort anyway
    }
}
