package com.example.scoutingapp.ui.scout;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Java equivalent of the Kotlin `data class ScoutUiState`. Kotlin used an immutable data
 * class mutated via `.copy()` and observed through Compose's `mutableStateOf`. Here it's a
 * plain mutable POJO: the ViewModel mutates fields directly and pushes the same reference
 * back through LiveData to trigger observers. Simpler than reimplementing structural copy().
 */
public class ScoutUiState {
    public String competition = "";
    public int matchId = 0;
    public int teamNumber = 0;
    public String scouterName = "";
    public String positionLabel = "";
    public Alliance deviceAlliance = Alliance.RED;
    public Alliance autoWinner = null;
    public Set<Integer> activeShifts = new HashSet<>();
    public ScoutStage stage = ScoutStage.Start;
    public Map<String, Long> timeTotals = new HashMap<>();
    public Map<String, Integer> timeCounts = new HashMap<>();
    public Map<String, Object> data = new HashMap<>();
    public boolean isClockRunning = false;
    public int elapsedSec = 0;
    public int totalDurationSec = 160;
    public boolean isSubmitting = false;
    public boolean submitSuccess = false;
    public String submitError = null;

    public int getRemainingSec() {
        return Math.max(0, totalDurationSec - elapsedSec);
    }

    public boolean canSubmit() {
        boolean wonMatchFilled = data.get("won_match") instanceof Boolean;
        String autoFuel = data.get("alliance_auto_fuel_score") instanceof String ? (String) data.get("alliance_auto_fuel_score") : "";
        String teleopFuel = data.get("alliance_teleop_fuel_score") instanceof String ? (String) data.get("alliance_teleop_fuel_score") : "";
        // Fuel % defaults to 0 and the slider must be dragged at least once, so a submitted 0
        // is always a deliberate observation rather than an untouched default masquerading as one.
        boolean fuelPercentTouched = Boolean.TRUE.equals(data.get("fuel_percent_touched"));
        return wonMatchFilled && !autoFuel.trim().isEmpty() && !teleopFuel.trim().isEmpty()
                && fuelPercentTouched;
    }

    /** Shallow copy - used when the ViewModel needs to hand out a fresh snapshot. */
    public ScoutUiState copy() {
        ScoutUiState s = new ScoutUiState();
        s.competition = competition;
        s.matchId = matchId;
        s.teamNumber = teamNumber;
        s.scouterName = scouterName;
        s.positionLabel = positionLabel;
        s.deviceAlliance = deviceAlliance;
        s.autoWinner = autoWinner;
        s.activeShifts = new HashSet<>(activeShifts);
        s.stage = stage;
        s.timeTotals = new HashMap<>(timeTotals);
        s.timeCounts = new HashMap<>(timeCounts);
        s.data = new HashMap<>(data);
        s.isClockRunning = isClockRunning;
        s.elapsedSec = elapsedSec;
        s.totalDurationSec = totalDurationSec;
        s.isSubmitting = isSubmitting;
        s.submitSuccess = submitSuccess;
        s.submitError = submitError;
        return s;
    }
}
