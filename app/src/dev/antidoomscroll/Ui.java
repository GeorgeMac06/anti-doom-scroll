package dev.antidoomscroll;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.GradientDrawable;
import android.text.format.DateFormat;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/** Shared formatting and small view builders. The UI is built in code, so the app needs no layout files. */
final class Ui {
    private Ui() {
    }

    static int dp(Context c, int value) {
        return Math.round(value * c.getResources().getDisplayMetrics().density);
    }

    /** Clock time in the phone's 12h/24h format, e.g. "16:10". */
    static String clock(Context c, long ms) {
        return DateFormat.getTimeFormat(c).format(new Date(ms));
    }

    /** Remaining time rounded up to minutes, e.g. "1 h 12 m". */
    static String duration(long ms) {
        long minutes = Math.max(1, (ms + 59_999) / 60_000);
        return minutes >= 60 ? (minutes / 60) + " h " + (minutes % 60) + " m" : minutes + " m";
    }

    /** Spoken form of {@link #duration} for TalkBack. */
    static String spokenDuration(long ms) {
        long minutes = Math.max(1, (ms + 59_999) / 60_000);
        String m = (minutes % 60) + ((minutes % 60) == 1 ? " minute" : " minutes");
        if (minutes < 60) return m;
        long h = minutes / 60;
        return h + (h == 1 ? " hour " : " hours ") + m;
    }

    /** Countdown such as "24:13" or "1:12:40". */
    static String countdown(long ms) {
        long s = Math.max(0, (ms + 999) / 1000);
        long h = s / 3600;
        return h > 0
                ? String.format(Locale.ROOT, "%d:%02d:%02d", h, s / 60 % 60, s % 60)
                : String.format(Locale.ROOT, "%d:%02d", s / 60, s % 60);
    }

    static long minutes(long ms) {
        return Math.round(ms / 60_000.0);
    }

    static long startOfToday() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    static boolean serviceEnabled(Context c) {
        AccessibilityManager am = c.getSystemService(AccessibilityManager.class);
        for (AccessibilityServiceInfo info : am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)) {
            if (c.getPackageName().equals(info.getResolveInfo().serviceInfo.packageName)) return true;
        }
        return false;
    }

    static int themeColor(Context c, int attr) {
        TypedArray a = c.obtainStyledAttributes(new int[] {attr});
        int color = a.getColor(0, 0xFF888888);
        a.recycle();
        return color;
    }

    /** Wraps content in a padded, scrollable screen and shows it. */
    static LinearLayout screen(Activity activity) {
        LinearLayout content = column(activity);
        int pad = dp(activity, 16);
        content.setPadding(pad, pad, pad, pad);
        ScrollView scroll = new ScrollView(activity);
        scroll.addView(content);
        activity.setContentView(scroll);
        return content;
    }

    static LinearLayout column(Context c) {
        LinearLayout layout = new LinearLayout(c);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    static TextView text(Context c, String value, float sp) {
        TextView view = new TextView(c);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(themeColor(c, android.R.attr.textColorPrimary));
        return view;
    }

    static TextView secondary(Context c, String value) {
        TextView view = text(c, value, 14);
        view.setTextColor(themeColor(c, android.R.attr.textColorSecondary));
        return view;
    }

    static TextView heading(Context c, String value) {
        TextView view = text(c, value, 24);
        view.setAccessibilityHeading(true);
        view.setPadding(0, 0, 0, dp(c, 12));
        return view;
    }

    static Button button(Context c, String label, View.OnClickListener onClick) {
        Button b = new Button(c);
        b.setText(label);
        b.setMinHeight(dp(c, 48));
        b.setOnClickListener(onClick);
        return b;
    }

    static Button textButton(Context c, String label, View.OnClickListener onClick) {
        Button b = new Button(c, null, android.R.attr.borderlessButtonStyle);
        b.setText(label);
        b.setMinHeight(dp(c, 48));
        b.setOnClickListener(onClick);
        return b;
    }

    /** A tappable list row with a chevron, at least 48 dp tall. */
    static TextView row(Context c, String label, View.OnClickListener onClick) {
        TextView view = text(c, label, 16);
        view.setMinHeight(dp(c, 48));
        view.setGravity(android.view.Gravity.CENTER_VERTICAL);
        view.setCompoundDrawablePadding(dp(c, 8));
        view.setOnClickListener(onClick);
        return view;
    }

    /** Rounded outline used for cards and banners; works in light and dark themes. */
    static GradientDrawable outline(Context c, int fill) {
        GradientDrawable d = new GradientDrawable();
        d.setCornerRadius(dp(c, 16));
        d.setStroke(dp(c, 1), 0x55888888);
        d.setColor(fill);
        return d;
    }

    static LinearLayout.LayoutParams spaced(Context c, int topDp) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p.topMargin = dp(c, topDp);
        return p;
    }
}
