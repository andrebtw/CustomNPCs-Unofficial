package noppes.npcs.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.Identifier;
import noppes.npcs.entity.data.DataDisplay;

/**
 * Turns the NPC's texture string into an {@link Identifier}, falling back to the default Steve
 * skin for anything that is not a valid identifier. No client-only imports: the entity calls this
 * on both sides.
 */
public final class NpcTextures {

    public static final Identifier DEFAULT = Identifier.parse(DataDisplay.DEFAULT_TEXTURE);

    private static final Map<String, Identifier> CACHE = new ConcurrentHashMap<>();

    private NpcTextures() {}

    public static Identifier resolve(String raw) {
        if (raw == null || raw.isBlank()) {
            return DEFAULT;
        }
        return CACHE.computeIfAbsent(raw, s -> {
            Identifier id = Identifier.tryParse(s);
            return id != null ? id : DEFAULT;
        });
    }
}
