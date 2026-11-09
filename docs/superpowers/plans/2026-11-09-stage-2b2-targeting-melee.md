# Stage 2b-2: NPC Targeting and Melee Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** An NPC picks targets the way the 1.12.2 NPC does (faction hostility, `OnAttack`, `AttackInvisible`, `DirectLOS`, retaliation, faction defence), chases and hits them in melee per `DataMelee`, and moves according to its `EnumNpcTactics` tactic. A default NPC stays harmless.

**Architecture:** Behaviour lives in `noppes.npcs.ai.*` (goals, `CombatHandler`, selector) and in `EntityCustomNpc` hooks (`setTarget`, `hurtServer`, `doHurtTarget`, `aiStep`). The 1.12.2 `updateTasks()` rebuild becomes `EntityCustomNpc.rebuildGoals()`, called when `DataAI` / `DataStats.aggroRange` / `DataAdvanced` change. Pure maths (threat value, geometry) sits in small static helpers with JUnit tests; behaviour is proved by GameTests on the dedicated server. Ranged attacks are a seam only (`AbstractTacticGoal.isRanged()` returns false until 2b-4).

**Tech Stack:** NeoForge 21.11.48, Minecraft 1.21.11 (Parchment), ModDevGradle 2.0.148, Java 21, JUnit 5, registry-based GameTests.

**Spec:** `docs/superpowers/specs/2026-11-08-stage-2b-combat-design.md` (slice 2b-2: goal 3, and goal 4 for melee and every tactic; goal 8 tests for them).

## Global Constraints

- Source of truth: `/home/andre/Documents/CustomNPCs-Unofficial` master (read-only). Paths below are under `src/main/java/noppes/npcs/` there. Where this plan and that code disagree, the code wins; ledger the deviation and put it in the commit message as a Ruling.
- Only code from that repo (CC BY-NC 3.0) and the NeoForge MDK may be copied. No decompiled jars. Check every 1.21.11 API in `build/moddev/artifacts/neoforge-21.11.48-sources.jar`, extracted into the scratchpad, never into the repo.
- NBT keys are already ported (2b-1). This slice adds exactly one key: `ThroughWalls` (Task 1). No renamed keys.
- A freshly placed default NPC (faction 0, `OnAttack` 0, default points) must not attack a survival player and must not wander.
- Client-only classes only under `noppes.npcs.client`. No mixins, no access transformers.
- `AiMutex` mapping onto 1.21 `Goal.Flag` (Ruling, no 1.21 equivalent of `PASSIVE`): `PATHING`→`MOVE`, `LOOK`→`LOOK`, target goals (mutex 1)→`TARGET`; `PASSIVE` goals: Sprint has no flags, Panic uses `MOVE` (it never coexists with an attack goal, since `OnAttack` picks one).
- Goals that port a 1.12.2 `tickRate` gate use `TICK_RATE = 3` (1.12.2 `EntityAITasks` default) and return `true` from `requiresUpdateEveryTick()`.
- Damage source for NPC melee is vanilla `damageSources().mobAttack(this)` until 2b-3 decides on a `customnpcs:npc` damage type (needed for resistances). Ledger it.
- Roles/jobs/owner/follower are out of scope: where 1.12.2 asks `getOwner()`, `isFollower()`, `JobGuard`, use a seam `EntityCustomNpc.getOwner()` returning `null` (documented), never a half port. Scripts, events (`NpcEvent`), animations, speech lines (`getAttackLine`) are skipped.
- Each task ends with `set -o pipefail; ./gradlew build test` (and `./gradlew runGameTestServer` where the task adds GameTests) passing, a ledger entry in `.superpowers/sdd/2026-11-09-stage-2b2-targeting-melee/` (git-ignored), and a local commit on `neoforge-1.21.11`. **Do not push.**

## Review Focus

