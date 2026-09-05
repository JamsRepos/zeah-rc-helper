package com.zeahrchelper;

import com.google.inject.Provides;
import com.zeahrchelper.overlay.IdleTintOverlay;
import com.zeahrchelper.overlay.NextClickOverlay;
import com.zeahrchelper.overlay.PathMinimapOverlay;
import com.zeahrchelper.overlay.StatusOverlay;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.GameState;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.DecorativeObjectDespawned;
import net.runelite.api.events.DecorativeObjectSpawned;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GroundObjectDespawned;
import net.runelite.api.events.GroundObjectSpawned;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.events.WallObjectDespawned;
import net.runelite.api.events.WallObjectSpawned;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@Slf4j
@PluginDescriptor(
	name = "Jam's Arceuus Runecrafting",
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
	private PathMinimapOverlay pathMinimapOverlay;

	@Inject
	private StatusOverlay statusOverlay;

	@Inject
	private IdleTintOverlay idleTintOverlay;

	@Inject
	private RotationHelper rotationHelper;

	@Inject
	private ReminderService reminderService;

	@Inject
	private InventoryChecker inventoryChecker;

	@Inject
	private SceneTracker sceneTracker;

	@Inject
	private ChangelogService changelogService;

	@Inject
	private PathDisplayMigration pathDisplayMigration;

	@Inject
	private ShortestPathBridge shortestPathBridge;

	@Inject
	private ClientThread clientThread;

	@Override
	protected void startUp()
	{
		pathDisplayMigration.run();
		rotationHelper.reset();
		reminderService.reset();
		inventoryChecker.reset();
		sceneTracker.reset();
		sceneTracker.scanScene();
		overlayManager.add(nextClickOverlay);
		overlayManager.add(pathMinimapOverlay);
		overlayManager.add(statusOverlay);
		overlayManager.add(idleTintOverlay);
		clientThread.invoke(changelogService::maybeAnnounce);
		log.debug("Jam's Arceuus Runecrafting started");
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(nextClickOverlay);
		overlayManager.remove(pathMinimapOverlay);
		overlayManager.remove(statusOverlay);
		overlayManager.remove(idleTintOverlay);
		rotationHelper.reset();
		reminderService.reset();
		sceneTracker.reset();
		changelogService.reset();
		shortestPathBridge.clear();
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!ZeahRcHelperConfig.GROUP.equals(event.getGroup()))
		{
			return;
		}
		String key = event.getKey();
		if (ZeahRcHelperConfig.PATH_DISPLAY_KEY.equals(key)
			|| ZeahRcHelperConfig.PATH_PROVIDER_KEY.equals(key))
		{
			shortestPathBridge.clear();
		}
	}

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		rotationHelper.update();
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		sceneTracker.onSpawn(event.getGameObject());
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned event)
	{
		sceneTracker.onDespawn(event.getGameObject());
	}

	@Subscribe
	public void onDecorativeObjectSpawned(DecorativeObjectSpawned event)
	{
		sceneTracker.onSpawn(event.getDecorativeObject());
	}

	@Subscribe
	public void onDecorativeObjectDespawned(DecorativeObjectDespawned event)
	{
		sceneTracker.onDespawn(event.getDecorativeObject());
	}

	@Subscribe
	public void onGroundObjectSpawned(GroundObjectSpawned event)
	{
		sceneTracker.onSpawn(event.getGroundObject());
	}

	@Subscribe
	public void onGroundObjectDespawned(GroundObjectDespawned event)
	{
		sceneTracker.onDespawn(event.getGroundObject());
	}

	@Subscribe
	public void onWallObjectSpawned(WallObjectSpawned event)
	{
		sceneTracker.onSpawn(event.getWallObject());
	}

	@Subscribe
	public void onWallObjectDespawned(WallObjectDespawned event)
	{
		sceneTracker.onDespawn(event.getWallObject());
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		int id = event.getVarbitId();
		if (id == VarbitID.ARCEUUS_RUNESTONE_1 || id == VarbitID.ARCEUUS_RUNESTONE_2)
		{
			rotationHelper.update();
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		reminderService.onChatMessage(event);
		inventoryChecker.onChatMessage(event);
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		changelogService.onGameStateChanged(event);
		if (event.getGameState() == GameState.LOADING)
		{
			sceneTracker.reset();
		}
		else if (event.getGameState() == GameState.LOGGED_IN)
		{
			sceneTracker.scanScene();
		}
		else if (event.getGameState() == GameState.LOGIN_SCREEN)
		{
			rotationHelper.reset();
			reminderService.reset();
			inventoryChecker.reset();
			sceneTracker.reset();
			shortestPathBridge.clear();
		}
	}

	@Provides
	ZeahRcHelperConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ZeahRcHelperConfig.class);
	}
}
