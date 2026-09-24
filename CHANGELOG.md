# Changelog

## 1.24.22

- Improve toggle readability with shadow-free white labels on a darker enabled background.
- Remove panel backgrounds behind health, mana, and vitality.
- Automatically fit defense and speed backgrounds and HUD Layout bounds to the displayed number.

## 1.24.21

- Apply the Commissions-inspired dark panel, orange heading, white text, and flat progress-bar style across Skyveil HUD displays and configuration screens.
- Restyle pet, performance, stats, skill XP, inventory preview, mining, and hunting panels with a shared palette.
- Add matching flat buttons to HUD Layout, color, inventory-button, and keybind editors.
- Preserve meaningful stat, rarity, and map colors; remove the old config color switch and inventory-preview purple selector.

## 1.24.20

- Refresh auction data only when prices are requested, at most once every five minutes, using low-priority background threads.
- Bound auction downloads to two prefetched pages and release processed page data immediately.
- Compress auction history only during shutdown, removing periodic compression and shared-cache copies from gameplay.

## 1.24.19

- Price pets by type, rarity, and level for Lowest BIN and 3-day auction comparisons.
- Compare same-level pets without requiring identical XP or held pet items.

## 1.24.18

- Replace the Crystal Hollows player dot and direction ray with a rotating arrowhead.
- Remove the map's background grid lines.
- Automatically discover King Yolkar and add a K map marker and world waypoint using his NPC position.

## 1.24.17

- Automatically mark discovered Divan, Goblin Queen's Den, Jungle Temple, Precursor City, and Bal entry points with colored squares on the Crystal Hollows map.
- Add matching world waypoints with distance labels at the recorded XYZ position.
- Update markers on re-entry, keep them fixed while exploring, and clear them between worlds. Locations use the server's area-label transitions.

## 1.24.16

- Add a Crystal Hollows region map under Mining with live player position, facing direction, coordinates, and region labels.
- Support moving, resizing, and right-click configuration from HUD Layout.
- Hide the map outside Crystal Hollows and while holding the player-list key.

## 1.24.15

- Exclude rolled reforges such as Blended from full craft cost instead of treating them as unpriced inputs.
- Keep reforge stones and their application fees in the calculation.

## 1.24.14

- Hide Commissions and Pickaxe Ability overlays while holding the player-list key so they do not overlap TAB widgets.
- Continue tracking live progress and cooldowns while the overlays are hidden.

## 1.24.13

- Make the pickaxe ability display a compact single line with a background sized to its current text.
- Match HUD Layout selection bounds to the live display dimensions.

## 1.24.12

- Keep the last server stat readings through temporary action-bar interruptions instead of clearing them after ten seconds.
- Observe original action-bar values before HUD message formatting and hide resource bars until their first server reading.
- Expand pickaxe widget parsing for named status rows, parenthesized timers, Available in timers, and formatted spacing.

## 1.24.11

- Added colored Lapis, Tungsten, and Umber corpse waypoints with live distances in the Glacite mining areas.
- Added individual corpse waypoint toggles under Mining. Markers use loaded server entities and clear on area/world changes.

## 1.24.10

- Add a Pickaxe Ability Cooldown HUD under Mining, using live Hypixel widget status.
- Support cooldown times, Ready and Active states, with movement, resizing, and right-click settings navigation in HUD Layout.
- Clear old ability status on world changes, disconnects, and missing widget data.

## 1.24.9

- Right-click any display in HUD Layout to open, scroll to, and highlight its exact configuration setting.
- Save pending layout changes before navigating to settings and show the shortcut in the editor hint.

## 1.24.8

- Add Mining settings and a live Commissions HUD sourced from the Hypixel widget, with positioning through the shared HUD Layout editor.
- Match three-day auction estimates by paid upgrade configuration and report when no similar active listings exist.
- Replace auction snapshots only on API updates and retain compact, bounded hourly price history.
- Correct craft-cost item IDs, drill parts, table-enchantment handling, and Warped reforge lookup.
- Distinguish inherently soulbound items from Museum-bound items and hide craft costs on unmodified, noncraftable materials.
- Remove the Active BIN average tooltip row and replace long missing-price lists with a compact count and command diagnostics.
- Require a version bump for each delivered batch of mod changes.

## 1.24.7

- Verify the shared armor craft-cost calculator across 658 catalog armor IDs and 100 fully upgraded Kuudra set/slot/tier combinations.
- Add regression coverage for all armor slots, applied books/recombobulation, enchants, reforges, gemstones, stars, and complete prestige chains using controlled prices.

## 1.24.6

- Calculate prestige equipment costs through every previous tier and required star level.
- Show Kuudra Teeth and Heavy Pearls as required materials alongside the coin estimate instead of failing the whole calculation.
- Accept innate level-one Kuudra attributes when finding a base item and valuing inherited attributes.
- Verify Fiery Aurora Chestplate with Loving, enchants, recombobulation, and gemstones using live prices.

## 1.24.5

- Fix unavailable craft costs caused by omitted basic enchanting-table enchants: Aiming, Impaling, Flame I, Piercing, Snipe III, Knockback, and Punch.
- Add the standalone /skyveilprices diagnostic command and verify registration of both existing aliases.
- Verify a five-star Precise Terminator with the pictured enchants against live market prices.

## 1.24.4

- Add /sv debugprices to identify missing Auction House tooltip prices in the running client: version, module/session state, price count/age, last download error, held item key, filters, and rendered price row.

## 1.24.3

- Refresh Auction House and Full Craft Cost prices at final screen tooltip construction as well as the shared item callback.
- Replace cached price rows without duplicates and show Loading during the initial auction download.
- Add live coverage for Thorny Blossom Necklace with Green Thumb III.

## 1.24.2

- Resolve reforge modifier IDs with different word separators, including blood_shot -> Bloodshot -> Shriveled Cornea, for full craft costs.

## 1.24.1

- Remove the Missing cost details from item tooltips.

## 1.24.0

- Add Full Craft Cost under Interface > Items, using current ingredient buy prices and applied upgrade costs.
- Include enchants, reforge stones/fees, potato books, recombobulators, gemstones/unlocks, stars, scrolls, and supported consumable upgrades.
- Bundle recipe and upgrade definitions; report missing costs instead of presenting partial totals.
- Price noncraftable base items from unmodified auctions to avoid counting applied upgrades twice.
- Keep a single current market snapshot and only the last hovered calculation; no historical craft-cost cache is written.

## 1.23.4

- Use Fabric's shared item-tooltip callback for Auction House prices.
- Refresh auction data when entering SkyBlock instead of waiting for an eligible hover.
- Remove the dependency on a successful Bazaar download before showing Auction House prices.
- Retry snapshot rollovers, cancel abandoned page requests, retain good snapshots on failures, and log coverage/decoding failures.
- Match actual soulbound markers, escaped pet metadata, rune variants, and combined enchantment books.
- Keep single-item tooltips free of redundant totals.

## 1.23.3

- Prevent gameplay HUDs from overlapping the item search field and its visible results panel.

## 1.23.2

- Prevent gameplay HUDs from drawing over visible Inventory Buttons, including scaled buttons.

## 1.23.1

- Match market item IDs through legacy and nested component metadata.
- Fetch auction pages with four bounded workers to reduce snapshot rollover failures.
- Show Total only for stacks containing multiple items; keep price numbers white.

## 1.23.0

## 1.23.0

- Add Auction House Item Prices under Interface > Items.
- Show Lowest BIN and stack total with colored labels and white numbers.
- Build an asynchronous price snapshot from complete, consistent Hypixel auction pages.
- Exclude Bazaar products, soulbound items, expired auctions, and non-BIN listings.
- Compare base item IDs, with pet species/rarity separation; upgrades and pet levels are not appraised.
## 1.22.1

## 1.23.0

- Add Auction House Item Prices under Interface > Items.
- Show Lowest BIN and stack total with colored labels and white numbers.
- Build an asynchronous price snapshot from complete, consistent Hypixel auction pages.
- Exclude Bazaar products, soulbound items, expired auctions, and non-BIN listings.
- Compare base item IDs, with pet species/rarity separation; upgrades and pet levels are not appraised.
## 1.22.1

- Color Bazaar tooltip labels while keeping prices white.
- Replace "coins (each)" with the stack total alongside the unit price.
- Format totals with thousands separators and up to one decimal place.
## 1.22.0

## 1.23.0

- Add Auction House Item Prices under Interface > Items.
- Show Lowest BIN and stack total with colored labels and white numbers.
- Build an asynchronous price snapshot from complete, consistent Hypixel auction pages.
- Exclude Bazaar products, soulbound items, expired auctions, and non-BIN listings.
- Compare base item IDs, with pet species/rarity separation; upgrades and pet levels are not appraised.
## 1.22.1

- Color Bazaar tooltip labels while keeping prices white.
- Replace "coins (each)" with the stack total alongside the unit price.
- Format totals with thousands separators and up to one decimal place.
## 1.22.0

- Add Bazaar Item Prices under Interface > Items.
- Show per-item Insta Buy above Insta Sell using the current best orders.
- Match Bazaar products, single-enchantment books, legacy dye IDs, and shards; exclude soulbound items.
- Reuse the shared asynchronous Bazaar request with a one-minute refresh and expire tooltip quotes after three minutes.
## 1.21.3

## 1.23.0

- Add Auction House Item Prices under Interface > Items.
- Show Lowest BIN and stack total with colored labels and white numbers.
- Build an asynchronous price snapshot from complete, consistent Hypixel auction pages.
- Exclude Bazaar products, soulbound items, expired auctions, and non-BIN listings.
- Compare base item IDs, with pet species/rarity separation; upgrades and pet levels are not appraised.
## 1.22.1

- Color Bazaar tooltip labels while keeping prices white.
- Replace "coins (each)" with the stack total alongside the unit price.
- Format totals with thousands separators and up to one decimal place.
## 1.22.0

- Add Bazaar Item Prices under Interface > Items.
- Show per-item Insta Buy above Insta Sell using the current best orders.
- Match Bazaar products, single-enchantment books, legacy dye IDs, and shards; exclude soulbound items.
- Reuse the shared asynchronous Bazaar request with a one-minute refresh and expire tooltip quotes after three minutes.
## 1.21.3

- Allow Performance Stats, Zoom, and Chat Copy outside SkyBlock.
- Restrict Inventory Preview and pet tracking to SkyBlock.
- Keep SkyBlock-specific HUD clipping inactive outside SkyBlock.
- Clear pending item-protection input when leaving SkyBlock.
## 1.21.2

## 1.23.0

- Add Auction House Item Prices under Interface > Items.
- Show Lowest BIN and stack total with colored labels and white numbers.
- Build an asynchronous price snapshot from complete, consistent Hypixel auction pages.
- Exclude Bazaar products, soulbound items, expired auctions, and non-BIN listings.
- Compare base item IDs, with pet species/rarity separation; upgrades and pet levels are not appraised.
## 1.22.1

- Color Bazaar tooltip labels while keeping prices white.
- Replace "coins (each)" with the stack total alongside the unit price.
- Format totals with thousands separators and up to one decimal place.
## 1.22.0

- Add Bazaar Item Prices under Interface > Items.
- Show per-item Insta Buy above Insta Sell using the current best orders.
- Match Bazaar products, single-enchantment books, legacy dye IDs, and shards; exclude soulbound items.
- Reuse the shared asynchronous Bazaar request with a one-minute refresh and expire tooltip quotes after three minutes.
## 1.21.3

- Allow Performance Stats, Zoom, and Chat Copy outside SkyBlock.
- Restrict Inventory Preview and pet tracking to SkyBlock.
- Keep SkyBlock-specific HUD clipping inactive outside SkyBlock.
- Clear pending item-protection input when leaving SkyBlock.
## 1.21.2

- Silence slot-lock toggle chat messages.
- Shorten item-protection toggle messages to "Item protected!" and "Item not protected!".
## 1.21.1

## 1.23.0

- Add Auction House Item Prices under Interface > Items.
- Show Lowest BIN and stack total with colored labels and white numbers.
- Build an asynchronous price snapshot from complete, consistent Hypixel auction pages.
- Exclude Bazaar products, soulbound items, expired auctions, and non-BIN listings.
- Compare base item IDs, with pet species/rarity separation; upgrades and pet levels are not appraised.
## 1.22.1

