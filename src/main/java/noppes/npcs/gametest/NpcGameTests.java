package noppes.npcs.gametest;

import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
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
}
