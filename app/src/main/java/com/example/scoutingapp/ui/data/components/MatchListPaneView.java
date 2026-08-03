package com.example.scoutingapp.ui.data.components;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.example.scoutingapp.R;
import com.example.scoutingapp.ui.data.DataModels;
import com.example.scoutingapp.ui.data.DataViewModel;

import java.util.List;
import java.util.function.Consumer;

public class MatchListPaneView {

    public final View root;
    private final LinearLayout listContainer;
    private final EditText searchBox;
    private boolean suppressSearchEvents = false;

    public MatchListPaneView(Context context, ViewGroup parent, Consumer<String> onQueryChange,
                              Consumer<DataModels.TeamSelection> onSelect) {
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(context, 12), dp(context, 12), dp(context, 12), dp(context, 12));
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(0xFF3B3B3B);
        bg.setCornerRadius(dp(context, 16));
        container.setBackground(bg);

        // Top row: menu icon (decorative, no-op) + search box
        LinearLayout topRow = new LinearLayout(context);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        ImageView menuIcon = new ImageView(context);
        menuIcon.setImageResource(R.drawable.ic_menu);
        menuIcon.setPadding(dp(context, 8), dp(context, 8), dp(context, 8), dp(context, 8));
        topRow.addView(menuIcon, new LinearLayout.LayoutParams(dp(context, 40), dp(context, 40)));

        searchBox = new EditText(context);
        searchBox.setHint("Search\u2026");
        searchBox.setHintTextColor(0xFFBBBBBB);
        searchBox.setTextColor(Color.WHITE);
        searchBox.setSingleLine(true);
        android.graphics.drawable.GradientDrawable searchBg = new android.graphics.drawable.GradientDrawable();
        searchBg.setColor(0xFF4A4A4A);
        searchBg.setCornerRadius(dp(context, 8));
        searchBox.setBackground(searchBg);
        searchBox.setPadding(dp(context, 12), dp(context, 8), dp(context, 12), dp(context, 8));
        searchBox.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                if (!suppressSearchEvents) onQueryChange.accept(s.toString());
            }
        });
        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        searchParams.setMarginStart(dp(context, 8));
        topRow.addView(searchBox, searchParams);

        container.addView(topRow, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        View spacer = new View(context);
        container.addView(spacer, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(context, 8)));

        ScrollView scrollView = new ScrollView(context);
        listContainer = new LinearLayout(context);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(listContainer, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        container.addView(scrollView, new LinearLayout.LayoutParams(0, 0, 1f));

        this.root = container;
        this.context = context;
        this.onSelect = onSelect;
    }

    private final Context context;
    private final Consumer<DataModels.TeamSelection> onSelect;

    public void render(DataViewModel.Ui ui) {
        // Keep the search box text in sync without re-triggering onQueryChange.
        if (!searchBox.getText().toString().equals(ui.query)) {
            suppressSearchEvents = true;
            searchBox.setText(ui.query);
            searchBox.setSelection(ui.query.length());
            suppressSearchEvents = false;
        }

        listContainer.removeAllViews();

        List<DataModels.MatchGroup> matches = ui.matches;
        if (matches.isEmpty() && !ui.loading) {
            TextView empty = new TextView(context);
            empty.setText("No matches found.");
            empty.setTextColor(0xFFD3D3D3);
            empty.setPadding(dp(context, 12), dp(context, 12), dp(context, 12), dp(context, 12));
            listContainer.addView(empty);
            return;
        }

        for (DataModels.MatchGroup group : matches) {
            TextView header = new TextView(context);
            header.setText("Quals " + group.matchId);
            header.setTextColor(Color.WHITE);
            header.setTypeface(null, Typeface.BOLD);
            header.setTextSize(16);
            header.setPadding(dp(context, 8), dp(context, 8), dp(context, 8), dp(context, 8));
            listContainer.addView(header);

            listContainer.addView(chipRow(group.red));
            View gap = new View(context);
            listContainer.addView(gap, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(context, 6)));
            listContainer.addView(chipRow(group.blue));

            View divider = new View(context);
            divider.setBackgroundColor(0xFF565656);
            LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(context, 1));
            dividerParams.topMargin = dp(context, 12);
            dividerParams.bottomMargin = dp(context, 12);
            listContainer.addView(divider, dividerParams);
        }
    }

    private LinearLayout chipRow(List<DataModels.TeamChip> chips) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dp(context, 8), 0, dp(context, 8), 0);
        for (DataModels.TeamChip chip : chips) {
            row.addView(teamChip(chip));
            View gap = new View(context);
            row.addView(gap, new LinearLayout.LayoutParams(dp(context, 8), 1));
        }
        return row;
    }

    private View teamChip(DataModels.TeamChip chip) {
        boolean isRed = chip.position == DataModels.TeamPosition.RED1
                || chip.position == DataModels.TeamPosition.RED2 || chip.position == DataModels.TeamPosition.RED3;
        int bgColor = isRed ? 0xFFB71C1C : 0xFF0D47A1;

        LinearLayout chipView = new LinearLayout(context);
        chipView.setOrientation(LinearLayout.HORIZONTAL);
        chipView.setGravity(Gravity.CENTER_VERTICAL);
        chipView.setPadding(dp(context, 10), dp(context, 6), dp(context, 10), dp(context, 6));
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(bgColor);
        bg.setCornerRadius(dp(context, 8));
        chipView.setBackground(bg);
        chipView.setClickable(true);
        chipView.setFocusable(true);
        chipView.setOnClickListener(v -> onSelect.accept(
                new DataModels.TeamSelection(chip.matchId, chip.teamNumber, chip.position)));

        TextView teamText = new TextView(context);
        teamText.setText(String.valueOf(chip.teamNumber));
        teamText.setTextColor(Color.WHITE);
        teamText.setTypeface(null, Typeface.BOLD);
        chipView.addView(teamText);

        View gap = new View(context);
        chipView.addView(gap, new LinearLayout.LayoutParams(dp(context, 8), 1));

        ImageView statusIcon = new ImageView(context);
        statusIcon.setImageResource(chip.scouted ? R.drawable.ic_check : R.drawable.ic_close_small);
        statusIcon.setColorFilter(chip.scouted ? 0xFFB2FF59 : 0xFFFF8A80);
        chipView.addView(statusIcon, new LinearLayout.LayoutParams(dp(context, 16), dp(context, 16)));

        return chipView;
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
