package com.zeahrchelper.overlay;

import com.zeahrchelper.HelperAction;
import com.zeahrchelper.RotationHelper;
import com.zeahrchelper.ZeahRcHelperConfig;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

public class NextClickOverlay extends Overlay
{
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
		if (!config.enableHelper() || !config.highlightNextClick())
		{
			return null;
		}

		HelperAction action = rotationHelper.getCurrentAction();
		if (action == null || action.getHighlightTiles() == null || action.getHighlightTiles().isEmpty())
		{
			return null;
		}

		Color color = action.getColor() != null ? action.getColor() : Color.CYAN;
		for (WorldPoint tile : action.getHighlightTiles())
		{
			LocalPoint local = LocalPoint.fromWorld(client, tile);
			if (local == null)
			{
				continue;
			}

			Polygon poly = Perspective.getCanvasTilePoly(client, local);
			if (poly != null)
			{
				OverlayUtil.renderPolygon(graphics, poly, color);
				graphics.setStroke(new BasicStroke(2));
			}
		}

		return null;
	}
}
