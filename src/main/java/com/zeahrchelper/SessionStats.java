package com.zeahrchelper;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import net.runelite.client.config.ConfigManager;

/**
 * Session counters last for the client process (survive logout). Lifetime totals are stored on the
 * RuneLite RS profile and survive client restarts.
 * <p>
 * Crafted runes are credited only when the fragment stack leaves the inventory and blood/soul
 * quantity rises. Drop/pickup of runes alone does not remove fragments, so it is ignored.
 */
@Singleton
public class SessionStats
{
	static final String KEY_TOTAL_TRIPS = "lifetimeTrips";
	static final String KEY_TOTAL_BLOOD = "lifetimeBloodRunes";
	static final String KEY_TOTAL_SOUL = "lifetimeSoulRunes";
	/** Ticks to wait for rune qty after fragments leave (split inventory updates). */
	private static final int PENDING_TICKS = 5;

	@Nullable
	private final ConfigManager configManager;

	@Getter
	private int tripsCompleted;
	@Getter
	private int bloodRunesCrafted;
	@Getter
	private int soulRunesCrafted;

	@Getter
	private int totalTrips;
	@Getter
	private int totalBloodRunes;
	@Getter
	private int totalSoulRunes;

	@Getter
	private int lastBloodQty = -1;
	@Getter
	private int lastSoulQty = -1;
	@Getter
	private boolean lastHadFragments;

	/** Fragments just left; waiting for the matching rune stack to rise. */
	private boolean waitingForRuneGain;
	private int waitBloodBaseline = -1;
	private int waitSoulBaseline = -1;
	private int pendingTicksRemaining;

	@Inject
	SessionStats(ConfigManager configManager)
	{
		this.configManager = configManager;
		reloadTotalsFromProfile();
	}

	SessionStats()
	{
		this.configManager = null;
	}

	public void reloadTotalsFromProfile()
	{
		totalTrips = readTotal(KEY_TOTAL_TRIPS);
		totalBloodRunes = readTotal(KEY_TOTAL_BLOOD);
		totalSoulRunes = readTotal(KEY_TOTAL_SOUL);
	}

	public void resetAllCounters()
	{
		tripsCompleted = 0;
		bloodRunesCrafted = 0;
		soulRunesCrafted = 0;
		totalTrips = 0;
		totalBloodRunes = 0;
		totalSoulRunes = 0;
		writeTotal(KEY_TOTAL_TRIPS, 0);
		writeTotal(KEY_TOTAL_BLOOD, 0);
		writeTotal(KEY_TOTAL_SOUL, 0);
		clearPending();
	}

	public void noteTripComplete()
	{
		tripsCompleted++;
		totalTrips++;
		writeTotal(KEY_TOTAL_TRIPS, totalTrips);
	}

	/**
	 * @param hasFragments whether a dark essence fragment stack is currently in the inventory
	 */
	public void noteRuneInventory(int bloodQty, int soulQty, boolean hasFragments)
	{
		bloodQty = Math.max(0, bloodQty);
		soulQty = Math.max(0, soulQty);

		if (lastBloodQty >= 0 && lastSoulQty >= 0 && lastHadFragments && !hasFragments)
		{
			int bloodDelta = bloodQty - lastBloodQty;
			int soulDelta = soulQty - lastSoulQty;
			if (bloodDelta > 0)
			{
				addBlood(bloodDelta);
				clearPending();
			}
			else if (soulDelta > 0)
			{
				addSoul(soulDelta);
				clearPending();
			}
			else
			{
				waitingForRuneGain = true;
				waitBloodBaseline = lastBloodQty;
				waitSoulBaseline = lastSoulQty;
				pendingTicksRemaining = PENDING_TICKS;
			}
		}
		else if (waitingForRuneGain)
		{
			if (waitBloodBaseline >= 0 && bloodQty > waitBloodBaseline)
			{
				addBlood(bloodQty - waitBloodBaseline);
				clearPending();
			}
			else if (waitSoulBaseline >= 0 && soulQty > waitSoulBaseline)
			{
				addSoul(soulQty - waitSoulBaseline);
				clearPending();
			}
		}

		lastBloodQty = bloodQty;
		lastSoulQty = soulQty;
		lastHadFragments = hasFragments;
	}

	/** Call after inventory has been processed for this tick. */
	public void onGameTick()
	{
		if (!waitingForRuneGain || pendingTicksRemaining <= 0)
		{
			return;
		}
		pendingTicksRemaining--;
		if (pendingTicksRemaining <= 0)
		{
			clearPending();
		}
	}

	public void clearBaselines()
	{
		lastBloodQty = -1;
		lastSoulQty = -1;
		lastHadFragments = false;
		clearPending();
	}

	private void addBlood(int delta)
	{
		bloodRunesCrafted += delta;
		totalBloodRunes += delta;
		writeTotal(KEY_TOTAL_BLOOD, totalBloodRunes);
	}

	private void addSoul(int delta)
	{
		soulRunesCrafted += delta;
		totalSoulRunes += delta;
		writeTotal(KEY_TOTAL_SOUL, totalSoulRunes);
	}

	private void clearPending()
	{
		waitingForRuneGain = false;
		waitBloodBaseline = -1;
		waitSoulBaseline = -1;
		pendingTicksRemaining = 0;
	}

	private int readTotal(String key)
	{
		if (configManager == null)
		{
			return 0;
		}
		try
		{
			Integer stored = configManager.getRSProfileConfiguration(
				ZeahRcHelperConfig.GROUP, key, Integer.class);
			return stored == null ? 0 : Math.max(0, stored);
		}
		catch (Exception ex)
		{
			return 0;
		}
	}

	private void writeTotal(String key, int value)
	{
		if (configManager == null)
		{
			return;
		}
		try
		{
			configManager.setRSProfileConfiguration(ZeahRcHelperConfig.GROUP, key, value);
		}
		catch (Exception ignored)
		{
		}
	}
}
