package noppes.npcs.ai.attack;

import java.util.EnumSet;
import javax.annotation.Nullable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import noppes.npcs.entity.EntityCustomNpc;

/**
 * CanLeap, from 1.12.2 {@code EntityAIPounceTarget}: now and then leaps at a visible target that is
 * between 4 and 8 blocks away (box test), and holds movement until it lands.
 */
public final class PounceTargetGoal extends Goal {

    private static final float LEAP_SPEED = 1.3f;

    private final EntityCustomNpc npc;
    @Nullable
    private LivingEntity leapTarget;

    public PounceTargetGoal(EntityCustomNpc npc) {
        this.npc = npc;
        setFlags(EnumSet.of(Goal.Flag.MOVE)); // 1.12.2 mutex 4 (PATHING)
    }

    /** 1.12.2 {@code getAngleForXYZ}: launch angle in degrees, g = 0.1. */
    static float launchAngle(double y, double horizontalDist) {
        float g = 0.1f;
        float s2 = LEAP_SPEED * LEAP_SPEED;
        double a = g * horizontalDist;
        double b = g * horizontalDist * horizontalDist + 2.0d * y * s2;
        double c = s2 * s2 - g * b;
        if (c < 0.0d) {
            return 90.0f;
        }
        float d = s2 - (float) Math.sqrt(c);
        return (float) Math.atan2(d, a) * 180.0f / (float) Math.PI;
    }

    @Override
    public boolean canUse() {
        if (!npc.onGround()) {
            return false;
        }
        leapTarget = npc.getTarget();
        return leapTarget != null && npc.canSee(leapTarget) && !npc.isInRange(leapTarget, 4.0d)
                && npc.isInRange(leapTarget, 8.0d) && npc.getRandom().nextInt(5) == 0;
    }

    @Override
    public boolean canContinueToUse() {
        return !npc.onGround();
    }

    @Override
    public void start() {
        double x = leapTarget.getX() - npc.getX();
        double y = leapTarget.getBoundingBox().minY - npc.getBoundingBox().minY;
        double z = leapTarget.getZ() - npc.getZ();
        float angle = launchAngle(y, Math.sqrt(x * x + z * z));
        float yaw = (float) Math.atan2(x, z) * 180.0f / (float) Math.PI;
        float pi = (float) Math.PI;
        npc.setDeltaMovement(
                Math.sin(yaw / 180.0f * pi) * Math.cos(angle / 180.0f * pi) * LEAP_SPEED,
                Math.sin((angle + 1.0f) / 180.0f * pi) * LEAP_SPEED,
                Math.cos(yaw / 180.0f * pi) * Math.cos(angle / 180.0f * pi) * LEAP_SPEED);
        npc.hurtMarked = true;
    }
}
