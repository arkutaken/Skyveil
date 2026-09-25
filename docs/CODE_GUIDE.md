# Reading and changing Skyveil

## Where to start

1. Read [ARCHITECTURE.md](ARCHITECTURE.md) for state ownership and thread boundaries.
2. Open [SkyveilClientEntrypoint.java](../src/client/java/name/skyveil/client/SkyveilClientEntrypoint.java) to see registration and lifecycle order.
3. Find the feature package in the architecture table. Start with its parser or data record, then its manager/HUD, then its settings and event hooks.
4. Read the corresponding tests under `src/test/java/name/skyveil/client`. Their fixtures show supported server messages and item metadata without requiring a live server.

`register()` usually installs callbacks once; `tick()` updates state; `render()` draws it; `reset()` drops transient observations. These are conventions, not an interface shared by every feature. Read each owner's reset conditions.

`CompoundTag extra` means Hypixel's item ExtraAttributes extracted from Minecraft custom data. An item ID identifies its kind; a UUID identifies one instance. Do not substitute display names for stable IDs. GUI coordinates are scaled screen coordinates; world coordinates are separate.

## Follow a price from item to tooltip

[AuctionTooltip](../src/client/java/name/skyveil/client/auction/AuctionTooltip.java) obtains the item's attributes and asks [AuctionPrices](../src/client/java/name/skyveil/client/auction/AuctionPrices.java) for a quote. `identity()` groups base items (including pet species, rarity and calculated level). Lowest BIN uses that identity.

[AuctionSimilarity](../src/client/java/name/skyveil/client/auction/AuctionSimilarity.java) normalizes supported upgrades and hashes them to a comparison key. Enchanting-table levels are removed from ordinary-item comparisons. Instance UUIDs and Museum binding are not upgrade identities. Pets additionally compare skin, not held items, exact XP or candy use.

[AuctionHistory](../src/client/java/name/skyveil/client/auction/AuctionHistory.java) averages observed hourly matching prices. This is an asking-price estimate, not a sale-history service. A current matching listing is required. Read [CRAFT_COST.md](CRAFT_COST.md) for all pricing rules and limitations.

[CraftCostTooltip](../src/client/java/name/skyveil/client/craftcost/CraftCostTooltip.java) handles visibility, market lookup and short-lived reuse. [CraftCostCalculator](../src/client/java/name/skyveil/client/craftcost/CraftCostCalculator.java) expands recipes and adds installed upgrades. Its injected price function lets tests supply fixed prices. A null coin result means required inputs are missing; do not turn it into zero or show a partial total as complete.

## Follow a widget to the screen

Commissions and pickaxe ability status read server-supplied TAB widget lines every five client ticks. `CommissionParser` and `PickaxeAbilityParser` interpret those lines; their HUDs retain and draw the result. They do not issue commands to enable server widgets. Missing widget data cannot be replaced with a guessed cooldown.

`CrystalHollowsMap` handles coordinate mapping and region/location interpretation. `CrystalHollowsMapHud` reads live position and draws the map. `CrystalHollowsDiscoveries` records entry positions on location transitions, and records King Yolkar from a loaded named entity. An entry marker is the observed transition position, not a scanned physical doorway. Reentry may replace it; a world change clears it.

`CorpseWaypoints` identifies loaded armor stands by helmet item IDs. It does not know where unloaded corpses are.

`PlayerStatsHud` observes original action-bar messages before text filters remove recognized stats. Keep that ordering: observing only filtered messages loses health/mana readings. Unrelated action-bar messages do not erase the last known values.

## Edit a setting or add a HUD

The [SkyveilConfig](../src/client/java/name/skyveil/client/config/SkyveilConfig.java) model holds defaults and saved values. [ConfigManager](../src/client/java/name/skyveil/client/config/ConfigManager.java) validates and migrates it. [SettingsRegistry](../src/client/java/name/skyveil/client/config/SettingsRegistry.java) assigns controls to categories; [SettingDefinition](../src/client/java/name/skyveil/client/config/SettingDefinition.java) binds each control to its getter/setter.

For a setting, update the model, validation/migration where necessary, and registry entry. Keep its registry key stable: HUD right-click navigation uses that exact key.

For a HUD, register its callback and lifecycle updates, add the configuration controls, and add its element and preview to [HudEditorScreen](../src/client/java/name/skyveil/client/gui/HudEditorScreen.java). Supply the same dimensions used by rendering so drag handles, clamping and hit testing match the display. Dimensions are unscaled; editor bounds multiply them by the configured scale.

Use [SkyveilTheme](../src/client/java/name/skyveil/client/gui/SkyveilTheme.java) for shared colors and [HudVisibility](../src/client/java/name/skyveil/client/gui/HudVisibility.java) for visibility/dimming/occlusion. Health, mana and vitality have no full panel background; defense and speed measure their displayed text to fit their panels. Mining widgets hide while TAB is held but continue polling.

## Comments worth keeping

Explain constraints, ownership, units, missing-data behavior and ordering dependencies. Use class Javadoc for purpose and method Javadoc for non-obvious contracts. Use inline comments for the reason behind a branch. Avoid comments that merely repeat an assignment or describe every closing brace.

When changing behavior, update the nearby comment and relevant guide together. Avoid large formatting-only rewrites mixed with logic fixes: a small focused diff is easier to review.

## Building and checking a change

From the repository root in PowerShell, with JDK 25 available:

```powershell
.\gradlew.bat test --console=plain
.\gradlew.bat test --tests "name.skyveil.client.mining.PickaxeAbilityParserTest" --console=plain
```

Choose the test command appropriate to the change. Tests mostly use controlled fixtures; `AuctionLiveTest` is opt-in and should not be treated as the normal offline check. Changes to Minecraft hooks or visuals also need an in-game check; compilation cannot prove the hook fires or the layout looks correct.

For delivery:

```powershell
.\gradlew.bat bumpPatch --console=plain
# Fill the new CHANGELOG.md section and src/client/resources/assets/skyveil/release_notes.txt.
.\gradlew.bat release --console=plain
```

The output is `build/release/Skyveil-<version>.jar`. Do not create alternate source copies or release directories. Generated catalogs live in resources; their developer generators live in `tools`. Preserve provenance and licenses when regenerating data.

Useful in-game diagnostics include `/skyveil debugprices`, `/skyveil debugpet`, `/skyveil debughunting` and `/skyveil debugwardrobe`; their implementation is in the entrypoint.
