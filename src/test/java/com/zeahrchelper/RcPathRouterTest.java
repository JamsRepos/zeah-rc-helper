package com.zeahrchelper;

import java.util.Arrays;
import java.util.List;
import net.runelite.api.coords.WorldPoint;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class RcPathRouterTest
{
	private final RcPathRouter router = new RcPathRouter(new SceneTracker(null));

	@Test
	public void nextClickDoesNotInventAShortcutWhenPathHasNoHop()
	{
		WorldPoint player = ZeahRcArea.MINE_STAND;
		List<WorldPoint> path = Arrays.asList(player, new WorldPoint(1750, 3860, 0), ZeahRcArea.DARK_ALTAR);

		RcPathRouter.ClickTarget click = router.nextClick(
			RotationStep.GO_DARK_FIRST, null, path, player, true);

		assertNull(click.getObject());
		assertNull(click.getTile());
		assertNull(AgilityShortcut.firstHop(path, player));
	}

	@Test
	public void nextClickFallsBackToFirstHopStandTileWhenObjectMissing()
	{
		WorldPoint player = new WorldPoint(1761, 3868, 0);
		List<WorldPoint> path = Arrays.asList(
			player,
			AgilityShortcut.NORTH_69.getFrom(),
			AgilityShortcut.NORTH_69.getTo(),
			ZeahRcArea.DARK_ALTAR);

		RcPathRouter.ClickTarget click = router.nextClick(
			RotationStep.GO_DARK_FIRST, null, path, player, true);

		assertNull(click.getObject());
		assertEquals(AgilityShortcut.NORTH_69.getFrom(), click.getTile());
	}

	@Test
	public void nextClickUsesFirstHopWhenTwoHopsRemain()
	{
		WorldPoint player = new WorldPoint(1776, 3888, 0);
		List<WorldPoint> path = Arrays.asList(
			player,
			AgilityShortcut.BOULDER_49.getFrom(),
			AgilityShortcut.BOULDER_49.getTo(),
			AgilityShortcut.NORTH_69.getFrom(),
			AgilityShortcut.NORTH_69.getTo());

		RcPathRouter.ClickTarget click = router.nextClick(
			RotationStep.RETURN_TO_MINE, null, path, player, false);

		assertEquals(AgilityShortcut.BOULDER_49.getFrom(), click.getTile());
	}

	@Test
	public void trimThenFirstHopSkipsPassedShortcut()
	{
		List<WorldPoint> path = Arrays.asList(
			AgilityShortcut.BOULDER_49.getFrom(),
			AgilityShortcut.BOULDER_49.getTo(),
			new WorldPoint(1768, 3870, 0),
			AgilityShortcut.NORTH_69.getFrom(),
			AgilityShortcut.NORTH_69.getTo());

		List<WorldPoint> trimmed = RcPathRouter.trimPath(path, new WorldPoint(1768, 3870, 0));
		AgilityShortcut.Hop hop = AgilityShortcut.firstHop(trimmed, new WorldPoint(1768, 3870, 0));
		assertEquals(AgilityShortcut.NORTH_69.getFrom(), hop.getFrom());
	}

	@Test
	public void goAltarTransportsOmit69And73And49()
	{
		List<Pathfinder.Transport> fromDark = RcPathRouter.transports(
			99, RotationStep.GO_ALTAR, ZeahRcArea.DARK_ALTAR, RcMode.BLOOD, false);
		assertTrue(fromDark.isEmpty());

		List<Pathfinder.Transport> fromSoul = RcPathRouter.transports(
			99, RotationStep.GO_ALTAR, new WorldPoint(1786, 3895, 0), RcMode.SOUL, false);
		assertFalse(contains(fromSoul, AgilityShortcut.NORTH_69));
		assertFalse(contains(fromSoul, AgilityShortcut.WEST_73));
		assertFalse(contains(fromSoul, AgilityShortcut.BOULDER_49));
	}

	@Test
	public void soulFromMineEnables52Only()
	{
		List<Pathfinder.Transport> transports = RcPathRouter.transports(
			99, RotationStep.GO_ALTAR, ZeahRcArea.MINE_STAND, RcMode.SOUL, true);
		assertTrue(contains(transports, AgilityShortcut.EAST_52));
		assertFalse(contains(transports, AgilityShortcut.NORTH_69));
		assertFalse(contains(transports, AgilityShortcut.WEST_73));
		assertFalse(contains(transports, AgilityShortcut.BOULDER_49));
	}

	@Test
	public void soulReturnNearBoulderEnables49()
	{
		List<Pathfinder.Transport> onStand = RcPathRouter.transports(
			99, RotationStep.RETURN_TO_MINE, AgilityShortcut.BOULDER_49.getFrom(), RcMode.SOUL, false);
		assertTrue(contains(onStand, AgilityShortcut.BOULDER_49));

		List<Pathfinder.Transport> onRidge = RcPathRouter.transports(
			99, RotationStep.CHISEL_AND_RETURN, new WorldPoint(1774, 3886, 0), RcMode.SOUL, false);
		assertTrue(contains(onRidge, AgilityShortcut.BOULDER_49));
	}

	@Test
	public void bloodReturnEnables73()
	{
		List<Pathfinder.Transport> transports = RcPathRouter.transports(
			99, RotationStep.RETURN_TO_MINE, ZeahRcArea.BLOOD_ALTAR, RcMode.BLOOD, false);
		assertTrue(contains(transports, AgilityShortcut.WEST_73));
		assertTrue(AgilityShortcut.WEST_73.enabled(
			99, RotationStep.RETURN_TO_MINE, ZeahRcArea.BLOOD_ALTAR, RcMode.BLOOD, false));
	}

	@Test
	public void bloodApproachStaysOnUntilNearAltar()
	{
		assertTrue(RcPathRouter.useBloodApproach(
			RotationStep.GO_ALTAR, ZeahRcArea.DARK_ALTAR, RcMode.BLOOD));
		assertTrue(RcPathRouter.useBloodApproach(
			RotationStep.GO_ALTAR, new WorldPoint(1735, 3830, 0), RcMode.BLOOD));
		assertFalse(RcPathRouter.useBloodApproach(
			RotationStep.GO_ALTAR, new WorldPoint(1718, 3832, 0), RcMode.BLOOD));
		assertFalse(RcPathRouter.useBloodApproach(
			RotationStep.GO_ALTAR, ZeahRcArea.MINE_STAND, RcMode.BLOOD));
	}

	@Test
	public void bloodCorridorDoesNotSnapBackwardWhileRunningSouth()
	{
		List<WorldPoint> corridor = ZeahRcArea.BLOOD_APPROACH;
		assertEquals(0, RcPathRouter.firstRemainingWaypoint(ZeahRcArea.DARK_ALTAR, corridor));
		assertEquals(corridor.indexOf(new WorldPoint(1735, 3828, 0)),
			RcPathRouter.firstRemainingWaypoint(new WorldPoint(1735, 3840, 0), corridor));
		assertEquals(corridor.size(),
			RcPathRouter.firstRemainingWaypoint(new WorldPoint(1717, 3826, 0), corridor));
	}

	@Test
	public void darkApproachUsedOnBloodDarkStepsUntilNearStandTile()
	{
		assertTrue(RcPathRouter.useDarkApproach(
			RotationStep.GO_DARK_SECOND, ZeahRcArea.MINE_STAND, RcMode.BLOOD));
		assertTrue(RcPathRouter.useDarkApproach(
			RotationStep.GO_DARK_FIRST, ZeahRcArea.NORTH_SHORTCUT, RcMode.BLOOD));
		assertFalse(RcPathRouter.useDarkApproach(
			RotationStep.GO_DARK_SECOND, ZeahRcArea.DARK_APPROACH, RcMode.BLOOD));
		assertFalse(RcPathRouter.useDarkApproach(
			RotationStep.GO_DARK_SECOND, ZeahRcArea.DARK_ALTAR, RcMode.BLOOD));
		assertFalse(RcPathRouter.useDarkApproach(
			RotationStep.GO_DARK_SECOND, ZeahRcArea.MINE_STAND, RcMode.SOUL));
		assertFalse(RcPathRouter.useDarkApproach(
			RotationStep.GO_ALTAR, ZeahRcArea.MINE_STAND, RcMode.BLOOD));
	}

	@Test
	public void chiselAndIdleHaveNoShortcutClick()
	{
		List<WorldPoint> path = Arrays.asList(
			AgilityShortcut.NORTH_69.getFrom(),
			AgilityShortcut.NORTH_69.getTo());
		assertNull(router.nextClick(RotationStep.CHISEL_AT_ALTAR, null, path, ZeahRcArea.DARK_ALTAR, false).getTile());
		assertNull(router.nextClick(RotationStep.IDLE, null, path, ZeahRcArea.MINE_STAND, true).getTile());
	}

	private static boolean contains(List<Pathfinder.Transport> transports, AgilityShortcut shortcut)
	{
		for (int i = 0; i < transports.size(); i++)
		{
			Pathfinder.Transport transport = transports.get(i);
			if (transport.from.equals(shortcut.getFrom()) && transport.to.equals(shortcut.getTo()))
			{
				return true;
			}
			if (shortcut.isBidirectional()
				&& transport.from.equals(shortcut.getTo()) && transport.to.equals(shortcut.getFrom()))
			{
				return true;
			}
		}
		return false;
	}
}
