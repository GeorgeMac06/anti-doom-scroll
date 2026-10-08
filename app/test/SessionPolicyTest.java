import dev.antidoomscroll.SessionPolicy;
import dev.antidoomscroll.SessionPolicy.State;

/**
 * Session rules on a fake clock. Real 30- and 90-minute waits make these impractical on the phone.
 * Run by build.ps1 before every build; a failure stops the build.
 */
public class SessionPolicyTest {
    private static final long MIN = 60_000L;
    private static final long T0 = 1_800_000_000_000L;
    private static int failures;

    public static void main(String[] args) {
        firstReelsViewStartsSession();
        sessionEndsIntoCooldownThenIdle();
        laterReelsViewsDoNotExtendSession();
        changesDuringCooldownApplyToNextSession();
        minuteBounds();
        if (failures > 0) {
            System.out.println("SessionPolicyTest: " + failures + " failed");
            System.exit(1);
        }
        System.out.println("SessionPolicyTest: all passed");
    }

    // AC-01: Idle, then opening Reels starts a session lasting X.
    private static void firstReelsViewStartsSession() {
        SessionPolicy p = new SessionPolicy(0, 30, 90);
        check(p.state(T0) == State.IDLE, "starts Idle");
        check(p.onReelsView(T0, 30, 90), "first Reels view allowed");
        check(p.state(T0) == State.IN_SESSION, "session running after first view");
        check(p.sessionEnd() == T0 + 30 * MIN, "session lasts X");
    }

    // AC-04, AC-05, AC-06: Reels blocked from X until X+Y, then a new session can start.
    private static void sessionEndsIntoCooldownThenIdle() {
        SessionPolicy p = new SessionPolicy(T0, 30, 90);
        check(p.onReelsView(T0 + 30 * MIN - 1, 30, 90), "allowed just before X");
        check(p.state(T0 + 30 * MIN) == State.COOLDOWN, "Cooldown at X");
        check(!p.onReelsView(T0 + 30 * MIN, 30, 90), "blocked at X");
        check(!p.onReelsView(T0 + 120 * MIN - 1, 30, 90), "blocked just before X+Y");
        check(p.state(T0 + 120 * MIN) == State.IDLE, "Idle at X+Y");
        check(p.onReelsView(T0 + 120 * MIN, 30, 90), "new session allowed at X+Y");
        check(p.sessionStart == T0 + 120 * MIN, "new session starts at X+Y");
    }

    // D-01: wall-clock session; returning to Reels mid-session must not restart the timer.
    private static void laterReelsViewsDoNotExtendSession() {
        SessionPolicy p = new SessionPolicy(T0, 30, 90);
        check(p.onReelsView(T0 + 20 * MIN, 30, 90), "allowed mid-session");
        check(p.sessionStart == T0, "mid-session view keeps the original start");
        check(p.state(T0 + 30 * MIN) == State.COOLDOWN, "session still ends at the original X");
    }

    // AC-08, AC-12, D-03: new limits wait for the cooldown to end, then apply to the next session.
    private static void changesDuringCooldownApplyToNextSession() {
        SessionPolicy p = new SessionPolicy(T0, 30, 90);
        check(!p.onReelsView(T0 + 40 * MIN, 10, 1), "still blocked after Y is lowered");
        check(p.cooldownEnd() == T0 + 120 * MIN, "current cooldown keeps its end time");
        check(p.onReelsView(T0 + 120 * MIN, 10, 1), "next session allowed");
        check(p.sessionEnd() == T0 + 130 * MIN, "next session uses the new X");
        check(p.cooldownEnd() == T0 + 131 * MIN, "next session uses the new Y");
    }

    // R-08: allowed range 1 to 180 minutes.
    private static void minuteBounds() {
        check(!SessionPolicy.isValidMinutes(0), "0 rejected");
        check(SessionPolicy.isValidMinutes(1), "1 accepted");
        check(SessionPolicy.isValidMinutes(180), "180 accepted");
        check(!SessionPolicy.isValidMinutes(181), "181 rejected");
    }

    private static void check(boolean ok, String what) {
        if (!ok) {
            failures++;
            System.out.println("FAIL: " + what);
        }
    }
}
