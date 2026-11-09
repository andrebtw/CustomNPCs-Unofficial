package noppes.npcs.ai.attack;

import noppes.npcs.entity.EntityCustomNpc;

/**
 * Tactic STALK, from 1.12.2 {@code EntityAIStalkTarget}: sneaks up on the target (slower) once close,
 * after attacking, or when the target can see it. The 1.12.2 pose 1 is the sneaking flag.
 */
public final class StalkGoal extends AbstractTacticGoal {

    public StalkGoal(EntityCustomNpc npc) {
        super(npc);
        npc.setShiftKeyDown(false);
    }

    @Override
    public boolean canUse() {
        if (super.canUse()) {
            return true;
        }
        standUp();
        return false;
    }

    @Override
    public void stop() {
        super.stop();
        standUp();
    }

    private void standUp() {
        if (npc.isShiftKeyDown()) {
            npc.setShiftKeyDown(false);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (isFriend || npc.tickCount % (TICK_RATE * 2) != 0) {
            return;
        }
        canSeeToAttack = npc.canSee(target);
        if (!npc.isShiftKeyDown() && distance < tacticalRange) {
            npc.setShiftKeyDown(true);
        }
        if (canSeeToAttack && distance <= range) {
            if (inMove) {
                npc.getNavigation().stop();
            }
        } else {
            npc.getNavigation().moveTo(target, npc.isShiftKeyDown() ? 0.725d : 1.3d);
        }
        tryToCauseDamage();
        if ((!npc.isShiftKeyDown() && hasAttack) || target.hasLineOfSight(npc)) {
            npc.setShiftKeyDown(true);
        }
    }
}
