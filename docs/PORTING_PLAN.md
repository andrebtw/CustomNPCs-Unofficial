# CustomNPCs-Unofficial: 1.12.2 Forge → 1.21.11 NeoForge

Source: `../CustomNPCs-Unofficial` (read-only reference, 1,660 files / ~210k lines).
Target: this repo. NeoForge 21.11.x, ModDevGradle 2.0.x, Java 21, Parchment mappings.
License: CC BY-NC 3.0 (inherited from upstream). Only that repo is used as a source.
No decompiled jars, no CannibalVox code.

Decisions made up front:
- **Mod id `customnpcs`, base package `noppes.npcs`.** The 1.12.2 names are kept. User scripts
  call `noppes.npcs.api.*`, so keeping the package means scripts work after a type-level update.
- **Ported, not copied wholesale.** The old repo is used as a reference while each subsystem is
  rewritten against 1.21.11 types. Files are copied only when they are MC-free (bucket A).
- **Saves:** 1.12.2 worlds cannot be opened in 1.21.11 anyway. Migration is limited to the
  mod's own JSON/NBT under `world/customnpcs/` (quests, dialogs, factions, clones, scripts),
  which is portable and worth a converter in stage 2/3.

## 1. Package classification

Counts are from the 1.12.2 tree. "MC" means the file imports `net.minecraft*` or Forge.
Only 260/1,660 files (8.3k lines) are MC-free.

### A. Pure logic, reusable as-is or with trivial edits (~3k lines)
| Package | Files / lines | Notes |
|---|---|---|
| `noppes.npcs.constants` | 31 / 534 | 28 MC-free enums (EnumJobType, EnumQuestType…). Copy. |
| `nikedemos.markovnames` | 15 / 479 | Name generator; 1 file touches resources loading. Copy. |
| `noppes.npcs.api.{gui,interfaces,overlay,functions,constants,handler(/data)}` | ~90 | Pure interfaces. Copy in stage 5 (or earlier as contracts). |
| `noppes.npcs.api.entity.data(/role)` | 48 / 1.5k | Mostly MC-free interfaces. |
| `shared/client/gui/{listeners,util}`, `shared/common/util`, 6 files of `util` | ~20 | Small helpers. |

### B. Logic worth keeping, MC types need translating (~45k lines)
The algorithms and data models are reusable. NBT, entity and world types are swapped for
1.21 equivalents (`CompoundTag`, `ValueInput/ValueOutput`, `Codec`, `ServerLevel`…).
| Package | Files / lines | 1.21 notes |
|---|---|---|
| `controllers` + `controllers/data` | 95 / 20k | Quests, dialogs, factions, banks, mail, transport, recipes, clones, spawns. Heavy NBT; add Codecs. Saved state → `SavedData`. |
| `entity/data` | 21 / 6.9k | NPC stats/display/inventory/AI config. Becomes NPC entity save format. |
| `roles` (+companion, data) | 30 / 4.6k | Trader, follower, bank, transporter, jobs. |
| `ai` (attack, movement, target, selector) | 35 / 2.6k | `EntityAIBase` → `Goal`; logic transfers almost 1:1. |
| `command` | 17 / 3.4k | `CommandBase` → Brigadier. Subcommand logic reusable. |
| `schematics` | 6 / 1.3k | Legacy `.schematic` reader: block ids → state flattening table. Or `.nbt` structures. |
| `util` | 17 / 5.5k | Mixed; split helpers from MC glue. |
| `config` | 3 / 0.8k | → `ModConfigSpec`. |

