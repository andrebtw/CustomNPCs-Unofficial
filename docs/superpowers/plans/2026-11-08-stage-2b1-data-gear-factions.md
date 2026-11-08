# Stage 2b-1: NPC Data, Gear and Minimal Factions Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Every 1.12.2 combat/AI/inventory/faction NBT key on the NPC loads and saves; NPCs hold and wear their gear; a server-side faction registry and per-player faction points answer the 1.12.2 hostility questions.

**Architecture:** Same pattern as 2a: entity-independent data POJOs (`onChange` callback, `save(ValueOutput)` / `load(ValueInput)`, missing key keeps the current value), owned by `EntityCustomNpc`, which maps them onto attributes, equipment slots and synced data. Factions live in a `SavedData` on the overworld plus a player data attachment. No combat behaviour yet (2b-2): a default NPC stays harmless.

**Tech Stack:** NeoForge 21.11.48, Minecraft 1.21.11 (Parchment 2025.12.20), ModDevGradle 2.0.148, Java 21, JUnit 5 (MDG `unitTest`), registry-based GameTests.

**Spec:** `docs/superpowers/specs/2026-11-08-stage-2b-combat-design.md` (slice 2b-1: goals 1, 2, 7 and their tests from goal 8).

## Global Constraints

- Source of truth: `/home/andre/Documents/CustomNPCs-Unofficial` master (read-only). Keys, defaults and ranges below are copied from it; when this plan and that code disagree on a value, the code wins and the deviation is ledgered.
- NBT keys verbatim from 1.12.2, flat at the entity's top level (DataStats, DataMelee, DataRanged, DataAI, DataInventory, DataAdvanced all write into the same compound, as 1.12.2 does). This includes the misspellings `AttackStrenght`, `MovingPatern`, `FrendFactions`.
- A key missing on load keeps the field's current value. A key of the wrong type behaves as missing.
- Out-of-range values (including enum ordinals) are clamped, never thrown. 1.12.2 throws or crashes on some of these; the port does not.
- Items use the 1.21.11 `ItemStack` format (`ItemStack.OPTIONAL_CODEC` / map codec) under the 1.12.2 key names, each list entry carrying a byte `Slot` like 1.12.2 `NBTTags.nbtIItemStackMap`. 1.12.2 item NBT conversion is out of scope (controllers stage).
- `Identifier`, not `ResourceLocation`. `ValueInput`/`ValueOutput` persistence. Check every 1.21.11 API in `build/moddev/artifacts/neoforge-21.11.48-sources.jar`.
- Client-only classes only under `noppes.npcs.client`, referenced only from `CustomNpcsClient`. No mixins, no access transformers.
- Only code from `CustomNPCs-Unofficial` (CC BY-NC 3.0) and the NeoForge MDK may be copied.
- Each task ends with `./gradlew build` (and `runGameTestServer` where the task has GameTests) passing and a local commit on `neoforge-1.21.11`. **Do not push.**

## Review Focus

1. **Legacy key shapes** (`DirectLOS` as boolean, drop `LootMode` as boolean, `Amount` as int list, `Resistances` as the old compound `{Arrow,Melee,Knockback,Explosion}`, `AttackRange`/`MaxFiringRange` as int): load to the same values 1.12.2 would. → Tasks 1, 2, 3, 5 `legacy*` tests.
2. **Out-of-range ordinals and numbers** (`TacticalVariant:99`, `NPCRarity:-1`, `CreatureType:7`, `SpawnCycle:9`, `DirectLOS:50`, `MoveSpeed:40`, `ShotCount:0`): clamped, no exception. → Tasks 2, 3 `*IsClamped` tests.
3. **Vanilla equipment changes** (`/item replace entity … weapon.mainhand`, a dispenser equipping armour): kept in `DataInventory` and survive reload. → Task 6 `vanillaEquipChangeSurvivesReload`.
4. **Unknown faction id** (`FactionID:99`, or a faction deleted from the registry): the NPC uses the first faction, nothing crashes, and the saved id is kept. → Task 6 `unknownFactionFallsBackToFirst`.
5. **Bad item entries** (`Weapons:[{Slot:0b,id:"minecraft:nope",count:1}]`, an entry without `Slot`): the entry is skipped, the rest loads. → Task 5 `badItemEntriesAreSkipped`.

