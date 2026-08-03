package com.example.scoutingapp.data.repository;

import com.example.scoutingapp.data.config.ScoutPosition;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * In-memory singleton cache shared by MatchRepository and DataRepository.
 * Java equivalent of the Kotlin `object ScheduleCache`.
 */
public class ScheduleCache {

    private ScheduleCache() {}

    // ── Quals schedule ────────────────────────────────────────────────────────

    private static final Map<String, List<QualMatch>> qualMatches = new HashMap<>();

    public static synchronized List<QualMatch> getSchedule(String competitionKey) {
        return qualMatches.get(competitionKey);
    }

    public static synchronized void putSchedule(String competitionKey, List<QualMatch> matches) {
        qualMatches.put(competitionKey, matches);
    }

    public static synchronized void invalidateSchedule(String competitionKey) {
        qualMatches.remove(competitionKey);
    }

    // ── Scouted IDs ───────────────────────────────────────────────────────────

    private static final class Key {
        final String competitionKey;
        final ScoutPosition position;

        Key(String competitionKey, ScoutPosition position) {
            this.competitionKey = competitionKey;
            this.position = position;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key)) return false;
            Key key = (Key) o;
            return competitionKey.equals(key.competitionKey) && position == key.position;
        }

        @Override
        public int hashCode() {
            return competitionKey.hashCode() * 31 + position.hashCode();
        }
    }

    private static final Map<Key, Set<Integer>> scoutedIds = new HashMap<>();

    public static synchronized Set<Integer> getScoutedIds(String competitionKey, ScoutPosition position) {
        return scoutedIds.get(new Key(competitionKey, position));
    }

    public static synchronized void putScoutedIds(String competitionKey, ScoutPosition position, Set<Integer> ids) {
        scoutedIds.put(new Key(competitionKey, position), ids);
    }

    public static synchronized void invalidateScoutedIds(String competitionKey, ScoutPosition position) {
        scoutedIds.remove(new Key(competitionKey, position));
    }

    // ── Nuclear option ────────────────────────────────────────────────────────

    public static synchronized void invalidateAll() {
        qualMatches.clear();
        scoutedIds.clear();
    }
}
