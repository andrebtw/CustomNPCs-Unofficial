package noppes.npcs.entity.data;

import java.util.Objects;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Ranged and projectile settings, from 1.12.2 {@code noppes.npcs.entity.data.DataRanged}. Saved
 * flat into the NPC compound by {@link DataStats}, keys verbatim. Ranges are the 1.12.2
 * {@code ValueUtil.correct*} bounds.
 */
public final class DataRanged {

    public static final int VERSION = 1;
    public static final double MIN_RANGE = 1.0d;
    public static final double MAX_RANGE = 64.0d;

    private final Runnable onChange;
    private int burstCount = 1;
    private int pSpeed = 10;
    private int pDamage = 4;
    private int pImpact = 0;
    private int pSize = 5;
    private int pArea = 0;
    private int pTrail = 0;
    private double range = 15.0d;
    private int fireRate = 5;
    private int minDelay = 20;
    private int maxDelay = 40;
    private int shotCount = 1;
    private int accuracy = 60;
    private boolean render3D = true;
    private boolean spin = false;
    private boolean stick = false;
    private boolean physics = true;
    private boolean accelerate = false;
    private boolean glows = false;
    private boolean aimWhileShooting = false;
    private int pEffect = 0;
    private int pDur = 5;
    private int pEffAmp = 0;
    private String fireSound = "minecraft:entity.arrow.shoot";
    private String hitSound = "minecraft:entity.arrow.hit";
    private String groundSound = "minecraft:block.stone.break";
    private int fireIndirect = 0;
    private int meleeDistance = 0;

    public DataRanged(Runnable onChange) {
        this.onChange = onChange;
    }

    public int getBurstCount() { return burstCount; }
    public int getPSpeed() { return pSpeed; }
    public int getPDamage() { return pDamage; }
    public int getPImpact() { return pImpact; }
    public int getPSize() { return pSize; }
    public int getPArea() { return pArea; }
    public int getPTrail() { return pTrail; }
    public double getRange() { return range; }
    public int getFireRate() { return fireRate; }
    public int getMinDelay() { return minDelay; }
    public int getMaxDelay() { return maxDelay; }
    public int getShotCount() { return shotCount; }
    public int getAccuracy() { return accuracy; }
    public boolean isRender3D() { return render3D; }
    public boolean isSpin() { return spin; }
    public boolean isStick() { return stick; }
    public boolean isPhysics() { return physics; }
    public boolean isAccelerate() { return accelerate; }
    public boolean isGlows() { return glows; }
    public boolean isAimWhileShooting() { return aimWhileShooting; }
    public int getPEffect() { return pEffect; }
    public int getPDur() { return pDur; }
    public int getPEffAmp() { return pEffAmp; }
    public String getFireSound() { return fireSound; }
    public String getHitSound() { return hitSound; }
    public String getGroundSound() { return groundSound; }
    public int getFireIndirect() { return fireIndirect; }
    public int getMeleeDistance() { return meleeDistance; }

    public void setBurstCount(int value) {
        if (value != burstCount) { burstCount = value; onChange.run(); }
    }

    public void setPSpeed(int value) {
        value = Math.clamp(value, 0, 100);
        if (value != pSpeed) { pSpeed = value; onChange.run(); }
    }

    public void setPDamage(int value) {
        if (value != pDamage) { pDamage = value; onChange.run(); }
    }

    public void setPImpact(int value) {
        if (value != pImpact) { pImpact = value; onChange.run(); }
    }

    public void setPSize(int value) {
        if (value != pSize) { pSize = value; onChange.run(); }
    }

    public void setPArea(int value) {
        if (value != pArea) { pArea = value; onChange.run(); }
    }

    public void setPTrail(int value) {
        if (value != pTrail) { pTrail = value; onChange.run(); }
    }

    public void setRange(double value) {
        if (Double.isNaN(value)) { value = 15.0d; }
        value = Math.clamp(value, MIN_RANGE, MAX_RANGE);
        if (value != range) { range = value; onChange.run(); }
    }

    public void setFireRate(int value) {
        if (value != fireRate) { fireRate = value; onChange.run(); }
    }

    public void setMinDelay(int value) {
        value = Math.clamp(value, 1, 9999);
        if (value != minDelay) { minDelay = value; onChange.run(); }
    }

    public void setMaxDelay(int value) {
        value = Math.clamp(value, 1, 9999);
        if (value != maxDelay) { maxDelay = value; onChange.run(); }
    }

    public void setShotCount(int value) {
        value = Math.clamp(value, 1, 10);
        if (value != shotCount) { shotCount = value; onChange.run(); }
    }

    public void setAccuracy(int value) {
        value = Math.clamp(value, 1, 100);
        if (value != accuracy) { accuracy = value; onChange.run(); }
    }

    public void setRender3D(boolean value) {
        if (value != render3D) { render3D = value; onChange.run(); }
    }

    public void setSpin(boolean value) {
        if (value != spin) { spin = value; onChange.run(); }
    }

    public void setStick(boolean value) {
        if (value != stick) { stick = value; onChange.run(); }
    }

    public void setPhysics(boolean value) {
        if (value != physics) { physics = value; onChange.run(); }
    }

