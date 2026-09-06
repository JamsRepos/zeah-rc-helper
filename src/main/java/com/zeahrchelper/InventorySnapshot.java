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
}
