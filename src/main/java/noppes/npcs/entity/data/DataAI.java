package noppes.npcs.entity.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import noppes.npcs.constants.EnumNpcTactics;
import noppes.npcs.constants.EnumSeeTarget;

/**
 * AI settings, from 1.12.2 {@code noppes.npcs.entity.data.DataAI}. Every key loads and saves (flat,
 * verbatim, including {@code MovingPatern}); combat keys get behaviour in 2b-2, movement keys in 2c.
 */
public final class DataAI {

    public static final float MAX_OFFSET = 9.99f;
    /** 1.12.2 {@code setMaxHurtResistantTime} cap. */
    public static final int MAX_HURT_RESISTANT_TIME = 1200;

    /** DirectLOS (1.12.2 EnumSeeTarget) ordinals; their behaviour is in {@code EntityCustomNpc.hasLineOfSight}. */
    public static final int SEE_TARGET_NORMAL = 0;
    public static final int SEE_TARGET_DEAF = 1;
    public static final int SEE_TARGET_WARY = 2;
    public static final int SEE_TARGET_CALM = 3;
    public static final int SEE_TARGET_REALISTIC = 4;
    public static final int SEE_TARGET_BLIND = 5;
    public static final int SEE_TARGET_NONE = 6;

    private final Runnable onChange;
    private boolean canSwim = true;
    private boolean reactsToFire = false;
    private boolean avoidsWater = false;
    private boolean avoidsSun = false;
    private boolean returnToStart = true;
    private boolean canLeap = false;
    private boolean canSprint = false;
    private boolean canBeCollide = true;
    private boolean movingPause = true;
    private boolean npcInteracting = true;
    private boolean stopAndInteract = true;
    private boolean attackInvisible = false;
    private boolean mountControl = false;
    private boolean aiDisabled = false;
    private int onAttack = 0;
    private int doorInteract = 2;
    private int findShelter = 2;
    private int tacticalRadius = 8;
    private int animationType = 0;
    private int standingType = 0;
    private int movingType = 0;
    private int movementType = 0;
    private int orientation = 0;
    private int walkingRange = 10;
    private int moveSpeed = 5;
    private int movingPos = 0;
    private int movingPattern = 0;
    private int activeRange = 32;
    private int maxHurtResistantTime = 20;
    private float bodyOffsetX = 5.0f;
    private float bodyOffsetY = 5.0f;
    private float bodyOffsetZ = 5.0f;
    private float stepHeight = 0.6f;
    /** {@link EnumNpcTactics} ordinal, saved as {@code TacticalVariant}. */
    private int tactic = 0;
    /** {@link EnumSeeTarget} ordinal, saved as {@code DirectLOS}. */
    private int seeTarget = 0;
    private List<int[]> movingPath = new ArrayList<>();
    @Nullable
    private BlockPos startPos = null;

    public DataAI(Runnable onChange) {
        this.onChange = onChange;
    }

    /** Base movement speed attribute value (1.12.2 {@code EntityNPCInterface.getSpeed}). */
    public float walkSpeed() {
        return moveSpeed / 20.0f;
    }

    public boolean canSwim() { return canSwim; }
    public boolean isReactsToFire() { return reactsToFire; }
    public boolean isAvoidsWater() { return avoidsWater; }
    public boolean isAvoidsSun() { return avoidsSun; }
    public boolean isReturnToStart() { return returnToStart; }
    public boolean isCanLeap() { return canLeap; }
    public boolean isCanSprint() { return canSprint; }
    public boolean isCanBeCollide() { return canBeCollide; }
    public boolean isMovingPause() { return movingPause; }
    public boolean isNpcInteracting() { return npcInteracting; }
    public boolean isStopAndInteract() { return stopAndInteract; }
    public boolean isAttackInvisible() { return attackInvisible; }
    public boolean isMountControl() { return mountControl; }
    public boolean isAiDisabled() { return aiDisabled; }
    public int getOnAttack() { return onAttack; }
    public int getDoorInteract() { return doorInteract; }
    public int getFindShelter() { return findShelter; }
    public int getTacticalRadius() { return tacticalRadius; }
    public int getAnimationType() { return animationType; }
    public int getStandingType() { return standingType; }
    public int getMovingType() { return movingType; }
    public int getMovementType() { return movementType; }
    public int getOrientation() { return orientation; }
    public int getWalkingRange() { return walkingRange; }
    public int getMoveSpeed() { return moveSpeed; }
    public int getMovingPos() { return movingPos; }
    public int getMovingPattern() { return movingPattern; }
    public int getActiveRange() { return activeRange; }
    public int getMaxHurtResistantTime() { return maxHurtResistantTime; }
    public float getBodyOffsetX() { return bodyOffsetX; }
    public float getBodyOffsetY() { return bodyOffsetY; }
    public float getBodyOffsetZ() { return bodyOffsetZ; }
    public float getStepHeight() { return stepHeight; }
    public int getTactic() { return tactic; }
    public int getSeeTarget() { return seeTarget; }
    /** Unmodifiable; replace through {@link #setMovingPath}. */
    public List<int[]> getMovingPath() { return Collections.unmodifiableList(movingPath); }
    @Nullable
    public BlockPos getStartPos() { return startPos; }

