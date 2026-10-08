package dev.antidoomscroll;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.List;

/** Main screen: status card (R-11), today's total (R-09) and links (J4, J6). */
public class MainActivity extends Activity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Store store;
    private LinearLayout card;
    private TextView title;
    private TextView value;
    private ProgressBar progress;
    private TextView sub;
    private Button turnOn;
    private TextView today;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            render();
            handler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        store = new Store(this);
        if (!store.onboarded()) startActivity(new Intent(this, OnboardingActivity.class));

        LinearLayout content = Ui.screen(this);
        card = Ui.column(this);
        int pad = Ui.dp(this, 16);
        card.setPadding(pad, pad, pad, pad);
        card.setBackground(Ui.outline(this, 0));
        card.setScreenReaderFocusable(true);

        title = Ui.text(this, "", 18);
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        title.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE); // Announce state changes only.
        value = Ui.text(this, "", 40);
        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        sub = Ui.secondary(this, "");
        turnOn = Ui.button(this, "Turn on", v -> startActivity(new Intent(this, OnboardingActivity.class)
                .putExtra(OnboardingActivity.EXTRA_STEP, OnboardingActivity.STEP_ACCESS)));
        card.addView(title);
        card.addView(value);
        card.addView(progress, Ui.spaced(this, 4));
        card.addView(sub, Ui.spaced(this, 4));
        card.addView(turnOn, Ui.spaced(this, 12));
        content.addView(card);

        today = Ui.row(this, "", v -> startActivity(new Intent(this, TodayActivity.class)));
        content.addView(today, Ui.spaced(this, 16));
        content.addView(Ui.row(this, "Settings  ›", v -> startActivity(new Intent(this, SettingsActivity.class))));
    }

    @Override
    protected void onResume() {
        super.onResume();
        tick.run();
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(tick);
    }

    private void render() {
        long now = System.currentTimeMillis();
        SessionPolicy policy = store.policy();
        boolean enabled = Ui.serviceEnabled(this);
        String t;
        String v = "";
        String s;
        String spoken;
        progress.setVisibility(View.GONE);
        if (!enabled) {
            t = "⚠ Not protecting";
            s = "Accessibility access is off. Reels is not limited.";
            spoken = "Not protecting. " + s;
        } else {
            switch (policy.state(now)) {
                case IN_SESSION: {
                    long left = policy.sessionEnd() - now;
                    t = "⏱ Session running";
                    v = Ui.countdown(left);
                    s = "Ends at " + Ui.clock(this, policy.sessionEnd());
                    spoken = "Session running, " + s + ", " + Ui.spokenDuration(left) + " left";
                    progress.setVisibility(View.VISIBLE);
                    progress.setMax(policy.sessionX * 60);
                    progress.setProgress((int) ((now - policy.sessionStart) / 1000));
                    break;
                }
                case COOLDOWN: {
                    long left = policy.cooldownEnd() - now;
                    t = "🔒 Reels locked";
                    v = Ui.countdown(left);
                    s = "Back at " + Ui.clock(this, policy.cooldownEnd());
                    spoken = "Reels locked, back at " + Ui.clock(this, policy.cooldownEnd()) + ", "
                            + Ui.spokenDuration(left) + " left";
                    break;
                }
                default:
                    t = "▶ Reels available";
                    s = "Opening Reels starts a " + store.limitX() + "-min session.";
                    spoken = "Reels available. " + s;
            }
        }
        setIfChanged(title, t);
        setIfChanged(value, v);
        value.setVisibility(v.isEmpty() ? View.GONE : View.VISIBLE);
        setIfChanged(sub, s);
        turnOn.setVisibility(enabled ? View.GONE : View.VISIBLE);
        card.setContentDescription(spoken);

        List<long[]> sessions = store.sessionsSince(Ui.startOfToday());
        long totalMs = 0;
        for (long[] row : sessions) totalMs += row[1];
        setIfChanged(today, "Today: " + Ui.minutes(totalMs) + " min · " + sessions.size()
                + (sessions.size() == 1 ? " session  ›" : " sessions  ›"));
    }

    /** Avoids re-announcing the live-region title every second. */
    private static void setIfChanged(TextView view, String text) {
        if (!text.contentEquals(view.getText())) view.setText(text);
    }
}
