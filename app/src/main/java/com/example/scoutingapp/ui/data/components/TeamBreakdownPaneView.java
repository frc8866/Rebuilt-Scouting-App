package com.example.scoutingapp.ui.data.components;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;

import com.example.scoutingapp.ui.data.DataModels;
import com.example.scoutingapp.ui.data.PitScoutEntry;
import com.example.scoutingapp.ui.data.TeamAggregate;

import java.util.List;
import java.util.Locale;

public class TeamBreakdownPaneView {

    private static final int CARD_BG = 0xFF424242;
    private static final int PANE_BG = 0xFF2E2E2E;
    private static final int LABEL_CLR = 0xFFBDBDBD;
    private static final int VALUE_CLR = Color.WHITE;
    private static final int DIM_CLR = 0xFF888888;
    private static final int ACCENT_CLR = 0xFF82B1FF;
    private static final int YES_CLR = 0xFFB2FF59;
    private static final int NO_CLR = 0xFFFF8A80;
    private static final int SECTION_CLR = 0xFF555555;

    public final View root;
    private final Context context;

    private final TextView tvHeader;
    private final TextView tvMatchCount;
    private final MaterialButton btnRefresh;
    private final TabLayout tabLayout;
    private final FrameLayout tabContentContainer;

    private int selectedTab = 0;
    private DataModels.TeamSelection lastSelection;
    private Runnable onRefresh;

    public TeamBreakdownPaneView(Context context, ViewGroup parent) {
        this.context = context;

        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(16), dp(16), dp(16), dp(16));
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(PANE_BG);
        bg.setCornerRadius(dp(16));
        container.setBackground(bg);

