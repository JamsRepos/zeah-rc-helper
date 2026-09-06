package com.zeahrchelper.overlay;

import com.zeahrchelper.InventorySnapshot;
import com.zeahrchelper.RotationHelper;
import com.zeahrchelper.ZeahRcHelperConfig;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
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

		graphics.setFont(FontManager.getRunescapeSmallFont());
		text.setText(inv.isFragmentsKnown() ? String.valueOf(inv.getFragments()) : "?");
		text.setColor(Color.WHITE);
		text.setPosition(new Point(location.getX(), location.getY() + TEXT_OFFSET_Y));
		text.render(graphics);
	}
}
