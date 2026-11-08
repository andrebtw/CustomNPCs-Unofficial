package noppes.npcs.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import noppes.npcs.CustomNpcs;

/**
 * Client-only entry point. Replaces ClientProxy and every @SideOnly(Side.CLIENT) hook.
 * Never loaded on a dedicated server.
 */
@Mod(value = CustomNpcs.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = CustomNpcs.MODID, value = Dist.CLIENT)
public final class CustomNpcsClient {

    public CustomNpcsClient(ModContainer container) {
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        CustomNpcs.LOGGER.info("Custom NPCs client setup");
    }
}
