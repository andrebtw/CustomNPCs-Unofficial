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

class NpcDataTest {

    private static CompoundTag save(Consumer<ValueOutput> saver) {
        TagValueOutput out = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
        saver.accept(out);
        return out.buildResult();
    }

    private static ValueInput in(CompoundTag tag) {
        return TagValueInput.create(ProblemReporter.DISCARDING, RegistryAccess.EMPTY, tag);
    }

    @Test
    void displayRoundTrip() {
        var a = new DataDisplay(() -> {});
        a.setName("Bob");
        a.setTitle("Smith");
        a.setTexture("customnpcs:textures/entity/humanmale/arcticsteve.png");
        a.setShowName(2);
        a.setSize(7.5f);
        var b = new DataDisplay(() -> {});
        b.load(in(save(a::save)));
        assertEquals("Bob", b.getName());
        assertEquals("Smith", b.getTitle());
        assertEquals("customnpcs:textures/entity/humanmale/arcticsteve.png", b.getTexture());
        assertEquals(2, b.getShowName());
        assertEquals(7.5f, b.getSize());
    }

    @Test
    void statsRoundTrip() {
        var a = new DataStats(() -> {});
        a.setMaxHealth(55.5);
        a.setAggroRange(30);
        a.setHealthRegen(4);
        a.setCombatRegen(2);
        a.setRespawnTime(99);
        a.setImmuneToFire(true);
        a.setCanDrown(false);
        a.setBurnInSun(true);
        a.setNoFallDamage(true);
        a.setHideKilledBody(true);
        var b = new DataStats(() -> {});
        b.load(in(save(a::save)));
        assertEquals(55.5, b.getMaxHealth());
        assertEquals(30, b.getAggroRange());
        assertEquals(4, b.getHealthRegen());
        assertEquals(2, b.getCombatRegen());
        assertEquals(99, b.getRespawnTime());
        assertTrue(b.isImmuneToFire());
        assertTrue(!b.canDrown());
        assertTrue(b.isBurnInSun());
        assertTrue(b.isNoFallDamage());
        assertTrue(b.isHideKilledBody());
    }

    @Test
    void savesLegacyKeyNames() {
        var a = new DataDisplay(() -> {});
        a.setName("Bob");
        var tag = save(a::save);
        assertEquals("Bob", tag.getStringOr("Name", ""));
        assertEquals(5.0f, tag.getFloatOr("Size", 0f));
        var s = new DataStats(() -> {});
        var st = save(s::save);
        assertEquals(20.0, st.getDoubleOr("MaxHealth", 0));
        assertTrue(st.getBooleanOr("CanDrown", false));
        assertTrue(st.contains("HideBodyWhenKilled"));
    }

    @Test
    void missingKeysKeepCurrentValues() {
        var d = new DataDisplay(() -> {});
        d.setName("Keep");
        d.load(in(new CompoundTag()));
        assertEquals("Keep", d.getName());
        assertEquals(DataDisplay.DEFAULT_TEXTURE, d.getTexture());
        var s = new DataStats(() -> {});
        s.load(in(new CompoundTag()));
        assertEquals(20.0, s.getMaxHealth());
        assertEquals(16, s.getAggroRange());
        assertTrue(s.canDrown());
    }

    @Test
    void sizeIsClamped() {
        var d = new DataDisplay(() -> {});
        d.setSize(0f);
        assertEquals(DataDisplay.MIN_SIZE, d.getSize());
        d.setSize(-3f);
        assertEquals(DataDisplay.MIN_SIZE, d.getSize());
        d.setSize(1000f);
        assertEquals(DataDisplay.MAX_SIZE, d.getSize());
        d.setSize(Float.NaN);
        assertEquals(5.0f, d.getSize());
        var tag = new CompoundTag();
        tag.putFloat("Size", 0f);
        d.load(in(tag));
        assertEquals(DataDisplay.MIN_SIZE, d.getSize());
    }

    @Test
    void showNameOutOfRangeBecomesZero() {
        var d = new DataDisplay(() -> {});
        d.setShowName(2);
        d.setShowName(7);
        assertEquals(0, d.getShowName());
        d.setShowName(-1);
        assertEquals(0, d.getShowName());
    }

    @Test
    void wrongTypesFallBackToDefaults() {
        var tag = new CompoundTag();
        tag.putString("Size", "big");
        tag.putString("MaxHealth", "x");
        tag.putInt("Name", 3);
        var d = new DataDisplay(() -> {});
        d.load(in(tag));
        assertEquals(5.0f, d.getSize());
        assertEquals("", d.getName());
        var s = new DataStats(() -> {});
        s.load(in(tag));
        assertEquals(20.0, s.getMaxHealth());
    }

    @Test
    void legacyIntMaxHealthLoads() {
        var tag = new CompoundTag();
        tag.putInt("MaxHealth", 40);
        var s = new DataStats(() -> {});
        s.load(in(tag));
        assertEquals(40.0, s.getMaxHealth());
    }

