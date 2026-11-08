package noppes.npcs.entity.data;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * One NPC drop, from 1.12.2 {@code noppes.npcs.entity.data.DropSet}. The item is in the 1.21.11
 * {@link ItemStack} format under {@code Item}. {@code Availability} and the enchant/attribute/tag
 * settings only round-trip until the systems that use them are ported. {@code Slot} is written by
 * {@link DataInventory} from the drop's position.
 */
public final class DropSet {

    private static final int EMPTY_ITEM_MAX_STACK = 64;

    public ItemStack item = ItemStack.EMPTY;
    public double chance = 100.0d;
    public float damage = 1.0f;
    /** 0 on the ground, 1 at the killer's feet, 2 into the corpse inventory. */
    public int lootMode = 0;
    public boolean tiedToLevel = false;
    public int[] amount = {1, 1};
    public CompoundTag availability = new CompoundTag();
    public List<CompoundTag> enchantSettings = new ArrayList<>();
    public List<CompoundTag> attributeSettings = new ArrayList<>();
    public List<CompoundTag> tagSettings = new ArrayList<>();

    /** 1.12.2 {@code setChance}: clamped to [0.0001, 100], rounded to 4 decimals. */
    public void setChance(double value) {
        if (Double.isNaN(value)) { value = 100.0d; }
        chance = Math.round(Math.clamp(value, 0.0001d, 100.0d) * 10000.0d) / 10000.0d;
    }

    public void setDamage(float value) {
        if (Float.isNaN(value)) { value = 1.0f; }
        damage = Math.clamp(value, 0.0f, 1.0f);
    }

    public void setLootMode(int value) {
        lootMode = Math.clamp(value, 0, 2);
    }

    /** 1.12.2 {@code setAmount}: ordered, at least 1, at most the item's stack size. */
    public void setAmount(int min, int max) {
        int maxStack = item.isEmpty() ? EMPTY_ITEM_MAX_STACK : item.getMaxStackSize();
        int lo = Math.min(min, max);
        int hi = Math.max(min, max);
        lo = Math.clamp(lo, 1, maxStack);
        hi = Math.clamp(hi, lo, maxStack);
        amount = new int[] {lo, hi};
    }

    public void save(ValueOutput out) {
        if (!item.isEmpty()) {
            out.store("Item", ItemStack.CODEC, item);
        }
        out.putDouble("Chance", chance);
        out.putDouble("DamageToItem", damage);
        out.putInt("LootMode", lootMode);
        out.putBoolean("TiedToLevel", tiedToLevel);
        out.store("Availability", CompoundTag.CODEC, availability);
        out.putIntArray("Amount", amount.clone());
        out.store("EnchantSettings", CompoundTag.CODEC.listOf(), enchantSettings);
        out.store("AttributeSettings", CompoundTag.CODEC.listOf(), attributeSettings);
        out.store("TagSettings", CompoundTag.CODEC.listOf(), tagSettings);
    }

    public void load(ValueInput in) {
        in.read("Item", ItemStack.CODEC).filter(s -> !s.isEmpty()).ifPresent(s -> item = s);
        setChance(in.getDoubleOr("Chance", chance));
        setDamage(in.getFloatOr("DamageToItem", damage));
        if (LegacyTags.isByte(in, "LootMode")) {
            // Older saves: a boolean, true = drop at the killer's feet.
            setLootMode(in.getBooleanOr("LootMode", false) ? 1 : 0);
        } else {
            setLootMode(in.getIntOr("LootMode", lootMode));
        }
        tiedToLevel = in.getBooleanOr("TiedToLevel", tiedToLevel);
        in.read("Availability", CompoundTag.CODEC).ifPresent(t -> availability = t);
        int[] counts = in.getIntArray("Amount").orElse(null);
        if (counts == null) {
            // Older saves: a list of ints.
            counts = in.list("Amount", com.mojang.serialization.Codec.INT)
                    .map(l -> l.stream().mapToInt(Integer::intValue).toArray()).orElse(null);
        }
        if (counts != null) {
            int lo = counts.length >= 1 ? counts[0] : 1;
            int hi = counts.length >= 2 ? counts[1] : 1;
            setAmount(lo, hi);
        } else {
            setAmount(amount[0], amount[1]);
        }
        in.read("EnchantSettings", CompoundTag.CODEC.listOf()).ifPresent(l -> enchantSettings = new ArrayList<>(l));
        in.read("AttributeSettings", CompoundTag.CODEC.listOf()).ifPresent(l -> attributeSettings = new ArrayList<>(l));
        in.read("TagSettings", CompoundTag.CODEC.listOf()).ifPresent(l -> tagSettings = new ArrayList<>(l));
    }
}
