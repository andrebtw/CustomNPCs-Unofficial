package noppes.npcs.ai.movement;

import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;
import noppes.npcs.entity.EntityCustomNpc;

/**
 * OnAttack 1 (panic), from 1.12.2 {@code EntityAINpcPanic}: while it has a target (or burns) it keeps
 * running to random spots nearby. 1.12.2 mutex PASSIVE maps to MOVE here.
 *
 * <p>1.12.2 kept the goal running while the path was done ({@code noPath() || !isMoving()}), which
 * froze the NPC on its first spot for the rest of the fight; this keeps it while the path runs, as
 * vanilla panic does, so it picks a new spot each time it arrives.
 */
public final class NpcPanicGoal extends Goal {

    private final EntityCustomNpc npc;
    private final double speed;
    private Vec3 runTo = Vec3.ZERO;

    public NpcPanicGoal(EntityCustomNpc npc, double speed) {
        this.npc = npc;
        this.speed = speed;
        setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (npc.getTarget() == null && !npc.isOnFire()) {
            return false;
        }
        Vec3 vec = DefaultRandomPos.getPos(npc, 5, 4);
        if (vec == null) {
            return false;
        }
        runTo = vec;
        return true;
    }

    @Override
    public void start() {
        npc.getNavigation().moveTo(runTo.x, runTo.y, runTo.z, speed);
    }

    @Override
    public boolean canContinueToUse() {
        return npc.getTarget() != null && !npc.getNavigation().isDone();
    }
}
