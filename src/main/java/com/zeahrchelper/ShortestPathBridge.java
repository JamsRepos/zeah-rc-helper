package com.zeahrchelper;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.events.PluginMessage;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginManager;

/**
 * Hands the next destination to the Shortest Path plugin over the event bus, tinted with the
 * current step's colour. Shortest Path cannot be a compile-time dependency of a hub plugin, so the
 * whole integration is fire-and-forget {@link PluginMessage}.
 */
@Slf4j
@Singleton
class ShortestPathBridge
{
	private static final String NAMESPACE = "shortestpath";
	private static final String MESSAGE_PATH = "path";
	private static final String MESSAGE_CLEAR = "clear";
	private static final String MESSAGE_START = "start";
	private static final String MESSAGE_TARGET = "target";
	private static final String MESSAGE_CONFIG = "config";
	private static final String PLUGIN_NAME = "Shortest Path";

	/**
	 * Shortest Path keeps the full route from the posted start, so without a refresh the line
	 * behind you stays drawn and can look like a U-turn. Re-posting from the player every few
	 * ticks trims it the same way Jam's Solo Tempoross does. Walkable stand tiles (and matching
	 * calculating colour) keep that refresh from flashing purple unreachable.
	 */
	private static final int REFRESH_TICKS = 5;

	private final Client client;
	private final ZeahRcHelperConfig config;
	private final EventBus eventBus;
	private final PluginManager pluginManager;

	private WorldPoint postedTarget;
	private Color postedColor;
	private PathDisplay postedDisplay;
	private int postedTick;

	@Inject
	ShortestPathBridge(
		Client client,
		ZeahRcHelperConfig config,
		EventBus eventBus,
		PluginManager pluginManager)
	{
		this.client = client;
		this.config = config;
		this.eventBus = eventBus;
		this.pluginManager = pluginManager;
	}

	/** True when Shortest Path is picked and actually running, so it owns the path drawing. */
	boolean isDriving()
	{
		return config.pathProvider() == PathProvider.SHORTEST_PATH && isShortestPathRunning();
	}

	void update(WorldPoint destination, Color color)
	{
		PathDisplay display = config.pathDisplay();
		if (!isDriving() || display.isOff() || destination == null)
		{
			clear();
			return;
		}

		Player player = client.getLocalPlayer();
		WorldPoint start = player == null ? null : player.getWorldLocation();
		if (start == null)
		{
			return;
		}

		int tick = client.getTickCount();
		boolean sameTarget = destination.equals(postedTarget);
		boolean unchanged = sameTarget && display == postedDisplay && Objects.equals(color, postedColor);
		if (unchanged && tick - postedTick < REFRESH_TICKS)
		{
			return;
		}

		if (!sameTarget)
		{
			log.debug("shortest path target {} -> {} (from {})", postedTarget, destination, start);
		}

		postedTarget = destination;
		postedColor = color;
		postedDisplay = display;
		postedTick = tick;

		Map<String, Object> data = new HashMap<>();
		data.put(MESSAGE_START, start);
		data.put(MESSAGE_TARGET, destination);
		data.put(MESSAGE_CONFIG, overrides(display, color));
		eventBus.post(new PluginMessage(NAMESPACE, MESSAGE_PATH, data));
	}

	/** Drops our path and hands Shortest Path's own colours back to the user. */
	void clear()
	{
		if (postedTarget == null)
		{
			return;
		}
		postedTarget = null;
		postedColor = null;
		postedDisplay = null;
		postedTick = 0;
		eventBus.post(new PluginMessage(NAMESPACE, MESSAGE_CLEAR));
	}

	/**
	 * Keys match Shortest Path's own config keys. Every path message replaces the whole override
	 * map, so this has to be complete each time.
	 */
	private static Map<String, Object> overrides(PathDisplay display, Color color)
	{
		Map<String, Object> overrides = new HashMap<>();
		overrides.put("colourPath", color);
		overrides.put("colourPathCalculating", color);
		overrides.put("colourPathUnreachable", color);
		overrides.put("pathStyle", "Lines");
		overrides.put("showTileCounter", "Disabled");
		overrides.put("drawTiles", display.showsFloor());
		overrides.put("drawMinimap", display.showsMinimap());
		overrides.put("drawMap", false);
		overrides.put("drawTransports", false);
		overrides.put("showTransportInfo", false);
		// Dense runestones / altar objects sit on blocked tiles; allow a short miss without the
		// purple unreachable flash. Prefer walkable stand tiles from RotationHelper when possible.
		overrides.put("unreachableTargetDistanceThreshold", 8);
		overrides.put("useAgilityShortcuts", true);
		overrides.put("useTeleportationItems", "None");
		overrides.put("useTeleportationSpells", false);
		overrides.put("useTeleportationSpellsHome", false);
		return overrides;
	}

	private boolean isShortestPathRunning()
	{
		for (Plugin plugin : pluginManager.getPlugins())
		{
			if (PLUGIN_NAME.equals(plugin.getName()))
			{
				return pluginManager.isPluginActive(plugin);
			}
		}
		return false;
	}
}
