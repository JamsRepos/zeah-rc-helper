package com.zeahrchelper;

/**
 * Pure rotation step inference and trip counting, free of the RuneLite client so it can be
 * unit-tested. {@link RotationHelper} supplies the location flags each tick.
 */
final class RotationLogic
{
	/** Enough Fragments held that the next load of Dense Blocks goes straight to the altar. */
	static final int FULL_FRAGMENTS = 100;

	private RotationLogic()
	{
	}

	static RotationStep infer(
		InventorySnapshot inv,
		boolean atAltar,
		boolean nearAltar,
		boolean atMine,
		RotationStep lastStep)
	{
		boolean hasFrags = inv.getFragments() > 0;
		boolean hasDark = inv.getDarkBlocks() > 0;
		boolean hasDense = inv.getDenseBlocks() > 0;
		boolean inventoryFull = inv.getEmptySlots() == 0;
		boolean fullFragmentStack = inv.getFragments() >= FULL_FRAGMENTS;

		if (atAltar)
		{
			if (hasFrags)
			{
				return secondBatch(lastStep) ? RotationStep.CRAFT_REMAINING : RotationStep.CRAFT_FRAGMENTS;
			}
			if (hasDark)
			{
				return RotationStep.CHISEL_AT_ALTAR;
			}
			return RotationStep.RETURN_TO_MINE;
		}

		if (hasFrags && hasDark && (fullFragmentStack || nearAltar))
		{
			return RotationStep.GO_ALTAR;
		}
		if (hasDense && inventoryFull)
		{
			return fullFragmentStack || hasFrags ? RotationStep.GO_DARK_SECOND : RotationStep.GO_DARK_FIRST;
		}
		if (hasDark && !fullFragmentStack)
		{
			return RotationStep.CHISEL_AND_RETURN;
		}
		if (hasFrags && !hasDark && !hasDense)
		{
			return atMine ? RotationStep.MINE_SECOND : RotationStep.RETURN_TO_MINE;
		}
		if (!atMine && hasDense && !fullFragmentStack)
		{
			return RotationStep.GO_DARK_FIRST;
		}
		return RotationStep.MINE_FIRST;
	}

	/** The second batch follows chiselling at the altar; it stays until the player leaves. */
	static boolean secondBatch(RotationStep lastStep)
	{
		return lastStep == RotationStep.CHISEL_AT_ALTAR || lastStep == RotationStep.CRAFT_REMAINING;
	}

	static boolean isTripCompleteTransition(RotationStep from, RotationStep to)
	{
		return (from == RotationStep.CRAFT_REMAINING || from == RotationStep.RETURN_TO_MINE)
			&& to == RotationStep.MINE_FIRST;
	}
}