1. **Creative/spectator players and invisible players:** never targeted, and an existing target that switches to creative is dropped. → Task 3 `creativeAndSpectatorNeverTargeted`, `targetSwitchingToCreativeIsDropped`.
2. **Default NPC and friendly factions:** a default NPC never targets a survival player at default points; attacking it from a friendly player does nothing (damage refused, no target). → Task 3 `defaultNpcIgnoresSurvivalPlayer`, Task 4 `friendlyPlayerDamageRefused`.
3. **Zero or negative damage and `OnAttack` 3:** 0-damage hits (snowball, `/damage 0`) do not make the NPC retaliate; `OnAttack:3` never retaliates; `AIDisabled:1b` never targets or attacks. → Task 4 `zeroDamageDoesNotProvoke`, `onAttackNothingNeverRetaliates`, Task 2 `aiDisabledNeverTargets`.
4. **Target lost:** target dies, leaves aggro range, or changes dimension: target cleared, `Attacking` false, goals idle, no exception. → Task 2 `targetOutOfRangeIsCleared`, `targetDeathClearsAttacking`.
5. **Hit-and-run target juggling:** HitAndRun sets the target to null and restores it; it must not loop, leak the target after the NPC dies, or let `canNewAttack()` stay false forever. → Task 7 `hitAndRunRestoresTarget`.

---

### Task 1: Pre-work (deferred 2b-1 minors this slice builds on) and `ThroughWalls`

Decisions taken (Rulings): fix the three minors that 2b-2 reads; leave the rest on the deferred list in `docs/PORTING_PLAN.md`.

**Files:**
- Modify: `src/main/java/noppes/npcs/entity/EntityCustomNpc.java`, `entity/data/DataAdvanced.java`, `entity/data/DataMelee.java`
- Test: `src/test/java/noppes/npcs/entity/data/DataAdvancedTest.java`, `src/test/java/noppes/npcs/entity/data/CombatDataTest.java`, `gametest/NpcGameTests.java`

**Interfaces:**
- Produces:
  - `DataAdvanced.isThroughWalls(): boolean` (default `true`), `setThroughWalls(boolean)`, key `ThroughWalls` (absent = `true`, as 1.12.2).
  - `DataMelee.getDelayRNG(RandomSource rng): int` — 1.12.2 `DataMelee.getDelayRNG`: `delay = attackSpeed; if (attackSpeed < 120 && attackSpeed > 10) delay += rng.nextInt((int)(attackSpeed * 0.15))`.
  - `EntityCustomNpc.getFaction()` becomes `@Nullable Faction` and returns `null` on the client instead of throwing (the registry is server-only); server callers are unchanged.
  - `DATA_FACTION` now syncs the effective faction id (`getFaction().id`), recomputed on `applyAdvanced` and when the registry changes is not needed (ledger).
  - `EntityCustomNpc.setGuaranteedDrop(EquipmentSlot)` overridden as a no-op (1.21.11 `Mob.setGuaranteedDrop`) so item pickup cannot override drop chance 0.
  - `EntityCustomNpc.getOwner(): @Nullable LivingEntity` returning `null` (role/follower seam).

- [ ] **Step 1: Write failing tests:** `throughWallsDefaultsTrueAndRoundTrips` (default true, saved key `ThroughWalls`, absent key keeps current value), `delayRngStaysInRange` (attackSpeed 20 → `[20,22]` over 200 seeds; attackSpeed 10 → exactly 10; 120 → exactly 120), GameTest `unknown_faction_syncs_effective_id` (`FactionID:99` → `getSyncedFaction()==0`, saved `FactionID` still 99).
- [ ] **Step 2: Run to see them fail:** `./gradlew test` → FAIL (methods missing).
- [ ] **Step 3: Implement** the Interfaces above.
- [ ] **Step 4: Run** `set -o pipefail; ./gradlew build test runGameTestServer` → all pass.
- [ ] **Step 5: Commit** `Add ThroughWalls and delay RNG; sync effective faction id; pin drop chances`.

---

### Task 2: Target state, `CombatHandler` and goal rebuild

**Files:**
- Create: `ai/CombatHandler.java`, `ai/ThreatMath.java`
- Modify: `entity/EntityCustomNpc.java`
- Test: `src/test/java/noppes/npcs/ai/ThreatMathTest.java`, `gametest/NpcGameTests.java`