---

### Task 1: `DataMelee` and `DataRanged`

**Files:**
- Create: `src/main/java/noppes/npcs/entity/data/DataMelee.java`
- Create: `src/main/java/noppes/npcs/entity/data/DataRanged.java`
- Test: `src/test/java/noppes/npcs/entity/data/CombatDataTest.java`

**Interfaces:**
- Produces:
  - `DataMelee(Runnable onChange)`. Fields (1.12.2 `DataMelee.java`), getters `getX()` and setters `setX(..)` firing `onChange` only on real change:
    `int strength = 5` (key `AttackStrenght`), `int delay = 20` (`AttackSpeed`), `int knockback = 0` (`KnockBack`), `int effectType = 0` (`PotionEffect`), `int effectTime = 5` (`PotionDuration`), `int effectStrength = 0` (`PotionAmp`), `double range = 2.0` (`AttackRange`, clamped `[0.2, 30.0]`; loads from an int tag too). Saves `version:1`.
  - `DataRanged(Runnable onChange)`, same rules. Fields and keys (1.12.2 `DataRanged.java`):
    `burstCount=1 BurstCount`, `pSpeed=10 pSpeed [0,100]`, `pDamage=4 pDamage`, `pImpact=0 pImpact`, `pSize=5 pSize`, `pArea=0 pArea`, `pTrail=0 pTrail`, `double range=15.0 MaxFiringRange [1,64]` (int tag too), `fireRate=5 FireRate`, `minDelay=20 minDelay [1,9999]`, `maxDelay=40 maxDelay [1,9999]`, `shotCount=1 ShotCount [1,10]`, `accuracy=60 Accuracy [1,100]`, booleans `render3D=true pRender3D`, `spin=false pSpin`, `stick=false pStick`, `physics=true pPhysics`, `accelerate=false pXlr8`, `glows=false pGlows`, `aimWhileShooting=false AimWhileShooting`, `pEffect=0 pEffect`, `pDur=5 pDur`, `pEffAmp=0 pEffAmp`, strings `fireSound="minecraft:entity.arrow.shoot" FiringSound`, `hitSound="minecraft:entity.arrow.hit" HitSound`, `groundSound="minecraft:block.stone.break" GroundSound`, `fireIndirect=0 FireIndirect`, `meleeDistance=0 DistanceToMelee`. Saves `version:1`.
  - Version migration (both classes): if a `version` int is present and `< 1`, add 1 to `knockback` / `pImpact` after loading, as 1.12.2 does. An absent `version` means current (Ruling: 1.12.2 treats absent as 0; doing so would bump knockback on every hand-written `/summon`).

- [ ] **Step 1: Write the failing tests** (helpers `save`/`in` as in `NpcDataTest`)

```java
@Test void meleeDefaultsAndKeys() {
    var t = save(new DataMelee(() -> {})::save);
    assertEquals(5, t.getIntOr("AttackStrenght", -1)); assertEquals(20, t.getIntOr("AttackSpeed", -1));
    assertEquals(2.0, t.getDoubleOr("AttackRange", -1)); assertEquals(1, t.getIntOr("version", -1));
}
@Test void rangedRoundTrip() { /* set every field to a non-default in range, save, load into a fresh one, assertEquals each */ }
@Test void legacyIntRangesLoad() {
    var t = new CompoundTag(); t.putInt("AttackRange", 4); t.putInt("MaxFiringRange", 30);
    var m = new DataMelee(() -> {}); m.load(in(t)); assertEquals(4.0, m.getRange());
    var r = new DataRanged(() -> {}); r.load(in(t)); assertEquals(30.0, r.getRange());
}
@Test void oldVersionBumpsKnockback() {
    var t = new CompoundTag(); t.putInt("KnockBack", 1); t.putInt("pImpact", 2); t.putInt("version", 0);
    var m = new DataMelee(() -> {}); m.load(in(t)); assertEquals(2, m.getKnockback());
    var r = new DataRanged(() -> {}); r.load(in(t)); assertEquals(3, r.getPImpact());
    var u = new CompoundTag(); u.putInt("KnockBack", 1);
    var m2 = new DataMelee(() -> {}); m2.load(in(u)); assertEquals(1, m2.getKnockback());
}
@Test void rangesAreClamped() {
    var r = new DataRanged(() -> {}); r.setShotCount(0); assertEquals(1, r.getShotCount());
    r.setRange(500); assertEquals(64.0, r.getRange()); r.setAccuracy(0); assertEquals(1, r.getAccuracy());
    var m = new DataMelee(() -> {}); m.setRange(0); assertEquals(0.2, m.getRange());
}
```

