package com.example.scoutingapp.ui.scout.components;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.scoutingapp.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class SummaryStageView {

    /** Snapshot of the fields this stage edits, mirroring the composable's parameters. */
    public static class Fields {
        public boolean bump, trench, groundIntake, station;
        public int driverSkill = 3;
        public int fuelPercent = 0;
        public String allianceAutoFuelScore = "";
        public String allianceTeleopFuelScore = "";
        public Boolean wonMatch = null;
        public String notes = "";
        public boolean isSubmitting = false;
        public boolean canSubmit = false;
    }

    public interface Callbacks {
        void onBumpChange(boolean v);
        void onTrenchChange(boolean v);
        void onGroundIntakeChange(boolean v);
        void onStationChange(boolean v);
        void onDriverSkillChange(int v);
        void onFuelPercentChange(int v);
        void onAutoFuelScoreChange(String v);
        void onTeleopFuelScoreChange(String v);
        void onWonMatchChange(boolean v);
        void onNotesChange(String v);
        void onSubmit();
    }

    private static final int[] SKILL_COLORS = {0xFFDC143C, 0xFFB05A2A, 0xFFEBB302, 0xFF7CB342, 0xFF4CAF50};
    private static final int UNSELECTED = 0xFF555555;
    private static final int GREEN = 0xFF4CAF50;
    private static final int RED = 0xFFDC143C;
    private static final int TEXT_DIM = 0xFFAAAAAA;

    public final View root;
    private final Context context;
    private final int allianceLight;
    private final Callbacks callbacks;

    private final LinearLayout rowDriverSkill;
    private final TextInputEditText etNotes;
    private final TextView checkTrenchLabel, checkBumpLabel, checkGroundLabel, checkStationLabel;
    private final CheckBox checkTrenchBox, checkBumpBox, checkGroundBox, checkStationBox;
    private final SeekBar seekFuelPercent;
    private final TextView tvFuelPercent;
    private final TextInputEditText etAutoFuel, etTeleopFuel;
    private final TextView tvFuelTotal, tvFuelRequiredHint;
    private final TextView btnWonYes, btnWonNo, tvWonRequiredHint;
    private final MaterialButton btnSubmit;
    private final ProgressBar progressSubmitting;
    private final TextView tvSubmitLockedHint, tvNotesRequiredHint;

    private boolean suppressTextEvents = false;

    public SummaryStageView(Context context, ViewGroup container, int teamNumber, int matchId,
                             String positionLabel, Fields fields, Callbacks callbacks) {
        this.context = context;
        this.callbacks = callbacks;
        root = LayoutInflater.from(context).inflate(R.layout.stage_summary, container, false);

        boolean isRed = positionLabel != null && positionLabel.toLowerCase().startsWith("red");
        int allianceColor = isRed ? 0xFFDC143C : 0xFF1565C0;
        allianceLight = isRed ? 0xFFFF5252 : 0xFF42A5F5;

        root.findViewById(R.id.banner).setBackgroundColor(allianceColor);
        ((TextView) root.findViewById(R.id.tv_position_label)).setText(positionLabel.toUpperCase());
        ((TextView) root.findViewById(R.id.tv_team_banner)).setText("Team " + teamNumber);
        ((TextView) root.findViewById(R.id.tv_quals_badge)).setText("QUALS " + matchId);

        rowDriverSkill = root.findViewById(R.id.row_driver_skill);

        etNotes = root.findViewById(R.id.et_notes);
        tvNotesRequiredHint = root.findViewById(R.id.tv_notes_required_hint);

        LinearLayout checkTrenchRow = root.findViewById(R.id.check_trench);
        LinearLayout checkBumpRow = root.findViewById(R.id.check_bump);
        LinearLayout checkGroundRow = root.findViewById(R.id.check_ground_intake);
        LinearLayout checkStationRow = root.findViewById(R.id.check_station);
        checkTrenchLabel = checkTrenchRow.findViewById(R.id.check_label);
        checkTrenchBox = checkTrenchRow.findViewById(R.id.check_box);
        checkBumpLabel = checkBumpRow.findViewById(R.id.check_label);
        checkBumpBox = checkBumpRow.findViewById(R.id.check_box);
        checkGroundLabel = checkGroundRow.findViewById(R.id.check_label);
        checkGroundBox = checkGroundRow.findViewById(R.id.check_box);
        checkStationLabel = checkStationRow.findViewById(R.id.check_label);
        checkStationBox = checkStationRow.findViewById(R.id.check_box);
        checkTrenchRow.setOnClickListener(v -> callbacks.onTrenchChange(!currentFields.trench));
        checkBumpRow.setOnClickListener(v -> callbacks.onBumpChange(!currentFields.bump));
        checkGroundRow.setOnClickListener(v -> callbacks.onGroundIntakeChange(!currentFields.groundIntake));
        checkStationRow.setOnClickListener(v -> callbacks.onStationChange(!currentFields.station));

        seekFuelPercent = root.findViewById(R.id.seek_fuel_percent);
        tvFuelPercent = root.findViewById(R.id.tv_fuel_percent);
        seekFuelPercent.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                if (fromUser) callbacks.onFuelPercentChange(progress);
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        });

        etAutoFuel = root.findViewById(R.id.et_auto_fuel_score);
        etTeleopFuel = root.findViewById(R.id.et_teleop_fuel_score);
        tvFuelTotal = root.findViewById(R.id.tv_fuel_total);
        tvFuelRequiredHint = root.findViewById(R.id.tv_fuel_required_hint);

        etAutoFuel.addTextChangedListener(digitsOnlyWatcher(callbacks::onAutoFuelScoreChange));
        etTeleopFuel.addTextChangedListener(digitsOnlyWatcher(callbacks::onTeleopFuelScoreChange));
        etNotes.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                if (!suppressTextEvents) callbacks.onNotesChange(s.toString());
            }
        });

        btnWonYes = root.findViewById(R.id.btn_won_yes);
        btnWonNo = root.findViewById(R.id.btn_won_no);
        tvWonRequiredHint = root.findViewById(R.id.tv_won_required_hint);
        btnWonYes.setOnClickListener(v -> callbacks.onWonMatchChange(true));
        btnWonNo.setOnClickListener(v -> callbacks.onWonMatchChange(false));

        btnSubmit = root.findViewById(R.id.btn_submit);
        progressSubmitting = root.findViewById(R.id.progress_submitting);
        tvSubmitLockedHint = root.findViewById(R.id.tv_submit_locked_hint);
        btnSubmit.setOnClickListener(v -> callbacks.onSubmit());

        render(fields);
    }

    private Fields currentFields = new Fields();

    /** Call whenever the underlying data changes (rebuild from ScoutUiState). */
    public void render(Fields f) {
        this.currentFields = f;

        rowDriverSkill.removeAllViews();
        for (int i = 1; i <= 5; i++) {
            boolean selected = i <= f.driverSkill;
            int color = selected ? SKILL_COLORS[i - 1] : UNSELECTED;
            TextView btn = pillButton(String.valueOf(i), color, selected ? Color.BLACK : TEXT_DIM, 22, true);
            final int level = i;
            btn.setOnClickListener(v -> callbacks.onDriverSkillChange(level));
            rowDriverSkill.addView(btn);
        }

        suppressTextEvents = true;
        if (!etNotes.getText().toString().equals(f.notes)) etNotes.setText(f.notes);
        suppressTextEvents = false;

        boolean notesMissing = (f.notes == null || f.notes.trim().isEmpty()) && !f.canSubmit;
        tvNotesRequiredHint.setVisibility(notesMissing ? View.VISIBLE : View.GONE);

        setCheckRow(checkTrenchLabel, checkTrenchBox, f.trench, "Under Trench");
        setCheckRow(checkBumpLabel, checkBumpBox, f.bump, "Over Bump");
        setCheckRow(checkGroundLabel, checkGroundBox, f.groundIntake, "Ground Pickup");
        setCheckRow(checkStationLabel, checkStationBox, f.station, "Outpost / Station");

        seekFuelPercent.setProgress(f.fuelPercent);
        tvFuelPercent.setText(f.fuelPercent + "%");

        setTextIfDifferent(etAutoFuel, f.allianceAutoFuelScore);
        setTextIfDifferent(etTeleopFuel, f.allianceTeleopFuelScore);

        int autoVal = parseIntOrZero(f.allianceAutoFuelScore);
        int teleopVal = parseIntOrZero(f.allianceTeleopFuelScore);
        tvFuelTotal.setText((autoVal + teleopVal) + " pts");
        tvFuelTotal.setTextColor(allianceLight);

        boolean autoMissing = (f.allianceAutoFuelScore == null || f.allianceAutoFuelScore.isEmpty()) && !f.canSubmit;
        boolean teleopMissing = (f.allianceTeleopFuelScore == null || f.allianceTeleopFuelScore.isEmpty()) && !f.canSubmit;
        tvFuelRequiredHint.setVisibility((autoMissing || teleopMissing) ? View.VISIBLE : View.GONE);

        boolean yesSelected = Boolean.TRUE.equals(f.wonMatch);
        boolean noSelected = Boolean.FALSE.equals(f.wonMatch);
        styleWonButton(btnWonYes, yesSelected, GREEN, Color.BLACK);
        styleWonButton(btnWonNo, noSelected, RED, Color.WHITE);
        tvWonRequiredHint.setVisibility((f.wonMatch == null && !f.canSubmit) ? View.VISIBLE : View.GONE);

        btnSubmit.setEnabled(!f.isSubmitting && f.canSubmit);
        btnSubmit.setText(f.isSubmitting ? "Submitting..." : "Submit Match Data");
        progressSubmitting.setVisibility(f.isSubmitting ? View.VISIBLE : View.GONE);
        tvSubmitLockedHint.setVisibility((!f.canSubmit && !f.isSubmitting) ? View.VISIBLE : View.GONE);
    }

    private void setCheckRow(TextView label, CheckBox box, boolean checked, String text) {
        label.setText(text);
        label.setTextColor(checked ? Color.WHITE : TEXT_DIM);
        label.setTypeface(null, checked ? Typeface.BOLD : Typeface.NORMAL);
        box.setChecked(checked);
        ((View) label.getParent()).setBackgroundColor(checked ? withAlpha(allianceLight, 46) : 0xFF3A3A3A);
    }

    private void styleWonButton(TextView btn, boolean selected, int selectedColor, int textColorWhenSelected) {
        btn.setBackgroundColor(selected ? selectedColor : UNSELECTED);
        btn.setTextColor(selected ? textColorWhenSelected : TEXT_DIM);
    }

    private TextView pillButton(String text, int bgColor, int textColor, float textSize, boolean bold) {
        TextView tv = new TextView(context);
        tv.setText(text);
        tv.setGravity(Gravity.CENTER);
        tv.setTextColor(textColor);
        tv.setTextSize(textSize);
        if (bold) tv.setTypeface(null, Typeface.BOLD);
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(bgColor);
        bg.setCornerRadius(8 * context.getResources().getDisplayMetrics().density);
        tv.setBackground(bg);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f);
        params.setMarginEnd(Math.round(6 * context.getResources().getDisplayMetrics().density));
        tv.setLayoutParams(params);
        return tv;
    }

    private void setTextIfDifferent(TextInputEditText et, String value) {
        if (!et.getText().toString().equals(value)) {
            suppressTextEvents = true;
            et.setText(value);
            et.setSelection(value.length());
            suppressTextEvents = false;
        }
    }

    private TextWatcher digitsOnlyWatcher(java.util.function.Consumer<String> consumer) {
        return new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                if (suppressTextEvents) return;
                String text = s.toString();
                if (text.chars().allMatch(Character::isDigit)) consumer.accept(text);
            }
        };
    }

    private static int parseIntOrZero(String s) {
        try { return Integer.parseInt(s); } catch (Exception e) { return 0; }
    }

    private static int withAlpha(int color, int alpha) {
        return (alpha << 24) | (color & 0x00FFFFFF);
    }
}
