package com.zeahrchelper.overlay;

import com.zeahrchelper.ReminderService;
import com.zeahrchelper.ZeahRcHelperConfig;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

public class IdleTintOverlay extends Overlay
{
	private static final Color TINT = new Color(255, 70, 70, 35);

	private final Client client;
	private final ZeahRcHelperConfig config;
	private final ReminderService reminderService;

	@Inject
	private IdleTintOverlay(Client client, ZeahRcHelperConfig config, ReminderService reminderService)
	{
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ALWAYS_ON_TOP);
		this.client = client;
		this.config = config;
		this.reminderService = reminderService;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.idleFlash() || !reminderService.isIdle())
		{
			return null;
		}

		graphics.setColor(TINT);
		graphics.fillRect(0, 0, client.getCanvasWidth(), client.getCanvasHeight());
		return null;
	}
}
