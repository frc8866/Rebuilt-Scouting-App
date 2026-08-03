package com.example.scoutingapp.data.config;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

/**
 * Java equivalent of the Kotlin DeviceConfigStore, which used Jetpack DataStore + Flow.
 * Reimplemented with SharedPreferences (functionally equivalent persisted key-value store)
 * and LiveData in place of Flow, since DataStore's Java API is Flow-based under the hood
 * and adds no benefit here.
 */
public class DeviceConfigStore {

    private static final String PREFS_NAME = "device_config";

    private static final String KEY_COMPETITION_KEY = "competition_key";
    private static final String KEY_COMPETITION_NAME = "competition_name";
    private static final String KEY_POSITION = "scout_position";
    private static final String KEY_SCOUT_NAME = "scout_name";
    private static final String KEY_SETUP_COMPLETE = "setup_complete";

    private final SharedPreferences prefs;

    private final MutableLiveData<DeviceConfig> configLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> setupCompleteLiveData = new MutableLiveData<>();

    private final SharedPreferences.OnSharedPreferenceChangeListener listener =
            (sharedPreferences, key) -> refreshLiveData();

    public DeviceConfigStore(Context context) {
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.registerOnSharedPreferenceChangeListener(listener);
        refreshLiveData();
    }

    private void refreshLiveData() {
        configLiveData.setValue(buildConfig());
        setupCompleteLiveData.setValue(prefs.getBoolean(KEY_SETUP_COMPLETE, false));
    }

    private DeviceConfig buildConfig() {
        String competitionKey = prefs.getString(KEY_COMPETITION_KEY, "dalton");
        String competitionName = prefs.getString(KEY_COMPETITION_NAME, "Dalton");
        String positionName = prefs.getString(KEY_POSITION, ScoutPosition.RED_1.name());
        String scoutName = prefs.getString(KEY_SCOUT_NAME, "");
        boolean setupComplete = prefs.getBoolean(KEY_SETUP_COMPLETE, false);

        return new DeviceConfig(
                new Competition(competitionKey, competitionName),
                ScoutPosition.valueOf(positionName),
                setupComplete,
                scoutName
        );
    }

    /** Equivalent of Kotlin's configFlow: Flow<DeviceConfig> */
    public LiveData<DeviceConfig> getConfigLiveData() {
        return configLiveData;
    }

    /** Equivalent of Kotlin's setupCompleteFlow: Flow<Boolean> */
    public LiveData<Boolean> getSetupCompleteLiveData() {
        return setupCompleteLiveData;
    }

    public void completeSetup(ScoutPosition position) {
        prefs.edit()
                .putString(KEY_POSITION, position.name())
                .putBoolean(KEY_SETUP_COMPLETE, true)
                .apply();
    }

    public void updateCompetition(Competition competition) {
        prefs.edit()
                .putString(KEY_COMPETITION_KEY, competition.getKey())
                .putString(KEY_COMPETITION_NAME, competition.getDisplayName())
                .apply();
    }

    public void updateScoutName(String name) {
        prefs.edit().putString(KEY_SCOUT_NAME, name).apply();
    }

    public void updateScoutPosition(ScoutPosition position) {
        prefs.edit().putString(KEY_POSITION, position.name()).apply();
    }
}
