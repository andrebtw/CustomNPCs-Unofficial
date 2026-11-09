package noppes.npcs.ai.target;

import javax.annotation.Nullable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import noppes.npcs.entity.EntityCustomNpc;

/**
 * 1.12.2 {@code EntityAIClearTarget}: drops the target when the owner is far away or the combat
 * handler finds nobody worth fighting. No flags (1.12.2 mutex 0), so it never blocks other goals.
 */
public final class NpcClearTargetGoal extends Goal {

    private final EntityCustomNpc npc;
    @Nullable
    private LivingEntity target;

    public NpcClearTargetGoal(EntityCustomNpc npc) {
        this.npc = npc;
    }

    @Override
    public boolean canUse() {
        target = npc.getTarget();
        LivingEntity owner = npc.getOwner();
        return target != null && ((owner != null && !npc.isInRange(owner, npc.stats.getAggroRange() * 2))
                || npc.combat.checkTarget());
    }

    @Override
    public void start() {
        npc.setTarget(null);
        if (target == npc.getLastHurtByMob()) {
            npc.setLastHurtByMob(null);
        }
    }

    @Override
    public void stop() {
        npc.getNavigation().stop();
    }
}
