package noppes.npcs.ai;

import net.minecraft.world.phys.Vec3;

/** Angle helpers from 1.12.2 {@code Util.getAngles3D} / {@code Util.getPosition} (RayTraceRotate/Vec). */
public final class NpcMath {

    private NpcMath() {}

    /**
     * {yaw, pitch} in degrees from {@code from} towards {@code to}, 1.12.2 convention: yaw 0 is +z,
     * 90 is -x, in [0, 360); pitch is negative upwards. Equal points give {0, 0}.
     */
    public static double[] yawPitch(Vec3 from, Vec3 to) {
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double dz = to.z - from.z;
        double radiusXZ = Math.sqrt(dx * dx + dz * dz);
        double rad = 180.0d / Math.PI;
        double pitch = -Math.atan(dy / radiusXZ) * rad;
        if (Double.isNaN(pitch)) {
            pitch = 0.0d;
        }
        pitch = Math.clamp(pitch, -90.0d, 90.0d);
        double yaw;
        if (dx == 0.0d) {
            yaw = from.z > to.z ? 180.0d : 0.0d;
        } else {
            double v = Math.atan(dz / dx) * rad;
            yaw = dx <= 0.0d ? 90.0d + v : 270.0d + v;
        }
        if (Double.isNaN(yaw)) {
            yaw = 0.0d;
        }
        yaw %= 360.0d;
        if (yaw < 0.0d) {
            yaw += 360.0d;
        }
        return new double[] {yaw, pitch};
    }

    /**
     * The point {@code distance} away from {@code origin} along yaw/pitch, as 1.12.2 computes it:
     * the full distance horizontally, plus {@code sin(pitch) * distance} vertically.
     */
    public static Vec3 pointAt(Vec3 origin, double yaw, double pitch, double distance) {
        double d = Math.abs(distance);
        return new Vec3(origin.x - Math.sin(yaw * Math.PI / 180.0d) * d,
                origin.y + Math.sin(pitch * Math.PI / 180.0d) * d,
                origin.z + Math.cos(yaw * Math.PI / 180.0d) * d);
    }
}
