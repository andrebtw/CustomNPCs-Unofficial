# Stage 2b: NPC combat core, design

Date: 2026-11-08. Follows stage 2a (`docs/superpowers/plans/2026-11-08-stage-2a-npc-entity.md`).
Spec it belongs to: `docs/PORTING_PLAN.md`, Stage 2 ("an NPC ... fights, and follows AI settings
set by command or NBT").

## Intent

The human partner wants the port to behave the same as the 1.12.2 mod it comes from. Stage 2b is
the slice that makes NPCs fight, take damage, die and come back the way 1.12.2 NPCs do. After it,
an NPC configured through NBT (`/summon`, `/data`) picks targets, attacks in melee or at range
with its weapon, uses its 1.12.2 tactic, drops its loot, lies dead or vanishes, and respawns on
its schedule.

**Source of truth:** the 1.12.2 code at `/home/andre/Documents/CustomNPCs-Unofficial` (master,
read-only). Behaviour, NBT key names, defaults and value ranges come from there. The `1.20.1`
branch may only be read to see how a concept maps to Mojang names. Only code from that repo
(CC BY-NC 3.0) and the NeoForge MDK may be copied. No decompiled jars.

## Order agreed with the human partner

2b combat core (this document) → 2c movement AI → 2d `/noppes` command skeleton → remaining
items/blocks → server-side controllers (full factions, dialogs, quests, transport, bank, mail).

## Goals

1. **Data classes, ported with their 1.12.2 NBT keys** (flat at the entity's top level, like 2a):
   - `DataStats`: the remaining keys `SpawnCycle`, `CreatureType`, `PotionImmune`,
     `IgnoreCobweb`, `ChanceBlockDamage`, `Resistances`, `NPCLevel`, `NPCRarity`, `RarityTitle`,
     `CalmdownRange`, with the `DataMelee` and `DataRanged` keys stored in the same compound as
     1.12.2 does (`DataStats.save` calls `ranged.writeToNBT(compound)`).
   - `DataMelee` (all its keys, including the misspelt `AttackStrenght`).
   - `DataRanged` (all its keys: burst, fire rate, delays, accuracy, projectile settings
     `pSpeed`/`pDamage`/…, sounds, `FireIndirect`, `DistanceToMelee`, `AimWhileShooting`).
   - `Resistances` (the per-damage-type list).
   - `DataInventory`: `Weapons`, `Armor`, `NpcInv` (drops), `LootMode`, `MinExp`, `MaxExp`,
     `DropType`, `Limitation`, `SaveDropsName`. `AWModItems` (Armourer's Workshop) is dropped.
   - `DataAI`: **all** keys load and save in 2b, because the combat keys (`OnAttack`,
     `TacticalVariant`, `TacticalRadius`, `AttackInvisible`, `DirectLOS`, `CanSprint`,
     `CanLeap`, `MaxHurtResistantTime`, `AIDisabled`) live in the same class as movement.
     Only combat-related keys get behaviour in 2b. Movement keys round-trip until 2c.
   - `DataAdvanced`, only the fields 2b reads: `FactionID` and `attackOtherFactions` (and
     the faction kill-point options, which round-trip only). Keys as in 1.12.2.
2. **Equipment:** the NPC holds its weapons (main hand, off hand, projectile item) and wears
   its armour, visible on the client through the vanilla equipment slots so the existing
   `HumanoidMobRenderer` layers draw them. A manual client check confirms it.
3. **Targeting:** the 1.12.2 target goals (`ai/target/**`, `ai/selector/NPCAttackSelector`):
   aggro range, retaliation per `OnAttack` (fight back, panic, retreat, nothing), attacking
   other factions per `DataAdvanced.attackOtherFactions` (port only the fields 2b reads),
   ignoring creative/spectator players, `AttackInvisible`, line-of-sight rules
   (`DirectLOS`, `EnumSeeTarget`), `CalmdownRange`.
4. **Attacking:** `ai/CombatHandler`, melee per `DataMelee` (strength, speed, range,
   knockback, potion effect), ranged per `DataRanged`, every tactic in `ai/attack/**`
   (`EnumNpcTactics`: rush/no tactic, dodge, surround, hit-and-run, ambush/stalk, pounce,
   orbit/onslaught, avoid, commander target, custom), and switching from ranged to melee at
   `DistanceToMelee`.
5. **Projectiles:** `EntityProjectile` as its own entity type (`customnpcs:projectile`),
   carrying the item it renders and the `DataRanged` projectile settings (speed, damage,
   impact, size, area, trail, gravity, acceleration, glow, spin, stick, potion effect,
   sounds). Rendered as the flying item (a manual client check confirms 3D vs flat and spin).
6. **Damage and death:**
   - Resistances applied per damage type, `PotionImmune`, `IgnoreCobweb`, `ChanceBlockDamage`
     (what it means in 1.12.2 decides what it does here).
   - Health regen out of combat and combat regen in combat, at the 1.12.2 interval
     (`EntityNPCInterface` onLivingUpdate, every 20 ticks).
   - `BurnInSun` (and the matching 1.12.2 conditions).
   - The 1.12.2 killed state: on death the NPC is not removed; it lies down (unless
     `HideBodyWhenKilled`, then invisible and non-colliding), drops loot per
     `DataInventory` and each drop's `LootMode` (0 on the ground, 1 at the killer's feet,
     2 into the corpse's loot inventory: shared or per attacker, as 1.12.2 `deadLoot`/
     `deadLoots`; players open it by right-clicking the killed NPC), gives XP between `MinExp` and `MaxExp`, and respawns after
     `RespawnTime` per `SpawnCycle` (0 always, 1 day, 2 night, 3 never, 4 natural/despawn),
     reset to its spawn position and full health. `reset()` restores what 1.12.2's `reset()`
     restores, for the parts that exist after 2b.
   - Void damage on a killed NPC resets it (1.12.2 `attackEntityFrom`).
7. **Factions, minimal:** a server-side faction registry saved as `SavedData`, holding the
   1.12.2 default factions (0 friendly "faction.name.friendly" 0x00DD00 2000, 1 neutral
   0xF2DD00 1000, 2 aggressive 0xDD0000 0, with their 1.12.2 hostility/default-points
   settings) and any loaded ones. Per-player faction points stored as a data attachment on the
   player, defaulting to each faction's default points. Enough to answer
   `isAggressiveToPlayer`, `isAggressiveToNpc` and the friendly/neutral thresholds exactly as
   1.12.2 does. New NPCs get the 1.12.2 default faction.
8. **Tests:** JUnit for every data class (round trip, legacy keys, missing keys keep values,
   wrong types fall back, clamping) and for faction logic. GameTests on the dedicated server
   for targeting, melee damage, ranged firing, death/drops/XP, respawn timing, regen, sun
   burning and retaliation. Manual client checks only for visuals (equipment, projectile
   rendering, the killed pose).

## Non-goals (later stages)

- Movement behaviour: wander, paths, return-to-start, doors, swimming, shade, standing
  orientation, mounts (2c). Their keys round-trip only.
- Roles and jobs (guard, follower, companion, bodyguard…), even where 1.12.2 combat code
  checks them. Leave a clear seam (e.g. a no-op hook), not a half port.
- Scripting events (`NpcEvent`, `api/**`), the `INPC*` API interfaces (stage 5).
- Animations (`DataAnimation`, `CustomAnimationHandler`), emotions, model parts (stage 4).
- Faction GUI, faction commands, quest/dialog effects on faction points, faction JSON import
  of the 1.12.2 world folder (controllers stage).
- NPC edit GUI and networking (stages 3/4).
- `DropSet` extras that depend on other systems (quest-linked drops, scripted drops) and
  template drops (`DropType` 1/2 via `DropController`). Their keys round-trip if present.
- The custom look of the corpse loot screen (stage 4); 2b uses the vanilla chest screen.

## Anti-goals (would be a failure even if it works)

- A default NPC that behaves differently from a default 1.12.2 NPC: e.g. attacks players,
  wanders, or never respawns. A freshly placed NPC must still stand still and be harmless.
- Renamed or restructured NBT keys, or keys nested where 1.12.2 had them flat.
- Client-only classes reachable from common code (the dedicated server must start clean).
- Mixins or access transformers added without a ledgered reason that no event/hook exists.

## Constraints

- NeoForge 21.11.48, Minecraft 1.21.11, Java 21, Parchment mappings, ModDevGradle. Check 1.21.11
  APIs in `build/moddev/artifacts/neoforge-21.11.48-sources.jar`, never from memory.
- `Identifier` (not `ResourceLocation`), `ValueInput`/`ValueOutput` persistence, registry-based
  GameTests, `player.permissions()`.
- Same data-class pattern as 2a: entity-independent POJOs with an `onChange` callback,
  `save(ValueOutput)`/`load(ValueInput)`, `getXOr(key, current)` on load so missing keys keep
  their value.
- Commit locally after each task, never push until the human partner says so.

## Decisions made by the builder of this design (not by the human partner)

- **Item format:** weapons, armour and drops are stored in the 1.21.11 `ItemStack` format under
  the 1.12.2 key names. Converting 1.12.2 item NBT (`id`/`Count`/`Damage`/`tag`, pre-flattening
  ids) is left to the clone/world importer in the controllers stage, because it needs the same
  flattening table for every subsystem.
- **DataAI is ported whole in 2b** (data only for movement keys), so its NBT is complete once
  and 2c only adds behaviour.
- **2b is split into four slices, each with its own plan and exit check,** run in this order
  before 2c:
  1. **2b-1 data and factions:** all data classes (goal 1), equipment (goal 2), minimal
     factions (goal 7). Exit: everything round-trips; NPCs wear and hold their gear.
  2. **2b-2 targeting and melee:** goal 3, goal 4 for melee and every tactic. Exit: an
     aggressive-faction NPC hunts and hits a survival player; retaliation works.
  3. **2b-3 damage, death and respawn:** goal 6 (including corpse loot). Exit: kill, loot,
     XP, respawn on schedule, regen and sun burning.
  4. **2b-4 ranged and projectiles:** ranged half of goal 4, goal 5. Exit: a ranged NPC
     shoots per its `DataRanged` settings and switches to melee at `DistanceToMelee`.
  Goal 8's tests are written inside each slice.
- **Corpse loot (`LootMode` 2) uses the vanilla chest menu** (`ChestMenu` through
  `player.openMenu`), sized to the loot, opened by right-clicking a killed NPC. This keeps the
  1.12.2 behaviour without stage 3 networking; stage 4 swaps in the 1.12.2-style screen.
- **Faction storage** uses `SavedData` and a player data attachment now, so the controllers stage
  extends them instead of replacing them.

## Left to the planner/builder

- Task breakdown and order inside 2b (data classes first is the natural order).
- How each 1.12.2 `EntityAIBase` maps onto a 1.21 `Goal` (flags, mutex bits from `AiMutex`).
- How the killed pose is rendered (a pose/synced flag read by the renderer).
- Exact 1.12.2 semantics of fields whose meaning is only clear from reading the code
  (`ChanceBlockDamage`, `CalmdownRange`, `SpawnCycle` 4); the 1.12.2 code decides.
