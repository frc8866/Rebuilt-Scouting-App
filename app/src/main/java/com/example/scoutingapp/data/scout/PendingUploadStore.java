package com.example.scoutingapp.data.scout;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Durable, disk-backed queue of scouted-match submissions that have not been confirmed
 * uploaded to Supabase yet.
 *
 * This is the core of "offline-first" submission: ScoutViewModel.submitData() writes here
 * immediately (before attempting any network call), so a match is never lost even if the
 * device has no signal at all or the app is killed right after. SyncManager drains this
 * queue whenever connectivity comes back.
 *
 * Static "object"-style class, matching the ScheduleCache / PendingMatchStore convention
 * already used in this codebase.
 */
public final class PendingUploadStore {

    private static final String PREFS_NAME = "pending_uploads";
    private static final String KEY_ITEMS = "items";

    private static final Gson gson = new Gson();
    private static final Type LIST_TYPE = new TypeToken<List<PendingUpload>>() {}.getType();

    private static SharedPreferences prefs;
    private static final List<PendingUpload> items = new ArrayList<>();
    private static final MutableLiveData<Integer> countLiveData = new MutableLiveData<>(0);

    private PendingUploadStore() {}

    /** Call once (from Application.onCreate) before any add/remove/read calls. */
    public static synchronized void init(Context context) {
        if (prefs != null) return; // already initialized
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        items.clear();
        String json = prefs.getString(KEY_ITEMS, null);
        if (json != null) {
            try {
                List<PendingUpload> loaded = gson.fromJson(json, LIST_TYPE);
                if (loaded != null) items.addAll(loaded);
            } catch (Exception ignored) {
                // corrupted prefs entry; start clean rather than crash-looping
            }
        }
        countLiveData.postValue(items.size());
    }

    private static void persist() {
        if (prefs == null) return;
        prefs.edit().putString(KEY_ITEMS, gson.toJson(items)).apply();
        countLiveData.postValue(items.size());
    }

    public static synchronized void add(PendingUpload item) {
        items.add(item);
        persist();
    }

    public static synchronized void removeById(String id) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).id.equals(id)) {
                items.remove(i);
                break;
            }
        }
        persist();
    }

    public static synchronized List<PendingUpload> getAll() {
        return new ArrayList<>(items);
    }

    public static synchronized int size() {
        return items.size();
    }

    /** Match numbers already queued locally for this competition+position but not yet
     *  confirmed uploaded - used to keep "already scouted" checks correct while offline. */
    public static synchronized Set<Integer> getPendingMatchNumbers(String competitionKey, String scoutingPosition) {
        Set<Integer> result = new HashSet<>();
        for (PendingUpload u : items) {
            if (u.competitionKey.equals(competitionKey) && u.scoutingPosition.equals(scoutingPosition)) {
                result.add(u.matchNumber);
            }
        }
        return result;
    }

    public static LiveData<Integer> getCountLiveData() {
        return countLiveData;
    }

    /** One row queued for upload: the exact PostgREST insert body plus enough metadata to
     *  route it and to keep local "already scouted" checks correct while it's in flight. */
    public static final class PendingUpload {
        public String id;
        public String competitionKey;
        public String tableName;
        public int matchNumber;
        public String scoutingPosition;
        public String payloadJson;
        public long createdAtEpochMs;

        public PendingUpload() {
            // no-arg constructor for Gson deserialization
        }

        public PendingUpload(String id, String competitionKey, String tableName, int matchNumber,
                              String scoutingPosition, String payloadJson, long createdAtEpochMs) {
            this.id = id;
            this.competitionKey = competitionKey;
            this.tableName = tableName;
            this.matchNumber = matchNumber;
            this.scoutingPosition = scoutingPosition;
            this.payloadJson = payloadJson;
            this.createdAtEpochMs = createdAtEpochMs;
        }
    }
}
