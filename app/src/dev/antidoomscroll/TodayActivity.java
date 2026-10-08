package dev.antidoomscroll;

import android.app.Activity;
import android.os.Bundle;
import android.widget.LinearLayout;

import java.util.List;

/** Today's sessions and Reels minutes (J4, R-09, N-06). */
public class TodayActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout content = Ui.screen(this);
        List<long[]> sessions = new Store(this).sessionsSince(Ui.startOfToday());
        if (sessions.isEmpty()) {
            content.addView(Ui.secondary(this, "No Reels today."));
            return;
        }
        long totalMs = 0;
        for (int i = sessions.size() - 1; i >= 0; i--) {
            long[] row = sessions.get(i);
            totalMs += row[1];
            content.addView(Ui.text(this, Ui.clock(this, row[0]) + "  ·  " + Ui.minutes(row[1]) + " min of Reels", 16),
                    Ui.spaced(this, 12));
        }
        content.addView(Ui.secondary(this, "Total: " + Ui.minutes(totalMs) + " min of Reels"), Ui.spaced(this, 12));
    }
}
