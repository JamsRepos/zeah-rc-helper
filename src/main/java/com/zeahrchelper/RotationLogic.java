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
		return infer(inv, atAltar, nearAltar, atMine, lastStep, false);
	}

	/**
	 * @param secondLoadReached True once the Second Load has actually been mined this Trip -
	 * there is no Third Load, so finishing this Load's Dark Blocks with nothing left to mine
	 * means craft, not another mining pass.
	 */
	static RotationStep infer(
		InventorySnapshot inv,
		boolean atAltar,
		boolean nearAltar,
		boolean atMine,
		RotationStep lastStep,
		boolean secondLoadReached)
	{
		boolean hasFrags = inv.getFragments() > 0;
		boolean hasDark = inv.getDarkBlocks() > 0;
		boolean hasDense = inv.getDenseBlocks() > 0;
		boolean inventoryFull = inv.getEmptySlots() == 0;
		boolean fullFragmentStack = inv.isFragmentsKnown() && inv.getFragments() >= FULL_FRAGMENTS;
		// Mid first-load chisel looks like the second load once the stack hits Full with dark left.
		boolean chiselling = lastStep == RotationStep.CHISEL_AND_RETURN;
		// A fresh Check confirming Full is a deliberate signal, unlike Full inferred from this run's
		// own chiselling - let it break the keep-chiselling veto instead of waiting on nearAltar.
		// Only once the Second Load is reached, though: on the First Load, Full must not skip
		// mining the Second Load entirely - a Trip is always both Loads.
		boolean checkOverride = secondLoadReached && inv.isFragmentsCheckConfirmed() && fullFragmentStack;
		boolean chisellingVeto = chiselling && !checkOverride;

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

		if (hasFrags && hasDark && (nearAltar || (!chisellingVeto && fullFragmentStack)))
		{
			return RotationStep.GO_ALTAR;
		}
		if (hasDense && inventoryFull)
		{
			return fullFragmentStack || hasFrags ? RotationStep.GO_DARK_SECOND : RotationStep.GO_DARK_FIRST;
		}
		if (hasDark && (chisellingVeto || !fullFragmentStack))
		{
			return RotationStep.CHISEL_AND_RETURN;
		}
		if (hasFrags && !hasDark && !hasDense)
		{
			if (secondLoadReached)
			{
				// Both Loads are fully chiselled with nothing left to mine - go craft.
				return RotationStep.GO_ALTAR;
			}
			return atMine ? RotationStep.MINE_SECOND : RotationStep.RETURN_TO_MINE;
		}
		if (!atMine && hasDense && !fullFragmentStack)
		{
			return hasFrags ? RotationStep.GO_DARK_SECOND : RotationStep.GO_DARK_FIRST;
		}
		return hasFrags ? RotationStep.MINE_SECOND : RotationStep.MINE_FIRST;
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

	/**
	 * True once inferring this step proves the Second Load has actually been mined, not merely
	 * prompted. MINE_SECOND is just the "go mine now" instruction shown while still empty-handed -
	 * treating it as reached would flip secondLoadReached before any mining happens, making the
	 * very next tick's identical empty-handed state (still no Dense/Dark, just Fragments) misread
	 * as the Second Load already being finished too (GitHub issue #9).
	 */
	static boolean marksSecondLoadReached(RotationStep step)
	{
		return step == RotationStep.GO_DARK_SECOND;
	}
}
