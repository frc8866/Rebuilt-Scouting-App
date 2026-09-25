package com.example.scoutingapp.data.repository;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.scoutingapp.data.config.ScoutPosition;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * In-memory + disk-backed cache shared by MatchRepository and DataRepository.
 *
 * Offline-first: init(Context) hydrates the in-memory maps from SharedPreferences at app
 * startup, so a previously-downloaded schedule and scouted-match lists are available
 * immediately with no network call. invalidateSchedule/invalidateScoutedIds/invalidateAll
 * only clear the in-memory copy (forcing the next read to hit the network) - the on-disk
 * copy is kept as an offline fallback (see getPersistedSchedule) so switching competitions
 * or restarting the app never throws away a schedule that was already downloaded.
 */
public class ScheduleCache {

    private ScheduleCache() {}

    private static final String PREFS_NAME = "schedule_cache";
    private static final String KEY_SCHEDULE_KEYS = "schedule_keys";
    private static final String KEY_SCOUTED_KEYS = "scouted_keys";
    private static final String SCHEDULE_PREFIX = "schedule_";
    private static final String SCOUTED_PREFIX = "scouted_";

    private static final Gson gson = new Gson();
    private static final Type SCHEDULE_TYPE = new TypeToken<List<QualMatch>>() {}.getType();
    private static final Type SCOUTED_TYPE = new TypeToken<HashSet<Integer>>() {}.getType();

    private static SharedPreferences prefs;

    private static final Map<String, List<QualMatch>> qualMatches = new HashMap<>();
    private static final Map<String, Set<Integer>> scoutedIds = new HashMap<>();

    // ── Init / disk hydration ────────────────────────────────────────────────

    /** Call once (from Application.onCreate) before any schedule/scouted-id lookups. */
    public static synchronized void init(Context context) {
        if (prefs != null) return; // already initialized
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        for (String key : prefs.getStringSet(KEY_SCHEDULE_KEYS, new HashSet<>())) {
            List<QualMatch> loaded = readSchedule(key);
            if (loaded != null) qualMatches.put(key, loaded);
        }
        for (String compoundKey : prefs.getStringSet(KEY_SCOUTED_KEYS, new HashSet<>())) {
            Set<Integer> loaded = readScouted(compoundKey);
            if (loaded != null) scoutedIds.put(compoundKey, loaded);
        }
    }

    private static List<QualMatch> readSchedule(String competitionKey) {
        if (prefs == null) return null;
        String json = prefs.getString(SCHEDULE_PREFIX + competitionKey, null);
        if (json == null) return null;
        try {
            return gson.fromJson(json, SCHEDULE_TYPE);
        } catch (Exception e) {
            return null;
        }
    }

    private static void writeSchedule(String competitionKey, List<QualMatch> matches) {
        if (prefs == null) return;
        Set<String> keys = new HashSet<>(prefs.getStringSet(KEY_SCHEDULE_KEYS, new HashSet<>()));
        keys.add(competitionKey);
        prefs.edit()
                .putString(SCHEDULE_PREFIX + competitionKey, gson.toJson(matches))
                .putStringSet(KEY_SCHEDULE_KEYS, keys)
                .apply();
    }

    private static Set<Integer> readScouted(String compoundKey) {
        if (prefs == null) return null;
        String json = prefs.getString(SCOUTED_PREFIX + compoundKey, null);
        if (json == null) return null;
        try {
            return gson.fromJson(json, SCOUTED_TYPE);
        } catch (Exception e) {
            return null;
        }
    }

    private static void writeScouted(String compoundKey, Set<Integer> ids) {
        if (prefs == null) return;
        Set<String> keys = new HashSet<>(prefs.getStringSet(KEY_SCOUTED_KEYS, new HashSet<>()));
        keys.add(compoundKey);
        prefs.edit()
                .putString(SCOUTED_PREFIX + compoundKey, gson.toJson(ids))
                .putStringSet(KEY_SCOUTED_KEYS, keys)
                .apply();
    }

    private static String scoutedKey(String competitionKey, ScoutPosition position) {
        return competitionKey + "|" + position.name();
    }

    // ── Quals schedule ───────────────────────────────────────────────────────

    public static synchronized List<QualMatch> getSchedule(String competitionKey) {
        return qualMatches.get(competitionKey);
    }

    /**
     * Disk-only lookup, for when a network refetch fails and nothing is in memory (e.g. the
     * app was killed and relaunched offline before a fresh fetch could happen). Does NOT
     * populate the in-memory map on its own - callers that use this as a fallback should
     * treat the result as "last known good" and can put() it back if they want it cached
     * for the rest of the session.
     */
    public static synchronized List<QualMatch> getPersistedSchedule(String competitionKey) {
        return readSchedule(competitionKey);
    }

    public static synchronized void putSchedule(String competitionKey, List<QualMatch> matches) {
        qualMatches.put(competitionKey, matches);
        writeSchedule(competitionKey, matches);
    }

    public static synchronized void invalidateSchedule(String competitionKey) {
        qualMatches.remove(competitionKey);
    }

    // ── Scouted IDs ───────────────────────────────────────────────────

    public static synchronized Set<Integer> getScoutedIds(String competitionKey, ScoutPosition position) {
        return scoutedIds.get(scoutedKey(competitionKey, position));
    }

    public static synchronized void putScoutedIds(String competitionKey, ScoutPosition position, Set<Integer> ids) {
        String key = scoutedKey(competitionKey, position);
        scoutedIds.put(key, ids);
        writeScouted(key, ids);
    }

    public static synchronized void invalidateScoutedIds(String competitionKey, ScoutPosition position) {
        scoutedIds.remove(scoutedKey(competitionKey, position));
    }

    // ── Nuclear option ────────────────────────────────────────────────────────

    /** Clears in-memory caches only; on-disk copies are kept so offline data isn't lost. */
    public static synchronized void invalidateAll() {
        qualMatches.clear();
        scoutedIds.clear();
    }
}
