package noppes.npcs.entity;

import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityEquipment;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import noppes.npcs.ai.attack.AbstractTacticGoal;
import noppes.npcs.ai.attack.DodgeGoal;
import noppes.npcs.ai.attack.HitAndRunGoal;
import noppes.npcs.ai.attack.NoTacticGoal;
import noppes.npcs.ai.attack.SurroundGoal;
import net.minecraft.world.phys.Vec3;
import noppes.npcs.ai.attack.OnslaughtGoal;
import noppes.npcs.entity.data.DataMelee;
import noppes.npcs.util.NpcMobEffects;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import noppes.npcs.ai.CombatHandler;
import noppes.npcs.ai.selector.NpcAttackSelector;
import noppes.npcs.ai.target.NpcClearTargetGoal;
import noppes.npcs.ai.target.NpcClosestTargetGoal;
import noppes.npcs.client.NpcTextures;
import noppes.npcs.entity.data.DataAI;
import noppes.npcs.entity.data.DataAdvanced;
import noppes.npcs.entity.data.DataDisplay;
import noppes.npcs.entity.data.DataInventory;
import noppes.npcs.entity.data.DataStats;
import noppes.npcs.faction.Faction;
import noppes.npcs.faction.FactionChecks;
import noppes.npcs.faction.FactionRegistry;
import noppes.npcs.util.NpcNames;

/**
 * The Custom NPC. Replaces 1.12.2 EntityNPCInterface/EntityCustomNpc for the display, stats, AI,
 * inventory and faction data; combat behaviour, roles and model parts come in later stages.
 */
public class EntityCustomNpc extends PathfinderMob {

