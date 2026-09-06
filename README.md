# Jam's Arceuus Runecrafting

A click-here helper for Arceuus blood and soul runecrafting. It highlights what to click next, draws a path there, and keeps your inventory counts and gear reminders in one small panel.

This plugin conflicts with [Easy Arceuus Runecrafting](https://github.com/poi56iop/easy-arceuus-runecrafting). Only one can be enabled at a time.

## What you see

- **Next click** — the runestone, shortcut, or altar you should click is highlighted.
- **Path** — a line on the floor and/or minimap to where you are going. Shortcuts you qualify for are used automatically. Choose where it draws (or turn it off) under Path display. Optionally hand the destination to the [Shortest Path](https://runelite.net/plugin-hub/show/shortest-path) plugin under Path source.
- **Status panel** — what to do next, plus:
  - **Dense** — uncharged blocks from the mine
  - **Dark** — blocks after the Dark Altar
  - **Fragments** — chiselled essence ready to craft
  - **Trips** — finished runs this session
  - **Essence** — blood essence status (bloods only)
- **Reminders** — the same panel tells you if you are missing a chisel, pickaxe, lantern, or blood essence, or if you have stood still too long.

Bloods show in red and Souls in teal. Auto uses souls at 90 Runecraft, otherwise bloods.

## The rotation

Same loop for bloods (77 Runecraft) and souls (90). Only the final altar changes.

1. Mine a full inventory of dense essence.
2. Venerate at the Dark Altar.
3. Chisel into fragments while running back to the mine.
4. Mine a second inventory (keep the fragment stack).
5. Venerate the second inventory.
6. Run to the Blood or Soul Altar.
7. Craft the fragments.
8. Chisel the remaining dark blocks.
9. Craft the second batch, then return to the mine.

## What to bring

- A **chisel** (jeweller's chisel is fine) and a **pickaxe**
- An **abyssal lantern** in the shield slot
  - Bloods: blisterwood (best), magic, or redwood
  - Souls: magic or redwood (willow also works). Blisterwood only helps bloods.
- **Blood essence**, activated, if you are doing bloods

## Settings

Everything is on by default except the idle screen tint.

| Setting | What it does |
| --- | --- |
| Rune type | Blood, Soul, or Auto (souls at 90 Runecraft) |
| Enable helper | Turns the highlights, path, and panel on or off |
| Highlight next click | Outline on the next object |
| Path display | Where the path is drawn: floor & minimap, floor only, minimap only, or off |
| Path source | Plugin lines, or hand the destination to the [Shortest Path](https://runelite.net/plugin-hub/show/shortest-path) plugin coloured by the current step |
| Show status panel | Step, counts, and reminders |
| Gear reminders | Warn if chisel, pickaxe, or lantern is missing |
| Check lantern logs | Warn if the lantern is unlit or using logs that do not help this method |
| Blood essence reminder | Warn to bring / activate essence, and when charges are low (bloods only) |
| Low essence charges | Charge count that counts as low (default 100) |
| Idle reminder | How long you can stand still before a warning |
| Idle screen tint | Optional faint tint when idle |

---

## Development

Requires JDK 11+.

```bash
./gradlew test        # unit tests
./gradlew shadowJar   # build plugin jar
./gradlew run         # launch dev client
```

For Jagex accounts, follow [Using Jagex Accounts](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts).

## License

BSD-2-Clause. See [LICENSE](LICENSE).