- [ ] **Step 2: Run them, confirm they fail**

Run: `./gradlew test --tests noppes.npcs.entity.data.CombatDataTest`
Expected: compilation failure, `DataMelee` not found.

- [ ] **Step 3: Implement both classes**

- [ ] **Step 4: Run, confirm pass**

Run: `./gradlew test` — Expected: `BUILD SUCCESSFUL`, all tests pass.

- [ ] **Step 5: Commit** — `git commit -m "Port DataMelee and DataRanged with 1.12.2 NBT keys"`

---

### Task 2: `DataStats` remainder and `Resistances`

**Files:**
- Modify: `src/main/java/noppes/npcs/entity/data/DataStats.java`
- Create: `src/main/java/noppes/npcs/entity/data/Resistances.java`
- Test: extend `src/test/java/noppes/npcs/entity/data/NpcDataTest.java`

**Interfaces:**
- Consumes: `DataMelee`, `DataRanged` (Task 1).
- Produces:
  - `DataStats` gains `public final DataMelee melee; public final DataRanged ranged; public final Resistances resistances;` (all built with the same `onChange`). `save`/`load` also call `melee`/`ranged`/`resistances` on the **same** output/input (flat keys).
  - New fields (1.12.2 `DataStats.java`), getters/setters with the 2a change rule: `int spawnCycle = 0` (`SpawnCycle`, clamp `[0,4]`), `int creatureType = 0` (`CreatureType`, clamp `[0,4]`: 1.12.2 `EnumCreatureAttribute` UNDEFINED, UNDEAD, ARTHROPOD, ILLAGER, WATER), `boolean potionImmune = false` (`PotionImmune`), `boolean ignoreCobweb = false` (`IgnoreCobweb`), `float chanceBlockDamage = 2.0f` (`ChanceBlockDamage`; negative → absolute value, `> 100` → 100), `int level = 1` (`NPCLevel`, clamp `[1, DataStats.MAX_LEVEL = 45]`, the 1.12.2 config default), `int rarity = 0` (`NPCRarity`, clamp `[0,2]`: `EnumCreatureRarity`), `String rarityTitle = "§flv.§21"` (`RarityTitle`), `boolean calmdown = true` (`CalmdownRange`).
  - `Resistances(Runnable onChange)`: `Map<String, Float>` with defaults `arrow, mob, knockback, explosion = 1.0f`. `float get(String damageName)` with the 1.12.2 aliases (`explosion.player`→`explosion`, `player`→`mob`, `thrown`→`arrow`, else 1.0). `void set(String damageName, float value)` clamps `[0, 2]`. Saves `Resistances` as a list of `{K:string, V:float}`. Load: a list replaces the map (1.12.2 `load` clears first); an old compound maps `Arrow`→arrow, `Melee`→mob, `Knockback`→knockback, `Explosion`→explosion; absent keeps current.

- [ ] **Step 1: Write the failing tests**

```java
@Test void statsNewKeysRoundTrip() { /* set every new field non-default, round trip, assert each, incl. resistances.set("fall", 0.5f) */ }
@Test void statsWritesMeleeAndRangedFlat() {
    var t = save(new DataStats(() -> {})::save);
    assertEquals(5, t.getIntOr("AttackStrenght", -1)); assertEquals(10, t.getIntOr("pSpeed", -1));
    assertEquals(4, t.getListOrEmpty("Resistances").size());
}
@Test void statsOrdinalsAreClamped() {
    var t = new CompoundTag(); t.putInt("SpawnCycle", 9); t.putInt("CreatureType", 7); t.putInt("NPCRarity", -1); t.putInt("NPCLevel", 500);
    var s = new DataStats(() -> {}); s.load(in(t));
    assertEquals(4, s.getSpawnCycle()); assertEquals(4, s.getCreatureType()); assertEquals(0, s.getRarity()); assertEquals(45, s.getLevel());
}
@Test void legacyResistancesCompoundLoads() {
    var old = new CompoundTag(); old.putFloat("Arrow", 0.5f); old.putFloat("Melee", 1.5f);
    var t = new CompoundTag(); t.put("Resistances", old);
    var s = new DataStats(() -> {}); s.load(in(t));
    assertEquals(0.5f, s.resistances.get("arrow")); assertEquals(1.5f, s.resistances.get("player"));
}
@Test void chanceBlockDamageFollows112Rules() {
    var s = new DataStats(() -> {}); s.setChanceBlockDamage(-30f); assertEquals(30f, s.getChanceBlockDamage());
    s.setChanceBlockDamage(400f); assertEquals(100f, s.getChanceBlockDamage());
}
```