    public static final EntityDataAccessor<String> DATA_TEXTURE =
            SynchedEntityData.defineId(EntityCustomNpc.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<Float> DATA_SIZE =
            SynchedEntityData.defineId(EntityCustomNpc.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Integer> DATA_SHOW_NAME =
            SynchedEntityData.defineId(EntityCustomNpc.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<String> DATA_TITLE =
            SynchedEntityData.defineId(EntityCustomNpc.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<Integer> DATA_FACTION =
            SynchedEntityData.defineId(EntityCustomNpc.class, EntityDataSerializers.INT);
    /** 1.12.2 {@code Attacking}: in combat (set by {@link CombatHandler}). */
    public static final EntityDataAccessor<Boolean> DATA_ATTACKING =
            SynchedEntityData.defineId(EntityCustomNpc.class, EntityDataSerializers.BOOLEAN);

    /** Vanilla slots the NPC's gear is shown in; vanilla never drops them (DropChances 0). */
    private static final EquipmentSlot[] GEAR_SLOTS = {EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND,
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    public final DataDisplay display = new DataDisplay(this::applyDisplay);
    public final DataStats stats = new DataStats(this::applyStats);
    public final DataAI ai = new DataAI(this::applyAi);
    public final DataInventory inventory = new DataInventory(this::applyInventory);
    public final DataAdvanced advanced = new DataAdvanced(this::applyAdvanced);
    public final CombatHandler combat = new CombatHandler(this);

    /** The attack tactic goal (1.12.2 {@code aiAttackTarget}); null when the NPC has none. */
    @Nullable
    public AbstractTacticGoal tacticGoal;
    /** The commander NPC this one follows into battle (1.12.2 {@code aiOwnerNPC}). */
    @Nullable
    public EntityCustomNpc aiOwnerNpc;

    /** Set when AI-relevant data changes; the goals are rebuilt on the next server tick (1.12.2 updateAI). */
    private boolean goalsDirty = true;

    public EntityCustomNpc(EntityType<? extends EntityCustomNpc> type, Level level) {
        super(type, level);
        // Only the server picks a name; clients receive it through CustomName. A loaded NPC
        // overwrites this in readAdditionalSaveData, so it is never re-rolled.
        if (!level.isClientSide()) {
            display.setName(NpcNames.randomName(new Random()));
        }
        applyDisplay();
        applyStats();
        applyAi();
        applyInventory();
        applyAdvanced();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FOLLOW_RANGE, 16.0)
                .add(Attributes.ATTACK_DAMAGE, 2.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_TEXTURE, DataDisplay.DEFAULT_TEXTURE);
        builder.define(DATA_SIZE, DataDisplay.DEFAULT_SIZE);
        builder.define(DATA_SHOW_NAME, 0);
        builder.define(DATA_TITLE, "");
        builder.define(DATA_FACTION, 0);
        builder.define(DATA_ATTACKING, false);
    }

    /** Goals are built by {@link #rebuildGoals()} on the first server tick, once the data is loaded. */
    @Override
    protected void registerGoals() {
    }

    /**
     * 1.12.2 {@code updateTasks()}: stops and drops every goal, then adds the ones the current data
     * asks for. Server only.
     */
    public void rebuildGoals() {
        if (ai == null || level().isClientSide()) {
            return;
        }
        goalsDirty = false;
        clearGoals(goalSelector);
        clearGoals(targetSelector);
        tacticGoal = null;
        aiOwnerNpc = null;
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8f));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        if (isKilled()) {
            return;
        }
        // Owner goals (1.12.2 EntityAIOwnerHurtByTarget/OwnerHurtTarget) come with roles.
        targetSelector.addGoal(0, new NpcClearTargetGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NpcClosestTargetGoal(this, 4, new NpcAttackSelector(this)));
        addResponseGoals();
    }

    /** 1.12.2 {@code setResponse()}: the attack goal for OnAttack and the tactic. */
    private void addResponseGoals() {
        if (ai.isAiDisabled()) {
            // 1.12.2 keeps an Onslaught goal; doHurtTarget refuses every hit.
            tacticGoal = new OnslaughtGoal(this);
        } else if (ai.getOnAttack() == 0) {
            tacticGoal = switch (ai.getTactic()) {
                case 1 -> new DodgeGoal(this);
                case 2 -> new SurroundGoal(this);
                case 3 -> new HitAndRunGoal(this);
                case 6 -> new NoTacticGoal(this);
                default -> new OnslaughtGoal(this);
            };
        }
        if (tacticGoal != null) {
            goalSelector.addGoal(3, tacticGoal);
        }
    }

    private static void clearGoals(GoalSelector selector) {
        for (WrappedGoal goal : selector.getAvailableGoals()) {
            if (goal.isRunning()) {
                goal.stop();
            }
        }
        selector.removeAllGoals(goal -> true);
    }

    private void markGoalsDirty() {
        goalsDirty = true;
    }

    /** Pushes display fields into synced data, the custom name and the SCALE attribute. */
    private void applyDisplay() {
        if (level().isClientSide()) {
            return;
        }
        entityData.set(DATA_TEXTURE, display.getTexture());
        entityData.set(DATA_SIZE, display.getSize());
        entityData.set(DATA_SHOW_NAME, display.getShowName());
        entityData.set(DATA_TITLE, display.getTitle());
        String name = display.getName();
        setCustomName(name.isEmpty() ? null : Component.literal(name));
        getAttribute(Attributes.SCALE).setBaseValue(display.scale());
    }

    /** Maps stats onto vanilla attributes. Lowering MAX_HEALTH clamps current health (LivingEntity). */
    private void applyStats() {
        if (level().isClientSide()) {
            return;
        }
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(stats.getMaxHealth());
        if (getHealth() > getMaxHealth()) {
            setHealth(getMaxHealth());
        }
        getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(stats.getAggroRange());
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(stats.melee.getStrength());
        markGoalsDirty();
    }

    /** Movement attributes from DataAI (1.12.2 setWalkingSpeed / stepHeight). */
    private void applyAi() {
        if (level().isClientSide()) {
            return;
        }
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(ai.walkSpeed());
        getAttribute(Attributes.STEP_HEIGHT).setBaseValue(ai.getStepHeight());
        markGoalsDirty();
    }

    /**
     * Shows the DataInventory gear in the vanilla slots (so the humanoid layers render it) and keeps
     * vanilla from dropping it: NPC drops are DataInventory's job (2b-3).
     */
    private void applyInventory() {
        if (level().isClientSide()) {
            return;
        }
        for (EquipmentSlot slot : GEAR_SLOTS) {
            ItemStack stack = inventoryStack(slot);
            if (getItemBySlot(slot) != stack) {
                super.setItemSlot(slot, stack, false);
            }
            setDropChance(slot, 0.0f);
        }
    }

    private void applyAdvanced() {
        if (level().isClientSide()) {
            return;
        }
        // The effective faction (an unknown id falls back to the first one), as 1.12.2 synced
        // faction.id. A later registry change is picked up on the next applyAdvanced.
        Faction faction = getFaction();
        entityData.set(DATA_FACTION, faction == null ? advanced.getFactionId() : faction.id);
        markGoalsDirty();
    }

    private ItemStack inventoryStack(EquipmentSlot slot) {
        return switch (slot) {
            case MAINHAND -> inventory.getWeapon(DataInventory.MAIN_HAND);
            case OFFHAND -> inventory.getWeapon(DataInventory.OFF_HAND);
            case HEAD -> inventory.getArmor(DataInventory.HEAD);
            case CHEST -> inventory.getArmor(DataInventory.CHEST);
            case LEGS -> inventory.getArmor(DataInventory.LEGS);
            case FEET -> inventory.getArmor(DataInventory.FEET);
            default -> ItemStack.EMPTY;
        };
    }

    /**
     * Keeps DataInventory in step with vanilla equipment changes (/item replace, dispensers), so they
     * survive the next applyInventory and a reload. Same idea as setCustomName; the setters' change
     * check stops the recursion.
     */
    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack, boolean insideTransaction) {
        super.setItemSlot(slot, stack, insideTransaction);
        if (inventory == null || level().isClientSide()) {
            return;
        }
        switch (slot) {
            case MAINHAND -> inventory.setWeapon(DataInventory.MAIN_HAND, stack);
            case OFFHAND -> inventory.setWeapon(DataInventory.OFF_HAND, stack);
            case HEAD -> inventory.setArmor(DataInventory.HEAD, stack);
            case CHEST -> inventory.setArmor(DataInventory.CHEST, stack);
            case LEGS -> inventory.setArmor(DataInventory.LEGS, stack);
            case FEET -> inventory.setArmor(DataInventory.FEET, stack);
            default -> { }
        }
    }

    /**
     * The NPC's faction. An id missing from the registry falls back to the first faction without
     * changing the saved FactionID. Null on the client, where the registry does not exist; use
     * {@link #getSyncedFaction()} there.
     */
    @Nullable
    public Faction getFaction() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        return FactionRegistry.get(serverLevel.getServer()).getOrFirst(advanced.getFactionId());
    }

