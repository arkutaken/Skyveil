# Skyveil architecture

Skyveil is a client-only Fabric mod. Shared metadata and the small Kotlin identifier helper live in `src/main`; all Minecraft-facing behavior lives in the `client` source set. `SkyveilClientEntrypoint` is the composition root and guards initialization so callbacks are registered once.

## Lifecycle and event flow

Initialization loads configuration, initializes reusable managers, registers retained HUDs, and installs one end-of-client-tick callback. Mixins are limited to points where Fabric events do not expose the required screen, packet, entity, tooltip, or input state. The required mixin configuration makes mapping or signature drift fail during development instead of silently disabling behavior.

Managers compare the current connection, level, or screen identity with the object they previously observed and clear transient data when that owner changes. Container packet mixins only mark menu-derived state dirty; parsing and rebuilding happen later on the client thread. Render callbacks consume prepared state and do not perform network or disk I/O.

## Threads and persistence

Minecraft callbacks, menu inspection, input handling, and rendering run on the client thread. `ConfigManager.save()` validates the mutable settings model, serializes a snapshot on the caller, and queues an atomic temporary-file replacement on one worker. The client-stopping callback waits briefly for that queue to drain so the final UI edit is not lost.

Skyveil has no Auction House, Bazaar, or NPC price client, refresh scheduler, or price cache. The retained Attribute Menu panel derives identity, rarity, ownership, and required quantities from local item data plus the bundled rarity catalog. SkyCoFL can be installed independently for its own tooltip and market features, but SkyCoFL currently documents cross-mod API access as planned rather than exposing a stable consumer interface, so Skyveil does not access its internals or duplicate its network traffic.

## Feature packages

- `config`, `gui`: settings registry, searchable configuration screen, themes, and HUD positioning.
- `hunting`: Attribute Menu detection, lobby-scoped observations, progression calculations, and local rarity/quantity sorting.
- `combat`: event-driven Compact Damage classification and bounded target-specific label aggregation; it never removes or mutates server entities.
- `zoom`: configurable hold-key state and bounded scroll-selected camera FOV magnification; no item-use or overlay behavior.
- `pet`: menu/tab/chat evidence fusion for the currently equipped pet. Connection changes invalidate world evidence; same-network server transfers retain the last pet as stale until the tab widget or `/pets` revalidates it.
- `itemprotection`, `inventorybuttons`, `customkeybind`, `wardrobe`: screen-aware input features that consume events only inside their verified bounds or menus.
- `mixin`: narrow bridges into mapping-sensitive Minecraft paths. Each class explains why its hook is necessary.

## Build and release

`build.gradle.kts` defines the split Loom source sets, Java/Kotlin 25 targets, JUnit tests, resource version expansion, semantic version bump tasks, and the single `release` path. A release contains exactly `build/release/Skyveil-<version>.jar`; the task verifies metadata, classes, mixins, the icon, and the absence of removed feature resources or raw world data.
