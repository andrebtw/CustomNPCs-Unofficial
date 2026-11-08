package noppes.npcs.faction;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * A player's points per faction, the 2b part of 1.12.2 {@code PlayerFactionData}. Stored as a player
 * data attachment ({@code NpcAttachments.FACTION_POINTS}) under the 1.12.2 key
 * {@code FactionData: [{Faction:int, Points:int}]}.
 */
public final class PlayerFactionPoints {

    private record Entry(int faction, int points) {
        static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("Faction").forGetter(Entry::faction),
                Codec.INT.fieldOf("Points").forGetter(Entry::points)).apply(i, Entry::new));
    }

    public static final MapCodec<PlayerFactionPoints> CODEC = Entry.CODEC.listOf().fieldOf("FactionData")
            .xmap(PlayerFactionPoints::fromEntries, PlayerFactionPoints::toEntries);

    private final Map<Integer, Integer> points = new TreeMap<>();

    /** Stored points, or (1.12.2) the faction's default points, which are then stored. */
    public int get(Faction faction) {
        return points.computeIfAbsent(faction.id, k -> Math.max(faction.defaultPoints, 0));
    }

    public void set(int factionId, int value) {
        points.put(factionId, Math.max(value, 0));
    }

    private static PlayerFactionPoints fromEntries(List<Entry> entries) {
        PlayerFactionPoints p = new PlayerFactionPoints();
        for (Entry e : entries) { p.set(e.faction(), e.points()); }
        return p;
    }

    private static List<Entry> toEntries(PlayerFactionPoints p) {
        return p.points.entrySet().stream().map(e -> new Entry(e.getKey(), e.getValue())).toList();
    }
}