- [ ] **Step 2: Run, confirm the new tests fail to compile**
- [ ] **Step 3: Implement**
- [ ] **Step 4: `./gradlew test` passes** (all old `NpcDataTest` tests still pass)
- [ ] **Step 5: Commit** — `"Port remaining DataStats keys and Resistances"`

---

### Task 3: `DataAI` (all keys)

**Files:**
- Create: `src/main/java/noppes/npcs/entity/data/DataAI.java`
- Test: `src/test/java/noppes/npcs/entity/data/DataAITest.java`

**Interfaces:**
- Produces: `DataAI(Runnable onChange)`, getters/setters with the change rule. Fields, defaults and keys (1.12.2 `DataAI.java`):
  booleans `canSwim=true CanSwim`, `reactsToFire=false ReactsToFire`, `avoidsWater=false AvoidsWater`, `avoidsSun=false AvoidsSun`, `returnToStart=true ReturnToStart`, `canLeap=false CanLeap`, `canSprint=false CanSprint`, `canBeCollide=true CanBeCollide`, `movingPause=true MovingPause`, `npcInteracting=true npcInteracting`, `stopAndInteract=true stopAndInteract`, `attackInvisible=false AttackInvisible`, `mountControl=false MountControl`, `aiDisabled=false AIDisabled`;
  ints `onAttack=0 OnAttack [0,3]`, `doorInteract=2 DoorInteract`, `findShelter=2 FindShelter [0,2]`, `tacticalRadius=8 TacticalRadius`, `animationType=0 MoveState`, `standingType=0 StandingState [0,4]`, `movingType=0 MovingState [0,2]`, `movementType=0 MovementType [0,2]`, `orientation=0 Orientation`, `walkingRange=10 WalkingRange`, `moveSpeed=5 MoveSpeed [0,10]`, `movingPos=0 MovingPos`, `movingPattern=0 MovingPatern`, `activeRange=32 ActiveRange`, `maxHurtResistantTime=20 MaxHurtResistantTime`;
  floats `bodyOffsetX/Y/Z=5.0 PositionOffsetX/Y/Z [0,9.99]`, `stepHeight=0.6 StepHeight`;
  `int tactic = 0` stored as `TacticalVariant` ordinal of `EnumNpcTactics` (clamp to `[0, values().length-1]`), `int seeTarget = 0` stored as `DirectLOS` ordinal of `EnumSeeTarget` (clamp likewise; a **boolean** `DirectLOS` maps `true`→NORMAL(0), `false`→NONE(6));
  `List<int[]> movingPath` (`MovingPathNew`: list of `{Array:int[]}`), `@Nullable BlockPos startPos` (`StartPosNew`: int[3]; saved only when non-null; an array whose length is not 3 is ignored).
  `float walkSpeed()` returns `moveSpeed / 20f` (1.12.2 `getSpeed`).

- [ ] **Step 1: Write the failing tests**

