# Zeah RC Helper

RuneLite plugin that guides the Arceuus (Zeah) blood and soul runecrafting loop with next-click highlights, a status panel, and equipment reminders.

Enabling this plugin conflicts with **Easy Arceuus Runecrafting**.

## The rotation

Same pattern for bloods (77 RC) and souls (90 RC) — only the altar changes:

1. Mine a full inventory of dense essence
2. Venerate at the Dark Altar
3. Chisel into fragments while running back to the mine
4. Mine a second inventory (keep the fragment stack)
5. Venerate the second inventory
6. Run to the Blood or Soul Altar
7. Craft the fragments
8. Chisel the remaining dark blocks
9. Craft the second batch, then return to the mine

## Reminders

- **Abyssal lantern** (shield slot) — remind to equip it. If log checking is on:
  - Bloods: blisterwood (20% more bloods), magic (10% all runes), or redwood
  - Souls: magic or redwood (willow also gives +5% runes). Blisterwood only helps bloods.
- **Blood essence** (blood mode) — bring it, activate it, and warn at a configurable charge threshold. Also reminds on idle/break. Check the essence in-game so the plugin can read remaining charges from chat.
- **Idle** — if you stand still in the Arceuus RC area too long. Optional gentle screen tint (off by default).

## Config

- Rune type: Blood / Soul / Auto (soul at 90 RC)
- Helper + next-click highlight + status panel
- Lantern reminder and log-type check
- Blood essence reminder and low-charge threshold
- Idle seconds and optional tint

## Development

Requires JDK 11+.

```bash
./gradlew run
```

For Jagex accounts, follow [Using Jagex Accounts](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts).

## License

BSD-2-Clause.
