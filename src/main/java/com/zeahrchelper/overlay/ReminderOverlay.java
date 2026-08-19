package com.zeahrchelper.overlay;

import com.zeahrchelper.ReminderService;
import com.zeahrchelper.ZeahRcHelperPlugin;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.List;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

public class ReminderOverlay extends OverlayPanel
{
	private final ReminderService reminderService;

	@Inject
	private ReminderOverlay(
		ZeahRcHelperPlugin plugin,
		ReminderService reminderService)
	{
		super(plugin);
		setPosition(OverlayPosition.TOP_RIGHT);
		this.reminderService = reminderService;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		List<String> warnings = reminderService.getWarnings();
		if (warnings.isEmpty())
		{
			return null;
		}

		panelComponent.getChildren().add(TitleComponent.builder()
			.text("Reminders")
			.color(Color.ORANGE)
			.build());

		for (String warning : warnings)
		{
			panelComponent.getChildren().add(LineComponent.builder()
				.left(warning)
				.leftColor(Color.WHITE)
				.build());
		}

		return super.render(graphics);
	}
}
