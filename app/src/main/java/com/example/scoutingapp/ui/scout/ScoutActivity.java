package com.example.scoutingapp.ui.scout;

import android.os.Bundle;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.scoutingapp.R;
import com.example.scoutingapp.data.scout.MatchStateStore;
import com.example.scoutingapp.data.scout.SavedMatchState;
import com.example.scoutingapp.ui.scout.components.AutoWinnerScreenView;
import com.example.scoutingapp.ui.scout.components.MainScoutingStageView;
import com.example.scoutingapp.ui.scout.components.StartStageView;
import com.example.scoutingapp.ui.scout.components.SummaryStageView;

import java.util.HashSet;
import java.util.Set;

/**
 * Java equivalent of ScoutScreen.kt. Instead of Compose's declarative recomposition,
 * this Activity rebuilds the single content container each time ScoutUiState.stage
 * (or other relevant fields) change, by inflating the matching stage view.
 */
public class ScoutActivity extends AppCompatActivity {

    public static final String EXTRA_MATCH_ID = "match_id";
    public static final String EXTRA_TEAM_NUMBER = "team_number";
    public static final String EXTRA_SCOUTER_NAME = "scouter_name";
    public static final String EXTRA_POSITION = "position";
    public static final String EXTRA_COMPETITION = "competition";
    public static final String EXTRA_RESUME = "is_resume";

    private ScoutViewModel viewModel;
    private FrameLayout container;
    private AlertDialog submitErrorDialog;