- Color Bazaar tooltip labels while keeping prices white.
- Replace "coins (each)" with the stack total alongside the unit price.
- Format totals with thousands separators and up to one decimal place.
## 1.22.0

- Add Bazaar Item Prices under Interface > Items.
- Show per-item Insta Buy above Insta Sell using the current best orders.
- Match Bazaar products, single-enchantment books, legacy dye IDs, and shards; exclude soulbound items.
- Reuse the shared asynchronous Bazaar request with a one-minute refresh and expire tooltip quotes after three minutes.
## 1.21.3

- Allow Performance Stats, Zoom, and Chat Copy outside SkyBlock.
- Restrict Inventory Preview and pet tracking to SkyBlock.
- Keep SkyBlock-specific HUD clipping inactive outside SkyBlock.
- Clear pending item-protection input when leaving SkyBlock.
## 1.21.2

- Silence slot-lock toggle chat messages.
- Shorten item-protection toggle messages to "Item protected!" and "Item not protected!".
## 1.21.1

- Send drop-protection toggles and blocked-drop messages to chat using the slot-locking prefix.
- Throttle repeated blocked-drop messages.
- Mark protected items with a small cyan shield in their upper-right corner, including inventory, hotbar, and storage previews.
## 1.21.0

## 1.23.0

- Add Auction House Item Prices under Interface > Items.
- Show Lowest BIN and stack total with colored labels and white numbers.
- Build an asynchronous price snapshot from complete, consistent Hypixel auction pages.
- Exclude Bazaar products, soulbound items, expired auctions, and non-BIN listings.
- Compare base item IDs, with pet species/rarity separation; upgrades and pet levels are not appraised.
## 1.22.1

- Color Bazaar tooltip labels while keeping prices white.
- Replace "coins (each)" with the stack total alongside the unit price.
- Format totals with thousands separators and up to one decimal place.
## 1.22.0

- Add Bazaar Item Prices under Interface > Items.
- Show per-item Insta Buy above Insta Sell using the current best orders.
- Match Bazaar products, single-enchantment books, legacy dye IDs, and shards; exclude soulbound items.
- Reuse the shared asynchronous Bazaar request with a one-minute refresh and expire tooltip quotes after three minutes.
## 1.21.3

- Allow Performance Stats, Zoom, and Chat Copy outside SkyBlock.
- Restrict Inventory Preview and pet tracking to SkyBlock.
- Keep SkyBlock-specific HUD clipping inactive outside SkyBlock.
- Clear pending item-protection input when leaving SkyBlock.
## 1.21.2

- Silence slot-lock toggle chat messages.
- Shorten item-protection toggle messages to "Item protected!" and "Item not protected!".
## 1.21.1

- Send drop-protection toggles and blocked-drop messages to chat using the slot-locking prefix.
- Throttle repeated blocked-drop messages.
- Mark protected items with a small cyan shield in their upper-right corner, including inventory, hotbar, and storage previews.
## 1.21.0

- Add item-based drop protection under Interface > Items, separate from slot locking.
- Press P while hovering an item to toggle protection; configure the key in settings.
- Protect UUID items individually and UUID-less items by type, with a tooltip indicator.
- Allow inventory/storage movement while blocking drop-key and outside-inventory drops.
- Require placing a protected cursor stack before closing the screen to avoid implicit drops.
## 1.20.0

## 1.23.0

- Add Auction House Item Prices under Interface > Items.
- Show Lowest BIN and stack total with colored labels and white numbers.
- Build an asynchronous price snapshot from complete, consistent Hypixel auction pages.
- Exclude Bazaar products, soulbound items, expired auctions, and non-BIN listings.
- Compare base item IDs, with pet species/rarity separation; upgrades and pet levels are not appraised.
## 1.22.1

- Color Bazaar tooltip labels while keeping prices white.
- Replace "coins (each)" with the stack total alongside the unit price.
- Format totals with thousands separators and up to one decimal place.
## 1.22.0

- Add Bazaar Item Prices under Interface > Items.
- Show per-item Insta Buy above Insta Sell using the current best orders.
- Match Bazaar products, single-enchantment books, legacy dye IDs, and shards; exclude soulbound items.
- Reuse the shared asynchronous Bazaar request with a one-minute refresh and expire tooltip quotes after three minutes.
## 1.21.3

- Allow Performance Stats, Zoom, and Chat Copy outside SkyBlock.
- Restrict Inventory Preview and pet tracking to SkyBlock.
- Keep SkyBlock-specific HUD clipping inactive outside SkyBlock.
- Clear pending item-protection input when leaving SkyBlock.
## 1.21.2

- Silence slot-lock toggle chat messages.
- Shorten item-protection toggle messages to "Item protected!" and "Item not protected!".
## 1.21.1

- Send drop-protection toggles and blocked-drop messages to chat using the slot-locking prefix.
- Throttle repeated blocked-drop messages.
- Mark protected items with a small cyan shield in their upper-right corner, including inventory, hotbar, and storage previews.
## 1.21.0

- Add item-based drop protection under Interface > Items, separate from slot locking.
- Press P while hovering an item to toggle protection; configure the key in settings.
- Protect UUID items individually and UUID-less items by type, with a tooltip indicator.
- Allow inventory/storage movement while blocking drop-key and outside-inventory drops.
- Require placing a protected cursor stack before closing the screen to avoid implicit drops.
## 1.20.0

- Add a saved Purple / Black config-theme button beside the Skyveil title.
- Use readable charcoal panels and silver accents for the Black theme.
- Apply theme changes immediately without resetting search, accordion, or scroll state.
## 1.19.6

## 1.23.0

- Add Auction House Item Prices under Interface > Items.
- Show Lowest BIN and stack total with colored labels and white numbers.
- Build an asynchronous price snapshot from complete, consistent Hypixel auction pages.
- Exclude Bazaar products, soulbound items, expired auctions, and non-BIN listings.
- Compare base item IDs, with pet species/rarity separation; upgrades and pet levels are not appraised.
## 1.22.1

- Color Bazaar tooltip labels while keeping prices white.
- Replace "coins (each)" with the stack total alongside the unit price.
- Format totals with thousands separators and up to one decimal place.
## 1.22.0

- Add Bazaar Item Prices under Interface > Items.
- Show per-item Insta Buy above Insta Sell using the current best orders.
- Match Bazaar products, single-enchantment books, legacy dye IDs, and shards; exclude soulbound items.
- Reuse the shared asynchronous Bazaar request with a one-minute refresh and expire tooltip quotes after three minutes.
## 1.21.3

- Allow Performance Stats, Zoom, and Chat Copy outside SkyBlock.
- Restrict Inventory Preview and pet tracking to SkyBlock.
- Keep SkyBlock-specific HUD clipping inactive outside SkyBlock.
- Clear pending item-protection input when leaving SkyBlock.
## 1.21.2

- Silence slot-lock toggle chat messages.
- Shorten item-protection toggle messages to "Item protected!" and "Item not protected!".
## 1.21.1

- Send drop-protection toggles and blocked-drop messages to chat using the slot-locking prefix.
- Throttle repeated blocked-drop messages.
- Mark protected items with a small cyan shield in their upper-right corner, including inventory, hotbar, and storage previews.
## 1.21.0

- Add item-based drop protection under Interface > Items, separate from slot locking.
- Press P while hovering an item to toggle protection; configure the key in settings.
- Protect UUID items individually and UUID-less items by type, with a tooltip indicator.
- Allow inventory/storage movement while blocking drop-key and outside-inventory drops.
- Require placing a protected cursor stack before closing the screen to avoid implicit drops.
## 1.20.0

- Add a saved Purple / Black config-theme button beside the Skyveil title.
- Use readable charcoal panels and silver accents for the Black theme.
- Apply theme changes immediately without resetting search, accordion, or scroll state.
## 1.19.6

- Restrict storage-item tooltips and hover highlights to the visible scissor area.
- Reject live-storage hover and click hit tests outside the workspace viewport.
- Prevent clipped slots from showing tooltips through the player inventory.
## 1.19.5

## 1.23.0

- Add Auction House Item Prices under Interface > Items.
- Show Lowest BIN and stack total with colored labels and white numbers.
- Build an asynchronous price snapshot from complete, consistent Hypixel auction pages.
- Exclude Bazaar products, soulbound items, expired auctions, and non-BIN listings.
- Compare base item IDs, with pet species/rarity separation; upgrades and pet levels are not appraised.
## 1.22.1

- Color Bazaar tooltip labels while keeping prices white.
- Replace "coins (each)" with the stack total alongside the unit price.
- Format totals with thousands separators and up to one decimal place.
## 1.22.0

- Add Bazaar Item Prices under Interface > Items.
- Show per-item Insta Buy above Insta Sell using the current best orders.
- Match Bazaar products, single-enchantment books, legacy dye IDs, and shards; exclude soulbound items.
- Reuse the shared asynchronous Bazaar request with a one-minute refresh and expire tooltip quotes after three minutes.
## 1.21.3

- Allow Performance Stats, Zoom, and Chat Copy outside SkyBlock.
- Restrict Inventory Preview and pet tracking to SkyBlock.
- Keep SkyBlock-specific HUD clipping inactive outside SkyBlock.
- Clear pending item-protection input when leaving SkyBlock.
## 1.21.2

- Silence slot-lock toggle chat messages.
- Shorten item-protection toggle messages to "Item protected!" and "Item not protected!".
## 1.21.1

- Send drop-protection toggles and blocked-drop messages to chat using the slot-locking prefix.
- Throttle repeated blocked-drop messages.
- Mark protected items with a small cyan shield in their upper-right corner, including inventory, hotbar, and storage previews.
## 1.21.0

- Add item-based drop protection under Interface > Items, separate from slot locking.
- Press P while hovering an item to toggle protection; configure the key in settings.
- Protect UUID items individually and UUID-less items by type, with a tooltip indicator.
- Allow inventory/storage movement while blocking drop-key and outside-inventory drops.
- Require placing a protected cursor stack before closing the screen to avoid implicit drops.
## 1.20.0

- Add a saved Purple / Black config-theme button beside the Skyveil title.
- Use readable charcoal panels and silver accents for the Black theme.
- Apply theme changes immediately without resetting search, accordion, or scroll state.
## 1.19.6

- Restrict storage-item tooltips and hover highlights to the visible scissor area.
- Reject live-storage hover and click hit tests outside the workspace viewport.
- Prevent clipped slots from showing tooltips through the player inventory.
## 1.19.5

- Defer container HUD submission until the current frame's tooltip layout is available.
- Clip all shared HUD modules against actual tooltip bounds, including tall and scrolled tooltips outside storage panels.
- Remove the opaque tooltip background workaround and retain normal tooltip styling.
- Clear queued HUD draws and tooltip bounds after each frame.
## 1.19.4

## 1.23.0

- Add Auction House Item Prices under Interface > Items.
- Show Lowest BIN and stack total with colored labels and white numbers.
- Build an asynchronous price snapshot from complete, consistent Hypixel auction pages.
- Exclude Bazaar products, soulbound items, expired auctions, and non-BIN listings.
- Compare base item IDs, with pet species/rarity separation; upgrades and pet levels are not appraised.
## 1.22.1

- Color Bazaar tooltip labels while keeping prices white.
- Replace "coins (each)" with the stack total alongside the unit price.
- Format totals with thousands separators and up to one decimal place.
## 1.22.0

- Add Bazaar Item Prices under Interface > Items.
- Show per-item Insta Buy above Insta Sell using the current best orders.
- Match Bazaar products, single-enchantment books, legacy dye IDs, and shards; exclude soulbound items.
- Reuse the shared asynchronous Bazaar request with a one-minute refresh and expire tooltip quotes after three minutes.
## 1.21.3

- Allow Performance Stats, Zoom, and Chat Copy outside SkyBlock.
- Restrict Inventory Preview and pet tracking to SkyBlock.
- Keep SkyBlock-specific HUD clipping inactive outside SkyBlock.
- Clear pending item-protection input when leaving SkyBlock.
## 1.21.2

- Silence slot-lock toggle chat messages.
- Shorten item-protection toggle messages to "Item protected!" and "Item not protected!".
## 1.21.1

