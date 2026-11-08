package noppes.npcs.entity;

import java.util.Random;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import noppes.npcs.entity.data.DataDisplay;
import noppes.npcs.entity.data.DataStats;
import noppes.npcs.util.NpcNames;

/**
 * The Custom NPC. Replaces 1.12.2 EntityNPCInterface/EntityCustomNpc for the display and stats
 * subset; AI, combat, roles and model parts come in later stages.
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

    public final DataDisplay display = new DataDisplay(this::applyDisplay);
    public final DataStats stats = new DataStats(this::applyStats);

    public EntityCustomNpc(EntityType<? extends EntityCustomNpc> type, Level level) {
        super(type, level);
        // Only the server picks a name; clients receive it through CustomName. A loaded NPC
        // overwrites this in readAdditionalSaveData, so it is never re-rolled.
        if (!level.isClientSide()) {
            display.setName(NpcNames.randomName(new Random()));
        }
        applyDisplay();
        applyStats();
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
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8f));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));
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
    }

    /** Render scale from synced data (size 5 = 1.0). Usable on both sides. */
    public float getDisplayScale() {
        return entityData.get(DATA_SIZE) / DataDisplay.DEFAULT_SIZE;
    }

    public String getSyncedTexture() {
        return entityData.get(DATA_TEXTURE);
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
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        display.load(in);
        stats.load(in);
        applyDisplay();
        applyStats();
    }
}
