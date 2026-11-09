package noppes.npcs.ai.target;

import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.monster.Monster;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.data.DataAI;

/**
 * 1.12.2 {@code EntityAIClosestTarget}: now and then (1 in {@code chance}) picks the nearest entity
 * the selector accepts. Keeping the target (range, line of sight memory) is vanilla {@link TargetGoal}.
 */
public final class NpcClosestTargetGoal extends TargetGoal {

    private final EntityCustomNpc npc;
    private final int chance;
    private final Predicate<LivingEntity> selector;
    @Nullable
    private LivingEntity candidate;

    public NpcClosestTargetGoal(EntityCustomNpc npc, int chance, Predicate<LivingEntity> selector) {
        // 1.12.2: sight is checked unless DirectLOS is NONE or BLIND.
        super(npc, npc.ai.getSeeTarget() != DataAI.SEE_TARGET_NONE && npc.ai.getSeeTarget() != DataAI.SEE_TARGET_BLIND);
        this.npc = npc;
        this.chance = chance;
        this.selector = selector;
        setFlags(java.util.EnumSet.of(Goal.Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        if (npc.isKilled() || npc.getTarget() != null) {
            return false;
        }
        if (chance > 0 && npc.getRandom().nextInt(chance) != 0) {
            return false;
        }
        double dist = Math.max(getFollowDistance(), npc.stats.getAggroRange());
        List<LivingEntity> list = npc.level().getEntitiesOfClass(LivingEntity.class,
                npc.getBoundingBox().inflate(dist, 4.0d, dist), selector);
        if (list.isEmpty()) {
            return false;
        }
        candidate = list.stream().min(Comparator.comparingDouble(npc::distanceToSqr)).orElseThrow();
        return true;
    }

    @Override
    public void start() {
        npc.setTarget(candidate);
        if (candidate instanceof Monster monster && monster.getTarget() == null) {
            monster.setTarget(npc);
        }
        super.start();
    }

    @Override
    public void stop() {
        candidate = null;
        super.stop();
    }
}
