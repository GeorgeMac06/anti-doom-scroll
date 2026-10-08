package dev.antidoomscroll;

import android.accessibilityservice.AccessibilityService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/** Detects Reels in Instagram and enforces sessions and cooldowns (R-01 to R-07, R-15). */
public class GuardService extends AccessibilityService {
    static final String INSTAGRAM = "com.instagram.android";
    /** Lets the screen settle; skips the ~40 ms tab-switch frame that shows feed and Reels together. */
    private static final long SETTLE_MS = 300;
    private static final long RETRY_MS = 500;
    private static final long TOAST_GAP_MS = 10_000;
    private static final long WARN_BEFORE_MS = 2 * 60_000;
    private static final long CARD_MS = 4_000;
    private static final long PROMPT_MS = 10_000;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Store store;
    private boolean evaluatePending;
    /** When the current stretch of allowed Reels viewing began; 0 when not watching. */
    private long watchingSince;
    private int exitAttempts;
    private long lastExitAt;
    private long lastToastAt;
    private long warnedSession;
    private View card;

    private final Runnable evaluate = () -> {
        evaluatePending = false;
        evaluate();
    };
    private final Runnable sessionEnd = this::onSessionEnd;
    private final Runnable warn = this::onWarn;
    private final Runnable hideCard = this::hideCard;
    private final BroadcastReceiver screenOff = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            setWatching(false, System.currentTimeMillis());
        }
    };

    @Override
    protected void onServiceConnected() {
        store = new Store(this);
        registerReceiver(screenOff, new IntentFilter(Intent.ACTION_SCREEN_OFF));
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (store == null) return;
        // Outside Instagram, events only matter for noticing that the user left Reels.
        if (!isInstagram(event.getPackageName()) && watchingSince == 0 && exitAttempts == 0) return;
        scheduleEvaluate(SETTLE_MS);
    }

    private void scheduleEvaluate(long delayMs) {
        if (evaluatePending) return;
        evaluatePending = true;
        handler.postDelayed(evaluate, delayMs);
    }

    private void evaluate() {
        long now = System.currentTimeMillis();
        if (!limitedReelsShowing(getRootInActiveWindow())) {
            setWatching(false, now);
            exitAttempts = 0;
            return;
        }
        SessionPolicy policy = store.policy();
        if (policy.allowsReels(now)) {
            exitAttempts = 0;
            setWatching(true, now);
            scheduleTimers(policy, now);
        } else {
            setWatching(false, now);
            exitReels(policy, now);
        }
    }

    /** True when a Reels view is on screen that is not a reel someone sent in a DM. */
    private static boolean limitedReelsShowing(AccessibilityNodeInfo root) {
        if (root == null || !isInstagram(root.getPackageName())) return false;
        // Probe finding (A-01): Instagram keeps the hidden Reels tab in the tree; only a visible pager counts.
        if (!anyVisible(root, "clips_viewer_view_pager")) return false;
        // Probe finding (A-04): a reel opened from a DM shows the sender and a reply bar (R-15).
        return !anyVisible(root, "sender_username_or_fullname") && !anyVisible(root, "reply_bar_edittext");
    }

    private static boolean anyVisible(AccessibilityNodeInfo root, String viewId) {
        for (AccessibilityNodeInfo node : root.findAccessibilityNodeInfosByViewId(INSTAGRAM + ":id/" + viewId)) {
            if (node.isVisibleToUser()) return true;
        }
        return false;
    }

    private static boolean isInstagram(CharSequence packageName) {
        return packageName != null && INSTAGRAM.contentEquals(packageName);
    }

    /** Tracks Reels watching time for the session log (R-09). */
    private void setWatching(boolean watching, long now) {
        if (watching) {
            if (watchingSince == 0) watchingSince = now;
            return;
        }
        if (watchingSince == 0) return;
        SessionPolicy policy = store.policy();
        long end = Math.min(now, policy.sessionEnd());
        if (end > watchingSince) store.addReelsTime(policy.sessionStart, end - watchingSince);
        watchingSince = 0;
        handler.removeCallbacks(warn);
    }

    private void scheduleTimers(SessionPolicy policy, long now) {
        handler.removeCallbacks(sessionEnd);
        handler.postDelayed(sessionEnd, policy.sessionEnd() - now);
        handler.removeCallbacks(warn);
        long warnAt = policy.sessionEnd() - WARN_BEFORE_MS;
        if (warnAt > policy.sessionStart && warnAt > now && warnedSession != policy.sessionStart) {
            handler.postDelayed(warn, warnAt - now);
        }
    }

    private void onWarn() {
        if (watchingSince == 0) return;
        warnedSession = store.policy().sessionStart;
        Toast.makeText(this, "2 min left in this Reels session", Toast.LENGTH_LONG).show();
    }

    private void onSessionEnd() {
        long now = System.currentTimeMillis();
        if (watchingSince != 0) {
            setWatching(false, now);
            showSessionOver(store.policy());
            lastToastAt = now; // The card replaces the lock toast.
        }
        handler.removeCallbacks(evaluate);
        evaluatePending = false;
        evaluate();
    }

    private void exitReels(SessionPolicy policy, long now) {
        long sinceExit = now - lastExitAt;
        if (sinceExit < RETRY_MS) {
            scheduleEvaluate(RETRY_MS - sinceExit);
            return;
        }
        boolean idle = policy.state(now) == SessionPolicy.State.IDLE;
        if (idle) {
            if (card == null) showStartPrompt();
        } else if (now - lastToastAt >= TOAST_GAP_MS) { // N-01
            lastToastAt = now;
            Toast.makeText(this, "Reels locked · back at " + Ui.clock(this, policy.cooldownEnd())
                    + " (" + Ui.duration(policy.cooldownEnd() - now) + ")", Toast.LENGTH_SHORT).show();
        }
        // N-02: Back up to 3 times, then restart Instagram at its home feed, then the phone's home screen.
        if (exitAttempts < 3) {
            performGlobalAction(GLOBAL_ACTION_BACK);
        } else if (exitAttempts > 3 || !openInstagramHome()) {
            performGlobalAction(GLOBAL_ACTION_HOME);
        }
        exitAttempts++;
        lastExitAt = now;
        scheduleEvaluate(RETRY_MS);
    }

    private boolean openInstagramHome() {
        Intent intent = getPackageManager().getLaunchIntentForPackage(INSTAGRAM);
        if (intent == null) return false;
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        try {
            startActivity(intent);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** N-03: "Session over" card. */
    private void showSessionOver(SessionPolicy policy) {
        String body = "Reels is back at " + Ui.clock(this, policy.cooldownEnd()) + ".";
        if (!showCard("Session over", body, CARD_MS)) {
            Toast.makeText(this, "Session over. " + body, Toast.LENGTH_LONG).show(); // Spec §4.2 fallback.
        }
    }

    /** R-16: Reels was opened while Idle; a session starts only on an explicit Start. */
    private void showStartPrompt() {
        int x = store.limitX();
        Context themed = new ContextThemeWrapper(this, android.R.style.Theme_DeviceDefault_Light);
        Button notNow = new Button(themed, null, android.R.attr.borderlessButtonStyle);
        notNow.setText("Not now");
        notNow.setOnClickListener(v -> hideCard());
        Button start = new Button(themed);
        start.setText("Start");
        start.setOnClickListener(v -> {
            hideCard();
            SessionPolicy policy = store.policy();
            if (policy.start(System.currentTimeMillis(), x, store.limitY())) store.save(policy);
            Toast.makeText(this, "Session started. Open Reels again.", Toast.LENGTH_SHORT).show();
        });
        if (!showCard("Start a " + x + "-min Reels session?", "Then open Reels again.", PROMPT_MS, notNow, start)) {
            Toast.makeText(this, "Open Anti-Doom-Scroll to start a Reels session.", Toast.LENGTH_LONG).show();
        }
    }

    /** Shows a white card at the bottom of the screen over Instagram. Returns false if the overlay failed. */
    private boolean showCard(String heading, String body, long hideAfterMs, Button... actions) {
        hideCard();
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = Ui.dp(this, 16);
        box.setPadding(pad, pad, pad, pad);
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.WHITE);
        background.setCornerRadius(Ui.dp(this, 16));
        box.setBackground(background);
        box.setElevation(Ui.dp(this, 6));
        TextView title = new TextView(this);
        title.setText(heading);
        title.setTextColor(0xFF1B1B1F);
        title.setTextSize(16);
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        TextView text = new TextView(this);
        text.setText(body);
        text.setTextColor(0xFF5F5F6B);
        text.setTextSize(14);
        box.addView(title);
        box.addView(text);
        if (actions.length > 0) {
            LinearLayout buttons = new LinearLayout(this);
            buttons.setGravity(Gravity.END);
            for (Button b : actions) {
                b.setMinHeight(Ui.dp(this, 48));
                buttons.addView(b);
            }
            box.addView(buttons);
        }

        FrameLayout wrap = new FrameLayout(this);
        wrap.setPadding(Ui.dp(this, 12), 0, Ui.dp(this, 12), Ui.dp(this, 32));
        wrap.addView(box);
        wrap.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        wrap.setOnClickListener(v -> hideCard());

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.BOTTOM;
        try {
            getSystemService(WindowManager.class).addView(wrap, params);
            card = wrap;
            handler.postDelayed(hideCard, hideAfterMs);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private void hideCard() {
        handler.removeCallbacks(hideCard);
        if (card == null) return;
        getSystemService(WindowManager.class).removeView(card);
        card = null;
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    public void onDestroy() {
        if (store != null) {
            setWatching(false, System.currentTimeMillis());
            unregisterReceiver(screenOff);
        }
        hideCard();
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