    private ScoutStage lastRenderedStage = null;
    private boolean initialized = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scout);
        container = findViewById(R.id.scout_container);

        MatchStateStore matchStateStore = new MatchStateStore(this);
        viewModel = new ViewModelProvider(this, ScoutViewModel.factory(matchStateStore)).get(ScoutViewModel.class);

        String competition = getIntent().getStringExtra(EXTRA_COMPETITION);
        int matchId = getIntent().getIntExtra(EXTRA_MATCH_ID, 0);
        int teamNumber = getIntent().getIntExtra(EXTRA_TEAM_NUMBER, 0);
        String scouterName = getIntent().getStringExtra(EXTRA_SCOUTER_NAME);
        String position = getIntent().getStringExtra(EXTRA_POSITION);
        boolean isResume = getIntent().getBooleanExtra(EXTRA_RESUME, false);

        if (scouterName != null && !scouterName.trim().isEmpty() && !initialized) {
            initialized = true;
            if (isResume) {
                SavedMatchState saved = matchStateStore.getSavedStateNow();
                if (saved != null) {
                    if (saved.scouterName == null || saved.scouterName.trim().isEmpty()) {
                        saved.scouterName = scouterName;
                    }
                    viewModel.restoreFrom(saved);
                } else {
                    viewModel.initialize(competition, matchId, teamNumber, position, scouterName);
                }
            } else {
                viewModel.initialize(competition, matchId, teamNumber, position, scouterName);
            }
        }

        viewModel.uiState.observe(this, this::render);
    }

    private void render(ScoutUiState state) {
        if (state == null) return;

        if (state.submitError != null) {
            if (submitErrorDialog == null || !submitErrorDialog.isShowing()) {
                submitErrorDialog = new AlertDialog.Builder(this)
                        .setTitle("Submission Error")
                        .setMessage(state.submitError)
                        .setPositiveButton("OK", (d, w) -> viewModel.clearSubmitError())
                        .setOnDismissListener(d -> viewModel.clearSubmitError())
                        .create();
                submitErrorDialog.show();
            }
        } else if (submitErrorDialog != null) {
            submitErrorDialog.dismiss();
            submitErrorDialog = null;
        }

        container.removeAllViews();

        switch (state.stage) {
            case Start: {
                Boolean preload = state.data.get("preload") instanceof Boolean ? (Boolean) state.data.get("preload") : null;
                Boolean onField = state.data.get("on_field") instanceof Boolean ? (Boolean) state.data.get("on_field") : null;
                StartStageView view = new StartStageView(this, container, state.teamNumber, state.matchId,
                        state.positionLabel, viewModel.formatClock(), state.scouterName, preload, onField,
                        viewModel::onPlayFromStart);
                container.addView(view.root);
                break;
            }
            case Auto: {
                MainScoutingStageView view = new MainScoutingStageView(this, container, state.teamNumber, state.matchId,
                        state.positionLabel, "Auto", viewModel.formatClock(), state.scouterName, state.getRemainingSec(),
                        false, viewModel::addTime, () -> {});
                container.addView(view.root);
                break;
            }
            case AutoWinnerScreen: {
                AutoWinnerScreenView view = new AutoWinnerScreenView(this, container, viewModel.formatClock(),
                        state.teamNumber, state.positionLabel, viewModel::setAutoWinner);
                container.addView(view.root);
                break;
            }
            case Transition:
            case Shift1:
            case Shift2:
            case Shift3:
            case Shift4:
            case Endgame: {
                String stageLabel = stageLabelFor(state);
                boolean showProceed = state.stage == ScoutStage.Endgame;
                MainScoutingStageView view = new MainScoutingStageView(this, container, state.teamNumber, state.matchId,
                        state.positionLabel, stageLabel, viewModel.formatClock(), state.scouterName, state.getRemainingSec(),
                        showProceed, viewModel::addTime, () -> viewModel.setStage(ScoutStage.Summary));
                container.addView(view.root);
                break;
            }
            case Summary: {
                SummaryStageView.Fields fields = new SummaryStageView.Fields();
                fields.bump = boolField(state, "bump");
                fields.trench = boolField(state, "trench");
                fields.groundIntake = boolField(state, "ground_intake");
                fields.station = boolField(state, "station");
                fields.driverSkill = intField(state, "driver_skill", 3);
                fields.fuelPercent = intField(state, "fuel_percent", 0);
                fields.allianceAutoFuelScore = stringField(state, "alliance_auto_fuel_score");
                fields.allianceTeleopFuelScore = stringField(state, "alliance_teleop_fuel_score");
                fields.wonMatch = state.data.get("won_match") instanceof Boolean ? (Boolean) state.data.get("won_match") : null;
                fields.notes = stringField(state, "notes");
                fields.isSubmitting = state.isSubmitting;
                fields.canSubmit = state.canSubmit();

                SummaryStageView view = new SummaryStageView(this, container, state.teamNumber, state.matchId,
                        state.positionLabel, fields, new SummaryStageView.Callbacks() {
                    @Override public void onBumpChange(boolean v) { viewModel.updateField("bump", v); }
                    @Override public void onTrenchChange(boolean v) { viewModel.updateField("trench", v); }
                    @Override public void onGroundIntakeChange(boolean v) { viewModel.updateField("ground_intake", v); }
                    @Override public void onStationChange(boolean v) { viewModel.updateField("station", v); }
                    @Override public void onDriverSkillChange(int v) { viewModel.updateField("driver_skill", v); }
                    @Override public void onFuelPercentChange(int v) { viewModel.updateField("fuel_percent", v); }
                    @Override public void onAutoFuelScoreChange(String v) { viewModel.updateField("alliance_auto_fuel_score", v); }
                    @Override public void onTeleopFuelScoreChange(String v) { viewModel.updateField("alliance_teleop_fuel_score", v); }
                    @Override public void onWonMatchChange(boolean v) { viewModel.updateField("won_match", v); }
                    @Override public void onNotesChange(String v) { viewModel.updateField("notes", v); }
                    @Override public void onSubmit() { viewModel.submitData(ScoutActivity.this::finish); }
                });
                container.addView(view.root);
                break;
            }
        }
    }

    private String stageLabelFor(ScoutUiState state) {
        Set<Integer> active = state.activeShifts != null ? state.activeShifts : new HashSet<>();
        switch (state.stage) {
            case Transition: return "Transition";
            case Shift1: return active.contains(1) ? "Active Shift" : "Inactive Shift";
            case Shift2: return active.contains(2) ? "Active Shift" : "Inactive Shift";
            case Shift3: return active.contains(3) ? "Active Shift" : "Inactive Shift";
            case Shift4: return active.contains(4) ? "Active Shift" : "Inactive Shift";
            case Endgame: return "Endgame";
            default: return "";
        }
    }

    private static boolean boolField(ScoutUiState state, String key) {
        Object v = state.data.get(key);
        return v instanceof Boolean && (Boolean) v;
    }

    private static int intField(ScoutUiState state, String key, int def) {
        Object v = state.data.get(key);
        return v instanceof Integer ? (Integer) v : def;
    }

    private static String stringField(ScoutUiState state, String key) {
        Object v = state.data.get(key);
        return v instanceof String ? (String) v : "";
    }
}
