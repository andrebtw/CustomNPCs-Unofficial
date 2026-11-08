package noppes.npcs.entity.data;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import noppes.npcs.faction.Faction;

/**
 * Faction subset of 1.12.2 {@code noppes.npcs.entity.data.DataAdvanced}. The other keys arrive with
 * their features. {@code FactionID} is saved as set, even when the registry has no such faction
 * (1.12.2 saved the fallback faction's id instead; see the 2b-1 plan).
 */
public final class DataAdvanced {

    private final Runnable onChange;
    private int factionId = 0;
    private boolean attackOtherFactions = false;
    private boolean defendFaction = false;
    private final Set<Integer> attackFactions = new TreeSet<>();
    private final Set<Integer> friendFactions = new TreeSet<>();
    private CompoundTag factionPoints = new CompoundTag();

    public DataAdvanced(Runnable onChange) {
        this.onChange = onChange;
    }

    public int getFactionId() { return factionId; }
    public boolean isAttackOtherFactions() { return attackOtherFactions; }
    public boolean isDefendFaction() { return defendFaction; }
    public Set<Integer> getAttackFactions() { return Collections.unmodifiableSet(attackFactions); }
    public Set<Integer> getFriendFactions() { return Collections.unmodifiableSet(friendFactions); }
    /** Kill-point options; round-trip only until the controllers stage. */
    public CompoundTag getFactionPoints() { return factionPoints.copy(); }

    public void setFactionId(int value) {
        if (value != factionId) { factionId = value; onChange.run(); }
    }

    public void setAttackOtherFactions(boolean value) {
        if (value != attackOtherFactions) { attackOtherFactions = value; onChange.run(); }
    }

    public void setDefendFaction(boolean value) {
        if (value != defendFaction) { defendFaction = value; onChange.run(); }
    }

    public void setAttackFactions(Set<Integer> value) {
        if (!attackFactions.equals(value)) { attackFactions.clear(); attackFactions.addAll(value); onChange.run(); }
    }

    public void setFriendFactions(Set<Integer> value) {
        if (!friendFactions.equals(value)) { friendFactions.clear(); friendFactions.addAll(value); onChange.run(); }
    }

    public void setFactionPoints(CompoundTag value) {
        factionPoints = value.copy();
    }

    public void save(ValueOutput out) {
        out.putInt("FactionID", factionId);
        out.putBoolean("AttackOtherFactions", attackOtherFactions);
        out.putBoolean("DefendFaction", defendFaction);
        out.store("FactionPoints", CompoundTag.CODEC, factionPoints);
        Faction.saveIntegerSet(out, "AttackFactions", attackFactions);
        Faction.saveIntegerSet(out, "FrendFactions", friendFactions);
    }

    public void load(ValueInput in) {
        setFactionId(in.getIntOr("FactionID", factionId));
        setAttackOtherFactions(in.getBooleanOr("AttackOtherFactions", attackOtherFactions));
        setDefendFaction(in.getBooleanOr("DefendFaction", defendFaction));
        in.read("FactionPoints", CompoundTag.CODEC).ifPresent(t -> factionPoints = t);
        Set<Integer> attack = new TreeSet<>(attackFactions);
        Faction.loadIntegerSet(in, "AttackFactions", attack);
        setAttackFactions(attack);
        Set<Integer> friend = new TreeSet<>(friendFactions);
        Faction.loadIntegerSet(in, "FrendFactions", friend);
        setFriendFactions(friend);
    }
}