### C. Minecraft-facing, rewrite (~140k lines)
| Package | Files / lines | 1.21 replacement |
|---|---|---|
| `client/**` | 364 / 82k | Biggest chunk. GUIs → `Screen`/`AbstractContainerScreen` + `GuiGraphics`. Models/layers → `EntityModel` + `EntityRenderState` + `LayerDefinition`. The custom-part system (horns, tails, legs, animation) becomes its own sub-project. |
| `shared/client/gui/components` | 36 / 8.4k | Noppes widget toolkit (GuiNpcButton, textfields, scroll lists). **Port this first in stage 4**, since every GUI depends on it. |
| `packets/{client,server}` | 318 / 19k | `CustomPacketPayload` records + `StreamCodec`, registered in `NpcNetwork`. Mechanical but large. |
| `mixin/**` | 121 / 4.1k | Most are accessor mixins working around private fields. In 1.21, replace with ATs, NeoForge events/hooks or `@Accessor` only when unavoidable. Expect < 30 to survive. |
| `entity` (+`old`) | 54 / 11k | `EntityNPCInterface` → `PathfinderMob` subclass + attributes; `entity/old` (legacy race aliases) → drop, handled by data migration. |
| `blocks` (+tiles, custom) | 34 / 5k | Blocks + `BlockEntity`; block metadata → blockstate properties. |
| `items` (+custom) | 34 / 4.5k | No metadata/damage values; NBT → data components; item model JSON → `assets/*/items/*.json`. |
| `containers` | 36 / 2.5k | `Container` → `AbstractContainerMenu` + `MenuType`. |
| `capability` | 4 / 87 | → Data Attachments. |
| `dimensions` | 3 / 1k | Runtime custom dimensions. 1.21 dims are datapack-driven; runtime creation needs a hack. **Defer or drop.** |
| `potions`, `fluids`, `recipebook`, `creativetab`, `CustomParticles` | small | MobEffect, FluidType, recipe JSON/serializers, CreativeModeTab. |
| `net.minecraft.*` shim classes | 4 / 0.9k | Back-ports of `FriendlyByteBuf`/`Component`. **Delete**, since they are native in 1.21. |
| `moe.plushie.armourers_workshop` | 39 / 1k | Armourer's Workshop 1.12 API copy. AW's 1.21 API differs. **Drop**; re-add as optional integration later. |
| root `noppes.npcs.*` | 25 / 6.1k | Event handlers, proxies, spawning. Split across stages 2 to 3. |

### D. Scripting API (~23k lines)
| Package | Files / lines | Plan |
|---|---|---|
| `api` interfaces | ~220 / 9k | Public contract. Keep names/packages. Swap MC param types for API types where they leak (`NBTTagCompound` → `INbt`, etc.). |
| `api/wrapper` (+data, gui, client) | 74 / 11k | Implementations, rewritten on 1.21 types. |
| `api/event` | 19 / 1.9k | Forge events → NeoForge `Event`/`ICancellableEvent`. |
| `controllers/scripts`, `ScriptController` | 6 / 1k | Already JSR-223 based and already probes GraalJS (`js.nashorn-compat`). |
| `client/gui/script` | 13 / 1.3k | Script editor GUI (stage 4/5). |

**Engine choice:** standalone **Nashorn 15.7** (`org.openjdk.nashorn:nashorn-core`, GPLv2+CPE,
runs on Java 15+) bundled with `jarJar`. It is a drop-in for existing scripts (`Java.type`,
`load`, Nashorn quirks). GraalJS (`js.nashorn-compat`) is the optional second engine: faster, but
~25 MB of extra jars. Keep the `IScriptExecutor` abstraction so both remain possible.

## 2. Staged plan

Each stage ends with something that runs (`./gradlew runClient`) and a commit.

### Stage 1: empty project that loads ✅ (build, dedicated server and client verified)
- MDG 2.0.148, NeoForge 21.11.48, Java 21 toolchain, Parchment 2025.12.20.
- `@Mod` common + client entry points, all DeferredRegisters wired (empty), payload
  registrar stub, `neoforge.mods.toml`, logo.
- **Exit:** `./gradlew build` succeeds; `runClient` reaches the title screen with Custom NPCs
  listed in the Mods menu; `runServer` starts.

### Stage 2: core NPC entity + registries
**2a ✅ (plan `docs/superpowers/plans/2026-11-08-stage-2a-npc-entity.md`):** constants + markovnames
copied; `EntityCustomNpc` (`customnpcs:customnpc`) with the `DataDisplay`/`DataStats` subset
(1.12.2 NBT keys: name, title, texture, show-name, size, max health, aggro range, regen, respawn
time, fire/drown/sun/fall/hide-body flags) and synced texture/size/title/show-name; humanoid
renderer with skin fallback; NPC Wand + creative tab. JUnit (`./gradlew test`) and GameTests
(`./gradlew runGameTestServer`) in place.
Moved to 2b and later: `burnInSun`, health/combat regen and respawn behaviour (fields round-trip
only), melee/ranged/resistances/rarity, `DataInventory`, `DataAI` and the `ai/**` goals (NPCs
only float and look around), spawn egg, the other items, the title line and full player model
(stage 4), wand edit GUI and right-clicking existing NPCs (stages 3/4), and the OBJ wand model.

