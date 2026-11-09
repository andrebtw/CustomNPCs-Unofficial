package noppes.npcs.ai;

/** The threat ("anger") value of one hit, from 1.12.2 {@code CombatHandler.damage}. */
public final class ThreatMath {

    private ThreatMath() {}

    public static double threat(double damage, double distance, boolean attackerIsPlayer,
            boolean attackerHoldsShield, boolean projectileOrMagic, boolean isCurrentTarget) {
        double value = damage <= 0.25d ? 0.25d : damage;
        // Further attackers anger more: +0% at 5 blocks, +25% at 32.
        if (distance > 5 && distance < 32) {
            value *= 0.009259d * distance + 0.953704d;
        }
        if (attackerIsPlayer) {
            value *= 1.1d;
        }
        if (attackerHoldsShield) {
            value *= 1.2d; // a tank
        } else if (projectileOrMagic) {
            value *= 1.025d; // a damage dealer
        }
        if (isCurrentTarget) {
            value *= 1.05d;
        }
        return value;
    }
}
