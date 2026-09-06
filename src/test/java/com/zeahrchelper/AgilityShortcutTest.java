package com.zeahrchelper;

import java.util.Arrays;
import java.util.List;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ObjectID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class AgilityShortcutTest
{
	@Test
	public void hopBetweenMatchesAllShortcutsIncludingReverse52AndTiny69()
	{
		assertTrue(AgilityShortcut.hopBetween(
			AgilityShortcut.BOULDER_49.getFrom(), AgilityShortcut.BOULDER_49.getTo()));
		assertTrue(AgilityShortcut.hopBetween(
			AgilityShortcut.EAST_52.getFrom(), AgilityShortcut.EAST_52.getTo()));
		assertTrue(AgilityShortcut.hopBetween(
			AgilityShortcut.EAST_52.getTo(), AgilityShortcut.EAST_52.getFrom()));
		assertTrue(AgilityShortcut.hopBetween(
			AgilityShortcut.NORTH_69.getFrom(), AgilityShortcut.NORTH_69.getTo()));
		assertTrue(AgilityShortcut.hopBetween(
			AgilityShortcut.NORTH_69.getTo(), AgilityShortcut.NORTH_69.getFrom()));
		assertTrue(AgilityShortcut.hopBetween(
			AgilityShortcut.WEST_73.getFrom(), AgilityShortcut.WEST_73.getTo()));
	}

	@Test
	public void oneWayShortcutsAreNotReversible()
	{
		assertFalse(AgilityShortcut.hopBetween(
			AgilityShortcut.BOULDER_49.getTo(), AgilityShortcut.BOULDER_49.getFrom()));
		assertFalse(AgilityShortcut.hopBetween(
			AgilityShortcut.WEST_73.getTo(), AgilityShortcut.WEST_73.getFrom()));
	}

	@Test
	public void objectIdFollows52Direction()
	{
		assertEquals(
			ObjectID.ARCHEUUS_RUNESTONE_SHORTCUT_MIDGREY_BOTTOM,
			AgilityShortcut.objectIdForHop(AgilityShortcut.EAST_52.getFrom(), AgilityShortcut.EAST_52.getTo()));
		assertEquals(
			ObjectID.ARCHEUUS_RUNESTONE_SHORTCUT_MIDGREY_TOP,
			AgilityShortcut.objectIdForHop(AgilityShortcut.EAST_52.getTo(), AgilityShortcut.EAST_52.getFrom()));
		assertEquals(
			AgilityShortcut.NORTH_OBJECT_ID,
			AgilityShortcut.objectIdForHop(AgilityShortcut.NORTH_69.getFrom(), AgilityShortcut.NORTH_69.getTo()));
	}

	@Test
	public void firstHopSkipsHopsAlreadyBehindThePlayer()
	{
		WorldPoint before = new WorldPoint(1776, 3888, 0);
		WorldPoint afterBoulder = new WorldPoint(1776, 3878, 0);
		WorldPoint before69 = new WorldPoint(1761, 3870, 0);
		List<WorldPoint> path = Arrays.asList(
			before,
			AgilityShortcut.BOULDER_49.getFrom(),
			AgilityShortcut.BOULDER_49.getTo(),
			afterBoulder,
			before69,
			AgilityShortcut.NORTH_69.getFrom(),
			AgilityShortcut.NORTH_69.getTo());

		AgilityShortcut.Hop first = AgilityShortcut.firstHop(path);
		assertNotNull(first);
		assertEquals(AgilityShortcut.BOULDER_49.getFrom(), first.getFrom());

		AgilityShortcut.Hop remaining = AgilityShortcut.firstHop(path, afterBoulder);
		assertNotNull(remaining);
		assertEquals(AgilityShortcut.NORTH_69.getFrom(), remaining.getFrom());
		assertEquals(AgilityShortcut.NORTH_69.getTo(), remaining.getTo());
	}

	@Test
	public void craftAtBloodAltarDoesNotEnable73()
	{
		WorldPoint blood = ZeahRcArea.BLOOD_ALTAR;
		assertFalse(AgilityShortcut.WEST_73.enabled(99, RotationStep.CRAFT_FRAGMENTS, blood, RcMode.BLOOD, false));
		assertFalse(AgilityShortcut.WEST_73.enabled(99, RotationStep.CHISEL_AT_ALTAR, blood, RcMode.BLOOD, false));
		assertFalse(AgilityShortcut.WEST_73.enabled(99, RotationStep.CRAFT_REMAINING, blood, RcMode.BLOOD, false));
		assertTrue(AgilityShortcut.WEST_73.enabled(99, RotationStep.RETURN_TO_MINE, blood, RcMode.BLOOD, false));
	}

	@Test
	public void goAltarDoesNotEnable69Or73Or49()
	{
		WorldPoint dark = ZeahRcArea.DARK_ALTAR;
		WorldPoint soulApproach = new WorldPoint(1786, 3895, 0);
		WorldPoint blood = ZeahRcArea.BLOOD_ALTAR;

		assertFalse(AgilityShortcut.NORTH_69.enabled(99, RotationStep.GO_ALTAR, dark, RcMode.BLOOD, false));
		assertFalse(AgilityShortcut.WEST_73.enabled(99, RotationStep.GO_ALTAR, blood, RcMode.BLOOD, false));
		assertFalse(AgilityShortcut.BOULDER_49.enabled(99, RotationStep.GO_ALTAR, soulApproach, RcMode.SOUL, false));
	}

	@Test
	public void boulderStaysEnabledOnTheStandTileAndNorthRidge()
	{
		WorldPoint stand = AgilityShortcut.BOULDER_49.getFrom();
		WorldPoint justWest = new WorldPoint(1774, 3886, 0);
		WorldPoint soulAltar = ZeahRcArea.SOUL_ALTAR;
		WorldPoint landed = AgilityShortcut.BOULDER_49.getTo();
		WorldPoint mine = ZeahRcArea.MINE_STAND;

		assertTrue(AgilityShortcut.BOULDER_49.enabled(99, RotationStep.RETURN_TO_MINE, stand, RcMode.SOUL, false));
		assertTrue(AgilityShortcut.BOULDER_49.enabled(99, RotationStep.RETURN_TO_MINE, justWest, RcMode.SOUL, false));
		assertTrue(AgilityShortcut.BOULDER_49.enabled(99, RotationStep.RETURN_TO_MINE, soulAltar, RcMode.SOUL, false));
		assertFalse(AgilityShortcut.BOULDER_49.enabled(99, RotationStep.RETURN_TO_MINE, landed, RcMode.SOUL, false));
		assertFalse(AgilityShortcut.BOULDER_49.enabled(99, RotationStep.RETURN_TO_MINE, mine, RcMode.SOUL, true));
	}

	@Test
	public void east52DisabledOnBloodWestCorridorAndEnabledFromMineOnSoul()
	{
		WorldPoint dark = ZeahRcArea.DARK_ALTAR;
		WorldPoint mine = ZeahRcArea.MINE_STAND;
		assertFalse(AgilityShortcut.EAST_52.enabled(99, RotationStep.GO_ALTAR, dark, RcMode.BLOOD, false));
		assertTrue(AgilityShortcut.EAST_52.enabled(99, RotationStep.GO_ALTAR, mine, RcMode.SOUL, true));
	}
}