    /**
     * Seam for 1.12.2 getOwner() (scene owner, follower/companion roles, follower job). Roles and
     * jobs are not ported yet, so an NPC never has an owner.
     */
    @Nullable
    public LivingEntity getOwner() {
        return null;
    }

    /**
     * Vanilla raises a slot's drop chance to 2 when the mob picks an item up. NPC gear never drops
     * through vanilla (DataInventory owns drops), so the chance stays 0.
     */
    @Override
    public void setGuaranteedDrop(EquipmentSlot slot) {
    }

    public boolean isAttacking() {
        return entityData.get(DATA_ATTACKING);
    }

    public void setAttacking(boolean attacking) {
        entityData.set(DATA_ATTACKING, attacking);
    }

    /** 1.12.2 {@code isFollower}: has an owner (scene, follower/companion role, follower job). */
    public boolean isFollower() {
        return getOwner() != null;
    }

    /** Whether the attack goal lets the target goals pick a new target (HitAndRun says no while running). */
    public boolean canNewAttack() {
        return tacticGoal == null || tacticGoal.canNewAttack();
    }

    /**
     * 1.12.2 {@code Util.jumpTowards}: a ballistic leap at {@code pos}. As in 1.12.2 the launch vector
     * is a unit vector (its {@code scale(speed)} result was discarded), and {@code speed} only shapes
     * the pitch.
     */
    public void jumpTowards(float speed, Vec3 pos) {
        double x = pos.x - getX();
        double y = pos.y - getBoundingBox().minY;
        double z = pos.z - getZ();
        float pitch = launchPitch(speed, y, Math.sqrt(x * x + z * z));
        float yaw = (float) (Math.atan2(x, z) * 180.0d / Math.PI);
        float pi = (float) Math.PI;
        setDeltaMovement(Math.sin(yaw / 180.0f * pi) * Math.cos(pitch / 180.0f * pi),
                Math.sin((pitch + 1.0f) / 180.0f * pi),
                Math.cos(yaw / 180.0f * pi) * Math.cos(pitch / 180.0f * pi));
        hurtMarked = true;
    }

