package com.zeahrchelper.overlay;

import com.zeahrchelper.AgilityShortcut;
import com.zeahrchelper.HelperAction;
import com.zeahrchelper.RotationHelper;
import com.zeahrchelper.ZeahRcHelperConfig;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.Path2D;
import java.util.List;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

public class PathMinimapOverlay extends Overlay
{
	private static final int MINIMAP_RANGE = 6400;
	private static final Stroke PATH_OUTLINE = new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
	private static final Stroke PATH_LINE = new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

	private final Client client;
	private final ZeahRcHelperConfig config;
	private final RotationHelper rotationHelper;

	@Inject
	private PathMinimapOverlay(Client client, ZeahRcHelperConfig config, RotationHelper rotationHelper)
	{
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setPriority(Overlay.PRIORITY_LOW);
		this.client = client;
		this.config = config;
		this.rotationHelper = rotationHelper;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.enableHelper() || !config.showMinimapPath())
		{
			return null;
		}

		HelperAction action = rotationHelper.getCurrentAction();
		if (action == null || action.getPath() == null || action.getPath().size() < 2)
		{
			return null;
		}

		Color base = action.getColor() != null ? action.getColor() : Color.CYAN;
		renderPath(graphics, action.getPath(), base);
		return null;
	}

	private void renderPath(Graphics2D graphics, List<WorldPoint> path, Color base)
	{
		Player player = client.getLocalPlayer();
		WorldView worldView = client.getTopLevelWorldView();
		if (worldView == null)
		{
			return;
		}

		Path2D.Float line = new Path2D.Float();
		boolean started = false;
		WorldPoint prevTile = player == null ? null : player.getWorldLocation();
		if (player != null)
		{
			Point mini = toMinimap(player.getWorldLocation(), worldView, player);
			if (mini != null)
			{
				line.moveTo(mini.getX(), mini.getY());
				started = true;
			}
		}

		for (int i = 0; i < path.size(); i++)
		{
			WorldPoint tile = path.get(i);
			Point mini = toMinimap(tile, worldView, player);
			if (mini == null)
			{
				prevTile = tile;
				continue;
			}
			boolean gap = AgilityShortcut.hopBetween(prevTile, tile);
			if (!started)
			{
				line.moveTo(mini.getX(), mini.getY());
				started = true;
			}
			else if (gap)
			{
				line.moveTo(mini.getX(), mini.getY());
			}
			else
			{
				line.lineTo(mini.getX(), mini.getY());
			}
			prevTile = tile;
		}

		if (!started)
		{
			return;
		}

		Stroke oldStroke = graphics.getStroke();
		Object oldHint = graphics.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		graphics.setStroke(PATH_OUTLINE);
		graphics.setColor(new Color(0, 0, 0, 140));
		graphics.draw(line);
		graphics.setStroke(PATH_LINE);
		graphics.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), 220));
		graphics.draw(line);
		graphics.setStroke(oldStroke);
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
			oldHint != null ? oldHint : RenderingHints.VALUE_ANTIALIAS_OFF);
	}

	private Point toMinimap(WorldPoint tile, WorldView worldView, Player player)
	{
		if (tile == null)
		{
			return null;
		}
		LocalPoint local = LocalPoint.fromWorld(worldView, tile);
		if (local == null && player != null && player.getWorldLocation() != null)
		{
			LocalPoint here = player.getLocalLocation();
			WorldPoint playerWorld = player.getWorldLocation();
			if (here != null && playerWorld.getPlane() == tile.getPlane())
			{
				int x = here.getX() + (tile.getX() - playerWorld.getX()) * Perspective.LOCAL_TILE_SIZE;
				int y = here.getY() + (tile.getY() - playerWorld.getY()) * Perspective.LOCAL_TILE_SIZE;
				local = new LocalPoint(x, y, worldView);
			}
		}
		if (local == null)
		{
			return null;
		}
		return Perspective.localToMinimap(client, local, MINIMAP_RANGE);
	}
}
