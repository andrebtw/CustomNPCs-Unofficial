package noppes.npcs.util;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;

/**
 * The 1.12.2 {@code PotionEffectType} ids (as saved in {@code PotionEffect}) and their effects.
 * 0 is none and 1 is fire; both are handled by the caller.
 */
public final class NpcMobEffects {

    private static final List<Holder<MobEffect>> BY_ID = List.of(
            MobEffects.POISON, MobEffects.HUNGER, MobEffects.WEAKNESS, MobEffects.SLOWNESS, MobEffects.NAUSEA,
            MobEffects.BLINDNESS, MobEffects.WITHER, MobEffects.SPEED, MobEffects.HASTE, MobEffects.MINING_FATIGUE,
            MobEffects.STRENGTH, MobEffects.INSTANT_HEALTH, MobEffects.INSTANT_DAMAGE, MobEffects.JUMP_BOOST,
            MobEffects.REGENERATION, MobEffects.RESISTANCE, MobEffects.FIRE_RESISTANCE, MobEffects.WATER_BREATHING,
            MobEffects.INVISIBILITY, MobEffects.NIGHT_VISION, MobEffects.HEALTH_BOOST, MobEffects.ABSORPTION,
            MobEffects.SATURATION, MobEffects.GLOWING, MobEffects.LEVITATION, MobEffects.LUCK, MobEffects.UNLUCK);

    private NpcMobEffects() {}

    /** The effect for a 1.12.2 id from 2 (poison) to 28 (bad luck); null otherwise. */
    @Nullable
    public static Holder<MobEffect> byLegacyId(int id) {
        int index = id - 2;
        return index >= 0 && index < BY_ID.size() ? BY_ID.get(index) : null;
    }
}
