package dev.antidoomscroll;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/** Session length and cooldown (J5, R-08, R-14). */
public class SettingsActivity extends Activity {
    private static final int STEP = 5;
    private static final String RANGE_ERROR = "Enter a number from 1 to 180.";

    private Store store;
    private TextView banner;
    private Limit session;
    private Limit cooldown;
    private TextView hint;
    private Button save;

    /** One stepper row: − / number field / +, plus its "Pending" line. */
    private final class Limit {
        final EditText field;
        final Button minus;
        final Button plus;
        final TextView pending;

        Limit(LinearLayout parent, String label, String spokenName, int initial) {
            LinearLayout row = new LinearLayout(SettingsActivity.this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            TextView name = Ui.text(SettingsActivity.this, label, 16);
            row.addView(name, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            minus = Ui.textButton(SettingsActivity.this, "−", v -> step(-STEP));
            minus.setContentDescription("Decrease " + spokenName);
            field = new EditText(SettingsActivity.this);
            field.setId(View.generateViewId());
            field.setInputType(InputType.TYPE_CLASS_NUMBER);
            field.setEms(3);
            field.setGravity(Gravity.CENTER);
            field.setText(String.valueOf(initial));
            name.setLabelFor(field.getId());
            plus = Ui.textButton(SettingsActivity.this, "+", v -> step(STEP));
            plus.setContentDescription("Increase " + spokenName);
            row.addView(minus);
            row.addView(field);
            row.addView(plus);
            parent.addView(row, Ui.spaced(SettingsActivity.this, 8));

            pending = Ui.secondary(SettingsActivity.this, "");
            pending.setTextColor(Ui.themeColor(SettingsActivity.this, android.R.attr.colorAccent));
            parent.addView(pending);

            field.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
                @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
                @Override public void afterTextChanged(Editable s) { refresh(); }
            });
            field.setOnFocusChangeListener((v, hasFocus) -> {
                if (!hasFocus) validate();
            });
        }

        /** The entered value, or -1 when empty or not a number. */
        int value() {
            try {
                return Integer.parseInt(field.getText().toString().trim());
            } catch (NumberFormatException e) {
                return -1;
            }
        }

        boolean validate() {
            boolean ok = SessionPolicy.isValidMinutes(value());
            field.setError(ok ? null : RANGE_ERROR);
            return ok;
        }

        void step(int delta) {
            int start = SessionPolicy.isValidMinutes(value()) ? value() : SessionPolicy.MIN_MINUTES;
            int next = Math.max(SessionPolicy.MIN_MINUTES, Math.min(SessionPolicy.MAX_MINUTES, start + delta));
            field.setText(String.valueOf(next));
            field.setError(null);
        }

        void refreshButtons() {
            int v = value();
            minus.setEnabled(v > SessionPolicy.MIN_MINUTES);
            plus.setEnabled(v < SessionPolicy.MAX_MINUTES);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        store = new Store(this);
        LinearLayout content = Ui.screen(this);

        banner = Ui.text(this, "", 14);
        int pad = Ui.dp(this, 12);
        banner.setPadding(pad, pad, pad, pad);
        int accent = Ui.themeColor(this, android.R.attr.colorAccent);
        banner.setBackground(Ui.outline(this, (accent & 0x00FFFFFF) | 0x24000000));
        content.addView(banner);

        session = new Limit(content, "Session length", "session length", store.limitX());
        cooldown = new Limit(content, "Cooldown", "cooldown", store.limitY());
        hint = Ui.secondary(this, "");
        content.addView(hint, Ui.spaced(this, 8));
        save = Ui.button(this, "Save", v -> save());
        content.addView(save, Ui.spaced(this, 24));
        refresh();
    }

    private void save() {
        boolean ok = session.validate() & cooldown.validate();
        if (!ok) return;
        store.setLimits(session.value(), cooldown.value());
        Toast.makeText(this, "Saved.", Toast.LENGTH_SHORT).show();
        refresh();
    }

    private void refresh() {
        session.refreshButtons();
        cooldown.refreshButtons();
        save.setEnabled(session.value() != store.limitX() || cooldown.value() != store.limitY());

        SessionPolicy policy = store.policy();
        long now = System.currentTimeMillis();
        boolean inCooldown = policy.state(now) == SessionPolicy.State.COOLDOWN;
        banner.setVisibility(inCooldown ? View.VISIBLE : View.GONE);
        banner.setText("🔒 Locked until the cooldown ends at " + Ui.clock(this, policy.cooldownEnd())
                + ". Changes will apply then.");
        showPending(session.pending, inCooldown, store.limitX(), policy.sessionX);
        showPending(cooldown.pending, inCooldown, store.limitY(), policy.sessionY);
        hint.setText("Minutes, 1–180. " + (inCooldown ? "Changes apply after the cooldown."
                : "Applies from the next session."));
    }

    private static void showPending(TextView view, boolean inCooldown, int saved, int current) {
        boolean show = inCooldown && saved != current;
        view.setVisibility(show ? View.VISIBLE : View.GONE);
        view.setText("Pending: " + saved + " min (now " + current + ")");
    }
}
