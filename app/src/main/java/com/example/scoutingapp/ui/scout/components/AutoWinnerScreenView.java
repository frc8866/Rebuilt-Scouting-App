package com.example.scoutingapp.ui.scout.components;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.scoutingapp.R;
import com.example.scoutingapp.ui.scout.Alliance;

public class AutoWinnerScreenView {

    public interface OnPickWinner {
        void pick(Alliance winner);
    }

    public final View root;

    public AutoWinnerScreenView(Context context, ViewGroup container, String clockText, int teamNumber,
                                 String positionLabel, OnPickWinner onPickWinner) {
        root = LayoutInflater.from(context).inflate(R.layout.stage_auto_winner, container, false);

        boolean isRed = positionLabel != null && positionLabel.toLowerCase().startsWith("red");
        int allianceColor = isRed ? 0xFFDC143C : 0xFF1E90FF;

        TextView positionBadge = root.findViewById(R.id.tv_position_badge);
        positionBadge.setText(positionLabel);
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(allianceColor);
        bg.setCornerRadius(8 * context.getResources().getDisplayMetrics().density);
        positionBadge.setBackground(bg);

        ((TextView) root.findViewById(R.id.tv_team_number)).setText(String.valueOf(teamNumber));
        ((TextView) root.findViewById(R.id.tv_clock)).setText(clockText);

        root.findViewById(R.id.btn_red).setOnClickListener(v -> onPickWinner.pick(Alliance.RED));
        root.findViewById(R.id.btn_blue).setOnClickListener(v -> onPickWinner.pick(Alliance.BLUE));
    }
}
