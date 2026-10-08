package dev.antidoomscroll;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * First-time setup (J1, R-12). The Android 13+ "restricted settings" step is shown as a hint on the
 * access step, since an app cannot detect it and adb installs skip it (A-02).
 */
public class OnboardingActivity extends Activity {
    static final String EXTRA_STEP = "step";
    static final int STEP_ACCESS = 2;
    private static final int STEP_BATTERY = 3;

    private Store store;
    private LinearLayout content;
    private int step;
    private boolean returnedFromSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        store = new Store(this);
        step = getIntent().getIntExtra(EXTRA_STEP, 0);
        content = Ui.screen(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Always re-check the real permission after returning from system settings.
        if (step == STEP_ACCESS && Ui.serviceEnabled(this)) {
            step = STEP_BATTERY;
            returnedFromSettings = false;
        }
        render();
    }

    @Override
    public void onBackPressed() {
        if (step > 0 && step < STEP_BATTERY) {
            step--;
            returnedFromSettings = false;
            render();
        } else {
            super.onBackPressed();
        }
    }

    private void go(int next) {
        step = next;
        render();
    }

    private void render() {
        content.removeAllViews();
        TextView heading;
        switch (step) {
            case 0:
                heading = Ui.heading(this, "Limit Reels, not Instagram.");
                content.addView(heading);
                content.addView(Ui.text(this, "• " + store.limitX() + "-min Reels sessions, then a "
                        + store.limitY() + "-min break.\n• Feed, Stories and DMs stay untouched.", 16));
                content.addView(Ui.button(this, "Get started", v -> go(1)), Ui.spaced(this, 24));
                break;
            case 1:
                heading = Ui.heading(this, "Why accessibility?");
                content.addView(heading);
                content.addView(Ui.text(this, "To spot when Reels is open, this app reads which Instagram "
                        + "screen is showing.", 16));
                content.addView(Ui.secondary(this, "It reads nothing else. Nothing leaves your phone. "
                        + "The app has no internet access."), Ui.spaced(this, 12));
                content.addView(Ui.button(this, "Continue", v -> go(STEP_ACCESS)), Ui.spaced(this, 24));
                break;
            case STEP_ACCESS:
                heading = Ui.heading(this, "Turn on access");
                content.addView(heading);
                content.addView(Ui.text(this, "In Accessibility, open Anti-Doom-Scroll (under Downloaded "
                        + "or Installed apps) and turn it on.", 16));
                if (returnedFromSettings) {
                    TextView error = Ui.text(this, "Access is still off. Try again.", 16);
                    error.setTextColor(Ui.themeColor(this, android.R.attr.colorError));
                    error.setAccessibilityLiveRegion(TextView.ACCESSIBILITY_LIVE_REGION_POLITE);
                    content.addView(error, Ui.spaced(this, 12));
                }
                content.addView(Ui.button(this, "Open Accessibility settings", v -> {
                    returnedFromSettings = true;
                    startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                }), Ui.spaced(this, 24));
                content.addView(Ui.secondary(this, "Switch greyed out? Open app info, tap ⋮, then "
                        + "Allow restricted settings, and come back."), Ui.spaced(this, 16));
                content.addView(Ui.textButton(this, "Open app info", v -> startActivity(
                        new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", getPackageName(), null)))));
                break;
            default:
                store.setOnboarded();
                heading = Ui.heading(this, "Keep it running");
                content.addView(heading);
                content.addView(Ui.text(this, "Some phones stop background services to save battery. "
                        + "Exempt this app so limits keep working.", 16));
                content.addView(Ui.button(this, "Open battery settings", v -> {
                    startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
                    finish();
                }), Ui.spaced(this, 24));
                content.addView(Ui.textButton(this, "Skip", v -> finish()));
        }
        // Move TalkBack focus to the new step's heading.
        heading.post(() -> heading.performAccessibilityAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS, null));
    }
}
