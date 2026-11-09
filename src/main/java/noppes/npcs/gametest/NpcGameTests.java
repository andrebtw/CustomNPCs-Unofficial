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
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
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
import noppes.npcs.entity.data.DataInventory;
import noppes.npcs.entity.data.DropSet;
import noppes.npcs.faction.FactionChecks;
import noppes.npcs.faction.FactionRegistry;
import noppes.npcs.registry.NpcAttachments;
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
        FUNCTIONS.register("faction_registry_has_defaults_and_persists", () -> NpcGameTests::factionRegistryHasDefaultsAndPersists);
        FUNCTIONS.register("player_points_default_and_attach", () -> NpcGameTests::playerPointsDefaultAndAttach);
        FUNCTIONS.register("inventory_round_trip", () -> NpcGameTests::inventoryRoundTrip);
        FUNCTIONS.register("legacy_drop_shapes_load", () -> NpcGameTests::legacyDropShapesLoad);
        FUNCTIONS.register("bad_item_entries_are_skipped", () -> NpcGameTests::badItemEntriesAreSkipped);
        FUNCTIONS.register("full_data_round_trip", () -> NpcGameTests::fullDataRoundTrip);
        FUNCTIONS.register("summoned_weapons_are_held", () -> NpcGameTests::summonedWeaponsAreHeld);
        FUNCTIONS.register("vanilla_equip_change_survives_reload", () -> NpcGameTests::vanillaEquipChangeSurvivesReload);
        FUNCTIONS.register("unknown_faction_falls_back_to_first", () -> NpcGameTests::unknownFactionFallsBackToFirst);
        FUNCTIONS.register("default_npc_is_harmless", () -> NpcGameTests::defaultNpcIsHarmless);
        FUNCTIONS.register("gear_is_never_dropped_by_vanilla", () -> NpcGameTests::gearIsNeverDroppedByVanilla);
        FUNCTIONS.register("start_pos_is_set_when_added", () -> NpcGameTests::startPosIsSetWhenAdded);
        FUNCTIONS.register("summoned_vanilla_equipment_is_kept", () -> NpcGameTests::summonedVanillaEquipmentIsKept);
        FUNCTIONS.register("data_merge_equipment_is_kept", () -> NpcGameTests::dataMergeEquipmentIsKept);
        FUNCTIONS.register("unknown_faction_syncs_effective_id", () -> NpcGameTests::unknownFactionSyncsEffectiveId);
        FUNCTIONS.register("guaranteed_drop_keeps_chance_zero", () -> NpcGameTests::guaranteedDropKeepsChanceZero);
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

    private static void factionRegistryHasDefaultsAndPersists(GameTestHelper h) {
        var server = h.getLevel().getServer();
        var reg = FactionRegistry.get(server);
        h.assertTrue(reg == FactionRegistry.get(server), "same instance from data storage");
        h.assertTrue(reg.get(2).isAggressiveToNpc(0, java.util.Set.of()), "aggressive hates friendly");
        h.assertValueEqual(0, reg.getOrFirst(99).id, "fallback to first");
        var tag = FactionRegistry.TYPE.codec().encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, reg).getOrThrow();
        var copy = FactionRegistry.TYPE.codec().parse(net.minecraft.nbt.NbtOps.INSTANCE, tag).getOrThrow();
        h.assertValueEqual(3, copy.all().size(), "factions persist through the codec");
        h.succeed();
    }

    private static void playerPointsDefaultAndAttach(GameTestHelper h) {
        var p = h.makeMockPlayer(GameType.SURVIVAL);
        var reg = FactionRegistry.get(h.getLevel().getServer());
        h.assertTrue(FactionChecks.isAggressiveToPlayer(reg.get(2), p), "0 points < 500");
        h.assertTrue(FactionChecks.isFriendlyToPlayer(reg.get(0), p), "2000 >= 1500");
        h.assertTrue(FactionChecks.isNeutralToPlayer(reg.get(1), p), "1000 is neutral");
        h.assertFalse(FactionChecks.isAggressiveToPlayer(reg.get(2), h.makeMockPlayer(GameType.CREATIVE)), "creative exempt");
        p.getData(NpcAttachments.FACTION_POINTS).set(2, 600);
        h.assertFalse(FactionChecks.isAggressiveToPlayer(reg.get(2), p), "points from the attachment");
        h.succeed();
    }

    /** Saves with registry context (items need it) and wraps the result as input. */
    private static ValueInput roundTrip(GameTestHelper h, Consumer<ValueOutput> saver) {
        TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, h.getLevel().registryAccess());
        saver.accept(out);
        return input(h, out.buildResult());
    }

    private static ValueInput input(GameTestHelper h, CompoundTag tag) {
        return TagValueInput.create(ProblemReporter.DISCARDING, h.getLevel().registryAccess(), tag);
    }

    private static CompoundTag itemEntry(Integer slot, String id, int count) {
        var t = new CompoundTag();
        if (slot != null) { t.putByte("Slot", slot.byteValue()); }
        t.putString("id", id);
        t.putInt("count", count);
        return t;
    }

    private static void inventoryRoundTrip(GameTestHelper h) {
        var inv = new DataInventory(() -> {});
        inv.setWeapon(0, new ItemStack(Items.IRON_SWORD));
        inv.setWeapon(1, new ItemStack(Items.ARROW, 16));
        inv.setArmor(0, new ItemStack(Items.IRON_HELMET));
        inv.setArmor(3, new ItemStack(Items.GOLDEN_BOOTS));
        var ds = new DropSet();
        ds.item = new ItemStack(Items.EMERALD, 3);
        ds.setChance(50);
        ds.setLootMode(2);
        ds.setAmount(2, 5);
        inv.drops.add(ds);
        inv.setMinExp(3);
        inv.setMaxExp(9);
        inv.setLootMode(false);
        inv.setDropType(1);
        inv.setLimitation(4);
        inv.setSaveDropsName("bandit");
        var copy = new DataInventory(() -> {});
        copy.load(roundTrip(h, inv::save));
        h.assertValueEqual(Items.IRON_SWORD, copy.getWeapon(0).getItem(), "main hand");
        h.assertValueEqual(16, copy.getWeapon(1).getCount(), "projectile count");
        h.assertTrue(copy.getWeapon(2).isEmpty(), "off hand empty");
        h.assertValueEqual(Items.IRON_HELMET, copy.getArmor(0).getItem(), "head");
        h.assertValueEqual(Items.GOLDEN_BOOTS, copy.getArmor(3).getItem(), "feet");
        h.assertValueEqual(1, copy.drops.size(), "drop count");
        var d = copy.drops.get(0);
        h.assertValueEqual(Items.EMERALD, d.item.getItem(), "drop item");
        h.assertValueEqual(3, d.item.getCount(), "drop item count");
        h.assertValueEqual(50.0, d.chance, "chance");
        h.assertValueEqual(2, d.lootMode, "loot mode");
        h.assertValueEqual(2, d.amount[0], "min amount");
        h.assertValueEqual(5, d.amount[1], "max amount");
        h.assertValueEqual(3, copy.getMinExp(), "min exp");
        h.assertValueEqual(9, copy.getMaxExp(), "max exp");
        h.assertFalse(copy.isLootMode(), "loot mode flag");
        h.assertValueEqual(1, copy.getDropType(), "drop type");
        h.assertValueEqual(4, copy.getLimitation(), "limitation");
        h.assertValueEqual("bandit", copy.getSaveDropsName(), "save drops name");
        h.succeed();
    }

    private static void legacyDropShapesLoad(GameTestHelper h) {
        var drop = new CompoundTag();
        drop.put("Item", itemEntry(null, "minecraft:emerald", 1));
        drop.putBoolean("LootMode", true);
        var amount = new ListTag();
        amount.add(IntTag.valueOf(2));
        amount.add(IntTag.valueOf(4));
        drop.put("Amount", amount);
        var drops = new ListTag();
        drops.add(drop);
        var tag = new CompoundTag();
        tag.put("NpcInv", drops);
        tag.putInt("DropType", -5);
        var inv = new DataInventory(() -> {});
        inv.load(input(h, tag));
        h.assertValueEqual(1, inv.drops.size(), "drop loaded");
        h.assertValueEqual(1, inv.drops.get(0).lootMode, "boolean LootMode true -> 1");
        h.assertValueEqual(2, inv.drops.get(0).amount[0], "amount min from int list");
        h.assertValueEqual(4, inv.drops.get(0).amount[1], "amount max from int list");
        h.assertValueEqual(2, inv.getDropType(), "DropType -5 -> 5 -> 5 % 3 = 2");
        h.succeed();
    }

    private static void badItemEntriesAreSkipped(GameTestHelper h) {
        var weapons = new ListTag();
        weapons.add(itemEntry(0, "minecraft:nope", 1));
        weapons.add(itemEntry(null, "minecraft:stick", 1));
        weapons.add(itemEntry(2, "minecraft:shield", 1));
        var tag = new CompoundTag();
        tag.put("Weapons", weapons);
        var inv = new DataInventory(() -> {});
        inv.load(input(h, tag));
        h.assertTrue(inv.getWeapon(0).isEmpty(), "unknown item skipped");
        h.assertTrue(inv.getWeapon(1).isEmpty(), "entry without Slot skipped");
        h.assertValueEqual(Items.SHIELD, inv.getWeapon(2).getItem(), "valid entry kept");
        h.succeed();
    }

    private static EntityCustomNpc loadNpc(GameTestHelper h, CompoundTag tag) {
        return (EntityCustomNpc) EntityType.create(NpcEntities.CUSTOM_NPC.get(), input(h, tag), h.getLevel(),
                EntitySpawnReason.LOAD).orElseThrow();
    }

    private static CompoundTag saveNpc(GameTestHelper h, EntityCustomNpc npc) {
        TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, h.getLevel().registryAccess());
        npc.saveWithoutId(out);
        return out.buildResult();
    }

    private static void fullDataRoundTrip(GameTestHelper h) {
        var npc = h.spawn(NpcEntities.CUSTOM_NPC.get(), CENTER);
        npc.stats.melee.setStrength(9);
        npc.ai.setMoveSpeed(8);
        npc.ai.setStepHeight(1.0f);
        npc.advanced.setFactionId(2);
        npc.inventory.setWeapon(0, new ItemStack(Items.IRON_SWORD));
        var copy = reload(h, npc);
        h.assertValueEqual(9, copy.stats.melee.getStrength(), "melee strength");
        h.assertValueEqual(8, copy.ai.getMoveSpeed(), "move speed");
        h.assertValueEqual(2, copy.advanced.getFactionId(), "faction id");
        h.assertValueEqual(9.0, copy.getAttributeBaseValue(Attributes.ATTACK_DAMAGE), "ATTACK_DAMAGE");
        h.assertTrue(Math.abs(copy.getAttributeBaseValue(Attributes.MOVEMENT_SPEED) - 0.4) < 1e-6, "MOVEMENT_SPEED 8/20");
        h.assertTrue(Math.abs(copy.getAttributeBaseValue(Attributes.STEP_HEIGHT) - 1.0) < 1e-6, "STEP_HEIGHT");
        h.assertTrue(copy.getMainHandItem().is(Items.IRON_SWORD), "main hand");
        h.assertValueEqual(2, copy.getSyncedFaction(), "synced faction");
        h.succeed();
    }

    private static void summonedWeaponsAreHeld(GameTestHelper h) {
        var weapons = new ListTag();
        weapons.add(itemEntry(0, "minecraft:bow", 1));
        weapons.add(itemEntry(2, "minecraft:shield", 1));
        var armor = new ListTag();
        armor.add(itemEntry(0, "minecraft:golden_helmet", 1));
        armor.add(itemEntry(1, "minecraft:iron_chestplate", 1));
        armor.add(itemEntry(2, "minecraft:iron_leggings", 1));
        armor.add(itemEntry(3, "minecraft:iron_boots", 1));
        var tag = new CompoundTag();
        tag.put("Weapons", weapons);
        tag.put("Armor", armor);
        var npc = loadNpc(h, tag);
        h.assertTrue(npc.getMainHandItem().is(Items.BOW), "main hand bow");
        h.assertTrue(npc.getOffhandItem().is(Items.SHIELD), "off hand shield");
        h.assertTrue(npc.getItemBySlot(EquipmentSlot.HEAD).is(Items.GOLDEN_HELMET), "head");
        h.assertTrue(npc.getItemBySlot(EquipmentSlot.CHEST).is(Items.IRON_CHESTPLATE), "chest");
        h.assertTrue(npc.getItemBySlot(EquipmentSlot.LEGS).is(Items.IRON_LEGGINGS), "legs");
        h.assertTrue(npc.getItemBySlot(EquipmentSlot.FEET).is(Items.IRON_BOOTS), "feet");
        h.succeed();
    }

    private static void vanillaEquipChangeSurvivesReload(GameTestHelper h) {
        var npc = h.spawn(NpcEntities.CUSTOM_NPC.get(), CENTER);
        npc.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
        npc.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
        h.assertTrue(npc.inventory.getWeapon(2).is(Items.SHIELD), "inventory follows the slot");
        var copy = reload(h, npc);
        h.assertTrue(copy.inventory.getWeapon(2).is(Items.SHIELD), "inventory after reload");
        h.assertTrue(copy.getOffhandItem().is(Items.SHIELD), "off hand after reload");
        h.assertTrue(copy.inventory.getArmor(0).is(Items.DIAMOND_HELMET), "head in inventory");
        npc.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        h.assertTrue(npc.inventory.getWeapon(2).isEmpty(), "clearing the slot clears the inventory");
        h.succeed();
    }

    private static void unknownFactionFallsBackToFirst(GameTestHelper h) {
        var tag = new CompoundTag();
        tag.putInt("FactionID", 99);
        var npc = loadNpc(h, tag);
        h.assertValueEqual(0, npc.getFaction().id, "falls back to the first faction");
        h.assertValueEqual(99, saveNpc(h, npc).getIntOr("FactionID", -1), "saved id kept");
        h.succeed();
    }

    private static void defaultNpcIsHarmless(GameTestHelper h) {
        var npc = h.spawn(NpcEntities.CUSTOM_NPC.get(), CENTER);
        h.makeMockPlayer(GameType.SURVIVAL).setPos(h.absoluteVec(new Vec3(3.5, 1, 2.5)));
        h.assertValueEqual(0, npc.getFaction().id, "default faction");
        h.runAfterDelay(20, () -> {
            h.assertTrue(npc.getTarget() == null, "no target");
            h.succeed();
        });
    }

    private static void gearIsNeverDroppedByVanilla(GameTestHelper h) {
        var npc = h.spawn(NpcEntities.CUSTOM_NPC.get(), CENTER);
        for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND,
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            h.assertValueEqual(0f, npc.getDropChances().byEquipment(slot), "drop chance " + slot);
        }
        var copy = reload(h, npc);
        h.assertValueEqual(0f, copy.getDropChances().byEquipment(EquipmentSlot.MAINHAND), "after reload");
        h.succeed();
    }

    private static void startPosIsSetWhenAdded(GameTestHelper h) {
        var npc = h.spawn(NpcEntities.CUSTOM_NPC.get(), CENTER);
        h.assertValueEqual(npc.blockPosition(), npc.ai.getStartPos(), "start pos = spawn pos");
        var tag = new CompoundTag();
        tag.putIntArray("StartPosNew", new int[] {1, 2, 3});
        var loaded = loadNpc(h, tag);
        loaded.setPos(h.absoluteVec(new Vec3(2.5, 1, 2.5)));
        h.getLevel().addFreshEntity(loaded);
        h.assertValueEqual(new BlockPos(1, 2, 3), loaded.ai.getStartPos(), "saved start pos kept");
        h.succeed();
    }

    /** /summon with the 1.21 "equipment" compound (no Weapons/Armor keys). */
    private static void summonedVanillaEquipmentIsKept(GameTestHelper h) {
        var equipment = new CompoundTag();
        equipment.put("mainhand", itemEntry(null, "minecraft:diamond_sword", 1));
        equipment.put("head", itemEntry(null, "minecraft:diamond_helmet", 1));
        var tag = new CompoundTag();
        tag.put("equipment", equipment);
        var npc = loadNpc(h, tag);
        h.assertTrue(npc.getMainHandItem().is(Items.DIAMOND_SWORD), "main hand from equipment");
        h.assertTrue(npc.inventory.getWeapon(0).is(Items.DIAMOND_SWORD), "inventory follows");
        h.assertTrue(npc.getItemBySlot(EquipmentSlot.HEAD).is(Items.DIAMOND_HELMET), "head from equipment");
        var saved = saveNpc(h, npc);
        h.assertFalse(saved.contains("equipment"), "gear is saved as Weapons/Armor only");
        var copy = loadNpc(h, saved);
        h.assertTrue(copy.getMainHandItem().is(Items.DIAMOND_SWORD), "survives reload");
        h.succeed();
    }

    /** /data merge: the NPC's own saved tag plus a user "equipment" compound. */
    private static void dataMergeEquipmentIsKept(GameTestHelper h) {
        var npc = h.spawn(NpcEntities.CUSTOM_NPC.get(), CENTER);
        npc.inventory.setWeapon(0, new ItemStack(Items.IRON_SWORD));
        var merged = saveNpc(h, npc);
        var equipment = new CompoundTag();
        equipment.put("head", itemEntry(null, "minecraft:diamond_helmet", 1));
        merged.put("equipment", equipment);
        var copy = loadNpc(h, merged);
        h.assertTrue(copy.getItemBySlot(EquipmentSlot.HEAD).is(Items.DIAMOND_HELMET), "merged helmet kept");
        h.assertTrue(copy.inventory.getArmor(0).is(Items.DIAMOND_HELMET), "helmet in inventory");
        h.assertTrue(copy.getMainHandItem().is(Items.IRON_SWORD), "existing gear untouched");
        h.succeed();
    }

    private static void unknownFactionSyncsEffectiveId(GameTestHelper h) {
        var tag = new CompoundTag();
        tag.putInt("FactionID", 99);
        var npc = loadNpc(h, tag);
        h.assertValueEqual(0, npc.getSyncedFaction(), "synced id is the fallback faction");
        h.assertValueEqual(99, saveNpc(h, npc).getIntOr("FactionID", -1), "saved id kept");
        h.succeed();
    }

    /** Item pickup calls Mob.setGuaranteedDrop; NPC gear must still never drop through vanilla. */
    private static void guaranteedDropKeepsChanceZero(GameTestHelper h) {
        var npc = h.spawn(NpcEntities.CUSTOM_NPC.get(), CENTER);
        npc.setGuaranteedDrop(EquipmentSlot.MAINHAND);
        h.assertValueEqual(0f, npc.getDropChances().byEquipment(EquipmentSlot.MAINHAND), "drop chance");
        h.succeed();
    }
}
