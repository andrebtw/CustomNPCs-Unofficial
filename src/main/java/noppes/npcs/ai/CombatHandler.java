package noppes.npcs.ai;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import noppes.npcs.entity.EntityCustomNpc;

/**
 * Who the NPC is angry at and how much, from 1.12.2 {@code noppes.npcs.ai.CombatHandler}. Abilities
 * are not ported. Runs on the server only; {@link #update()} is called once per tick.
 */
public final class CombatHandler {

    public final Map<LivingEntity, Long> lastDamages = new HashMap<>();
    public final Map<LivingEntity, Double> aggressors = new HashMap<>();
    public boolean onlyPlayers = false;
    @Nullable
    public LivingEntity priorityTarget = null;

    private final EntityCustomNpc npc;
    private int combatResetTimer = 0;
    private int delay = 10;
    private int lastCheckTick = -10;

    public CombatHandler(EntityCustomNpc npc) {
        this.npc = npc;
    }

    /** True when the current target should be dropped: no valid aggressor is worth attacking. */
    public boolean checkTarget() {
        // 1.12.2 checked on ticksExisted % 10; goals are evaluated every other tick here, which
        // would never line up for odd entity ids, so the 10-tick gate keeps its own clock.
        if (aggressors.isEmpty() || npc.tickCount - lastCheckTick < 10) {
            return false;
        }
        lastCheckTick = npc.tickCount;
        LivingEntity target = npc.getTarget();
        double current = 0.0d;
        if (isValidTarget(target)) {
            current = aggressors.getOrDefault(target, 0.0d);
        } else {
            target = null;
        }
        for (Map.Entry<LivingEntity, Double> entry : aggressors.entrySet()) {
            if (entry.getValue() > current && isValidTarget(entry.getKey())) {
                current = entry.getValue();
                target = entry.getKey();
            }
        }
        return target == null;
    }

    public void damage(DamageSource source, double amount) {
        combatResetTimer = 0;
        if (!(source.getEntity() instanceof LivingEntity attacker)) {
            return;
        }
        if (attacker instanceof Player) {
            onlyPlayers = true;
        }
        boolean shield = attacker.getMainHandItem().getItem() instanceof ShieldItem
                || attacker.getOffhandItem().getItem() instanceof ShieldItem;
        boolean projectileOrMagic = source.is(DamageTypeTags.IS_PROJECTILE) || source.is(DamageTypes.MAGIC)
                || source.is(DamageTypes.INDIRECT_MAGIC) || source.is(DamageTypes.THORNS);
        double value = ThreatMath.threat(amount, npc.distanceTo(attacker), attacker instanceof Player, shield,
                projectileOrMagic, attacker.equals(npc.getTarget()));
        aggressors.merge(attacker, value, Double::sum);
        lastDamages.put(attacker, npc.level().getGameTime());
        if (priorityTarget == null) {
            priorityTarget = attacker;
        }
    }

    public boolean isValidTarget(@Nullable LivingEntity target) {
        return target != null && target.isAlive()
                && !EntityCustomNpc.isUntargetablePlayer(target)
                && npc.isInRange(target, npc.stats.getAggroRange())
                && npc.level().dimension() == target.level().dimension();
    }

    public void reset() {
        combatResetTimer = 0;
        delay = 10;
        onlyPlayers = false;
        aggressors.clear();
        lastDamages.clear();
        priorityTarget = null;
        npc.setAttacking(false);
    }

    private boolean shouldCombatContinue() {
        return isValidTarget(npc.getTarget());
    }

    public void start() {
        combatResetTimer = 0;
        npc.setAttacking(true);
    }

    public void update() {
        if (npc.isKilled()) {
            if (npc.isAttacking()) {
                reset();
            }
            return;
        }
        if (npc.getTarget() != null && !npc.isAttacking()) {
            start();
        }
        if (!shouldCombatContinue()) {
            if (combatResetTimer++ > 40) {
                reset();
            }
            return;
        }
        combatResetTimer = 0;
        if (aggressors.isEmpty()) {
            delay = 10;
            return;
        }
        if (--delay > 0) {
            return;
        }
        delay = 10;
        List<LivingEntity> remove = new ArrayList<>();
        double maxValue = Double.MIN_VALUE;
        priorityTarget = null;
        double maxDist = npc.stats.getAggroRange() * 2.0d;
        for (Map.Entry<LivingEntity, Double> entry : aggressors.entrySet()) {
            LivingEntity entity = entry.getKey();
            if (!isValidTarget(entity)) {
                remove.add(entity);
                continue;
            }
            if (!canReach(entity)) {
                continue;
            }
            if (npc.distanceTo(entity) > maxDist) {
                remove.add(entity);
            }
            if (maxValue == Double.MIN_VALUE || entry.getValue() >= maxValue) {
                maxValue = entry.getValue();
                priorityTarget = entity;
            }
        }
        remove.forEach(aggressors::remove);
        if (priorityTarget != null && !priorityTarget.equals(npc.getTarget())) {
            npc.setPriorityTarget(priorityTarget);
            npc.getNavigation().moveTo(priorityTarget, 1.5d);
            delay = 60;
        }
    }

    /** 1.12.2 {@code Util.canMoveEntityToEntity}: a path exists that ends next to the entity. */
    private boolean canReach(LivingEntity entity) {
        Path path = npc.getNavigation().createPath(entity, 0);
        if (path == null) {
            return false;
        }
        Node end = path.getEndNode();
        if (end == null) {
            return false;
        }
        return Math.abs(entity.getX() - end.x) <= 1.0d && Math.abs(entity.getY() - end.y) < 2.0d
                && Math.abs(entity.getZ() - end.z) <= 1.0d;
    }

    /**
     * 1.12.2 hurt-resistance rule, using {@code MaxHurtResistantTime}: a living attacker may hurt the
     * NPC once per half of that time; other sources only when they hit harder than the last hit
     * while the NPC is still invulnerable.
     */
    public boolean canDamage(DamageSource source, float amount) {
        Entity entity = source.getEntity();
        int maxHurtTime = npc.ai.getMaxHurtResistantTime();
        long now = npc.level().getGameTime();
        if (!(entity instanceof LivingEntity living)) {
            if (maxHurtTime != 0 && npc.invulnerableTime > maxHurtTime / 2.0f) {
                return amount > npc.getLastHurtAmount();
            }
            return true;
        }
        Long last = lastDamages.get(living);
        if (last == null || maxHurtTime == 0 || last + maxHurtTime / 2.0f < now) {
            lastDamages.put(living, now);
            return true;
        }
        return false;
    }
}
