package noppes.npcs.ai.attack;

import noppes.npcs.entity.EntityCustomNpc;

/** Tactic NONE, from 1.12.2 {@code EntityAINoTactic}: always walks at the target and hits it. */
public final class NoTacticGoal extends AbstractTacticGoal {

    public NoTacticGoal(EntityCustomNpc npc) {
        super(npc);
    }

    @Override
    public void tick() {
        super.tick();
        if (isFriend || npc.tickCount % (TICK_RATE * 2) != 0) {
            return;
        }
        canSeeToAttack = npc.canSee(target);
        tryMoveToTarget();
        tryToCauseDamage();
    }
}
