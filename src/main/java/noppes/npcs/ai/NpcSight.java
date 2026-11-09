package noppes.npcs.ai;

/**
 * The DirectLOS math from 1.12.2 {@code EntityNPCInterface.canEntityBeSeen}: the view cone of the
 * WARY/CALM/REALISTIC modes, the chance to sense a target outside it (WARY/REALISTIC), and the
 * chance to notice an invisible target. Angles use the 1.12.2/vanilla convention of {@link NpcMath}.
 */
public final class NpcSight {

    /** Half the width of the view cone, horizontally and vertically, in degrees. */
    public static final double CONE = 60.0d;

    private NpcSight() {}

    /**
     * True when the direction {@code yawTo}/{@code pitchTo} is within 60 degrees of where the head looks
     * (yaw and pitch tested separately, as in 1.12.2).
     */
    public static boolean inViewCone(double headYaw, double headPitch, double yawTo, double pitchTo) {
        double yaw = (headYaw - yawTo) % 360.0d;
        double pitch = (headPitch - pitchTo) % 360.0d;
        while (yaw < 0.0d) {
            yaw += 360.0d;
        }
        return !((yaw > CONE && yaw < 360.0d - CONE) || pitch > CONE || pitch < -CONE);
    }

    /**
     * Chance (per check) that a WARY/REALISTIC NPC senses a target outside its view cone: always within
     * a quarter of the aggro range (at least 3) unless the target sneaks, rarely beyond it, never when a
     * far target sneaks.
     */
    public static double senseChance(double distance, double aggroRange, boolean sneaking) {
        double close = clamp(aggroRange / 4.0d, 3.0d, aggroRange);
        double sneak = sneaking ? 3.0d : 1.0d;
        if (distance < close) {
            return clamp((0.9d * distance / close + 1.0d) / sneak, 0.0d, 1.0d);
        }
        if (sneaking) {
            return 0.0d;
        }
        double slope = 0.1d / (4.0d - aggroRange);
        double offset = 0.1d - slope * 4.0d;
        return clamp((slope * distance + offset) / sneak / 10.0d, 0.0d, 1.0d); // 10: per tick
    }

    /**
     * Chance (per check) to notice a target with the invisibility effect at {@code amplifier}: a curve
     * of the effect strength (at least 0.002), falling to 0 at the aggro range, times 0.3 when sneaking.
     */
    public static double invisibleChance(int amplifier, double distance, double aggroRange, boolean sneaking) {
        double invisible = 1.0d + amplifier;
        double chance = clamp(-0.00026d * Math.pow(invisible, 3.0d) + 0.00489d * Math.pow(invisible, 2.0d)
                - 0.03166d * invisible + 0.08d, 0.002d, 1.0d);
        if (chance != 1.0d) {
            chance *= 1.0d - distance / aggroRange;
        }
        if (chance != 1.0d && sneaking) {
            chance *= 0.3d;
        }
        return clamp(chance, 0.0d, 1.0d);
    }

    /** 1.12.2 {@code ValueUtil.correctDouble}: the lower bound first, then the upper one. */
    private static double clamp(double value, double min, double max) {
        return Math.min(Math.max(value, min), max);
    }
}