- Send drop-protection toggles and blocked-drop messages to chat using the slot-locking prefix.
- Throttle repeated blocked-drop messages.
- Mark protected items with a small cyan shield in their upper-right corner, including inventory, hotbar, and storage previews.
## 1.21.0

- Add item-based drop protection under Interface > Items, separate from slot locking.
- Press P while hovering an item to toggle protection; configure the key in settings.
- Protect UUID items individually and UUID-less items by type, with a tooltip indicator.
- Allow inventory/storage movement while blocking drop-key and outside-inventory drops.
- Require placing a protected cursor stack before closing the screen to avoid implicit drops.
## 1.20.0

- Add a saved Purple / Black config-theme button beside the Skyveil title.
- Use readable charcoal panels and silver accents for the Black theme.
- Apply theme changes immediately without resetting search, accordion, or scroll state.
## 1.19.6

- Restrict storage-item tooltips and hover highlights to the visible scissor area.
- Reject live-storage hover and click hit tests outside the workspace viewport.
- Prevent clipped slots from showing tooltips through the player inventory.
## 1.19.5

- Defer container HUD submission until the current frame's tooltip layout is available.
- Clip all shared HUD modules against actual tooltip bounds, including tall and scrolled tooltips outside storage panels.
- Remove the opaque tooltip background workaround and retain normal tooltip styling.
- Clear queued HUD draws and tooltip bounds after each frame.
## 1.19.4

- Draw container tooltips on a separate layer with an opaque interior to block HUD text showing through.
- Use final tooltip bounds so the fix also follows oversized, scrolled item tooltips.
- Preserve tooltip frames, shadows, and HUD placement.
## 1.19.3

## 1.23.0

- Add Auction House Item Prices under Interface > Items.
- Show Lowest BIN and stack total with colored labels and white numbers.
- Build an asynchronous price snapshot from complete, consistent Hypixel auction pages.
- Exclude Bazaar products, soulbound items, expired auctions, and non-BIN listings.
- Compare base item IDs, with pet species/rarity separation; upgrades and pet levels are not appraised.
## 1.22.1

- Color Bazaar tooltip labels while keeping prices white.
- Replace "coins (each)" with the stack total alongside the unit price.
- Format totals with thousands separators and up to one decimal place.
## 1.22.0

- Add Bazaar Item Prices under Interface > Items.
- Show per-item Insta Buy above Insta Sell using the current best orders.
- Match Bazaar products, single-enchantment books, legacy dye IDs, and shards; exclude soulbound items.
- Reuse the shared asynchronous Bazaar request with a one-minute refresh and expire tooltip quotes after three minutes.
## 1.21.3

- Allow Performance Stats, Zoom, and Chat Copy outside SkyBlock.
- Restrict Inventory Preview and pet tracking to SkyBlock.
- Keep SkyBlock-specific HUD clipping inactive outside SkyBlock.
- Clear pending item-protection input when leaving SkyBlock.
## 1.21.2

- Silence slot-lock toggle chat messages.
- Shorten item-protection toggle messages to "Item protected!" and "Item not protected!".
## 1.21.1

- Send drop-protection toggles and blocked-drop messages to chat using the slot-locking prefix.
- Throttle repeated blocked-drop messages.
- Mark protected items with a small cyan shield in their upper-right corner, including inventory, hotbar, and storage previews.
## 1.21.0

- Add item-based drop protection under Interface > Items, separate from slot locking.
- Press P while hovering an item to toggle protection; configure the key in settings.
- Protect UUID items individually and UUID-less items by type, with a tooltip indicator.
- Allow inventory/storage movement while blocking drop-key and outside-inventory drops.
- Require placing a protected cursor stack before closing the screen to avoid implicit drops.
## 1.20.0

- Add a saved Purple / Black config-theme button beside the Skyveil title.
- Use readable charcoal panels and silver accents for the Black theme.
- Apply theme changes immediately without resetting search, accordion, or scroll state.
## 1.19.6

- Restrict storage-item tooltips and hover highlights to the visible scissor area.
- Reject live-storage hover and click hit tests outside the workspace viewport.
- Prevent clipped slots from showing tooltips through the player inventory.
## 1.19.5

- Defer container HUD submission until the current frame's tooltip layout is available.
- Clip all shared HUD modules against actual tooltip bounds, including tall and scrolled tooltips outside storage panels.
- Remove the opaque tooltip background workaround and retain normal tooltip styling.
- Clear queued HUD draws and tooltip bounds after each frame.
## 1.19.4

- Draw container tooltips on a separate layer with an opaque interior to block HUD text showing through.
- Use final tooltip bounds so the fix also follows oversized, scrolled item tooltips.
- Preserve tooltip frames, shadows, and HUD placement.
## 1.19.3

- Clip gameplay HUDs around the active Storage Preview workspace, inventory, and controls.
- Apply the shared rule to pet, stats, skill XP, performance, and inventory preview displays.
- Preserve HUD positions, resizing, and normal inventory dimming.
## 1.19.2

## 1.23.0

- Add Auction House Item Prices under Interface > Items.
- Show Lowest BIN and stack total with colored labels and white numbers.
- Build an asynchronous price snapshot from complete, consistent Hypixel auction pages.
- Exclude Bazaar products, soulbound items, expired auctions, and non-BIN listings.
- Compare base item IDs, with pet species/rarity separation; upgrades and pet levels are not appraised.
## 1.22.1

- Color Bazaar tooltip labels while keeping prices white.
- Replace "coins (each)" with the stack total alongside the unit price.
- Format totals with thousands separators and up to one decimal place.
## 1.22.0

- Add Bazaar Item Prices under Interface > Items.
- Show per-item Insta Buy above Insta Sell using the current best orders.
- Match Bazaar products, single-enchantment books, legacy dye IDs, and shards; exclude soulbound items.
- Reuse the shared asynchronous Bazaar request with a one-minute refresh and expire tooltip quotes after three minutes.
## 1.21.3

- Allow Performance Stats, Zoom, and Chat Copy outside SkyBlock.
- Restrict Inventory Preview and pet tracking to SkyBlock.
- Keep SkyBlock-specific HUD clipping inactive outside SkyBlock.
- Clear pending item-protection input when leaving SkyBlock.
## 1.21.2

- Silence slot-lock toggle chat messages.
- Shorten item-protection toggle messages to "Item protected!" and "Item not protected!".
## 1.21.1

- Send drop-protection toggles and blocked-drop messages to chat using the slot-locking prefix.
- Throttle repeated blocked-drop messages.
- Mark protected items with a small cyan shield in their upper-right corner, including inventory, hotbar, and storage previews.
## 1.21.0

- Add item-based drop protection under Interface > Items, separate from slot locking.
- Press P while hovering an item to toggle protection; configure the key in settings.
- Protect UUID items individually and UUID-less items by type, with a tooltip indicator.
- Allow inventory/storage movement while blocking drop-key and outside-inventory drops.
- Require placing a protected cursor stack before closing the screen to avoid implicit drops.
## 1.20.0

- Add a saved Purple / Black config-theme button beside the Skyveil title.
- Use readable charcoal panels and silver accents for the Black theme.
- Apply theme changes immediately without resetting search, accordion, or scroll state.
## 1.19.6

- Restrict storage-item tooltips and hover highlights to the visible scissor area.
- Reject live-storage hover and click hit tests outside the workspace viewport.
- Prevent clipped slots from showing tooltips through the player inventory.
## 1.19.5

- Defer container HUD submission until the current frame's tooltip layout is available.
- Clip all shared HUD modules against actual tooltip bounds, including tall and scrolled tooltips outside storage panels.
- Remove the opaque tooltip background workaround and retain normal tooltip styling.
- Clear queued HUD draws and tooltip bounds after each frame.
## 1.19.4

- Draw container tooltips on a separate layer with an opaque interior to block HUD text showing through.
- Use final tooltip bounds so the fix also follows oversized, scrolled item tooltips.
- Preserve tooltip frames, shadows, and HUD placement.
## 1.19.3

- Clip gameplay HUDs around the active Storage Preview workspace, inventory, and controls.
- Apply the shared rule to pet, stats, skill XP, performance, and inventory preview displays.
- Preserve HUD positions, resizing, and normal inventory dimming.
## 1.19.2

- Refactor pet metadata parsing into a structured reader and remove regex-based field extraction.
- Bound menu scan delays so continuous container packets cannot prevent caching.
- Keep selected pet snapshots atomic instead of merging visual details from another state.
- Preserve owned head textures and use bundled species textures when texture data is absent.
- Resolve held-item icons by ID or lore name without a second growing icon cache.
- Replace misleading unresolved "No Pet Item" output with a Pets-menu sync prompt.
- Rebuild the legacy pet cache using the corrected format.
## 1.19.1

## 1.23.0

- Add Auction House Item Prices under Interface > Items.
- Show Lowest BIN and stack total with colored labels and white numbers.
- Build an asynchronous price snapshot from complete, consistent Hypixel auction pages.
- Exclude Bazaar products, soulbound items, expired auctions, and non-BIN listings.
- Compare base item IDs, with pet species/rarity separation; upgrades and pet levels are not appraised.
## 1.22.1

- Color Bazaar tooltip labels while keeping prices white.
- Replace "coins (each)" with the stack total alongside the unit price.
- Format totals with thousands separators and up to one decimal place.
## 1.22.0

- Add Bazaar Item Prices under Interface > Items.
- Show per-item Insta Buy above Insta Sell using the current best orders.
- Match Bazaar products, single-enchantment books, legacy dye IDs, and shards; exclude soulbound items.
- Reuse the shared asynchronous Bazaar request with a one-minute refresh and expire tooltip quotes after three minutes.
## 1.21.3

- Allow Performance Stats, Zoom, and Chat Copy outside SkyBlock.
- Restrict Inventory Preview and pet tracking to SkyBlock.
- Keep SkyBlock-specific HUD clipping inactive outside SkyBlock.
- Clear pending item-protection input when leaving SkyBlock.
## 1.21.2

- Silence slot-lock toggle chat messages.
- Shorten item-protection toggle messages to "Item protected!" and "Item not protected!".
## 1.21.1

- Send drop-protection toggles and blocked-drop messages to chat using the slot-locking prefix.
- Throttle repeated blocked-drop messages.
- Mark protected items with a small cyan shield in their upper-right corner, including inventory, hotbar, and storage previews.
## 1.21.0

- Add item-based drop protection under Interface > Items, separate from slot locking.
- Press P while hovering an item to toggle protection; configure the key in settings.
- Protect UUID items individually and UUID-less items by type, with a tooltip indicator.
- Allow inventory/storage movement while blocking drop-key and outside-inventory drops.
- Require placing a protected cursor stack before closing the screen to avoid implicit drops.
## 1.20.0

- Add a saved Purple / Black config-theme button beside the Skyveil title.
- Use readable charcoal panels and silver accents for the Black theme.
- Apply theme changes immediately without resetting search, accordion, or scroll state.
## 1.19.6

- Restrict storage-item tooltips and hover highlights to the visible scissor area.
- Reject live-storage hover and click hit tests outside the workspace viewport.
- Prevent clipped slots from showing tooltips through the player inventory.
## 1.19.5

- Defer container HUD submission until the current frame's tooltip layout is available.
- Clip all shared HUD modules against actual tooltip bounds, including tall and scrolled tooltips outside storage panels.
- Remove the opaque tooltip background workaround and retain normal tooltip styling.
- Clear queued HUD draws and tooltip bounds after each frame.
## 1.19.4

- Draw container tooltips on a separate layer with an opaque interior to block HUD text showing through.
- Use final tooltip bounds so the fix also follows oversized, scrolled item tooltips.
- Preserve tooltip frames, shadows, and HUD placement.
## 1.19.3

- Clip gameplay HUDs around the active Storage Preview workspace, inventory, and controls.
- Apply the shared rule to pet, stats, skill XP, performance, and inventory preview displays.
- Preserve HUD positions, resizing, and normal inventory dimming.
## 1.19.2

