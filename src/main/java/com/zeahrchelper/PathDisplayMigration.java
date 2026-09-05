package com.zeahrchelper;

import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;

/**
 * Folds the old "show path on floor" and "show path on minimap" checkboxes into the single
 * {@link PathDisplay} dropdown, so upgrading does not silently reset either choice.
 */
@Slf4j
@Singleton
class PathDisplayMigration
{
	private final ConfigManager configManager;

	@Inject
	PathDisplayMigration(ConfigManager configManager)
	{
		this.configManager = configManager;
	}

	void run()
	{
		if (configManager.getConfiguration(ZeahRcHelperConfig.GROUP, ZeahRcHelperConfig.PATH_DISPLAY_KEY) != null)
		{
			return;
		}

		String floor = configManager.getConfiguration(
			ZeahRcHelperConfig.GROUP, ZeahRcHelperConfig.LEGACY_SHOW_PATH_KEY);
		String minimap = configManager.getConfiguration(
			ZeahRcHelperConfig.GROUP, ZeahRcHelperConfig.LEGACY_SHOW_MINIMAP_PATH_KEY);
		if (floor == null && minimap == null)
		{
			return;
		}

		PathDisplay display = displayFor(enabled(floor), enabled(minimap));
		configManager.setConfiguration(
			ZeahRcHelperConfig.GROUP, ZeahRcHelperConfig.PATH_DISPLAY_KEY, display);
		configManager.unsetConfiguration(
			ZeahRcHelperConfig.GROUP, ZeahRcHelperConfig.LEGACY_SHOW_PATH_KEY);
		configManager.unsetConfiguration(
			ZeahRcHelperConfig.GROUP, ZeahRcHelperConfig.LEGACY_SHOW_MINIMAP_PATH_KEY);
		log.debug("migrated path checkboxes to {}", display);
	}

	static PathDisplay displayFor(boolean floor, boolean minimap)
	{
		if (floor && minimap)
		{
			return PathDisplay.FLOOR_AND_MINIMAP;
		}
		if (floor)
		{
			return PathDisplay.FLOOR;
		}
		if (minimap)
		{
			return PathDisplay.MINIMAP;
		}
		return PathDisplay.OFF;
	}

	/** Both checkboxes defaulted to on, so an absent key means the user never turned it off. */
	private static boolean enabled(String value)
	{
		return value == null || Boolean.parseBoolean(value);
	}
}
