package com.example.scoutingapp.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;

import com.example.scoutingapp.R;
import com.example.scoutingapp.data.config.Competition;
import com.example.scoutingapp.data.config.DeviceConfigStore;
import com.example.scoutingapp.data.config.ScoutPosition;
import com.example.scoutingapp.ui.scout.ScoutActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class ManualScoutActivity extends AppCompatActivity {

    private ManualScoutViewModel viewModel;
    private LinearLayout redRow;
    private LinearLayout blueRow;
    private MaterialButton btnStart;
    private ProgressBar progressValidating;
    private View warningAlreadyScouted;
    private TextView tvDefaultPosition;
    private TextView tvCompetitionBadge;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual_scout);

        DeviceConfigStore configStore = new DeviceConfigStore(this);
        viewModel = new ViewModelProvider(this, ManualScoutViewModel.factory(configStore))
                .get(ManualScoutViewModel.class);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        redRow = findViewById(R.id.row_red_positions);
        blueRow = findViewById(R.id.row_blue_positions);
        btnStart = findViewById(R.id.btn_start_scouting);
        progressValidating = findViewById(R.id.progress_validating);
        warningAlreadyScouted = findViewById(R.id.warning_already_scouted);
        tvDefaultPosition = findViewById(R.id.tv_default_position);
        tvCompetitionBadge = findViewById(R.id.tv_competition_badge);

        TextInputEditText etMatch = findViewById(R.id.et_match_number);
        TextInputEditText etTeam = findViewById(R.id.et_team_number);
        TextInputEditText etScouter = findViewById(R.id.et_scouter_name);

        etMatch.addTextChangedListener(watcherFor(viewModel::onMatchIdChange));
        etTeam.addTextChangedListener(watcherFor(viewModel::onTeamNumberChange));
        etScouter.addTextChangedListener(watcherFor(viewModel::onScouterNameChange));

        btnStart.setOnClickListener(v -> {
            ManualScoutState s = viewModel.state.getValue();
            if (s == null || s.competition == null) return;
            Integer matchId = parseIntOrNull(s.matchId);
            Integer teamNumber = parseIntOrNull(s.teamNumber);
            if (matchId == null || teamNumber == null) return;
            ScoutPosition position = s.effectivePosition();

            Intent intent = new Intent(this, ScoutActivity.class);
            intent.putExtra(ScoutActivity.EXTRA_MATCH_ID, matchId);
            intent.putExtra(ScoutActivity.EXTRA_TEAM_NUMBER, teamNumber);
            intent.putExtra(ScoutActivity.EXTRA_SCOUTER_NAME, s.scouterName);
            intent.putExtra(ScoutActivity.EXTRA_POSITION, position.toLabel());
            intent.putExtra(ScoutActivity.EXTRA_COMPETITION, s.competition.getKey());
            intent.putExtra(ScoutActivity.EXTRA_RESUME, false);
            startActivity(intent);
        });

        viewModel.state.observe(this, this::render);
    }

    private void render(ManualScoutState s) {
        if (s == null) return;

        if (s.competition != null) {
            tvCompetitionBadge.setText(s.competition.getDisplayName());
        }
        tvDefaultPosition.setText("Device default: " + s.defaultPosition.toLabel());

        ScoutPosition effective = s.effectivePosition();
        populatePositions(redRow, new ScoutPosition[]{ScoutPosition.RED_1, ScoutPosition.RED_2, ScoutPosition.RED_3},
                getColor(R.color.alliance_red), effective);
        populatePositions(blueRow, new ScoutPosition[]{ScoutPosition.BLUE_1, ScoutPosition.BLUE_2, ScoutPosition.BLUE_3},
                getColor(R.color.alliance_blue), effective);

        warningAlreadyScouted.setVisibility(s.isAlreadyScouted ? View.VISIBLE : View.GONE);

        boolean enabled = s.canSubmit && !s.isValidating;
        btnStart.setEnabled(enabled);
        btnStart.setVisibility(s.isValidating ? View.INVISIBLE : View.VISIBLE);
        progressValidating.setVisibility(s.isValidating ? View.VISIBLE : View.GONE);
    }

    private void populatePositions(LinearLayout row, ScoutPosition[] positions, int accentColor, ScoutPosition selected) {
        row.removeAllViews();
        for (ScoutPosition pos : positions) {
            TextView chip = new TextView(this);
            chip.setText(pos.toLabel());
            chip.setGravity(android.view.Gravity.CENTER);
            chip.setTextSize(14);
            boolean isSelected = pos == selected;
            chip.setTextColor(isSelected ? accentColor : getColor(R.color.text_dim));
            chip.setTypeface(null, isSelected ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);

            android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
            bg.setCornerRadius(dp(8));
            bg.setColor(isSelected ? withAlpha(accentColor, 46) : getColor(R.color.surface));
            bg.setStroke(dp(isSelected ? 2 : 1), isSelected ? accentColor : 0xFF555555);
            chip.setBackground(bg);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f);
            params.setMarginEnd(dp(10));
            chip.setLayoutParams(params);
            chip.setOnClickListener(v -> viewModel.onPositionChange(pos));
            row.addView(chip);
        }
    }

    private int withAlpha(int color, int alpha) {
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static Integer parseIntOrNull(String s) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return null; }
    }

    private interface StringConsumer { void accept(String value); }

    private static TextWatcher watcherFor(StringConsumer consumer) {
        return new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { consumer.accept(s.toString()); }
        };
    }
}