    /**
     * 1.12.2 {@code getCurrentMovingPath}: the path point the NPC is heading to, for movingPos and
     * movingPattern (0 loop, 1 back and forth). Unlike 1.12.2 it does not reset movingPos. Null when
     * the path is empty.
     */
    @Nullable
    public int[] getCurrentMovingPath() {
        int size = movingPath.size();
        if (size == 0) { return null; }
        if (size == 1) { return movingPath.get(0).clone(); }
        int pos = movingPos;
        if (movingPattern == 0 && pos >= size) {
            pos = 0;
        } else if (movingPattern == 1) {
            int size2 = size * 2 - 1;
            if (pos >= size2) { pos = 0; } else if (pos >= size) { pos = size2 - pos; }
        }
        return movingPath.get(Math.min(pos, size - 1)).clone();
    }

    public void setCanSwim(boolean value) {
        if (value != canSwim) { canSwim = value; onChange.run(); }
    }

    public void setReactsToFire(boolean value) {
        if (value != reactsToFire) { reactsToFire = value; onChange.run(); }
    }

    public void setAvoidsWater(boolean value) {
        if (value != avoidsWater) { avoidsWater = value; onChange.run(); }
    }

    public void setAvoidsSun(boolean value) {
        if (value != avoidsSun) { avoidsSun = value; onChange.run(); }
    }

    public void setReturnToStart(boolean value) {
        if (value != returnToStart) { returnToStart = value; onChange.run(); }
    }

    public void setCanLeap(boolean value) {
        if (value != canLeap) { canLeap = value; onChange.run(); }
    }

    public void setCanSprint(boolean value) {
        if (value != canSprint) { canSprint = value; onChange.run(); }
    }

    public void setCanBeCollide(boolean value) {
        if (value != canBeCollide) { canBeCollide = value; onChange.run(); }
    }

    public void setMovingPause(boolean value) {
        if (value != movingPause) { movingPause = value; onChange.run(); }
    }

    public void setNpcInteracting(boolean value) {
        if (value != npcInteracting) { npcInteracting = value; onChange.run(); }
    }

    public void setStopAndInteract(boolean value) {
        if (value != stopAndInteract) { stopAndInteract = value; onChange.run(); }
    }

    public void setAttackInvisible(boolean value) {
        if (value != attackInvisible) { attackInvisible = value; onChange.run(); }
    }

    public void setMountControl(boolean value) {
        if (value != mountControl) { mountControl = value; onChange.run(); }
    }

    public void setAiDisabled(boolean value) {
        if (value != aiDisabled) { aiDisabled = value; onChange.run(); }
    }

    public void setOnAttack(int value) {
        value = Math.clamp(value, 0, 3);
        if (value != onAttack) { onAttack = value; onChange.run(); }
    }

    public void setDoorInteract(int value) {
        if (value != doorInteract) { doorInteract = value; onChange.run(); }
    }

    public void setFindShelter(int value) {
        value = Math.clamp(value, 0, 2);
        if (value != findShelter) { findShelter = value; onChange.run(); }
    }

    public void setTacticalRadius(int value) {
        if (value != tacticalRadius) { tacticalRadius = value; onChange.run(); }
    }

    public void setAnimationType(int value) {
        if (value != animationType) { animationType = value; onChange.run(); }
    }

    public void setStandingType(int value) {
        value = Math.clamp(value, 0, 4);
        if (value != standingType) { standingType = value; onChange.run(); }
    }

    public void setMovingType(int value) {
        value = Math.clamp(value, 0, 2);
        if (value != movingType) { movingType = value; onChange.run(); }
    }

    public void setMovementType(int value) {
        value = Math.clamp(value, 0, 2);
        if (value != movementType) { movementType = value; onChange.run(); }
    }

    public void setOrientation(int value) {
        if (value != orientation) { orientation = value; onChange.run(); }
    }

