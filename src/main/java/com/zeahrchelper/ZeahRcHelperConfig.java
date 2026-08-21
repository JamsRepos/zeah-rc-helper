package com.zeahrchelper;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup("zeah-rc-helper")
public interface ZeahRcHelperConfig extends Config
{
	@ConfigSection(
		name = "Helper",
		description = "Click-here rotation guidance",
		position = 0
	)
	String helperSection = "helper";

	@ConfigSection(
		name = "Reminders",
		description = "Gear, blood essence, and idle warnings",
		position = 1
	)
	String reminderSection = "reminders";

	@ConfigItem(
		keyName = "mode",
		name = "Rune type",
		description = "Which altar to guide. Auto uses soul at 90 Runecraft, otherwise blood.",
		section = helperSection,
		position = 0
	)
	default RcMode mode()
	{
		return RcMode.AUTO;
	}

	@ConfigItem(
		keyName = "enableHelper",
		name = "Enable helper",
		description = "Show the next-step overlay and status panel",
		section = helperSection,
		position = 1
	)
	default boolean enableHelper()
	{
		return true;
	}

	@ConfigItem(
		keyName = "highlightNextClick",
		name = "Highlight next click",
		description = "Highlight the next object's clickbox (runestone, shortcut, or altar)",
		section = helperSection,
		position = 2
	)
	default boolean highlightNextClick()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showPath",
		name = "Show path",
		description = "Draw a line on the floor to your destination, using agility shortcuts you qualify for.",
		section = helperSection,
		position = 3
	)
	default boolean showPath()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showStatusPanel",
		name = "Show status panel",
		description = "Show the current step, fragment/block counts, and trip counter",
		section = helperSection,
		position = 4
	)
	default boolean showStatusPanel()
	{
		return true;
	}

	@ConfigItem(
		keyName = "lanternReminder",
		name = "Gear reminders",
		description = "Remind you to bring a chisel (including jeweller's), a pickaxe, and an abyssal lantern",
		section = reminderSection,
		position = 0
	)
	default boolean lanternReminder()
	{
		return true;
	}

	@ConfigItem(
		keyName = "lanternLogCheck",
		name = "Check lantern logs",
		description = "When gear reminders are on, warn if the lantern is unlit or using logs that don't help this method",
		section = reminderSection,
		position = 1
	)
	default boolean lanternLogCheck()
	{
		return true;
	}

	@ConfigItem(
		keyName = "bloodEssenceReminder",
		name = "Blood essence reminder",
		description = "Remind you to bring and activate blood essence (blood mode only)",
		section = reminderSection,
		position = 2
	)
	default boolean bloodEssenceReminder()
	{
		return true;
	}

	@Range(min = 0, max = 1000)
	@ConfigItem(
		keyName = "bloodEssenceLowCharges",
		name = "Low essence charges",
		description = "Warn when activated blood essence is at or below this many charges",
		section = reminderSection,
		position = 3
	)
	default int bloodEssenceLowCharges()
	{
		return 100;
	}

	@Range(min = 3, max = 120)
	@ConfigItem(
		keyName = "idleReminderSeconds",
		name = "Idle reminder (seconds)",
		description = "Warn if you stand still in the Arceuus RC area this long",
		section = reminderSection,
		position = 4
	)
	default int idleReminderSeconds()
	{
		return 15;
	}

	@ConfigItem(
		keyName = "idleFlash",
		name = "Idle screen tint",
		description = "Gently tint the screen when idle. Off by default.",
		section = reminderSection,
		position = 5
	)
	default boolean idleFlash()
	{
		return false;
	}
}
