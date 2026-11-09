package noppes.npcs.entity.data;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Melee settings, from 1.12.2 {@code noppes.npcs.entity.data.DataMelee}. Saved flat into the
 * NPC compound by {@link DataStats}, keys verbatim (including the misspelt {@code AttackStrenght}).
 */
public final class DataMelee {

    public static final int VERSION = 1;
    public static final double MIN_RANGE = 0.2d;
    public static final double MAX_RANGE = 30.0d;

    private final Runnable onChange;
    private int strength = 5;
    private int delay = 20;
    private int knockback = 0;
    private int effectType = 0;
    private int effectTime = 5;
    private int effectStrength = 0;
    private double range = 2.0d;

    public DataMelee(Runnable onChange) {
        this.onChange = onChange;
    }

    public int getStrength() { return strength; }
    public int getDelay() { return delay; }
    public int getKnockback() { return knockback; }
    public int getEffectType() { return effectType; }
    public int getEffectTime() { return effectTime; }
    public int getEffectStrength() { return effectStrength; }
    public double getRange() { return range; }

    /** 1.12.2 getDelayRNG: the delay plus up to 15% random extra, for delays between 10 and 120. */
    public int getDelayRNG(RandomSource rng) {
        int result = delay;
        if (delay < 120 && delay > 10) {
            result += rng.nextInt((int) (delay * 0.15d));
        }
        return result;
    }

    public void setStrength(int value) {
        if (value != strength) { strength = value; onChange.run(); }
    }

    public void setDelay(int value) {
        if (value != delay) { delay = value; onChange.run(); }
    }

    public void setKnockback(int value) {
        if (value != knockback) { knockback = value; onChange.run(); }
    }

    public void setEffectType(int value) {
        if (value != effectType) { effectType = value; onChange.run(); }
    }

    public void setEffectTime(int value) {
        if (value != effectTime) { effectTime = value; onChange.run(); }
    }

    public void setEffectStrength(int value) {
        if (value != effectStrength) { effectStrength = value; onChange.run(); }
    }

    public void setRange(double value) {
        if (Double.isNaN(value)) { value = 2.0d; }
        value = Math.clamp(value, MIN_RANGE, MAX_RANGE);
        if (value != range) { range = value; onChange.run(); }
    }

    public void save(ValueOutput out) {
        out.putInt("AttackStrenght", strength);
        out.putInt("AttackSpeed", delay);
        out.putInt("KnockBack", knockback);
        out.putInt("PotionEffect", effectType);
        out.putInt("PotionDuration", effectTime);
        out.putInt("PotionAmp", effectStrength);
        out.putDouble("AttackRange", range);
        out.putInt("version", VERSION);
    }

    public void load(ValueInput in) {
        setStrength(in.getIntOr("AttackStrenght", strength));
        setDelay(in.getIntOr("AttackSpeed", delay));
        setKnockback(in.getIntOr("KnockBack", knockback));
        setEffectType(in.getIntOr("PotionEffect", effectType));
        setEffectTime(in.getIntOr("PotionDuration", effectTime));
        setEffectStrength(in.getIntOr("PotionAmp", effectStrength));
        // getDoubleOr reads any numeric tag, so the older int AttackRange loads too.
        setRange(in.getDoubleOr("AttackRange", range));
        // 1.12.2 counts knockback from 1 since version 1. Absent version means current (see plan).
        if (in.getInt("version").map(v -> v < VERSION).orElse(false)) {
            setKnockback(knockback + 1);
        }
    }
}
