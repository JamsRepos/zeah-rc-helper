package com.zeahrchelper.overlay;

import com.zeahrchelper.InventorySnapshot;
import com.zeahrchelper.RotationHelper;
import com.zeahrchelper.ZeahRcHelperConfig;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import javax.inject.Inject;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.WidgetItemOverlay;
import net.runelite.client.ui.overlay.components.TextComponent;

/**
 * Draws the fragment estimate on the inventory stack (the game leaves it unnumbered).
 */
public class FragmentStackOverlay extends WidgetItemOverlay
{
	private static final int TEXT_OFFSET_Y = 15;
	private static final int PULSE_MS = 1200;
	/** Draws the same attention as StatusOverlay's warning lines - visible even with the panel off. */
	private static final Color UNKNOWN_COLOR = new Color(255, 168, 76);
	private static final Stroke BORDER_STROKE = new BasicStroke(2);

	private final ZeahRcHelperConfig config;
	private final RotationHelper rotationHelper;
	private final TextComponent text = new TextComponent();

	@Inject
	private FragmentStackOverlay(ZeahRcHelperConfig config, RotationHelper rotationHelper)
	{
		this.config = config;
		this.rotationHelper = rotationHelper;
		showOnInventory();
	}

	@Override
	public void renderItemOverlay(Graphics2D graphics, int itemId, WidgetItem itemWidget)
	{
		if (!config.enableHelper() || itemId != ItemID.BIGBLANKRUNE)
		{
			return;
		}

		InventorySnapshot inv = rotationHelper.getSnapshot();
		if (inv == null || inv.getFragments() <= 0)
		{
			return;
		}

		net.runelite.api.Point location = itemWidget.getCanvasLocation();
		if (location == null)
		{
			return;
		}

		boolean known = inv.isFragmentsKnown();
		if (!known)
		{
			Rectangle bounds = itemWidget.getCanvasBounds();
			Stroke original = graphics.getStroke();
			graphics.setStroke(BORDER_STROKE);
			graphics.setColor(pulse(UNKNOWN_COLOR));
			graphics.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);
			graphics.setStroke(original);
		}

		graphics.setFont(FontManager.getRunescapeSmallFont());
		text.setText(known ? String.valueOf(inv.getFragments()) : "Check");
		text.setColor(known ? Color.WHITE : UNKNOWN_COLOR);
		text.setPosition(new Point(location.getX(), location.getY() + TEXT_OFFSET_Y));
		text.render(graphics);
	}

	/** Same breathing pulse as NextClickOverlay's highlights, so an unknown stack draws the eye. */
	private static Color pulse(Color base)
	{
		long t = System.currentTimeMillis() % PULSE_MS;
		float phase = t < PULSE_MS / 2
			? t / (float) (PULSE_MS / 2)
			: (PULSE_MS - t) / (float) (PULSE_MS / 2);
		int alpha = 120 + (int) (phase * 135);
		return new Color(base.getRed(), base.getGreen(), base.getBlue(), alpha);
	}
}
