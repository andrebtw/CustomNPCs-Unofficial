package noppes.npcs.registry;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import noppes.npcs.items.ItemNpcWand;

/** Items and the creative tab. Replaces the 1.12.2 CustomItems / CustomTabs. */
public final class NpcItems {

    public static final DeferredItem<ItemNpcWand> NPC_WAND =
            NpcRegistries.ITEMS.registerItem("npcwand", ItemNpcWand::new, () -> new Item.Properties().stacksTo(1));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB =
            NpcRegistries.CREATIVE_TABS.register("tools", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.customnpcs"))
                    .icon(() -> NPC_WAND.get().getDefaultInstance())
                    .displayItems((params, output) -> output.accept(NPC_WAND.get()))
                    .build());

    private NpcItems() {}

    /** Forces class loading so the holders are added before the registers are attached to the bus. */
    public static void init() {}
}
