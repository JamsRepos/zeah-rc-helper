package com.zeahrchelper;

import net.runelite.api.coords.WorldPoint;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class BloodAltarReachTest
{
	private static final WorldPoint MINE_EDGE_IN = new WorldPoint(1765, 3855, 0);
	private static final WorldPoint MINE_EDGE_OUT = new WorldPoint(1766, 3855, 0);
	private static final WorldPoint SOUTH_OF_DARK_ALTAR = ZeahRcArea.DARK_APPROACH;
	private static final WorldPoint ONE_TILE_SHORT = new WorldPoint(1723, 3879, 0);
	private static final WorldPoint DARK_ALTAR = ZeahRcArea.DARK_ALTAR;

	@Test
	public void reachIsMeasuredToTheNearestFootprintTile()
	{
		assertEquals(46, BloodAltarReach.reachTo(MINE_EDGE_IN));
		assertEquals(46, BloodAltarReach.reachTo(SOUTH_OF_DARK_ALTAR));
		assertEquals(47, BloodAltarReach.reachTo(MINE_EDGE_OUT));
		assertEquals(47, BloodAltarReach.reachTo(ONE_TILE_SHORT));
		assertEquals(50, BloodAltarReach.reachTo(DARK_ALTAR));
		assertEquals(0, BloodAltarReach.reachTo(ZeahRcArea.BLOOD_ALTAR));
	}

	@Test
	public void goAltarReadyWhenInReachAndLoaded()
	{
		assertEquals(BloodAltarReach.State.READY, at(SOUTH_OF_DARK_ALTAR, true));
		assertEquals(BloodAltarReach.State.READY, at(MINE_EDGE_IN, true));
	}

	@Test
	public void goAltarStandThenClickWhenOutsideReach()
	{
		assertEquals(BloodAltarReach.State.STAND_THEN_CLICK, at(ONE_TILE_SHORT, true));
		assertEquals(BloodAltarReach.State.STAND_THEN_CLICK, at(DARK_ALTAR, true));
	}

	@Test
	public void goAltarReadyAfterAltarClickEvenOutsideReach()
	{
		assertEquals(BloodAltarReach.State.READY,
			BloodAltarReach.evaluate(RotationStep.GO_ALTAR, RcMode.BLOOD, DARK_ALTAR, true, true));
	}

	@Test
	public void goAltarWalkWhenAltarNotLoaded()
	{
		assertEquals(BloodAltarReach.State.WALK, at(SOUTH_OF_DARK_ALTAR, false));
	}

	@Test
	public void otherStepsAndSoulsDoNotApply()
	{
		assertEquals(BloodAltarReach.State.NONE,
			BloodAltarReach.evaluate(RotationStep.GO_DARK_SECOND, RcMode.BLOOD, DARK_ALTAR, true));
		assertEquals(BloodAltarReach.State.NONE,
			BloodAltarReach.evaluate(RotationStep.GO_ALTAR, RcMode.SOUL, SOUTH_OF_DARK_ALTAR, true));
		assertEquals(BloodAltarReach.State.NONE,
			BloodAltarReach.evaluate(RotationStep.GO_ALTAR, RcMode.BLOOD, null, true));
	}

	private static BloodAltarReach.State at(WorldPoint tile, boolean altarInScene)
	{
		return BloodAltarReach.evaluate(RotationStep.GO_ALTAR, RcMode.BLOOD, tile, altarInScene);
	}
}
