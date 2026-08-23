package com.zeahrchelper;

import java.util.Arrays;
import java.util.List;
import net.runelite.api.coords.WorldPoint;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class PathfinderTest
{
	@Test
	public void simplifyKeepsAgilityHopEndpoints()
	{
		WorldPoint start = new WorldPoint(1815, 3854, 0);
		WorldPoint before = new WorldPoint(1776, 3886, 0);
		WorldPoint shortcut = AgilityShortcut.BOULDER_49.getFrom();
		WorldPoint landing = AgilityShortcut.BOULDER_49.getTo();
		WorldPoint mid = new WorldPoint(1768, 3865, 0);
		WorldPoint end = new WorldPoint(1761, 3853, 0);

		List<WorldPoint> simplified = Pathfinder.simplify(
			Arrays.asList(start, before, shortcut, landing, mid, end));

		assertTrue(simplified.contains(shortcut));
		assertTrue(simplified.contains(landing));
		int hopAt = simplified.indexOf(shortcut);
		assertTrue(hopAt >= 0);
		assertEquals(landing, simplified.get(hopAt + 1));
		assertTrue(Pathfinder.isHop(shortcut, landing));
	}

	@Test
	public void simplifyKeepsTwoTileNorthClimb()
	{
		WorldPoint before = new WorldPoint(1761, 3868, 0);
		WorldPoint origin = AgilityShortcut.NORTH_69.getFrom();
		WorldPoint dest = AgilityShortcut.NORTH_69.getTo();
		WorldPoint after = new WorldPoint(1755, 3878, 0);

		List<WorldPoint> simplified = Pathfinder.simplify(Arrays.asList(before, origin, dest, after));

		assertTrue(simplified.contains(origin));
		assertTrue(simplified.contains(dest));
		assertTrue(AgilityShortcut.hopBetween(origin, dest));
	}

	@Test
	public void simplifyStillDropsCollinearWalkTiles()
	{
		WorldPoint a = new WorldPoint(1760, 3850, 0);
		WorldPoint b = new WorldPoint(1761, 3850, 0);
		WorldPoint c = new WorldPoint(1762, 3850, 0);
		List<WorldPoint> simplified = Pathfinder.simplify(Arrays.asList(a, b, c));
		assertEquals(Arrays.asList(a, c), simplified);
		assertFalse(simplified.contains(b));
	}

	@Test
	public void guessedOldBoulderTilesAreNotHops()
	{
		assertFalse(Pathfinder.isHop(new WorldPoint(1775, 3888, 0), new WorldPoint(1773, 3883, 0)));
	}
}