    public void setAccelerate(boolean value) {
        if (value != accelerate) { accelerate = value; onChange.run(); }
    }

    public void setGlows(boolean value) {
        if (value != glows) { glows = value; onChange.run(); }
    }

    public void setAimWhileShooting(boolean value) {
        if (value != aimWhileShooting) { aimWhileShooting = value; onChange.run(); }
    }

    public void setPEffect(int value) {
        if (value != pEffect) { pEffect = value; onChange.run(); }
    }

    public void setPDur(int value) {
        if (value != pDur) { pDur = value; onChange.run(); }
    }

    public void setPEffAmp(int value) {
        if (value != pEffAmp) { pEffAmp = value; onChange.run(); }
    }

    public void setFireSound(String value) {
        if (value == null) { value = ""; }
        if (!Objects.equals(value, fireSound)) { fireSound = value; onChange.run(); }
    }

    public void setHitSound(String value) {
        if (value == null) { value = ""; }
        if (!Objects.equals(value, hitSound)) { hitSound = value; onChange.run(); }
    }

    public void setGroundSound(String value) {
        if (value == null) { value = ""; }
        if (!Objects.equals(value, groundSound)) { groundSound = value; onChange.run(); }
    }

    public void setFireIndirect(int value) {
        if (value != fireIndirect) { fireIndirect = value; onChange.run(); }
    }

    public void setMeleeDistance(int value) {
        if (value != meleeDistance) { meleeDistance = value; onChange.run(); }
    }

    public void save(ValueOutput out) {
        out.putInt("BurstCount", burstCount);
        out.putInt("pSpeed", pSpeed);
        out.putInt("pDamage", pDamage);
        out.putInt("pImpact", pImpact);
        out.putInt("pSize", pSize);
        out.putInt("pArea", pArea);
        out.putInt("pTrail", pTrail);
        out.putDouble("MaxFiringRange", range);
        out.putInt("FireRate", fireRate);
        out.putInt("minDelay", minDelay);
        out.putInt("maxDelay", maxDelay);
        out.putInt("ShotCount", shotCount);
        out.putInt("Accuracy", accuracy);
        out.putBoolean("pRender3D", render3D);
        out.putBoolean("pSpin", spin);
        out.putBoolean("pStick", stick);
        out.putBoolean("pPhysics", physics);
        out.putBoolean("pXlr8", accelerate);
        out.putBoolean("pGlows", glows);
        out.putBoolean("AimWhileShooting", aimWhileShooting);
        out.putInt("pEffect", pEffect);
        out.putInt("pDur", pDur);
        out.putInt("pEffAmp", pEffAmp);
        out.putString("FiringSound", fireSound);
        out.putString("HitSound", hitSound);
        out.putString("GroundSound", groundSound);
        out.putInt("FireIndirect", fireIndirect);
        out.putInt("DistanceToMelee", meleeDistance);
        out.putInt("version", VERSION);
    }

    public void load(ValueInput in) {
        setBurstCount(in.getIntOr("BurstCount", burstCount));
        setPSpeed(in.getIntOr("pSpeed", pSpeed));
        setPDamage(in.getIntOr("pDamage", pDamage));
        setPImpact(in.getIntOr("pImpact", pImpact));
        setPSize(in.getIntOr("pSize", pSize));
        setPArea(in.getIntOr("pArea", pArea));
        setPTrail(in.getIntOr("pTrail", pTrail));
        // getDoubleOr reads any numeric tag, so the older int MaxFiringRange loads too.
        setRange(in.getDoubleOr("MaxFiringRange", range));
        setFireRate(in.getIntOr("FireRate", fireRate));
        setMinDelay(in.getIntOr("minDelay", minDelay));
        setMaxDelay(in.getIntOr("maxDelay", maxDelay));
        setShotCount(in.getIntOr("ShotCount", shotCount));
        setAccuracy(in.getIntOr("Accuracy", accuracy));
        setRender3D(in.getBooleanOr("pRender3D", render3D));
        setSpin(in.getBooleanOr("pSpin", spin));
        setStick(in.getBooleanOr("pStick", stick));
        setPhysics(in.getBooleanOr("pPhysics", physics));
        setAccelerate(in.getBooleanOr("pXlr8", accelerate));
        setGlows(in.getBooleanOr("pGlows", glows));
        setAimWhileShooting(in.getBooleanOr("AimWhileShooting", aimWhileShooting));
        setPEffect(in.getIntOr("pEffect", pEffect));
        setPDur(in.getIntOr("pDur", pDur));
        setPEffAmp(in.getIntOr("pEffAmp", pEffAmp));
        setFireSound(in.getStringOr("FiringSound", fireSound));
        setHitSound(in.getStringOr("HitSound", hitSound));
        setGroundSound(in.getStringOr("GroundSound", groundSound));
        setFireIndirect(in.getIntOr("FireIndirect", fireIndirect));
        setMeleeDistance(in.getIntOr("DistanceToMelee", meleeDistance));
        // 1.12.2 counts impact from 1 since version 1. Absent version means current (see plan).
        if (in.getInt("version").map(v -> v < VERSION).orElse(false)) {
            setPImpact(pImpact + 1);
        }
    }
}
