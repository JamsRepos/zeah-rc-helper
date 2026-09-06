package com.zeahrchelper;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class SessionStatsTest
{
	@Test
	public void fragmentDepleteAndRuneGainCreditsSoul()
	{
		SessionStats stats = new SessionStats();
		stats.noteRuneInventory(0, 50, true);
		stats.noteRuneInventory(0, 170, false);
		assertEquals(120, stats.getSoulRunesCrafted());
		assertEquals(0, stats.getBloodRunesCrafted());
	}

	@Test
	public void fragmentDepleteAndRuneGainCreditsBlood()
	{
		SessionStats stats = new SessionStats();
		stats.noteRuneInventory(10, 0, true);
		stats.noteRuneInventory(130, 0, false);
		assertEquals(120, stats.getBloodRunesCrafted());
		assertEquals(0, stats.getSoulRunesCrafted());
	}

	@Test
	public void splitUpdatesCreditWhenRunesLag()
	{
		SessionStats stats = new SessionStats();
		stats.noteRuneInventory(0, 50, true);
		stats.noteRuneInventory(0, 50, false);
		assertEquals(0, stats.getSoulRunesCrafted());
		stats.noteRuneInventory(0, 170, false);
		assertEquals(120, stats.getSoulRunesCrafted());
	}

	@Test
	public void pendingExpiresWithoutRuneGain()
	{
		SessionStats stats = new SessionStats();
		stats.noteRuneInventory(0, 50, true);
		stats.noteRuneInventory(0, 50, false);
		for (int i = 0; i < 5; i++)
		{
			stats.onGameTick();
		}
		stats.noteRuneInventory(0, 170, false);
		assertEquals(0, stats.getSoulRunesCrafted());
	}

	@Test
	public void dropPickupDoesNotCount()
	{
		SessionStats stats = new SessionStats();
		stats.noteRuneInventory(0, 500, false);
		stats.noteRuneInventory(0, 0, false);
		stats.noteRuneInventory(0, 500, false);
		assertEquals(0, stats.getSoulRunesCrafted());
	}

	@Test
	public void fragmentDepleteWithoutRuneGainDoesNotCount()
	{
		SessionStats stats = new SessionStats();
		stats.noteRuneInventory(0, 50, true);
		stats.noteRuneInventory(0, 50, false);
		assertEquals(0, stats.getSoulRunesCrafted());
	}

	@Test
	public void firstSnapshotDoesNotCountEntireStack()
	{
		SessionStats stats = new SessionStats();
		stats.noteRuneInventory(0, 5000, false);
		assertEquals(0, stats.getSoulRunesCrafted());
	}

	@Test
	public void secondSoulCraftAdds()
	{
		SessionStats stats = new SessionStats();
		stats.noteRuneInventory(0, 0, true);
		stats.noteRuneInventory(0, 100, false);
		stats.noteRuneInventory(0, 100, true);
		stats.noteRuneInventory(0, 180, false);
		assertEquals(180, stats.getSoulRunesCrafted());
	}

	@Test
	public void tripCompleteIncrementsSessionAndTotal()
	{
		SessionStats stats = new SessionStats();
		stats.noteTripComplete();
		assertEquals(1, stats.getTripsCompleted());
		assertEquals(1, stats.getTotalTrips());
	}
}
