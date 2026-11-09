package noppes.npcs.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ThreatMathTest {

    private static final double EPS = 1e-9;

    @Test
    void threatMatchesTable() {
        assertEquals(4.0, ThreatMath.threat(4, 4, false, false, false, false), EPS);
        assertEquals(4 * (0.009259 * 20 + 0.953704), ThreatMath.threat(4, 20, false, false, false, false), EPS);
        assertEquals(4.0, ThreatMath.threat(4, 32, false, false, false, false), EPS, "32 is outside (5, 32)");
        assertEquals(4.0, ThreatMath.threat(4, 5, false, false, false, false), EPS, "5 is outside (5, 32)");
        assertEquals(4 * 1.1, ThreatMath.threat(4, 4, true, false, false, false), EPS);
        assertEquals(4 * 1.2, ThreatMath.threat(4, 4, false, true, true, false), EPS, "shield wins over projectile");
        assertEquals(4 * 1.025, ThreatMath.threat(4, 4, false, false, true, false), EPS);
        assertEquals(4 * 1.05, ThreatMath.threat(4, 4, false, false, false, true), EPS);
        assertEquals(0.25, ThreatMath.threat(0, 4, false, false, false, false), EPS);
        assertEquals(0.25, ThreatMath.threat(-3, 4, false, false, false, false), EPS);
        assertEquals(4 * 1.1 * 1.2 * 1.05, ThreatMath.threat(4, 4, true, true, false, true), EPS);
    }
}