```java
@Test void defaultsMatch112() { var a = new DataAI(() -> {}); assertEquals(5, a.getMoveSpeed()); assertEquals(0.25f, a.walkSpeed()); assertTrue(a.canSwim()); assertEquals(20, a.getMaxHurtResistantTime()); }
@Test void roundTripAllKeys() { /* set every field non-default, incl. movingPath [[1,2,3],[4,5,6]] and startPos (7,8,9); round trip; assert each */ }
@Test void legacyBooleanDirectLOS() {
    var t = new CompoundTag(); t.putBoolean("DirectLOS", false);
    var a = new DataAI(() -> {}); a.load(in(t)); assertEquals(EnumSeeTarget.NONE.ordinal(), a.getSeeTarget());
}
@Test void ordinalsAreClamped() {
    var t = new CompoundTag(); t.putInt("TacticalVariant", 99); t.putInt("DirectLOS", 50); t.putInt("MoveSpeed", 40); t.putInt("OnAttack", -2);
    var a = new DataAI(() -> {}); a.load(in(t));
    assertEquals(EnumNpcTactics.values().length - 1, a.getTactic()); assertEquals(EnumSeeTarget.values().length - 1, a.getSeeTarget());
    assertEquals(10, a.getMoveSpeed()); assertEquals(0, a.getOnAttack());
}
@Test void noStartPosIsNotSaved() { assertFalse(save(new DataAI(() -> {})::save).contains("StartPosNew")); }
```

- [ ] **Step 2: Run, confirm compile failure**
- [ ] **Step 3: Implement**
- [ ] **Step 4: `./gradlew test` passes**
- [ ] **Step 5: Commit** — `"Port DataAI with all 1.12.2 keys (behaviour in 2b-2/2c)"`

---

### Task 4: Factions (registry, player points, hostility)

**Files:**
- Create: `src/main/java/noppes/npcs/faction/Faction.java`
- Create: `src/main/java/noppes/npcs/faction/FactionRegistry.java` (a `SavedData`)
- Create: `src/main/java/noppes/npcs/faction/PlayerFactionPoints.java` (attachment value)
- Create: `src/main/java/noppes/npcs/registry/NpcAttachments.java`
- Modify: `src/main/java/noppes/npcs/CustomNpcs.java` (`NpcAttachments.init()` before `NpcRegistries.register`)
- Modify: `src/main/resources/assets/customnpcs/lang/en_us.json` (`faction.name.friendly` "Friendly", `faction.name.neutral` "Neutral", `faction.name.aggressive` "Aggressive", from the 1.12.2 lang file)
- Test: `src/test/java/noppes/npcs/faction/FactionTest.java`; GameTest `faction_registry_has_defaults_and_persists`, `player_points_default_and_attach` in `NpcGameTests`.

**Interfaces:**
- Produces:
  - `Faction`: fields and keys from 1.12.2 `controllers/data/Faction.java`: `int id=-1 Slot`, `String name="" Name`, `int color=0xFFFFFF Color`, `int neutralPoints=500 NeutralPoints`, `int friendlyPoints=1500 FriendlyPoints`, `int defaultPoints=1000 DefaultPoints`, `boolean hideFaction HideFaction`, `boolean getsAttacked GetsAttacked`, `Set<Integer> attackFactions AttackFactions`, `Set<Integer> friendFactions FrendFactions` (both lists of `{Integer:int}`), `CompoundTag factionPoints FactionPoints` (round-trip only), `String flag Flag`, `String description Description` (JSON text, round-trip only). Constructor `Faction(int id, String name, int color, int defaultPoints)`. `void save(ValueOutput)`, `void load(ValueInput)`.
    Logic on points, testable without a player: `boolean isAggressiveAt(int points)` (`< neutralPoints`), `boolean isFriendlyAt(int points)` (`>= friendlyPoints`), `boolean isNeutralAt(int points)`, `boolean isAggressiveToNpc(int npcFactionId, Set<Integer> npcAttackFactions)` (`attackFactions.contains(npcFactionId) || npcAttackFactions.contains(id)`).
  - `FactionRegistry extends SavedData`, `static final SavedDataType<FactionRegistry> TYPE` with id `"customnpcs_factions"`, codec over `{lastID:int, NPCFactions:[faction…]}` (1.12.2 `factions.dat` shape). `static FactionRegistry get(MinecraftServer)` (overworld `getDataStorage().computeIfAbsent(TYPE)`). On creation with no factions it adds the 1.12.2 defaults: `0 "faction.name.friendly" 0x00DD00 2000` with friendFactions `{1}`; `1 "faction.name.neutral" 0xF2DD00 1000` flag `customnpcs:textures/cloak/baconcape.png`; `2 "faction.name.aggressive" 0xDD0000 0` attackFactions `{0,1}`, flag `customnpcs:textures/cloak/enderdragoncape.png`. Methods: `@Nullable Faction get(int id)`, `Faction getOrFirst(int id)` (falls back to the lowest id), `Collection<Faction> all()`.
  - `PlayerFactionPoints`: `Map<Integer,Integer>`, codec `FactionData: [{Faction:int, Points:int}]`. `int get(Faction f)` returns stored points or, if absent, stores and returns `f.defaultPoints`; `void set(int factionId, int points)` (clamp `>= 0`).
  - `NpcAttachments.FACTION_POINTS : Supplier<AttachmentType<PlayerFactionPoints>>`, serialized, `copyOnDeath()`.
  - `final class FactionChecks { static boolean isAggressiveToPlayer(Faction f, Player p); static boolean isFriendlyToPlayer(Faction f, Player p); static boolean isNeutralToPlayer(Faction f, Player p); }`: creative players are never aggressed (1.12.2 `isAggressiveToPlayer`); points come from the attachment.

