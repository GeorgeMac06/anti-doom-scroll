package dev.antidoomscroll;

/**
 * Session and cooldown rules (R-02 to R-06, R-14, D-01, D-03).
 * Pure Java with no Android imports, so test/SessionPolicyTest.java can run it on a PC.
 */
public final class SessionPolicy {
    public enum State { IDLE, IN_SESSION, COOLDOWN }

    public static final int MIN_MINUTES = 1;
    public static final int MAX_MINUTES = 180;
    private static final long MINUTE_MS = 60_000L;

    /** Wall-clock start of the latest session; 0 when there has been none (D-01). */
    public long sessionStart;
    /**
     * X and Y copied when the session started. Settings changed later apply only to the
     * next session, which also covers changes made during Cooldown (R-14, D-03).
     */
    public int sessionX;
    public int sessionY;

    public SessionPolicy(long sessionStart, int sessionX, int sessionY) {
        this.sessionStart = sessionStart;
        this.sessionX = sessionX;
        this.sessionY = sessionY;
    }

    public long sessionEnd() {
        return sessionStart + sessionX * MINUTE_MS;
    }

    public long cooldownEnd() {
        return sessionEnd() + sessionY * MINUTE_MS;
    }

    public State state(long now) {
        if (sessionStart == 0 || now >= cooldownEnd()) return State.IDLE;
        return now < sessionEnd() ? State.IN_SESSION : State.COOLDOWN;
    }

    /**
     * Call when a limited Reels view is on screen. Starts a session with limits x and y when Idle.
     * Returns whether Reels may stay on screen.
     */
    public boolean onReelsView(long now, int x, int y) {
        State state = state(now);
        if (state == State.IDLE) {
            sessionStart = now;
            sessionX = x;
            sessionY = y;
            return true;
        }
        return state == State.IN_SESSION;
    }

    public static boolean isValidMinutes(int minutes) {
        return minutes >= MIN_MINUTES && minutes <= MAX_MINUTES;
    }
}
