package noppes.npcs.gametest;

import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.player.Player;

/**
 * A mock player that lives in the level like any entity (so selectors find it and it ticks), with a
 * game mode the test can switch. Not a ServerPlayer: it has no connection. Tests discard it when done,
 * because the GameTest cleanup leaves players alone.
 */
public final class TestPlayer extends Player {

    public GameType mode;
    /** Game times at which a hit got through. */
    public final List<Long> hits = new ArrayList<>();

    private TestPlayer(GameTestHelper h, GameType mode) {
        super(h.getLevel(), new GameProfile(UUID.randomUUID(), "test-npc-target"));
        this.mode = mode;
    }

    /** Spawns a player at {@code relative} (test coordinates). */
    public static TestPlayer spawn(GameTestHelper h, GameType mode, Vec3 relative) {
        TestPlayer player = new TestPlayer(h, mode);
        player.setPos(h.absoluteVec(relative));
        h.getLevel().addFreshEntity(player);
        return player;
    }

    @Override
    public GameType gameMode() {
        return mode;
    }

    @Override
    public boolean isClientAuthoritative() {
        return false;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        boolean hurt = super.hurtServer(level, source, amount);
        if (hurt) {
            hits.add(level.getGameTime());
        }
        return hurt;
    }
}