- [ ] **Step 1: Write the failing JUnit tests**

```java
@Test void thresholdsMatch112() { var f = new Faction(5, "x", 0, 1000); assertTrue(f.isAggressiveAt(499)); assertTrue(f.isNeutralAt(500)); assertTrue(f.isFriendlyAt(1500)); assertFalse(f.isNeutralAt(1500)); }
@Test void npcHostilityUsesBothSides() { var agg = new Faction(2, "a", 0, 0); agg.attackFactions.add(0); assertTrue(agg.isAggressiveToNpc(0, Set.of())); assertTrue(new Faction(1,"n",0,0).isAggressiveToNpc(3, Set.of(1))); assertFalse(new Faction(1,"n",0,0).isAggressiveToNpc(3, Set.of())); }
@Test void factionRoundTrip() { /* all fields non-default; round trip; assert each incl. both sets */ }
@Test void playerPointsDefaultToFactionDefault() { var p = new PlayerFactionPoints(); var f = new Faction(1, "n", 0, 1000); assertEquals(1000, p.get(f)); p.set(1, -5); assertEquals(0, p.get(f)); }
```

- [ ] **Step 2: Write the failing GameTests**

```java
// faction_registry_has_defaults_and_persists
var reg = FactionRegistry.get(h.getLevel().getServer());
h.assertTrue(reg.get(2).isAggressiveToNpc(0, Set.of()), "aggressive hates friendly");
h.assertValueEqual(0, reg.getOrFirst(99).id, "fallback to first"); h.succeed();
// player_points_default_and_attach
var p = h.makeMockPlayer(GameType.SURVIVAL); var reg = FactionRegistry.get(h.getLevel().getServer());
h.assertTrue(FactionChecks.isAggressiveToPlayer(reg.get(2), p), "0 points < 500");
h.assertTrue(FactionChecks.isFriendlyToPlayer(reg.get(0), p), "2000 >= 1500");
h.assertFalse(FactionChecks.isAggressiveToPlayer(reg.get(2), h.makeMockPlayer(GameType.CREATIVE)), "creative exempt"); h.succeed();
```

- [ ] **Step 3: Run `./gradlew test runGameTestServer`, confirm compile failure**
- [ ] **Step 4: Implement**
- [ ] **Step 5: `./gradlew build runGameTestServer` passes** (all GameTests)
- [ ] **Step 6: Commit** — `"Add minimal faction registry and player faction points"`

---

### Task 5: `DropSet`, `DataInventory`, `DataAdvanced` (faction subset)

