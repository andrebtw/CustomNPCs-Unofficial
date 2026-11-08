package noppes.npcs.gametest;

import java.util.function.Consumer;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import noppes.npcs.CustomNpcs;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.NpcSpawner;
import noppes.npcs.registry.NpcItems;
import noppes.npcs.registry.NpcEntities;

/**
 * Headless tests run by {@code ./gradlew runGameTestServer} (a dedicated server). Registered only
 * when GameTests are enabled, so production servers get no extra registry entries.
 */
public final class NpcGameTests {

    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS =
            DeferredRegister.create(Registries.TEST_FUNCTION, CustomNpcs.MODID);
    private static final Identifier STRUCTURE = Identifier.fromNamespaceAndPath(CustomNpcs.MODID, "empty_5x5");
    private static final int MAX_TICKS = 100;
    private static final BlockPos CENTER = new BlockPos(2, 1, 2);

    static {
        FUNCTIONS.register("spawns_with_defaults", () -> NpcGameTests::spawnsWithDefaults);
        FUNCTIONS.register("round_trip_keeps_display_and_stats", () -> NpcGameTests::roundTripKeepsDisplayAndStats);
        FUNCTIONS.register("name_persists_and_is_not_rerolled", () -> NpcGameTests::namePersistsAndIsNotRerolled);
        FUNCTIONS.register("max_health_change_does_not_leave_health_above_max",
                () -> NpcGameTests::maxHealthChangeDoesNotLeaveHealthAboveMax);
        FUNCTIONS.register("summoned_max_health_spawns_at_full_health", () -> NpcGameTests::summonedMaxHealthSpawnsAtFullHealth);
        FUNCTIONS.register("name_tag_rename_survives_reload", () -> NpcGameTests::nameTagRenameSurvivesReload);
        FUNCTIONS.register("spawner_places_npc_above_clicked_block", () -> NpcGameTests::spawnerPlacesNpcAboveClickedBlock);
        FUNCTIONS.register("wand_denied_without_permission", () -> NpcGameTests::wandDeniedWithoutPermission);
        FUNCTIONS.register("wand_spawns_npc_with_permission", () -> NpcGameTests::wandSpawnsNpcWithPermission);
    }

    private NpcGameTests() {}

    public static void register(IEventBus modBus) {
        FUNCTIONS.register(modBus);
        modBus.addListener(NpcGameTests::registerInstances);
    }

    private static void registerInstances(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition> env = event.registerEnvironment(
                Identifier.fromNamespaceAndPath(CustomNpcs.MODID, "default"), new TestEnvironmentDefinition.AllOf());
        TestData<Holder<TestEnvironmentDefinition>> data = new TestData<>(env, STRUCTURE, MAX_TICKS, 0, true);
        for (DeferredHolder<Consumer<GameTestHelper>, ? extends Consumer<GameTestHelper>> fn : FUNCTIONS.getEntries()) {
            event.registerTest(fn.getId(), new FunctionGameTestInstance(fn.getKey(), data));
        }
    }

