package com.zeahrchelper;

import java.util.Arrays;
import java.util.List;
import net.runelite.api.coords.WorldPoint;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class PathfinderTest
{
	@Test
	public void simplifyKeepsAgilityHopEndpoints()
	{
		WorldPoint start = new WorldPoint(1815, 3854, 0);
		WorldPoint before = new WorldPoint(1776, 3890, 0);
		WorldPoint shortcut = new WorldPoint(1775, 3888, 0);
		WorldPoint landing = new WorldPoint(1773, 3883, 0);
		WorldPoint end = new WorldPoint(1761, 3853, 0);

		List<WorldPoint> simplified = Pathfinder.simplify(
			Arrays.asList(start, before, shortcut, landing, end));

		assertTrue(simplified.contains(shortcut));
		assertTrue(simplified.contains(landing));
		int hopAt = simplified.indexOf(shortcut);
		assertTrue(hopAt >= 0);
		assertEquals(landing, simplified.get(hopAt + 1));
		assertTrue(Pathfinder.isHop(shortcut, landing));
	}
}
