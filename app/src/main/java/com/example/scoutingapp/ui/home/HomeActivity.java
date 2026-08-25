package com.example.scoutingapp.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;

import com.example.scoutingapp.R;
import com.example.scoutingapp.data.config.Competition;
import com.example.scoutingapp.data.config.DeviceConfig;
import com.example.scoutingapp.data.config.DeviceConfigStore;
import com.example.scoutingapp.data.config.ScoutPosition;
import com.example.scoutingapp.data.repository.NextMatchInfo;
import com.example.scoutingapp.data.scout.MatchStateStore;
import com.example.scoutingapp.data.scout.SavedMatchState;
import com.example.scoutingapp.ui.data.DataActivity;
import com.example.scoutingapp.ui.scout.ScoutActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

/**
 * Java equivalent of HomeScreen.kt + all its dialogs (ResumeMatchDialog, ScoutNextMatchDialog,
 * PinDialog, PositionPickerDialog, and the inline competition-picker AlertDialog).
 */
public class HomeActivity extends AppCompatActivity {

    private HomeViewModel viewModel;
    private DeviceConfigStore configStore;
    private MatchStateStore matchStateStore;

    private DrawerLayout drawerLayout;
    private TextView tvCompetitionName;
    private MaterialButton btnScoutNextMatch;
    private ProgressBar progressNextMatch;

    private AlertDialog resumeDialog;
    private AlertDialog scoutNextDialog;
    private AlertDialog pinDialog;
    private AlertDialog positionDialog;

    private boolean resumeDialogHandled = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        configStore = new DeviceConfigStore(this);
        matchStateStore = new MatchStateStore(this);
        viewModel = new ViewModelProvider(this, HomeViewModel.factory(configStore, matchStateStore))
                .get(HomeViewModel.class);

        drawerLayout = findViewById(R.id.drawer_layout);
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        // Toolbar "actions": the scout-position button, added programmatically since it needs
        // dynamic color + click-to-open-PIN-dialog behavior.
        setupToolbarPositionButton(toolbar);

        findViewById(R.id.drawer_item_scout).setOnClickListener(v -> drawerLayout.closeDrawers());
        findViewById(R.id.drawer_item_data).setOnClickListener(v -> {
            drawerLayout.closeDrawers();
            startActivity(new Intent(this, DataActivity.class));
        });

        tvCompetitionName = findViewById(R.id.tv_competition_name);
        btnScoutNextMatch = findViewById(R.id.btn_scout_next_match);
        progressNextMatch = findViewById(R.id.progress_next_match);

        findViewById(R.id.btn_change_competition).setOnClickListener(v -> {
            viewModel.loadCompetitions();
            showCompetitionPickerDialog();
        });

        btnScoutNextMatch.setOnClickListener(v -> viewModel.openScoutNextDialog(this::navigateToScout));

        findViewById(R.id.btn_scout_manually).setOnClickListener(v -> {
            viewModel.discardResume();
            startActivity(new Intent(this, ManualScoutActivity.class));
        });

