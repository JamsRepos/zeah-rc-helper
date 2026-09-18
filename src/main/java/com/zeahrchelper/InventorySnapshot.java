package com.zeahrchelper;

import lombok.Value;

@Value
public class InventorySnapshot
{
	int denseBlocks;
	int darkBlocks;
	int fragments;
	int emptySlots;
	boolean hasChisel;
	boolean hasPickaxe;
	boolean hasInactiveBloodEssence;
	boolean hasActiveBloodEssence;
	boolean lanternEquipped;
	boolean lanternInInventory;
	int lanternItemId;
	/** False when a fragment stack is present but the count was not watched or Count-checked. */
	boolean fragmentsKnown;
	/** True when a dark essence fragment stack is in the inventory. */
	boolean hasFragments;
	/** Inventory quantity plus any held Rune Pouch/Divine Rune Pouch quantity, combined. */
	int bloodRunes;
	/** Inventory quantity plus any held Rune Pouch/Divine Rune Pouch quantity, combined. */
	int soulRunes;
	/** True once an explicit Check has confirmed the current fragment count this trip. */
	boolean fragmentsCheckConfirmed;
}
