package noppes.npcs.registry;

import java.util.function.Supplier;
import net.neoforged.neoforge.attachment.AttachmentType;
import noppes.npcs.faction.PlayerFactionPoints;

/** Data attachments (replace the 1.12.2 PlayerData capability, piece by piece). */
public final class NpcAttachments {

    public static final Supplier<AttachmentType<PlayerFactionPoints>> FACTION_POINTS =
            NpcRegistries.ATTACHMENT_TYPES.register("faction_points", () -> AttachmentType
                    .builder(PlayerFactionPoints::new)
                    .serialize(PlayerFactionPoints.CODEC)
                    .copyOnDeath()
                    .build());

    private NpcAttachments() {}

    /** Loads the class so the entries are added before the registers are attached to the bus. */
    public static void init() {}
}
