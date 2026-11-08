# Stage 2a: Spawnable NPC Entity Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A `customnpcs:customnpc` entity that can be placed with the NPC Wand or `/summon`. It renders with its skin, name and size, and saves/loads its display and stats data using the 1.12.2 NBT key names.

**Architecture:** Port from the 1.12.2 `master` branch (`/home/andre/Documents/CustomNPCs-Unofficial`). 1.12.2 is the source of truth for behaviour, key names and defaults. The `1.20.1` branch may be read to see how a concept maps to Mojang names, never for behaviour. Data classes (`DataDisplay`, `DataStats`) are entity-independent POJOs that save to `ValueOutput` / load from `ValueInput`, so JUnit can test them. The entity owns them, pushes render-relevant fields into `SynchedEntityData`, and maps stats onto vanilla attributes. This plan is the first slice of stage 2 in `docs/PORTING_PLAN.md`. AI goals, the remaining items/blocks, commands and controllers get their own plans.

**Tech Stack:** NeoForge 21.11.48, Minecraft 1.21.11 (Parchment 2025.12.20), ModDevGradle 2.0.148, Java 21, JUnit 5 (MDG `unitTest`), vanilla GameTest framework (registry-based, 1.21.5+).

**Spec:** `docs/PORTING_PLAN.md`, sections "Stage 2" and "1. Package classification".

## Global Constraints

- Mod id `customnpcs`; base package `noppes.npcs`; entity id `customnpcs:customnpc`; wand id `customnpcs:npcwand`.
- NBT keys are copied verbatim from 1.12.2 (`Name`, `Title`, `Texture`, `ShowName`, `Size`, `MaxHealth`, `AggroRange`, `HealthRegen`, `CombatRegen`, `ImmuneToFire`, `CanDrown`, `BurnInSun`, `NoFallDamage`, `HideBodyWhenKilled`, `RespawnTime`), flat at the top level of the entity's save data.
- A key missing on load keeps the field's current value (default or constructor-assigned). It never resets to empty/zero.
- 1.21.11 renamed `ResourceLocation` → `net.minecraft.resources.Identifier`. Use `Identifier` everywhere.
- Entity persistence uses `addAdditionalSaveData(ValueOutput)` / `readAdditionalSaveData(ValueInput)`. There are no raw `CompoundTag` entity overrides.
- Client-only classes live under `noppes.npcs.client` and are referenced only from `CustomNpcsClient`.
- No mixins and no access transformers in this plan.
- Only code from `CustomNPCs-Unofficial` (CC BY-NC 3.0) and the NeoForge MDK may be copied. No decompiled mod jars.
- Each task ends with `./gradlew build` passing and a commit on branch `neoforge-1.21.11`. **Do not push.**

## Review Focus

1. **Wrong NBT tag types** (e.g. `/summon … {Size:"big"}` or `{MaxHealth:"x"}`): the field keeps its default and nothing crashes. → Task 2 `wrongTypesFallBackToDefaults`.
2. **Out-of-range size** (`Size:0f`, `-3f`, `1000f`): clamped to `[0.3125, 80.0]`, i.e. vanilla `SCALE` 0.0625–16. → Task 2 `sizeIsClamped`.
3. **Invalid texture string** (`Texture:"Not A Path!"`, `""`): renders the default Steve skin and does not crash the client. → Task 4 `invalidTextureFallsBackToSteve`.
4. **Dedicated server loading client classes**: the server must start with no `ClassNotFoundException`/`RuntimeDistCleaner` errors. → Task 3 and Task 5 run `./gradlew runGameTestServer`, which is a dedicated server.
5. **Name persistence**: a fresh NPC gets a random non-blank name. A reloaded NPC keeps its saved name, including an explicitly empty `Name:""`, and is never re-randomised. → Task 3 `namePersistsAndIsNotRerolled`.

---

### Task 1: Test infrastructure, constants, Markov names

