# Zeah Rune-Crafting Rotation

Domain vocabulary for the plugin's guided rotation through the Arceuus blood/soul rune-crafting method: mining dense essence, veneration, chiselling, and crafting at the altar.

## Language

**Trip**:
One full cycle of the rotation, from the first block mined through crafting runes at the altar, ending back at the start of a fresh Load.
_Avoid_: Run, Cycle

**Load**:
One full inventory of essence blocks mined in a single mining pass. A Trip consists of exactly two Loads (First Load, Second Load) before crafting.
_Avoid_: Inventory, Batch

**Venerate**:
Converting a Load's Dense Blocks into Dark Blocks at the Dark Altar.
_Avoid_: Convert, Charge

**Chisel run**:
Chiselling a Load's Dark Blocks into Fragments while walking back toward the mine. Only the First Load has one: there is no Third Load, so the Second Load's leftover Dark Blocks are chiselled while walking to the altar instead - see ADR-0001.
_Avoid_: Chiselling session

**Fragment stack full**:
The Fragment count has reached the craft-readiness threshold, regardless of whether any Dark Blocks remain unchiselled. Distinct from Inventory full (all inventory slots occupied) — a different condition that happens to share the word "full".
_Avoid_: Full (bare), Maxed
