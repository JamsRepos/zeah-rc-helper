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

	@Test
	public void firstChiselOnFullSecondLoadStaysChiselNotAltar()
	{
		// Fragments + dark + full inventory (first chisel): must not flip to GO_ALTAR.
		assertEquals(RotationStep.CHISEL_AND_RETURN,
			RotationLogic.infer(carrying(0, 25, 4), false, false, false, RotationStep.GO_DARK_SECOND));
	}

	@Test
	public void finishingFirstLoadWithNothingLeftGoesToMineSecond()
	{
		// No Second Load yet this Trip - carrying on to mine it is correct, even fully chiselled.
		assertEquals(RotationStep.MINE_SECOND,
			RotationLogic.infer(carrying(0, 0, 60), false, false, true, RotationStep.CHISEL_AND_RETURN, false));
	}

	@Test
	public void miningPartWayThroughTheSecondLoadStillSaysSecond()
	{
		// Held Fragments prove this mining pass is the Second Load, same signal already used to
		// pick GO_DARK_SECOND once the inventory fills - "Mine" must agree with "Venerate"
		// instead of falling back to the First Load's label mid-pass.
		assertEquals(RotationStep.MINE_SECOND,
			RotationLogic.infer(carrying(5, 0, 104), false, false, true, RotationStep.MINE_SECOND, false));
	}

	@Test
	public void finishingSecondLoadWithNothingLeftGoesToAltar()
	{
		// There is no Third Load - nothing left to mine means go craft, not "Mine again".
		assertEquals(RotationStep.GO_ALTAR,
			RotationLogic.infer(carrying(0, 0, 60), false, false, true, RotationStep.CHISEL_AND_RETURN, true));
	}

	@Test
	public void secondLoadBelowFullStackStillGoesToAltarNotMine()
	{
		// Issue #12: a reduced Load (non-essence items eating slots) can chisel to well under the
		// fixed Fragment threshold. With no Third Load to mine, that must not send the player back
		// to the runestones - Chisel/Altar routing here cares about Loads left, not the count.
		assertEquals(RotationStep.GO_ALTAR,
			RotationLogic.infer(carrying(0, 20, 96), false, false, false,
				RotationStep.CHISEL_AND_RETURN, true));
	}

	@Test
	public void firstLoadBelowFullStackKeepsChisellingTowardTheMine()
	{
		// Same Fragment/Dark shape as the issue #12 case, but on the First Load: the Second Load
		// still needs mining, so this must stay CHISEL_AND_RETURN (mine-ward), not flip to altar.
		assertEquals(RotationStep.CHISEL_AND_RETURN,
			RotationLogic.infer(carrying(0, 20, 96), false, false, false,
				RotationStep.CHISEL_AND_RETURN, false));
	}

	@Test
	public void fullStackMidChiselKeepsChiselling()
	{
		// 4 frags/block: known Full after ~25 of 27, with dark still left.
		assertEquals(RotationStep.CHISEL_AND_RETURN,
			RotationLogic.infer(carrying(0, 2, RotationLogic.FULL_FRAGMENTS), false, false, false,
				RotationStep.CHISEL_AND_RETURN));
	}

	@Test
	public void checkConfirmedFullOnFirstLoadStillKeepsChiselling()
	{
		// A Check confirming Full must not skip mining the Second Load - a Trip is always
		// both Loads, even if the First Load alone already crossed the threshold.
		assertEquals(RotationStep.CHISEL_AND_RETURN,
			RotationLogic.infer(carryingChecked(0, 2, RotationLogic.FULL_FRAGMENTS), false, false, false,
				RotationStep.CHISEL_AND_RETURN, false));
	}

	@Test
	public void checkConfirmedFullBreaksTheChiselVetoMidRun()
	{
		// Same inputs as fullStackMidChiselKeepsChiselling, but the Second Load has already
		// been reached and a fresh Check just confirmed Full - unlike a count merely inferred
		// from this run's own chiselling, that is a deliberate signal that should unlock the
		// altar immediately rather than finishing this Load's remaining Dark Blocks.
		assertEquals(RotationStep.GO_ALTAR,
			RotationLogic.infer(carryingChecked(0, 2, RotationLogic.FULL_FRAGMENTS), false, false, false,
				RotationStep.CHISEL_AND_RETURN, true));
	}

	@Test
	public void secondLoadWithFullStackGoesToAltar()
	{
		assertEquals(RotationStep.GO_ALTAR,
			RotationLogic.infer(carrying(0, 10, RotationLogic.FULL_FRAGMENTS), false, false, false,
				RotationStep.GO_DARK_SECOND));
	}

	@Test
	public void unknownHighFragmentEstimateDoesNotForceAltar()
	{
		// Inferred/unwatched counts must not flip to GO_ALTAR; overlay would show "?" too.
		assertEquals(RotationStep.CHISEL_AND_RETURN,
			RotationLogic.infer(carryingUnknown(0, 10, 108), false, false, false,
				RotationStep.GO_DARK_SECOND));
	}

	@Test
	public void nearAltarWithFragmentsAndDarkGoesToAltar()
	{
		assertEquals(RotationStep.GO_ALTAR,
			RotationLogic.infer(carrying(0, 10, 40), false, true, false, RotationStep.CHISEL_AND_RETURN));
		assertEquals(RotationStep.GO_ALTAR,
			RotationLogic.infer(carryingUnknown(0, 10, 1), false, true, false, RotationStep.CHISEL_AND_RETURN));
		assertEquals(RotationStep.GO_ALTAR,
			RotationLogic.infer(carrying(0, 2, RotationLogic.FULL_FRAGMENTS), false, true, false,
				RotationStep.CHISEL_AND_RETURN));
	}

	@Test
	public void onlyMiningTheFullSecondLoadMarksItReached()
	{
		// Arriving at the mine and being told to start (MINE_SECOND) is not the same as having
		// actually mined it - only a full Second Load (GO_DARK_SECOND) proves that (issue #9).
		assertEquals(false, RotationLogic.marksSecondLoadReached(RotationStep.MINE_SECOND));
		assertEquals(true, RotationLogic.marksSecondLoadReached(RotationStep.GO_DARK_SECOND));
	}

	@Test
	public void arrivingAtMineForSecondLoadDoesNotJumpToAltarNextTick()
	{
		// Regression for issue #9: flipping secondLoadReached off the back of MINE_SECOND made
		// the very next tick's identical empty-handed state (still no Dense/Dark, just the First
		// Load's Fragments) read as the Second Load already being done too, before any mining
		// happened.
		InventorySnapshot arrivingAtMine = carrying(0, 0, 104);
		RotationStep firstTick = RotationLogic.infer(
			arrivingAtMine, false, false, true, RotationStep.CHISEL_AND_RETURN, false);
		assertEquals(RotationStep.MINE_SECOND, firstTick);

		boolean secondLoadReached = RotationLogic.marksSecondLoadReached(firstTick);
		assertEquals(false, secondLoadReached);
		assertEquals(RotationStep.MINE_SECOND,
			RotationLogic.infer(arrivingAtMine, false, false, true, firstTick, secondLoadReached));
	}

	/** Fragments are one stackable slot; blocks take one slot each. */
	private static InventorySnapshot carrying(int dense, int dark, int fragments)
	{
		return snapshot(dense, dark, fragments, true, false);
	}

	private static InventorySnapshot carryingUnknown(int dense, int dark, int fragments)
	{
		return snapshot(dense, dark, fragments, false, false);
	}

	/** A fresh Check just confirmed this exact count. */
	private static InventorySnapshot carryingChecked(int dense, int dark, int fragments)
	{
		return snapshot(dense, dark, fragments, true, true);
	}

	private static InventorySnapshot snapshot(
		int dense, int dark, int fragments, boolean fragmentsKnown, boolean checkConfirmed)
	{
		int used = dense + dark + (fragments > 0 ? 1 : 0);
		return new InventorySnapshot(
			dense, dark, fragments, SLOTS - used, true, true, false, false, true, false, -1, fragmentsKnown,
			fragments > 0, 0, 0, checkConfirmed);
	}
}