**Files:**
- Modify: `build.gradle` (add `unitTest` + JUnit; add `gameTestServer` run)
- Create: `src/main/java/noppes/npcs/constants/*.java`: copy from 1.12.2 `noppes/npcs/constants/` **except** `CustomBlockTypes`, `EnumCompanionTalent`, `EnumGuiType`, which depend on MC/Forge and arrive with their subsystems. Delete any `import noppes.npcs.CustomNpcs` / `LogWriter` usage that breaks compilation.
- Create: `src/main/java/nikedemos/markovnames/**`: copy all 15 files. Rewrite `MarkovDictionary.getResource(...)` to `MarkovDictionary.class.getResourceAsStream("/assets/customnpcs/markovnames/" + file)`, removing the Forge `Loader`/`ModContainer`/`ResourceLocation` imports. Drop `Main.java` (a CLI demo).
- Create: `src/main/resources/assets/customnpcs/markovnames/*.txt`: copy from 1.12.2.
- Create: `src/main/java/noppes/npcs/util/NpcNames.java`
- Test: `src/test/java/noppes/npcs/util/NpcNamesTest.java`

**Interfaces:**
- Produces: `final class NpcNames { static MarkovGenerator generator(int id); static int generatorCount(); static String randomName(java.util.Random random); }`. Generator ids 0–9 in the 1.12.2 order: Roman(3), Japanese(4), Slavic(3), Welsh(3), Saami(3), OldNorse(4), AncientGreek(3), Aztec(3), CustomNPCsClassic(3), Spanish(3). Generators load lazily on first use, so mod construction stays fast. `randomName` picks `generator(random.nextInt(9))` (1.12.2 excludes the last one) and calls `fetch(random.nextInt(2))`.

- [ ] **Step 1: Add test infrastructure to `build.gradle`**

Inside `neoForge { }` add `unitTest { enable(); testedMod = mods."${mod_id}" }`, and add a run `gameTestServer { type = "gameTestServer"; systemProperty 'neoforge.enabledGameTestNamespaces', project.mod_id }`. In `dependencies` add `testImplementation 'org.junit.jupiter:junit-jupiter:5.11.4'` and `testRuntimeOnly 'org.junit.platform:junit-platform-launcher'`. Add `tasks.named('test') { useJUnitPlatform() }`.

- [ ] **Step 2: Write the failing test**

```java
class NpcNamesTest {
    @Test void hasTenGenerators() { assertEquals(10, NpcNames.generatorCount()); }

    @ParameterizedTest @ValueSource(ints = {0,1,2,3,4,5,6,7,8,9})
    void everyGeneratorProducesNames(int id) {
        for (int gender = 0; gender < 2; gender++)
            assertFalse(NpcNames.generator(id).fetch(gender).isBlank(), "generator " + id);
    }

    @Test void randomNameIsDeterministicForSeed() {
        assertEquals(NpcNames.randomName(new Random(42)), NpcNames.randomName(new Random(42)));
        assertFalse(NpcNames.randomName(new Random(7)).isBlank());
    }
}
```

- [ ] **Step 3: Run it and confirm it fails**

Run: `./gradlew test --tests noppes.npcs.util.NpcNamesTest`
Expected: compilation failure, `NpcNames` not found.

- [ ] **Step 4: Copy constants and markovnames, assets, implement `NpcNames`**

- [ ] **Step 5: Run the tests and confirm they pass**

Run: `./gradlew test`
Expected: `BUILD SUCCESSFUL`, 12 tests passed. If `randomNameIsDeterministicForSeed` fails, the generators use their own unseeded `Random`. In that case make the test assert non-blank only and note it in the commit message. Do not modify the Markov algorithm.

- [ ] **Step 6: Commit**

```bash
git add build.gradle src/main/java/noppes/npcs/constants src/main/java/nikedemos src/main/java/noppes/npcs/util src/main/resources/assets/customnpcs/markovnames src/test
git commit -m "Port constants and Markov name generators; add JUnit + GameTest runs"
```

---

### Task 2: `DataDisplay` and `DataStats`

**Files:**
- Create: `src/main/java/noppes/npcs/entity/data/DataDisplay.java`
- Create: `src/main/java/noppes/npcs/entity/data/DataStats.java`
- Test: `src/test/java/noppes/npcs/entity/data/NpcDataTest.java`

