# Rune pouch-aware crafted-rune tracking

Source: https://github.com/JamsRepos/zeah-rc-helper/issues/14

## Problem

`SessionStats` only credits a crafted Blood/Soul rune when the fragment stack
leaves the main inventory *and* `ItemID.BLOODRUNE`/`ItemID.SOULRUNE` quantity
rises in the main 28-slot inventory (`InventoryChecker.scan()` only reads
`InventoryID.INV`). Runes that OSRS deposits directly into a held Rune Pouch
or Divine Rune Pouch never raise the main-inventory quantity, so they are
silently undercounted in both the session and RS-profile lifetime totals.

## Decisions (from grilling session)

- **Scope**: Regular Rune Pouch (`ItemID.BH_RUNE_POUCH`, `BH_RUNE_POUCH_TROUVER`)
  and Divine Rune Pouch (`ItemID.DIVINE_RUNE_POUCH`, `DIVINE_RUNE_POUCH_TROUVER`).
  Blood and Soul runes only - the only rune types this plugin tracks.
- **Counting model**: Pouch-deposited runes feed the *same* counters as
  inventory-crafted ones (`SessionStats.bloodRunesCrafted`/`soulRunesCrafted`
  and their lifetime equivalents). No separate "crafted into pouch" stat.
  Concretely: the quantity fed into `SessionStats.noteRuneInventory(...)`
  becomes inventory qty + pouch qty, combined per rune type. The existing
  delta/pending-tick crediting logic in `SessionStats` is otherwise unchanged.
- **Stale-data guard**: Only add pouch quantity for a rune type while the
  corresponding pouch item is actually present in the player's inventory
  this tick. The pouch's contents are exposed via varbits that are populated
  from the item's own data and may keep reporting last-known contents when
  the pouch isn't held (e.g. it's in the bank) - gate on presence to avoid
  false credit from stale reads.
- **UI**: No new display. This is a correctness fix for the existing
  session/lifetime "Runes Crafted" stats, not a new pouch-contents readout.
  (RuneLite's own bundled Rune Pouch plugin already covers a live contents
  overlay if a user wants that.)
- **Historical totals**: No backfill/correction for past undercounting -
  lifetime totals (`lifetimeBloodRunes`/`lifetimeSoulRunes` RS-profile config)
  simply become accurate going forward. The existing "reset counters" action
  (`SessionStats.resetAllCounters()`) remains the escape hatch for anyone who
  wants a clean baseline.
- **Documentation**: This spec only - no ADR. The pouch-gating rationale
  belongs as a short code comment (matching `SessionStats`' existing
  crediting-logic doc comment), not a standalone architecture record.

## Implementation notes (facts gathered, not decisions)

- There is no `InventoryID`/`ItemContainer` for the rune pouch - its
  contents are exposed only via varbits: `VarbitID.RUNE_POUCH_TYPE_1..6` /
  `RUNE_POUCH_QUANTITY_1..6` (6 slot pairs). Both the regular and Divine
  pouch read the same 6 slots uniformly - the game only populates the slots
  the held pouch actually has; there is no separate varbit range per pouch
  type. No new decision needed to distinguish which pouch is held.
- Resolve a slot's rune identity via `client.getEnum(EnumID.RUNEPOUCH_RUNE)`,
  using the `RUNE_POUCH_TYPE_n` varbit value as the enum key to get the real
  item ID (`getIntValue(...)`). Do not hardcode a varbit-value-to-rune
  mapping - this is how RuneLite's own bundled `RunepouchOverlay` does it,
  and it satisfies this project's "no magic numbers, use gameval-derived
  lookups" convention.
- RuneLite's own `RunepouchOverlay` gates on the pouch item being a widget
  item in the inventory (via `WidgetItemOverlay` + itemId check against
  `BH_RUNE_POUCH`, `BH_RUNE_POUCH_TROUVER`, `DIVINE_RUNE_POUCH`,
  `DIVINE_RUNE_POUCH_TROUVER`), not a manual `ItemContainer` scan for
  presence. Either approach satisfies the presence-gating decision above;
  a manual `ItemContainer.getItems()` scan over `InventoryID.INV` (which
  `InventoryChecker.scan()` already performs) is the more natural fit here
  since this plugin doesn't use `WidgetItemOverlay`.
- Reading pouch varbits can piggyback on the existing `onGameTick`-driven
  `RotationHelper.update()` path (varbits are already current by the time
  `GameTick` fires) rather than requiring a new `VarbitChanged` subscription,
  mirroring how `onInventoryChanged()` already re-scans and re-credits
  mid-tick for the main-inventory case.
- Confirmed no other logic in the plugin depends on `bloodRunes`/`soulRunes`
  counts besides `SessionStats` crediting and the `StatusOverlay` display of
  `SessionStats`' own totals - `RotationLogic`'s only inventory-occupancy
  signal is `emptySlots` (for `inventoryFull`), and `ReminderService` doesn't
  reference rune counts at all. So this fix is scoped entirely to
  `InventoryChecker`/`InventorySnapshot`/`SessionStats`/`RotationHelper`
  wiring - no routing/reminder behavior changes.

## Out of scope

- Any rune type other than Blood/Soul.
- Any new overlay/UI surfacing pouch contents.
- Backfilling or correcting already-stored lifetime totals.
- A dedicated ADR.
