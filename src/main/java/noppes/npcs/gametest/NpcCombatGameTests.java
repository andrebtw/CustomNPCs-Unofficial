package noppes.npcs.gametest;

import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredRegister;
import noppes.npcs.CustomNpcs;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.registry.NpcEntities;

/**
 * Combat GameTests (stage 2b-2). They run in a 33x33 arena so NPCs, players and mobs have room, and
 * neighbouring tests stay out of each other's aggro range.
 */
public final class NpcCombatGameTests {

    static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS =
            DeferredRegister.create(Registries.TEST_FUNCTION, CustomNpcs.MODID);
    static final Identifier ARENA = Identifier.fromNamespaceAndPath(CustomNpcs.MODID, "empty_33x33");
    static final int MAX_TICKS = 400;
    static final BlockPos CENTER = new BlockPos(16, 1, 16);

    static {
        FUNCTIONS.register("ai_disabled_never_targets", () -> NpcCombatGameTests::aiDisabledNeverTargets);
        FUNCTIONS.register("combat_starts_and_resets", () -> NpcCombatGameTests::combatStartsAndResets);
        FUNCTIONS.register("set_target_refuses_creative_player", () -> NpcCombatGameTests::setTargetRefusesCreativePlayer);
        FUNCTIONS.register("priority_target_locks_target", () -> NpcCombatGameTests::priorityTargetLocksTarget);
        FUNCTIONS.register("in_range_is_a_box", () -> NpcCombatGameTests::inRangeIsABox);
    }

    private NpcCombatGameTests() {}

    static EntityCustomNpc npc(GameTestHelper h) {
        return h.spawn(NpcEntities.CUSTOM_NPC.get(), CENTER);
    }

    static Vec3 offset(double dx, double dz) {
        return new Vec3(CENTER.getX() + 0.5 + dx, CENTER.getY(), CENTER.getZ() + 0.5 + dz);
    }

    private static void aiDisabledNeverTargets(GameTestHelper h) {
        var npc = npc(h);
        npc.ai.setAiDisabled(true);
        var pig = h.spawn(EntityType.PIG, CENTER.east(3));
        h.assertFalse(npc.canAttack(pig), "AIDisabled NPCs can attack nothing (1.12.2 canAttackClass)");
        npc.onAttack(pig);
        h.assertTrue(npc.getTarget() == null, "onAttack ignored while AIDisabled");
        h.succeed();
    }

    private static void combatStartsAndResets(GameTestHelper h) {
        var npc = npc(h);
        var pig = h.spawn(EntityType.PIG, CENTER.east(3));
        npc.setTarget(pig);
        h.runAfterDelay(2, () -> {
            h.assertTrue(npc.isAttacking(), "attacking once it has a target");
            npc.setTarget(null); // what the target goals do when the target is lost (Task 3)
        });
        // 1.12.2 CombatHandler.update resets after more than 40 ticks without a valid target.
        h.runAfterDelay(30, () -> h.assertTrue(npc.isAttacking(), "still attacking within 40 ticks"));
        h.runAfterDelay(50, () -> {
            h.assertFalse(npc.isAttacking(), "combat reset");
            h.succeed();
        });
    }

    private static void setTargetRefusesCreativePlayer(GameTestHelper h) {
        var npc = npc(h);
        var player = TestPlayer.spawn(h, GameType.CREATIVE, offset(3, 0));
        npc.setTarget(player);
        h.assertTrue(npc.getTarget() == null, "creative refused");
        player.mode = GameType.SPECTATOR;
        npc.setTarget(player);
        h.assertTrue(npc.getTarget() == null, "spectator refused");
        player.mode = GameType.SURVIVAL;
        npc.setTarget(player);
        h.assertTrue(npc.getTarget() == player, "survival accepted");
        player.mode = GameType.CREATIVE;
        h.runAfterDelay(2, () -> {
            h.assertTrue(npc.getTarget() == null, "dropped after switching to creative");
            player.discard();
            h.succeed();
        });
    }

    private static void priorityTargetLocksTarget(GameTestHelper h) {
        var npc = npc(h);
        var a = h.spawn(EntityType.PIG, CENTER.east(3));
        var b = h.spawn(EntityType.PIG, CENTER.west(3));
        npc.setTarget(a);
        npc.combat.priorityTarget = a;
        npc.setTarget(b);
        h.assertTrue(npc.getTarget() == a, "kept while a priority target exists");
        npc.setTarget(null);
        h.assertTrue(npc.getTarget() == a, "not even cleared");
        npc.combat.priorityTarget = null;
        npc.setTarget(b);
        h.assertTrue(npc.getTarget() == b, "switches without a priority target");
        h.succeed();
    }

    private static void inRangeIsABox(GameTestHelper h) {
        var npc = npc(h);
        double x = npc.getX(), y = npc.getY(), z = npc.getZ();
        h.assertTrue(npc.isInRange(x + 10, y, z + 10, 10), "corner of the box is in range (not a sphere)");
        h.assertFalse(npc.isInRange(x + 10.5, y, z, 10), "beyond x");
        h.assertFalse(npc.isInRange(x, y - 10.5, z, 10), "below, even under y=0");
        h.succeed();
    }
}
