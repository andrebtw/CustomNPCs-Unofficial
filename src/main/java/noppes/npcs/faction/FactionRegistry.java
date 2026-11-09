package noppes.npcs.faction;

import com.mojang.serialization.Codec;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import javax.annotation.Nullable;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Server-wide faction list, the minimal 2b part of 1.12.2 {@code FactionController}. Saved on the
 * overworld as {@code customnpcs_factions} in the 1.12.2 {@code factions.dat} shape
 * {@code {lastID:int, NPCFactions:[faction...]}}. GUI, commands and JSON import come later.
 */
public final class FactionRegistry extends SavedData {

    public static final Codec<FactionRegistry> CODEC =
            CompoundTag.CODEC.xmap(FactionRegistry::fromTag, FactionRegistry::toTag);
    public static final SavedDataType<FactionRegistry> TYPE =
            new SavedDataType<>("customnpcs_factions", FactionRegistry::new, CODEC);

    private final Map<Integer, Faction> factions = new TreeMap<>();
    private int lastId = 0;

    public FactionRegistry() {
        createDefaultFactions();
    }

    public static FactionRegistry get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    /** 1.12.2 {@code createDefaultFactions}: only fills ids that are missing. */
    private void createDefaultFactions() {
        if (!factions.containsKey(0)) {
            Faction friendly = new Faction(0, "faction.name.friendly", 0x00DD00, 2000);
            friendly.friendFactions.add(1);
            factions.put(0, friendly);
        }
        if (!factions.containsKey(1)) {
            Faction neutral = new Faction(1, "faction.name.neutral", 0xF2DD00, 1000);
            neutral.flag = "customnpcs:textures/cloak/baconcape.png";
            factions.put(1, neutral);
        }
        if (!factions.containsKey(2)) {
            Faction aggressive = new Faction(2, "faction.name.aggressive", 0xDD0000, 0);
            aggressive.attackFactions.add(0);
            aggressive.attackFactions.add(1);
            aggressive.flag = "customnpcs:textures/cloak/enderdragoncape.png";
            factions.put(2, aggressive);
        }
        lastId = Math.max(lastId, 2);
        setDirty();
    }

    @Nullable
    public Faction get(int id) {
        return factions.get(id);
    }

    /** The faction with this id, or the one with the lowest id (1.12.2 {@code getFirstFaction}). */
    public Faction getOrFirst(int id) {
        Faction faction = factions.get(id);
        if (faction != null) { return faction; }
        if (factions.isEmpty()) { createDefaultFactions(); }
        return factions.values().iterator().next();
    }

    public Collection<Faction> all() {
        return Collections.unmodifiableCollection(factions.values());
    }

    /** Adds or replaces a faction by its id (faction editing arrives with the controllers stage). */
    public void put(Faction faction) {
        factions.put(faction.id, faction);
        lastId = Math.max(lastId, faction.id);
        setDirty();
    }

    public int getLastId() {
        return lastId;
    }

    private static FactionRegistry fromTag(CompoundTag tag) {
        FactionRegistry reg = new FactionRegistry();
        ValueInput in = TagValueInput.create(ProblemReporter.DISCARDING, RegistryAccess.EMPTY, tag);
        reg.lastId = in.getIntOr("lastID", 0);
        var list = in.childrenList("NPCFactions");
        if (list.isPresent() && !list.get().isEmpty()) {
            reg.factions.clear();
            for (ValueInput entry : list.get()) {
                Faction faction = new Faction();
                faction.load(entry);
                if (faction.id < 0) { continue; }
                reg.factions.put(faction.id, faction);
            }
            if (reg.factions.isEmpty()) { reg.createDefaultFactions(); }
        }
        reg.setDirty(false);
        return reg;
    }

    private static CompoundTag toTag(FactionRegistry reg) {
        TagValueOutput out = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
        out.putInt("lastID", reg.lastId);
        ValueOutput.ValueOutputList list = out.childrenList("NPCFactions");
        for (Faction faction : reg.factions.values()) {
            faction.save(list.addChild());
        }
        return out.buildResult();
    }
}
