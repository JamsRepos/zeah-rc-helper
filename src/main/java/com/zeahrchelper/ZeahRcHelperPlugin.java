package com.zeahrchelper;

import com.google.inject.Provides;
import com.zeahrchelper.overlay.IdleTintOverlay;
import com.zeahrchelper.overlay.NextClickOverlay;
import com.zeahrchelper.overlay.ReminderOverlay;
import com.zeahrchelper.overlay.StatusOverlay;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.GameState;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@Slf4j
@PluginDescriptor(
	name = "Zeah RC Helper",
	description = "Click-here helper for Arceuus blood and soul runecrafting, with lantern and blood essence reminders",
	tags = {"runecraft", "runecrafting", "blood", "soul", "arceuus", "zeah", "kourend", "skilling"},
	conflicts = {"Easy Arceuus Runecrafting", "easy-arceuus-runecrafting"}
)
public class ZeahRcHelperPlugin extends Plugin
{
	@Inject
	private OverlayManager overlayManager;

	@Inject
	private NextClickOverlay nextClickOverlay;

	@Inject
	private StatusOverlay statusOverlay;

	@Inject
	private ReminderOverlay reminderOverlay;

	@Inject
	private IdleTintOverlay idleTintOverlay;

	@Inject
	private RotationHelper rotationHelper;

	@Inject
	private ReminderService reminderService;

	@Override
	protected void startUp()
	{
		rotationHelper.reset();
		reminderService.reset();
		overlayManager.add(nextClickOverlay);
		overlayManager.add(statusOverlay);
		overlayManager.add(reminderOverlay);
		overlayManager.add(idleTintOverlay);
		log.debug("Zeah RC Helper started");
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(nextClickOverlay);
		overlayManager.remove(statusOverlay);
		overlayManager.remove(reminderOverlay);
		overlayManager.remove(idleTintOverlay);
		rotationHelper.reset();
		reminderService.reset();
	}

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		rotationHelper.update();
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		reminderService.onChatMessage(event);
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGIN_SCREEN)
		{
			rotationHelper.reset();
			reminderService.reset();
		}
	}

	@Provides
	ZeahRcHelperConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ZeahRcHelperConfig.class);
	}
}
