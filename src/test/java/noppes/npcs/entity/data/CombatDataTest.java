package noppes.npcs.entity.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.junit.jupiter.api.Test;

class CombatDataTest {

    static CompoundTag save(Consumer<ValueOutput> saver) {
        TagValueOutput out = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
        saver.accept(out);
        return out.buildResult();
    }

    static ValueInput in(CompoundTag tag) {
        return TagValueInput.create(ProblemReporter.DISCARDING, RegistryAccess.EMPTY, tag);
    }

    @Test
    void meleeDefaultsAndKeys() {
        var t = save(new DataMelee(() -> {})::save);
        assertEquals(5, t.getIntOr("AttackStrenght", -1));
        assertEquals(20, t.getIntOr("AttackSpeed", -1));
        assertEquals(0, t.getIntOr("KnockBack", -1));
        assertEquals(0, t.getIntOr("PotionEffect", -1));
        assertEquals(5, t.getIntOr("PotionDuration", -1));
        assertEquals(0, t.getIntOr("PotionAmp", -1));
        assertEquals(2.0, t.getDoubleOr("AttackRange", -1));
        assertEquals(1, t.getIntOr("version", -1));
    }

    @Test
    void meleeRoundTrip() {
        var a = new DataMelee(() -> {});
        a.setStrength(9);
        a.setDelay(33);
        a.setKnockback(2);
        a.setEffectType(4);
        a.setEffectTime(12);
        a.setEffectStrength(3);
        a.setRange(5.5);
        var b = new DataMelee(() -> {});
        b.load(in(save(a::save)));
        assertEquals(9, b.getStrength());
        assertEquals(33, b.getDelay());
        assertEquals(2, b.getKnockback());
        assertEquals(4, b.getEffectType());
        assertEquals(12, b.getEffectTime());
        assertEquals(3, b.getEffectStrength());
        assertEquals(5.5, b.getRange());
    }

    @Test
    void rangedDefaultsAndKeys() {
        var t = save(new DataRanged(() -> {})::save);
        assertEquals(1, t.getIntOr("BurstCount", -1));
        assertEquals(10, t.getIntOr("pSpeed", -1));
        assertEquals(4, t.getIntOr("pDamage", -1));
        assertEquals(15.0, t.getDoubleOr("MaxFiringRange", -1));
        assertEquals(60, t.getIntOr("Accuracy", -1));
        assertTrue(t.getBooleanOr("pRender3D", false));
        assertTrue(t.getBooleanOr("pPhysics", false));
        assertEquals("minecraft:entity.arrow.shoot", t.getStringOr("FiringSound", ""));
        assertEquals("minecraft:entity.arrow.hit", t.getStringOr("HitSound", ""));
        assertEquals("minecraft:block.stone.break", t.getStringOr("GroundSound", ""));
        assertEquals(1, t.getIntOr("version", -1));
    }

    @Test
    void rangedRoundTrip() {
        var a = new DataRanged(() -> {});
        a.setBurstCount(3);
        a.setPSpeed(20);
        a.setPDamage(7);
        a.setPImpact(2);
        a.setPSize(8);
        a.setPArea(1);
        a.setPTrail(2);
        a.setRange(30.5);
        a.setFireRate(9);
        a.setMinDelay(15);
        a.setMaxDelay(55);
        a.setShotCount(4);
        a.setAccuracy(80);
        a.setRender3D(false);
        a.setSpin(true);
        a.setStick(true);
        a.setPhysics(false);
        a.setAccelerate(true);
        a.setGlows(true);
        a.setAimWhileShooting(true);
        a.setPEffect(3);
        a.setPDur(11);
        a.setPEffAmp(2);
        a.setFireSound("minecraft:entity.snowball.throw");
        a.setHitSound("minecraft:entity.generic.hurt");
        a.setGroundSound("minecraft:block.wood.break");
        a.setFireIndirect(1);
        a.setMeleeDistance(4);
        var b = new DataRanged(() -> {});
        b.load(in(save(a::save)));
        assertEquals(3, b.getBurstCount());
        assertEquals(20, b.getPSpeed());
        assertEquals(7, b.getPDamage());
        assertEquals(2, b.getPImpact());
        assertEquals(8, b.getPSize());
        assertEquals(1, b.getPArea());
        assertEquals(2, b.getPTrail());
        assertEquals(30.5, b.getRange());
        assertEquals(9, b.getFireRate());
        assertEquals(15, b.getMinDelay());
        assertEquals(55, b.getMaxDelay());
        assertEquals(4, b.getShotCount());
        assertEquals(80, b.getAccuracy());
        assertFalse(b.isRender3D());
        assertTrue(b.isSpin());
        assertTrue(b.isStick());
        assertFalse(b.isPhysics());
        assertTrue(b.isAccelerate());
        assertTrue(b.isGlows());
        assertTrue(b.isAimWhileShooting());
        assertEquals(3, b.getPEffect());
        assertEquals(11, b.getPDur());
        assertEquals(2, b.getPEffAmp());
        assertEquals("minecraft:entity.snowball.throw", b.getFireSound());
        assertEquals("minecraft:entity.generic.hurt", b.getHitSound());
        assertEquals("minecraft:block.wood.break", b.getGroundSound());
        assertEquals(1, b.getFireIndirect());
        assertEquals(4, b.getMeleeDistance());
    }