**Interfaces:**
- Consumes: `NpcNames.randomName(Random)` (Task 1).
- Produces:
  - `DataDisplay(Runnable onChange)`. Fields with getters and setters; every setter calls `onChange.run()` only when the value actually changes:
    `String name` (default `""`; the entity assigns a random one, not this class),
    `String title` (default `""`),
    `String texture` (default `DataDisplay.DEFAULT_TEXTURE = "customnpcs:textures/entity/humanmale/steve.png"`),
    `int showName` (0 = always, 1 = never, 2 = when attacking; default 0; values outside 0–2 become 0),
    `float size` (default 5.0; clamped to `[DataDisplay.MIN_SIZE = 0.3125f, DataDisplay.MAX_SIZE = 80f]`; NaN → 5.0),
    `float scale()` returns `size / 5f`.
    `void save(ValueOutput out)`, `void load(ValueInput in)` with keys `Name`, `Title`, `Texture`, `ShowName`, `Size`.
  - `DataStats(Runnable onChange)`, with the same setter rule. Defaults copied from 1.12.2 `DataStats.java:18-31`:
    `double maxHealth = 20` (min 1),
    `int aggroRange = 16` (min 1),
    `int healthRegen = 1`, `int combatRegen = 0`, `int respawnTime = 20`,
    `boolean immuneToFire = false`, `canDrown = true`, `burnInSun = false`, `noFallDamage = false`, `hideKilledBody = false`.
    Getters: `getMaxHealth()`, `getAggroRange()`, `getHealthRegen()`, `getCombatRegen()`, `getRespawnTime()`, `isImmuneToFire()`, `canDrown()`, `isBurnInSun()`, `isNoFallDamage()`, `isHideKilledBody()`. Setters: `setX(...)` for each field (e.g. `setMaxHealth(double)`, `setImmuneToFire(boolean)`).
    `save`/`load` with keys `MaxHealth`, `AggroRange`, `HealthRegen`, `CombatRegen`, `RespawnTime`, `ImmuneToFire`, `CanDrown`, `BurnInSun`, `NoFallDamage`, `HideBodyWhenKilled`. `MaxHealth` loads from either a double or a legacy int tag, as 1.12.2 `DataStats.load` does.
  - `load` reads each key with `getXOr(key, currentValue)` and passes it through the setter, so clamping applies to loaded data too.

- [ ] **Step 1: Write the failing tests**

Helpers inside the test: `CompoundTag save(Consumer<ValueOutput>)` uses `TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING)` then `buildResult()`. `ValueInput in(CompoundTag)` uses `TagValueInput.create(ProblemReporter.DISCARDING, RegistryAccess.EMPTY, tag)`.

```java
@Test void displayRoundTrip() {
    var a = new DataDisplay(() -> {});
    a.setName("Bob"); a.setTitle("Smith"); a.setTexture("customnpcs:textures/entity/humanmale/arcticsteve.png");
    a.setShowName(2); a.setSize(7.5f);
    var b = new DataDisplay(() -> {}); b.load(in(save(a::save)));
    assertEquals("Bob", b.getName()); assertEquals("Smith", b.getTitle());
    assertEquals("customnpcs:textures/entity/humanmale/arcticsteve.png", b.getTexture());
    assertEquals(2, b.getShowName()); assertEquals(7.5f, b.getSize());
}
@Test void savesLegacyKeyNames() {
    var a = new DataDisplay(() -> {}); a.setName("Bob");
    var tag = save(a::save);
    assertEquals("Bob", tag.getStringOr("Name", "")); assertEquals(5.0f, tag.getFloatOr("Size", 0f));
    var s = new DataStats(() -> {}); var st = save(s::save);
    assertEquals(20.0, st.getDoubleOr("MaxHealth", 0)); assertTrue(st.getBooleanOr("CanDrown", false));
}
@Test void missingKeysKeepCurrentValues() {
    var d = new DataDisplay(() -> {}); d.setName("Keep"); d.load(in(new CompoundTag()));
    assertEquals("Keep", d.getName()); assertEquals(DataDisplay.DEFAULT_TEXTURE, d.getTexture());
    var s = new DataStats(() -> {}); s.load(in(new CompoundTag()));
    assertEquals(20.0, s.getMaxHealth()); assertEquals(16, s.getAggroRange()); assertTrue(s.canDrown());
}
@Test void sizeIsClamped() {
    var d = new DataDisplay(() -> {});
    d.setSize(0f); assertEquals(DataDisplay.MIN_SIZE, d.getSize());
    d.setSize(-3f); assertEquals(DataDisplay.MIN_SIZE, d.getSize());
    d.setSize(1000f); assertEquals(DataDisplay.MAX_SIZE, d.getSize());
    d.setSize(Float.NaN); assertEquals(5.0f, d.getSize());
    var tag = new CompoundTag(); tag.putFloat("Size", 0f); d.load(in(tag));
    assertEquals(DataDisplay.MIN_SIZE, d.getSize());
}
@Test void wrongTypesFallBackToDefaults() {
    var tag = new CompoundTag(); tag.putString("Size", "big"); tag.putString("MaxHealth", "x"); tag.putInt("Name", 3);
    var d = new DataDisplay(() -> {}); d.load(in(tag));
    assertEquals(5.0f, d.getSize()); assertEquals("", d.getName());
    var s = new DataStats(() -> {}); s.load(in(tag)); assertEquals(20.0, s.getMaxHealth());
}
@Test void legacyIntMaxHealthLoads() {
    var tag = new CompoundTag(); tag.putInt("MaxHealth", 40);
    var s = new DataStats(() -> {}); s.load(in(tag)); assertEquals(40.0, s.getMaxHealth());
}
@Test void onChangeFiresOnlyOnRealChanges() {
    var n = new AtomicInteger(); var d = new DataDisplay(n::incrementAndGet);
    d.setName("A"); d.setName("A"); d.setSize(5.0f);
    assertEquals(1, n.get());
}
```

