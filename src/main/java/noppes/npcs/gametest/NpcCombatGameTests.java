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
        FUNCTIONS.register("friendly_player_damage_refused", () -> NpcCombatGameTests::friendlyPlayerDamageRefused);
        FUNCTIONS.register("hit_makes_neutral_npc_fight_back", () -> NpcCombatGameTests::hitMakesNeutralNpcFightBack);
        FUNCTIONS.register("zero_damage_provokes_only_through_hurt_by", () -> NpcCombatGameTests::zeroDamageProvokesOnlyThroughHurtBy);
        FUNCTIONS.register("same_faction_npc_damage_refused", () -> NpcCombatGameTests::sameFactionNpcDamageRefused);
        FUNCTIONS.register("in_wall_damage_refused", () -> NpcCombatGameTests::inWallDamageRefused);
        FUNCTIONS.register("defend_faction_recruits_neighbour", () -> NpcCombatGameTests::defendFactionRecruitsNeighbour);
        FUNCTIONS.register("defend_faction_without_through_walls_needs_sight", () -> NpcCombatGameTests::defendFactionWithoutThroughWallsNeedsSight);
        FUNCTIONS.register("hit_registers_aggressor", () -> NpcCombatGameTests::hitRegistersAggressor);
        FUNCTIONS.register("rush_npc_walks_to_and_hits_player", () -> NpcCombatGameTests::rushNpcWalksToAndHitsPlayer);
        FUNCTIONS.register("melee_delay_respected", () -> NpcCombatGameTests::meleeDelayRespected);
        FUNCTIONS.register("melee_fire_effect_applied", () -> NpcCombatGameTests::meleeFireEffectApplied);
        FUNCTIONS.register("melee_potion_effect_applied", () -> NpcCombatGameTests::meleePotionEffectApplied);
        FUNCTIONS.register("melee_knockback_moves_target", () -> NpcCombatGameTests::meleeKnockbackMovesTarget);
        FUNCTIONS.register("no_tactic_still_hits", () -> NpcCombatGameTests::noTacticStillHits);
        FUNCTIONS.register("ai_disabled_never_hits", () -> NpcCombatGameTests::aiDisabledNeverHits);
        FUNCTIONS.register("default_npc_stays_put", () -> NpcCombatGameTests::defaultNpcStaysPut);
        FUNCTIONS.register("stagger_npc_still_reaches_and_hits", () -> NpcCombatGameTests::staggerNpcStillReachesAndHits);
        FUNCTIONS.register("orbit_npc_keeps_close_and_hits", () -> NpcCombatGameTests::orbitNpcKeepsCloseAndHits);
        FUNCTIONS.register("hit_and_run_restores_target", () -> NpcCombatGameTests::hitAndRunRestoresTarget);
        FUNCTIONS.register("hit_and_run_stop_clears_run", () -> NpcCombatGameTests::hitAndRunStopClearsRun);
        FUNCTIONS.register("stalk_npc_sneaks_when_close", () -> NpcCombatGameTests::stalkNpcSneaksWhenClose);
        FUNCTIONS.register("commander_recruits_neighbour_and_both_attack", () -> NpcCombatGameTests::commanderRecruitsNeighbourAndBothAttack);
        FUNCTIONS.register("commander_without_friends_attacks_alone", () -> NpcCombatGameTests::commanderWithoutFriendsAttacksAlone);
        FUNCTIONS.register("avoid_npc_keeps_distance", () -> NpcCombatGameTests::avoidNpcKeepsDistance);
        FUNCTIONS.register("panic_npc_moves_when_targeted", () -> NpcCombatGameTests::panicNpcMovesWhenTargeted);
        FUNCTIONS.register("sprint_sets_sprinting", () -> NpcCombatGameTests::sprintSetsSprinting);
        FUNCTIONS.register("pounce_leaps_at_mid_range", () -> NpcCombatGameTests::pounceLeapsAtMidRange);
        FUNCTIONS.register("on_attack_nothing_has_no_attack_goal", () -> NpcCombatGameTests::onAttackNothingHasNoAttackGoal);
        FUNCTIONS.register("calmdown_keeps_hit_and_run_near_home", () -> NpcCombatGameTests::calmdownKeepsHitAndRunNearHome);
        FUNCTIONS.register("provoked_target_death_is_cleared", () -> NpcCombatGameTests::provokedTargetDeathIsCleared);
        FUNCTIONS.register("provoked_target_out_of_range_is_cleared", () -> NpcCombatGameTests::provokedTargetOutOfRangeIsCleared);
        FUNCTIONS.register("defender_target_death_is_cleared", () -> NpcCombatGameTests::defenderTargetDeathIsCleared);
        FUNCTIONS.register("exit_aggressive_npc_hunts_and_hits_survival_player", () -> NpcCombatGameTests::exitAggressiveNpcHuntsAndHitsSurvivalPlayer);
        FUNCTIONS.register("exit_default_npc_is_harmless", () -> NpcCombatGameTests::exitDefaultNpcIsHarmless);
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

    static boolean hurt(GameTestHelper h, EntityCustomNpc npc, net.minecraft.world.damagesource.DamageSource source, float amount) {
        return npc.hurtServer(h.getLevel(), source, amount);
    }

    private static void friendlyPlayerDamageRefused(GameTestHelper h) {
        var npc = npc(h);
        var player = TestPlayer.spawn(h, GameType.SURVIVAL, offset(3, 0));
        h.assertFalse(hurt(h, npc, npc.damageSources().playerAttack(player), 4), "refused");
        h.assertValueEqual(20f, npc.getHealth(), "health");
        h.runAfterDelay(20, () -> {
            h.assertTrue(npc.getTarget() == null, "no target");
            player.discard();
            h.succeed();
        });
    }

    /** Faction 1 is neutral to a new player: no target on its own, but a hit is answered. */
    private static void hitMakesNeutralNpcFightBack(GameTestHelper h) {
        var npc = npc(h);
        npc.advanced.setFactionId(1);
        var player = TestPlayer.spawn(h, GameType.SURVIVAL, offset(8, 0));
        h.runAfterDelay(20, () -> {
            h.assertTrue(npc.getTarget() == null, "neutral: no target before the hit");
            h.assertTrue(hurt(h, npc, npc.damageSources().playerAttack(player), 4), "hurt");
            h.assertValueEqual(16f, npc.getHealth(), "health");
            h.assertTrue(npc.getTarget() == player, "fights back at once");
            player.discard();
            h.succeed();
        });
    }

    /**
     * 1.12.2 attackEntityFrom only takes up the attacker when damage > 0, but the hit still sets the
     * revenge target, so the vanilla hurt-by goal answers it a moment later.
     */
    private static void zeroDamageProvokesOnlyThroughHurtBy(GameTestHelper h) {
        var npc = npc(h);
        npc.advanced.setFactionId(1);
        var player = TestPlayer.spawn(h, GameType.SURVIVAL, offset(5, 0));
        h.startSequence()
                .thenIdle(5)
                .thenExecute(() -> {
                    hurt(h, npc, npc.damageSources().playerAttack(player), 0);
                    h.assertTrue(npc.getTarget() == null, "no direct retaliation for 0 damage");
                })
                .thenWaitUntil(() -> h.assertTrue(npc.getTarget() == player, "hurt-by goal answers"))
                .thenExecute(player::discard)
                .thenSucceed();
    }

    private static void sameFactionNpcDamageRefused(GameTestHelper h) {
        var a = aggressive(h, -2, 0);
        var b = aggressive(h, 2, 0);
        h.assertFalse(hurt(h, b, b.damageSources().mobAttack(a), 4), "refused");
        h.assertValueEqual(20f, b.getHealth(), "health");
        h.assertTrue(b.getTarget() == null, "no target");
        h.succeed();
    }

    private static void inWallDamageRefused(GameTestHelper h) {
        var npc = npc(h);
        h.assertFalse(hurt(h, npc, npc.damageSources().inWall(), 4), "refused");
        h.assertValueEqual(20f, npc.getHealth(), "health");
        h.succeed();
    }

    private static void defendFactionRecruitsNeighbour(GameTestHelper h) {
        var victim = npc(h, -3, 0);
        var neighbour = npc(h, 3, 0);
        neighbour.advanced.setDefendFaction(true);
        var husk = h.spawn(EntityType.HUSK, CENTER.north(5));
        h.runAfterDelay(2, () -> {
            hurt(h, victim, victim.damageSources().mobAttack(husk), 2);
            h.assertTrue(neighbour.getTarget() == husk, "neighbour defends its faction");
            h.succeed();
        });
    }

    /** A neighbour behind a wall only defends with ThroughWalls (within aggro range). */
    private static void defendFactionWithoutThroughWallsNeedsSight(GameTestHelper h) {
        for (int z = -3; z <= 3; z++) {
            for (int y = 0; y <= 3; y++) {
                h.setBlock(CENTER.offset(0, y, z), net.minecraft.world.level.block.Blocks.STONE);
            }
        }
        var victim = npc(h, -3, 0);
        var blind = npc(h, 3, -1);
        blind.advanced.setDefendFaction(true);
        blind.advanced.setThroughWalls(false);
        var hearing = npc(h, 3, 1);
        hearing.advanced.setDefendFaction(true);
        var husk = h.spawn(EntityType.HUSK, CENTER.west(6));
        h.runAfterDelay(2, () -> {
            hurt(h, victim, victim.damageSources().mobAttack(husk), 2);
            h.assertTrue(blind.getTarget() == null, "no sight, no ThroughWalls: ignores it");
            h.assertTrue(hearing.getTarget() == husk, "ThroughWalls: defends within aggro range");
            h.succeed();
        });
    }

    private static void hitRegistersAggressor(GameTestHelper h) {
        var npc = npc(h);
        npc.advanced.setFactionId(1);
        var player = TestPlayer.spawn(h, GameType.SURVIVAL, offset(3, 0));
        hurt(h, npc, npc.damageSources().playerAttack(player), 4);
        h.assertTrue(npc.combat.aggressors.containsKey(player), "aggressor recorded");
        h.assertTrue(npc.combat.priorityTarget == player, "first attacker is the priority target");
        player.discard();
        h.succeed();
    }

    /** Waits until the player took a hit, then checks it came from melee range. */
    private static void huntAndHit(GameTestHelper h, EntityCustomNpc npc, TestPlayer player) {
        h.startSequence()
                .thenWaitUntil(() -> h.assertFalse(player.hits.isEmpty(), "player was hit"))
                .thenExecute(() -> {
                    h.assertTrue(player.getHealth() < player.getMaxHealth(), "player lost health");
                    double reach = npc.stats.melee.getRange() + 1.5; // the hit's knockback (and Dodge's leap) may already apply
                    h.assertTrue(npc.distanceTo(player) <= reach, "hit from melee range: " + npc.distanceTo(player));
                    player.discard();
                })
                .thenSucceed();
    }

    private static void rushNpcWalksToAndHitsPlayer(GameTestHelper h) {
        var npc = aggressive(h, -4, 0);
        npc.stats.melee.setStrength(3);
        huntAndHit(h, npc, TestPlayer.spawn(h, GameType.SURVIVAL, offset(4, 0)));
    }

    private static void noTacticStillHits(GameTestHelper h) {
        var npc = aggressive(h, -4, 0);
        npc.ai.setTactic(6);
        huntAndHit(h, npc, TestPlayer.spawn(h, GameType.SURVIVAL, offset(4, 0)));
    }

    private static void meleeDelayRespected(GameTestHelper h) {
        var npc = aggressive(h, -1, 0);
        npc.stats.melee.setDelay(40);
        npc.stats.melee.setStrength(1);
        var player = TestPlayer.spawn(h, GameType.SURVIVAL, offset(1, 0));
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(player.hits.size() >= 3, "three hits"))
                .thenExecute(() -> {
                    for (int i = 1; i < player.hits.size(); i++) {
                        long gap = player.hits.get(i) - player.hits.get(i - 1);
                        h.assertTrue(gap >= 40, "hits at least AttackSpeed apart, was " + gap);
                    }
                    player.discard();
                })
                .thenSucceed();
    }

    private static void meleeFireEffectApplied(GameTestHelper h) {
        var npc = aggressive(h, -1, 0);
        npc.stats.melee.setEffectType(1);
        var player = TestPlayer.spawn(h, GameType.SURVIVAL, offset(1, 0));
        h.startSequence()
                .thenWaitUntil(() -> h.assertFalse(player.hits.isEmpty(), "hit"))
                .thenExecute(() -> {
                    h.assertTrue(player.getRemainingFireTicks() > 0, "set on fire");
                    player.discard();
                })
                .thenSucceed();
    }

    private static void meleePotionEffectApplied(GameTestHelper h) {
        var npc = aggressive(h, -1, 0);
        npc.stats.melee.setEffectType(2); // 1.12.2 PotionEffectType POISON
        npc.stats.melee.setEffectStrength(1);
        var player = TestPlayer.spawn(h, GameType.SURVIVAL, offset(1, 0));
        h.startSequence()
                .thenWaitUntil(() -> h.assertFalse(player.hits.isEmpty(), "hit"))
                .thenExecute(() -> {
                    var effect = player.getEffect(net.minecraft.world.effect.MobEffects.POISON);
                    h.assertTrue(effect != null, "poisoned");
                    h.assertValueEqual(1, effect.getAmplifier(), "amplifier = PotionAmp");
                    player.discard();
                })
                .thenSucceed();
    }

    private static void meleeKnockbackMovesTarget(GameTestHelper h) {
        var weak = npc(h, -6, -4);
        var strong = npc(h, -6, 4);
        strong.stats.melee.setKnockback(3);
        var pigWeak = h.spawnWithNoFreeWill(EntityType.PIG, offset(-5, -4));
        var pigStrong = h.spawnWithNoFreeWill(EntityType.PIG, offset(-5, 4));
        h.runAfterDelay(2, () -> {
            h.assertTrue(weak.doHurtTarget(h.getLevel(), pigWeak), "weak hit");
            h.assertTrue(strong.doHurtTarget(h.getLevel(), pigStrong), "strong hit");
            double vWeak = pigWeak.getDeltaMovement().horizontalDistance();
            double vStrong = pigStrong.getDeltaMovement().horizontalDistance();
            h.assertTrue(vStrong > vWeak + 0.2, "KnockBack pushes harder: " + vWeak + " vs " + vStrong);
            h.assertTrue(pigStrong.getDeltaMovement().x > 0, "pushed away from the NPC");
            h.succeed();
        });
    }

    private static void aiDisabledNeverHits(GameTestHelper h) {
        var npc = aggressive(h, -1, 0);
        npc.ai.setAiDisabled(true);
        var player = TestPlayer.spawn(h, GameType.SURVIVAL, offset(1, 0));
        h.runAfterDelay(2, () -> h.assertFalse(npc.doHurtTarget(h.getLevel(), player), "doHurtTarget refused"));
        h.runAfterDelay(100, () -> {
            h.assertTrue(player.hits.isEmpty(), "never hit");
            player.discard();
            h.succeed();
        });
    }

    private static void defaultNpcStaysPut(GameTestHelper h) {
        var npc = npc(h);
        var player = TestPlayer.spawn(h, GameType.SURVIVAL, offset(1.5, 0));
        h.runAfterDelay(20, () -> {
            var start = npc.position();
            h.runAfterDelay(60, () -> {
                h.assertTrue(npc.position().subtract(start).horizontalDistance() < 0.1, "did not move: " + start + " -> " + npc.position());
                h.assertTrue(player.hits.isEmpty(), "player unharmed");
                player.discard();
                h.succeed();
            });
        });
    }

    private static void staggerNpcStillReachesAndHits(GameTestHelper h) {
        var npc = aggressive(h, -4, 0);
        npc.ai.setTactic(1);
        huntAndHit(h, npc, TestPlayer.spawn(h, GameType.SURVIVAL, offset(4, 0)));
    }

    /** Orbit keeps min(TacticalRadius, AttackRange) from the target: in hit range with defaults. */
    private static void orbitNpcKeepsCloseAndHits(GameTestHelper h) {
        var npc = aggressive(h, -5, 0);
        npc.ai.setTactic(2);
        npc.ai.setTacticalRadius(6);
        var player = TestPlayer.spawn(h, GameType.SURVIVAL, offset(5, 0));
        h.startSequence()
                .thenWaitUntil(() -> h.assertFalse(player.hits.isEmpty(), "hit"))
                .thenIdle(40)
                .thenExecute(() -> {
                    double tr = Math.min(6, npc.stats.melee.getRange());
                    h.assertTrue(npc.distanceTo(player) <= tr * 1.1 + 1.5, "stays near: " + npc.distanceTo(player));
                    player.discard();
                })
                .thenSucceed();
    }

    private static void hitAndRunRestoresTarget(GameTestHelper h) {
        var npc = aggressive(h, -3, 0);
        npc.ai.setTactic(3);
        npc.ai.setTacticalRadius(6);
        npc.stats.melee.setStrength(1);
        var player = TestPlayer.spawn(h, GameType.SURVIVAL, offset(3, 0));
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(npc.getTarget() == null && !player.hits.isEmpty(), "ran off after a hit"))
                .thenExecute(() -> h.assertTrue(player.isAlive(), "player still alive"))
                .thenExecute(() -> h.assertFalse(npc.canNewAttack(), "no new targets while running"))
                .thenWaitUntil(() -> h.assertTrue(npc.getTarget() == player, "target restored"))
                .thenExecute(() -> {
                    h.assertTrue(npc.canNewAttack(), "free again");
                    player.discard();
                })
                .thenSucceed();
    }

    private static void hitAndRunStopClearsRun(GameTestHelper h) {
        var npc = aggressive(h, -3, 0);
        npc.ai.setTactic(3);
        npc.ai.setTacticalRadius(6);
        npc.stats.melee.setStrength(1);
        var player = TestPlayer.spawn(h, GameType.SURVIVAL, offset(3, 0));
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(npc.getTarget() == null && !player.hits.isEmpty(), "ran off after a hit"))
                .thenExecute(() -> h.assertTrue(player.isAlive(), "player still alive"))
                .thenExecute(() -> {
                    var goal = npc.tacticGoal;
                    npc.rebuildGoals(); // stops the running goals, as a data change or death would
                    h.assertTrue(goal.canNewAttack(), "a stopped HitAndRun keeps no run state");
                    player.discard();
                })
                .thenSucceed();
    }

    private static void stalkNpcSneaksWhenClose(GameTestHelper h) {
        var npc = aggressive(h, -3, 0);
        npc.ai.setTactic(5);
        npc.ai.setTacticalRadius(8);
        npc.stats.melee.setStrength(1);
        var player = TestPlayer.spawn(h, GameType.SURVIVAL, offset(3, 0));
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(npc.isShiftKeyDown(), "sneaks within TacticalRadius"))
                .thenExecute(player::discard)
                .thenWaitUntil(() -> h.assertFalse(npc.isShiftKeyDown(), "stands up once the target is gone"))
                .thenSucceed();
    }

    /** The neighbour's aggro range is too short to see the player: only the commander can call it in. */
    private static void commanderRecruitsNeighbourAndBothAttack(GameTestHelper h) {
        var commander = aggressive(h, -4, 0);
        commander.ai.setTactic(4);
        commander.stats.melee.setStrength(1);
        var neighbour = aggressive(h, -6, 0);
        neighbour.stats.setAggroRange(4);
        neighbour.stats.melee.setStrength(1);
        boolean[] recruited = {false};
        h.onEachTick(() -> recruited[0] |= neighbour.aiOwnerNpc == commander);
        TestPlayer[] holder = new TestPlayer[1];
        h.startSequence()
                .thenIdle(20) // both NPCs land first: no path can be made mid-air
                .thenExecute(() -> holder[0] = TestPlayer.spawn(h, GameType.SURVIVAL, offset(6, 0)))
                .thenWaitUntil(() -> h.assertTrue(commander.getTarget() == holder[0], "commander targets"))
                .thenWaitUntil(() -> h.assertTrue(neighbour.getTarget() == holder[0], "neighbour joins the attack"))
                .thenExecute(() -> {
                    h.assertTrue(recruited[0], "the neighbour was recruited first");
                    h.assertTrue(neighbour.aiOwnerNpc == null, "released on attack");
                    holder[0].discard();
                })
                .thenSucceed();
    }

    private static void commanderWithoutFriendsAttacksAlone(GameTestHelper h) {
        var npc = aggressive(h, -4, 0);
        npc.ai.setTactic(4);
        huntAndHit(h, npc, TestPlayer.spawn(h, GameType.SURVIVAL, offset(4, 0)));
    }

    private static void avoidNpcKeepsDistance(GameTestHelper h) {
        var npc = aggressive(h, 0, 0);
        npc.ai.setOnAttack(2);
        var player = TestPlayer.spawn(h, GameType.SURVIVAL, offset(3, 0));
        h.runAfterDelay(80, () -> {
            h.assertTrue(npc.getTarget() == player, "still has the target");
            h.assertTrue(npc.distanceTo(player) > 4.5, "backed away: " + npc.distanceTo(player));
            h.assertTrue(player.hits.isEmpty(), "never hits");
            player.discard();
            h.succeed();
        });
    }

    /** Panic re-picks a random spot every few ticks, so it is measured as distance travelled. */
    private static void panicNpcMovesWhenTargeted(GameTestHelper h) {
        var npc = npc(h);
        npc.advanced.setFactionId(1);
        npc.ai.setOnAttack(1);
        var player = TestPlayer.spawn(h, GameType.SURVIVAL, offset(3, 0));
        double[] travelled = {0};
        Vec3[] last = {null};
        // Registered up front: adding tick callbacks from inside one breaks the GameTest ticker.
        h.onEachTick(() -> {
            if (last[0] != null) {
                travelled[0] += npc.position().subtract(last[0]).horizontalDistance();
                last[0] = npc.position();
            }
        });
        h.runAfterDelay(20, () -> {
            npc.setTarget(player); // what a hit does, without the knockback
            last[0] = npc.position();
            h.runAfterDelay(60, () -> {
                h.assertTrue(travelled[0] > 2.0, "ran around: " + travelled[0] + " target " + npc.getTarget());
                h.assertTrue(player.hits.isEmpty(), "never hits");
                player.discard();
                h.succeed();
            });
        });
    }

    private static void sprintSetsSprinting(GameTestHelper h) {
        var npc = aggressive(h, -6, 0);
        npc.ai.setCanSprint(true);
        var player = TestPlayer.spawn(h, GameType.SURVIVAL, offset(6, 0));
        h.runAfterDelay(1, () -> h.succeedWhen(() -> {
            h.assertTrue(npc.isSprinting(), "sprints towards a far target");
            player.discard();
        }));
    }

    /** The NPC cannot walk (speed 0), so it stays in the 4-8 block band until the 1-in-5 roll. */
    private static void pounceLeapsAtMidRange(GameTestHelper h) {
        var npc = aggressive(h, -3, 0);
        npc.ai.setCanLeap(true);
        npc.ai.setMoveSpeed(0);
        var player = TestPlayer.spawn(h, GameType.SURVIVAL, offset(3, 0));
        h.succeedWhen(() -> {
            // A 6-block pounce is flat: about 1.28 horizontal, 0.26 up. The NPC itself cannot walk.
            h.assertTrue(!npc.onGround() && npc.getDeltaMovement().horizontalDistance() > 0.5, "leaps at the target");
            player.discard();
        });
    }

    private static void onAttackNothingHasNoAttackGoal(GameTestHelper h) {
        var npc = aggressive(h, -3, 0);
        npc.ai.setOnAttack(3);
        var player = TestPlayer.spawn(h, GameType.SURVIVAL, offset(3, 0));
        Vec3[] start = {null};
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(npc.getTarget() == player, "OnAttack 3 still targets"))
                .thenExecute(() -> start[0] = npc.position())
                .thenIdle(60)
                .thenExecute(() -> {
                    h.assertTrue(npc.tacticGoal == null, "no attack goal");
                    h.assertTrue(npc.position().subtract(start[0]).horizontalDistance() < 0.1, "never closes in");
                    h.assertTrue(player.hits.isEmpty(), "never hits");
                    player.discard();
                })
                .thenSucceed();
    }

    /** Stage 2b-2 exit check: a default-settings aggressive NPC hunts, reaches and hits a survival player. */
    private static void exitAggressiveNpcHuntsAndHitsSurvivalPlayer(GameTestHelper h) {
        var npc = aggressive(h, -6, 0);
        var player = TestPlayer.spawn(h, GameType.SURVIVAL, offset(6, 0));
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(npc.getTarget() == player, "targets the player"))
                .thenWaitUntil(() -> h.assertFalse(player.hits.isEmpty(), "reaches and hits the player"))
                .thenExecute(() -> {
                    h.assertTrue(npc.isAttacking(), "attacking during the fight");
                    h.assertTrue(player.getHealth() < player.getMaxHealth(), "player damaged");
                    player.discard();
                })
                .thenIdle(60)
                .thenExecute(() -> {
                    h.assertTrue(npc.getTarget() == null, "target dropped");
                    h.assertFalse(npc.isAttacking(), "combat over");
                })
                .thenSucceed();
    }

    /** Stage 2b-2 exit check: a default NPC next to a survival player does nothing. */
    private static void exitDefaultNpcIsHarmless(GameTestHelper h) {
        var npc = npc(h);
        var player = TestPlayer.spawn(h, GameType.SURVIVAL, offset(1.5, 0));
        Vec3[] start = {null};
        h.startSequence()
                .thenIdle(20)
                .thenExecute(() -> start[0] = npc.position())
                .thenIdle(100)
                .thenExecute(() -> {
                    h.assertTrue(player.hits.isEmpty(), "no damage");
                    h.assertTrue(npc.getTarget() == null, "no target");
                    h.assertFalse(npc.isAttacking(), "not attacking");
                    h.assertTrue(npc.position().subtract(start[0]).horizontalDistance() < 0.1, "did not move");
                    player.discard();
                })
                .thenSucceed();
    }

    /**
     * 1.12.2 CalmdownRange: HitAndRun only retreats to spots within 32 blocks (NpcNavRange) of home.
     * With home 100 blocks away no spot qualifies, so the NPC keeps its target after hitting.
     */
    private static void calmdownKeepsHitAndRunNearHome(GameTestHelper h) {
        var npc = aggressive(h, -3, 0);
        npc.ai.setTactic(3);
        npc.ai.setTacticalRadius(6);
        npc.ai.setReturnToStart(false);
        npc.stats.melee.setStrength(1);
        npc.ai.setStartPos(npc.blockPosition().offset(100, 0, 0));
        var player = TestPlayer.spawn(h, GameType.SURVIVAL, offset(3, 0));
        boolean[] ran = {false};
        h.onEachTick(() -> ran[0] |= !player.hits.isEmpty() && npc.getTarget() == null);
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(player.hits.size() >= 3, "keeps hitting"))
                .thenExecute(() -> {
                    h.assertFalse(ran[0], "never ran away from home");
                    player.discard();
                })
                .thenSucceed();
    }

    /** The player hits the NPC first (priority target set), then dies: the NPC must let go. */
    private static void provokedTargetDeathIsCleared(GameTestHelper h) {
        provokedTargetLost(h, (npc, player) -> player.kill(h.getLevel()));
    }

    private static void provokedTargetOutOfRangeIsCleared(GameTestHelper h) {
        provokedTargetLost(h, (npc, player) -> player.setPos(player.getX() + 40, player.getY(), player.getZ()));
    }

    private static void provokedTargetLost(GameTestHelper h, java.util.function.BiConsumer<EntityCustomNpc, TestPlayer> lose) {
        var npc = aggressive(h, -3, 0);
        npc.stats.melee.setStrength(1);
        var player = TestPlayer.spawn(h, GameType.SURVIVAL, offset(3, 0));
        var other = TestPlayer.spawn(h, GameType.SURVIVAL, offset(0, 12));
        other.mode = GameType.CREATIVE; // out of the way until the first fight is over
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(npc.getTarget() == player, "targets the player"))
                .thenExecute(() -> {
                    hurt(h, npc, npc.damageSources().playerAttack(player), 1);
                    h.assertTrue(npc.combat.priorityTarget == player, "provoked");
                    lose.accept(npc, player);
                })
                .thenWaitUntil(() -> h.assertTrue(npc.getTarget() == null, "lost target dropped"))
                .thenWaitUntil(() -> h.assertFalse(npc.isAttacking(), "combat over"))
                .thenExecute(() -> other.mode = GameType.SURVIVAL)
                .thenWaitUntil(() -> h.assertTrue(npc.getTarget() == other, "free to pick a new target"))
                .thenExecute(() -> {
                    player.discard();
                    other.discard();
                })
                .thenSucceed();
    }

    /** A DefendFaction NPC takes up a fight through onAttack (never hit itself); the foe then dies. */
    private static void defenderTargetDeathIsCleared(GameTestHelper h) {
        var victim = npc(h, -3, 0);
        var neighbour = npc(h, 3, 0);
        neighbour.advanced.setDefendFaction(true);
        var husk = h.spawn(EntityType.HUSK, CENTER.north(5));
        h.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    hurt(h, victim, victim.damageSources().mobAttack(husk), 2);
                    h.assertTrue(neighbour.getTarget() == husk, "defends");
                    husk.kill(h.getLevel());
                })
                .thenWaitUntil(() -> h.assertTrue(neighbour.getTarget() == null, "dead foe dropped"))
                .thenWaitUntil(() -> h.assertFalse(neighbour.isAttacking(), "combat over"))
                .thenSucceed();
    }
}