**Interfaces:**
- Consumes: `DataAI`, `DataStats.getAggroRange()`, `DataAdvanced` (Task 1).
- Produces:
  - `ThreatMath.threat(double damage, double distance, boolean attackerIsPlayer, boolean attackerHoldsShield, boolean projectileOrMagic, boolean isCurrentTarget): double` — exactly 1.12.2 `CombatHandler.damage`: `damage<=0.25 → 0.25`; `5<dist<32 → ×(0.009259*dist+0.953704)`; player ×1.1; shield ×1.2 else projectile/magic ×1.025; current target ×1.05.
  - `CombatHandler(EntityCustomNpc npc)`, fields `Map<LivingEntity,Double> aggressors`, `Map<LivingEntity,Long> lastDamages` (key = entity, value = game time), `LivingEntity priorityTarget`, `boolean onlyPlayers`; methods `checkTarget(): boolean`, `damage(DamageSource, double)`, `isValidTarget(LivingEntity): boolean` (alive, not creative/spectator player, within `aggroRange` by `isInRange`, same dimension), `reset()`, `start()`, `update()` (once per tick from `aiStep`), `canDamage(DamageSource, float): boolean` (hurt-resistance rule using `DataAI.getMaxHurtResistantTime()`; the "non-living source" branch compares with `lastHurt`, `getLastHurt()` in 1.21). Port `CombatHandler.java` method for method; drop `abilities`.
  - `EntityCustomNpc`: `DATA_ATTACKING` (synced boolean) + `isAttacking()`; `combat: CombatHandler`; `isInRange(Entity, double)` and `isInRange(double,double,double,double)` (1.12.2 box test, not spherical); `isFriend(Entity): boolean` (1.12.2 `isFriend`: same faction id or friend lists, using `Faction.friendFactions` and `DataAdvanced.getFriendFactions()`); `rebuildGoals()` (clears `goalSelector` and `targetSelector`, re-adds, no-op while `ai == null` or on the client; called from the constructor tail, `applyAi`, `applyStats`, `applyAdvanced`); `setTarget(@Nullable LivingEntity)` override with the 1.12.2 `setAttackTarget` rules (reject creative/spectator players, reject owner, keep an existing target while `combat.priorityTarget != null`, ignore while `ai.isAiDisabled()`; null clears target goals' state); `setPriorityTarget(LivingEntity)`; `onAttack(LivingEntity)` (1.12.2 `onAttack`: not AI-disabled, not self, not already attacking, `OnAttack != 3`).
  - `registerGoals()` must tolerate the super-constructor call (null-guard `ai`).

- [ ] **Step 1: Write failing tests.** JUnit `threatMatchesTable` (damage 4, dist 4, plain → 4.0; dist 20 → `4*(0.009259*20+0.953704)`; player → ×1.1; shield → ×1.2 and not also ×1.025; current target → ×1.05; damage 0 → 0.25). GameTests: `aiDisabledNeverTargets` (`AIDisabled:1b`, pig in range set as target via `setTarget` → stays null), `targetOutOfRangeIsCleared` (target 40 blocks away with aggro 16 → after 15 ticks `getTarget()==null`, `isAttacking()==false`), `targetDeathClearsAttacking`.
- [ ] **Step 2: Run to see them fail.** `./gradlew test` → FAIL.
- [ ] **Step 3: Implement** `ThreatMath`, `CombatHandler`, and the `EntityCustomNpc` additions. `combat.update()` runs from `aiStep()` on the server when not killed (killed state is 2b-3).
- [ ] **Step 4: Run** build/test/GameTests → pass.
- [ ] **Step 5: Commit** `Add CombatHandler, threat table, target rules and goal rebuild`.

---

### Task 3: Target selection (selector, closest target, clear target, hurt-by)

**Files:**
- Create: `ai/selector/NpcAttackSelector.java`, `ai/target/NpcClearTargetGoal.java`, `ai/target/NpcClosestTargetGoal.java`, `ai/target/NpcHurtByTargetGoal.java`
- Modify: `faction/FactionChecks.java`, `entity/EntityCustomNpc.java` (`rebuildGoals` adds them)
- Test: `gametest/NpcGameTests.java`, `src/test/java/noppes/npcs/faction/FactionTest.java`

**Interfaces:**
- Consumes: Task 2 (`combat`, `isInRange`, `isFriend`, `getOwner`).
- Produces:
  - `FactionChecks.isAggressiveToNpc(Faction own, Set<Integer> ownAttackFactions, boolean ownAttackOtherFactions, int otherFactionId): boolean` wrapper matching 1.12.2 `Faction.isAggressiveToNpc` gated by `attackOtherFactions` (reuse `Faction.isAggressiveToNpc`).
  - `NpcAttackSelector implements Predicate<LivingEntity>`: `test(entity)` = alive, not self, `isInRange(aggroRange)`, health ≥ 0.1, current tactic goal `canNewAttack()`, `nearHome(entity)`, `wanted(entity)`, and (`DirectLOS` is `NONE` or `npc.hasLineOfSight(entity)`). `wanted`: `ServerPlayer` → `FactionChecks.isAggressiveToPlayer` and not creative and not spectator and (if `AttackInvisible` is false and the player is invisible → false; 1.12.2 `InvisibleNpcs` algorithm option is not ported: ledger); another `EntityCustomNpc` → not killed (2b-3 seam: `isKilled()` returns false for now) and `attackOtherFactions` and the 1.12.2 faction test. `nearHome`: 1.12.2 rule when `returnToStart`, using `DataAI.getStartPos()`, `walkingRange`, `movingType` 0/1 (movingType 2 path uses the first path point if present).
  - `NpcClearTargetGoal` (flags `TARGET`): runs while a target exists and `combat.checkTarget()`; start clears target and the revenge target; stop clears navigation.
  - `NpcClosestTargetGoal(EntityCustomNpc, int chance=4, Predicate)` (flags `TARGET`): every `canUse` roll `nextInt(4)==0`, search `AABB.inflate(max(followRange, aggroRange), 4, same)`, pick nearest, `setTarget` on start; a `Monster` target with no target gets the NPC as target (1.12.2).
  - `NpcHurtByTargetGoal` (flags `TARGET`): vanilla `HurtByTargetGoal` semantics via `getLastHurtByMob()`, no alert-others, respects `onAttack` through `setTarget`/`onAttack`.
  - `rebuildGoals()` target order: Clear(0), HurtBy(1), Closest(2). Owner goals are the seam (not added).
  - Faction-aggro mobs: in `aiStep` once per 20 ticks, if `getFaction().getsAttacked`, nearby `Monster`s with no target and line of sight get the NPC as target (1.12.2 `onLivingUpdate`).

- [ ] **Step 1: Write failing GameTests** (mock players via the existing helpers in `NpcGameTests`): `defaultNpcIgnoresSurvivalPlayer` (default NPC, survival player 5 blocks away, 40 ticks → no target); `aggressiveNpcTargetsSurvivalPlayer` (`FactionID:2`, player 6 blocks away → `getTarget()` is the player within 40 ticks); `creativeAndSpectatorNeverTargeted`; `targetSwitchingToCreativeIsDropped`; `attackOtherFactionsTargetsHostileNpc` (faction 0 vs faction 2 NPC with `attackOtherFactions` true); `directLosNoneIgnoresWalls` (wall between, `DirectLOS:6` targets, `DirectLOS:0` does not); `attackInvisibleFalseIgnoresInvisiblePlayer`; `getsAttackedFactionDrawsMonsters`.
- [ ] **Step 2: Run** `./gradlew runGameTestServer` → FAIL.
- [ ] **Step 3: Implement** the Interfaces above.
- [ ] **Step 4: Run** build/test/GameTests → pass.
- [ ] **Step 5: Commit** `Port NPC target selection goals and attack selector`.

---

### Task 4: Damage intake and retaliation

**Files:**
- Modify: `entity/EntityCustomNpc.java` (`hurtServer`), `entity/data/DataAdvanced.java` (`tryDefendFaction`)
- Test: `gametest/NpcGameTests.java`

**Interfaces:**
- Consumes: Tasks 2–3.
- Produces:
  - `EntityCustomNpc.hurtServer(ServerLevel, DamageSource, float): boolean` following 1.12.2 `attackEntityFrom` for the parts that exist in 2b-2, in this order: tactic goal `damaged()` veto (Dodge); `combat.canDamage`; attacker == owner → refuse; attacker `EntityCustomNpc` of the same faction id → refuse; attacker `Player` with `FactionChecks.isFriendlyToPlayer` → refuse (no hurt, no target); `inWall` damage refused; then for a living non-creative attacker with `damage > 0`: notify NPCs within `AABB.inflate(32,16,32)` via `DataAdvanced.tryDefendFaction`; if attacking, switch target when the attacker is closer than the current target; else `setTarget(attacker)` via `onAttack` rules; finally `super.hurtServer` and, when hurt, `combat.damage(source, amount)`. Skipped (documented): resistances (2b-3), `NpcEvent`, blocking (`ChanceBlockDamage`, 2b-3), void reset (2b-3), animations.
  - `DataAdvanced.tryDefendFaction(EntityCustomNpc self, int attackedFactionId, LivingEntity attacked, LivingEntity attacker)`: 1.12.2 logic — ignore when `!defendFaction` or `attacked == self`; `canSee = self.hasLineOfSight(attacked)`, if not and `throughWalls` then `canSee = distance <= aggroRange`; require `self` faction id == attacked faction id or friend lists contain it; then `self.onAttack(attacker)`.

- [ ] **Step 1: Write failing GameTests:** `friendlyPlayerDamageRefused` (faction 0 NPC, default points: attack by player → health unchanged, no target); `hostilePlayerHitMakesNpcFightBack` (`FactionID:2`, hit by player at range → target is player, health reduced); `zeroDamageDoesNotProvoke`; `onAttackNothingNeverRetaliates` (`OnAttack:3`); `sameFactionNpcDamageRefused`; `defendFactionRecruitsNeighbour` (two faction-0 NPCs, `DefendFaction:1b` on the neighbour, hit one with a hostile mob → neighbour targets the attacker; with `ThroughWalls:0b` and a wall, and attacked beyond sight, it does not).
- [ ] **Step 2: Run** → FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** build/test/GameTests → pass.
- [ ] **Step 5: Commit** `Add NPC damage intake, retaliation and faction defence`.

---

### Task 5: Melee hit and the tactic base class

**Files:**
- Create: `ai/attack/AbstractTacticGoal.java`, `ai/attack/NoTacticGoal.java`, `ai/attack/OnslaughtGoal.java`
- Modify: `entity/EntityCustomNpc.java` (`doHurtTarget`, `rebuildGoals` attack branch, `aiAttackGoal` field)
- Test: `gametest/NpcGameTests.java`

**Interfaces:**
- Consumes: Tasks 1–4 (`getDelayRNG`, `combat`).
- Produces:
  - `EntityCustomNpc.doHurtTarget(ServerLevel, Entity): boolean` per 1.12.2 `tryAttackEntityAsMob` (no animation parts): refused when `AIDisabled` or the target is dead; damage = `stats.melee.getStrength()` through `mobAttack`; `if (melee.getDelay() < 10) target.invulnerableTime = 0` (1.12.2 `hurtResistantTime`); on a hit with `Player` owner nothing (seam); effect `PotionEffect` 0 none, 1 fire for `effectTime` seconds, otherwise the 1.12.2 `PotionEffectType` id mapped to a vanilla `MobEffect` for `effectTime*20` ticks, amplifier `effectStrength`; knockback: after a successful hit apply `knockback(0.2 * melee.getKnockback(), dx, dz)` away from the NPC (1.12.2 does this in a mixin).
  - `AbstractTacticGoal extends Goal` (port of `EntityAICustom`): fields `target`, `distance`, `range`, `tacticalRange`, `inMove`, `canSeeToAttack`, `isFriend`, `hasAttack`, `meleeTick`; `canUse` = `target != null && alive` (also `aiOwner` commander link, Task 8); `updateTask`-equivalent `tick()` sets look-at 30°, `inMove = !navigation.isDone()`, `tacticalRange = ai.getTacticalRadius()`, `distance`, and for melee `meleeTick--`, `range = melee.getRange()` raised to `(npc.width + target.width)/2`; `tryMoveToTarget()` with the 1.12.2 speed curve (`baseSpeed = canSprint ? 1.5 : 1.3`, priority target `1.6/1.4`, `speed = (0.75/aggroRange*dist + 0.5)*baseSpeed` clamped to `[1.3, baseSpeed]`); `tryToCauseDamage()` melee branch only (`meleeTick>0 || distance>range || !canSeeToAttack` waits; else `meleeTick = getDelayRNG`, `swing(MAIN_HAND)`, `doHurtTarget`, `attacked()`, `hasAttack=true`); hooks `canNewAttack(): boolean` (true), `attacked()`, `damaged(): boolean` (false); `isRanged(): boolean` returns `false` (2b-4 seam). Flags `MOVE`.
  - `NoTacticGoal` (tactic NONE) and `OnslaughtGoal` (RUSH; with `AIDisabled` 1.12.2 also registers Onslaught but `doHurtTarget` refuses, keep that): `tick()` after `super`: `if (isFriend || npc.tickCount % (TICK_RATE*2) != 0) return;` then NoTactic: `canSee=...; tryMoveToTarget(); tryToCauseDamage()`; Onslaught: if `canSee && distance <= range` stop navigation when `inMove`, else `tryMoveToTarget()`, then `tryToCauseDamage()`.
  - `rebuildGoals()` attack branch (`!aiDisabled`): `OnAttack==0` → switch on `DataAI.getTactic()` (RUSH 0 Onslaught, STAGGER 1 and ORBIT 2, HIT_AND_RUN 3, COMMANDER 4, STALK 5 come in Tasks 6–8, NONE 6 NoTactic; until those tasks, ordinals 1–5 map to Onslaught); `AIDisabled` → Onslaught.

- [ ] **Step 1: Write failing GameTests:** `rushNpcWalksToAndHitsPlayer` (`FactionID:2`, `AttackStrenght:3`, player 8 blocks away in survival, within 100 ticks player health dropped by ≥3, at the player's feet distance ≤ `AttackRange`+1); `meleeDelayRespected` (`AttackSpeed:40`: between ticks 20 and 60 at most 2 hits); `meleeEffectApplied` (`PotionEffect:1` fire → player on fire; poison id → effect present); `meleeKnockbackMovesTarget`; `noTacticStillHits` (`TacticalVariant:6`); `aiDisabledNeverHits`; `defaultNpcStaysPut` (default NPC, 60 ticks, position unchanged, player unharmed).
- [ ] **Step 2: Run** → FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** build/test/GameTests → pass.
- [ ] **Step 5: Commit** `Add melee attack, tactic base goal, NoTactic and Onslaught`.

---

### Task 6: Geometry helpers, Dodge (STAGGER), Surround (ORBIT), HitAndRun

**Files:**
- Create: `ai/NpcMath.java`, `ai/attack/DodgeGoal.java`, `ai/attack/SurroundGoal.java`, `ai/attack/HitAndRunGoal.java`
- Modify: `entity/EntityCustomNpc.java` (`rebuildGoals` cases 1, 2, 3; `jumpTowards`)
- Test: `src/test/java/noppes/npcs/ai/NpcMathTest.java`, `gametest/NpcGameTests.java`

**Interfaces:**
- Produces:
  - `NpcMath.yawPitch(Vec3 from, Vec3 to): float[]` and `NpcMath.pointAt(Vec3 origin, float yawDeg, float pitchDeg, double distance): Vec3` — ports of `Util.getAngles3D` / `Util.getPosition`; they must satisfy `pointAt(a, yawPitch(a,b)..., |b-a|) ≈ b` (round-trip test, tolerance 1e-6, plus degenerate case `from==to` returns `[0,0]`, no NaN).
  - `EntityCustomNpc.jumpTowards(double speed, Vec3 pos)` (1.12.2 `Util.jumpTowards`: ballistic launch like `PounceGoal.getAngleForXYZ`, g = 0.1).
  - `DodgeGoal` (STAGGER): `tick` gate `tickCount % (TICK_RATE*2) > TICK_RATE → return`; `if (!(canSee && distance<=range) && !inMove) tryMoveToTarget(); tryToCauseDamage()`; `attacked()` and `damaged()` call `tryJump()`: at most one jump per 1000 ms of **game ticks** (Ruling: 20 ticks, not wall-clock) choosing a random position via `DefaultRandomPos.getPos(npc, tacticalRange, 2)` with the 1.12.2 retry loop (≤8 attempts, ≥3 errors abort, distance must exceed `tacticalRange`, `range` (or `range/2` ranged) and not exceed `aggroRange`, must have a path); `damaged()` returns true (damage refused) with probability 0.05 after a successful jump, as 1.12.2.
  - `SurroundGoal` (ORBIT): `tr = min(tacticalRange, range)`; out of sight or too far → `tryMoveToTarget`; else when `distance <= 0.9*tr || >= 1.1*tr`, path to `pointAt(target, yawPitch(target, npc), tr)` at speed 1.3, falling back to `DefaultRandomPos.getPosAway(npc, 2, 2, npcPos - pointPos)`.
  - `HitAndRunGoal`: after each hit (`hasAttack`) choose a retreat point (behind the target, then random as 1.12.2), move there at 1.3, remember `runPos`, `targetIn`, and `setTarget(null)`; when reached or stalled (`clearTarget()`), restore the target. `canNewAttack()` returns `runPos == null`. Port the 1.12.2 point-in-box condition **as intended** (`|point-runPos| <= 2`; the original `x < a-2 && x > a+2` can never be true: Ruling, ledger it).
  - `rebuildGoals()`: 1 → Dodge, 2 → Surround, 3 → HitAndRun.

- [ ] **Step 1: Write failing tests:** JUnit `NpcMathTest` (round trip, degenerate); GameTests `staggerNpcStillReachesAndHits`, `orbitNpcKeepsTacticalDistance` (`TacticalVariant:2`, `TacticalRadius:6`, `AttackRange:2`: after 100 ticks distance to a stationary player is within `[0.9*6-1, 1.1*6+1]` or hit range), `hitAndRunRestoresTarget` (after a hit the target is null for some ticks then the player again; NPC killed mid-run leaves no goal state; `canNewAttack()` true afterwards).
- [ ] **Step 2: Run** → FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** build/test/GameTests → pass.
- [ ] **Step 5: Commit** `Add Dodge, Surround and HitAndRun tactics`.

---

### Task 7: Stalk (STALK) and Commander (COMMANDER)

**Files:**
- Create: `ai/attack/StalkGoal.java`, `ai/attack/CommanderGoal.java`
- Modify: `ai/attack/AbstractTacticGoal.java` (owner-link in target resolution), `entity/EntityCustomNpc.java` (`aiOwnerNpc` field, `rebuildGoals` cases 4, 5)
- Test: `gametest/NpcGameTests.java`

**Interfaces:**
- Produces:
  - `StalkGoal`: 1.12.2 `EntityAIStalkTarget`. Pose 1 = sneaking: `setShiftKeyDown(true/false)` (Ruling: crouch pose via the shift flag; the poses/animations system is stage 4). Sneak when `distance < tacticalRange`, after attacking, or when the target can see the NPC (`target.hasLineOfSight(npc)`); approach at 0.725 sneaking, 1.3 otherwise; stand up when the goal stops.
  - `CommanderGoal`: 1.12.2 `EntityAICommanderTarget`. Recruits same-faction idle NPCs (no target, `OnAttack` 0 or 2, no commander) within `tacticalRange` that have a path to it; they walk to the commander; when they are within `minDist` (3.0, or `0.4*n+1` when ≥5 followers), after `time` ticks (`tacticalRange<5 ? 18 : (int)(4.90909*r - 6.54545)`), or when the target is close, `attack()` gives all followers the target. Followers carry `aiOwnerNpc`; `AbstractTacticGoal` target resolution adopts the commander's target. `reset()` sends followers home when `returnToStart`. The 1.12.2 `setCurrentAnimation(4)` calls are skipped (animation system, stage 4).
  - `EntityCustomNpc.aiOwnerNpc: @Nullable EntityCustomNpc`.

- [ ] **Step 1: Write failing GameTests:** `stalkNpcSneaksWhenClose` (`TacticalVariant:5`, `TacticalRadius:8`, player 6 blocks away → `isShiftKeyDown()` true within 40 ticks; unsneaks after the target is lost), `commanderRecruitsNeighbourAndBothAttack` (two faction-2 NPCs, commander tactic on one, player 10 away → both end up with the player as target within 200 ticks), `commanderWithoutFriendsAttacksAlone`.
- [ ] **Step 2: Run** → FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** build/test/GameTests → pass.
- [ ] **Step 5: Commit** `Add Stalk and Commander tactics`.

---

### Task 8: `OnAttack` responses (Avoid, Panic), Sprint, Pounce

**Files:**
- Create: `ai/attack/AvoidTargetGoal.java`, `ai/movement/NpcPanicGoal.java`, `ai/movement/SprintToTargetGoal.java`, `ai/attack/PounceTargetGoal.java`
- Modify: `entity/EntityCustomNpc.java` (`rebuildGoals`)
- Test: `gametest/NpcGameTests.java`

**Interfaces:**
- Produces: ports of `EntityAIAvoidTarget` (`OnAttack 2`: `range = aggroRange/2`, `tacticalRange = (int) range`, minimum 4; flees along the away-vector, path to the raycast end or `DefaultRandomPos.getPos`, speed 1.3, keeps `runPos`), `EntityAINpcPanic` (`OnAttack 1`: while a target exists or the NPC burns, run to `DefaultRandomPos.getPos(npc, 5, 4)` at 1.2), `EntityAISprintToTarget` (`CanSprint`: sprint while navigating toward a target; `OnAttack 0` only when beyond `aggroRange/3`, `OnAttack 2` when within `aggroRange`, else always), `EntityAIPounceTarget` (`CanLeap`: when on ground, target visible, `4 < dist <= 8` (1.12.2 `isInRange` box test), 1-in-5 chance: launch with `leapSpeed 1.3` using `getAngleForXYZ`, flag `MOVE`, runs while airborne).
  - `rebuildGoals()` order, as 1.12.2 `setResponse()`: Sprint if `canSprint`; `OnAttack 1` Panic; `OnAttack 2` Avoid; `OnAttack 0` Pounce if `canLeap`, then the tactic goal; `OnAttack 3` none. The target goals from Task 3 are unchanged for all values.

- [ ] **Step 1: Write failing GameTests:** `avoidNpcKeepsDistance` (`OnAttack:2`, `FactionID:2`, player closing in → NPC ends farther than it started, never hits), `panicNpcMovesWhenTargeted` (`OnAttack:1` after being hit: position changes, no damage dealt), `sprintSetsSprinting` (`CanSprint:1b` while chasing at distance > aggro/3), `pounceLeapsAtMidRange` (`CanLeap:1b`, player 6 blocks away: vertical velocity > 0 at some tick within 100), `onAttackNothingHasNoAttackGoal` (`OnAttack:3`: no melee goal registered, NPC never moves toward a hostile player).
- [ ] **Step 2: Run** → FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** build/test/GameTests → pass.
- [ ] **Step 5: Commit** `Add Avoid, Panic, Sprint and Pounce responses`.

---

### Task 9: Exit check, docs and review

**Files:**
- Modify: `docs/PORTING_PLAN.md` (mark 2b-2 done, list what 2b-3/2b-4 still owe, carry the still-deferred minors), `.superpowers/sdd/…` ledger.
- Test: `gametest/NpcGameTests.java`

- [ ] **Step 1: Write the exit GameTest** `exit_aggressive_npc_hunts_and_hits_survival_player` (faction 2 NPC, survival mock player 12 blocks away, default `OnAttack`/tactic: NPC targets, reaches, and damages the player; `isAttacking()` true during, false ~60 ticks after the player is removed) and `exit_default_npc_is_harmless` (faction 0 NPC, survival player adjacent for 100 ticks: no damage, no target, position unchanged).
- [ ] **Step 2: Run** `set -o pipefail; ./gradlew build test runGameTestServer` → everything passes; record the counts (expected: 58 JUnit + new, 24 GameTests + new).
- [ ] **Step 3: Update `docs/PORTING_PLAN.md`**; commit `Mark 2b-2 done`.
- [ ] **Step 4: Dispatch one fresh reviewer on the most capable model** over `git diff e7fbf0c..HEAD` with the spec, this plan and the 1.12.2 files named in each task; fix Critical/Important findings, commit.
- [ ] **Step 5: Manual check for the user** (a client is needed to see it): `/summon customnpcs:customnpc ~3 ~ ~ {FactionID:2}` hunts and hits a survival player; same with `{FactionID:2,TacticalVariant:2}` (circles), `{FactionID:2,OnAttack:2}` (backs away), `{}` (stays still), and a creative player is ignored.
