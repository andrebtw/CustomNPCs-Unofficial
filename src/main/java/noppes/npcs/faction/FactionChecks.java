package noppes.npcs.faction;

import net.minecraft.world.entity.player.Player;
import noppes.npcs.registry.NpcAttachments;

/** The player-facing 1.12.2 {@code Faction} checks; points come from the player's attachment. */
public final class FactionChecks {

    private FactionChecks() {}

    private static int points(Faction faction, Player player) {
        return player.getData(NpcAttachments.FACTION_POINTS).get(faction);
    }

    /** Creative players are never aggressed (1.12.2 {@code isAggressiveToPlayer}). */
    public static boolean isAggressiveToPlayer(Faction faction, Player player) {
        if (player.isCreative()) { return false; }
        return faction.isAggressiveAt(points(faction, player));
    }

    public static boolean isFriendlyToPlayer(Faction faction, Player player) {
        return faction.isFriendlyAt(points(faction, player));
    }

    public static boolean isNeutralToPlayer(Faction faction, Player player) {
        return faction.isNeutralAt(points(faction, player));
    }
}