**Files:**
- Create: `src/main/java/noppes/npcs/entity/data/DropSet.java`
- Create: `src/main/java/noppes/npcs/entity/data/DataInventory.java`
- Create: `src/main/java/noppes/npcs/entity/data/DataAdvanced.java`
- Test: GameTests in `NpcGameTests` (items need the server's registries): `inventory_round_trip`, `legacy_drop_shapes_load`, `bad_item_entries_are_skipped`; JUnit `DataAdvancedTest`.

**Interfaces:**
- Produces:
  - `DropSet`: `ItemStack item=EMPTY Item`, `double chance=100.0 Chance [0.0001,100]` (rounded to 4 decimals), `float damage=1.0 DamageToItem [0,1]`, `int lootMode=0 LootMode [0,2]` (a boolean tag: `true`→1, `false`→0), `boolean tiedToLevel TiedToLevel`, `int[] amount={1,1} Amount` (int array, or legacy int list; padded to 2 as 1.12.2 does), `Slot` written from its position. Round-trip-only raw tags: `Availability` (compound), `EnchantSettings`, `AttributeSettings`, `TagSettings` (lists of compounds). `void save(ValueOutput)`, `void load(ValueInput)`.
  - `DataInventory(Runnable onChange)`: `Map<Integer, ItemStack> weapons` (`Weapons`; slot 0 main hand, 1 projectile, 2 off hand), `Map<Integer, ItemStack> armor` (`Armor`; 0 head, 1 chest, 2 legs, 3 feet, i.e. 1.12.2 `3 - slot.getIndex()`), `List<DropSet> drops` (`NpcInv`), `boolean lootMode=true LootMode`, `int minExp=0 MinExp`, `int maxExp=0 MaxExp`, `String saveDropsName="" SaveDropsName`, `int dropType=0 DropType` (1.12.2 normalisation: negative → absolute, `> 2` → `% 3`), `int limitation=0 Limitation`. Item lists are `[{Slot:byte, <ItemStack map codec>}]`; empty stacks are not written. `ItemStack getWeapon(int slot)`, `void setWeapon(int slot, ItemStack)`, `ItemStack getArmor(int slot)`, `void setArmor(int slot, ItemStack)`, each firing `onChange`.
  - `DataAdvanced(Runnable onChange)`, faction subset of 1.12.2 `DataAdvanced.java`: `int factionId=0 FactionID`, `boolean attackOtherFactions AttackOtherFactions`, `boolean defendFaction DefendFaction`, `Set<Integer> attackFactions AttackFactions`, `Set<Integer> friendFactions FrendFactions`, `CompoundTag factionPoints FactionPoints` (round-trip only). Other `DataAdvanced` keys arrive with their features.

- [ ] **Step 1: Write the failing tests**

```java
// JUnit DataAdvancedTest.roundTrip: set every field, round trip, assert; defaults factionId 0.
// GameTest inventory_round_trip
var inv = new DataInventory(() -> {});
inv.setWeapon(0, new ItemStack(Items.IRON_SWORD)); inv.setWeapon(1, new ItemStack(Items.ARROW, 16)); inv.setArmor(0, new ItemStack(Items.IRON_HELMET));
var ds = new DropSet(); ds.item = new ItemStack(Items.EMERALD, 3); ds.chance = 50; ds.lootMode = 2; inv.drops.add(ds);
var copy = new DataInventory(() -> {}); copy.load(roundTrip(h, inv::save));   // helper: TagValueOutput.createWithContext(…, registryAccess) → TagValueInput
h.assertValueEqual(Items.IRON_SWORD, copy.getWeapon(0).getItem(), "main hand"); h.assertValueEqual(16, copy.getWeapon(1).getCount(), "projectile count");
h.assertValueEqual(Items.IRON_HELMET, copy.getArmor(0).getItem(), "head"); h.assertValueEqual(2, copy.drops.get(0).lootMode, "loot mode"); h.succeed();
// GameTest legacy_drop_shapes_load: NpcInv entry with LootMode:1b (boolean) and Amount as an int list [2,4] → lootMode 1, amount {2,4}; DropType:-5 → 2.
// GameTest bad_item_entries_are_skipped: Weapons = [{Slot:0b,id:"minecraft:nope",count:1}, {id:"minecraft:stick",count:1}, {Slot:2b,id:"minecraft:shield",count:1}] → only slot 2 present.
```

- [ ] **Step 2: Run, confirm compile failure**
- [ ] **Step 3: Implement**
- [ ] **Step 4: `./gradlew build runGameTestServer` passes**
- [ ] **Step 5: Commit** — `"Port DataInventory, DropSet and DataAdvanced faction fields"`

---

### Task 6: Entity integration, equipment, docs

**Files:**
- Modify: `src/main/java/noppes/npcs/entity/EntityCustomNpc.java`
- Modify: `src/main/java/noppes/npcs/gametest/NpcGameTests.java`
- Modify: `docs/PORTING_PLAN.md` (2b-1 done; what 2b-2…2b-4 still owe)

**Interfaces:**
- Consumes: all data classes (Tasks 1–5), `FactionRegistry`, `NpcAttachments` (Task 4).
- Produces:
  - `EntityCustomNpc` gains `public final DataAI ai; public final DataInventory inventory; public final DataAdvanced advanced;` (field initialisers, like `display`/`stats`). Save/load order: `display, stats, ai, inventory, advanced`, then re-apply everything.
  - Applying (server only, like 2a): `ATTACK_DAMAGE` base = `stats.melee.getStrength()`; `MOVEMENT_SPEED` base = `ai.walkSpeed()`; `STEP_HEIGHT` base = `ai.getStepHeight()`; equipment: main hand ← weapon 0, off hand ← weapon 2, head/chest/legs/feet ← armor 0/1/2/3, via `super.setItemSlot`; all six `DropChances` set to 0 (gear is not dropped by vanilla; NPC drops are `DataInventory`'s job in 2b-3); synced `DATA_FACTION` (Integer, default 0) ← `advanced.getFactionId()`.
  - `setItemSlot(EquipmentSlot, ItemStack)` override: `super`, then (server, fields non-null) writes the stack into `inventory` at the mapped slot, so vanilla equipment changes survive reload (same idea as 2a's `setCustomName`; the change check prevents recursion).
  - `Faction getFaction()` (server): `FactionRegistry.get(server).getOrFirst(advanced.getFactionId())`. The saved `FactionID` is never rewritten by the fallback.
  - `ai.startPos`: set to `blockPosition()` when the NPC is first added to a level with no saved start (1.12.2 `startPos()` lazily uses the current position).

- [ ] **Step 1: Write the failing GameTests**

```java
// full_data_round_trip: set a value in each data class on a spawned NPC (stats.melee.setStrength(9), ai.setMoveSpeed(8), advanced.setFactionId(2), inventory weapon 0 = iron sword); reload; assert all, plus copy.getAttributeBaseValue(ATTACK_DAMAGE) == 9, MOVEMENT_SPEED == 0.4, copy.getMainHandItem().is(Items.IRON_SWORD).
// summoned_weapons_are_held: load from tag {Weapons:[{Slot:0b,id:"minecraft:bow",count:1}],Armor:[{Slot:0b,id:"minecraft:golden_helmet",count:1}]} → getMainHandItem bow, getItemBySlot(HEAD) golden helmet.
// vanillaEquipChangeSurvivesReload: npc.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD)); reload → copy.inventory.getWeapon(2) is shield and copy.getOffhandItem() is shield.
// unknownFactionFallsBackToFirst: load {FactionID:99} → getFaction().id == 0; save again → tag FactionID still 99.
// default_npc_is_harmless: freshly spawned NPC → getFaction().id == 0, getTarget() == null after 20 ticks with a survival mock player beside it (h.runAfterDelay(20, …)).
```

- [ ] **Step 2: Run, confirm compile/assert failures**

Run: `./gradlew runGameTestServer` — Expected: compilation failure (`ai`/`inventory`/`advanced` missing).

- [ ] **Step 3: Implement the entity changes and the PORTING_PLAN update**

- [ ] **Step 4: Run everything**

Run: `./gradlew build test runGameTestServer` — Expected: `BUILD SUCCESSFUL`, all JUnit and GameTests pass, dedicated server log has no `ClassNotFoundException`.

- [ ] **Step 5: Manual client check** (human partner)

`./gradlew runClient`, creative world:
`/summon customnpcs:customnpc ~2 ~ ~ {Weapons:[{Slot:0b,id:"minecraft:diamond_sword",count:1},{Slot:2b,id:"minecraft:shield",count:1}],Armor:[{Slot:0b,id:"minecraft:iron_helmet",count:1},{Slot:1b,id:"minecraft:iron_chestplate",count:1}]}`
Expected: the NPC holds the sword (right hand) and shield (left), wears the helmet and chestplate. Kill it with `/kill @e[type=customnpcs:customnpc]`: it drops none of that gear. Summon another, reload the world: gear unchanged.

- [ ] **Step 6: Commit** — `"Wire 2b-1 data into EntityCustomNpc: equipment, attributes, faction"`