- Refactor pet metadata parsing into a structured reader and remove regex-based field extraction.
- Bound menu scan delays so continuous container packets cannot prevent caching.
- Keep selected pet snapshots atomic instead of merging visual details from another state.
- Preserve owned head textures and use bundled species textures when texture data is absent.
- Resolve held-item icons by ID or lore name without a second growing icon cache.
- Replace misleading unresolved "No Pet Item" output with a Pets-menu sync prompt.
- Rebuild the legacy pet cache using the corrected format.
## 1.19.1

- Remove dark squares around HUD item icons while retaining inventory dimming.
- Add inventory preview background opacity and Purple / Dark Grey controls.
- Read nested petInfo directly and distinguish pets by name when type metadata is missing.
## 1.19.0

## 1.23.0

- Add Auction House Item Prices under Interface > Items.
- Show Lowest BIN and stack total with colored labels and white numbers.
- Build an asynchronous price snapshot from complete, consistent Hypixel auction pages.
- Exclude Bazaar products, soulbound items, expired auctions, and non-BIN listings.
- Compare base item IDs, with pet species/rarity separation; upgrades and pet levels are not appraised.
## 1.22.1

- Color Bazaar tooltip labels while keeping prices white.
- Replace "coins (each)" with the stack total alongside the unit price.
- Format totals with thousands separators and up to one decimal place.
## 1.22.0

- Add Bazaar Item Prices under Interface > Items.
- Show per-item Insta Buy above Insta Sell using the current best orders.
- Match Bazaar products, single-enchantment books, legacy dye IDs, and shards; exclude soulbound items.
- Reuse the shared asynchronous Bazaar request with a one-minute refresh and expire tooltip quotes after three minutes.
## 1.21.3

- Allow Performance Stats, Zoom, and Chat Copy outside SkyBlock.
- Restrict Inventory Preview and pet tracking to SkyBlock.
- Keep SkyBlock-specific HUD clipping inactive outside SkyBlock.
- Clear pending item-protection input when leaving SkyBlock.
## 1.21.2

- Silence slot-lock toggle chat messages.
- Shorten item-protection toggle messages to "Item protected!" and "Item not protected!".
## 1.21.1

- Send drop-protection toggles and blocked-drop messages to chat using the slot-locking prefix.
- Throttle repeated blocked-drop messages.
- Mark protected items with a small cyan shield in their upper-right corner, including inventory, hotbar, and storage previews.
## 1.21.0

- Add item-based drop protection under Interface > Items, separate from slot locking.
- Press P while hovering an item to toggle protection; configure the key in settings.
- Protect UUID items individually and UUID-less items by type, with a tooltip indicator.
- Allow inventory/storage movement while blocking drop-key and outside-inventory drops.
- Require placing a protected cursor stack before closing the screen to avoid implicit drops.
## 1.20.0

- Add a saved Purple / Black config-theme button beside the Skyveil title.
- Use readable charcoal panels and silver accents for the Black theme.
- Apply theme changes immediately without resetting search, accordion, or scroll state.
## 1.19.6

- Restrict storage-item tooltips and hover highlights to the visible scissor area.
- Reject live-storage hover and click hit tests outside the workspace viewport.
- Prevent clipped slots from showing tooltips through the player inventory.
## 1.19.5

- Defer container HUD submission until the current frame's tooltip layout is available.
- Clip all shared HUD modules against actual tooltip bounds, including tall and scrolled tooltips outside storage panels.
- Remove the opaque tooltip background workaround and retain normal tooltip styling.
- Clear queued HUD draws and tooltip bounds after each frame.
## 1.19.4

- Draw container tooltips on a separate layer with an opaque interior to block HUD text showing through.
- Use final tooltip bounds so the fix also follows oversized, scrolled item tooltips.
- Preserve tooltip frames, shadows, and HUD placement.
## 1.19.3

- Clip gameplay HUDs around the active Storage Preview workspace, inventory, and controls.
- Apply the shared rule to pet, stats, skill XP, performance, and inventory preview displays.
- Preserve HUD positions, resizing, and normal inventory dimming.
## 1.19.2

- Refactor pet metadata parsing into a structured reader and remove regex-based field extraction.
- Bound menu scan delays so continuous container packets cannot prevent caching.
- Keep selected pet snapshots atomic instead of merging visual details from another state.
- Preserve owned head textures and use bundled species textures when texture data is absent.
- Resolve held-item icons by ID or lore name without a second growing icon cache.
- Replace misleading unresolved "No Pet Item" output with a Pets-menu sync prompt.
- Rebuild the legacy pet cache using the corrected format.
## 1.19.1

- Remove dark squares around HUD item icons while retaining inventory dimming.
- Add inventory preview background opacity and Purple / Dark Grey controls.
- Read nested petInfo directly and distinguish pets by name when type metadata is missing.
## 1.19.0

- Add a live 27-slot inventory preview with a translucent purple background above the stat displays.
- Show stack counts and durability using the current inventory contents.
- Integrate movement, smooth resizing, and alignment into the existing HUD Layout editor.
- Add the inventory preview toggle under Interface > Screen Overlays.
## 1.18.2

## 1.23.0

- Add Auction House Item Prices under Interface > Items.
- Show Lowest BIN and stack total with colored labels and white numbers.
- Build an asynchronous price snapshot from complete, consistent Hypixel auction pages.
- Exclude Bazaar products, soulbound items, expired auctions, and non-BIN listings.
- Compare base item IDs, with pet species/rarity separation; upgrades and pet levels are not appraised.
## 1.22.1

- Color Bazaar tooltip labels while keeping prices white.
- Replace "coins (each)" with the stack total alongside the unit price.
- Format totals with thousands separators and up to one decimal place.
## 1.22.0

- Add Bazaar Item Prices under Interface > Items.
- Show per-item Insta Buy above Insta Sell using the current best orders.
- Match Bazaar products, single-enchantment books, legacy dye IDs, and shards; exclude soulbound items.
- Reuse the shared asynchronous Bazaar request with a one-minute refresh and expire tooltip quotes after three minutes.
## 1.21.3

- Allow Performance Stats, Zoom, and Chat Copy outside SkyBlock.
- Restrict Inventory Preview and pet tracking to SkyBlock.
- Keep SkyBlock-specific HUD clipping inactive outside SkyBlock.
- Clear pending item-protection input when leaving SkyBlock.
## 1.21.2

- Silence slot-lock toggle chat messages.
- Shorten item-protection toggle messages to "Item protected!" and "Item not protected!".
## 1.21.1

- Send drop-protection toggles and blocked-drop messages to chat using the slot-locking prefix.
- Throttle repeated blocked-drop messages.
- Mark protected items with a small cyan shield in their upper-right corner, including inventory, hotbar, and storage previews.
## 1.21.0

- Add item-based drop protection under Interface > Items, separate from slot locking.
- Press P while hovering an item to toggle protection; configure the key in settings.
- Protect UUID items individually and UUID-less items by type, with a tooltip indicator.
- Allow inventory/storage movement while blocking drop-key and outside-inventory drops.
- Require placing a protected cursor stack before closing the screen to avoid implicit drops.
## 1.20.0

- Add a saved Purple / Black config-theme button beside the Skyveil title.
- Use readable charcoal panels and silver accents for the Black theme.
- Apply theme changes immediately without resetting search, accordion, or scroll state.
## 1.19.6

- Restrict storage-item tooltips and hover highlights to the visible scissor area.
- Reject live-storage hover and click hit tests outside the workspace viewport.
- Prevent clipped slots from showing tooltips through the player inventory.
## 1.19.5

- Defer container HUD submission until the current frame's tooltip layout is available.
- Clip all shared HUD modules against actual tooltip bounds, including tall and scrolled tooltips outside storage panels.
- Remove the opaque tooltip background workaround and retain normal tooltip styling.
- Clear queued HUD draws and tooltip bounds after each frame.
## 1.19.4

- Draw container tooltips on a separate layer with an opaque interior to block HUD text showing through.
- Use final tooltip bounds so the fix also follows oversized, scrolled item tooltips.
- Preserve tooltip frames, shadows, and HUD placement.
## 1.19.3

- Clip gameplay HUDs around the active Storage Preview workspace, inventory, and controls.
- Apply the shared rule to pet, stats, skill XP, performance, and inventory preview displays.
- Preserve HUD positions, resizing, and normal inventory dimming.
## 1.19.2

- Refactor pet metadata parsing into a structured reader and remove regex-based field extraction.
- Bound menu scan delays so continuous container packets cannot prevent caching.
- Keep selected pet snapshots atomic instead of merging visual details from another state.
- Preserve owned head textures and use bundled species textures when texture data is absent.
- Resolve held-item icons by ID or lore name without a second growing icon cache.
- Replace misleading unresolved "No Pet Item" output with a Pets-menu sync prompt.
- Rebuild the legacy pet cache using the corrected format.
## 1.19.1

- Remove dark squares around HUD item icons while retaining inventory dimming.
- Add inventory preview background opacity and Purple / Dark Grey controls.
- Read nested petInfo directly and distinguish pets by name when type metadata is missing.
## 1.19.0

- Add a live 27-slot inventory preview with a translucent purple background above the stat displays.
- Show stack counts and durability using the current inventory contents.
- Integrate movement, smooth resizing, and alignment into the existing HUD Layout editor.
- Add the inventory preview toggle under Interface > Screen Overlays.
## 1.18.2

- Reuse menu-cached pet heads and held items when loadouts or Autopet select pets that have levelled.
- Keep live XP and selection updates out of the persistent pet cache.
- Keep UUID-less identities stable across held-item changes and update changed menu details in place.
- Remove stale entries after all pages of the normal Pets menu have been scanned.
## 1.18.1

## 1.23.0

- Add Auction House Item Prices under Interface > Items.
- Show Lowest BIN and stack total with colored labels and white numbers.
- Build an asynchronous price snapshot from complete, consistent Hypixel auction pages.
- Exclude Bazaar products, soulbound items, expired auctions, and non-BIN listings.
- Compare base item IDs, with pet species/rarity separation; upgrades and pet levels are not appraised.
## 1.22.1

- Color Bazaar tooltip labels while keeping prices white.
- Replace "coins (each)" with the stack total alongside the unit price.
- Format totals with thousands separators and up to one decimal place.
## 1.22.0

- Add Bazaar Item Prices under Interface > Items.
- Show per-item Insta Buy above Insta Sell using the current best orders.
- Match Bazaar products, single-enchantment books, legacy dye IDs, and shards; exclude soulbound items.
- Reuse the shared asynchronous Bazaar request with a one-minute refresh and expire tooltip quotes after three minutes.
## 1.21.3

- Allow Performance Stats, Zoom, and Chat Copy outside SkyBlock.
- Restrict Inventory Preview and pet tracking to SkyBlock.
- Keep SkyBlock-specific HUD clipping inactive outside SkyBlock.
- Clear pending item-protection input when leaving SkyBlock.
## 1.21.2

- Silence slot-lock toggle chat messages.
- Shorten item-protection toggle messages to "Item protected!" and "Item not protected!".
## 1.21.1

- Send drop-protection toggles and blocked-drop messages to chat using the slot-locking prefix.
- Throttle repeated blocked-drop messages.
- Mark protected items with a small cyan shield in their upper-right corner, including inventory, hotbar, and storage previews.
## 1.21.0

- Add item-based drop protection under Interface > Items, separate from slot locking.
- Press P while hovering an item to toggle protection; configure the key in settings.
- Protect UUID items individually and UUID-less items by type, with a tooltip indicator.
- Allow inventory/storage movement while blocking drop-key and outside-inventory drops.
- Require placing a protected cursor stack before closing the screen to avoid implicit drops.
## 1.20.0

- Add a saved Purple / Black config-theme button beside the Skyveil title.
- Use readable charcoal panels and silver accents for the Black theme.
- Apply theme changes immediately without resetting search, accordion, or scroll state.
## 1.19.6

- Restrict storage-item tooltips and hover highlights to the visible scissor area.
- Reject live-storage hover and click hit tests outside the workspace viewport.
- Prevent clipped slots from showing tooltips through the player inventory.
## 1.19.5

- Defer container HUD submission until the current frame's tooltip layout is available.
- Clip all shared HUD modules against actual tooltip bounds, including tall and scrolled tooltips outside storage panels.
- Remove the opaque tooltip background workaround and retain normal tooltip styling.
- Clear queued HUD draws and tooltip bounds after each frame.
## 1.19.4

