# Zeah RC Helper

RuneLite plugin for Arceuus blood and soul runecrafting. It walks you through the full rotation with next-click highlights, floor paths, a status panel, and reminders for your lantern and blood essence.

> **Note:** This plugin conflicts with [Easy Arceuus Runecrafting](https://github.com/poi56iop/easy-arceuus-runecrafting). Only one can be enabled at a time.

## Install

### Plugin Hub

Search for **Zeah RC Helper** in the RuneLite Plugin Hub (Configuration → Plugin Hub).

### Build from source

Requires JDK 11+.

```bash
./gradlew run
```

For Jagex accounts, follow [Using Jagex Accounts](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts).

## Features

- **Next-click highlight** — flashes the runestone, agility shortcut, or altar you should click next. Runestone selection follows the same varbit logic as Easy Arceuus Runecrafting and swaps when a stone depletes.
- **Floor path** — draws a line to your destination while travelling. Uses agility shortcuts you qualify for: 69 north (mine ↔ Dark Altar), 73 west (Blood Altar → mine), 52 east (mine ↔ soul path), and 49 boulder (soul approach → mine). Hidden when you are already at the target.
- **Status panel** — current step, dense/dark/fragment counts, and trip counter.
- **Gear reminders** — chisel (including jeweller's), pickaxe, and abyssal lantern. Optional lantern log-type check:
  - Bloods: blisterwood (20% more bloods), magic (10% all runes), or redwood
  - Souls: magic or redwood (willow also gives +5% runes). Blisterwood only helps bloods.
- **Blood essence** (blood mode) — reminds you to bring and activate it, and tracks charges from activate/craft chat (and Item Charges if available). Warns at a configurable charge threshold.
- **Idle reminder** — warns if you stand still in the Arceuus RC area too long. Optional gentle screen tint (off by default).

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

## Configuration

| Setting | Default | Description |
| --- | --- | --- |
| Rune type | Auto | Blood, Soul, or Auto (soul at 90 RC) |
| Enable helper | On | Master toggle for overlays |
| Highlight next click | On | Object clickbox highlight |
| Show path | On | Floor path to destination |
| Show status panel | On | Step and inventory panel |
| Gear reminders | On | Chisel, pickaxe, and lantern |
| Check lantern logs | On | Warn on wrong/unlit logs |
| Blood essence reminder | On | Blood mode only |
| Low essence charges | 100 | Warn at or below this charge count |
| Idle reminder | 15s | Idle warning delay |
| Idle screen tint | Off | Optional screen tint when idle |

## Development

```bash
./gradlew test        # unit tests
./gradlew shadowJar   # build plugin jar
./gradlew run         # launch dev client
```

## License

BSD-2-Clause. See [LICENSE](LICENSE).