    @Test
    void legacyIntRangesLoad() {
        var t = new CompoundTag();
        t.putInt("AttackRange", 4);
        t.putInt("MaxFiringRange", 30);
        var m = new DataMelee(() -> {});
        m.load(in(t));
        assertEquals(4.0, m.getRange());
        var r = new DataRanged(() -> {});
        r.load(in(t));
        assertEquals(30.0, r.getRange());
    }

    @Test
    void oldVersionBumpsKnockback() {
        var t = new CompoundTag();
        t.putInt("KnockBack", 1);
        t.putInt("pImpact", 2);
        t.putInt("version", 0);
        var m = new DataMelee(() -> {});
        m.load(in(t));
        assertEquals(2, m.getKnockback());
        var r = new DataRanged(() -> {});
        r.load(in(t));
        assertEquals(3, r.getPImpact());
        var u = new CompoundTag();
        u.putInt("KnockBack", 1);
        var m2 = new DataMelee(() -> {});
        m2.load(in(u));
        assertEquals(1, m2.getKnockback());
    }

    @Test
    void rangesAreClamped() {
        var r = new DataRanged(() -> {});
        r.setShotCount(0);
        assertEquals(1, r.getShotCount());
        r.setShotCount(50);
        assertEquals(10, r.getShotCount());
        r.setRange(500);
        assertEquals(64.0, r.getRange());
        r.setAccuracy(0);
        assertEquals(1, r.getAccuracy());
        r.setPSpeed(-4);
        assertEquals(0, r.getPSpeed());
        r.setMinDelay(0);
        assertEquals(1, r.getMinDelay());
        r.setMaxDelay(20000);
        assertEquals(9999, r.getMaxDelay());
        var m = new DataMelee(() -> {});
        m.setRange(0);
        assertEquals(0.2, m.getRange());
        m.setRange(99);
        assertEquals(30.0, m.getRange());
    }

    @Test
    void loadedValuesAreClamped() {
        var t = new CompoundTag();
        t.putInt("ShotCount", 0);
        t.putDouble("MaxFiringRange", 0.5);
        t.putDouble("AttackRange", 100);
        var r = new DataRanged(() -> {});
        r.load(in(t));
        assertEquals(1, r.getShotCount());
        assertEquals(1.0, r.getRange());
        var m = new DataMelee(() -> {});
        m.load(in(t));
        assertEquals(30.0, m.getRange());
    }

    @Test
    void missingAndWrongTypedKeysKeepValues() {
        var t = new CompoundTag();
        t.putString("AttackStrenght", "lots");
        t.putString("pDamage", "x");
        t.putInt("FiringSound", 3);
        var m = new DataMelee(() -> {});
        m.setStrength(8);
        m.load(in(t));
        assertEquals(8, m.getStrength());
        assertEquals(20, m.getDelay());
        var r = new DataRanged(() -> {});
        r.load(in(t));
        assertEquals(4, r.getPDamage());
        assertEquals("minecraft:entity.arrow.shoot", r.getFireSound());
    }

    @Test
    void onChangeFiresOnlyOnRealChanges() {
        var n = new AtomicInteger();
        var m = new DataMelee(n::incrementAndGet);
        m.setStrength(5);
        m.setStrength(6);
        var r = new DataRanged(n::incrementAndGet);
        r.setAccuracy(60);
        r.setAccuracy(61);
        assertEquals(2, n.get());
    }
}
