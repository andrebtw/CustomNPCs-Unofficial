package noppes.npcs.ai.attack;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import noppes.npcs.entity.EntityCustomNpc;

/**
 * Tactic STAGGER, from 1.12.2 {@code EntityAIDodge}: fights like Onslaught but leaps aside after
 * attacking or being hit, and sometimes dodges the hit. The 1000 ms jump cooldown is 20 game ticks.
 */
public final class DodgeGoal extends AbstractTacticGoal {

    static final int JUMP_COOLDOWN = 20;
    /** 1.12.2 config {@code NpcNavRange} default; no config system yet. */
    static final double NAV_RANGE = 32.0d;
    private static final float LEAP_SPEED = 1.3f;

    private long nextJump;

    public DodgeGoal(EntityCustomNpc npc) {
        super(npc);
        nextJump = npc.level().getGameTime() + JUMP_COOLDOWN;
    }

    @Override
    public void tick() {
        super.tick();
        if (isFriend || npc.tickCount % (TICK_RATE * 2) > TICK_RATE) {
            return;
        }
        canSeeToAttack = npc.canSee(target);
        if (!(canSeeToAttack && distance <= range) && !inMove) {
            tryMoveToTarget();
        }
        tryToCauseDamage();
    }

    @Override
    public void attacked() {
        tryJump();
    }

    @Override
    public boolean damaged() {
        return tryJump() && npc.getRandom().nextFloat() < 0.05f;
    }

    private boolean tryJump() {
        long now = npc.level().getGameTime();
        if (nextJump >= now) {
            return false;
        }
        Vec3 point = findRetreat(npc, tacticalRange, range, isRanged(), false);
        if (point == null) {
            return false;
        }
        npc.jumpTowards(LEAP_SPEED, point);
        nextJump = now + JUMP_COOLDOWN;
        return true;
    }

    /**
     * The 1.12.2 retreat search shared by Dodge and HitAndRun: up to 8 random spots within
     * {@code tacticalRange}, stopping after 3 misses in a row, until one is at least
     * {@code tacticalRange} and the attack range away (half of it when ranged) and within aggro range.
     * As in 1.12.2 the last reachable spot is returned even when the loop gave up. With
     * {@code calmdown} (HitAndRun and CalmdownRange) spots further than {@link #NAV_RANGE} from home
     * are skipped.
     */
    @Nullable
    static Vec3 findRetreat(EntityCustomNpc npc, int tacticalRange, double range, boolean ranged, boolean calmdown) {
        double dist = 0.0d;
        int error = 0;
        int attempts = 0;
        Vec3 found = null;
        BlockPos here = npc.blockPosition();
        while ((dist < tacticalRange || dist < (ranged ? range / 2.0d : range) || dist > npc.stats.getAggroRange())
                && error < 3 && attempts < 8) {
            attempts++;
            Vec3 candidate = DefaultRandomPos.getPos(npc, tacticalRange, 2);
            if (candidate == null) {
                error++;
                continue;
            }
            error = 0;
            dist = npc.position().distanceTo(candidate);
            BlockPos home = npc.ai.getStartPos();
            if (calmdown && home != null && candidate.distanceTo(new Vec3(home.getX() + npc.ai.getBodyOffsetX() / 10.0d,
                    home.getY(), home.getZ() + npc.ai.getBodyOffsetZ() / 10.0d)) > NAV_RANGE) {
                continue;
            }
            if (BlockPos.containing(candidate).equals(here)) {
                dist = 0.0d;
                continue;
            }
            Path path = npc.getNavigation().createPath(candidate.x, candidate.y, candidate.z, 0);
            Node end = path == null ? null : path.getEndNode();
            if (end == null || end.asBlockPos().equals(here)) {
                dist = 0.0d;
            } else {
                found = new Vec3(end.x, end.y, end.z);
            }
        }
        return found;
    }
}
