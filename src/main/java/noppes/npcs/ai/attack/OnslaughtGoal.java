package noppes.npcs.ai.attack;

import noppes.npcs.entity.EntityCustomNpc;

/** Tactic RUSH, from 1.12.2 {@code EntityAIOnslaught}: closes in, stops in range and hits. */
public final class OnslaughtGoal extends AbstractTacticGoal {

    public OnslaughtGoal(EntityCustomNpc npc) {
        super(npc);
    }

    @Override
    public void tick() {
        super.tick();
        if (isFriend || npc.tickCount % (TICK_RATE * 2) != 0) {
            return;
        }
        canSeeToAttack = npc.canSee(target);
        if (canSeeToAttack && distance <= range) {
            if (inMove) {
                npc.getNavigation().stop();
            }
        } else {
            tryMoveToTarget();
        }
        tryToCauseDamage();
    }
}
