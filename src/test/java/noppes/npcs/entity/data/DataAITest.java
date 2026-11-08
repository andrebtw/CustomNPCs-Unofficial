package noppes.npcs.entity.data;

import static noppes.npcs.entity.data.CombatDataTest.in;
import static noppes.npcs.entity.data.CombatDataTest.save;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import noppes.npcs.constants.EnumNpcTactics;
import noppes.npcs.constants.EnumSeeTarget;
import org.junit.jupiter.api.Test;

class DataAITest {

    @Test
    void defaultsMatch112() {
        var a = new DataAI(() -> {});
        assertEquals(5, a.getMoveSpeed());
        assertEquals(0.25f, a.walkSpeed());
        assertTrue(a.canSwim());
        assertTrue(a.isReturnToStart());
        assertEquals(20, a.getMaxHurtResistantTime());
        assertEquals(2, a.getDoorInteract());
        assertEquals(2, a.getFindShelter());
        assertEquals(8, a.getTacticalRadius());
        assertEquals(10, a.getWalkingRange());
        assertEquals(32, a.getActiveRange());
        assertEquals(0.6f, a.getStepHeight());
        assertEquals(5.0f, a.getBodyOffsetY());
        assertEquals(EnumNpcTactics.RUSH.ordinal(), a.getTactic());
        assertEquals(EnumSeeTarget.NORMAL.ordinal(), a.getSeeTarget());
        assertNull(a.getStartPos());
    }

    @Test
    void roundTripAllKeys() {
        var a = new DataAI(() -> {});
        a.setCanSwim(false);
        a.setReactsToFire(true);
        a.setAvoidsWater(true);
        a.setAvoidsSun(true);
        a.setReturnToStart(false);
        a.setCanLeap(true);
        a.setCanSprint(true);
        a.setCanBeCollide(false);
        a.setMovingPause(false);
        a.setNpcInteracting(false);
        a.setStopAndInteract(false);
        a.setAttackInvisible(true);
        a.setMountControl(true);
        a.setAiDisabled(true);
        a.setOnAttack(2);
        a.setDoorInteract(1);
        a.setFindShelter(1);
        a.setTacticalRadius(12);
        a.setAnimationType(3);
        a.setStandingType(4);
        a.setMovingType(2);
        a.setMovementType(1);
        a.setOrientation(90);
        a.setWalkingRange(25);
        a.setMoveSpeed(8);
        a.setMovingPos(1);
        a.setMovingPattern(1);
        a.setActiveRange(48);
        a.setMaxHurtResistantTime(40);
        a.setBodyOffsetX(1.5f);
        a.setBodyOffsetY(2.5f);
        a.setBodyOffsetZ(3.5f);
        a.setStepHeight(1.0f);
        a.setTactic(EnumNpcTactics.STALK.ordinal());
        a.setSeeTarget(EnumSeeTarget.BLIND.ordinal());
        a.setMovingPath(List.of(new int[] {1, 2, 3}, new int[] {4, 5, 6}));
        a.setStartPos(new BlockPos(7, 8, 9));
        var b = new DataAI(() -> {});
        b.load(in(save(a::save)));
        assertFalse(b.canSwim());
        assertTrue(b.isReactsToFire());
        assertTrue(b.isAvoidsWater());
        assertTrue(b.isAvoidsSun());
        assertFalse(b.isReturnToStart());
        assertTrue(b.isCanLeap());
        assertTrue(b.isCanSprint());
        assertFalse(b.isCanBeCollide());
        assertFalse(b.isMovingPause());
        assertFalse(b.isNpcInteracting());
        assertFalse(b.isStopAndInteract());
        assertTrue(b.isAttackInvisible());
        assertTrue(b.isMountControl());
        assertTrue(b.isAiDisabled());
        assertEquals(2, b.getOnAttack());
        assertEquals(1, b.getDoorInteract());
        assertEquals(1, b.getFindShelter());
        assertEquals(12, b.getTacticalRadius());
        assertEquals(3, b.getAnimationType());
        assertEquals(4, b.getStandingType());
        assertEquals(2, b.getMovingType());
        assertEquals(1, b.getMovementType());
        assertEquals(90, b.getOrientation());
        assertEquals(25, b.getWalkingRange());
        assertEquals(8, b.getMoveSpeed());
        assertEquals(1, b.getMovingPos());
        assertEquals(1, b.getMovingPattern());
        assertEquals(48, b.getActiveRange());
        assertEquals(40, b.getMaxHurtResistantTime());
        assertEquals(1.5f, b.getBodyOffsetX());
        assertEquals(2.5f, b.getBodyOffsetY());
        assertEquals(3.5f, b.getBodyOffsetZ());
        assertEquals(1.0f, b.getStepHeight());
        assertEquals(EnumNpcTactics.STALK.ordinal(), b.getTactic());
        assertEquals(EnumSeeTarget.BLIND.ordinal(), b.getSeeTarget());
        assertEquals(2, b.getMovingPath().size());
        assertArrayEquals(new int[] {1, 2, 3}, b.getMovingPath().get(0));
        assertArrayEquals(new int[] {4, 5, 6}, b.getMovingPath().get(1));
        assertEquals(new BlockPos(7, 8, 9), b.getStartPos());
    }

