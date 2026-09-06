package com.zeahrchelper;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class StatsScopeTest
{
	@Test
	public void formatsSessionTotalAndBoth()
	{
		assertEquals("12", StatsScope.SESSION.format(12, 99));
		assertEquals("99", StatsScope.TOTAL.format(12, 99));
		assertEquals("12/99", StatsScope.SESSION_AND_TOTAL.format(12, 99));
	}

	@Test
	public void panelSuffixMatchesDropdown()
	{
		assertEquals("Session", StatsScope.SESSION.panelSuffix());
		assertEquals("Total", StatsScope.TOTAL.panelSuffix());
		assertEquals("Session/Total", StatsScope.SESSION_AND_TOTAL.panelSuffix());
	}
}
