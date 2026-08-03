package com.example.scoutingapp.data.scout;

import java.util.List;
import java.util.Map;

/**
 * Fully serializable snapshot of an in-progress scouting session.
 * Persisted to SharedPreferences (as JSON) so it survives process death.
 *
 * exitEpochMs = System.currentTimeMillis() at the moment of last save.
 * On resume the timer fast-forwards by (now - exitEpochMs) / 1000 seconds.
 */
public class SavedMatchState {
    public String competition;
    public int matchId;
    public int teamNumber;
    public String positionLabel;
    public String scouterName;

    // Timer
    public int elapsedSec;
    public long exitEpochMs;
    public boolean clockWasRunning;

    // Stage - stored as simple class name
    public String stageName;

    // Auto winner
    public String autoWinnerName; // "RED" | "BLUE" | null
    public List<Integer> activeShifts;

    // Time accumulators
    public Map<String, Long> timeTotals;
    public Map<String, Integer> timeCounts;

    // data map - serialized as strings to avoid dynamic-type serialization issues
    public Map<String, String> dataFields;
    public Map<String, String> dataTypes; // "bool" | "int" | "double" | "string" | "null"

    public SavedMatchState() {
        // no-arg constructor for Gson-style deserialization
    }

    public SavedMatchState(String competition, int matchId, int teamNumber, String positionLabel,
                            String scouterName, int elapsedSec, long exitEpochMs, boolean clockWasRunning,
                            String stageName, String autoWinnerName, List<Integer> activeShifts,
                            Map<String, Long> timeTotals, Map<String, Integer> timeCounts,
                            Map<String, String> dataFields, Map<String, String> dataTypes) {
        this.competition = competition;
        this.matchId = matchId;
        this.teamNumber = teamNumber;
        this.positionLabel = positionLabel;
        this.scouterName = scouterName;
        this.elapsedSec = elapsedSec;
        this.exitEpochMs = exitEpochMs;
        this.clockWasRunning = clockWasRunning;
        this.stageName = stageName;
        this.autoWinnerName = autoWinnerName;
        this.activeShifts = activeShifts;
        this.timeTotals = timeTotals;
        this.timeCounts = timeCounts;
        this.dataFields = dataFields;
        this.dataTypes = dataTypes;
    }
}