    public void setWalkingRange(int value) {
        if (value != walkingRange) { walkingRange = value; onChange.run(); }
    }

    public void setMoveSpeed(int value) {
        value = Math.clamp(value, 0, 10);
        if (value != moveSpeed) { moveSpeed = value; onChange.run(); }
    }

    public void setMovingPos(int value) {
        if (value != movingPos) { movingPos = value; onChange.run(); }
    }

    public void setMovingPattern(int value) {
        if (value != movingPattern) { movingPattern = value; onChange.run(); }
    }

    public void setActiveRange(int value) {
        if (value != activeRange) { activeRange = value; onChange.run(); }
    }

    public void setMaxHurtResistantTime(int value) {
        value = Math.min(Math.abs(value), MAX_HURT_RESISTANT_TIME);
        if (value != maxHurtResistantTime) { maxHurtResistantTime = value; onChange.run(); }
    }

    public void setBodyOffsetX(float value) {
        if (Float.isNaN(value)) { value = 5.0f; }
        value = Math.clamp(value, 0.0f, MAX_OFFSET);
        if (value != bodyOffsetX) { bodyOffsetX = value; onChange.run(); }
    }

    public void setBodyOffsetY(float value) {
        if (Float.isNaN(value)) { value = 5.0f; }
        value = Math.clamp(value, 0.0f, MAX_OFFSET);
        if (value != bodyOffsetY) { bodyOffsetY = value; onChange.run(); }
    }

    public void setBodyOffsetZ(float value) {
        if (Float.isNaN(value)) { value = 5.0f; }
        value = Math.clamp(value, 0.0f, MAX_OFFSET);
        if (value != bodyOffsetZ) { bodyOffsetZ = value; onChange.run(); }
    }

    public void setStepHeight(float value) {
        if (Float.isNaN(value)) { value = 0.6f; }
        if (value != stepHeight) { stepHeight = value; onChange.run(); }
    }

    /** 1.12.2 {@code ValueUtil.onlyPositiveInt}: a negative ordinal becomes its absolute value, then capped. */
    private static int ordinal(int value, int count) {
        return Math.min(Math.abs(value == Integer.MIN_VALUE ? 0 : value), count - 1);
    }

    public void setTactic(int value) {
        value = ordinal(value, EnumNpcTactics.values().length);
        if (value != tactic) { tactic = value; onChange.run(); }
    }

    public void setSeeTarget(int value) {
        value = ordinal(value, EnumSeeTarget.values().length);
        if (value != seeTarget) { seeTarget = value; onChange.run(); }
    }

    public void setMovingPath(List<int[]> path) {
        List<int[]> copy = new ArrayList<>();
        for (int[] point : path) { copy.add(point.clone()); }
        movingPath = copy;
        onChange.run();
    }

    public void setStartPos(@Nullable BlockPos pos) {
        if (pos != null) { pos = pos.immutable(); }
        if (pos == null ? startPos != null : !pos.equals(startPos)) { startPos = pos; onChange.run(); }
    }

    public void save(ValueOutput out) {
        out.putBoolean("CanSwim", canSwim);
        out.putBoolean("ReactsToFire", reactsToFire);
        out.putBoolean("AvoidsWater", avoidsWater);
        out.putBoolean("AvoidsSun", avoidsSun);
        out.putBoolean("ReturnToStart", returnToStart);
        out.putBoolean("CanLeap", canLeap);
        out.putBoolean("CanSprint", canSprint);
        out.putBoolean("CanBeCollide", canBeCollide);
        out.putBoolean("MovingPause", movingPause);
        out.putBoolean("npcInteracting", npcInteracting);
        out.putBoolean("stopAndInteract", stopAndInteract);
        out.putBoolean("AttackInvisible", attackInvisible);
        out.putBoolean("MountControl", mountControl);
        out.putBoolean("AIDisabled", aiDisabled);
        out.putInt("OnAttack", onAttack);
        out.putInt("DoorInteract", doorInteract);
        out.putInt("FindShelter", findShelter);
        out.putInt("TacticalRadius", tacticalRadius);
        out.putInt("MoveState", animationType);
        out.putInt("StandingState", standingType);
        out.putInt("MovingState", movingType);
        out.putInt("MovementType", movementType);
        out.putInt("Orientation", orientation);
        out.putInt("WalkingRange", walkingRange);
        out.putInt("MoveSpeed", moveSpeed);
        out.putInt("MovingPos", movingPos);
        out.putInt("MovingPatern", movingPattern);
        out.putInt("ActiveRange", activeRange);
        out.putInt("MaxHurtResistantTime", maxHurtResistantTime);
        out.putFloat("PositionOffsetX", bodyOffsetX);
        out.putFloat("PositionOffsetY", bodyOffsetY);
        out.putFloat("PositionOffsetZ", bodyOffsetZ);
        out.putFloat("StepHeight", stepHeight);
        out.putInt("TacticalVariant", tactic);
        out.putInt("DirectLOS", seeTarget);
        ValueOutput.ValueOutputList path = out.childrenList("MovingPathNew");
        for (int[] point : movingPath) {
            path.addChild().putIntArray("Array", point);
        }
        if (startPos != null) {
            out.putIntArray("StartPosNew", new int[] {startPos.getX(), startPos.getY(), startPos.getZ()});
        }
    }

