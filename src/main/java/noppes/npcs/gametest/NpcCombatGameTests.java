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
        FUNCTIONS.register("default_npc_ignores_survival_player", () -> NpcCombatGameTests::defaultNpcIgnoresSurvivalPlayer);
        FUNCTIONS.register("aggressive_npc_targets_survival_player", () -> NpcCombatGameTests::aggressiveNpcTargetsSurvivalPlayer);
        FUNCTIONS.register("creative_and_spectator_never_targeted", () -> NpcCombatGameTests::creativeAndSpectatorNeverTargeted);
        FUNCTIONS.register("target_switching_to_creative_is_dropped", () -> NpcCombatGameTests::targetSwitchingToCreativeIsDropped);
        FUNCTIONS.register("attack_other_factions_targets_hostile_npc", () -> NpcCombatGameTests::attackOtherFactionsTargetsHostileNpc);
        FUNCTIONS.register("npc_without_attack_other_factions_ignores_npcs", () -> NpcCombatGameTests::npcWithoutAttackOtherFactionsIgnoresNpcs);
        FUNCTIONS.register("direct_los_none_ignores_walls", () -> NpcCombatGameTests::directLosNoneIgnoresWalls);
        FUNCTIONS.register("target_out_of_range_is_cleared", () -> NpcCombatGameTests::targetOutOfRangeIsCleared);
        FUNCTIONS.register("target_death_clears_attacking", () -> NpcCombatGameTests::targetDeathClearsAttacking);
        FUNCTIONS.register("gets_attacked_faction_draws_monsters", () -> NpcCombatGameTests::getsAttackedFactionDrawsMonsters);
    }

    private NpcCombatGameTests() {}

    static EntityCustomNpc npc(GameTestHelper h) {
        return h.spawn(NpcEntities.CUSTOM_NPC.get(), CENTER);
    }

    static EntityCustomNpc npc(GameTestHelper h, double dx, double dz) {
        return h.spawn(NpcEntities.CUSTOM_NPC.get(), offset(dx, dz));
    }

    static EntityCustomNpc aggressive(GameTestHelper h, double dx, double dz) {
        var npc = npc(h, dx, dz);
        npc.advanced.setFactionId(2);
        return npc;
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

    private static void defaultNpcIgnoresSurvivalPlayer(GameTestHelper h) {
        var npc = npc(h);
        var player = TestPlayer.spawn(h, GameType.SURVIVAL, offset(5, 0));
        h.runAfterDelay(40, () -> {
            h.assertTrue(npc.getTarget() == null, "no target");
            h.assertFalse(npc.isAttacking(), "not attacking");
            player.discard();
            h.succeed();
        });
    }

    private static void aggressiveNpcTargetsSurvivalPlayer(GameTestHelper h) {
        var npc = aggressive(h, 0, 0);
        var player = TestPlayer.spawn(h, GameType.SURVIVAL, offset(6, 0));
        h.runAfterDelay(1, () -> h.succeedWhen(() -> {
            h.assertTrue(npc.getTarget() == player, "targets the player");
            h.assertTrue(npc.isAttacking(), "attacking");
            player.discard();
        }));
    }

    private static void creativeAndSpectatorNeverTargeted(GameTestHelper h) {
        var npc = aggressive(h, 0, 0);
        var creative = TestPlayer.spawn(h, GameType.CREATIVE, offset(5, 0));
        var spectator = TestPlayer.spawn(h, GameType.SPECTATOR, offset(-5, 0));
        h.runAfterDelay(60, () -> {
            h.assertTrue(npc.getTarget() == null, "no target");
            creative.discard();
            spectator.discard();
            h.succeed();
        });
    }

    private static void targetSwitchingToCreativeIsDropped(GameTestHelper h) {
        var npc = aggressive(h, 0, 0);
        var player = TestPlayer.spawn(h, GameType.SURVIVAL, offset(6, 0));
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(npc.getTarget() == player, "targets the player"))
                .thenExecute(() -> player.mode = GameType.CREATIVE)
                .thenIdle(3)
                .thenExecute(() -> h.assertTrue(npc.getTarget() == null, "dropped"))
                .thenIdle(40)
                .thenExecute(() -> h.assertTrue(npc.getTarget() == null, "not picked again"))
                .thenExecute(player::discard)
                .thenSucceed();
    }

    /** 1.12.2 Faction.isAggressiveToNpc: the attacker's faction hates the target's (2 hates 0). */
    private static void attackOtherFactionsTargetsHostileNpc(GameTestHelper h) {
        var hunter = aggressive(h, -4, 0);
        hunter.advanced.setAttackOtherFactions(true);
        var prey = npc(h, 4, 0);
        h.runAfterDelay(1, () -> h.succeedWhen(() -> h.assertTrue(hunter.getTarget() == prey, "targets the friendly NPC")));
    }

    private static void npcWithoutAttackOtherFactionsIgnoresNpcs(GameTestHelper h) {
        var hunter = aggressive(h, -4, 0);
        var prey = npc(h, 4, 0);
        h.runAfterDelay(60, () -> {
            h.assertTrue(hunter.getTarget() == null, "AttackOtherFactions off: NPCs are ignored");
            h.assertTrue(prey.getTarget() == null, "faction 0 does not hate faction 2");
            h.succeed();
        });
    }

    /** The player sits in a closed stone box; only DirectLOS NONE (6) targets through it. */
    private static void directLosNoneIgnoresWalls(GameTestHelper h) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                for (int y = 0; y <= 3; y++) {
                    boolean inside = x == 0 && z == 0 && (y == 1 || y == 2);
                    if (!inside) {
                        h.setBlock(CENTER.offset(x, y, z), net.minecraft.world.level.block.Blocks.STONE);
                    }
                }
            }
        }
        var player = TestPlayer.spawn(h, GameType.SURVIVAL, new Vec3(CENTER.getX() + 0.5, CENTER.getY() + 1, CENTER.getZ() + 0.5));
        var blind = aggressive(h, -6, 0);
        blind.ai.setSeeTarget(6);
        var normal = aggressive(h, 6, 0);
        normal.ai.setSeeTarget(0);
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(blind.getTarget() == player, "DirectLOS NONE targets through the wall"))
                .thenIdle(40)
                .thenExecute(() -> h.assertTrue(normal.getTarget() == null, "DirectLOS NORMAL needs a line of sight"))
                .thenExecute(player::discard)
                .thenSucceed();
    }

    private static void targetOutOfRangeIsCleared(GameTestHelper h) {
        var npc = aggressive(h, 0, 0);
        var player = TestPlayer.spawn(h, GameType.SURVIVAL, offset(6, 0));
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(npc.getTarget() == player, "targets the player"))
                .thenExecute(() -> player.setPos(player.getX() + 40, player.getY(), player.getZ()))
                .thenWaitUntil(() -> h.assertTrue(npc.getTarget() == null, "target cleared"))
                .thenWaitUntil(() -> h.assertFalse(npc.isAttacking(), "combat reset"))
                .thenExecute(player::discard)
                .thenSucceed();
    }

    private static void targetDeathClearsAttacking(GameTestHelper h) {
        var npc = aggressive(h, 0, 0);
        var player = TestPlayer.spawn(h, GameType.SURVIVAL, offset(6, 0));
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(npc.getTarget() == player, "targets the player"))
                .thenExecute(() -> player.kill(h.getLevel()))
                .thenWaitUntil(() -> h.assertTrue(npc.getTarget() == null, "dead target cleared"))
                .thenWaitUntil(() -> h.assertFalse(npc.isAttacking(), "combat reset"))
                .thenExecute(player::discard)
                .thenSucceed();
    }

    /** 1.12.2 onLivingUpdate: monsters near an NPC of a GetsAttacked faction attack it. */
    private static void getsAttackedFactionDrawsMonsters(GameTestHelper h) {
        var registry = noppes.npcs.faction.FactionRegistry.get(h.getLevel().getServer());
        var hated = new noppes.npcs.faction.Faction(77, "test.hated", 0, 1000);
        hated.getsAttacked = true;
        registry.put(hated);
        var npc = npc(h, -4, 0);
        npc.advanced.setFactionId(77);
        var husk = h.spawn(EntityType.HUSK, CENTER.east(4));
        h.runAfterDelay(1, () -> h.succeedWhen(() -> h.assertTrue(husk.getTarget() == npc, "the husk attacks the NPC")));
    }
}
