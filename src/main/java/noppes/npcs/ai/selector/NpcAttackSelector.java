package noppes.npcs.ai.selector;

import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.data.DataAI;
import noppes.npcs.faction.Faction;
import noppes.npcs.faction.FactionChecks;

/**
 * Who an NPC goes after on its own, from 1.12.2 {@code NPCAttackSelector}. Guard/companion jobs are
 * not ported (roles stage). Players are matched as {@link Player}: on a server every real player is a
 * ServerPlayer (1.12.2 checked EntityPlayerMP).
 */
public final class NpcAttackSelector implements Predicate<LivingEntity> {

    private final EntityCustomNpc npc;

    public NpcAttackSelector(EntityCustomNpc npc) {
        this.npc = npc;
    }

    @Override
    public boolean test(LivingEntity entity) {
        if (entity == null || !entity.isAlive() || entity == npc
                || !npc.isInRange(entity, npc.stats.getAggroRange()) || entity.getHealth() < 0.1f) {
            return false;
        }
        if (!npc.canNewAttack() || !npc.mayTarget(entity)) {
            return false;
        }
        return nearHome(entity) && wanted(entity)
                && (npc.ai.getSeeTarget() == DataAI.SEE_TARGET_NONE || npc.canSee(entity));
    }

    /** With ReturnToStart, only targets near home (start position or current path point). */
    private boolean nearHome(LivingEntity entity) {
        if (npc.isFollower() || !npc.ai.isReturnToStart()) {
            return true;
        }
        int allowed = npc.stats.getAggroRange() * 2;
        if (npc.ai.getMovingType() == 1) {
            allowed += npc.ai.getWalkingRange();
        }
        double distanceSq;
        int[] point = npc.ai.getMovingType() == 2 ? npc.ai.getCurrentMovingPath() : null;
        if (point != null) {
            distanceSq = entity.distanceToSqr(point[0], point[1], point[2]);
        } else {
            BlockPos start = npc.ai.getStartPos() == null ? npc.blockPosition() : npc.ai.getStartPos();
            distanceSq = entity.distanceToSqr(start.getX() + npc.ai.getBodyOffsetX() / 10.0d, start.getY(),
                    start.getZ() + npc.ai.getBodyOffsetZ() / 10.0d);
        }
        return distanceSq <= (double) allowed * allowed;
    }

    private boolean wanted(LivingEntity entity) {
        Faction faction = npc.getFaction();
        if (faction == null) {
            return false;
        }
        if (entity instanceof Player player) {
            return !player.isCreative() && !player.isSpectator() && FactionChecks.isAggressiveToPlayer(faction, player);
        }
        if (entity instanceof EntityCustomNpc other && !other.isKilled()) {
            Faction theirs = other.getFaction();
            return theirs != null && FactionChecks.isAggressiveToNpc(faction, npc.advanced.isAttackOtherFactions(),
                    theirs.id, other.advanced.getAttackFactions());
        }
        return false;
    }
}
