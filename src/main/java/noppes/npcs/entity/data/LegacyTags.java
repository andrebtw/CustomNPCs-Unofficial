package noppes.npcs.entity.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import net.minecraft.nbt.ByteTag;
import net.minecraft.world.level.storage.ValueInput;

/** Helpers for 1.12.2 keys whose tag type changed over time. */
final class LegacyTags {

    private LegacyTags() {}

    /**
     * True when {@code key} holds a byte tag (how 1.12.2 stored booleans). Needed because the
     * numeric {@link ValueInput} getters accept every numeric tag type.
     */
    static boolean isByte(ValueInput in, String key) {
        return in.read(key, Codec.PASSTHROUGH).map(Dynamic::getValue).filter(v -> v instanceof ByteTag).isPresent();
    }
}
