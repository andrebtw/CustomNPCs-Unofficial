package noppes.npcs.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The DirectLOS math from 1.12.2 {@code EntityNPCInterface.canEntityBeSeen}. */
class NpcSightTest {

    private static final double EPS = 1e-9;

    @Test
    void viewConeIsSixtyDegreesEachWay() {
        assertTrue(NpcSight.inViewCone(0, 0, 0, 0), "straight ahead");
        assertTrue(NpcSight.inViewCone(0, 0, 300, 0), "60 to the right is still in");
        assertTrue(NpcSight.inViewCone(0, 0, 60, 0), "60 to the left is still in");
        assertFalse(NpcSight.inViewCone(0, 0, 299, 0));
        assertFalse(NpcSight.inViewCone(0, 0, 61, 0));
        assertFalse(NpcSight.inViewCone(90, 0, 270, 0), "behind");
        assertTrue(NpcSight.inViewCone(350, 0, 20, 0), "wraps around 0");
        assertTrue(NpcSight.inViewCone(-10, 0, 20, 0), "negative head yaw wraps too");
        assertFalse(NpcSight.inViewCone(0, 0, 0, 61), "far below the look direction");
        assertFalse(NpcSight.inViewCone(0, 0, 0, -61), "far above");
        assertTrue(NpcSight.inViewCone(0, 30, 0, -30), "60 apart vertically is still in");
    }

    @Test
    void closeTargetsAreAlwaysSensedUnlessSneaking() {
        // aggro 16: the close radius is aggro / 4 = 4 (at least 3)
        assertEquals(1.0, NpcSight.senseChance(0, 16, false), EPS);
        assertEquals(1.0, NpcSight.senseChance(3.9, 16, false), EPS);
        assertEquals(1.0 / 3.0, NpcSight.senseChance(0, 16, true), EPS);
        assertEquals((0.9 * 2 / 4 + 1) / 3, NpcSight.senseChance(2, 16, true), EPS);
        assertEquals((0.9 * 2 / 3 + 1) / 3, NpcSight.senseChance(2, 8, true), EPS, "radius at least 3");
    }

    @Test
    void farTargetsAreSensedRarelyAndNeverSneaking() {
        double d2 = 0.1 / (4 - 16.0);
        double d3 = 0.1 - d2 * 4;
        assertEquals((d2 * 4 + d3) / 10, NpcSight.senseChance(4, 16, false), EPS);
        assertEquals(0.01, NpcSight.senseChance(4, 16, false), EPS);
        assertEquals((d2 * 10 + d3) / 10, NpcSight.senseChance(10, 16, false), EPS);
        assertEquals(0.0, NpcSight.senseChance(16, 16, false), EPS);
        assertEquals(0.0, NpcSight.senseChance(6, 16, true), EPS, "sneaking far away: never");
    }

    @Test
    void invisibleChanceFallsWithDistanceAndSneaking() {
        double base = -0.00026 + 0.00489 - 0.03166 + 0.08; // amplifier 0 -> invisible 1
        assertEquals(base, NpcSight.invisibleChance(0, 0, 16, false), EPS);
        assertEquals(base * 0.5, NpcSight.invisibleChance(0, 8, 16, false), EPS);
        assertEquals(base * 0.5 * 0.3, NpcSight.invisibleChance(0, 8, 16, true), EPS);
        assertEquals(0.0, NpcSight.invisibleChance(0, 16, 16, false), EPS);
        double i = 3; // amplifier 2
        double strong = -0.00026 * i * i * i + 0.00489 * i * i - 0.03166 * i + 0.08;
        assertEquals(strong, NpcSight.invisibleChance(2, 0, 16, false), EPS);
        assertEquals(0.002, NpcSight.invisibleChance(20, 0, 16, false), EPS, "never below 0.002 before distance");
    }
}