    @Test
    void legacyBooleanDirectLOS() {
        var t = new CompoundTag();
        t.putBoolean("DirectLOS", false);
        var a = new DataAI(() -> {});
        a.load(in(t));
        assertEquals(EnumSeeTarget.NONE.ordinal(), a.getSeeTarget());
        var u = new CompoundTag();
        u.putBoolean("DirectLOS", true);
        a.load(in(u));
        assertEquals(EnumSeeTarget.NORMAL.ordinal(), a.getSeeTarget());
        var v = new CompoundTag();
        v.putInt("DirectLOS", 1);
        a.load(in(v));
        assertEquals(EnumSeeTarget.DEAF.ordinal(), a.getSeeTarget());
    }

    @Test
    void ordinalsAreClamped() {
        var t = new CompoundTag();
        t.putInt("TacticalVariant", 99);
        t.putInt("DirectLOS", 50);
        t.putInt("MoveSpeed", 40);
        t.putInt("OnAttack", -2);
        t.putInt("StandingState", 9);
        t.putFloat("PositionOffsetX", 20f);
        var a = new DataAI(() -> {});
        a.load(in(t));
        assertEquals(EnumNpcTactics.values().length - 1, a.getTactic());
        assertEquals(EnumSeeTarget.values().length - 1, a.getSeeTarget());
        assertEquals(10, a.getMoveSpeed());
        assertEquals(0, a.getOnAttack());
        assertEquals(4, a.getStandingType());
        assertEquals(9.99f, a.getBodyOffsetX());
    }

    /** 1.12.2 ValueUtil.onlyPositiveInt: a negative ordinal becomes its absolute value. */
    @Test
    void negativeTacticUsesAbsoluteValueLike112() {
        var t = new CompoundTag();
        t.putInt("TacticalVariant", -2);
        var a = new DataAI(() -> {});
        a.load(in(t));
        assertEquals(2, a.getTactic());
    }

    @Test
    void noStartPosIsNotSaved() {
        assertFalse(save(new DataAI(() -> {})::save).contains("StartPosNew"));
    }

    @Test
    void badStartPosIsIgnored() {
        var t = new CompoundTag();
        t.putIntArray("StartPosNew", new int[] {1, 2});
        var a = new DataAI(() -> {});
        a.load(in(t));
        assertNull(a.getStartPos());
    }

    @Test
    void missingKeysKeepValues() {
        var a = new DataAI(() -> {});
        a.setMoveSpeed(7);
        a.setStartPos(new BlockPos(1, 2, 3));
        a.load(in(new CompoundTag()));
        assertEquals(7, a.getMoveSpeed());
        assertEquals(new BlockPos(1, 2, 3), a.getStartPos());
    }
}