    public void load(ValueInput in) {
        setCanSwim(in.getBooleanOr("CanSwim", canSwim));
        setReactsToFire(in.getBooleanOr("ReactsToFire", reactsToFire));
        setAvoidsWater(in.getBooleanOr("AvoidsWater", avoidsWater));
        setAvoidsSun(in.getBooleanOr("AvoidsSun", avoidsSun));
        setReturnToStart(in.getBooleanOr("ReturnToStart", returnToStart));
        setCanLeap(in.getBooleanOr("CanLeap", canLeap));
        setCanSprint(in.getBooleanOr("CanSprint", canSprint));
        setCanBeCollide(in.getBooleanOr("CanBeCollide", canBeCollide));
        setMovingPause(in.getBooleanOr("MovingPause", movingPause));
        setNpcInteracting(in.getBooleanOr("npcInteracting", npcInteracting));
        setStopAndInteract(in.getBooleanOr("stopAndInteract", stopAndInteract));
        setAttackInvisible(in.getBooleanOr("AttackInvisible", attackInvisible));
        setMountControl(in.getBooleanOr("MountControl", mountControl));
        setAiDisabled(in.getBooleanOr("AIDisabled", aiDisabled));
        setOnAttack(in.getIntOr("OnAttack", onAttack));
        setDoorInteract(in.getIntOr("DoorInteract", doorInteract));
        setFindShelter(in.getIntOr("FindShelter", findShelter));
        setTacticalRadius(in.getIntOr("TacticalRadius", tacticalRadius));
        setAnimationType(in.getIntOr("MoveState", animationType));
        setStandingType(in.getIntOr("StandingState", standingType));
        setMovingType(in.getIntOr("MovingState", movingType));
        setMovementType(in.getIntOr("MovementType", movementType));
        setOrientation(in.getIntOr("Orientation", orientation));
        setWalkingRange(in.getIntOr("WalkingRange", walkingRange));
        setMoveSpeed(in.getIntOr("MoveSpeed", moveSpeed));
        setMovingPos(in.getIntOr("MovingPos", movingPos));
        setMovingPattern(in.getIntOr("MovingPatern", movingPattern));
        setActiveRange(in.getIntOr("ActiveRange", activeRange));
        setMaxHurtResistantTime(in.getIntOr("MaxHurtResistantTime", maxHurtResistantTime));
        setBodyOffsetX(in.getFloatOr("PositionOffsetX", bodyOffsetX));
        setBodyOffsetY(in.getFloatOr("PositionOffsetY", bodyOffsetY));
        setBodyOffsetZ(in.getFloatOr("PositionOffsetZ", bodyOffsetZ));
        setStepHeight(in.getFloatOr("StepHeight", stepHeight));
        setTactic(in.getIntOr("TacticalVariant", tactic));
        // Old saves stored DirectLOS as a boolean: true = NORMAL (vanilla sight), false = NONE.
        if (LegacyTags.isByte(in, "DirectLOS")) {
            setSeeTarget(in.getBooleanOr("DirectLOS", true) ? EnumSeeTarget.NORMAL.ordinal() : EnumSeeTarget.NONE.ordinal());
        } else {
            setSeeTarget(in.getIntOr("DirectLOS", seeTarget));
        }
        in.childrenList("MovingPathNew").ifPresent(list -> {
            List<int[]> path = new ArrayList<>();
            for (ValueInput point : list) {
                point.getIntArray("Array").ifPresent(path::add);
            }
            setMovingPath(path);
        });
        in.getIntArray("StartPosNew").filter(a -> a.length == 3)
                .ifPresent(a -> setStartPos(new BlockPos(a[0], a[1], a[2])));
    }
}