    /** 1.12.2 {@code Util.getPitch}: launch angle (degrees) for speed, height and distance, g = 0.2. */
    static float launchPitch(float speed, double y, double horizontalDist) {
        float g = 0.2f;
        float s2 = speed * speed;
        double a = g * horizontalDist;
        double b = g * horizontalDist * horizontalDist + 2.0d * y * s2;
        double c = (double) (s2 * s2) - g * b;
        if (c < 0.0d) {
            return 90.0f;
        }
        float d = s2 - (float) Math.sqrt(c);
        return (float) (Math.atan2(d, a) * 180.0d / Math.PI);
    }

    /**
     * 1.12.2 {@code canSee} through the NPC's {@code canEntityBeSeen}: never beyond aggro range;
     * DirectLOS NONE needs no line of sight, every other mode uses the vanilla line of sight.
     */
    public boolean canSee(Entity entity) {
        if (distanceTo(entity) > stats.getAggroRange()) {
            return false;
        }
        return ai.getSeeTarget() == DataAI.SEE_TARGET_NONE || getSensing().hasLineOfSight(entity);
    }

    /**
     * 1.12.2 {@code tryAttackEntityAsMob} (no animation hitboxes, no scripted event): MeleeStrength
     * through a mob attack, short delays skip the target's hurt cooldown, the potion effect is applied
     * on every swing, and KnockBack pushes the target away on a hit.
     */
    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        if (ai.isAiDisabled() || target == null || !target.isAlive()) {
            return false;
        }
        DataMelee melee = stats.melee;
        if (melee.getDelay() < 10) {
            target.invulnerableTime = 0;
        }
        boolean hit = target.hurtServer(level, damageSources().mobAttack(this), melee.getStrength());
        if (hit) {
            setLastHurtMob(target);
            if (melee.getKnockback() > 0 && target instanceof LivingEntity living) {
                living.knockback(0.2d * melee.getKnockback(), getX() - target.getX(), getZ() - target.getZ());
            }
        }
        if (melee.getEffectType() == 1) {
            target.igniteForSeconds(melee.getEffectTime());
        } else if (melee.getEffectType() != 0 && target instanceof LivingEntity living) {
            Holder<MobEffect> effect = NpcMobEffects.byLegacyId(melee.getEffectType());
            if (effect != null) {
                living.addEffect(new MobEffectInstance(effect, melee.getEffectTime() * 20, melee.getEffectStrength()), this);
            }
        }
        return hit;
    }

    /** Seam for the 1.12.2 killed state (stage 2b-3). */
    public boolean isKilled() {
        return false;
    }

    /** The amount of the last hit taken (vanilla {@code lastHurt}), for {@link CombatHandler#canDamage}. */
    public float getLastHurtAmount() {
        return lastHurt;
    }

    /** 1.12.2 {@code capabilities.disableDamage}: creative and spectator players are never targets. */
    public static boolean isUntargetablePlayer(@Nullable Entity entity) {
        return entity instanceof Player player
                && (player.getAbilities().invulnerable || player.isCreative() || player.isSpectator());
    }

    /**
     * 1.12.2 {@code isInRange}: a box test (each axis within {@code range}), not a sphere. 1.12.2
     * skipped the vertical test below y=0; worlds go below 0 now, so it always applies.
     */
    public boolean isInRange(double x, double y, double z, double range) {
        return Math.abs(getY() - y) <= range && Math.abs(getX() - x) <= range && Math.abs(getZ() - z) <= range;
    }

    public boolean isInRange(Entity entity, double range) {
        return isInRange(entity.getX(), entity.getY(), entity.getZ(), range);
    }

    /** 1.12.2 {@code isFriend}: another NPC of the same faction or of a friend faction (either way). */
    public boolean isFriend(@Nullable Entity entity) {
        if (!(entity instanceof EntityCustomNpc other)) {
            return false;
        }
        Faction own = getFaction();
        Faction theirs = other.getFaction();
        if (own == null || theirs == null) {
            return false;
        }
        return own.id == theirs.id || theirs.friendFactions.contains(own.id)
                || other.advanced.getFriendFactions().contains(own.id)
                || own.friendFactions.contains(theirs.id)
                || advanced.getFriendFactions().contains(theirs.id);
    }

    /**
     * 1.12.2 {@code setAttackTarget}: creative/spectator players and the owner are refused, and an
     * existing target is kept while the combat handler has a priority target. Clearing the target
     * also stops the running target goals. Scripted target events and attack lines are not ported.
     */
    @Override
    public void setTarget(@Nullable LivingEntity entity) {
        if (combat == null) { // Entity super-constructor
            super.setTarget(entity);
            return;
        }
        if (isUntargetablePlayer(entity) || (entity != null && entity == getOwner()) || getTarget() == entity) {
            return;
        }
        if (getTarget() != null && combat.priorityTarget != null) {
            return;
        }
        if (entity == null) {
            for (WrappedGoal goal : targetSelector.getAvailableGoals()) {
                if (goal.isRunning()) {
                    goal.stop();
                }
            }
        }
        super.setTarget(entity);
    }

    /** 1.12.2 {@code setPriorityAttackTarget}: the combat handler's choice, past the priority lock. */
    public void setPriorityTarget(@Nullable LivingEntity entity) {
        if (!isAlive() || getTarget() == entity || isUntargetablePlayer(entity)
                || (entity != null && entity == getOwner()) || isFriend(entity)) {
            return;
        }
        super.setTarget(entity);
    }

    /** 1.12.2 {@code onAttack}: take up a fight on someone else's behalf (faction defence). */
    public void onAttack(@Nullable LivingEntity entity) {
        if (!ai.isAiDisabled() && entity != null && entity != this && !isAttacking() && ai.getOnAttack() != 3
                && entity != getOwner()) {
            super.setTarget(entity);
        }
    }

    /**
     * 1.12.2 {@code canAttackClass} is false while AIDisabled, so target goals never pick anyone. NPCs
     * fight in peaceful too (1.12.2 NPCs are not monsters), so the vanilla peaceful check is skipped.
     */
    @Override
    public boolean canAttack(LivingEntity target) {
        return !ai.isAiDisabled() && target.canBeSeenAsEnemy();
    }

    @Override
    public void aiStep() {
        if (!level().isClientSide()) {
            if (goalsDirty) {
                rebuildGoals();
            }
            // 1.12.2 onUpdate: a target that turned creative (or a dying NPC) drops it, past the lock.
            if (deathTime > 0 || isUntargetablePlayer(getTarget())) {
                super.setTarget(null);
            }
            if (!ai.isAiDisabled() && !isKilled() && tickCount % 20 == 0) {
                drawMonsters();
            }
            combat.update();
        }
        super.aiStep();
    }

    /**
     * 1.12.2 {@code attackEntityFrom}, for the parts that exist in 2b-2: the NPC refuses damage from
     * its owner, its own faction and friendly players, and from suffocation; otherwise nearby NPCs
     * may defend it and it takes up the fight. Resistances, blocking, the killed state and void reset
     * (2b-3), scripted events and animations are not ported.
     */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (tacticRefusesDamage()) {
            return false;
        }
        if (source.is(DamageTypes.IN_WALL) || !combat.canDamage(source, amount)) {
            return false;
        }
        LivingEntity attacker = source.getEntity() instanceof LivingEntity living ? living : null;
        Faction faction = getFaction();
        if (attacker != null && attacker == getOwner()) {
            return false;
        }
        if (attacker instanceof EntityCustomNpc other) {
            Faction theirs = other.getFaction();
            if (faction != null && theirs != null && faction.id == theirs.id) {
                return false;
            }
        } else if (attacker instanceof Player player && faction != null && FactionChecks.isFriendlyToPlayer(faction, player)) {
            return false;
        }
        if (isKilled()) {
            return false;
        }
        if (attacker == null || (attacker instanceof Player player && player.isCreative())) {
            return super.hurtServer(level, source, amount);
        }
        if (amount > 0.0f && faction != null) {
            for (EntityCustomNpc npc : level.getEntitiesOfClass(EntityCustomNpc.class, getBoundingBox().inflate(32.0, 16.0, 32.0))) {
                if (npc != this) {
                    npc.advanced.tryDefendFaction(npc, faction.id, this, attacker);
                }
            }
        }
        if (isAttacking()) {
            LivingEntity target = getTarget();
            if (target != null && distanceTo(target) > distanceTo(attacker)) {
                setTarget(attacker);
            }
        } else if (amount > 0.0f) {
            setTarget(attacker);
        }
        return super.hurtServer(level, source, amount);
    }

    /** The tactic's damage veto (1.12.2 {@code aiAttackTarget.damaged()}, Dodge). */
    private boolean tacticRefusesDamage() {
        return tacticGoal != null && tacticGoal.damaged();
    }

    /** 1.12.2 {@code damageEntity}: every applied hit feeds the combat handler. */
    @Override
    protected void actuallyHurt(ServerLevel level, DamageSource source, float amount) {
        super.actuallyHurt(level, source, amount);
        combat.damage(source, amount);
    }

    /** 1.12.2 onLivingUpdate: idle monsters that see an NPC of a GetsAttacked faction go for it. */
    private void drawMonsters() {
        Faction faction = getFaction();
        if (faction == null || !faction.getsAttacked) {
            return;
        }
        double range = getAttributeValue(Attributes.FOLLOW_RANGE);
        for (Monster monster : level().getEntitiesOfClass(Monster.class, getBoundingBox().inflate(range))) {
            if (monster.getTarget() == null && monster.hasLineOfSight(this)) {
                monster.setTarget(this);
            }
        }
    }

    public int getSyncedFaction() {
        return entityData.get(DATA_FACTION);
    }

    /** 1.12.2 startPos() lazily takes the current position; here it is fixed when first added. */
    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        if (!level().isClientSide() && ai.getStartPos() == null) {
            ai.setStartPos(blockPosition());
        }
    }

    /** Render scale from synced data (size 5 = 1.0). Usable on both sides. */
    public float getDisplayScale() {
        return entityData.get(DATA_SIZE) / DataDisplay.DEFAULT_SIZE;
    }

    public String getSyncedTexture() {
        return entityData.get(DATA_TEXTURE);
    }

    /** Skin from the synced texture string; invalid strings fall back to Steve. */
    public Identifier getSkinTexture() {
        return NpcTextures.resolve(getSyncedTexture());
    }

    public String getSyncedTitle() {
        return entityData.get(DATA_TITLE);
    }

    public int getSyncedShowName() {
        return entityData.get(DATA_SHOW_NAME);
    }

    @Override
    public boolean fireImmune() {
        // stats is null while the Entity constructor runs.
        return (stats != null && stats.isImmuneToFire()) || super.fireImmune();
    }

    @Override
    public boolean canBreatheUnderwater() {
        return stats != null && !stats.canDrown();
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float damageMultiplier, DamageSource damageSource) {
        if (stats.isNoFallDamage()) {
            return false;
        }
        return super.causeFallDamage(fallDistance, damageMultiplier, damageSource);
    }

    @Override
    public boolean shouldShowName() {
        if (!hasCustomName()) {
            return false;
        }
        return switch (getSyncedShowName()) {
            case 1 -> false;
            case 2 -> isAggressive();
            default -> true;
        };
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public boolean requiresCustomPersistence() {
        return true;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        display.save(out);
        stats.save(out);
        ai.save(out);
        inventory.save(out);
        advanced.save(out);
        // The gear lives in Weapons/Armor. Without this, /data merge would re-read stale vanilla
        // equipment; with it, an "equipment" key on load always comes from the user.
        out.discard("equipment");
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        display.load(in);
        stats.load(in);
        ai.load(in);
        inventory.load(in);
        advanced.load(in);
        // 1.21 /summon and /data merge give gear as "equipment"; take it into DataInventory so
        // applyInventory keeps it instead of reverting to Weapons/Armor.
        in.read("equipment", EntityEquipment.CODEC).ifPresent(equipment -> {
            for (EquipmentSlot slot : GEAR_SLOTS) {
                ItemStack stack = equipment.get(slot);
                if (!stack.isEmpty()) {
                    setItemSlot(slot, stack);
                }
            }
        });
        applyDisplay();
        applyStats();
        applyAi();
        applyInventory();
        applyAdvanced();
        // Vanilla read Health while MAX_HEALTH was still the default (flat /summon NBT has no
        // "attributes"), so it was clamped to 20. Re-read it now that MaxHealth is applied.
        setHealth(in.getFloatOr("Health", getMaxHealth()));
    }

    /**
     * Keeps display.name in step with vanilla renames (name tags, /data, CustomName on load), so
     * they are not reverted by the next applyDisplay. setName's equality check stops the recursion.
     */
    @Override
    public void setCustomName(Component name) {
        super.setCustomName(name);
        if (display != null && !level().isClientSide()) {
            display.setName(name == null ? "" : name.getString());
        }
    }
}
