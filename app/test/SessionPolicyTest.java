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
        reelsNeedAnExplicitStart();
        sessionEndsIntoCooldownThenIdle();
        startIsIgnoredDuringSessionOrCooldown();
        changesDuringCooldownApplyToNextSession();
        minuteBounds();
        if (failures > 0) {
            System.out.println("SessionPolicyTest: " + failures + " failed");
            System.exit(1);
        }
        System.out.println("SessionPolicyTest: all passed");
    }

    // R-16, AC-01: Idle blocks Reels until Start; an accidental tap costs nothing.
    private static void reelsNeedAnExplicitStart() {
        SessionPolicy p = new SessionPolicy(0, 30, 90);
        check(p.state(T0) == State.IDLE, "starts Idle");
        check(!p.allowsReels(T0), "Idle does not allow Reels");
        check(p.state(T0 + 5 * MIN) == State.IDLE, "an unanswered prompt leaves it Idle");
        check(p.start(T0, 30, 90), "Start starts a session");
        check(p.allowsReels(T0), "Reels allowed after Start");
        check(p.sessionEnd() == T0 + 30 * MIN, "session lasts X");
    }

    // AC-04, AC-05, AC-06: Reels blocked from X until X+Y, then Idle again.
    private static void sessionEndsIntoCooldownThenIdle() {
        SessionPolicy p = new SessionPolicy(T0, 30, 90);
        check(p.allowsReels(T0 + 30 * MIN - 1), "allowed just before X");
        check(p.state(T0 + 30 * MIN) == State.COOLDOWN, "Cooldown at X");
        check(!p.allowsReels(T0 + 30 * MIN), "blocked at X");
        check(!p.allowsReels(T0 + 120 * MIN - 1), "blocked just before X+Y");
        check(p.state(T0 + 120 * MIN) == State.IDLE, "Idle at X+Y");
    }

    // D-01, D-03: a running session or cooldown cannot be restarted or skipped.
    private static void startIsIgnoredDuringSessionOrCooldown() {
        SessionPolicy p = new SessionPolicy(T0, 30, 90);
        check(!p.start(T0 + 20 * MIN, 30, 90), "Start ignored mid-session");
        check(p.sessionStart == T0, "mid-session Start keeps the original start");
        check(!p.start(T0 + 40 * MIN, 30, 90), "Start ignored during Cooldown");
        check(p.cooldownEnd() == T0 + 120 * MIN, "Cooldown keeps its end time");
    }

    // AC-08, AC-12, D-03: new limits wait for the cooldown to end, then apply to the next session.
    private static void changesDuringCooldownApplyToNextSession() {
        SessionPolicy p = new SessionPolicy(T0, 30, 90);
        check(!p.start(T0 + 40 * MIN, 10, 1), "lowered limits do not end the cooldown");
        check(p.start(T0 + 120 * MIN, 10, 1), "next session can start");
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
