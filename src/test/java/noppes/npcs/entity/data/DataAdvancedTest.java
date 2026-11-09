package noppes.npcs.entity.data;

import static noppes.npcs.entity.data.CombatDataTest.in;
import static noppes.npcs.entity.data.CombatDataTest.save;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

class DataAdvancedTest {

    @Test
    void defaults() {
        var a = new DataAdvanced(() -> {});
        assertEquals(0, a.getFactionId());
        assertFalse(a.isAttackOtherFactions());
        assertFalse(a.isDefendFaction());
        assertTrue(a.getAttackFactions().isEmpty());
    }

    @Test
    void roundTrip() {
        var a = new DataAdvanced(() -> {});
        a.setFactionId(2);
        a.setAttackOtherFactions(true);
        a.setDefendFaction(true);
        a.setAttackFactions(Set.of(1, 5));
        a.setFriendFactions(Set.of(3));
        var fp = new CompoundTag();
        fp.putInt("OptionFactions1", 4);
        a.setFactionPoints(fp);
        var t = save(a::save);
        assertEquals(2, t.getIntOr("FactionID", -1));
        assertEquals(1, t.getListOrEmpty("FrendFactions").size());
        var b = new DataAdvanced(() -> {});
        b.load(in(t));
        assertEquals(2, b.getFactionId());
        assertTrue(b.isAttackOtherFactions());
        assertTrue(b.isDefendFaction());
        assertEquals(Set.of(1, 5), b.getAttackFactions());
        assertEquals(Set.of(3), b.getFriendFactions());
        assertEquals(4, b.getFactionPoints().getIntOr("OptionFactions1", -1));
    }

    @Test
    void missingKeysKeepValues() {
        var a = new DataAdvanced(() -> {});
        a.setFactionId(7);
        a.load(in(new CompoundTag()));
        assertEquals(7, a.getFactionId());
    }

    @Test
    void throughWallsDefaultsTrueAndRoundTrips() {
        var a = new DataAdvanced(() -> {});
        assertTrue(a.isThroughWalls());
        a.setThroughWalls(false);
        var t = save(a::save);
        assertTrue(t.contains("ThroughWalls"));
        assertFalse(t.getBooleanOr("ThroughWalls", true));
        var b = new DataAdvanced(() -> {});
        b.load(in(t));
        assertFalse(b.isThroughWalls());
        // 1.12.2 DataAdvanced.readToNBT: an absent ThroughWalls key means true.
        b.load(in(new CompoundTag()));
        assertTrue(b.isThroughWalls());
    }
}
