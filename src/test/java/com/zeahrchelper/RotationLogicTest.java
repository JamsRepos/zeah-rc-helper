package com.zeahrchelper;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class RotationLogicTest
{
	private static final int SLOTS = 28;

	@Test
	public void atAltarWithFragmentsCraftsFirstBatch()
	{
		assertEquals(RotationStep.CRAFT_FRAGMENTS,
			RotationLogic.infer(carrying(0, 5, 40), true, true, false, RotationStep.GO_ALTAR));
	}

	@Test
	public void chiselAtAltarThenFragmentsCraftsSecondBatch()
	{
		assertEquals(RotationStep.CRAFT_REMAINING,
			RotationLogic.infer(carrying(0, 0, 32), true, true, false, RotationStep.CHISEL_AT_ALTAR));
	}

	@Test
	public void secondBatchStaysWhileCrafting()
	{
		assertEquals(RotationStep.CRAFT_REMAINING,
			RotationLogic.infer(carrying(0, 0, 32), true, true, false, RotationStep.CRAFT_REMAINING));
	}

	@Test
	public void leavingTheAltarAfterChisellingForgetsTheSecondBatch()
	{
		assertEquals(RotationStep.RETURN_TO_MINE,
			RotationLogic.infer(carrying(0, 0, 32), false, true, false, RotationStep.CHISEL_AT_ALTAR));
		assertEquals(RotationStep.CRAFT_FRAGMENTS,
			RotationLogic.infer(carrying(0, 0, 32), true, true, false, RotationStep.RETURN_TO_MINE));
	}

	@Test
	public void tripCountsFromSecondBatchToMining()
	{
		assertEquals(true,
			RotationLogic.isTripCompleteTransition(RotationStep.CRAFT_REMAINING, RotationStep.MINE_FIRST));
		assertEquals(true,
			RotationLogic.isTripCompleteTransition(RotationStep.RETURN_TO_MINE, RotationStep.MINE_FIRST));
		assertEquals(false,
			RotationLogic.isTripCompleteTransition(RotationStep.CRAFT_FRAGMENTS, RotationStep.MINE_FIRST));
	}

	@Test
	public void atAltarWithOnlyDarkBlocksChisels()
	{
		assertEquals(RotationStep.CHISEL_AT_ALTAR,
			RotationLogic.infer(carrying(0, 5, 0), true, true, false, RotationStep.CRAFT_FRAGMENTS));
	}

	@Test
	public void fullDenseAtMineGoesToDarkAltar()
	{
		assertEquals(RotationStep.GO_DARK_FIRST,
			RotationLogic.infer(carrying(SLOTS, 0, 0), false, false, true, RotationStep.MINE_FIRST));
	}

	@Test
	public void secondFullDenseWithFragmentsGoesToDarkAgain()
	{
		assertEquals(RotationStep.GO_DARK_SECOND,
			RotationLogic.infer(carrying(SLOTS - 1, 0, 40), false, false, true, RotationStep.MINE_SECOND));
	}

	/** Fragments are one stackable slot; blocks take one slot each. */
	private static InventorySnapshot carrying(int dense, int dark, int fragments)
	{
		int used = dense + dark + (fragments > 0 ? 1 : 0);
		return new InventorySnapshot(dense, dark, fragments, SLOTS - used, true, true, false, false, true, false, -1);
	}
}
