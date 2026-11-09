package noppes.npcs.ai.attack;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.faction.Faction;

/**
 * Tactic COMMANDER, from 1.12.2 {@code EntityAICommanderTarget}: before attacking, calls idle NPCs of
 * its faction within TacticalRadius to its side, then sends them all at the target once they have
 * gathered, time runs out, or the target is already close. The 1.12.2 animation calls are skipped.
 */
public final class CommanderGoal extends AbstractTacticGoal {

    private final List<EntityCustomNpc> followers = new ArrayList<>();
    private boolean done = false;
    private int time = 0;
    private double minDist;

    public CommanderGoal(EntityCustomNpc npc) {
        super(npc);
        npc.aiOwnerNpc = null;
    }

    @Override
    public boolean canUse() {
        if (super.canUse()) {
            return true;
        }
        reset();
        return false;
    }

    private void attack() {
        done = true;
        time = 0;
        for (EntityCustomNpc follower : followers) {
            follower.aiOwnerNpc = null;
            follower.setTarget(target);
            if (follower.tacticGoal instanceof CommanderGoal commander) {
                commander.done = true;
            }
        }
        followers.clear();
    }

    private void reset() {
        done = false;
        time = 0;
        for (EntityCustomNpc follower : followers) {
            follower.aiOwnerNpc = null;
            BlockPos start = follower.ai.getStartPos();
            if (follower.ai.isReturnToStart() && start != null) {
                follower.getNavigation().moveTo(start.getX() + follower.ai.getBodyOffsetX() / 10.0d, start.getY(),
                        start.getZ() + follower.ai.getBodyOffsetZ() / 10.0d, 1.3d);
            }
        }
        followers.clear();
    }

    @Override
    public void tick() {
        super.tick();
        if (isFriend || npc.tickCount % (TICK_RATE * 2) != 0) {
            return;
        }
        canSeeToAttack = npc.canSee(target);
        if (done) {
            if (canSeeToAttack && distance <= range) {
                if (inMove) {
                    npc.getNavigation().stop();
                }
            } else {
                tryMoveToTarget();
            }
            tryToCauseDamage();
            return;
        }
        if (canSeeToAttack && distance <= range && distance <= tacticalRange) {
            attack(); // the target is already close
            return;
        }
        if (followers.isEmpty()) {
            recruit();
            if (followers.isEmpty()) {
                attack(); // no friends
                return;
            }
            minDist = followers.size() < 5 ? 3.0d : 0.4d * followers.size() + 1.0d;
            time = tacticalRange < 5 ? 18 : (int) (4.90909f * tacticalRange - 6.54545f);
        } else {
            boolean gathered = true;
            for (EntityCustomNpc follower : followers) {
                if (follower.aiOwnerNpc == null) {
                    follower.aiOwnerNpc = npc;
                }
                float dist = npc.distanceTo(follower);
                if (dist > minDist) {
                    gathered = false;
                    follower.getNavigation().moveTo(npc, 1.0d);
                } else if (dist < 1.5d) {
                    follower.getNavigation().stop();
                }
            }
            time--;
            if (gathered || time <= 0) {
                attack();
            }
        }
    }

    private void recruit() {
        Faction faction = npc.getFaction();
        if (faction == null) {
            return;
        }
        double r = tacticalRange;
        AABB box = new AABB(npc.getX() - r, npc.getY() - r, npc.getZ() - r, npc.getX() + r, npc.getY() + r, npc.getZ() + r);
        for (EntityCustomNpc other : npc.level().getEntitiesOfClass(EntityCustomNpc.class, box,
                e -> npc.distanceTo(e) < tacticalRange)) {
            if (other == npc) {
                continue;
            }
            Faction theirs = other.getFaction();
            if (theirs != null && theirs.id == faction.id && other.getTarget() == null
                    && (other.ai.getOnAttack() == 0 || other.ai.getOnAttack() == 2) && other.aiOwnerNpc == null) {
                Path path = other.getNavigation().createPath(npc, 0);
                if (path != null) {
                    followers.add(other);
                    other.getNavigation().moveTo(path, 1.0d);
                    other.aiOwnerNpc = npc;
                }
            }
        }
    }
}
