package noppes.npcs.entity.data;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The 1.12.2 {@code noppes.npcs.entity.data.DataStats} (defaults from its field initialisers).
 * {@link #melee}, {@link #ranged} and {@link #resistances} save into the same flat compound, as in
 * 1.12.2. Behaviour for the 2b keys arrives with 2b-2/2b-3.
 */
public final class DataStats {

    /** 1.12.2 config default {@code CustomNpcs.MaxLv}. */
    public static final int MAX_LEVEL = 45;
    /** Highest ordinal of 1.12.2 EnumCreatureAttribute (UNDEFINED, UNDEAD, ARTHROPOD, ILLAGER, WATER). */
    public static final int MAX_CREATURE_TYPE = 4;
    /** SpawnCycle: 0 always, 1 day, 2 night, 3 never, 4 natural. */
    public static final int MAX_SPAWN_CYCLE = 4;
    /** Highest ordinal of 1.12.2 EnumCreatureRarity (NORMAL, ELITE, BOSS). */
    public static final int MAX_RARITY = 2;

    private final Runnable onChange;
    public final DataMelee melee;
    public final DataRanged ranged;
    public final Resistances resistances;
    private double maxHealth = 20.0d;
    private int aggroRange = 16;
    private int healthRegen = 1;
    private int combatRegen = 0;
    private int respawnTime = 20;
    private boolean immuneToFire = false;
    private boolean canDrown = true;
    private boolean burnInSun = false;
    private boolean noFallDamage = false;
    private boolean hideKilledBody = false;
    private int spawnCycle = 0;
    private int creatureType = 0;
    private boolean potionImmune = false;
    private boolean ignoreCobweb = false;
    private float chanceBlockDamage = 2.0f;
    private int level = 1;
    private int rarity = 0;
    private String rarityTitle = "\u00a7flv.\u00a721";
    private boolean calmdown = true;

    public DataStats(Runnable onChange) {
        this.onChange = onChange;
        this.melee = new DataMelee(onChange);
        this.ranged = new DataRanged(onChange);
        this.resistances = new Resistances(onChange);
    }

    public double getMaxHealth() { return maxHealth; }
    public int getAggroRange() { return aggroRange; }
    public int getHealthRegen() { return healthRegen; }
    public int getCombatRegen() { return combatRegen; }
    public int getRespawnTime() { return respawnTime; }
    public boolean isImmuneToFire() { return immuneToFire; }
    public boolean canDrown() { return canDrown; }
    public boolean isBurnInSun() { return burnInSun; }
    public boolean isNoFallDamage() { return noFallDamage; }
    public boolean isHideKilledBody() { return hideKilledBody; }
    public int getSpawnCycle() { return spawnCycle; }
    public int getCreatureType() { return creatureType; }
    public boolean isPotionImmune() { return potionImmune; }
    public boolean isIgnoreCobweb() { return ignoreCobweb; }
    public float getChanceBlockDamage() { return chanceBlockDamage; }
    public int getLevel() { return level; }
    public int getRarity() { return rarity; }
    public String getRarityTitle() { return rarityTitle; }
    public boolean isCalmdown() { return calmdown; }

    public void setMaxHealth(double value) {
        if (Double.isNaN(value)) { value = 20.0d; }
        value = Math.max(value, 1.0d);
        if (value != maxHealth) { maxHealth = value; onChange.run(); }
    }

    public void setAggroRange(int value) {
        value = Math.max(value, 1);
        if (value != aggroRange) { aggroRange = value; onChange.run(); }
    }

    public void setHealthRegen(int value) {
        if (value != healthRegen) { healthRegen = value; onChange.run(); }
    }

    public void setCombatRegen(int value) {
        if (value != combatRegen) { combatRegen = value; onChange.run(); }
    }

    public void setRespawnTime(int value) {
        if (value != respawnTime) { respawnTime = value; onChange.run(); }
    }

    public void setImmuneToFire(boolean value) {
        if (value != immuneToFire) { immuneToFire = value; onChange.run(); }
    }

    public void setCanDrown(boolean value) {
        if (value != canDrown) { canDrown = value; onChange.run(); }
    }

    public void setBurnInSun(boolean value) {
        if (value != burnInSun) { burnInSun = value; onChange.run(); }
    }

    public void setNoFallDamage(boolean value) {
        if (value != noFallDamage) { noFallDamage = value; onChange.run(); }
    }

    public void setHideKilledBody(boolean value) {
        if (value != hideKilledBody) { hideKilledBody = value; onChange.run(); }
    }

    public void setSpawnCycle(int value) {
        value = Math.clamp(value, 0, MAX_SPAWN_CYCLE);
        if (value != spawnCycle) { spawnCycle = value; onChange.run(); }
    }

    public void setCreatureType(int value) {
        value = Math.clamp(value, 0, MAX_CREATURE_TYPE);
        if (value != creatureType) { creatureType = value; onChange.run(); }
    }

    public void setPotionImmune(boolean value) {
        if (value != potionImmune) { potionImmune = value; onChange.run(); }
    }

    public void setIgnoreCobweb(boolean value) {
        if (value != ignoreCobweb) { ignoreCobweb = value; onChange.run(); }
    }

    /** 1.12.2 rules: a negative chance becomes its absolute value, above 100 becomes 100. */
    public void setChanceBlockDamage(float value) {
        if (Float.isNaN(value)) { value = 2.0f; }
        value = Math.min(Math.abs(value), 100.0f);
        if (value != chanceBlockDamage) { chanceBlockDamage = value; onChange.run(); }
    }

    public void setLevel(int value) {
        value = Math.clamp(value, 1, MAX_LEVEL);
        if (value != level) { level = value; onChange.run(); }
    }

    public void setRarity(int value) {
        value = Math.clamp(value, 0, MAX_RARITY);
        if (value != rarity) { rarity = value; onChange.run(); }
    }

    public void setRarityTitle(String value) {
        if (value == null) { value = ""; }
        if (!value.equals(rarityTitle)) { rarityTitle = value; onChange.run(); }
    }

    public void setCalmdown(boolean value) {
        if (value != calmdown) { calmdown = value; onChange.run(); }
    }

    public void save(ValueOutput out) {
        out.putDouble("MaxHealth", maxHealth);
        out.putInt("AggroRange", aggroRange);
        out.putInt("HealthRegen", healthRegen);
        out.putInt("CombatRegen", combatRegen);
        out.putInt("RespawnTime", respawnTime);
        out.putBoolean("ImmuneToFire", immuneToFire);
        out.putBoolean("CanDrown", canDrown);
        out.putBoolean("BurnInSun", burnInSun);
        out.putBoolean("NoFallDamage", noFallDamage);
        out.putBoolean("HideBodyWhenKilled", hideKilledBody);
        out.putInt("SpawnCycle", spawnCycle);
        out.putInt("CreatureType", creatureType);
        out.putBoolean("PotionImmune", potionImmune);
        out.putBoolean("IgnoreCobweb", ignoreCobweb);
        melee.save(out);
        ranged.save(out);
        out.putFloat("ChanceBlockDamage", chanceBlockDamage);
        resistances.save(out);
        out.putInt("NPCLevel", level);
        out.putInt("NPCRarity", rarity);
        out.putString("RarityTitle", rarityTitle);
        out.putBoolean("CalmdownRange", calmdown);
    }

    public void load(ValueInput in) {
        // getDoubleOr accepts any numeric tag, so the legacy int MaxHealth (1.12.2 "Old") loads too.
        setMaxHealth(in.getDoubleOr("MaxHealth", maxHealth));
        setAggroRange(in.getIntOr("AggroRange", aggroRange));
        setHealthRegen(in.getIntOr("HealthRegen", healthRegen));
        setCombatRegen(in.getIntOr("CombatRegen", combatRegen));
        setRespawnTime(in.getIntOr("RespawnTime", respawnTime));
        setImmuneToFire(in.getBooleanOr("ImmuneToFire", immuneToFire));
        setCanDrown(in.getBooleanOr("CanDrown", canDrown));
        setBurnInSun(in.getBooleanOr("BurnInSun", burnInSun));
        setNoFallDamage(in.getBooleanOr("NoFallDamage", noFallDamage));
        setHideKilledBody(in.getBooleanOr("HideBodyWhenKilled", hideKilledBody));
        setSpawnCycle(in.getIntOr("SpawnCycle", spawnCycle));
        setCreatureType(in.getIntOr("CreatureType", creatureType));
        setPotionImmune(in.getBooleanOr("PotionImmune", potionImmune));
        setIgnoreCobweb(in.getBooleanOr("IgnoreCobweb", ignoreCobweb));
        melee.load(in);
        ranged.load(in);
        setChanceBlockDamage(in.getFloatOr("ChanceBlockDamage", chanceBlockDamage));
        resistances.load(in);
        setLevel(in.getIntOr("NPCLevel", level));
        setRarity(in.getIntOr("NPCRarity", rarity));
        setRarityTitle(in.getStringOr("RarityTitle", rarityTitle));
        setCalmdown(in.getBooleanOr("CalmdownRange", calmdown));
    }
}