- Draw container tooltips on a separate layer with an opaque interior to block HUD text showing through.
- Use final tooltip bounds so the fix also follows oversized, scrolled item tooltips.
- Preserve tooltip frames, shadows, and HUD placement.
## 1.19.3

- Clip gameplay HUDs around the active Storage Preview workspace, inventory, and controls.
- Apply the shared rule to pet, stats, skill XP, performance, and inventory preview displays.
- Preserve HUD positions, resizing, and normal inventory dimming.
## 1.19.2

- Refactor pet metadata parsing into a structured reader and remove regex-based field extraction.
- Bound menu scan delays so continuous container packets cannot prevent caching.
- Keep selected pet snapshots atomic instead of merging visual details from another state.
- Preserve owned head textures and use bundled species textures when texture data is absent.
- Resolve held-item icons by ID or lore name without a second growing icon cache.
- Replace misleading unresolved "No Pet Item" output with a Pets-menu sync prompt.
- Rebuild the legacy pet cache using the corrected format.
## 1.19.1

- Remove dark squares around HUD item icons while retaining inventory dimming.
- Add inventory preview background opacity and Purple / Dark Grey controls.
- Read nested petInfo directly and distinguish pets by name when type metadata is missing.
## 1.19.0

- Add a live 27-slot inventory preview with a translucent purple background above the stat displays.
- Show stack counts and durability using the current inventory contents.
- Integrate movement, smooth resizing, and alignment into the existing HUD Layout editor.
- Add the inventory preview toggle under Interface > Screen Overlays.
## 1.18.2

- Reuse menu-cached pet heads and held items when loadouts or Autopet select pets that have levelled.
- Keep live XP and selection updates out of the persistent pet cache.
- Keep UUID-less identities stable across held-item changes and update changed menu details in place.
- Remove stale entries after all pages of the normal Pets menu have been scanned.
## 1.18.1

- Keep top-level categories in the sidebar and show only the selected category's module accordions.
- Rename Chat Copy to Chat and move the Bestiary toggle into it.
- Refine settings spacing, section headers, selection styling, card alignment, and scrollbar clearance.
## 1.18.0

## 1.23.0

- Add Auction House Item Prices under Interface > Items.
- Show Lowest BIN and stack total with colored labels and white numbers.
- Build an asynchronous price snapshot from complete, consistent Hypixel auction pages.
- Exclude Bazaar products, soulbound items, expired auctions, and non-BIN listings.
- Compare base item IDs, with pet species/rarity separation; upgrades and pet levels are not appraised.
## 1.22.1

- Color Bazaar tooltip labels while keeping prices white.
- Replace "coins (each)" with the stack total alongside the unit price.
- Format totals with thousands separators and up to one decimal place.
## 1.22.0

- Add Bazaar Item Prices under Interface > Items.
- Show per-item Insta Buy above Insta Sell using the current best orders.
- Match Bazaar products, single-enchantment books, legacy dye IDs, and shards; exclude soulbound items.
- Reuse the shared asynchronous Bazaar request with a one-minute refresh and expire tooltip quotes after three minutes.
## 1.21.3

- Allow Performance Stats, Zoom, and Chat Copy outside SkyBlock.
- Restrict Inventory Preview and pet tracking to SkyBlock.
- Keep SkyBlock-specific HUD clipping inactive outside SkyBlock.
- Clear pending item-protection input when leaving SkyBlock.
## 1.21.2

- Silence slot-lock toggle chat messages.
- Shorten item-protection toggle messages to "Item protected!" and "Item not protected!".
## 1.21.1

- Send drop-protection toggles and blocked-drop messages to chat using the slot-locking prefix.
- Throttle repeated blocked-drop messages.
- Mark protected items with a small cyan shield in their upper-right corner, including inventory, hotbar, and storage previews.
## 1.21.0

- Add item-based drop protection under Interface > Items, separate from slot locking.
- Press P while hovering an item to toggle protection; configure the key in settings.
- Protect UUID items individually and UUID-less items by type, with a tooltip indicator.
- Allow inventory/storage movement while blocking drop-key and outside-inventory drops.
- Require placing a protected cursor stack before closing the screen to avoid implicit drops.
## 1.20.0

- Add a saved Purple / Black config-theme button beside the Skyveil title.
- Use readable charcoal panels and silver accents for the Black theme.
- Apply theme changes immediately without resetting search, accordion, or scroll state.
## 1.19.6

- Restrict storage-item tooltips and hover highlights to the visible scissor area.
- Reject live-storage hover and click hit tests outside the workspace viewport.
- Prevent clipped slots from showing tooltips through the player inventory.
## 1.19.5

- Defer container HUD submission until the current frame's tooltip layout is available.
- Clip all shared HUD modules against actual tooltip bounds, including tall and scrolled tooltips outside storage panels.
- Remove the opaque tooltip background workaround and retain normal tooltip styling.
- Clear queued HUD draws and tooltip bounds after each frame.
## 1.19.4

- Draw container tooltips on a separate layer with an opaque interior to block HUD text showing through.
- Use final tooltip bounds so the fix also follows oversized, scrolled item tooltips.
- Preserve tooltip frames, shadows, and HUD placement.
## 1.19.3

- Clip gameplay HUDs around the active Storage Preview workspace, inventory, and controls.
- Apply the shared rule to pet, stats, skill XP, performance, and inventory preview displays.
- Preserve HUD positions, resizing, and normal inventory dimming.
## 1.19.2

- Refactor pet metadata parsing into a structured reader and remove regex-based field extraction.
- Bound menu scan delays so continuous container packets cannot prevent caching.
- Keep selected pet snapshots atomic instead of merging visual details from another state.
- Preserve owned head textures and use bundled species textures when texture data is absent.
- Resolve held-item icons by ID or lore name without a second growing icon cache.
- Replace misleading unresolved "No Pet Item" output with a Pets-menu sync prompt.
- Rebuild the legacy pet cache using the corrected format.
## 1.19.1

- Remove dark squares around HUD item icons while retaining inventory dimming.
- Add inventory preview background opacity and Purple / Dark Grey controls.
- Read nested petInfo directly and distinguish pets by name when type metadata is missing.
## 1.19.0

- Add a live 27-slot inventory preview with a translucent purple background above the stat displays.
- Show stack counts and durability using the current inventory contents.
- Integrate movement, smooth resizing, and alignment into the existing HUD Layout editor.
- Add the inventory preview toggle under Interface > Screen Overlays.
## 1.18.2

- Reuse menu-cached pet heads and held items when loadouts or Autopet select pets that have levelled.
- Keep live XP and selection updates out of the persistent pet cache.
- Keep UUID-less identities stable across held-item changes and update changed menu details in place.
- Remove stale entries after all pages of the normal Pets menu have been scanned.
## 1.18.1

- Keep top-level categories in the sidebar and show only the selected category's module accordions.
- Rename Chat Copy to Chat and move the Bestiary toggle into it.
- Refine settings spacing, section headers, selection styling, card alignment, and scrollbar clearance.
## 1.18.0

- Organize Interface settings into Screen Overlays and Items, with Bestiary available as a direct toggle.
- Expand categories and groups inline as accordions; show single-setting modules directly.
- Remove duplicate HUD layout and performance scale controls; use the shared HUD Layout editor.
- Remove the skill XP inventory dimming option while retaining automatic dimming.
- Search results expand the matching category and group and highlight the setting.
## 1.17.6

- Fit stat icons and numbers using their visible glyph bounds, including font bearings and shadows.
- Tighten the icon/value row above resource bars with a one-pixel gap.
- Match HUD editor bounds to the compact layout, including shorter Defense and Speed displays.

## 1.17.5

- Fix skill XP remaining in the default action-bar position over player stat displays.
- Skill XP now uses a separate movable and resizable HUD with XP gained and level progress.
- Remove the duplicate XP message while preserving other action-bar text.
- Configure the module under Interface > Skill XP; disabling it restores the default display.

## 1.17.4

- Dim default SkyBlock skill XP messages and switched-item names while inventory or container screens are open.
- Added Dim Skill XP In Inventory and Show Switched Item Name controls under Interface > Vanilla HUD.
- Preserve styled text colors and vanilla fade timing without changing inventory buttons or tooltips.

## 1.17.3

- Keep gameplay HUDs visible at reduced brightness while inventory or container screens are open.
- HUD displays remain passive in inventory; moving and resizing is only available in the HUD editor.
- Inventory buttons keep their existing appearance and interactions.

## 1.17.2

- Hide Player Stats, Pet Display, and Performance HUDs while inventory or container screens are open.
- Continue updating HUD data while hidden and preserve Relocate / Resize previews.
- Inventory buttons and inventory-specific tools retain their existing behavior.

## 1.17.1

- HUD resizing is continuous; alignment guides no longer snap displays to specific sizes.
- Scroll resizing uses fine proportional adjustments and all HUDs can shrink to 25%.
- Stat icons and values fit within separate padded areas and scale together.
- Resize handles sit outside HUD content, and fractional scaling no longer shifts Pet or Performance text away from the editor bounds.

## 1.17.0

- Added visible corner resize handles for every HUD display in Relocate / Resize.
- Added optional alignment snapping with cyan guides for edges, centers, and screen boundaries.
- Resizing can snap to neighboring edges while preserving the display's proportions.
- The HUD editor shows selection coordinates and scale, saves alignment preferences, and retains scroll resizing.

## 1.16.1

- Increased player resource bars from 2 to 8 pixels with a dark border and highlighted fill.
- Replaced stat name labels with enlarged SkyBlock resource-pack icons beside the values.
- Improved Vitality contrast and preserved existing HUD positions and scales.

## 1.16.0

- Added separate Health, Defense, Mana, Vitality, and Speed displays under Interface > Player Stats.
- Every stat has an independently saved position and scale in HUD Layout / Relocate / Resize.
- Health, Mana, and Vitality include resource bars; Health shows absorption and Mana retains overflow values.
- While enabled in SkyBlock, Player Stats hides vanilla hearts and hunger and removes replaced action-bar stats while preserving other messages.
- Supports both legacy stat symbols and current Hypixel resource-pack glyphs.

## 1.15.1

- Performance Display now measures ping/pong round trips over the active connection instead of trusting the server-supplied TAB latency.
- Missing or stale ping replies display -- rather than a misleading latency value.

## 1.15.0

- Added a compact FPS, color-coded ping, and estimated server TPS display.
- Performance Display can be toggled in Interface settings and moved or resized in HUD Layout.
- Fixed pet equip notifications being missed and stale TAB data reverting pet selections.

## 1.14.9

- Fixed excluded secondary damage numbers remaining visible underneath the Compact Damage display.
- Compact Damage now hides every recognized original damage splash while enabled.
- Disabled sources are hidden and ignored, while enabled sources are hidden and merged into the latest melee sample.

## 1.14.8

- Compact Damage now builds its five-hit average from actual melee attacks instead of every damage tick near the target.
- Venomous, poison, Fire Aspect, burning, Thunderlord, Thunderbolt, Crimson Swipe, Ferocity, pet, and other secondary damage are excluded by default.
- Added an independent opt-in toggle for every supported secondary damage category under Compact Damage.
- Enabled secondary damage is added to its triggering melee sample and never consumes one of the five melee-hit slots.
- Secondary damage labels remain visible normally when their category is excluded from Compact Damage.

## 1.14.7

- Removed the Void Compact Damage style.
- Compact Damage now offers only Minimal, Neon, and Crimson styles.
- Existing Void selections migrate automatically to Minimal.

## 1.14.6

- Fixed the TAB parser continuing past the active `Pet:` widget into the separate `Pet Training:` widget.
- Pet Display now reads only the first pet inside the active `Pet:` section and stops at `Pet Training:`.

## 1.14.5

- Removed the Chat Copy Bindings management screen completely.
- Chat Copy now has exactly one keybind control directly inside its normal settings page, matching Item Protection's layout.
- The single control still records complete held-key and mouse chords such as Ctrl + C + Left Click.
- Existing multi-binding configurations migrate safely by retaining the first saved binding.

## 1.14.4

- Replaced Chat Copy's separate binding editor with inline keybind buttons in the binding list.
- Adding a binding now creates it immediately; click its keybind button and press the desired held-key plus mouse chord.
- Multi-input chords such as Ctrl + C + Left Click remain fully supported.

