package com.example.scoutingapp.ui.scout.components;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.example.scoutingapp.R;

import java.util.ArrayList;
import java.util.List;

public class StartStageView {

    public interface OnPlay {
        void play(boolean preload, boolean onField);
    }

    private final Context context;
    public final View root;

    private Boolean preload;
    private Boolean onField;

    private final TextView btnYesPreload, btnNoPreload, btnYesOnField, btnNoOnField;
    private final TextView btnPlay;

    public StartStageView(Context context, ViewGroup container, int teamNumber, int matchId,
                           String positionLabel, String clockText, String scouterName,
                           Boolean initialPreload, Boolean initialOnField, OnPlay onPlay) {
        this.context = context;
        this.root = LayoutInflater.from(context).inflate(R.layout.stage_start, container, false);
        this.preload = initialPreload;
        this.onField = initialOnField;

        ((TextView) root.findViewById(R.id.tv_team_number)).setText(String.valueOf(teamNumber));

        View scouterBadge = root.findViewById(R.id.badge_scouter);
        if (scouterName != null && !scouterName.trim().isEmpty()) {
            scouterBadge.setVisibility(View.VISIBLE);
            ((TextView) root.findViewById(R.id.tv_scouter_name)).setText(titleCase(scouterName.replace('_', ' ')));
        } else {
            scouterBadge.setVisibility(View.GONE);
        }

        boolean isRed = positionLabel != null && positionLabel.toLowerCase().startsWith("red");
        int allianceColor = isRed ? 0xFFDC143C : 0xFF1E90FF;
        TextView positionBadge = root.findViewById(R.id.tv_position_badge);
        positionBadge.setText(positionLabel);
        positionBadge.setBackground(roundedDrawable(allianceColor, 8));

        ((TextView) root.findViewById(R.id.tv_match_badge)).setText("Quals " + matchId + " - Start");
        ((TextView) root.findViewById(R.id.tv_clock)).setText(clockText);

        btnPlay = root.findViewById(R.id.btn_play);
        btnPlay.setOnClickListener(v -> {
            if (preload != null && onField != null) {
                onPlay.play(preload, onField);
            } else {
                showValidationError();
            }
        });

        View selectorPreload = root.findViewById(R.id.selector_preload);
        ((TextView) selectorPreload.findViewById(R.id.selector_label)).setText("Fuel Preload");
        btnYesPreload = selectorPreload.findViewById(R.id.btn_yes);
        btnNoPreload = selectorPreload.findViewById(R.id.btn_no);
        btnYesPreload.setOnClickListener(v -> { this.preload = true; refresh(); });
        btnNoPreload.setOnClickListener(v -> { this.preload = false; refresh(); });

        View selectorOnField = root.findViewById(R.id.selector_on_field);
        ((TextView) selectorOnField.findViewById(R.id.selector_label)).setText("On Field");
        btnYesOnField = selectorOnField.findViewById(R.id.btn_yes);
        btnNoOnField = selectorOnField.findViewById(R.id.btn_no);
        btnYesOnField.setOnClickListener(v -> { this.onField = true; refresh(); });
        btnNoOnField.setOnClickListener(v -> { this.onField = false; refresh(); });

        refresh();
    }

    private void refresh() {
        updatePlayButton();
        updateSelector(btnYesPreload, btnNoPreload, preload);
        updateSelector(btnYesOnField, btnNoOnField, onField);
    }

    private void updatePlayButton() {
        boolean bothSelected = preload != null && onField != null;
        btnPlay.setTextColor(bothSelected ? Color.BLACK : 0xFF9E9E9E);
        btnPlay.setBackground(roundedDrawable(bothSelected ? 0xFFEBB302 : 0xFF6B6B6B, 8));
    }

    private void updateSelector(TextView yes, TextView no, Boolean value) {
        int accent = 0xFFEBB302, gray = 0xFF6B6B6B;
        boolean isYes = Boolean.TRUE.equals(value);
        boolean isNo = Boolean.FALSE.equals(value);

        yes.setBackground(roundedDrawable(isYes ? accent : gray, 8));
        yes.setTextColor(isYes ? Color.BLACK : Color.WHITE);

        no.setBackground(roundedDrawable(isNo ? accent : gray, 8));
        no.setTextColor(isNo ? Color.BLACK : Color.WHITE);
    }

    private android.graphics.drawable.GradientDrawable roundedDrawable(int color, int radiusDp) {
        android.graphics.drawable.GradientDrawable d = new android.graphics.drawable.GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radiusDp * context.getResources().getDisplayMetrics().density);
        return d;
    }

    private void showValidationError() {
        List<String> missing = new ArrayList<>();
        if (preload == null) missing.add("Fuel Preload");
        if (onField == null) missing.add("On Field");
        new AlertDialog.Builder(context)
                .setTitle("Selection Required")
                .setMessage("Please select a value for " + String.join(" and ", missing) + " before starting the match.")
                .setPositiveButton("OK", null)
                .show();
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
}
