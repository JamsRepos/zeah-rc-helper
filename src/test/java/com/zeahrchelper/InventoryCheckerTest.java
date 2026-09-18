package com.zeahrchelper;

import net.runelite.api.gameval.ItemID;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class InventoryCheckerTest
{
	@Test
	public void regularAndDivinePouchAreRecognised()
	{
		assertTrue(InventoryChecker.isRunePouch(ItemID.BH_RUNE_POUCH));
		assertTrue(InventoryChecker.isRunePouch(ItemID.BH_RUNE_POUCH_TROUVER));
		assertTrue(InventoryChecker.isRunePouch(ItemID.DIVINE_RUNE_POUCH));
		assertTrue(InventoryChecker.isRunePouch(ItemID.DIVINE_RUNE_POUCH_TROUVER));
	}

	@Test
	public void unrelatedItemsAreNotRunePouches()
	{
		assertFalse(InventoryChecker.isRunePouch(ItemID.CHISEL));
		assertFalse(InventoryChecker.isRunePouch(ItemID.BLOODRUNE));
	}

	@Test
	public void sumsBloodAndSoulSlotsSeparately()
	{
		int[] itemIds = {ItemID.BLOODRUNE, ItemID.SOULRUNE, ItemID.WATERRUNE, 0, 0, 0};
		int[] quantities = {200, 300, 9999, 0, 0, 0};

		InventoryChecker.PouchRuneTotals totals = InventoryChecker.sumPouchRunes(itemIds, quantities);

		assertEquals(200, totals.blood);
		assertEquals(300, totals.soul);
	}

	@Test
	public void ignoresSlotsWithZeroOrNegativeQuantity()
	{
		int[] itemIds = {ItemID.BLOODRUNE, ItemID.SOULRUNE};
		int[] quantities = {0, -5};

		InventoryChecker.PouchRuneTotals totals = InventoryChecker.sumPouchRunes(itemIds, quantities);

		assertEquals(0, totals.blood);
		assertEquals(0, totals.soul);
	}

	@Test
	public void emptyPouchSumsToZero()
	{
		InventoryChecker.PouchRuneTotals totals = InventoryChecker.sumPouchRunes(new int[6], new int[6]);

		assertEquals(0, totals.blood);
		assertEquals(0, totals.soul);
	}

	@Test
	public void accumulatesMultipleBloodSlots()
	{
		int[] itemIds = {ItemID.BLOODRUNE, ItemID.BLOODRUNE, ItemID.AIRRUNE};
		int[] quantities = {100, 50, 1000};

		InventoryChecker.PouchRuneTotals totals = InventoryChecker.sumPouchRunes(itemIds, quantities);

		assertEquals(150, totals.blood);
		assertEquals(0, totals.soul);
	}
}