## 1.14.3

- Fixed pets placed in Fann's Pet Training being mistaken for the active summoned pet in Pet Display.
- Training pets remain cached for their head, level, rarity, and held-item visuals, but are excluded from every active-pet selection path.

## 1.14.2

- Replaced Chat Copy's separate key, modifier, and mouse controls with one full-chord recorder.
- Hold any number of keyboard keys and click a mouse button while recording; every captured input is then required to copy a hovered message.
- Existing Chat Copy bindings migrate automatically to the new chord format.

## 1.14.1

- Chat Copy now guarantees plain-text clipboard output by removing embedded Minecraft formatting and color codes.
- Normal message text and Unicode symbols remain unchanged.

## 1.14.0

- Added Chat Copy under Interface with support for any number of configurable bindings.
- Each binding supports a mouse button, optional held keyboard key, and optional Ctrl, Shift, or Alt modifiers.
- Hovering any wrapped chat line and activating a binding copies its complete original message to the clipboard.

## 1.13.36

- Restored rarity backgrounds for genuine Pet Menu entries whose visible lore omits a parseable rarity footer.
- Pet Menu backgrounds now use the same structured Hypixel pet-tier metadata already validated by the pet tracker, while non-pet GUI controls remain unaffected.

## 1.13.35

- Fixed the real multi-page Attribute filter cache reset caused by Minecraft removing and replacing the container screen on every Hypixel page change.
- Screen replacement now preserves the active filter and all observed pages; the cache is cleared only after the client confirms that the Attribute Menu was actually left.

## 1.13.34

- Fixed filtered Attribute pages still being discarded during Hypixel's temporary page-loading state.
- The active filter is now changed or cleared only from an explicitly selected filter tooltip, preserving every manually scanned page for combined price sorting.

## 1.13.33

- Fixed filtered Attribute Menu pages being discarded when Hypixel replaces the screen during manual page changes.
- Every manually visited page in the active filter is now retained and merged before price sorting, so the cheapest shards are ordered across the complete observed filter.
- Filtered-page data is cleared only after leaving the Attribute Menu or selecting a different filter.

## 1.13.32

- Removed automatic Attribute Menu page navigation that repeatedly took control of the menu.
- Attribute filtering now observes only pages the player opens, preserving normal clicks and menu interaction.

## 1.13.31

- Restored item-rarity backgrounds for real pet entries in the Pets Menu and pet results in Auctions.
- The visible, correctly colored tooltip rarity footer is now authoritative even when a real item carries Hypixel GUI metadata.
- Attribute filters now automatically traverse every page in the selected category before presenting the complete price-sorted shard list.
- Filter scanning can move forward or backward to collect missed pages and stops once every filtered page has been observed.

## 1.13.30

- Fixed Attribute Progress ignoring the Attribute Menu's active category filter.
- The panel now detects Hypixel's selected filter, resets its filtered page set when the category changes, and displays only shards observed on that filter's pages.
- Filtered pages accumulate while browsing them, while the persisted all-attribute collection and global completion counters remain intact.

## 1.13.29

- Pet Display held-item icons now resolve from the same modeled SkyBlock item catalog used by Item Search.
- Existing cached pets are enriched at render time, so items such as Lucky Clover immediately use their correct built-in SkyBlock model without requiring another `/pets` scan.
- Retained the previous cached icon and safe vanilla mappings as fallbacks when a catalog entry is unavailable.

## 1.13.28

- Removed the Cosmic and Runic Pet Display styles and their bundled frame textures.
- Simplified Pet Display style selection to Panel and Minimal.
- Existing Cosmic, Runic, Neon, or Glass selections now migrate safely to Panel.

## 1.13.27

- Expanded the Cosmic and Runic Pet Display safe area so the complete pet HUD remains inside the decorative frame.
- Moved the pet icon, name, level, XP text, progress bar, and pet-item row away from the ornate borders.
- Raised the pet-item row above the lower frame and shortened the progress bar to respect the inner right edge.

## 1.13.26

- Reworked the Cosmic Pet Display frame with a sharper astral double border, restrained constellations, and gold star accents while preserving the readable content area.
- Reworked the Runic Pet Display frame with angular amethyst-and-gold linework, segmented rune details, and compact centered sigils.
- Added a central SkyBlock session gate that requires both a Hypixel server address and a live `SKYBLOCK` sidebar before gameplay features activate.
- Disabled Skyveil HUDs, overlays, GUI replacements, menu tools, combat processing, chat filters, item protection, and Zoom outside Hypixel SkyBlock.
- Kept the Skyveil configuration and update commands available outside SkyBlock so the mod can still be configured and maintained.

## 1.13.25

- Fixed the Autopet chat filter so level-less, formatted, spaced, and rule-number notification variants are hidden when enabled.
- Added an in-place Pet Display preview to the Display Style settings card; cycling the option updates the real HUD preview immediately.
- Removed the Neon and Glass Pet Display styles.
- Added clean Cosmic and Runic styles with thin full-panel pixel-art borders, translucent backgrounds, safe content padding, and aligned XP progress bars.
- Migrated existing Neon selections to Cosmic and Glass selections to Runic.
- Kept Panel and Minimal available and preserved the account-aware pet texture and attached-item cache.

## 1.13.24

- Reworked the Skyveil configuration menu into categorized feature dashboards with focused, expandable subcategory pages and clearer navigation.
- Moved Zoom into Interface, made HUD Layout a direct action, added four Compact Damage styles, and fixed five-hit averages resetting too quickly for slower attacks.
- Removed the complete Map feature, including the minimap, large map, map keybind, NPC and custom markers, settings, bundled maps, and developer generator.
- Consolidated Storage Preview, equipment, Attribute Progress, and shard prices into one validated, account-aware runtime cache.
- Loads cache data once at startup, keeps changes in memory, and atomically replaces the single cache file during normal shutdown.
- Migrates and removes confirmed legacy cache files after a successful save; malformed, outdated, and oversized data is safely rejected.
- Removed disk access and serialization from menu rendering and live container updates.
- Made equipment scans packet-driven and prepared Attribute and Hunting Box price rows only when source data changes.
- Batched Storage Preview slot grids and balanced every modified pose and scissor state with guaranteed cleanup.
- Removed unsafe cached Minecraft item-render states so animated and context-dependent models remain correct across reloads and GUI changes.
- Fixed Storage Preview Shift-clicking so compatible partial stacks are filled before remaining items move into empty storage slots.
- Fixed false item-rarity backgrounds by requiring an explicit rarity footer in the visible tooltip instead of reading hidden metadata or descriptive text.
- Restored the correct rarity-colored backgrounds for recombobulated items with decorated tooltip rarity lines.
- Rarity parsing now ignores visual glyphs, letters, numbers, and item-type text surrounding the correctly colored rarity word.

## 1.13.23

- Removed synchronous Storage Preview cache serialization from live item updates; previews now flush safely when the storage UI closes or Minecraft shuts down.
- Cached pet-level tooltip parsing, item-rarity backgrounds, decorative-item detection results, and lore-search matches for unchanged menu stacks.
- Removed repeated title parsing from every Pets Menu and Auction House slot render.
- Disabled expensive Attribute Menu diagnostic-string construction unless debug mode is enabled.
- Reused immutable Storage Preview color palettes and skipped rarity analysis entirely while rarity backgrounds are disabled.

## 1.13.22

- Optimized Storage Preview by caching item rarity and menu-item analysis instead of repeating NBT parsing every frame.
- Changed active storage synchronization from continuous full-slot fingerprinting to server container-update events.
- Skipped unnecessary item-decoration extraction for single, undamaged stored items.

## 1.13.21

- Added bottom-right pet-level overlays to pet listings throughout the Auction House.
- Kept auction controls, non-pet listings, and player inventory items free of level overlays.

## 1.13.20

- Added pet-level overlays to pet icons in the Pets Menu.
- Positioned each level compactly in the bottom-right corner without affecting navigation controls or player inventory items.

## 1.13.19

- Disabled Inventory Button rendering and click handling while the Storage Preview workspace is active.
- Prevented invisible Inventory Buttons from running commands behind Storage, Ender Chest, and Backpack previews.

## 1.13.18

- Included Crimson Armor Swipe damage in Compact Damage calculations.
- Added support for new Hypixel damage-splash glyphs without accepting mob health, names, or progress labels as damage.

## 1.13.17

- Enabled Inventory Buttons across SkyBlock container menus by default.
- Migrated existing installations so saved buttons remain visible in Auction House, Bazaar, Accessory Bag, Storage, and similar menus.
- Preserved the dedicated uncluttered layout while the full Storage Preview workspace is active.

## 1.13.16

- Prevented Compact Damage from selecting other players as damage-label targets.
- Merged connected multipart mob hitboxes into one compact damage batch.
- Anchored multipart damage above the named or primary hitbox instead of rendering a separate label for every body segment.

## 1.13.15

- Added recipe opening when clicking craftable items in Item Search results.
- Added upstream recipe metadata to the bundled SkyBlock item catalog so non-craftable results do not send recipe commands.
- Changed Attribute Menu shard hover text from the generated head name to "Click to search".

## 1.13.14

- Synchronized Inventory Button backgrounds, borders, and hover colors with the selected Dark Mode color.
- Synchronized the inventory equipment showcase slots with Default, Dark, and Dark Purple GUI colors.
- Preserved item rarity backgrounds above the themed equipment slot color.

## 1.13.13

- Fixed previously opened Backpack previews requiring another scan after relaunching Minecraft.
- Added a final synchronous Storage Preview cache flush when the client closes.
- Made storage persistence skip an unreadable item instead of discarding the entire Ender Chest and Backpack cache update.

## 1.13.12

- Hidden the Pet Display HUD while the Storage Preview workspace is active.
- Added SkyBlock item rarity backgrounds to the four equipment showcase slots in the inventory.

## 1.13.11

- Persisted the last confirmed equipment loadout shown in the inventory showcase.
- Restored the equipped necklace, cloak, belt, and gloves or bracelet after relaunching Minecraft.
- Stored equipment showcase data separately for each Minecraft account.

## 1.13.10

- Moved the Storage Preview control strip beyond the Inventory Buttons column while Storage Preview is disabled.
- Restored the control strip to its fused inventory-edge position whenever Storage Preview is enabled.
- Added SkyBlock item rarity backgrounds to cached and active Storage Preview slots.

## 1.13.9

- Added a compact two-button control strip fused to the right side of Minecraft's inventory in Storage menus.
- Added an ON/OFF button that immediately enables or disables Storage Preview and remains available while disabled.
- Added a color button beneath it that cycles Default, Dark, and Dark Purple themes.
- Made the Storage Preview color independent from the global Dark Mode setting.
- Added the same Storage Preview Color choice under General > Storage Preview.
- Persisted both the preview toggle and selected storage color between game sessions.

## 1.13.8

- Persisted the complete owned Ender Chest and Backpack index between Minecraft sessions.
- Persisted whether every storage page has already been opened and scanned.
- Restored cached item contents, stack counts, components, custom models, and tooltips after relaunching the game.
- Reconstructed the new storage-status index from existing item caches for backward compatibility.
- Protected a previously saved owned-page index from being erased while Hypixel's Storage menu is still loading.

## 1.13.7

- Turned Storage Preview into one continuous scrollable grid above the fixed inventory.
- Clipped rows at the storage viewport boundary instead of arranging them around the inventory.
- Lowered Minecraft's original inventory GUI to sit just above the SkyBlock health, armor, mana, and hotbar HUD.
- Preserved and restored the cursor position when switching Ender Chest or Backpack pages.
- Preserved the storage scroll position across page switches.
- Made preview panels fully opaque so HUD and mod overlays behind them no longer show through.
- Prevented clipped storage cells from remaining clickable outside the visible viewport.

## 1.13.6

- Rebuilt Storage Preview as a numeric row-major grid: Ender Chest pages 1-9 first, followed by Backpack slots.
- Filled the complete area above the inventory instead of reserving an unnecessary full-height center gap.
- Reserved only the actual Minecraft inventory rectangle when arranging lower preview rows.
- Moved preview rendering below Minecraft's cursor-stack layer, fixing carried items disappearing until the next slot click.

