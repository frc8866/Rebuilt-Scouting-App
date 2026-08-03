package com.example.scoutingapp.data.scout;

import java.util.ArrayList;
import java.util.List;

public class PendingMatchStore {

    private static PendingMatch currentMatch;

    private PendingMatchStore() {}

    public static synchronized void start(PendingMatch match) {
        currentMatch = match;
    }

    public static synchronized PendingMatch get() {
        return currentMatch;
    }

    public static synchronized void clear() {
        currentMatch = null;
    }

    public static class PendingMatch {
        public final int matchId;
        public final int teamNumber;
        public final List<String> events;

        public PendingMatch(int matchId, int teamNumber) {
            this(matchId, teamNumber, new ArrayList<>());
        }

        public PendingMatch(int matchId, int teamNumber, List<String> events) {
            this.matchId = matchId;
            this.teamNumber = teamNumber;
            this.events = events;
        }
    }
}
