package com.example.scoutingapp.data.scout;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.gson.Gson;

/**
 * Java equivalent of the Kotlin MatchStateStore (DataStore + kotlinx.serialization.Json).
 * Uses SharedPreferences for persistence and Gson for (de)serializing SavedMatchState -
 * Gson is added here (rest of the app deliberately avoids it, using org.json) because this
 * class saves one fixed-shape object with nested maps/lists; reflection-based (de)serialization
 * is far simpler and less error-prone here than hand-writing JSON mapping for ~15 fields.
 */
public class MatchStateStore {

    private static final String PREFS_NAME = "pending_match_state";
    private static final String KEY = "saved_match_state";

    private final SharedPreferences prefs;
    private final Gson gson = new Gson();

    private final MutableLiveData<SavedMatchState> savedStateLiveData = new MutableLiveData<>();

    public MatchStateStore(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.registerOnSharedPreferenceChangeListener((sp, key) -> refresh());
        refresh();
    }

    private void refresh() {
        String json = prefs.getString(KEY, null);
        if (json == null) {
            savedStateLiveData.setValue(null);
            return;
        }
        try {
            savedStateLiveData.setValue(gson.fromJson(json, SavedMatchState.class));
        } catch (Exception e) {
            savedStateLiveData.setValue(null);
        }
    }

    /** Equivalent of Kotlin's savedStateFlow: Flow<SavedMatchState?> */
    public LiveData<SavedMatchState> getSavedStateLiveData() {
        return savedStateLiveData;
    }

    /** One-shot synchronous read, for callers that don't want to observe LiveData. */
    public SavedMatchState getSavedStateNow() {
        String json = prefs.getString(KEY, null);
        if (json == null) return null;
        try {
            return gson.fromJson(json, SavedMatchState.class);
        } catch (Exception e) {
            return null;
        }
    }

    public void save(SavedMatchState state) {
        prefs.edit().putString(KEY, gson.toJson(state)).apply();
    }

    public void clear() {
        prefs.edit().remove(KEY).apply();
    }
}
