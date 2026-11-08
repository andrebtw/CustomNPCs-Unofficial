package noppes.npcs.faction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.junit.jupiter.api.Test;

class FactionTest {

    private static CompoundTag save(Consumer<ValueOutput> saver) {
        TagValueOutput out = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
        saver.accept(out);
        return out.buildResult();
    }

    private static ValueInput in(CompoundTag tag) {
        return TagValueInput.create(ProblemReporter.DISCARDING, RegistryAccess.EMPTY, tag);
    }

    @Test
    void thresholdsMatch112() {
        var f = new Faction(5, "x", 0, 1000);
        assertTrue(f.isAggressiveAt(499));
        assertFalse(f.isAggressiveAt(500));
        assertTrue(f.isNeutralAt(500));
        assertTrue(f.isNeutralAt(1499));
        assertTrue(f.isFriendlyAt(1500));
        assertFalse(f.isNeutralAt(1500));
        assertFalse(f.isFriendlyAt(1499));
    }

    @Test
    void npcHostilityUsesBothSides() {
        var agg = new Faction(2, "a", 0, 0);
        agg.attackFactions.add(0);
        assertTrue(agg.isAggressiveToNpc(0, Set.of()));
        assertTrue(new Faction(1, "n", 0, 0).isAggressiveToNpc(3, Set.of(1)));
        assertFalse(new Faction(1, "n", 0, 0).isAggressiveToNpc(3, Set.of()));
    }

    @Test
    void factionRoundTrip() {
        var a = new Faction(7, "Bandits", 0x123456, 250);
        a.neutralPoints = 300;
        a.friendlyPoints = 900;
        a.hideFaction = true;
        a.getsAttacked = true;
        a.attackFactions.add(1);
        a.attackFactions.add(4);
        a.friendFactions.add(3);
        var fp = new CompoundTag();
        fp.putInt("OptionFactions1", 2);
        a.factionPoints = fp;
        a.flag = "customnpcs:textures/cloak/baconcape.png";
        a.description = "{\"text\":\"Outlaws\"}";
        var t = save(a::save);
        assertEquals(7, t.getIntOr("Slot", -1));
        assertEquals(1, t.getListOrEmpty("FrendFactions").size());
        var b = new Faction();
        b.load(in(t));
        assertEquals(7, b.id);
        assertEquals("Bandits", b.name);
        assertEquals(0x123456, b.color);
        assertEquals(250, b.defaultPoints);
        assertEquals(300, b.neutralPoints);
        assertEquals(900, b.friendlyPoints);
        assertTrue(b.hideFaction);
        assertTrue(b.getsAttacked);
        assertEquals(Set.of(1, 4), b.attackFactions);
        assertEquals(Set.of(3), b.friendFactions);
        assertEquals(2, b.factionPoints.getIntOr("OptionFactions1", -1));
        assertEquals("customnpcs:textures/cloak/baconcape.png", b.flag);
        assertEquals("{\"text\":\"Outlaws\"}", b.description);
    }

    @Test
    void playerPointsDefaultToFactionDefault() {
        var p = new PlayerFactionPoints();
        var f = new Faction(1, "n", 0, 1000);
        assertEquals(1000, p.get(f));
        p.set(1, -5);
        assertEquals(0, p.get(f));
        p.set(1, 1700);
        assertEquals(1700, p.get(f));
    }

    @Test
    void registryHasDefaultFactions() {
        var reg = new FactionRegistry();
        assertEquals(3, reg.all().size());
        assertEquals("faction.name.friendly", reg.get(0).name);
        assertEquals(0x00DD00, reg.get(0).color);
        assertEquals(2000, reg.get(0).defaultPoints);
        assertEquals(Set.of(1), reg.get(0).friendFactions);
        assertEquals("customnpcs:textures/cloak/baconcape.png", reg.get(1).flag);
        assertEquals(Set.of(0, 1), reg.get(2).attackFactions);
        assertEquals(0, reg.get(2).defaultPoints);
        assertEquals(0, reg.getOrFirst(99).id);
    }

    @Test
    void registryCodecRoundTrip() {
        var reg = new FactionRegistry();
        reg.get(1).name = "Townsfolk";
        var tag = FactionRegistry.TYPE.codec().encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, reg).getOrThrow();
        var copy = FactionRegistry.TYPE.codec().parse(net.minecraft.nbt.NbtOps.INSTANCE, tag).getOrThrow();
        assertEquals("Townsfolk", copy.get(1).name);
        assertEquals(3, copy.all().size());
        assertTrue(((CompoundTag) tag).getListOrEmpty("NPCFactions").size() == 3);
    }

    @Test
    void playerPointsCodecRoundTrip() {
        var p = new PlayerFactionPoints();
        p.set(2, 640);
        var tag = PlayerFactionPoints.CODEC.codec().encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, p).getOrThrow();
        var copy = PlayerFactionPoints.CODEC.codec().parse(net.minecraft.nbt.NbtOps.INSTANCE, tag).getOrThrow();
        assertEquals(640, copy.get(new Faction(2, "a", 0, 0)));
        assertEquals(1, ((CompoundTag) tag).getListOrEmpty("FactionData").size());
    }
}
