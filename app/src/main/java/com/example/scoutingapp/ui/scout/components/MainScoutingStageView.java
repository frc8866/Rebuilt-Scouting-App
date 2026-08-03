package com.example.scoutingapp.ui.scout.components;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.scoutingapp.R;

public class MainScoutingStageView {

    public interface OnTimeUpdate {
        void update(String action, long additionalMs);
    }

    public final View root;

    public MainScoutingStageView(Context context, ViewGroup container, int teamNumber, int matchId,
                                  String positionLabel, String stageLabel, String clockText, String scouterName,
                                  int remainingSec, boolean showProceedButton,
                                  OnTimeUpdate onTimeUpdate, Runnable onProceedToSummary) {
        root = LayoutInflater.from(context).inflate(R.layout.stage_main_scouting, container, false);

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
        android.graphics.drawable.GradientDrawable badgeBg = new android.graphics.drawable.GradientDrawable();
        badgeBg.setColor(allianceColor);
        badgeBg.setCornerRadius(8 * context.getResources().getDisplayMetrics().density);
        positionBadge.setBackground(badgeBg);

        ((TextView) root.findViewById(R.id.tv_match_badge)).setText("Quals " + matchId + " - " + stageLabel);
        ((TextView) root.findViewById(R.id.tv_clock)).setText(clockText);

        View proceedBtn = root.findViewById(R.id.btn_proceed);
        boolean showProceed = showProceedButton && remainingSec == 0;
        proceedBtn.setVisibility(showProceed ? View.VISIBLE : View.GONE);
        proceedBtn.setOnClickListener(v -> onProceedToSummary.run());

        LinearLayout holdButtonsContainer = root.findViewById(R.id.hold_timer_buttons_container);
        holdButtonsContainer.addView(makeSpacedHoldButton(context, "Intake", 0xFFEBB302, android.graphics.Color.BLACK,
                ms -> onTimeUpdate.update("intake", ms), false));
        holdButtonsContainer.addView(makeSpacedHoldButton(context, "Shoot", 0xFFEBB302, android.graphics.Color.BLACK,
                ms -> onTimeUpdate.update("shoot", ms), true));
        holdButtonsContainer.addView(makeSpacedHoldButton(context, "Defend", 0xFF6B6B6B, android.graphics.Color.WHITE,
                ms -> onTimeUpdate.update("defend", ms), true));
    }

    private HoldTimerButtonView makeSpacedHoldButton(Context context, String label, int bgColor, int textColor,
                                                      HoldTimerButtonView.OnTimeUpdate listener, boolean marginStart) {
        HoldTimerButtonView button = new HoldTimerButtonView(context, label, bgColor, textColor);
        button.setOnTimeUpdate(listener);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        if (marginStart) params.setMarginStart(Math.round(16 * context.getResources().getDisplayMetrics().density));
        button.setLayoutParams(params);
        return button;
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