    @Test
    void onChangeFiresOnlyOnRealChanges() {
        var n = new AtomicInteger();
        var d = new DataDisplay(n::incrementAndGet);
        d.setName("A");
        d.setName("A");
        d.setSize(5.0f);
        assertEquals(1, n.get());
    }

    @Test
    void statsNewKeysRoundTrip() {
        var a = new DataStats(() -> {});
        a.setSpawnCycle(3);
        a.setCreatureType(2);
        a.setPotionImmune(true);
        a.setIgnoreCobweb(true);
        a.setChanceBlockDamage(12.5f);
        a.setLevel(20);
        a.setRarity(2);
        a.setRarityTitle("Boss");
        a.setCalmdown(false);
        a.resistances.set("fall", 0.5f);
        a.resistances.set("arrow", 1.5f);
        a.melee.setStrength(11);
        a.ranged.setAccuracy(90);
        var b = new DataStats(() -> {});
        b.load(in(save(a::save)));
        assertEquals(3, b.getSpawnCycle());
        assertEquals(2, b.getCreatureType());
        assertTrue(b.isPotionImmune());
        assertTrue(b.isIgnoreCobweb());
        assertEquals(12.5f, b.getChanceBlockDamage());
        assertEquals(20, b.getLevel());
        assertEquals(2, b.getRarity());
        assertEquals("Boss", b.getRarityTitle());
        assertFalse(b.isCalmdown());
        assertEquals(0.5f, b.resistances.get("fall"));
        assertEquals(1.5f, b.resistances.get("arrow"));
        assertEquals(1.0f, b.resistances.get("mob"));
        assertEquals(11, b.melee.getStrength());
        assertEquals(90, b.ranged.getAccuracy());
    }

    @Test
    void statsNewDefaultsMatch112() {
        var s = new DataStats(() -> {});
        assertEquals(0, s.getSpawnCycle());
        assertEquals(0, s.getCreatureType());
        assertEquals(2.0f, s.getChanceBlockDamage());
        assertEquals(1, s.getLevel());
        assertEquals(0, s.getRarity());
        assertEquals("\u00a7flv.\u00a721", s.getRarityTitle());
        assertTrue(s.isCalmdown());
    }

    @Test
    void statsWritesMeleeAndRangedFlat() {
        var t = save(new DataStats(() -> {})::save);
        assertEquals(5, t.getIntOr("AttackStrenght", -1));
        assertEquals(10, t.getIntOr("pSpeed", -1));
        assertEquals(4, t.getListOrEmpty("Resistances").size());
        assertEquals(1, t.getIntOr("NPCLevel", -1));
        assertTrue(t.getBooleanOr("CalmdownRange", false));
    }

    @Test
    void statsOrdinalsAreClamped() {
        var t = new CompoundTag();
        t.putInt("SpawnCycle", 9);
        t.putInt("CreatureType", 7);
        t.putInt("NPCRarity", -1);
        t.putInt("NPCLevel", 500);
        var s = new DataStats(() -> {});
        s.load(in(t));
        assertEquals(4, s.getSpawnCycle());
        assertEquals(4, s.getCreatureType());
        assertEquals(0, s.getRarity());
        assertEquals(DataStats.MAX_LEVEL, s.getLevel());
    }

    @Test
    void legacyResistancesCompoundLoads() {
        var old = new CompoundTag();
        old.putFloat("Arrow", 0.5f);
        old.putFloat("Melee", 1.5f);
        old.putFloat("Knockback", 0.25f);
        old.putFloat("Explosion", 2.0f);
        var t = new CompoundTag();
        t.put("Resistances", old);
        var s = new DataStats(() -> {});
        s.load(in(t));
        assertEquals(0.5f, s.resistances.get("arrow"));
        assertEquals(1.5f, s.resistances.get("player"));
        assertEquals(0.25f, s.resistances.get("knockback"));
        assertEquals(2.0f, s.resistances.get("explosion.player"));
    }

    @Test
    void resistanceListReplacesMapAndIsClamped() {
        var r = new Resistances(() -> {});
        r.set("fall", 0.5f);
        r.set("arrow", 5f);
        assertEquals(2.0f, r.get("arrow"));
        r.set("mob", -1f);
        assertEquals(0.0f, r.get("mob"));
        var entry = new CompoundTag();
        entry.putString("K", "lava");
        entry.putFloat("V", 0.75f);
        var list = new net.minecraft.nbt.ListTag();
        list.add(entry);
        var t = new CompoundTag();
        t.put("Resistances", list);
        var s = new DataStats(() -> {});
        s.resistances.set("fall", 0.5f);
        s.load(in(t));
        assertEquals(0.75f, s.resistances.get("lava"));
        assertEquals(1.0f, s.resistances.get("fall"));
        assertEquals(1.0f, s.resistances.get("thrown"));
    }

    @Test
    void chanceBlockDamageFollows112Rules() {
        var s = new DataStats(() -> {});
        s.setChanceBlockDamage(-30f);
        assertEquals(30f, s.getChanceBlockDamage());
        s.setChanceBlockDamage(400f);
        assertEquals(100f, s.getChanceBlockDamage());
    }
}
