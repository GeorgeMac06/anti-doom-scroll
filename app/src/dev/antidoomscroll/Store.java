package dev.antidoomscroll;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/** On-device state: limits, the current session (R-10) and the session log (R-09). Nothing leaves the phone. */
final class Store {
    private static final String TAG = "AntiDoomScroll";
    private static final int DEFAULT_X = 30;
    private static final int DEFAULT_Y = 90;

    private final SharedPreferences prefs;
    private final File log;

    Store(Context context) {
        prefs = context.getSharedPreferences("state", Context.MODE_PRIVATE);
        log = new File(context.getFilesDir(), "sessions.csv");
    }

    int limitX() {
        return prefs.getInt("x", DEFAULT_X);
    }

    int limitY() {
        return prefs.getInt("y", DEFAULT_Y);
    }

    void setLimits(int x, int y) {
        prefs.edit().putInt("x", x).putInt("y", y).apply();
    }

    SessionPolicy policy() {
        return new SessionPolicy(prefs.getLong("sessionStart", 0),
                prefs.getInt("sessionX", DEFAULT_X), prefs.getInt("sessionY", DEFAULT_Y));
    }

    /** Written synchronously so a session survives an immediate app kill or restart (R-10). */
    void save(SessionPolicy policy) {
        prefs.edit()
                .putLong("sessionStart", policy.sessionStart)
                .putInt("sessionX", policy.sessionX)
                .putInt("sessionY", policy.sessionY)
                .commit();
    }

    boolean onboarded() {
        return prefs.getBoolean("onboarded", false);
    }

    void setOnboarded() {
        prefs.edit().putBoolean("onboarded", true).apply();
    }

    /** Adds Reels watching time to the session's log row. Rows are "sessionStart,reelsMs". */
    void addReelsTime(long sessionStart, long ms) {
        List<long[]> rows = sessions();
        long[] last = rows.isEmpty() ? null : rows.get(rows.size() - 1);
        if (last != null && last[0] == sessionStart) {
            last[1] += ms;
        } else {
            rows.add(new long[] {sessionStart, ms});
        }
        StringBuilder out = new StringBuilder();
        for (long[] row : rows) out.append(row[0]).append(',').append(row[1]).append('\n');
        try {
            Files.write(log.toPath(), out.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            Log.w(TAG, "Could not write session log", e);
        }
    }

    /** Sessions started at or after {@code since}, oldest first, as {sessionStart, reelsMs}. */
    List<long[]> sessionsSince(long since) {
        List<long[]> result = new ArrayList<>();
        for (long[] row : sessions()) if (row[0] >= since) result.add(row);
        return result;
    }

    private List<long[]> sessions() {
        List<long[]> rows = new ArrayList<>();
        if (!log.exists()) return rows;
        try {
            for (String line : Files.readAllLines(log.toPath(), StandardCharsets.UTF_8)) {
                String[] parts = line.split(",");
                if (parts.length == 2) rows.add(new long[] {Long.parseLong(parts[0]), Long.parseLong(parts[1])});
            }
        } catch (IOException | NumberFormatException e) {
            Log.w(TAG, "Could not read session log", e);
        }
        return rows;
    }
}
