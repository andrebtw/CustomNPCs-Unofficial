package noppes.npcs.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import noppes.npcs.entity.EntityCustomNpc;

/** Entity types. Replaces the 1.12.2 CustomEntities registration. */
public final class NpcEntities {

    /** Size and eye height are the 1.12.2 EntityNPCInterface baseWidth/baseHeight/baseEyeHeight. */
    public static final DeferredHolder<EntityType<?>, EntityType<EntityCustomNpc>> CUSTOM_NPC =
            NpcRegistries.ENTITY_TYPES.register("customnpc", id -> EntityType.Builder
                    .of(EntityCustomNpc::new, MobCategory.CREATURE)
                    .sized(0.6f, 1.9f)
                    .eyeHeight(1.615f)
                    .clientTrackingRange(10)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, id)));

    private NpcEntities() {}

    /** Forces class loading so the holders are added before the register is attached to the bus. */
    public static void init() {}

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(CUSTOM_NPC.get(), EntityCustomNpc.createAttributes().build());
    }
}
