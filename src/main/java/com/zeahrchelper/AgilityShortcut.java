package com.zeahrchelper;

import java.util.List;
import lombok.Getter;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ObjectID;

/**
 * Arceuus RC agility hops. Stand/land tiles match Shortest Path
 * {@code agility_shortcuts.tsv} (BSD-2).
 */
@Getter
public enum AgilityShortcut
{
	BOULDER_49(
		new WorldPoint(1776, 3884, 0),
		new WorldPoint(1776, 3880, 0),
		49,
		false,
		ObjectID.ARCHEUUS_RUNESTONE_SHORTCUT_BOULDER,
		ObjectID.ARCHEUUS_RUNESTONE_SHORTCUT_BOULDER),
	EAST_52(
		new WorldPoint(1769, 3849, 0),
		new WorldPoint(1774, 3849, 0),
		52,
		true,
		ObjectID.ARCHEUUS_RUNESTONE_SHORTCUT_MIDGREY_BOTTOM,
		ObjectID.ARCHEUUS_RUNESTONE_SHORTCUT_MIDGREY_TOP),
	NORTH_69(
		new WorldPoint(1761, 3872, 0),
		new WorldPoint(1761, 3874, 0),
		69,
		true,
		34741,
		34741),
	WEST_73(
		new WorldPoint(1742, 3854, 0),
		new WorldPoint(1752, 3854, 0),
		73,
		false,
		ObjectID.ARCHEUUS_RUNESTONE_SHORTCUT_GREY_MIDDLE,
		ObjectID.ARCHEUUS_RUNESTONE_SHORTCUT_GREY_MIDDLE);

	/** Wiki/gameval id for the 69 northern scramble. ObjectID1 is package-private. */
	public static final int NORTH_OBJECT_ID = 34741;
	static final int PATH_DRIFT_TILES = 6;

	private final WorldPoint from;
	private final WorldPoint to;
	private final int minAgility;
	private final boolean bidirectional;
	private final int forwardObjectId;
	private final int reverseObjectId;

	AgilityShortcut(
		WorldPoint from,
		WorldPoint to,
		int minAgility,
		boolean bidirectional,
		int forwardObjectId,
		int reverseObjectId)
	{
		this.from = from;
		this.to = to;
		this.minAgility = minAgility;
		this.bidirectional = bidirectional;
		this.forwardObjectId = forwardObjectId;
		this.reverseObjectId = reverseObjectId;
	}

	public static boolean hopBetween(WorldPoint a, WorldPoint b)
	{
		return match(a, b) != null;
	}

	public static Hop firstHop(List<WorldPoint> path)
	{
		return firstHop(path, null);
	}

	public static Hop firstHop(List<WorldPoint> path, WorldPoint player)
	{
		if (path == null || path.size() < 2)
		{
			return null;
		}

		int start = 0;
		if (player != null)
		{
			int best = 0;
			int bestDist = Integer.MAX_VALUE;
			for (int i = 0; i < path.size(); i++)
			{
				int dist = player.distanceTo(path.get(i));
				if (dist < bestDist)
				{
					bestDist = dist;
					best = i;
				}
			}
			if (bestDist <= PATH_DRIFT_TILES)
			{
				start = best;
			}
		}

		for (int i = start; i < path.size() - 1; i++)
		{
			WorldPoint origin = path.get(i);
			WorldPoint dest = path.get(i + 1);
			if (hopBetween(origin, dest))
			{
				return new Hop(origin, dest);
			}
		}
		return null;
	}

	public static int objectIdForHop(WorldPoint origin, WorldPoint dest)
	{
		AgilityShortcut shortcut = match(origin, dest);
		if (shortcut == null)
		{
			return -1;
		}
		if (origin.equals(shortcut.from) && dest.equals(shortcut.to))
		{
			return shortcut.forwardObjectId;
		}
		return shortcut.reverseObjectId;
	}

	public boolean enabled(int agility, RotationStep step, WorldPoint player, RcMode mode, boolean atMine)
	{
		if (agility < minAgility || player == null)
		{
			return false;
		}

		switch (this)
		{
			case NORTH_69:
				return step != RotationStep.GO_ALTAR && !craftingAtAltar(step);
			case WEST_73:
				return returningToMine(step) && isFromBloodAltar(player);
			case BOULDER_49:
				return returningToMine(step) && isFromSoulApproach(player);
			case EAST_52:
				if (player.getY() > 3875)
				{
					return false;
				}
				if (isBloodWestWalk(step, player, mode))
				{
					return false;
				}
				if (step == RotationStep.GO_ALTAR)
				{
					return atMine || player.getX() > 1768;
				}
				return true;
			default:
				return false;
		}
	}

	static boolean returningToMine(RotationStep step)
	{
		return step == RotationStep.RETURN_TO_MINE
			|| step == RotationStep.CHISEL_AND_RETURN
			|| step == RotationStep.MINE_FIRST
			|| step == RotationStep.MINE_SECOND;
	}

	static boolean craftingAtAltar(RotationStep step)
	{
		return step == RotationStep.CRAFT_FRAGMENTS
			|| step == RotationStep.CRAFT_REMAINING
			|| step == RotationStep.CHISEL_AT_ALTAR;
	}

	static boolean isFromBloodAltar(WorldPoint player)
	{
		return player.getX() < 1743 && player.getY() < 3860;
	}

	static boolean isFromSoulApproach(WorldPoint player)
	{
		// North of the boulder (including the 1776,3884 stand tile) or still east of it.
		// After the hop (y <= 3880) this is false so the mine uses 69, not a reverse jump.
		return player.getX() > 1776 || player.getY() >= 3884;
	}

	static boolean isBloodWestWalk(RotationStep step, WorldPoint player, RcMode mode)
	{
		return step == RotationStep.GO_ALTAR
			&& mode == RcMode.BLOOD
			&& player.getY() >= 3836
			&& player.getX() <= 1745;
	}

	private static AgilityShortcut match(WorldPoint a, WorldPoint b)
	{
		if (a == null || b == null)
		{
			return null;
		}
		for (AgilityShortcut shortcut : values())
		{
			if (a.equals(shortcut.from) && b.equals(shortcut.to))
			{
				return shortcut;
			}
			if (shortcut.bidirectional && a.equals(shortcut.to) && b.equals(shortcut.from))
			{
				return shortcut;
			}
		}
		return null;
	}

	@Getter
	public static final class Hop
	{
		private final WorldPoint from;
		private final WorldPoint to;

		Hop(WorldPoint from, WorldPoint to)
		{
			this.from = from;
			this.to = to;
		}
	}
}
