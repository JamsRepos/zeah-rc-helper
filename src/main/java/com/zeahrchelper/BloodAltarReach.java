package com.zeahrchelper;

import net.runelite.api.coords.WorldPoint;

/**
 * Whether the Blood Altar can be clicked from the player's tile on the way from the Dark Altar.
 * Reach 46 is the measured client menu distance to the altar footprint.
 */
public final class BloodAltarReach
{
	static final int REACH = 46;

	enum State
	{
		NONE,
		/** Altar in scene and within reach — click it. */
		READY,
		/** Altar in scene but too far — stand on {@link ZeahRcArea#DARK_APPROACH} first. */
		STAND_THEN_CLICK,
		/** Altar not in the loaded scene — walk the normal blood corridor. */
		WALK
	}

	private BloodAltarReach()
	{
	}

	static State evaluate(RotationStep step, RcMode rune, WorldPoint tile, boolean altarInScene)
	{
		return evaluate(step, rune, tile, altarInScene, false);
	}

	/**
	 * @param altarClickCommitted true after the player has clicked the Blood Altar this step —
	 *                            keep highlighting the altar instead of the stand tile while they walk.
	 */
	static State evaluate(
		RotationStep step,
		RcMode rune,
		WorldPoint tile,
		boolean altarInScene,
		boolean altarClickCommitted)
	{
		if (rune != RcMode.BLOOD || tile == null || step != RotationStep.GO_ALTAR)
		{
			return State.NONE;
		}
		if (!altarInScene)
		{
			return State.WALK;
		}
		if (reachTo(tile) <= REACH || altarClickCommitted)
		{
			return State.READY;
		}
		return State.STAND_THEN_CLICK;
	}

	/** Chebyshev distance from a tile to the nearest tile of the Blood Altar's footprint. */
	static int reachTo(WorldPoint tile)
	{
		WorldPoint sw = ZeahRcArea.BLOOD_ALTAR_FOOTPRINT_SW;
		int max = ZeahRcArea.BLOOD_ALTAR_FOOTPRINT_SIZE - 1;
		int dx = Math.max(Math.max(sw.getX() - tile.getX(), tile.getX() - (sw.getX() + max)), 0);
		int dy = Math.max(Math.max(sw.getY() - tile.getY(), tile.getY() - (sw.getY() + max)), 0);
		return Math.max(dx, dy);
	}
}
