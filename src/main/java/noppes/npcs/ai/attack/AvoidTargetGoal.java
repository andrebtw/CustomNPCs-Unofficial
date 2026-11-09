package noppes.npcs.ai.attack;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import noppes.npcs.ai.NpcMath;
import noppes.npcs.entity.EntityCustomNpc;

/**
 * OnAttack 2 (retreat), from 1.12.2 {@code EntityAIAvoidTarget}: while the target is within half the
 * aggro range (at least 4), runs straight away from it, or to a random spot when blocked. Never
 * attacks. The run-spot test uses the intended {@code ||} (1.12.2's {@code &&} was never true).
 */
public final class AvoidTargetGoal extends AbstractTacticGoal {

    @Nullable
    private int[] runPos;

    public AvoidTargetGoal(EntityCustomNpc npc) {
        super(npc);
    }

    @Override
    public void tick() {
        super.tick();
        if (target == null || isFriend || npc.tickCount % (TICK_RATE * 2) > 3) {
            return;
        }
        range = npc.stats.getAggroRange() / 2.0d;
        tacticalRange = (int) range;
        if (range < 4) {
            range = 4;
        }
        if (distance >= range) {
            return;
        }
        if (inMove) {
            if (runPos == null) {
                npc.getNavigation().stop();
            } else {
                Path current = npc.getNavigation().getPath();
                Node point = current == null ? null : current.getEndNode();
                if (point == null || Math.abs(point.x - runPos[0]) > 2 || Math.abs(point.y - runPos[1]) > 2
                        || Math.abs(point.z - runPos[2]) > 2) {
                    runPos = null;
                    npc.getNavigation().stop();
                }
            }
            return;
        }
        runPos = null;
        Path path = null;
        Vec3 eyes = npc.getEyePosition();
        double yaw = NpcMath.yawPitch(target.position(), npc.position())[0];
        Vec3 end = NpcMath.pointAt(eyes, yaw, 0.0d, tacticalRange);
        BlockHitResult hit = npc.level().clip(new ClipContext(eyes, end, ClipContext.Block.COLLIDER,
                npc.ai.isAvoidsWater() ? ClipContext.Fluid.ANY : ClipContext.Fluid.NONE, npc));
        BlockPos pos = hit.getBlockPos().relative(hit.getDirection());
        if (Math.sqrt(npc.distanceToSqr(pos.getX(), pos.getY(), pos.getZ())) >= 4) {
            path = npc.getNavigation().createPath(pos, 0);
        }
        if (path == null) {
            Vec3 vec = DefaultRandomPos.getPos(npc, tacticalRange, 2);
            if (vec != null) {
                path = npc.getNavigation().createPath(vec.x, vec.y, vec.z, 0);
            }
        }
        if (path != null) {
            npc.getNavigation().moveTo(path, 1.3d);
            Node point = path.getEndNode();
            if (point != null) {
                runPos = new int[] {point.x, point.y, point.z};
            }
        }
    }

    @Override
    public void stop() {
        super.stop();
        runPos = null;
    }
}
