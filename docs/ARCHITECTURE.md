# Skyveil architecture

Start with the [code guide](CODE_GUIDE.md) for a reading order and common editing tasks. This document describes the runtime boundaries; [pricing](CRAFT_COST.md) explains the market and replacement-cost rules.

## Source and startup

Skyveil is a client-only Fabric mod. Loom's split source sets are configured in [build.gradle.kts](../build.gradle.kts). The small Kotlin mod initializer and shared metadata live in `src/main`; Minecraft-facing features live in `src/client/java/name/skyveil/client`. Tests mirror those packages under `src/test/java`.

[Fabric metadata](../src/main/resources/fabric.mod.json) names the entrypoints. [SkyveilClientEntrypoint](../src/client/java/name/skyveil/client/SkyveilClientEntrypoint.java) wires configuration, caches, managers, tooltip callbacks, HUDs, commands, ticks, disconnects, and shutdown. Its initialization guard prevents duplicate registration.

## Events and state

`SkyblockSession` identifies whether SkyBlock features should run. The end-of-client-tick callback updates session state before most feature managers. Features with their own reset logic also receive ticks outside SkyBlock so they can discard stale observations.

The usual flow is server evidence (item data, action bar, TAB widgets, scoreboard, or menu packets), then parsing and retained state, then rendering. Parser classes isolate text interpretation from Minecraft UI code. Managers own state and lifecycle; HUD classes draw it. Some HUD classes also own their small tick-driven state.

The [mixin manifest](../src/client/resources/skyveil.client.mixins.json) lists hooks for screen/input, packets, camera, entities, and tooltip rendering. Mixins bridge Minecraft paths that need interception or access; follow the manager they call to find feature policy. Required injections make missing hooks fail visibly when mappings change.

World, connection, and menu identities determine the lifetime of observations. Resetting matters: a commission, corpse, or discovered structure from one world must not appear in the next. Pet tracking has additional transfer/revalidation rules; consult `PetTracker` and its tests before changing them.

## Threads and persistence

Minecraft objects are inspected on the client thread. Network services perform HTTP work on workers and publish snapshots for readers. A quote lookup may schedule work but does not wait for its HTTP response. Craft-cost calculation itself is synchronous and bounded, with the most recent hovered result briefly retained.

| Owner | Data and lifetime |
| --- | --- |
| `ConfigManager` | Mutable settings; validation and JSON snapshot on save, serialized disk writes on one worker to `config/skyveil.json`. |
| `SkyveilCacheManager` | Shared runtime NBT cache at `config/skyveil/skyveil-cache.nbt`; asynchronous startup load, in-memory section updates, atomic replacement at normal shutdown if dirty. |
| `AuctionPrices` | One published immutable market snapshot; demand-triggered refresh with a five-minute retry interval, two download workers, and two prefetched pages. |
| `AuctionHistory` | Latest matching-listing average per observed hour in a rolling 72-hour window; guarded by its monitor, compressed into the shared cache at shutdown. |
| `CraftCostTooltip` | Only the last hovered calculation, reused for up to one second unless market revisions change; reset on disconnect. |
| Mining HUDs | Live observations scoped to the current world/connection; no persistent map of old lobby discoveries. |

Auction history must flush into the cache **before** `SkyveilCacheManager.shutdown()`. Other feature flushes follow the same ownership rule. Do not move history compression into tooltip rendering or every auction refresh. Shutdown persistence means an abnormal process termination may lose observations from that session.

Auction refresh downloads a complete generation before publishing it. Mixed generations, HTTP failures, or mostly undecodable data cannot publish a partial replacement. Current quotes older than 15 minutes are unavailable. This bounds stale display, but a full refresh still transfers the auction pages; the implementation does not claim zero network or CPU cost.

## Feature map

| Packages | Responsibility |
| --- | --- |
| `config`, `gui` | Settings model/registry, search, shared theme, HUD layout and occlusion. |
| `auction`, `bazaar`, `craftcost` | Market snapshots, tooltip comparisons, recipes and applied-upgrade costs. |
| `mining` | Commissions, pickaxe widget, Crystal Hollows map/discoveries, corpse waypoints. |
| `stats`, `performance` | Action-bar statistics, skill XP, FPS/ping/TPS displays. |
| `pet` | Equipped-pet evidence from TAB, chat and menus; XP/level calculation and HUD. |
| `hunting` | Attribute/shard identity, progression, menu lifecycle, sorting and prices. |
| `storage`, `equipment`, `itemsearch` | Observed inventory previews, shortcuts and bundled item search. |
| `itemprotection`, `inventorybuttons`, `customkeybind`, `wardrobe` | Screen-aware input handling and inventory controls. |
| `combat`, `slayer`, `zoom` | Damage aggregation, boss overlay and camera zoom. |
| `itemrarity`, `dungeon`, `tooltip`, `chatcopy`, `bestiary` | Item presentation, tooltip scrolling and chat features. |
| `update`, `cache`, `mixin` | Release notices/updater, persistence and Minecraft integration. |

## Build and release

Use JDK 25 and the checked-in Gradle wrapper. `test` runs JUnit; `release` depends on `build` and verifies the single player-facing JAR in `build/release`, including its versioned metadata, required resources and forbidden legacy entries.

For a completed change batch, run `bumpPatch`, fill in `CHANGELOG.md` and bundled `release_notes.txt`, then run `release`. Retrying a failed build does not require another version bump. See the [code guide](CODE_GUIDE.md#building-and-checking-a-change) for commands.