- [ ] **Step 2: Run them and confirm they fail**

Run: `./gradlew test --tests noppes.npcs.entity.data.NpcDataTest`
Expected: compilation failure, `DataDisplay` not found.

- [ ] **Step 3: Implement `DataDisplay` and `DataStats`**

- [ ] **Step 4: Run the tests and confirm they pass**

Run: `./gradlew test`
Expected: `BUILD SUCCESSFUL`, all `NpcNamesTest` and `NpcDataTest` tests pass.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/noppes/npcs/entity/data src/test/java/noppes/npcs/entity/data
git commit -m "Port DataDisplay and DataStats subset with 1.12.2 NBT keys"
```

---

### Task 3: `EntityCustomNpc` + registration + GameTests

**Files:**
- Create: `src/main/java/noppes/npcs/entity/EntityCustomNpc.java`
- Create: `src/main/java/noppes/npcs/registry/NpcEntities.java`
- Modify: `src/main/java/noppes/npcs/CustomNpcs.java` (call `NpcEntities.init()` before `NpcRegistries.register`; add an `EntityAttributeCreationEvent` listener)
- Modify: `src/main/resources/assets/customnpcs/lang/en_us.json` (`"entity.customnpcs.customnpc": "Custom NPC"`)
- Create: `src/main/java/noppes/npcs/gametest/NpcGameTests.java`
- Create: `src/main/resources/data/customnpcs/structure/empty_5x5.nbt`: a gzipped structure NBT with `size:[5,5,5]`, palette `[{Name:"minecraft:air"}]`, empty `blocks` and `entities`, and `DataVersion` taken from `version.json` → `world_version` inside `build/moddev/artifacts/neoforge-21.11.48-client-extra-aka-minecraft-resources.jar`. Generate it with a throwaway script; commit only the `.nbt`.

**Interfaces:**
- Consumes: `DataDisplay`, `DataStats` (Task 2); `NpcNames.randomName` (Task 1); `NpcRegistries.ENTITY_TYPES` (stage 1).
- Produces:
  - `NpcEntities.CUSTOM_NPC : DeferredHolder<EntityType<?>, EntityType<EntityCustomNpc>>`, registered as `"customnpc"`, `MobCategory.CREATURE`, sized `0.6f × 1.9f` with eye height `1.615f` (1.12.2 `baseWidth`/`baseHeight`/`baseEyeHeight`), `clientTrackingRange(10)`.
  - `class EntityCustomNpc extends PathfinderMob` with `public final DataDisplay display; public final DataStats stats;`, `static AttributeSupplier.Builder createAttributes()` (`MAX_HEALTH` 20, `MOVEMENT_SPEED` 0.25, `FOLLOW_RANGE` 16, `ATTACK_DAMAGE` 2), `Identifier getSkinTexture()` (client use, see Task 4), `float getDisplayScale()` (reads the synced size).
  - The constructor assigns `display.setName(NpcNames.randomName(new Random()))`. Loading never calls `randomName`.
  - Synced data: `DATA_TEXTURE` (String), `DATA_SIZE` (Float), `DATA_SHOW_NAME` (Integer), `DATA_TITLE` (String). `display`'s onChange copies its fields into these accessors, calls `setCustomName(Component.literal(name))`, and sets the `SCALE` attribute base to `display.scale()` (vanilla then refreshes dimensions). `stats`' onChange sets the `MAX_HEALTH` base to `getMaxHealth()` (then clamps current health to max) and the `FOLLOW_RANGE` base to `getAggroRange()`.
  - Overrides: `fireImmune()` → `stats.isImmuneToFire()`, `canBreatheUnderwater()` → `!stats.canDrown()`, `causeFallDamage(...)` → `false` when `stats.isNoFallDamage()`, `shouldShowName()` → per `showName` (0 = non-empty name, 1 = false, 2 = non-empty and `isAggressive()`), `removeWhenFarAway(double)` → `false`, `requiresCustomPersistence()` → `true`.
  - `burnInSun` and the regen/respawn fields only round-trip in this plan. Their behaviour is stage 2b (combat/AI).
  - Goals: `FloatGoal`, `LookAtPlayerGoal(Player, 8f)`, `RandomLookAroundGoal`. No wandering yet: 1.12.2 NPCs stand still by default.
  - `addAdditionalSaveData(out)`: `super`, then `display.save(out)`, `stats.save(out)`. `readAdditionalSaveData(in)` does the reverse, then re-applies the synced data and attributes.
- `NpcGameTests`: test functions registered through a `DeferredRegister<Consumer<GameTestHelper>>` on `Registries.TEST_FUNCTION`. Instances are registered in `RegisterGameTestsEvent` as `FunctionGameTestInstance` with structure `customnpcs:empty_5x5`, maxTicks 100, `required = true`. Check the exact `TestData`/environment construction against `net.neoforged.neoforge.event.RegisterGameTestsEvent` and `net.minecraft.gametest.framework.TestData` in `neoforge-21.11.48-sources.jar`.

- [ ] **Step 1: Write the failing GameTests in `NpcGameTests`**

Helper `EntityCustomNpc reload(GameTestHelper h, EntityCustomNpc npc)`: save with `TagValueOutput.createWithContext(ProblemReporter.DISCARDING, h.getLevel().registryAccess())` + `npc.saveWithoutId(out)`, then create a new entity from that tag with `EntityType.create(ValueInput, Level, EntitySpawnReason.LOAD)` or the 1.21.11 equivalent.

```java
// spawnsWithDefaults
var npc = h.spawn(NpcEntities.CUSTOM_NPC.get(), new BlockPos(2, 1, 2));
h.assertFalse(npc.display.getName().isBlank(), "random name");
h.assertValueEqual(npc.getMaxHealth(), 20f, "max health");
h.assertValueEqual(npc.getDisplayScale(), 1f, "scale"); h.succeed();

