package com.zeahrchelper;

import java.util.List;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldPoint;

/**
 * Arceuus blood/soul runecrafting locations. Tiles are standing/click tiles for highlights;
 * they can be refined in-game if a highlight is a square off.
 */
public final class ZeahRcArea
{
	/** Dense essence mine, south runestone. */
	public static final WorldPoint RUNESTONE_SOUTH = new WorldPoint(1761, 3853, 0);
	/** Dense essence mine, north runestone. */
	public static final WorldPoint RUNESTONE_NORTH = new WorldPoint(1761, 3873, 0);
	public static final List<WorldPoint> RUNESTONES = List.of(RUNESTONE_SOUTH, RUNESTONE_NORTH);
	public static final WorldPoint MINE_STAND = new WorldPoint(1762, 3854, 0);
	public static final WorldPoint DARK_ALTAR = new WorldPoint(1718, 3882, 0);
	public static final WorldPoint BLOOD_ALTAR = new WorldPoint(1718, 3828, 0);
	public static final WorldPoint SOUL_ALTAR = new WorldPoint(1815, 3856, 0);
	/** 73 Agility rock scramble between the mine and Dark Altar. */
	public static final WorldPoint SHORTCUT = new WorldPoint(1743, 3854, 0);

	private static final int AREA_MIN_X = 1688;
	private static final int AREA_MAX_X = 1836;
	private static final int AREA_MIN_Y = 3810;
	private static final int AREA_MAX_Y = 3908;

	private ZeahRcArea()
	{
	}

	public static boolean isInArceuusRc(Client client)
	{
		WorldPoint loc = player(client);
		if (loc == null)
		{
			return false;
		}
		return loc.getX() >= AREA_MIN_X && loc.getX() <= AREA_MAX_X
			&& loc.getY() >= AREA_MIN_Y && loc.getY() <= AREA_MAX_Y;
	}

	public static boolean isAtMine(Client client)
	{
		WorldPoint loc = player(client);
		return loc != null && (loc.distanceTo(RUNESTONE_SOUTH) <= 10 || loc.distanceTo(RUNESTONE_NORTH) <= 10);
	}

	public static boolean isAtDarkAltar(Client client)
	{
		WorldPoint loc = player(client);
		return loc != null && loc.distanceTo(DARK_ALTAR) <= 8;
	}

	public static boolean isAtBloodAltar(Client client)
	{
		WorldPoint loc = player(client);
		return loc != null && loc.distanceTo(BLOOD_ALTAR) <= 8;
	}

	public static boolean isAtSoulAltar(Client client)
	{
		WorldPoint loc = player(client);
		return loc != null && loc.distanceTo(SOUL_ALTAR) <= 8;
	}

	public static boolean isAtCraftAltar(Client client, RcMode mode)
	{
		return mode == RcMode.SOUL ? isAtSoulAltar(client) : isAtBloodAltar(client);
	}

	private static WorldPoint player(Client client)
	{
		if (client.getLocalPlayer() == null)
		{
			return null;
		}
		return client.getLocalPlayer().getWorldLocation();
	}
}
