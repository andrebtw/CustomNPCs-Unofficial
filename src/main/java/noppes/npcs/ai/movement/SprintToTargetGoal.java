package noppes.npcs.ai.movement;

import java.util.EnumSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import noppes.npcs.entity.EntityCustomNpc;

/**
 * CanSprint, from 1.12.2 {@code EntityAISprintToTarget}: sprints while moving to its target (beyond a
 * third of the aggro range when attacking, within it when retreating, always when panicking) until it
 * is hit or leaves the ground. No flags (1.12.2 mutex PASSIVE).
 */
public final class SprintToTargetGoal extends Goal {

    private final EntityCustomNpc npc;

    public SprintToTargetGoal(EntityCustomNpc npc) {
        this.npc = npc;
        setFlags(EnumSet.noneOf(Goal.Flag.class));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = npc.getTarget();
        if (target == null || !target.isAlive() || npc.getNavigation().isDone()) {
            return false;
        }
        return switch (npc.ai.getOnAttack()) {
            case 0 -> !npc.isInRange(target, npc.stats.getAggroRange() / 3.0d);
            case 2 -> npc.isInRange(target, npc.stats.getAggroRange());
            default -> true;
        };
    }

    @Override
    public boolean canContinueToUse() {
        return npc.isAlive() && npc.ai.isCanSprint() && npc.onGround() && npc.hurtTime <= 0;
    }

    @Override
    public void start() {
        npc.setSprinting(true);
    }

    @Override
    public void stop() {
        npc.setSprinting(false);
    }
}