// roundTripKeepsDisplayAndStats
npc.display.setName("Bob"); npc.display.setSize(10f); npc.stats.setMaxHealth(40); npc.stats.setImmuneToFire(true);
var copy = reload(h, npc);
h.assertValueEqual(copy.display.getName(), "Bob", "name"); h.assertValueEqual(copy.display.getSize(), 10f, "size");
h.assertValueEqual(copy.getMaxHealth(), 40f, "max health"); h.assertTrue(copy.fireImmune(), "fire immune");
h.assertValueEqual(copy.getScale(), 2f, "SCALE attribute"); h.succeed();

// namePersistsAndIsNotRerolled
npc.display.setName(""); var copy = reload(h, npc);
h.assertValueEqual(copy.display.getName(), "", "empty name kept"); h.succeed();

// maxHealthChangeDoesNotLeaveHealthAboveMax
npc.stats.setMaxHealth(40); npc.setHealth(40); npc.stats.setMaxHealth(10);
h.assertTrue(npc.getHealth() <= 10f, "health clamped"); h.succeed();
```

- [ ] **Step 2: Run them and confirm they fail**

Run: `./gradlew runGameTestServer`
Expected: compilation failure (`EntityCustomNpc` missing).

- [ ] **Step 3: Implement `EntityCustomNpc`, `NpcEntities`, the attribute event, the lang entry and the structure file**

- [ ] **Step 4: Run them and confirm they pass**

Run: `./gradlew build runGameTestServer`
Expected: `BUILD SUCCESSFUL`. The log reports all 4 `customnpcs:` tests passed, and the server exits 0 with no `ClassNotFoundException` (Review Focus #4).

- [ ] **Step 5: Commit**

```bash
git add src/main/java/noppes/npcs src/main/resources
git commit -m "Add EntityCustomNpc with display/stats persistence and GameTests"
```

---

### Task 4: Client renderer

**Files:**
- Create: `src/main/java/noppes/npcs/client/renderer/NpcRenderState.java`
- Create: `src/main/java/noppes/npcs/client/renderer/NpcRenderer.java`
- Create: `src/main/java/noppes/npcs/client/NpcTextures.java`
- Modify: `src/main/java/noppes/npcs/client/CustomNpcsClient.java` (register the renderer in `EntityRenderersEvent.RegisterRenderers`)
- Create: `src/main/resources/assets/customnpcs/textures/entity/**`: copy the whole 1.12.2 `textures/entity/` tree. 1.21 still uses the plural `entity` folder, so no rename is needed.
- Test: `src/test/java/noppes/npcs/client/NpcTexturesTest.java`

**Interfaces:**
- Consumes: `EntityCustomNpc.getSkinTexture()`, `getDisplayScale()`, synced `DATA_TEXTURE` (Task 3).
- Produces:
  - `final class NpcTextures { static final Identifier DEFAULT = Identifier.parse(DataDisplay.DEFAULT_TEXTURE); static Identifier resolve(String raw); }`. Returns `DEFAULT` for null, blank, or anything `Identifier.tryParse` rejects. Results are cached in a `ConcurrentHashMap<String, Identifier>`. `EntityCustomNpc.getSkinTexture()` delegates to it. `NpcTextures` must not import client-only classes, because the entity calls it on both sides.
  - `NpcRenderState extends HumanoidRenderState` with `Identifier texture`.
  - `NpcRenderer extends HumanoidMobRenderer<EntityCustomNpc, NpcRenderState, HumanoidModel<NpcRenderState>>`. The model is baked from `ModelLayers.ZOMBIE` (64×64 humanoid; full player model with sleeves/jacket is stage 4). Shadow radius is 0.5. `extractRenderState` copies the texture; `getTextureLocation(state)` returns it. Scale comes from the vanilla `SCALE` attribute, so there is no manual scaling.
  - The title line under the name is stage 4 (it needs a custom name-tag submit).

- [ ] **Step 1: Write the failing test**

```java
@Test void validTextureIsKept() {
    assertEquals(Identifier.parse("customnpcs:textures/entity/humanmale/arcticsteve.png"),
        NpcTextures.resolve("customnpcs:textures/entity/humanmale/arcticsteve.png"));
}
@Test void invalidTextureFallsBackToSteve() {
    assertEquals(NpcTextures.DEFAULT, NpcTextures.resolve("Not A Path!"));
    assertEquals(NpcTextures.DEFAULT, NpcTextures.resolve(""));
    assertEquals(NpcTextures.DEFAULT, NpcTextures.resolve(null));
}
```

- [ ] **Step 2: Run it and confirm it fails**

Run: `./gradlew test --tests noppes.npcs.client.NpcTexturesTest`
Expected: compilation failure.

- [ ] **Step 3: Implement `NpcTextures`, `NpcRenderState`, `NpcRenderer` and the registration; copy the textures**

- [ ] **Step 4: Run the tests and confirm they pass**

Run: `./gradlew test runGameTestServer`
Expected: all JUnit tests and the 4 GameTests pass.

- [ ] **Step 5: Manual client check** (the human partner runs this; agents cannot see the window)

Run: `./gradlew runClient`. In a creative superflat world, run:
`/summon customnpcs:customnpc ~2 ~ ~ {Name:"Bob",Size:10f,Texture:"customnpcs:textures/entity/humanmale/arcticsteve.png"}`
`/summon customnpcs:customnpc ~-2 ~ ~ {Texture:"Not A Path!"}`
Expected: Bob is twice player size with the arctic skin and the name "Bob". The second NPC is normal size with the Steve skin and a random name. No crash. Quit and reload the world: both NPCs are unchanged.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/noppes/npcs/client src/main/resources/assets/customnpcs/textures/entity src/test/java/noppes/npcs/client
git commit -m "Add NPC renderer with skin texture fallback"
```

---

### Task 5: NPC Wand, creative tab, docs

**Files:**
- Create: `src/main/java/noppes/npcs/items/ItemNpcWand.java`
- Create: `src/main/java/noppes/npcs/registry/NpcItems.java`
- Create: `src/main/java/noppes/npcs/entity/NpcSpawner.java`
- Modify: `CustomNpcs.java` (call `NpcItems.init()`)
- Create: `src/main/resources/assets/customnpcs/items/npcwand.json` (`{"model":{"type":"minecraft:model","model":"customnpcs:item/npcwand"}}`)
- Create: `src/main/resources/assets/customnpcs/models/item/npcwand.json`: parent `minecraft:item/handheld`, `layer0` = `minecraft:item/stick`. This is a placeholder: the 1.12.2 wand is an OBJ model, ported in stage 4.
- Modify: `en_us.json` (`"item.customnpcs.npcwand": "Npc Wand"`, `"availability.permission": "You don't have permission to do this"`)
- Modify: `NpcGameTests.java` (2 tests)
- Modify: `docs/PORTING_PLAN.md` (mark stage 2a done in the Stage 2 section, listing what moved to 2b)

**Interfaces:**
- Consumes: `NpcEntities.CUSTOM_NPC`, `EntityCustomNpc` (Task 3); `NpcRegistries.ITEMS`, `CREATIVE_TABS` (stage 1).
- Produces:
  - `final class NpcSpawner { static EntityCustomNpc spawnAt(ServerLevel level, BlockPos ground, float yaw); }`. It spawns at `ground.above()` centred (`x+0.5, z+0.5`), facing `yaw`, with full health, using `EntitySpawnReason.SPAWN_ITEM_USE`.
  - `NpcItems.NPC_WAND : DeferredItem<ItemNpcWand>` (`"npcwand"`, stacksTo 1). `NpcItems.TAB : DeferredHolder<CreativeModeTab, CreativeModeTab>` (`"tools"`, title `itemGroup.customnpcs`, icon = wand, contains the wand).
  - `ItemNpcWand.useOn(UseOnContext)`. On the server, if the player lacks `Permissions.COMMANDS_GAMEMASTER` (`player.permissions().hasPermission(...)`), it sends `Component.translatable("availability.permission").withStyle(ChatFormatting.RED)` and returns `InteractionResult.FAIL`. Otherwise it calls `NpcSpawner.spawnAt(level, ctx.getClickedPos(), player.getYRot())` and returns `SUCCESS`. Opening the NPC edit GUI (1.12.2 behaviour) arrives in stage 3/4, as does right-clicking an existing NPC.

- [ ] **Step 1: Write the failing GameTests**

```java
// spawnerPlacesNpcAboveClickedBlock
var ground = h.absolutePos(new BlockPos(2, 0, 2));
var npc = NpcSpawner.spawnAt(h.getLevel(), ground, 90f);
h.assertValueEqual(npc.blockPosition(), ground.above(), "position");
h.assertValueEqual(npc.getHealth(), npc.getMaxHealth(), "full health"); h.succeed();

// wandDeniedWithoutPermission
var player = h.makeMockPlayer(GameType.SURVIVAL);   // permission level 0
player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(NpcItems.NPC_WAND.get()));
var hit = new BlockHitResult(Vec3.atCenterOf(h.absolutePos(new BlockPos(2,0,2))), Direction.UP, h.absolutePos(new BlockPos(2,0,2)), false);
var result = player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
h.assertValueEqual(result, InteractionResult.FAIL, "denied");
h.assertEntityNotPresent(NpcEntities.CUSTOM_NPC.get()); h.succeed();
```

- [ ] **Step 2: Run them and confirm they fail**

Run: `./gradlew runGameTestServer`
Expected: compilation failure (`NpcSpawner`/`NpcItems` missing).

- [ ] **Step 3: Implement the spawner, wand, registry, tab, item model files, lang and docs update**

- [ ] **Step 4: Run everything and confirm it passes**

Run: `./gradlew build runGameTestServer`
Expected: all JUnit tests and 6 GameTests pass; the dedicated server exits 0.

- [ ] **Step 5: Manual client check** (human partner)

`./gradlew runClient` → creative inventory → "Custom NPCs" tab shows the Npc Wand (stick icon). Right-click the ground and an NPC appears on that block, facing the same way as the player. The jar from `./gradlew build` (`build/libs/`) does the same when installed in a real NeoForge 21.11.48+ instance.

- [ ] **Step 6: Commit**

```bash
git add src/main docs/PORTING_PLAN.md
git commit -m "Add NPC Wand and creative tab; stage 2a complete"
```
