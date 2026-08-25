package com.example.scoutingapp.ui.scout;

import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.scoutingapp.data.config.Competition;
import com.example.scoutingapp.data.config.ScoutPosition;
import com.example.scoutingapp.data.repository.MatchRepository;
import com.example.scoutingapp.data.scout.MatchStateStore;
import com.example.scoutingapp.data.scout.SavedMatchState;
import com.example.scoutingapp.data.supabase.PostgrestClient;
import com.example.scoutingapp.domain.TableResolver;
import com.example.scoutingapp.util.AppExecutors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ScoutViewModel extends ViewModel {

    public interface OnSuccess { void run(); }

    private final PostgrestClient postgrest = new PostgrestClient();
    private final MatchStateStore matchStateStore;
    private final MatchRepository matchRepository;

    private final MutableLiveData<ScoutUiState> _uiState = new MutableLiveData<>(new ScoutUiState());
    public final LiveData<ScoutUiState> uiState = _uiState;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> timerTask;
    private ScheduledFuture<?> periodicSaveTask;
    private long startMillis = 0L;
    private boolean initialized = false;

    public ScoutViewModel(MatchStateStore matchStateStore) {
        this(matchStateStore, new MatchRepository());
    }

    public ScoutViewModel(MatchStateStore matchStateStore, MatchRepository matchRepository) {
        this.matchStateStore = matchStateStore;
        this.matchRepository = matchRepository;
    }

    private ScoutUiState state() {
        ScoutUiState v = _uiState.getValue();
        return v != null ? v : new ScoutUiState();
    }

    /** Mutate the current state in place, then notify observers. Must run on main thread. */
    private void update(java.util.function.Consumer<ScoutUiState> mutator) {
        ScoutUiState s = state();
        mutator.accept(s);
        _uiState.setValue(s);
    }

    // ── Init ──────────────────────────────────────────────────────────────────

    public void initialize(String competition, int matchId, int teamNumber, String positionLabel, String scouterName) {
        if (initialized) return;
        if (scouterName == null || scouterName.trim().isEmpty()) return;
        initialized = true;

        cancelTimer();
        cancelPeriodicSave();

        ScoutUiState s = new ScoutUiState();
        s.competition = competition;
        s.matchId = matchId;
        s.teamNumber = teamNumber;
        s.positionLabel = positionLabel;
        s.deviceAlliance = Alliance.fromLabel(positionLabel);
        s.scouterName = scouterName;
        s.stage = ScoutStage.Start;
        s.timeTotals = new HashMap<>();
        s.timeTotals.put("intake", 0L);
        s.timeTotals.put("shoot", 0L);
        s.timeTotals.put("defend", 0L);
        s.timeCounts = new HashMap<>();
        s.timeCounts.put("intake", 0);
        s.timeCounts.put("shoot", 0);
        s.timeCounts.put("defend", 0);
        s.data = new HashMap<>();
        s.data.put("team_number", teamNumber);
        s.data.put("preload", false);
        s.data.put("on_field", true);
        s.data.put("won_auto", null);
        s.data.put("bump", false);
        s.data.put("trench", false);
        s.data.put("ground_intake", false);
        s.data.put("station", false);
        s.data.put("driver_skill", 3);
        s.data.put("fuel_percent", 0);
        s.data.put("alliance_auto_fuel_score", "");
        s.data.put("alliance_teleop_fuel_score", "");
        s.data.put("won_match", null);
        s.data.put("notes", "");
        s.data.put("avg_intake", 0.0);
        s.data.put("avg_shoot", 0.0);
        s.data.put("total_shoot", 0.0);
        s.data.put("avg_defend", 0.0);
        s.data.put("fuel_percent_touched", false);
        s.totalDurationSec = 160;

        _uiState.setValue(s);
        Log.d("ScoutVM", "initialized: match=" + matchId + " team=" + teamNumber + " scouter=" + scouterName);
        AppExecutors.runBackground(this::saveSnapshotBlocking);
    }

    // ── Resume ────────────────────────────────────────────────────────────────

    public void restoreFrom(SavedMatchState saved) {
        Log.d("ScoutVM", "restoring scouterName=" + saved.scouterName);
        cancelTimer();
        cancelPeriodicSave();

        Map<String, Object> data = new HashMap<>();
        if (saved.dataFields != null) {
            for (Map.Entry<String, String> e : saved.dataFields.entrySet()) {
                String key = e.getKey();
                String strVal = e.getValue();
                String type = saved.dataTypes != null ? saved.dataTypes.get(key) : null;
                Object value = null;
                if (type != null && strVal != null) {
                    switch (type) {
                        case "bool": value = Boolean.parseBoolean(strVal); break;
                        case "int":
                            try { value = Integer.parseInt(strVal); } catch (NumberFormatException ignored) {}
                            break;
                        case "double":
                            try { value = Double.parseDouble(strVal); } catch (NumberFormatException ignored) {}
                            break;
                        case "string": value = strVal; break;
                        default: value = null;
                    }
                }
                data.put(key, value);
            }
        }

        int fastForwardSec = saved.clockWasRunning
                ? (int) ((System.currentTimeMillis() - saved.exitEpochMs) / 1000L)
                : 0;
        int resumedElapsed = Math.min(saved.elapsedSec + fastForwardSec, 160);
        ScoutStage stage = resolveStageOnResume(saved.stageName, resumedElapsed, saved.autoWinnerName);

        Map<String, Long> restoredTotals = new HashMap<>();
        restoredTotals.put("intake", 0L);
        restoredTotals.put("shoot", 0L);
        restoredTotals.put("defend", 0L);
        Map<String, Integer> restoredCounts = new HashMap<>();
        restoredCounts.put("intake", 0);
        restoredCounts.put("shoot", 0);
        restoredCounts.put("defend", 0);

        if (saved.timeTotals != null) {
            for (Map.Entry<String, Long> e : saved.timeTotals.entrySet()) {
                String key = e.getKey();
                long ms = e.getValue();
                if (key.endsWith("intake")) restoredTotals.put("intake", restoredTotals.get("intake") + ms);
                else if (key.endsWith("shoot")) restoredTotals.put("shoot", restoredTotals.get("shoot") + ms);
                else if (key.endsWith("defend")) restoredTotals.put("defend", restoredTotals.get("defend") + ms);
            }
        }
        if (saved.timeCounts != null) {
            for (Map.Entry<String, Integer> e : saved.timeCounts.entrySet()) {
                String key = e.getKey();
                int count = e.getValue();
                if (key.endsWith("intake")) restoredCounts.put("intake", restoredCounts.get("intake") + count);
                else if (key.endsWith("shoot")) restoredCounts.put("shoot", restoredCounts.get("shoot") + count);
                else if (key.endsWith("defend")) restoredCounts.put("defend", restoredCounts.get("defend") + count);
            }
        }

        ScoutUiState s = new ScoutUiState();
        s.competition = saved.competition;
        s.matchId = saved.matchId;
        s.teamNumber = saved.teamNumber;
        s.scouterName = saved.scouterName != null ? saved.scouterName : "";
        s.positionLabel = saved.positionLabel;
        s.deviceAlliance = Alliance.fromLabel(saved.positionLabel);
        s.autoWinner = saved.autoWinnerName != null ? Alliance.valueOf(saved.autoWinnerName) : null;
        s.activeShifts = saved.activeShifts != null ? new HashSet<>(saved.activeShifts) : new HashSet<>();
        s.stage = stage;
        s.timeTotals = restoredTotals;
        s.timeCounts = restoredCounts;
        s.data = data;
        s.isClockRunning = saved.clockWasRunning;
        s.elapsedSec = resumedElapsed;
        s.totalDurationSec = 160;

        _uiState.setValue(s);

        if (saved.clockWasRunning) {
            startMillis = SystemClock.elapsedRealtime() - (resumedElapsed * 1000L);
            launchTimerLoop();
            startPeriodicSave();
        }
    }

    // ── Mutations ─────────────────────────────────────────────────────────────

    public void updateField(String field, Object value) {
        update(s -> s.data.put(field, value));
        AppExecutors.runBackground(this::saveSnapshotBlocking);
    }

    public void addTime(String action, long additionalMs) {
        if (additionalMs < 200L) return;
        update(s -> {
            s.timeTotals.put(action, s.timeTotals.getOrDefault(action, 0L) + additionalMs);
            s.timeCounts.put(action, s.timeCounts.getOrDefault(action, 0) + 1);
        });
        AppExecutors.runBackground(this::saveSnapshotBlocking);
    }

    public void setStage(ScoutStage stage) {
        update(s -> s.stage = stage);
        AppExecutors.runBackground(this::saveSnapshotBlocking);
    }

    public void onPlayFromStart(boolean preload, boolean onField) {
        update(s -> {
            s.data.put("preload", preload);
            s.data.put("on_field", onField);
            s.stage = ScoutStage.Auto;
        });
        startMatchClock();
    }

    public void setAutoWinner(Alliance winner) {
        update(s -> {
            boolean won = winner == s.deviceAlliance;
            s.data.put("won_auto", won);
            s.autoWinner = winner;
            s.activeShifts = activeShiftsFor(s.deviceAlliance, winner);
            s.stage = stageForSecond(s.elapsedSec);
        });
        AppExecutors.runBackground(this::saveSnapshotBlocking);
    }

    public String formatClock() {
        int r = state().getRemainingSec();
        return String.format("%d:%02d", r / 60, r % 60);
    }

    public void clearSubmitError() {
        update(s -> s.submitError = null);
    }

    // ── Timer ─────────────────────────────────────────────────────────────────

    private void startMatchClock() {
        cancelTimer();
        startMillis = SystemClock.elapsedRealtime();
        update(s -> { s.isClockRunning = true; s.elapsedSec = 0; });
        launchTimerLoop();
        startPeriodicSave();
    }

    private void launchTimerLoop() {
        timerTask = scheduler.scheduleWithFixedDelay(() -> {
            ScoutUiState s = state();
            int elapsed = Math.min((int) ((SystemClock.elapsedRealtime() - startMillis) / 1000L), s.totalDurationSec);
            AppExecutors.runOnMain(() -> {
                update(state -> {
                    state.elapsedSec = elapsed;
                    // Don't auto-advance the stage once the scouter is on the Summary screen
                    if (state.stage != ScoutStage.Summary) {
                        if (elapsed >= 20 && state.autoWinner == null) {
                            state.stage = ScoutStage.AutoWinnerScreen;
                        } else {
                            state.stage = stageForSecond(elapsed);
                        }
                    }
                });
            });
            if (elapsed >= s.totalDurationSec) {
                cancelTimer();
            }
        }, 500, 500, TimeUnit.MILLISECONDS);
    }

    private void cancelTimer() {
        if (timerTask != null) {
            timerTask.cancel(false);
            timerTask = null;
        }
    }

    private void startPeriodicSave() {
        cancelPeriodicSave();
        periodicSaveTask = scheduler.scheduleWithFixedDelay(this::saveSnapshotBlocking, 3, 3, TimeUnit.SECONDS);
    }

    private void cancelPeriodicSave() {
        if (periodicSaveTask != null) {
            periodicSaveTask.cancel(false);
            periodicSaveTask = null;
        }
    }

    // ── Snapshot ──────────────────────────────────────────────────────────────

    /** Runs on a background thread (called from the scheduler or AppExecutors.runBackground). */
    private void saveSnapshotBlocking() {
        ScoutUiState s = state();
        Map<String, String> fields = new HashMap<>();
        Map<String, String> types = new HashMap<>();
        for (Map.Entry<String, Object> e : s.data.entrySet()) {
            Object value = e.getValue();
            String key = e.getKey();
            if (value == null) { fields.put(key, null); types.put(key, "null"); }
            else if (value instanceof Boolean) { fields.put(key, value.toString()); types.put(key, "bool"); }
            else if (value instanceof Integer) { fields.put(key, value.toString()); types.put(key, "int"); }
            else if (value instanceof Double) { fields.put(key, value.toString()); types.put(key, "double"); }
            else { fields.put(key, value.toString()); types.put(key, "string"); }
        }

        SavedMatchState saved = new SavedMatchState(
                s.competition, s.matchId, s.teamNumber, s.positionLabel, s.scouterName,
                s.elapsedSec, System.currentTimeMillis(), s.isClockRunning,
                stageToName(s.stage), s.autoWinner != null ? s.autoWinner.name() : null,
                new ArrayList<>(s.activeShifts), new HashMap<>(s.timeTotals), new HashMap<>(s.timeCounts),
                fields, types
        );
        matchStateStore.save(saved);
    }

    private static String stageToName(ScoutStage stage) {
        return stage.name();
    }

    private static ScoutStage resolveStageOnResume(String name, int elapsedSec, String autoWinnerName) {
        if (name == null) return stageForSecond(elapsedSec);
        switch (name) {
            case "Start": return ScoutStage.Start;
            case "Auto": return ScoutStage.Auto;
            case "AutoWinnerScreen":
                return autoWinnerName != null ? stageForSecond(elapsedSec) : ScoutStage.AutoWinnerScreen;
            case "Transition": return ScoutStage.Transition;
            case "Shift1": return ScoutStage.Shift1;
            case "Shift2": return ScoutStage.Shift2;
            case "Shift3": return ScoutStage.Shift3;
            case "Shift4": return ScoutStage.Shift4;
            case "Endgame": return ScoutStage.Endgame;
            case "Summary": return ScoutStage.Summary;
            default: return stageForSecond(elapsedSec);
        }
    }

    private static ScoutStage stageForSecond(int sec) {
        ScoutStage result = ScoutStage.Auto;
        for (PhaseWindow w : PHASE_PLAN_STATIC) {
            if (sec >= w.startSec) result = w.stage;
        }
        return result;
    }

    private static final List<PhaseWindow> PHASE_PLAN_STATIC = Arrays.asList(
            new PhaseWindow(ScoutStage.Auto, 0, 20),
            new PhaseWindow(ScoutStage.Transition, 20, 30),
            new PhaseWindow(ScoutStage.Shift1, 30, 55),
            new PhaseWindow(ScoutStage.Shift2, 55, 80),
            new PhaseWindow(ScoutStage.Shift3, 80, 105),
            new PhaseWindow(ScoutStage.Shift4, 105, 130),
            new PhaseWindow(ScoutStage.Endgame, 130, 160)
    );

    private static Set<Integer> activeShiftsFor(Alliance alliance, Alliance winner) {
        Set<Integer> result = new HashSet<>();
        if (alliance == winner) { result.add(2); result.add(4); } else { result.add(1); result.add(3); }
        return result;
    }

    // ── Submit ────────────────────────────────────────────────────────────────

    public void submitData(OnSuccess onSuccess) {
        ScoutUiState current = state();
        if (!current.canSubmit()) {
            update(s -> s.submitError = "Please fill in Won Match and Alliance Fuel Score before submitting.");
            return;
        }

        update(s -> { s.isSubmitting = true; s.submitError = null; });
        calculateAverages();

        AppExecutors.runBackground(() -> {
            ScoutUiState s = state();
            Map<String, Object> d = s.data;
            String positionStr = TableResolver.positionString(parsePositionLabel(s.positionLabel));

            ScoutDataPayload payload = new ScoutDataPayload(
                    s.matchId,
                    s.teamNumber,
                    positionStr,
                    s.scouterName,
                    boolOrDefault(d.get("preload"), false),
                    boolOrDefault(d.get("on_field"), true),
                    boolOrDefault(d.get("won_auto"), false),
                    boolOrDefault(d.get("bump"), false),
                    boolOrDefault(d.get("trench"), false),
                    boolOrDefault(d.get("ground_intake"), false),
                    boolOrDefault(d.get("station"), false),
                    intOrDefault(d.get("driver_skill"), 3),
                    intOrDefault(d.get("fuel_percent"), 0),
                    parseIntOrDefault(d.get("alliance_auto_fuel_score"), 0),
                    parseIntOrDefault(d.get("alliance_teleop_fuel_score"), 0),
                    boolOrDefault(d.get("won_match"), false),
                    d.get("notes") instanceof String ? (String) d.get("notes") : "",
                    doubleOrDefault(d.get("avg_intake"), 0.0),
                    doubleOrDefault(d.get("avg_shoot"), 0.0),
                    doubleOrDefault(d.get("total_shoot"), 0.0),
                    doubleOrDefault(d.get("avg_defend"), 0.0)
            );
            String tableName = TableResolver.data(new Competition(s.competition, ""));

            try {
                postgrest.insert(tableName, payload.toJson());
                Log.d("ScoutVM", "Submit succeeded: " + tableName + " match=" + payload.matchNumber);
                AppExecutors.runOnMain(() -> onSubmitSuccess(onSuccess));
            } catch (Exception e) {
                Log.e("ScoutVM", "Submit failed: " + e.getMessage());
                AppExecutors.runOnMain(() -> update(st -> {
                    st.isSubmitting = false;
                    st.submitError = "Submission failed: " + e.getMessage();
                }));
            }
        });
    }

    private void onSubmitSuccess(OnSuccess onSuccess) {
        cancelTimer();
        cancelPeriodicSave();

        ScoutUiState s = state();
        Competition competition = new Competition(s.competition, "");
        ScoutPosition position = parsePositionLabel(s.positionLabel);
        matchRepository.invalidateScoutedIds(competition, position);

        AppExecutors.runBackground(matchStateStore::clear);

        update(st -> { st.isSubmitting = false; st.submitSuccess = true; });
        onSuccess.run();
    }

    // ── Averages ──────────────────────────────────────────────────────────────

    private void calculateAverages() {
        updateField("avg_intake", calcAvg("intake"));
        updateField("avg_shoot", calcAvg("shoot"));
        updateField("total_shoot", calcTotal("shoot"));
        updateField("avg_defend", calcAvg("defend"));
    }

    private double calcAvg(String action) {
        ScoutUiState s = state();
        long totalMs = s.timeTotals.getOrDefault(action, 0L);
        int totalCount = s.timeCounts.getOrDefault(action, 0);
        if (totalCount == 0) return 0.0;
        return ((long) ((totalMs / (double) totalCount / 1000.0) * 10)) / 10.0;
    }

    /** Total accumulated time (seconds) spent in the given action across the whole match. */
    private double calcTotal(String action) {
        ScoutUiState s = state();
        long totalMs = s.timeTotals.getOrDefault(action, 0L);
        return ((long) ((totalMs / 1000.0) * 10)) / 10.0;
    }

    private static ScoutPosition parsePositionLabel(String label) {
        String normalized = label.replace(" ", "_").toUpperCase();
        switch (normalized) {
            case "RED_1": return ScoutPosition.RED_1;
            case "RED_2": return ScoutPosition.RED_2;
            case "RED_3": return ScoutPosition.RED_3;
            case "BLUE_1": return ScoutPosition.BLUE_1;
            case "BLUE_2": return ScoutPosition.BLUE_2;
            case "BLUE_3": return ScoutPosition.BLUE_3;
            default: throw new IllegalArgumentException("Invalid position label: " + label);
        }
    }

    private static boolean boolOrDefault(Object v, boolean def) {
        return v instanceof Boolean ? (Boolean) v : def;
    }

    private static int intOrDefault(Object v, int def) {
        return v instanceof Integer ? (Integer) v : def;
    }

    private static double doubleOrDefault(Object v, double def) {
        return v instanceof Double ? (Double) v : def;
    }

    private static int parseIntOrDefault(Object v, int def) {
        if (v instanceof String) {
            try { return Integer.parseInt((String) v); } catch (NumberFormatException ignored) {}
        }
        return def;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        cancelTimer();
        cancelPeriodicSave();
        scheduler.shutdownNow();
    }

    public static ViewModelProvider.Factory factory(MatchStateStore matchStateStore) {
        return new ViewModelProvider.Factory() {
            @NonNull
            @Override
            @SuppressWarnings("unchecked")
            public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                return (T) new ScoutViewModel(matchStateStore);
            }
        };
    }
}
