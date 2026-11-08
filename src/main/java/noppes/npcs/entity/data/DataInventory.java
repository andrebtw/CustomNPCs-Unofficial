package noppes.npcs.entity.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Gear, drops and XP, from 1.12.2 {@code noppes.npcs.entity.data.DataInventory}. Item lists are
 * {@code [{Slot:byte, <1.21.11 ItemStack fields>}]} like 1.12.2 {@code NBTTags.nbtIItemStackMap}.
 * {@code AWModItems} (Armourer's Workshop) is dropped. Drop behaviour arrives in 2b-3.
 */
public final class DataInventory {

    /** Weapon slots: main hand, projectile, off hand. */
    public static final int MAIN_HAND = 0, PROJECTILE = 1, OFF_HAND = 2;
    /** Armour slots: head, chest, legs, feet (1.12.2 {@code 3 - slot.getIndex()}). */
    public static final int HEAD = 0, CHEST = 1, LEGS = 2, FEET = 3;

    private final Runnable onChange;
    private final Map<Integer, ItemStack> weapons = new TreeMap<>();
    private final Map<Integer, ItemStack> armor = new TreeMap<>();
    public final List<DropSet> drops = new ArrayList<>();
    private boolean lootMode = true;
    private int minExp = 0;
    private int maxExp = 0;
    private String saveDropsName = "";
    private int dropType = 0;
    private int limitation = 0;

    public DataInventory(Runnable onChange) {
        this.onChange = onChange;
    }

    public ItemStack getWeapon(int slot) { return weapons.getOrDefault(slot, ItemStack.EMPTY); }
    public ItemStack getArmor(int slot) { return armor.getOrDefault(slot, ItemStack.EMPTY); }
    public boolean isLootMode() { return lootMode; }
    public int getMinExp() { return minExp; }
    public int getMaxExp() { return maxExp; }
    public String getSaveDropsName() { return saveDropsName; }
    public int getDropType() { return dropType; }
    public int getLimitation() { return limitation; }

    public void setWeapon(int slot, ItemStack stack) {
        if (put(weapons, slot, stack)) { onChange.run(); }
    }

    public void setArmor(int slot, ItemStack stack) {
        if (put(armor, slot, stack)) { onChange.run(); }
    }

    /**
     * Stores (by reference, so it stays in step with the equipped stack) or removes the stack; true
     * when the slot now holds a different stack.
     */
    private static boolean put(Map<Integer, ItemStack> map, int slot, ItemStack stack) {
        ItemStack old = map.getOrDefault(slot, ItemStack.EMPTY);
        if (stack == null) { stack = ItemStack.EMPTY; }
        if (old == stack || (old.isEmpty() && stack.isEmpty())) { return false; }
        if (stack.isEmpty()) { map.remove(slot); } else { map.put(slot, stack); }
        return true;
    }

    public void setLootMode(boolean value) {
        if (value != lootMode) { lootMode = value; onChange.run(); }
    }

    public void setMinExp(int value) {
        if (value != minExp) { minExp = value; onChange.run(); }
    }

    public void setMaxExp(int value) {
        if (value != maxExp) { maxExp = value; onChange.run(); }
    }

    public void setSaveDropsName(String value) {
        if (value == null) { value = ""; }
        if (!Objects.equals(value, saveDropsName)) { saveDropsName = value; onChange.run(); }
    }

    /** 1.12.2 normalisation: negative becomes its absolute value, above 2 becomes {@code % 3}. */
    public void setDropType(int value) {
        if (value < 0) { value = value == Integer.MIN_VALUE ? 0 : -value; }
        if (value > 2) { value %= 3; }
        if (value != dropType) { dropType = value; onChange.run(); }
    }

    public void setLimitation(int value) {
        if (value != limitation) { limitation = value; onChange.run(); }
    }

    public void save(ValueOutput out) {
        out.putInt("MinExp", minExp);
        out.putInt("MaxExp", maxExp);
        saveItems(out, "Armor", armor);
        saveItems(out, "Weapons", weapons);
        out.putBoolean("LootMode", lootMode);
        ValueOutput.ValueOutputList list = out.childrenList("NpcInv");
        for (int i = 0; i < drops.size(); i++) {
            ValueOutput entry = list.addChild();
            drops.get(i).save(entry);
            entry.putInt("Slot", i);
        }
        out.putString("SaveDropsName", saveDropsName);
        out.putInt("DropType", dropType);
        out.putInt("Limitation", limitation);
    }

    public void load(ValueInput in) {
        setMinExp(in.getIntOr("MinExp", minExp));
        setMaxExp(in.getIntOr("MaxExp", maxExp));
        loadItems(in, "Armor", armor);
        loadItems(in, "Weapons", weapons);
        setLootMode(in.getBooleanOr("LootMode", lootMode));
        in.childrenList("NpcInv").ifPresent(list -> {
            drops.clear();
            for (ValueInput entry : list) {
                DropSet ds = new DropSet();
                ds.load(entry);
                drops.add(ds);
            }
            onChange.run();
        });
        setSaveDropsName(in.getStringOr("SaveDropsName", saveDropsName));
        setLimitation(in.getIntOr("Limitation", limitation));
        setDropType(in.getIntOr("DropType", dropType));
    }

    private static void saveItems(ValueOutput out, String key, Map<Integer, ItemStack> map) {
        ValueOutput.ValueOutputList list = out.childrenList(key);
        for (Map.Entry<Integer, ItemStack> e : map.entrySet()) {
            if (e.getValue().isEmpty()) { continue; }
            ValueOutput entry = list.addChild();
            entry.putByte("Slot", e.getKey().byteValue());
            entry.store(ItemStack.MAP_CODEC, e.getValue());
        }
    }

    /**
     * A present list replaces the map (as in 1.12.2). Entries without a {@code Slot} or whose item
     * does not decode are skipped; the rest load.
     */
    private void loadItems(ValueInput in, String key, Map<Integer, ItemStack> map) {
        in.childrenList(key).ifPresent(list -> {
            map.clear();
            for (ValueInput entry : list) {
                var slot = entry.getInt("Slot");
                if (slot.isEmpty()) { continue; }
                @SuppressWarnings("deprecation")
                var stack = entry.read(ItemStack.MAP_CODEC);
                stack.filter(s -> !s.isEmpty()).ifPresent(s -> map.put(slot.get() & 0xFF, s));
            }
            onChange.run();
        });
    }
}
