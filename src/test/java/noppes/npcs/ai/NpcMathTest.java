package noppes.npcs.ai;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class NpcMathTest {

    private static void assertVec(Vec3 expected, Vec3 actual) {
        assertEquals(expected.x, actual.x, 1e-6, "x");
        assertEquals(expected.y, actual.y, 1e-6, "y");
        assertEquals(expected.z, actual.z, 1e-6, "z");
    }

    @Test
    void horizontalRoundTrip() {
        Vec3 a = new Vec3(10, 64, -3);
        for (Vec3 b : new Vec3[] {new Vec3(15, 64, -3), new Vec3(5, 64, 2), new Vec3(10, 64, 7),
                new Vec3(10, 64, -9), new Vec3(-4, 64, -20)}) {
            double[] yp = NpcMath.yawPitch(a, b);
            assertVec(b, NpcMath.pointAt(a, yp[0], yp[1], a.distanceTo(b)));
        }
    }

    @Test
    void yawFollows112Convention() {
        Vec3 o = Vec3.ZERO;
        assertEquals(0.0, NpcMath.yawPitch(o, new Vec3(0, 0, 1))[0], 1e-9, "+z is 0");
        assertEquals(90.0, NpcMath.yawPitch(o, new Vec3(-1, 0, 0))[0], 1e-9, "-x is 90");
        assertEquals(180.0, NpcMath.yawPitch(o, new Vec3(0, 0, -1))[0], 1e-9, "-z is 180");
        assertEquals(270.0, NpcMath.yawPitch(o, new Vec3(1, 0, 0))[0], 1e-9, "+x is 270");
        assertEquals(-45.0, NpcMath.yawPitch(o, new Vec3(0, 1, 1))[1], 1e-9, "looking up is negative pitch");
    }

    /** 1.12.2 getPosition: horizontal distance is the full radius and y uses +sin(pitch). */
    @Test
    void pointAtMatches112() {
        assertVec(new Vec3(0, -5, 10), NpcMath.pointAt(Vec3.ZERO, 0, -30, 10));
    }

    @Test
    void degenerateIsZeroNotNaN() {
        double[] yp = NpcMath.yawPitch(new Vec3(1, 2, 3), new Vec3(1, 2, 3));
        assertArrayEquals(new double[] {0, 0}, yp);
        assertFalse(Double.isNaN(yp[0]) || Double.isNaN(yp[1]));
    }
}
