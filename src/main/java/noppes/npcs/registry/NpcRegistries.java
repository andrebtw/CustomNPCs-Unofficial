package noppes.npcs.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import noppes.npcs.CustomNpcs;

/**
 * One place for every DeferredRegister. Replaces CustomItems, CustomBlocks,
 * CustomEntities, CustomTabs, the sound registration and the 1.12.2 capabilities.
 * Stage 2 fills these in, one content class per register (NpcItems, NpcEntities...).
 */
public final class NpcRegistries {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CustomNpcs.MODID);
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(CustomNpcs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, CustomNpcs.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, CustomNpcs.MODID);
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, CustomNpcs.MODID);
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, CustomNpcs.MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CustomNpcs.MODID);
    /** Replaces item NBT (stack metadata/tag) from 1.12.2. */
    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, CustomNpcs.MODID);
    /** Replaces Forge capabilities (PlayerData, MarkData, ItemStackWrapper...). */
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, CustomNpcs.MODID);

    private NpcRegistries() {}

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        BLOCKS.register(modBus);
        ENTITY_TYPES.register(modBus);
        BLOCK_ENTITY_TYPES.register(modBus);
        MENU_TYPES.register(modBus);
        SOUND_EVENTS.register(modBus);
        CREATIVE_TABS.register(modBus);
        DATA_COMPONENTS.register(modBus);
        ATTACHMENT_TYPES.register(modBus);
    }
}
