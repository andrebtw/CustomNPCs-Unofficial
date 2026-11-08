package noppes.npcs.faction;

import java.util.Set;
import java.util.TreeSet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A faction, from 1.12.2 {@code noppes.npcs.controllers.data.Faction}. Keys verbatim (including
 * {@code FrendFactions}). {@code FactionPoints} (kill-point options) and {@code Description} only
 * round-trip until the controllers stage.
 */
public final class Faction {

    public int id = -1;
    public String name = "";
    public int color = 0xFFFFFF;
    public int neutralPoints = 500;
    public int friendlyPoints = 1500;
    public int defaultPoints = 1000;
    public boolean hideFaction = false;
    public boolean getsAttacked = false;
    public final Set<Integer> attackFactions = new TreeSet<>();
    public final Set<Integer> friendFactions = new TreeSet<>();
    public CompoundTag factionPoints = new CompoundTag();
    public String flag = "customnpcs:textures/cloak/mojang.png";
    /** JSON text component, as 1.12.2 saves it. */
    public String description = "";

    public Faction() {}

    public Faction(int id, String name, int color, int defaultPoints) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.defaultPoints = defaultPoints;
    }

    public boolean isAggressiveAt(int points) {
        return points < neutralPoints;
    }

    public boolean isNeutralAt(int points) {
        return points >= neutralPoints && points < friendlyPoints;
    }

    public boolean isFriendlyAt(int points) {
        return points >= friendlyPoints;
    }

    /** 1.12.2 {@code isAggressiveToNpc}: this faction hates the NPC's, or the NPC is set to attack this one. */
    public boolean isAggressiveToNpc(int npcFactionId, Set<Integer> npcAttackFactions) {
        return attackFactions.contains(npcFactionId) || npcAttackFactions.contains(id);
    }

    public void save(ValueOutput out) {
        out.putInt("Slot", id);
        out.putString("Name", name);
        out.putInt("Color", color);
        out.putInt("NeutralPoints", neutralPoints);
        out.putInt("FriendlyPoints", friendlyPoints);
        out.putInt("DefaultPoints", defaultPoints);
        out.putBoolean("HideFaction", hideFaction);
        out.putBoolean("GetsAttacked", getsAttacked);
        saveIntegerSet(out, "AttackFactions", attackFactions);
        out.store("FactionPoints", CompoundTag.CODEC, factionPoints);
        out.putString("Flag", flag);
        out.putString("Description", description);
        saveIntegerSet(out, "FrendFactions", friendFactions);
    }

    public void load(ValueInput in) {
        name = in.getStringOr("Name", name);
        color = in.getIntOr("Color", color);
        id = in.getIntOr("Slot", id);
        neutralPoints = in.getIntOr("NeutralPoints", neutralPoints);
        friendlyPoints = in.getIntOr("FriendlyPoints", friendlyPoints);
        defaultPoints = in.getIntOr("DefaultPoints", defaultPoints);
        hideFaction = in.getBooleanOr("HideFaction", hideFaction);
        getsAttacked = in.getBooleanOr("GetsAttacked", getsAttacked);
        loadIntegerSet(in, "AttackFactions", attackFactions);
        in.read("FactionPoints", CompoundTag.CODEC).ifPresent(t -> factionPoints = t);
        flag = in.getStringOr("Flag", flag);
        description = in.getStringOr("Description", description);
        loadIntegerSet(in, "FrendFactions", friendFactions);
    }

    /** 1.12.2 {@code NBTTags.nbtIntegerCollection}: a list of {@code {Integer:int}}. */
    public static void saveIntegerSet(ValueOutput out, String key, Set<Integer> set) {
        ValueOutput.ValueOutputList list = out.childrenList(key);
        for (int value : set) {
            list.addChild().putInt("Integer", value);
        }
    }

    /** Replaces {@code set} when the list is present; entries without an int {@code Integer} are skipped. */
    public static void loadIntegerSet(ValueInput in, String key, Set<Integer> set) {
        in.childrenList(key).ifPresent(list -> {
            set.clear();
            for (ValueInput entry : list) {
                entry.getInt("Integer").ifPresent(set::add);
            }
        });
    }
}