        observeViewModel();
    }

    private void observeViewModel() {
        viewModel.deviceConfig.observe(this, config -> {
            if (config == null) return;
            tvCompetitionName.setText(config.getCompetition().getDisplayName());
            updateToolbarPositionButton(config.getScoutPosition());
        });

        viewModel.nextMatchLoading.observe(this, loading -> {
            boolean isLoading = loading != null && loading;
            btnScoutNextMatch.setEnabled(!isLoading);
            progressNextMatch.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            btnScoutNextMatch.setText(isLoading ? "" : "Scout Next Match");
        });

        viewModel.nextMatchError.observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                // Simple surfacing of the error; original showed it inline near the button.
                android.widget.Toast.makeText(this, error, android.widget.Toast.LENGTH_LONG).show();
                viewModel.clearNextMatchError();
            }
        });

        viewModel.dialogState.observe(this, this::renderScoutNextDialog);

        viewModel.pendingResume.observe(this, this::renderResumeDialog);
    }

    // ── Toolbar position button ────────────────────────────────────────────────

    private TextView toolbarPositionButton;

    private void setupToolbarPositionButton(Toolbar toolbar) {
        toolbarPositionButton = new TextView(this);
        toolbarPositionButton.setTextSize(13);
        toolbarPositionButton.setTypeface(toolbarPositionButton.getTypeface(), android.graphics.Typeface.BOLD);
        toolbarPositionButton.setPadding(dp(14), dp(6), dp(14), dp(6));
        toolbarPositionButton.setOnClickListener(v -> showPinDialog());
        Toolbar.LayoutParams params = new Toolbar.LayoutParams(
                Toolbar.LayoutParams.WRAP_CONTENT, Toolbar.LayoutParams.WRAP_CONTENT, Gravity.END | Gravity.CENTER_VERTICAL);
        toolbar.addView(toolbarPositionButton, params);

        TextView title = new TextView(this);
        title.setText("Scout");
        title.setTextColor(getColor(R.color.accent));
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        title.setTextSize(13);
        title.setPadding(dp(14), dp(6), dp(14), dp(6));
        toolbar.addView(title);
    }

    private void updateToolbarPositionButton(ScoutPosition position) {
        toolbarPositionButton.setText(position.toLabel());
        boolean isRed = position.name().startsWith("RED");
        toolbarPositionButton.setTextColor(getColor(isRed ? R.color.alliance_red : R.color.alliance_blue));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    // ── Resume dialog ─────────────────────────────────────────────────────────

    private void renderResumeDialog(@Nullable SavedMatchState saved) {
        if (saved == null) {
            if (resumeDialog != null) resumeDialog.dismiss();
            return;
        }
        if (resumeDialog != null && resumeDialog.isShowing()) return;

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_resume_match, null);
        bindInfoRow(view, R.id.row_match, "Match", "Qual " + saved.matchId);
        bindInfoRow(view, R.id.row_team, "Team", String.valueOf(saved.teamNumber));
        bindInfoRow(view, R.id.row_position, "Position", saved.positionLabel);

        String displayName = titleCase(saved.scouterName != null ? saved.scouterName.replace('_', ' ') : "");
        bindInfoRow(view, R.id.row_scouter, "Scouter", displayName.isEmpty() ? "\u2014" : displayName);

        View timeRow = view.findViewById(R.id.row_time_left);
        if (saved.clockWasRunning) {
            int remaining = Math.max(0, 160 - saved.elapsedSec);
            bindInfoRow(view, R.id.row_time_left, "Time left (approx)",
                    String.format(Locale.US, "%d:%02d", remaining / 60, remaining % 60));
            timeRow.setVisibility(View.VISIBLE);
        } else {
            timeRow.setVisibility(View.GONE);
        }

        resumeDialog = new AlertDialog.Builder(this)
                .setTitle("Resume Match?")
                .setView(view)
                .setCancelable(false)
                .setPositiveButton("Resume", (d, w) -> navigateToResume(saved))
                .setNegativeButton("Discard", (d, w) -> viewModel.discardResume())
                .create();
        resumeDialog.show();
    }

    private void bindInfoRow(View parent, int rowId, String label, String value) {
        View row = parent.findViewById(rowId);
        ((TextView) row.findViewById(R.id.row_label)).setText(label);
        ((TextView) row.findViewById(R.id.row_value)).setText(value);
    }

    private static String titleCase(String s) {
        StringBuilder sb = new StringBuilder();
        for (String word : s.split(" ")) {
            if (word.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return sb.toString();
    }

    // ── Scout-next dialog ─────────────────────────────────────────────────────

    private void renderScoutNextDialog(NextMatchDialogState state) {
        if (state == null || state.kind() == NextMatchDialogState.Kind.HIDDEN) {
            if (scoutNextDialog != null) scoutNextDialog.dismiss();
            return;
        }

        if (scoutNextDialog == null) {
            View view = LayoutInflater.from(this).inflate(R.layout.dialog_scout_next_match, null);
            scoutNextDialog = new AlertDialog.Builder(this)
                    .setTitle("Scout Next Match")
                    .setView(view)
                    .setPositiveButton("Scout This Match", (d, w) -> viewModel.confirmPendingMatch())
                    .setNegativeButton("Cancel", (d, w) -> viewModel.dismissScoutNextDialog())
                    .setOnDismissListener(d -> viewModel.dismissScoutNextDialog())
                    .create();
            scoutNextDialog.setOnShowListener(d -> styleScoutNextDialogButtons());
            view.findViewById(R.id.btn_prev_match).setOnClickListener(v -> viewModel.shiftPendingMatch(-1));
            view.findViewById(R.id.btn_next_match).setOnClickListener(v -> viewModel.shiftPendingMatch(1));
        }
        if (!scoutNextDialog.isShowing()) scoutNextDialog.show();

        View stateLoading = scoutNextDialog.findViewById(R.id.state_loading);
        View stateError = scoutNextDialog.findViewById(R.id.state_error);
        View stateReady = scoutNextDialog.findViewById(R.id.state_ready);
        View progressShifting = scoutNextDialog.findViewById(R.id.progress_shifting);

        boolean isLoading = state.kind() == NextMatchDialogState.Kind.LOADING;
        boolean isError = state.kind() == NextMatchDialogState.Kind.ERROR;
        boolean isReadyOrShifting = state.kind() == NextMatchDialogState.Kind.READY
                || state.kind() == NextMatchDialogState.Kind.SHIFTING;

        if (stateLoading != null) stateLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        if (stateError != null) stateError.setVisibility(isError ? View.VISIBLE : View.GONE);
        if (stateReady != null) stateReady.setVisibility(isReadyOrShifting ? View.VISIBLE : View.GONE);

        if (isError) {
            ((TextView) stateError).setText(((NextMatchDialogState.Error) state).message);
        }

        NextMatchInfo info = null;
        boolean shifting = state.kind() == NextMatchDialogState.Kind.SHIFTING;
        if (state instanceof NextMatchDialogState.Ready) info = ((NextMatchDialogState.Ready) state).info;
        if (state instanceof NextMatchDialogState.Shifting) info = ((NextMatchDialogState.Shifting) state).info;

        if (info != null) {
            ((TextView) scoutNextDialog.findViewById(R.id.tv_qual_number)).setText("Qual " + info.matchId);
            ((TextView) scoutNextDialog.findViewById(R.id.tv_team_number)).setText(String.valueOf(info.teamNumber));
            String scouter = titleCase(info.scouterName != null ? info.scouterName.replace('_', ' ') : "");
            ((TextView) scoutNextDialog.findViewById(R.id.tv_scouter_name)).setText(scouter.isEmpty() ? "\u2014" : scouter);
            scoutNextDialog.findViewById(R.id.btn_prev_match).setEnabled(!shifting);
            scoutNextDialog.findViewById(R.id.btn_next_match).setEnabled(!shifting);
            if (progressShifting != null) progressShifting.setVisibility(shifting ? View.VISIBLE : View.GONE);
        }

        boolean canConfirm = state.kind() == NextMatchDialogState.Kind.READY;
        scoutNextDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(canConfirm);
    }

    /** Style the Scout This Match / Cancel dialog buttons to match the app's accent/outline button look. */
    private void styleScoutNextDialogButtons() {
        Button positive = scoutNextDialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Button negative = scoutNextDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        if (positive != null) {
            positive.setBackgroundTintList(null); // theme's TextButton.Dialog style sets a backgroundTint
            positive.setBackgroundResource(R.drawable.bg_button_accent_12);           // that otherwise washes out this drawable's fill.
            positive.setTextColor(getColor(R.color.black));
            positive.setTypeface(positive.getTypeface(), android.graphics.Typeface.BOLD);
            positive.setAllCaps(false);
            positive.setPadding(dp(20), dp(10), dp(20), dp(10));
        }
        if (negative != null) {
            negative.setTextColor(getColor(R.color.accent));
            negative.setAllCaps(false);
            negative.setPadding(dp(20), dp(10), dp(20), dp(10));
        }
    }

    // ── PIN dialog ────────────────────────────────────────────────────────────

    private static final String CORRECT_PIN = "2026";

    private void showPinDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_pin, null);
        TextInputEditText input = view.findViewById(R.id.et_pin);
        TextView error = view.findViewById(R.id.tv_pin_error);

        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { error.setVisibility(View.GONE); }
        });

        pinDialog = new AlertDialog.Builder(this)
                .setTitle("Enter PIN")
                .setView(view)
                .setPositiveButton("Confirm", null) // overridden below to prevent auto-dismiss on wrong PIN
                .setNegativeButton("Cancel", (d, w) -> {})
                .create();
        pinDialog.setOnShowListener(d -> pinDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String text = input.getText() != null ? input.getText().toString() : "";
            if (text.equals(CORRECT_PIN)) {
                pinDialog.dismiss();
                showPositionPickerDialog();
            } else {
                error.setVisibility(View.VISIBLE);
            }
        }));
        pinDialog.show();
    }

    // ── Position picker dialog ────────────────────────────────────────────────

    private void showPositionPickerDialog() {
        DeviceConfig config = viewModel.deviceConfig.getValue();
        ScoutPosition current = config != null ? config.getScoutPosition() : ScoutPosition.RED_1;

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_position_picker, null);
        LinearLayout redRow = view.findViewById(R.id.row_red_positions);
        LinearLayout blueRow = view.findViewById(R.id.row_blue_positions);

        positionDialog = new AlertDialog.Builder(this)
                .setTitle("Select Position")
                .setView(view)
                .setNegativeButton("Cancel", (d, w) -> {})
                .create();

        addPositionOptions(redRow, new ScoutPosition[]{ScoutPosition.RED_1, ScoutPosition.RED_2, ScoutPosition.RED_3},
                getColor(R.color.alliance_red), current);
        addPositionOptions(blueRow, new ScoutPosition[]{ScoutPosition.BLUE_1, ScoutPosition.BLUE_2, ScoutPosition.BLUE_3},
                getColor(R.color.alliance_blue), current);

        positionDialog.show();
    }

    private void addPositionOptions(LinearLayout row, ScoutPosition[] positions, int color, ScoutPosition current) {
        for (ScoutPosition pos : positions) {
            TextView option = new TextView(this);
            option.setText(pos.toLabel());
            option.setTextColor(getColor(android.R.color.white));
            option.setGravity(Gravity.CENTER);
            option.setTextSize(13);
            boolean selected = pos == current;
            option.setTypeface(null, selected ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);

            android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
            bg.setCornerRadius(dp(8));
            bg.setColor(selected ? color : 0xFF555555);
            option.setBackground(bg);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f);
            params.setMarginEnd(dp(8));
            option.setLayoutParams(params);
            option.setOnClickListener(v -> {
                viewModel.changeScoutPosition(pos);
                positionDialog.dismiss();
            });
            row.addView(option);
        }
    }

    // ── Competition picker dialog ─────────────────────────────────────────────

    private void showCompetitionPickerDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_competition_picker, null);
        ProgressBar progress = view.findViewById(R.id.progress_competitions);
        LinearLayout list = view.findViewById(R.id.list_competitions);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Select Competition")
                .setView(view)
                .create();

        viewModel.competitionsLoading.observe(this, loading -> {
            boolean isLoading = loading != null && loading;
            progress.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            list.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        });

        viewModel.availableCompetitions.observe(this, competitions -> {
            if (competitions == null) return;
            populateCompetitionList(list, competitions, dialog);
        });

        dialog.show();
    }

    private void populateCompetitionList(LinearLayout list, List<Competition> competitions, AlertDialog dialog) {
        list.removeAllViews();
        DeviceConfig config = viewModel.deviceConfig.getValue();
        String currentKey = config != null ? config.getCompetition().getKey() : null;

        for (Competition comp : competitions) {
            View row = LayoutInflater.from(this).inflate(R.layout.item_competition_row, list, false);
            TextView label = row.findViewById(R.id.tv_competition_label);
            ImageView check = row.findViewById(R.id.iv_selected_check);

            boolean isSelected = comp.getKey().equals(currentKey);
            label.setText(comp.getDisplayName());
            label.setTextColor(getColor(isSelected ? R.color.accent : R.color.text));
            label.setTypeface(null, isSelected ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
            check.setVisibility(isSelected ? View.VISIBLE : View.GONE);

            row.setOnClickListener(v -> {
                viewModel.changeCompetition(comp);
                dialog.dismiss();
            });
            list.addView(row);
        }
    }

    // ── Navigation ─────────────────────────────────────────────────────────────

    private void navigateToScout(int matchId, int teamNumber, String scouterName) {
        viewModel.discardResume();
        DeviceConfig config = viewModel.deviceConfig.getValue();
        String position = config != null ? config.getScoutPosition().toLabel() : "";
        String competition = config != null ? config.getCompetition().getKey() : "";

        Intent intent = new Intent(this, ScoutActivity.class);
        intent.putExtra(ScoutActivity.EXTRA_MATCH_ID, matchId);
        intent.putExtra(ScoutActivity.EXTRA_TEAM_NUMBER, teamNumber);
        intent.putExtra(ScoutActivity.EXTRA_SCOUTER_NAME, scouterName);
        intent.putExtra(ScoutActivity.EXTRA_POSITION, position);
        intent.putExtra(ScoutActivity.EXTRA_COMPETITION, competition);
        intent.putExtra(ScoutActivity.EXTRA_RESUME, false);
        startActivity(intent);
    }

    private void navigateToResume(SavedMatchState saved) {
        Intent intent = new Intent(this, ScoutActivity.class);
        intent.putExtra(ScoutActivity.EXTRA_MATCH_ID, saved.matchId);
        intent.putExtra(ScoutActivity.EXTRA_TEAM_NUMBER, saved.teamNumber);
        intent.putExtra(ScoutActivity.EXTRA_SCOUTER_NAME, saved.scouterName);
        intent.putExtra(ScoutActivity.EXTRA_POSITION, saved.positionLabel);
        intent.putExtra(ScoutActivity.EXTRA_COMPETITION, saved.competition);
        intent.putExtra(ScoutActivity.EXTRA_RESUME, true);
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawers();
        } else {
            super.onBackPressed();
        }
    }
}
