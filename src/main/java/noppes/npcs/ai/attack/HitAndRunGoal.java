package noppes.npcs.ai.attack;

import javax.annotation.Nullable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import noppes.npcs.ai.NpcMath;
import noppes.npcs.entity.EntityCustomNpc;

/**
 * Tactic HIT_AND_RUN, from 1.12.2 {@code EntityAIHitAndRun}: after each hit it runs to a spot away
 * from the target with no target set (so the target goals pick nobody new), then takes the target
 * back once there or stuck.
 *
 * <p>Differences from 1.12.2, which could never take the target back (the goal stopped as soon as
 * the target was cleared, leaving {@code canNewAttack()} false for good): the goal keeps running
 * while it runs, restores the target when the run ends, and forgets the run when stopped. Its
 * "path still leads to the run spot" test uses the intended {@code ||} (1.12.2's {@code &&} test
 * was never true), and {@code clearTarget()} only restores a target it took away.
 */
public final class HitAndRunGoal extends AbstractTacticGoal {

    @Nullable
    private int[] runPos;
    @Nullable
    private LivingEntity targetIn;

    public HitAndRunGoal(EntityCustomNpc npc) {
        super(npc);
    }

    @Override
    public boolean canUse() {
        return runPos != null || super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        return npc.isAlive() && (runPos != null || super.canContinueToUse());
    }

    @Override
    public void stop() {
        super.stop();
        runPos = null;
        targetIn = null;
    }

    @Override
    public void tick() {
        if (runPos != null && npc.getTarget() == null) {
            // Running with the target cleared: take it back once there, stuck, or the target is gone.
            if (npc.tickCount % (TICK_RATE * 2) == 0
                    && (npc.getNavigation().isDone() || targetIn == null || !targetIn.isAlive())) {
                clearTarget();
            }
            return;
        }
        super.tick();
        if (target == null || isFriend || npc.tickCount % (TICK_RATE * 2) != 0) {
            return;
        }
        canSeeToAttack = npc.canSee(target);
        if (canSeeToAttack && distance <= range) {
            if (inMove) {
                if (runPos == null) {
                    clearTarget();
                } else {
                    Path path = npc.getNavigation().getPath();
                    Node point = path == null ? null : path.getEndNode();
                    if (point == null || Math.abs(point.x - runPos[0]) > 2 || Math.abs(point.y - runPos[1]) > 2
                            || Math.abs(point.z - runPos[2]) > 2) {
                        clearTarget();
                    }
                }
            }
        } else if (!inMove) {
            clearTarget();
            tryMoveToTarget();
        }
        tryToCauseDamage();
        if (hasAttack) {
            startRun();
        }
    }

    private void startRun() {
        hasAttack = false;
        runPos = null;
        Vec3 vec = null;
        if (!isRanged() || distance < tacticalRange) {
            // 1.12.2 looks behind the target first, but only uses a random spot when that fails.
            Vec3 behind = NpcMath.pointAt(target.position(), target.getYRot() + 180.0f, target.getXRot(), tacticalRange);
            if (npc.getNavigation().createPath(behind.x, behind.y, behind.z, 0) == null) {
                vec = DefaultRandomPos.getPos(npc, tacticalRange, 2);
            }
        }
        if (vec == null) {
            vec = DodgeGoal.findRetreat(npc, tacticalRange, range, isRanged());
        }
        if (vec != null) {
            npc.getNavigation().moveTo(vec.x, vec.y, vec.z, 1.3d);
            runPos = new int[] {(int) Math.floor(vec.x), (int) Math.floor(vec.y), (int) Math.floor(vec.z)};
            targetIn = target;
            npc.setTarget(null);
        }
    }

    /** Ends a run: gives the target back and stops moving. Outside a run it only stops moving. */
    private void clearTarget() {
        runPos = null;
        if (targetIn != null) {
            npc.setTarget(targetIn);
        }
        targetIn = null;
        npc.getNavigation().stop();
    }

    @Override
    public boolean canNewAttack() {
        return runPos == null;
    }
}
