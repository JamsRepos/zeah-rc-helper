package com.zeahrchelper.overlay;

import com.zeahrchelper.HelperAction;
import com.zeahrchelper.InventorySnapshot;
import com.zeahrchelper.RcMode;
import com.zeahrchelper.ReminderService;
import com.zeahrchelper.RotationHelper;
import com.zeahrchelper.RotationStep;
import com.zeahrchelper.ZeahRcHelperConfig;
import com.zeahrchelper.ZeahRcHelperPlugin;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.api.MenuAction;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import static net.runelite.client.ui.overlay.OverlayManager.OPTION_CONFIGURE;

public class StatusOverlay extends OverlayPanel
{
	private final ZeahRcHelperConfig config;
	private final RotationHelper rotationHelper;
	private final ReminderService reminderService;

	@Inject
	private StatusOverlay(
		ZeahRcHelperPlugin plugin,
		ZeahRcHelperConfig config,
		RotationHelper rotationHelper,
		ReminderService reminderService)
	{
		super(plugin);
		setPosition(OverlayPosition.TOP_LEFT);
		this.config = config;
		this.rotationHelper = rotationHelper;
		this.reminderService = reminderService;
		addMenuEntry(MenuAction.RUNELITE_OVERLAY_CONFIG, OPTION_CONFIGURE, "Zeah RC Helper");
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.enableHelper() || !config.showStatusPanel())
		{
			return null;
		}

		HelperAction action = rotationHelper.getCurrentAction();
		if (action == null || action.getStep() == RotationStep.IDLE)
		{
			return null;
		}

		InventorySnapshot inv = rotationHelper.getSnapshot();
		RcMode mode = rotationHelper.getResolvedMode();

		panelComponent.getChildren().add(TitleComponent.builder()
			.text("Zeah RC — " + (mode == RcMode.SOUL ? "Souls" : "Bloods"))
			.color(Color.CYAN)
			.build());

		Color stepColor = action.getColor() != null ? action.getColor() : Color.WHITE;
		panelComponent.getChildren().add(LineComponent.builder()
			.left(action.getStep().getLabel())
			.leftColor(stepColor)
			.build());
		panelComponent.getChildren().add(LineComponent.builder()
			.left(action.getDetail())
			.build());

		panelComponent.getChildren().add(LineComponent.builder()
			.left("Blocks")
			.right(inv.getDenseBlocks() + " dense / " + inv.getDarkBlocks() + " dark")
			.build());
		panelComponent.getChildren().add(LineComponent.builder()
			.left("Fragments")
			.right(String.valueOf(inv.getFragments()))
			.build());
		panelComponent.getChildren().add(LineComponent.builder()
			.left("Trips")
			.right(String.valueOf(rotationHelper.getTripsCompleted()))
			.build());

		if (mode == RcMode.BLOOD)
		{
			String essence;
			if (inv.isHasActiveBloodEssence())
			{
				Integer charges = reminderService.getBloodEssenceCharges();
				essence = charges != null ? charges + " charges" : "active (check)";
			}
			else if (inv.isHasInactiveBloodEssence())
			{
				essence = "inactive";
			}
			else
			{
				essence = "none";
			}
			panelComponent.getChildren().add(LineComponent.builder()
				.left("Essence")
				.right(essence)
				.rightColor(inv.isHasActiveBloodEssence() ? Color.GREEN : Color.ORANGE)
				.build());
		}

		return super.render(graphics);
	}
}
