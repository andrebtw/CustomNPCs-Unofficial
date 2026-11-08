package noppes.npcs;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.gametest.GameTestHooks;
import noppes.npcs.gametest.NpcGameTests;
import noppes.npcs.network.NpcNetwork;
import noppes.npcs.registry.NpcEntities;
import noppes.npcs.registry.NpcAttachments;
import noppes.npcs.registry.NpcItems;
import noppes.npcs.registry.NpcRegistries;
import org.slf4j.Logger;

/**
 * Common entry point. Replaces the 1.12.2 {@code @Mod} class plus CommonProxy.
 * Client-only setup lives in {@link noppes.npcs.client.CustomNpcsClient}.
 */
@Mod(CustomNpcs.MODID)
public final class CustomNpcs {

    public static final String MODID = "customnpcs";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CustomNpcs(IEventBus modBus, ModContainer container) {
        NpcEntities.init();
        NpcItems.init();
        NpcAttachments.init();
        NpcRegistries.register(modBus);
        modBus.addListener(NpcEntities::registerAttributes);
        if (GameTestHooks.isGametestEnabled()) {
            NpcGameTests.register(modBus);
        }
        modBus.addListener(NpcNetwork::register);
        modBus.addListener(this::commonSetup);

        NeoForge.EVENT_BUS.register(this);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Custom NPCs common setup ({})", MODID);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Custom NPCs: server starting");
    }
}