**2b-1 ✅ (plan `docs/superpowers/plans/2026-11-08-stage-2b1-data-gear-factions.md`, design
`docs/superpowers/specs/2026-11-08-stage-2b-combat-design.md`):** every remaining `DataStats` key
plus `DataMelee`, `DataRanged` and `Resistances` (flat, 1.12.2 keys); `DataAI` with all keys;
`DataInventory` + `DropSet` (1.21.11 item format under the 1.12.2 keys); the faction subset of
`DataAdvanced`. The NPC holds and wears its gear through the vanilla slots (vanilla never drops it)
and vanilla equipment changes are kept; melee strength, walk speed and step height drive the
attributes. Factions: `FactionRegistry` (`SavedData`, 1.12.2 defaults) and per-player
`PlayerFactionPoints` (attachment, `copyOnDeath`) answering the 1.12.2 friendly/neutral/aggressive
questions. A default NPC stays harmless. These keys are data only; behaviour comes next.

**2b-2 ✅ (plan `docs/superpowers/plans/2026-11-09-stage-2b2-targeting-melee.md`):** `CombatHandler`
(aggressors, threat, priority target, hurt-resistance rule, 40-tick combat reset) and the synced
`Attacking` flag; target goals in the 1.12.2 order (clear target, vanilla hurt-by, closest target
through `NpcAttackSelector`: faction hostility, `AttackOtherFactions`, creative/spectator ignored,
`DirectLOS`, near-home rule); monsters go for `GetsAttacked` factions; damage intake (owner,
same-faction, friendly-player and suffocation damage refused; faction defence with `ThroughWalls`;
retaliation); melee per `DataMelee` (strength, delay, range, knockback, fire/potion effect); every
`EnumNpcTactics` tactic plus the `OnAttack` responses (panic, retreat, nothing), sprint and pounce;
`CalmdownRange` in HitAndRun. Combat GameTests run in a 33x33 arena with an in-level mock player.
Seams left: `getOwner()`/`isFollower()` (roles), `isKilled()` (2b-3), `AbstractTacticGoal.isRanged()`
(2b-4).
Follow-ups (user rulings after the 2b-2 review): a 0-damage hit does not provoke (no revenge target, no
aggressor); an AIDisabled NPC takes no target on any path. `AttackInvisible` (1.12.2 only saved it):
false, the default, means invisible entities are never targeted (selector, hurt-by, retaliation,
defence) and a target that turns invisible is dropped. `DirectLOS` is the 1.12.2 `canEntityBeSeen`,
ported as `EntityCustomNpc.hasLineOfSight` (so vanilla sensing, target goals and the tactics use it):
within aggro range (eye to eye), NORMAL needs a free line; DEAF never senses; BLIND and NONE need no
line; WARY/CALM/REALISTIC need a free line inside a 60 degree view cone, WARY/REALISTIC also sense
targets outside it by chance (always when close, never when far and sneaking); every mode but NORMAL
and NONE rolls a small chance for an invisible mob or player (user ruling; 1.12.2 tested
`EntityLiving`, which left players out). Not ported: 1.12.2's extra "opaque cube" block test (vanilla collider clip instead)
and the sleeping animation part of "sleeping" (animations stage; only dead counts). The client-side
DirectLOS debug render is stage 4.
Not ported (later stages): scripted NPC events, attack lines, animations.
Still owed by 2b:
- **2b-3 damage, death, respawn:** resistances, `PotionImmune`, `IgnoreCobweb`, `ChanceBlockDamage`,
  regen, `BurnInSun`, the killed state, drops per `DataInventory`/`LootMode` (corpse loot via the
  vanilla chest menu), XP, `SpawnCycle`/`RespawnTime`, `reset()`.
- **2b-4 ranged and projectiles:** `EntityProjectile` (`customnpcs:projectile`) driven by
  `DataRanged`, switching to melee at `DistanceToMelee`.
