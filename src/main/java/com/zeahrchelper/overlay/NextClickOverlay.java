package com.zeahrchelper.overlay;

import com.zeahrchelper.AgilityShortcut;
import com.zeahrchelper.HelperAction;
import com.zeahrchelper.RotationHelper;
import com.zeahrchelper.ZeahRcHelperConfig;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Path2D;
import java.util.List;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.TileObject;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

public class NextClickOverlay extends Overlay
{
	private static final int PULSE_MS = 1200;
	private static final Stroke PATH_OUTLINE = new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
	private static final Stroke PATH_LINE = new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

	private final Client client;
	private final ZeahRcHelperConfig config;
	private final RotationHelper rotationHelper;

	@Inject
	private NextClickOverlay(Client client, ZeahRcHelperConfig config, RotationHelper rotationHelper)
	{
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
		this.client = client;
		this.config = config;
		this.rotationHelper = rotationHelper;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.enableHelper())
		{
			return null;
		}

		HelperAction action = rotationHelper.getCurrentAction();
		if (action == null)
		{
			return null;
		}

		Color base = action.getColor() != null ? action.getColor() : Color.CYAN;
		if (config.showPath())
		{
			renderPath(graphics, action.getPath(), base);
		}
		if (config.highlightNextClick())
		{
			if (action.getHighlightObject() != null)
			{
				renderObject(graphics, action.getHighlightObject(), pulse(base));
			}
			else
			{
				renderTile(graphics, action.getHighlightTile(), pulse(base));
			}
		}
		return null;
	}

	private void renderPath(Graphics2D graphics, List<WorldPoint> path, Color base)
	{
		if (path == null || path.size() < 2)
		{
			return;
		}

		Player player = client.getLocalPlayer();
		WorldView worldView = client.getTopLevelWorldView();
		if (worldView == null)
		{
			return;
		}

		int plane = worldView.getPlane();
		Path2D.Float line = new Path2D.Float();
		boolean started = false;

		if (player != null)
		{
			LocalPoint playerLocal = player.getLocalLocation();
			Point canvas = playerLocal == null ? null : Perspective.localToCanvas(client, playerLocal, plane);
			if (canvas != null)
			{
				line.moveTo(canvas.getX(), canvas.getY());
				started = true;
			}
		}

		WorldPoint here = player == null ? null : player.getWorldLocation();
		WorldPoint prevTile = here;
		boolean pendingGap = false;
		for (int i = 0; i < path.size(); i++)
		{
			WorldPoint tile = path.get(i);
			if (here != null && tile.distanceTo(here) <= 1 && i < path.size() - 1)
			{
				prevTile = tile;
				continue;
			}

			LocalPoint local = LocalPoint.fromWorld(worldView, tile);
			if (local == null)
			{
				pendingGap = true;
				prevTile = tile;
				continue;
			}
			Point canvas = Perspective.localToCanvas(client, local, plane);
			if (canvas == null)
			{
				pendingGap = true;
				prevTile = tile;
				continue;
			}
			boolean gap = pendingGap || AgilityShortcut.hopBetween(prevTile, tile);
			pendingGap = false;
			if (!started)
			{
				line.moveTo(canvas.getX(), canvas.getY());
				started = true;
			}
			else if (gap)
			{
				line.moveTo(canvas.getX(), canvas.getY());
			}
			else
			{
				line.lineTo(canvas.getX(), canvas.getY());
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

	private void renderTile(Graphics2D graphics, WorldPoint tile, Color color)
	{
		if (tile == null)
		{
			return;
		}
		WorldView worldView = client.getTopLevelWorldView();
		if (worldView == null)
		{
			return;
		}
		LocalPoint local = LocalPoint.fromWorld(worldView, tile);
		if (local == null)
		{
			return;
		}
		Polygon poly = Perspective.getCanvasTilePoly(client, local);
		if (poly != null)
		{
			OverlayUtil.renderPolygon(graphics, poly, color);
		}
	}

	private void renderObject(Graphics2D graphics, TileObject object, Color color)
	{
		if (object == null)
		{
			return;
		}

		Shape clickbox = object.getClickbox();
		if (clickbox != null)
		{
			Point mouse = client.getMouseCanvasPosition();
			OverlayUtil.renderHoverableArea(graphics, clickbox, mouse, color, color, color);
			return;
		}

		LocalPoint local = object.getLocalLocation();
		if (local == null)
		{
			return;
		}
		Polygon poly = Perspective.getCanvasTilePoly(client, local);
		if (poly != null)
		{
			OverlayUtil.renderPolygon(graphics, poly, color);
		}
	}

	private static Color pulse(Color base)
	{
		long t = System.currentTimeMillis() % PULSE_MS;
		float phase = t < PULSE_MS / 2
			? t / (float) (PULSE_MS / 2)
			: (PULSE_MS - t) / (float) (PULSE_MS / 2);
		int alpha = 60 + (int) (phase * 120);
		return new Color(base.getRed(), base.getGreen(), base.getBlue(), alpha);
	}
}
