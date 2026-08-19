package com.zeahrchelper;

import net.runelite.api.gameval.ItemID;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ReminderServiceTest
{
	@Test
	public void blisterwoodHelpsBloodsOnly()
	{
		assertTrue(ReminderService.isUsefulLantern(ItemID.ABYSSAL_LANTERN_BLISTERWOOD, RcMode.BLOOD));
		assertFalse(ReminderService.isUsefulLantern(ItemID.ABYSSAL_LANTERN_BLISTERWOOD, RcMode.SOUL));
	}

	@Test
	public void magicAndRedwoodHelpBoth()
	{
		assertTrue(ReminderService.isUsefulLantern(ItemID.ABYSSAL_LANTERN_MAGIC, RcMode.BLOOD));
		assertTrue(ReminderService.isUsefulLantern(ItemID.ABYSSAL_LANTERN_MAGIC, RcMode.SOUL));
		assertTrue(ReminderService.isUsefulLantern(ItemID.ABYSSAL_LANTERN_REDWOOD, RcMode.SOUL));
		assertTrue(ReminderService.isUsefulLantern(ItemID.ABYSSAL_LANTERN_WILLOW, RcMode.SOUL));
	}

	@Test
	public void gotrOnlyLogsDoNotHelpZeah()
	{
		assertFalse(ReminderService.isUsefulLantern(ItemID.ABYSSAL_LANTERN_NORMAL, RcMode.BLOOD));
		assertFalse(ReminderService.isUsefulLantern(ItemID.ABYSSAL_LANTERN_OAK, RcMode.SOUL));
		assertFalse(ReminderService.isUsefulLantern(ItemID.ABYSSAL_LANTERN_MAPLE, RcMode.BLOOD));
		assertFalse(ReminderService.isUsefulLantern(ItemID.ABYSSAL_LANTERN_YEW, RcMode.SOUL));
	}
}