    /** Saves {@code npc} the way chunk saving does and loads a fresh copy (not added to the level). */
    private static EntityCustomNpc reload(GameTestHelper h, EntityCustomNpc npc) {
        TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, h.getLevel().registryAccess());
        npc.saveWithoutId(out);
        var in = TagValueInput.create(ProblemReporter.DISCARDING, h.getLevel().registryAccess(), out.buildResult());
        return (EntityCustomNpc) EntityType.create(NpcEntities.CUSTOM_NPC.get(), in, h.getLevel(), EntitySpawnReason.LOAD)
                .orElseThrow();
    }

    private static void spawnsWithDefaults(GameTestHelper h) {
        var npc = h.spawn(NpcEntities.CUSTOM_NPC.get(), CENTER);
        h.assertFalse(npc.display.getName().isBlank(), "random name");
        h.assertValueEqual(20f, npc.getMaxHealth(), "max health");
        h.assertValueEqual(1f, npc.getDisplayScale(), "scale");
        h.succeed();
    }

    private static void roundTripKeepsDisplayAndStats(GameTestHelper h) {
        var npc = h.spawn(NpcEntities.CUSTOM_NPC.get(), CENTER);
        npc.display.setName("Bob");
        npc.display.setSize(10f);
        npc.stats.setMaxHealth(40);
        npc.stats.setImmuneToFire(true);
        var copy = reload(h, npc);
        h.assertValueEqual("Bob", copy.display.getName(), "name");
        h.assertValueEqual(10f, copy.display.getSize(), "size");
        h.assertValueEqual(40f, copy.getMaxHealth(), "max health");
        h.assertTrue(copy.fireImmune(), "fire immune");
        h.assertValueEqual(2f, copy.getScale(), "SCALE attribute");
        h.assertValueEqual(2f, copy.getDisplayScale(), "synced size");
        h.succeed();
    }

    private static void namePersistsAndIsNotRerolled(GameTestHelper h) {
        var npc = h.spawn(NpcEntities.CUSTOM_NPC.get(), CENTER);
        npc.display.setName("");
        var copy = reload(h, npc);
        h.assertValueEqual("", copy.display.getName(), "empty name kept");
        h.succeed();
    }

    private static void maxHealthChangeDoesNotLeaveHealthAboveMax(GameTestHelper h) {
        var npc = h.spawn(NpcEntities.CUSTOM_NPC.get(), CENTER);
        npc.stats.setMaxHealth(40);
        npc.setHealth(40);
        npc.stats.setMaxHealth(10);
        h.assertTrue(npc.getHealth() <= 10f, "health clamped");
        h.succeed();
    }

    private static void spawnerPlacesNpcAboveClickedBlock(GameTestHelper h) {
        var ground = h.absolutePos(new BlockPos(2, 0, 2));
        var npc = NpcSpawner.spawnAt(h.getLevel(), ground, 90f);
        h.assertValueEqual(ground.above(), npc.blockPosition(), "position");
        h.assertValueEqual(90f, npc.getYRot(), "yaw");
        h.assertValueEqual(npc.getMaxHealth(), npc.getHealth(), "full health");
        h.succeed();
    }

    private static InteractionResult useWand(GameTestHelper h, Player player) {
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(NpcItems.NPC_WAND.get()));
        var pos = h.absolutePos(new BlockPos(2, 0, 2));
        var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        return player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
    }

    private static void wandDeniedWithoutPermission(GameTestHelper h) {
        var player = h.makeMockPlayer(GameType.SURVIVAL); // PermissionSet.NO_PERMISSIONS
        h.assertValueEqual(InteractionResult.FAIL, useWand(h, player), "denied");
        h.assertEntityNotPresent(NpcEntities.CUSTOM_NPC.get());
        h.succeed();
    }

    private static void wandSpawnsNpcWithPermission(GameTestHelper h) {
        var player = new Player(h.getLevel(), new GameProfile(UUID.randomUUID(), "test-mock-op")) {
            @Override
            public GameType gameMode() { return GameType.CREATIVE; }

            @Override
            public boolean isClientAuthoritative() { return false; }

            @Override
            public PermissionSet permissions() { return LevelBasedPermissionSet.GAMEMASTER; }
        };
        h.assertValueEqual(InteractionResult.SUCCESS, useWand(h, player), "allowed");
        h.assertEntityPresent(NpcEntities.CUSTOM_NPC.get(), new BlockPos(2, 1, 2));
        h.succeed();
    }

    /** /summon-style flat NBT with no "attributes" list: health must follow the loaded MaxHealth. */
    private static void summonedMaxHealthSpawnsAtFullHealth(GameTestHelper h) {
        var tag = new CompoundTag();
        tag.putDouble("MaxHealth", 100.0);
        var npc = (EntityCustomNpc) EntityType.create(NpcEntities.CUSTOM_NPC.get(),
                TagValueInput.create(ProblemReporter.DISCARDING, h.getLevel().registryAccess(), tag),
                h.getLevel(), EntitySpawnReason.LOAD).orElseThrow();
        h.assertValueEqual(100f, npc.getMaxHealth(), "max health");
        h.assertValueEqual(100f, npc.getHealth(), "health");
        h.succeed();
    }

    /** A vanilla rename (name tag, /data) must update display.name so it is not reverted on reload. */
    private static void nameTagRenameSurvivesReload(GameTestHelper h) {
        var npc = h.spawn(NpcEntities.CUSTOM_NPC.get(), CENTER);
        npc.setCustomName(Component.literal("Tagged"));
        h.assertValueEqual("Tagged", npc.display.getName(), "display name follows CustomName");
        var copy = reload(h, npc);
        h.assertValueEqual("Tagged", copy.display.getName(), "display name after reload");
        h.assertValueEqual("Tagged", copy.getCustomName().getString(), "CustomName after reload");
        h.succeed();
    }
}