- 2b-3 also owes: a `customnpcs:npc` damage type (resistances need it; melee uses vanilla
  `mobAttack` until then), `ChanceBlockDamage` blocking in `hurtServer`, the void-damage reset of a
  killed NPC, and `isKilled()` behind the seams above.
- Deferred minors from 2b-1 (still open): old `NpcInv`+`DropChance` entries load as empty
  `DropSet`s and are overwritten on save; `Faction.description` defaults to `""` instead of
  `{"text":""}`; `FactionRegistry` `lastId` can load as 0; `DataAI.getMovingPath()` exposes the live
  `int[]` arrays; int `Slot` tags are masked with `& 0xFF`.
- Checked by hand in `runClient` (2026-11-09): the 2a wand check, the 2b-1 gear render check and the
  2b-2 combat check.

1. Copy bucket A (`constants`, `markovnames`).
2. `EntityCustomNpc extends PathfinderMob`, entity type, attributes, spawn egg.
   Port `entity/data` (DataDisplay, DataStats, DataInventory, DataAI) with `ValueInput/ValueOutput`
   save/load. Copy the save format so old clone/NBT exports can be read.
3. Port `ai/**` goals.
4. Minimal renderer: humanoid model + skin texture from `DataDisplay` (no custom parts yet).
5. Items: NPC wand, mob cloner, scripter, mounter, teleporter. Data components for their state.
6. Blocks: the simple ones first (border, waypoint, redstone, mailbox, carpentry bench).
7. Brigadier `/noppes` command skeleton, enough to spawn/inspect NPCs.
8. Server-side controllers in dependency order: Factions → Dialogs → Quests → Transport/Bank/Mail.
   `SavedData` + JSON import of the 1.12.2 `customnpcs/` world folder.
- **Exit:** an NPC can be placed with the wand, saves/loads, fights, and follows AI settings
  set by command or NBT.

### Stage 3: networking
- Payload base: one `record XPayload(...) implements CustomPacketPayload` + `StreamCodec` per packet.
- Port in feature slices, not in bulk: wand/NPC edit packets first, then
  quests/dialogs/factions, then the rest. Write a small helper so the 223 server-bound packets
  become one-liners where they just carry an id + NBT.
- Permission checks (`CustomNpcsPermissions`) move to the server handlers.
- **Exit:** a client can request/receive NPC data. Ready for GUIs.

### Stage 4: GUIs + rendering
1. Port the widget toolkit (`shared/client/gui/**`) onto `GuiGraphics`.
2. NPC edit GUIs (`client/gui/mainmenu`, `advanced`, `roles`), then global GUIs (quests,
   dialogs, factions…), then player-facing GUIs (dialog, quest log, trader, bank).
3. Menus/containers for inventory GUIs.
4. Model parts + animation system (`client/model/**`, `client/layer/**`) on the
   1.21 render-state pipeline. OBJ models via NeoForge's OBJ loader.
5. Remaining blocks/items with custom renderers.
- **Exit:** feature parity for non-script gameplay.

### Stage 5: scripting
1. Copy `api` interfaces, fix leaked MC types.
2. Bundle Nashorn 15.7; port `ScriptController` and the executors.
3. Re-implement `api/wrapper/**` on 1.21 types; port `api/event` and the event hooks.
4. Script editor GUI; script-driven custom GUIs/overlays.
5. Regression corpus: collect real scripts from the community and run them headless
   via GameTests.

### Cross-cutting
- **Mixins:** add `customnpcs.mixins.json` only when the first unavoidable one appears.
  Prefer ATs (`META-INF/accesstransformer.cfg`) and NeoForge events.
- **Assets (56 MB):** textures/sounds/lang copy straight over. Lang `.lang` → `.json`
  (script it). Blockstates/models need 1.21 formats and item-model definitions.
  Texture paths `textures/blocks` → `textures/block`, `items` → `item`.
- **1.21.11 naming:** Mojang renamed `ResourceLocation` → `Identifier` in 1.21.11. Old
  tutorials/1.21.1 code will not compile verbatim.
- **Tests:** GameTests per stage (spawn NPC, save/load roundtrip, quest completion).