## 1.13.5

- Replaced simulated preview clicks with Minecraft's native container slot handling, following NEU's remapped-slot approach.
- Fixed preview clicks being treated as outside-window drops.
- Fixed carried items visually remaining in their previous slot while moving them between storage and inventory.
- Restored native dragging, right-click splitting, double-click collection, and Shift-click movement for the active preview.
- Switched directly between pages with Hypixel's Ender Chest and Backpack commands.
- Replaced the custom inventory panel with Minecraft's original container inventory GUI and texture.

## 1.13.4

- Removed the duplicate normal storage window from the Storage Preview workspace.
- Made the selected preview card the live storage interface, including every usable empty slot.
- Added direct left-click, right-click, double-click, and Shift-click handling to live preview slots for moving, taking, and adding items.
- Kept only the real player inventory panel visible in the center of the workspace.
- Changed the selected storage outline to white so the active Ender Chest or Backpack is unmistakable.

## 1.13.3

- Kept Storage Preview visible while an Ender Chest or Backpack page is open.
- Preserved the live storage container and player inventory so items can be moved normally while previews remain visible.
- Positioned preview cards only beside the live container, preventing them from covering storage or inventory slots.
- Added a double accent outline to the currently open storage page.
- Changed unopened page text to "Open storage to display the preview".
- Made preview cards navigate between storage pages through Hypixel's Storage menu while retaining the preview workspace.
- Stopped preview clicks from blocking normal live-container interactions.

## 1.13.2

- Made the Storage overview background transparent so the game remains visible between panels.
- Removed the bottom page launcher and player-inventory footer from Storage Preview.
- Added unopened owned Ender Chest and Backpack cards directly beside cached pages; clicking one opens it for the first scan.
- Excluded locked and unowned Ender Chest or Backpack slots from the overview.
- Collapsed cached cards to their last occupied item row and added a compact scrollbar when needed.

## 1.13.1

- Replaced the small Storage hover popup with a full-screen, scrollable multi-column overview inspired by NEU's Storage interface.
- Displayed all cached Ender Chest and Backpack pages together with the live player inventory and Storage page buttons.
- Made cached page cards and page buttons open the corresponding real Hypixel storage page for refreshing.

## 1.13.0

- Added Storage Preview under General settings.
- Added large hover previews for previously opened Ender Chest and Backpack pages in Hypixel's Storage menu.
- Persisted complete storage item snapshots between game sessions, including custom item components, models, heads, stack counts, and tooltips.
- Storage snapshots update only while their real page is open and its contents change.
- Matched Storage Preview colors to the selected Default, Dark, or Dark Purple GUI mode.

## 1.12.4

- Made Attribute Progress shard rows clickable, opening a focused Hypixel Bazaar search for the selected shard.
- Added a highlighted border to the shard row currently under the mouse.

## 1.12.3

- Added an opaque backing around the complete equipment shortcut column so the vanilla offhand frame cannot remain visible around the fourth slot.

## 1.12.2

- Fixed the inventory equipment display for SkyBlock 0.26 Equipment Sets by following the active lime selector and reading its four-slot column.
- Added support for numbered empty-slot placeholders such as `Slot 1 Necklace` without displaying the placeholder as equipment.

## 1.12.1

- Fixed Equipment Wardrobe detection so equipped items populate the inventory shortcut row from names, internal IDs, and current tooltip formats.
- Preserved detected equipment through temporary wardrobe page refreshes.
- Removed the unusable SkyBlock offhand slot from inventory rendering and interaction.

## 1.12.0

- Added a four-slot Equipment shortcut row inside the player inventory at the canonical offhand-column position.
- Added `/equipment` click handling and live caching of Necklace, Cloak, Belt, and Gloves/Bracelet stacks from Hypixel's Equipment menu.

## 1.11.10

- Merged NPC icons into the main SkyBlock Heads picker source and removed the separate NPC tab.
- Removed island cosmetics/furniture, dyes, accessories, and SkyBlock equipment-category heads, leaving 2,535 focused entries.

## 1.11.9

- Removed 596 cosmetic item-skin player heads from the Inventory Button icon catalog.
- Retained 3,402 focused non-skin SkyBlock heads, including all 520 NPC entries.

## 1.11.8

- Collapsed 719 minion-tier head rows to one highest-tier icon for each of the 61 minion types.
- Added a dedicated Inventory Button picker source for 520 genuine SkyBlock NPC player heads.

## 1.11.7

- Preserved active `lore:` searches across Auction House container and page refreshes.
- Collapsed 322 pet-rarity head rows to one highest-rarity icon for each of the 89 pets, reducing the picker to 4,656 focused entries.

## 1.11.6

- Added separate Vanilla Items and SkyBlock Heads sources to the Inventory Button icon picker.
- Added 4,889 searchable, persistent SkyBlock player-head icons backed by the bundled item catalog.

## 1.11.5

- Changed `lore:` to scan the current GUI's real item stacks and highlight matching inventory, container, and Auction House slots in green.
- Removed catalog result-panel output for lore searches.

## 1.11.4

- Added contiguous `lore:` phrase searches against normalized item tooltip text and green backgrounds for lore-matched results.

## 1.11.3

- Fixed Item Search model selection, tooltip italics, and the oversized opaque results background.
- Removed the Item Search enable/disable setting; the feature is always available.

## 1.11.2

- Moved player-inventory Item Search results onto `InventoryScreen`'s dedicated final render-state hook, producing a HUD-like overlay after its custom player/effect layers.
- Retained the generic final container hook for Attribute Menu, Hunting Box, chest, and other container screens without double-rendering player-inventory results.
- Added the exact `terminator` query as a catalog regression fixture.
- Normalized omitted Base64 padding in 147 repository head profiles so Minecraft 26.1's stricter skin decoder accepts every bundled player-head payload.

## 1.11.1

- Fixed result synchronization by polling the live search-field value as a fallback when Minecraft does not invoke the `EditBox` responder for typed characters.
- Prevented inventory, keybind, lock, and wardrobe actions from processing any non-Escape key while Item Search is focused; Escape retains vanilla screen-closing behavior.
- Made Item Search explicitly click-to-focus, blur and safely consume the first click outside the field, and reduced the field from 392×20 to 240×18 pixels.
- Added `Giant's Sword` as a punctuation-aware search regression fixture.

## 1.11.0

- Added Item Search to every inventory/container screen, with an empty-by-default search field positioned below the inventory and no result panel until text is entered.
- Added a scrollable right-side result grid matching SkyBlock names and internal identities across 8,746 bundled item definitions.
- Reconstructed genuine player-head profiles, Hypixel's Minecraft 26.1 built-in `ItemModel` identifiers, base items, display names, and complete hover lore for search results.
- Added an Item Search configuration toggle and bundled a reproducible catalog generator plus MIT attribution for the NotEnoughUpdates item repository data.

## 1.10.9

- Fixed live Attribute updates for named single-shard chat headers such as `You used Syphon on Cavernfish Shard!`.
- Kept support for bulk headers such as `You used Syphon on 52 Shards!` and added the Cavernfish/Cave Fishing sequence as a regression fixture.

## 1.10.8

- Added live parsing of Hypixel's `You used Syphon on ...` result block and its per-attribute level lines.
- Updated cached tiers, shards remaining, maxed state, owned quantities, and Bazaar totals immediately for every attribute reported by a syphon action.
- Required the exact server syphon header and a short result window so unrelated player, party, or guild chat cannot alter Attribute progress.

## 1.10.7

- Removed virtual shard UID suffixes such as `(R74)` from Attribute Menu source names.
- Persisted scanned Attribute Menu progress, page completion, and Hunting Box quantities per Minecraft account so the maxing HUD survives restarts without rescanning every page.
- Kept the cache synchronized with stable live Attribute Menu slot updates after syphoning or maxing and recalculated remaining purchases when Hunting Box quantities change.
- Matched Hunting Box values to the Attribute Menu's compact `unit ea • combined total` format on the shard-name row.

## 1.10.6

- Changed Attribute Menu rows from ability names to the actual shard source shown by the item's `Source:` tooltip field, with the maintained identity catalog as a fallback.
- Encoded rarity through the source-name text color and removed the redundant rarity line, reducing each row from three lines to two.
- Added a self-contained catalog of 320 unique source-specific player-head profiles so Attribute Menu paper, dye, block, and generic skull placeholders are never used as row icons.
- Kept the original Attribute Menu stack for hover tooltips while rendering the source-specific head separately.

## 1.10.5

- Fixed current paginated Attribute Menu entries not being detected when Hypixel omits the concrete shard subtype from item metadata.
- Added an exact 320-entry ability-name fallback catalog, excluding only the non-syphonable Chameleon attribute.
- Included partially upgraded attributes in the missing list and show their remaining shard quantity, live unit Bazaar price, and total acquisition cost to level X.
- Kept page totals visible even while a page is loading and restricted scanning to the real Attribute Menu grid so navigation and inventory items cannot become false entries.
- Verified calculations against the official cumulative shard requirements: Common 96, Uncommon 64, Rare 48, Epic 32, and Legendary 24, without expanding the existing compact panel layout.

## 1.10.4

- Connected Skyveil to the public `arkutaken/Skyveil` GitHub Releases feed for one asynchronous update check per launch.
- Added a one-time in-game chat notification for newer stable versions, including concise release notes and a clickable download action.
- Added `/sv update` and `/skyveil update` to check, download, verify, and stage the latest release without opening the mods folder.
- Required the GitHub SHA-256 digest, matching Skyveil mod ID/version metadata, an HTTPS repository asset URL, and bounded JAR size before installation.
- Replaced the installed JAR only after validation, retaining rollback behavior if either filesystem move fails; Minecraft still needs to restart to load the new version.

## 1.10.3

- Condensed every Hunting Box shard into one row without changing the surrounding panel.
- Placed the owned count beside the shard name and aligned the per-shard price plus combined value in a single right-hand price column.
- Kept exact comma-separated prices whenever they fit, with compact-number fallback for unusually large values or narrow layouts.

## 1.10.2

- Updated Skyveil for Minecraft 26.1.2, Fabric Loader 0.19.3, and Java 25.
- Replaced the SkyCofl dependency with Skyveil's own asynchronous shard-only Hypixel Bazaar pricing service and last-known-price cache.
- Restored live Hunting Box sell values and Attribute Menu acquisition estimates using the bundled shard identity catalog.
- Made Hunting Box entries compact with the combined value beside the shard name and a count-by-unit-price breakdown below it.
- Added a one-time in-game chat summary after installing or changing Skyveil versions, with `/sv changelog` to show the current notes again.

## 1.10.1

- Removed the Fishing, Farming, and Mining categories and every exclusively owned implementation, registration, mixin, test, and resource.
- Removed Skyveil's AH/Bazaar/NPC pricing clients, cache, tooltips, search replacement, diagnostics, and price-only Hunting Box panel.
- Kept Attribute Progress as a local-only feature with stable shard identity plus rarity/quantity sorting; market values remain unavailable because SkyCoFL does not expose a documented cross-mod price API.
- Added config schema 15 migration that drops obsolete fields while preserving retained settings, plus release checks that reject removed classes and resources.
- Extended Hide Sea Creature Messages so a preserved Double Hook notification is followed by `You just double hooked <Sea Creature>!` instead of losing the creature identity.
- Replaced the dialogue-only resource with an unambiguous 89-entry dialogue-to-creature mapping and retained a short timeout plus support for intervening Autopet or system messages.
- Kept ordinary catches hidden without replacement and added Double Hook naming, timeout, and normal-catch regression tests without adding another setting.

## 1.10.0

- Added the default-off Fishing > General > Hide Sea Creature Messages toggle.
- Added exact matching for 89 verified current Hypixel sea-creature catch dialogues instead of a broad fishing-chat regex.
- Explicitly preserved both current Double Hook message variants, action-bar messages, trophy catches, loot catches, player chat, and unknown future dialogue.
- Added focused formatted-dialogue, Double Hook, unrelated-message, disabled-state, overlay, and catalog-completeness tests.

## 1.9.0