        // Header row
        LinearLayout headerRow = new LinearLayout(context);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);

        tvHeader = new TextView(context);
        tvHeader.setText("\u2014");
        tvHeader.setTextColor(VALUE_CLR);
        tvHeader.setTypeface(null, Typeface.BOLD);
        tvHeader.setTextSize(24);
        headerRow.addView(tvHeader);

        tvMatchCount = new TextView(context);
        tvMatchCount.setTextColor(DIM_CLR);
        tvMatchCount.setTextSize(13);
        LinearLayout.LayoutParams matchCountParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        matchCountParams.setMarginStart(dp(12));
        headerRow.addView(tvMatchCount, matchCountParams);

        View spacer = new View(context);
        headerRow.addView(spacer, new LinearLayout.LayoutParams(0, 0, 1f));

        btnRefresh = new MaterialButton(context, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        btnRefresh.setText("Refresh");
        btnRefresh.setOnClickListener(v -> { if (lastSelection != null && onRefresh != null) onRefresh.run(); });
        headerRow.addView(btnRefresh);

        container.addView(headerRow);

        View gap1 = new View(context);
        container.addView(gap1, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(8)));

        // Tabs
        tabLayout = new TabLayout(context);
        tabLayout.setBackgroundColor(0xFF3A3A3A);
        tabLayout.setTabTextColors(0xFFAAAAAA, ACCENT_CLR);
        tabLayout.setSelectedTabIndicatorColor(ACCENT_CLR);
        tabLayout.addTab(tabLayout.newTab().setText("Match Scouting"));
        tabLayout.addTab(tabLayout.newTab().setText("Pit Scouting"));
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                selectedTab = tab.getPosition();
                renderTabContent();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
        container.addView(tabLayout);

        View gap2 = new View(context);
        container.addView(gap2, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(12)));

        tabContentContainer = new FrameLayout(context);
        container.addView(tabContentContainer, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        this.root = container;
    }

    private DataModels.TeamSelection currentSelection;
    private TeamAggregate currentAggregate;
    private boolean currentLoading;
    private List<PitScoutEntry> currentPitEntries;
    private boolean currentPitLoading;

    public void render(DataModels.TeamSelection selection, TeamAggregate aggregate, boolean loading,
                        List<PitScoutEntry> pitEntries, boolean pitLoading, Runnable onRefresh) {
        this.onRefresh = onRefresh;

        // Reset to first tab when the selected team changes, matching remember(selection?.teamNumber).
        boolean teamChanged = lastSelection == null || selection == null
                || lastSelection.teamNumber != selection.teamNumber;
        if (teamChanged) {
            selectedTab = 0;
            TabLayout.Tab tab = tabLayout.getTabAt(0);
            if (tab != null) tab.select();
        }
        lastSelection = selection;

        currentSelection = selection;
        currentAggregate = aggregate;
        currentLoading = loading;
        currentPitEntries = pitEntries;
        currentPitLoading = pitLoading;

        tvHeader.setText(selection != null ? "Team " + selection.teamNumber : "\u2014");
        if (aggregate != null) {
            int n = aggregate.matchesScoutedCount;
            tvMatchCount.setText(n + " match" + (n != 1 ? "es" : "") + " scouted");
            tvMatchCount.setVisibility(View.VISIBLE);
        } else {
            tvMatchCount.setVisibility(View.GONE);
        }
        btnRefresh.setEnabled(selection != null && !loading && !pitLoading);

        renderTabContent();
    }

    private void renderTabContent() {
        tabContentContainer.removeAllViews();
        if (selectedTab == 0) {
            tabContentContainer.addView(buildMatchScoutingTab(currentSelection, currentAggregate, currentLoading));
        } else {
            tabContentContainer.addView(buildPitScoutingTab(currentSelection, currentPitEntries, currentPitLoading));
        }
    }

    // ── Tab 1: Match Scouting ─────────────────────────────────────────────────

    private View buildMatchScoutingTab(DataModels.TeamSelection selection, TeamAggregate aggregate, boolean loading) {
        if (selection == null) return centeredMessage("Select a team from the left.");
        if (loading) return centeredProgress();
        if (aggregate == null) return centeredMessage("No data available.");
        if (aggregate.matchesScoutedCount == 0) return centeredMessage("No scouted matches yet for this team.");
        return matchScoutingContent(aggregate);
    }

    private View matchScoutingContent(TeamAggregate agg) {
        double ballsPerSec = agg.avgShoot > 0 ? 1.0 / (agg.avgShoot / 1000.0) : 0.0;

        ScrollView scroll = new ScrollView(context);
        LinearLayout column = new LinearLayout(context);
        column.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(column, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        addSectionHeader(column, "Win Rates");
        addStatRow(column,
                new String[]{"Won Match", "Won Auto", "Preloaded", "On Field"},
                new String[]{fmtPct(agg.wonMatchPct) + "%", fmtPct(agg.wonAutoPct) + "%",
                        fmtPct(agg.preloadPct) + "%", fmtPct(agg.onFieldPct) + "%"});

        addSectionHeader(column, "Activity Averages");
        addStatRow(column,
                new String[]{"Avg Intake (s)", "Avg Shoot (s)", "Avg Defend (s)", "Balls / Sec"},
                new String[]{fmt(agg.avgIntake), fmt(agg.avgShoot), fmt(agg.avgDefend), fmt2(ballsPerSec)});

        addSectionHeader(column, "Robot Capabilities");
        addBoolRow(column,
                new String[]{"Over Bump", "Under Trench", "Ground Pickup", "Station Intake"},
                new boolean[]{agg.bump, agg.trench, agg.groundIntake, agg.station});

        addSectionHeader(column, "Climb & Driver");
        addStatRow(column, new String[]{"Driver Skill"}, new String[]{fmt(agg.avgDriverSkill)});

        addSectionHeader(column, "Fuel");
        addStatRow(column,
                new String[]{"Avg Fuel %", "Est. Fuel/Match"},
                new String[]{fmt(agg.avgFuelPercent) + "%", fmt(agg.avgFuelPerMatch)});

        addSectionHeader(column, "Scouter Notes (" + agg.notes.size() + ")");
        if (agg.notes.isEmpty()) {
            TextView empty = new TextView(context);
            empty.setText("No notes recorded.");
            empty.setTextColor(DIM_CLR);
            empty.setTextSize(13);
            empty.setPadding(dp(4), 0, 0, 0);
            column.addView(empty);
        } else {
            for (int i = 0; i < agg.notes.size(); i++) {
                column.addView(noteCard(i + 1, agg.notes.get(i)));
                addGap(column, 8);
            }
        }
        addGap(column, 8);
        return scroll;
    }

    // ── Tab 2: Pit Scouting ───────────────────────────────────────────────────

    private View buildPitScoutingTab(DataModels.TeamSelection selection, List<PitScoutEntry> entries, boolean loading) {
        if (selection == null) return centeredMessage("Select a team from the left.");
        if (loading) return centeredProgress();
        if (entries == null || entries.isEmpty()) return centeredMessage("No pit scouting data for this team.");

        ScrollView scroll = new ScrollView(context);
        LinearLayout column = new LinearLayout(context);
        column.setOrientation(LinearLayout.VERTICAL);
        for (PitScoutEntry entry : entries) {
            column.addView(pitEntryCard(entry));
            addGap(column, 14);
        }
        scroll.addView(column, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return scroll;
    }

    private View pitEntryCard(PitScoutEntry entry) {
        LinearLayout card = cardContainer(14);
        LinearLayout inner = new LinearLayout(context);
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setPadding(dp(16), dp(16), dp(16), dp(16));

        LinearLayout headerRow = new LinearLayout(context);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        TextView idLabel = new TextView(context);
        idLabel.setText("Entry #" + entry.id);
        idLabel.setTextColor(ACCENT_CLR);
        idLabel.setTypeface(null, Typeface.BOLD);
        idLabel.setTextSize(14);
        headerRow.addView(idLabel, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView scoutedBy = new TextView(context);
        scoutedBy.setText("Scouted by: " + (entry.scouterName == null || entry.scouterName.trim().isEmpty() ? "\u2014" : entry.scouterName));
        scoutedBy.setTextColor(DIM_CLR);
        scoutedBy.setTextSize(12);
        headerRow.addView(scoutedBy);
        inner.addView(headerRow);

        addDivider(inner);

        LinearLayout fieldsRow = new LinearLayout(context);
        fieldsRow.setOrientation(LinearLayout.HORIZONTAL);
        fieldsRow.addView(pitField("Drive Train", orDash(entry.driveTrain), orNull(entry.driveTrainOther)));
        fieldsRow.addView(pitField("Shooter", orDash(entry.shooter), orNull(entry.shooterOther)));
        fieldsRow.addView(pitField("Indexer", orDash(entry.indexer), orNull(entry.indexerOther)));
        fieldsRow.addView(pitField("Camera", orDash(entry.camera), null));
        fieldsRow.addView(pitField("Hopper Capacity", String.valueOf(entry.hopperCapacity), null));
        inner.addView(fieldsRow);

        if (entry.notes != null && !entry.notes.trim().isEmpty()) {
            LinearLayout notesCol = new LinearLayout(context);
            notesCol.setOrientation(LinearLayout.VERTICAL);
            TextView notesLabel = new TextView(context);
            notesLabel.setText("NOTES");
            notesLabel.setTextColor(ACCENT_CLR);
            notesLabel.setTextSize(10);
            notesLabel.setTypeface(null, Typeface.BOLD);
            notesCol.addView(notesLabel);
            TextView notesText = new TextView(context);
            notesText.setText(entry.notes);
            notesText.setTextColor(VALUE_CLR);
            notesText.setTextSize(13);
            LinearLayout.LayoutParams notesParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            notesParams.topMargin = dp(4);
            notesCol.addView(notesText, notesParams);
            LinearLayout.LayoutParams outerParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            outerParams.topMargin = dp(10);
            inner.addView(notesCol, outerParams);
        }

        card.addView(inner);
        return card;
    }

    private View pitField(String label, String value, String subValue) {
        LinearLayout col = new LinearLayout(context);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView labelView = new TextView(context);
        labelView.setText(label);
        labelView.setTextColor(LABEL_CLR);
        labelView.setTextSize(10);
        col.addView(labelView);

        TextView valueView = new TextView(context);
        valueView.setText(value);
        valueView.setTextColor(VALUE_CLR);
        valueView.setTextSize(15);
        valueView.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams valueParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        valueParams.topMargin = dp(2);
        col.addView(valueView, valueParams);

        if (subValue != null) {
            TextView subView = new TextView(context);
            subView.setText("(" + subValue + ")");
            subView.setTextColor(DIM_CLR);
            subView.setTextSize(12);
            subView.setTypeface(null, Typeface.ITALIC);
            col.addView(subView);
        }
        return col;
    }

    // ── Shared building blocks ──────────────────────────────────────────────

    private void addSectionHeader(LinearLayout parent, String title) {
        LinearLayout col = new LinearLayout(context);
        col.setOrientation(LinearLayout.VERTICAL);
        TextView label = new TextView(context);
        label.setText(title.toUpperCase(Locale.US));
        label.setTextColor(ACCENT_CLR);
        label.setTextSize(11);
        label.setTypeface(null, Typeface.BOLD);
        col.addView(label);
        addDivider(col);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(14);
        parent.addView(col, params);
    }

    private void addDivider(LinearLayout parent) {
        View divider = new View(context);
        divider.setBackgroundColor(SECTION_CLR);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
        params.topMargin = dp(4);
        parent.addView(divider, params);
    }

    private void addGap(LinearLayout parent, int dpValue) {
        View gap = new View(context);
        parent.addView(gap, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(dpValue)));
    }

    private void addStatRow(LinearLayout parent, String[] titles, String[] values) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < titles.length; i++) {
            row.addView(statCard(titles[i], values[i]), spacedWeightParams(i > 0));
        }
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(10);
        parent.addView(row, params);
    }

    private void addBoolRow(LinearLayout parent, String[] titles, boolean[] values) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < titles.length; i++) {
            row.addView(boolCard(titles[i], values[i]), spacedWeightParams(i > 0));
        }
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(10);
        parent.addView(row, params);
    }

    private LinearLayout.LayoutParams spacedWeightParams(boolean marginStart) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        if (marginStart) p.setMarginStart(dp(10));
        return p;
    }

    private LinearLayout cardContainer(int radiusDp) {
        LinearLayout card = new LinearLayout(context);
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(CARD_BG);
        bg.setCornerRadius(dp(radiusDp));
        card.setBackground(bg);
        return card;
    }

    private View statCard(String title, String value) {
        LinearLayout card = cardContainer(12);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(12), dp(10), dp(12), dp(10));
        TextView titleView = new TextView(context);
        titleView.setText(title);
        titleView.setTextColor(LABEL_CLR);
        titleView.setTextSize(11);
        card.addView(titleView);
        TextView valueView = new TextView(context);
        valueView.setText(value);
        valueView.setTextColor(VALUE_CLR);
        valueView.setTextSize(22);
        valueView.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.topMargin = dp(4);
        card.addView(valueView, p);
        return card;
    }

    private View boolCard(String title, boolean value) {
        LinearLayout card = cardContainer(12);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(12), dp(10), dp(12), dp(10));
        TextView titleView = new TextView(context);
        titleView.setText(title);
        titleView.setTextColor(LABEL_CLR);
        titleView.setTextSize(11);
        card.addView(titleView);
        TextView valueView = new TextView(context);
        valueView.setText(value ? "Yes" : "No");
        valueView.setTextColor(value ? YES_CLR : NO_CLR);
        valueView.setTextSize(22);
        valueView.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.topMargin = dp(4);
        card.addView(valueView, p);
        return card;
    }

    private View noteCard(int index, String text) {
        LinearLayout card = cardContainer(10);
        card.setOrientation(LinearLayout.HORIZONTAL);
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(0xFF383838);
        bg.setCornerRadius(dp(10));
        card.setBackground(bg);
        card.setPadding(dp(12), dp(12), dp(12), dp(12));

        TextView indexView = new TextView(context);
        indexView.setText("#" + index);
        indexView.setTextColor(DIM_CLR);
        indexView.setTextSize(12);
        indexView.setTypeface(null, Typeface.BOLD);
        card.addView(indexView, new LinearLayout.LayoutParams(dp(28), ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView textView = new TextView(context);
        textView.setText(text);
        textView.setTextColor(VALUE_CLR);
        textView.setTextSize(13);
        card.addView(textView, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        return card;
    }

    private View centeredMessage(String text) {
        FrameLayout frame = new FrameLayout(context);
        TextView tv = new TextView(context);
        tv.setText(text);
        tv.setTextColor(DIM_CLR);
        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        frame.addView(tv, p);
        return frame;
    }

    private View centeredProgress() {
        FrameLayout frame = new FrameLayout(context);
        ProgressBar progress = new ProgressBar(context);
        progress.setIndeterminateTintList(android.content.res.ColorStateList.valueOf(ACCENT_CLR));
        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        frame.addView(progress, p);
        return frame;
    }

    private static String orDash(String s) {
        return s == null || s.trim().isEmpty() ? "\u2014" : s;
    }

    private static String orNull(String s) {
        return s == null || s.trim().isEmpty() ? null : s;
    }

    private static String fmt(double d) {
        String s = String.format(Locale.US, "%.1f", d);
        return s.endsWith(".0") ? s.substring(0, s.length() - 2) : s;
    }

    private static String fmt2(double d) {
        return String.format(Locale.US, "%.2f", d);
    }

    private static String fmtPct(double d) {
        return String.format(Locale.US, "%.1f", d);
    }

    private int dp(int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
