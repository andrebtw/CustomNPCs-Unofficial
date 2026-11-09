package noppes.npcs.ai.attack;

import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import noppes.npcs.ai.NpcMath;
import noppes.npcs.entity.EntityCustomNpc;

/**
 * Tactic ORBIT, from 1.12.2 {@code EntityAISurround}: keeps {@code min(TacticalRadius, attack range)}
 * from the target, moving to the point at that distance on its own side.
 */
public final class SurroundGoal extends AbstractTacticGoal {

    public SurroundGoal(EntityCustomNpc npc) {
        super(npc);
    }

    @Override
    public void tick() {
        super.tick();
        if (isFriend || npc.tickCount % (TICK_RATE * 2) != 0) {
            return;
        }
        canSeeToAttack = npc.canSee(target);
        double tr = Math.min(tacticalRange, range);
        if (!canSeeToAttack || distance > range) {
            tryMoveToTarget();
        } else if (distance <= tr * 0.9d || distance >= tr * 1.1d) {
            double[] angles = NpcMath.yawPitch(target.position(), npc.position());
            Vec3 pos = NpcMath.pointAt(target.position(), angles[0], angles[1], tr);
            Path path = npc.getNavigation().createPath(pos.x, pos.y, pos.z, 0);
            if (path != null) {
                npc.getNavigation().moveTo(path, 1.3d);
            } else {
                // 1.12.2 passes the offset, not a position, as the point to get away from; kept.
                Vec3 vec = DefaultRandomPos.getPosAway(npc, 2, 2, npc.position().subtract(pos));
                if (vec != null) {
                    npc.getNavigation().moveTo(vec.x, vec.y, vec.z, 1.3d);
                }
            }
        }
        tryToCauseDamage();
    }
}
