package com.example.scoutingapp.ui.scout.components;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;

/**
 * Java equivalent of the HoldTimerButton composable. Reproduces its tap-merging logic exactly:
 * - A press starts a 50ms live-display ticker.
 * - On release, if held >= 200ms, the duration is added to a pending total.
 * - A 200ms "commit" delay follows each release; if a new press starts within that window,
 *   the commit is cancelled and the new press continues the same merged sequence.
 * - If no new press arrives, the pending total is committed as one onTimeUpdate() call.
 */
public class HoldTimerButtonView extends LinearLayout {

    public interface OnTimeUpdate {
        void update(long additionalMs);
    }

    private final TextView labelView;
    private final TextView liveView;
    private final int textColor;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable tickRunnable;
    private Runnable commitRunnable;

    private long pressStart = 0L;
    private long pendingMs = 0L;
    private long lastReleaseTime = 0L;
    private boolean isPressed = false;

    private OnTimeUpdate onTimeUpdate = ms -> {};

    public HoldTimerButtonView(Context context, String label, int backgroundColor, int textColor) {
        super(context);
        this.textColor = textColor;
        setOrientation(VERTICAL);
        setGravity(android.view.Gravity.CENTER);
        int padding = dp(16);
        setPadding(padding, padding, padding, padding);
        setMinimumWidth(dp(140));
        setMinimumHeight(dp(90));

        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(backgroundColor);
        bg.setCornerRadius(dp(12));
        setBackground(bg);

        labelView = new TextView(context);
        labelView.setText(label);
        labelView.setTextColor(textColor);
        labelView.setTypeface(null, android.graphics.Typeface.BOLD);
        labelView.setTextSize(18);
        labelView.setGravity(android.view.Gravity.CENTER);
        addView(labelView);

        liveView = new TextView(context);
        liveView.setTextColor(textColor);
        liveView.setTypeface(null, android.graphics.Typeface.BOLD);
        liveView.setTextSize(15);
        liveView.setGravity(android.view.Gravity.CENTER);
        liveView.setVisibility(GONE);
        addView(liveView);

        setupTouchHandling();
    }

    public void setOnTimeUpdate(OnTimeUpdate listener) {
        this.onTimeUpdate = listener;
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupTouchHandling() {
        setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    onPress();
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    onRelease();
                    return true;
                default:
                    return false;
            }
        });
    }

    private void onPress() {
        pressStart = System.currentTimeMillis();
        long gapMs = pressStart - lastReleaseTime;

        if (gapMs < 200L) {
            if (commitRunnable != null) handler.removeCallbacks(commitRunnable);
        } else {
            pendingMs = 0L;
        }

        isPressed = true;
        liveView.setVisibility(VISIBLE);

        tickRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isPressed) return;
                long currentPressMs = System.currentTimeMillis() - pressStart;
                liveView.setText(String.format(Locale.US, "\u25B6 %.1fs", currentPressMs / 1000.0));
                handler.postDelayed(this, 50);
            }
        };
        handler.post(tickRunnable);
    }

    private void onRelease() {
        long heldMs = System.currentTimeMillis() - pressStart;
        if (tickRunnable != null) handler.removeCallbacks(tickRunnable);
        isPressed = false;
        liveView.setVisibility(GONE);
        lastReleaseTime = System.currentTimeMillis();

        // Only count presses held for at least 200ms.
        if (heldMs >= 200L) {
            pendingMs += heldMs;
            commitRunnable = () -> {
                long toCommit = pendingMs;
                pendingMs = 0L;
                onTimeUpdate.update(toCommit);
            };
            handler.postDelayed(commitRunnable, 200L);
        }
        // If heldMs < 200ms this tap is ignored entirely.
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
