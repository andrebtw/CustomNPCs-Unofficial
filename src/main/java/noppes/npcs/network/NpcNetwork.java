package noppes.npcs.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Replaces the 1.12.2 packets package (94 client-bound + 223 server-bound packet classes).
 * Stage 3: each packet becomes a CustomPacketPayload record with a StreamCodec,
 * registered here via registrar.playToClient / playToServer.
 */
public final class NpcNetwork {

    /** Bump whenever the wire format changes; mismatched clients are refused. */
    public static final String PROTOCOL_VERSION = "1";

    private NpcNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        // Stage 3: registrar.playToServer(...), registrar.playToClient(...)
    }
}
