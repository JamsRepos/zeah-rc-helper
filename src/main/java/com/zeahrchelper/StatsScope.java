package com.zeahrchelper;

import net.runelite.client.util.QuantityFormatter;

/**
 * Which counters the status panel Stats section shows: this client session, all-time total, or both.
 */
public enum StatsScope
{
	SESSION("Session"),
	TOTAL("Total"),
	SESSION_AND_TOTAL("Session/Total");

	private final String label;

	StatsScope(String label)
	{
		this.label = label;
	}

	/** Text inside {@code Stats (...)} on the panel. */
	public String panelSuffix()
	{
		return label;
	}

	public String format(int session, int total)
	{
		switch (this)
		{
			case SESSION:
				return formatCount(session);
			case TOTAL:
				return formatCount(total);
			case SESSION_AND_TOTAL:
			default:
				return formatCount(session) + "/" + formatCount(total);
		}
	}

	static String formatCount(int count)
	{
		return QuantityFormatter.quantityToStackSize((long) Math.max(0, count));
	}

	@Override
	public String toString()
	{
		return label;
	}
}
