package noppes.npcs.ai.attack;

import java.util.EnumSet;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import noppes.npcs.entity.EntityCustomNpc;

/**
 * Base of the attack tactics, from 1.12.2 {@code EntityAICustom}. Melee only: ranged attacks come in
 * stage 2b-4 ({@link #isRanged()} is the seam). Animations and scripted events are not ported.
 */
public abstract class AbstractTacticGoal extends Goal {

    /** 1.12.2 EntityAITasks tick rate; the tactics act every {@code TICK_RATE * 2} ticks. */
    public static final int TICK_RATE = 3;

    protected final EntityCustomNpc npc;
    @Nullable
    protected LivingEntity target;

    public boolean hasAttack;
    public boolean canSeeToAttack;
    public boolean inMove;
    public boolean isFriend;
    protected int tacticalRange;
    protected int meleeTick;
    public double distance = -1.0d;
    public double range;

    protected AbstractTacticGoal(EntityCustomNpc npc) {
        this.npc = npc;
        setFlags(EnumSet.of(Goal.Flag.MOVE)); // 1.12.2 AiMutex.PATHING
    }

    @Nullable
    public LivingEntity getTarget() {
        return target;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public boolean canUse() {
        distance = -1.0d;
        canSeeToAttack = false;
        hasAttack = false;
        return resolveTarget();
    }

    @Override
    public boolean canContinueToUse() {
        return npc.isAlive() && resolveTarget();
    }

    @Override
    public void stop() {
        canSeeToAttack = false;
    }

    /** 1.12.2 {@code setTarget()}: follows the NPC's target, or its commander's. */
    private boolean resolveTarget() {
        target = npc.getTarget();
        EntityCustomNpc commander = npc.aiOwnerNpc;
        if (commander != null && commander.isAlive()) {
            LivingEntity ownerTarget = commander.getTarget();
            if (ownerTarget != null && ownerTarget.equals(target)) {
                npc.setTarget(ownerTarget);
            }
            target = npc.getTarget();
        }
        if (target == null || !target.isAlive()) {
            return false;
        }
        isFriend = npc.isFriend(target);
        return true;
    }

    @Override
    public void tick() {
        if (target == null) {
            return;
        }
        npc.getLookControl().setLookAt(target, 30.0f, npc.getMaxHeadXRot());
        inMove = !npc.getNavigation().isDone();
        tacticalRange = npc.ai.getTacticalRadius();
        distance = Math.sqrt(npc.distanceToSqr(target.getX(), target.getBoundingBox().minY, target.getZ()));
        meleeTick--;
        range = npc.stats.melee.getRange();
        double minRange = (npc.getBbWidth() + target.getBbWidth()) / 2.0d;
        if (minRange > range) {
            range = minRange;
        }
    }

    /** Paths to the target, faster the further it is (1.12.2 speed curve). */
    protected void tryMoveToTarget() {
        double baseSpeed = npc.ai.isCanSprint() ? 1.5d : 1.3d;
        if (target.equals(npc.combat.priorityTarget)) {
            baseSpeed = npc.ai.isCanSprint() ? 1.6d : 1.4d;
        }
        double dist = npc.distanceTo(target);
        double speed = (0.75d / npc.stats.getAggroRange() * dist + 0.5d) * baseSpeed;
        if (speed < 1.3d) {
            speed = 1.3d;
        } else if (speed > baseSpeed) {
            speed = baseSpeed;
        }
        npc.getNavigation().moveTo(target, speed);
    }

    /** Hits when in range, in sight and the delay is over (1.12.2 melee branch). */
    protected void tryToCauseDamage() {
        if (meleeTick > 0 || distance > range || !canSeeToAttack) {
            if (meleeTick == 0 && !canSeeToAttack) {
                meleeTick = 5;
            }
            return;
        }
        meleeTick = npc.stats.melee.getDelayRNG(npc.getRandom());
        npc.swing(InteractionHand.MAIN_HAND);
        npc.doHurtTarget((ServerLevel) npc.level(), target);
        attacked();
        hasAttack = true;
    }

    /** Seam for stage 2b-4: true when the NPC should shoot instead of hitting. */
    public boolean isRanged() {
        return false;
    }

    /** False while the tactic does not want the target goals to pick a new target. */
    public boolean canNewAttack() {
        return true;
    }

    /** Called after each attack. */
    public void attacked() {
    }

    /** Called when the NPC is about to take damage; true refuses it. */
    public boolean damaged() {
        return false;
    }
}