- Added General > Zoom with a default C keybind that can be changed from Skyveil settings.
- Added hold-to-zoom world FOV magnification without using a Spyglass or rendering its scope overlay.
- Added scroll-controlled 2x-12x zoom levels while held, preserving ordinary hotbar scrolling whenever zoom is inactive.
- Added configuration migration and focused default-key, scroll-direction, clamping, magnification, and safe-FOV tests.

## 1.8.2

- Fixed real Hypixel and Training Dummy damage indicators bypassing Compact Damage because of unsupported direct-attack, component-layout, equipment, and entity-age requirements.
- Applied source-label suppression in the concrete Minecraft 26.1 Armor Stand renderer override and retained strict damage-only text plus nearby-target classification.
- Raised the single boxed rolling-average display above the mob's normal name-tag stack while keeping it attached to the mob's live position.

## 1.8.1

- Changed Compact Damage from summed armor-stand batches to one rolling average of the latest five accepted hits per target.
- Moved the replacement label to the target's live world position so it follows moving mobs while all accepted source damage labels remain suppressed.
- Kept responsive partial averages, nearest-integer formatting, inactivity reset, bounded expiry, target separation, and arbitrary-precision arithmetic.

## 1.8.0

- Added the default-off General > Compact Damage toggle for client-side aggregation of up to five rapid, locally initiated hits against the same nearby target.
- Added strict damage-only armor-stand parsing for verified Hypixel number, critical-decoration, and effect-symbol formats with arbitrary-precision totals.
- Reused entity lifecycle and render-state hooks to suppress only accepted original labels, update a camera-facing combined vanilla name tag, and clear bounded state on removal, disable, expiry, disconnect, or world change.
- Added focused parser, rejection, batching, target separation, timeout, expiry, reset, formatting, and overflow tests.

## 1.7.4

- Fixed Attribute Menu entries resolving their ability-style internal IDs through the wrong shard-display catalog, which left real entries without the stable Bazaar/ownership identity used by the Hunting Box.
- Added stable content fingerprints, two-observation page settling, transient-empty-page protection, page merging, and gated Attribute Menu diagnostics to `/sv debughunting`.
- Updated the verified 320-entry consumable shard catalog for Rainbug, excluded non-syphonable Chameleon, and added catalog-backed rarity so required quantities do not depend on prices or menu styling.
- Added sanitized regression coverage for real concrete IDs, ownership states, catalog boundaries, normalization, page merging, loading states, lifecycle isolation, optional values, and unknown-last sorting.

## 1.7.3

- Declared Skyveil client-only in Fabric metadata and removed the unused Maven publication template while retaining the single verified release path.
- Hardened Bazaar parsing against malformed numeric product fields, kept unavailable prices distinct from zero, and added expiry and menu-title boundary tests.
- Flushed queued configuration writes during normal client shutdown, logged failed corrupt-file backups, and precompiled repeated bait-name patterns.
- Added architecture and lifecycle documentation, strict Java compiler diagnostics, and removed ignored IDE output plus obsolete placeholder/log clutter.

## 1.7.2

- Fixed the global Bazaar reversal so `buy_summary`/`quick_status.buyPrice` drive immediate purchases and `sell_summary`/`quick_status.sellPrice` drive immediate sales.
- Renamed cached Bazaar fields around explicit player actions and order-book roles, and invalidated incompatible v12 interpreted prices.
- Preserved green/red normal tooltip labels, stack totals, per-unit values, and compact formatting while applying the corrected values to recipes, Attribute Menu costs, and Hunting Box totals.
- Added Sell Offer and Insta Sell values to each missing Attribute Menu row and fixture-based Bazaar provider, tooltip, cache, and missing-market-side tests.

## 1.7.1

- Fixed Attribute Menu discovery for Hypixel's concrete `ATTRIBUTE_SHARD_<NAME>;1` item identities and mapped each shard to its distinct Bazaar product.
- Made ownership explicit and row construction resilient so unknown rarity, quantity, or price data cannot discard legitimate unowned attributes.
- Added gated, event-driven Attribute Menu pipeline diagnostics and focused regression coverage for identity, progression, pricing fallbacks, deduplication, and sorting.

## 1.7.0

- Added persistent Price, Rarity, and Quantity sorting with direction toggles to the Hunting Box value panel.
- Added an Attribute Menu progress panel with page-aware collection scanning, missing-attribute rows, preserved shard icons/styles, remaining-shard requirements, and current Insta Buy cost estimates.
- Added centralized post-Foraging rarity-specific Attribute syphon progression calculations and lobby-scoped menu/Hunting Box caches.
- Corrected Bazaar order-side semantics so Insta Buy uses sell offers and Insta Sell uses buy orders, invalidating only obsolete Bazaar cache data.
- Moved configuration file writes onto a serialized background worker and made both shard panels rebuild from container updates, sort changes, or price snapshot changes rather than per-frame tooltip parsing.

## 1.6.1

- Fixed `/skyveil` and `/sv` being closed immediately by Minecraft's chat screen after command execution.
- Menu commands now open Skyveil safely on the following client tick.

## 1.6.0

- Rebuilt the existing Skyveil menu search as a cached, registry-backed feature-name-only search engine.
- Added case-insensitive whitespace-normalized exact, prefix, and substring ranking without indexing descriptions, tooltips, internal keys, or category names.
- Added compact scrollable name/path results, duplicate registration detection, direct category/subcategory navigation, row scrolling, and a short selected-row highlight.
- Kept search text temporary and protected focused search input from setting-keybind capture and gameplay custom commands.

## 1.5.1

- Fixed Wardrobe Number Keys failing to detect Hypixel's current `(page/total) Armor Sets` title and falling through to vanilla hovered-slot hotbar swaps.
- Switched resolution to the verified Wardrobe selector row at container slots 36-44 while still preferring each selector's displayed Wardrobe number.
- Supported the current three-page layout, consumed supported number keys whenever the real Wardrobe structure is detected, and added gated last-action details to `/sv debugwardrobe`.

## 1.5.0

- Added General -> Keybinds -> Wardrobe Number Keys, enabled by default.
- Added safe live Wardrobe-menu detection and metadata-derived Wardrobe-number resolution without hardcoded GUI coordinates.
- Added top-row and numpad 1-9 support, plus 0 for actual Wardrobe Slot 10, with one normal click per physical key press.
- Added `/sv debugwardrobe` diagnostics for live titles, container sizes, resolved menu slots, and rejected controls.

## 1.4.5

- Corrected the modern Hypixel Banshee wail event from the obsolete Ghast warning sound to `minecraft:entity.ghast.ambient`.
- Kept the exact Banshee pitch whitelist so unrelated Backwater Bayou and game audio remains audible.

## 1.4.4

- Fixed Mute Banshee Sounds cancelling unrelated audio near a Banshee.
- Restricted muting to the exact low-pitched `minecraft:entity.ghast.warn` sequence used by the Banshee wail in Backwater Bayou.

## 1.4.3

- Fixed Banshee wails bypassing sound muting by covering Hypixel's server-seeded entity and positional sound paths.
- Recognized the Banshee's player-based mob entity, associated positional sounds with its verified nameplate, and added sidebar fallback detection for Backwater Bayou.

## 1.4.2

- Changed filtered Bestiary tier-up chat into one compact `[Bestiary Level Up] Mob Tier` message while continuing to hide the reward block.

## 1.4.1

- Added a median-based Calculated Price directly beneath Auction House LBIN values.
- Matched active BIN comparisons by tooltip-visible enchantments and levels, reforge, recombobulation, stars, rarity, pets, skins, dyes, editions, gemstones, attributes, scrolls, and major item upgrades.
- Added hierarchical comparison fallbacks and upgraded the compact auction cache without discarding cached Bazaar or NPC data.

## 1.4.0

- Added General → Item Prices with separate Auction, Bazaar, NPC, compact-number, and stack-mode controls.
- Added non-blocking official Hypixel price providers with source-specific refresh schedules, throttling, exponential backoff, immutable snapshots, and a compact persistent disk cache.
- Added stable custom-data item identity resolution, pet/rarity/recombobulation/major-variant auction matching, decorative-menu filtering, and `/sv debugprice` diagnostics.
- Added precisely labelled Bazaar Instant Sell/Buy values, normalized active lowest-BIN prices, and verified positive NPC sell values to SkyBlock item tooltips.

## 1.3.8

- Added a quiet half-second chime cadence during the first two seconds of an Elusive Sea Creature alert.
- Linked alerts to their resolved mob or nameplate entity so the HUD closes immediately when that entity dies or despawns.
- Restricted sound muting to confirmed Banshees while in Backwater Bayou and renamed the setting to Mute Banshee Sounds.

## 1.3.7

- Hid the vanilla potion-effect panel when opening inventory by default.
- Added a one-time config migration so existing installations also receive the hidden-panel behavior while retaining the Show Potion Effects opt-in toggle.

## 1.3.6

- Fixed incorrect Reel Now countdown values caused by switching to unrelated nearby numeric Armor Stands.
- Locked each cast to one fresh timer entity, rejected stale previous-cast tags, preferred explicit seconds nameplates, and ignored upward stale-data jumps.
- Expanded `/sv debugfishing` output with timer entity age and explicit-seconds detection.

## 1.3.5

- Replaced the fabricated bobber elapsed-time panel with Hypixel's authoritative Armor Stand countdown text.
- Unified the live countdown and green Reel Now alert at the existing Reel Now HUD position and scale.
- Added nearest-local-bobber association, crowded-bobber ambiguity rejection, metadata-driven updates, and complete cast/world/despawn cleanup.
- Added the Show Bobber Timer toggle and `/sv debugfishing` diagnostics without affecting Reel Now when the countdown is disabled.

## 1.3.4

- Audited all client initialization, event registrations, HUD hooks, Mixins, configuration entries, resources, and release tasks.
- Removed duplicate Pet Display system-message processing and blocked signed player chat from reaching server-state parsers.
- Fixed repeated Trophy Catch config writes, stale bait and pet-menu state, stale NPC skin and Sea Creature entity caches, and invalid persisted marker/config data.
- Fixed Inventory Button management rendering while the main feature is disabled and stopped closed inventory screens from being retained.
- Reduced per-frame settings-search work and deferred slider/HUD-resize persistence until editing completes.
- Removed temporary fishing diagnostics and verified Minecraft 26.1 compilation, startup, and the single-JAR release workflow.

## 1.3.3

- Fixed fishing reward messages disconnecting the client with `RenderSystem called from wrong thread`.
- Moved Bestiary chat filtering after Minecraft's packet-to-client-thread handoff.
- Serialized Sea Creature entity callbacks on the client thread to prevent cross-thread world and tracker access.

## 1.3.2

- Replaced literal Elusive nameplate matching with a bundled, registry-based Sea Creature identity and classification system.
- Added 90 current Sea Creature identities and seven verified Elusive classifications with exact normalized lookup.
- Unified Rare Sea Creature alerts and sound muting around shared live entity resolution.
- Removed the obsolete local-bobber detector and duplicate Elusive/nameplate parsing code.

## 1.3.1

- Fixed Rare Sea Creature Alert detection so it no longer depends on the local player's fishing hook or catch window.
- Added event-driven, 80-block Elusive nameplate tracking with delayed metadata support and split Armor Stand component resolution.
- Added robust per-creature duplicate prevention and `/sv debugseacreature` diagnostics.

## 1.3.0

- Added the Bestiary top-level settings category.
- Added Hide Bestiary Rewards, preserving the original mob/tier line while removing its reward block.
- Added bounded multi-packet Bestiary chat tracking with safe timeout and false-start recovery.

## 1.2.0

- Added Mute Sea Creature Sounds under Fishing → Sea Creatures.
- Added a three-second Elusive Sea Creature alert using recent fishing Armor Stand nameplates.
- Added contextual Sea Creature association, world cleanup, and duplicate-alert prevention.

## 1.1.1

- Added a persistent Show Potion Effects toggle under General → Inventory Buttons.
- Inventory status-effect rendering can now be hidden without changing active effects or HUD icons.

## 1.1.0

- Added persistent custom command keybinds under General → Keybinds.
- Added key capture, editing, per-binding toggles, deletion, and conflict warnings.
- Restricted command execution to one trigger per key press during normal gameplay.

## 1.0.1

## 1.0.0

- Established the semantic-versioning baseline for future Skyveil releases.
