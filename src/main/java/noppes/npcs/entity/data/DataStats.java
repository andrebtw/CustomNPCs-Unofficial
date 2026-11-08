package noppes.npcs.entity.data;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Stats subset of the 1.12.2 {@code noppes.npcs.entity.data.DataStats} (defaults from its field
 * initialisers). Melee/ranged/resistances/rarity arrive with combat in stage 2b.
 */
public final class DataStats {

    private final Runnable onChange;
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

    public DataStats(Runnable onChange) {
        this.onChange = onChange;
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
    }
}
