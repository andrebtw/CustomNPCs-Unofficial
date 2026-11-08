package noppes.npcs.entity.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
