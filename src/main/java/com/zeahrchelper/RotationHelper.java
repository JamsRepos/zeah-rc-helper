package com.zeahrchelper;

import java.awt.Color;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.TileObject;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;

@Singleton
public class RotationHelper
{
	private static final int FULL_FRAGMENTS = 100;
	private static final int SOUL_LEVEL = 90;
	private static final int AT_ALTAR_TILES = 12;
	private static final int NEAR_ALTAR_TILES = 24;

	private final Client client;
	private final ZeahRcHelperConfig config;
	private final InventoryChecker inventoryChecker;
	private final ReminderService reminderService;
	private final SceneTracker sceneTracker;
	private final RcPathRouter pathRouter;

	@Getter
	private HelperAction currentAction = HelperAction.idle();

	@Getter
	private InventorySnapshot snapshot = new InventorySnapshot(0, 0, 0, 28, false, false, false, false, false, false, -1);

	@Getter
	private RcMode resolvedMode = RcMode.BLOOD;

	@Getter
	private int tripsCompleted;

	private RotationStep lastStep = RotationStep.IDLE;

	@Inject
	RotationHelper(
		Client client,
		ZeahRcHelperConfig config,
		InventoryChecker inventoryChecker,
		ReminderService reminderService,
		SceneTracker sceneTracker,
		RcPathRouter pathRouter)
	{
		this.client = client;
		this.config = config;
		this.inventoryChecker = inventoryChecker;
		this.reminderService = reminderService;
		this.sceneTracker = sceneTracker;
		this.pathRouter = pathRouter;
	}

	public void reset()
	{
		currentAction = HelperAction.idle();
		tripsCompleted = 0;
		lastStep = RotationStep.IDLE;
		pathRouter.reset();
		inventoryChecker.reset();
	}

	public void update()
	{
		if (!ZeahRcArea.isInArceuusRc(client))
		{
			currentAction = HelperAction.idle();
			pathRouter.reset();
			reminderService.update(snapshot, resolvedMode, false);
			return;
		}

		resolvedMode = resolveMode();
		snapshot = inventoryChecker.scan();
		reminderService.update(snapshot, resolvedMode, true);

		if (!config.enableHelper())
		{
			currentAction = HelperAction.idle();
			pathRouter.reset();
			return;
		}

		RotationStep step = inferStep(snapshot);
		if (isTripCompleteTransition(lastStep, step))
		{
			tripsCompleted++;
		}
		lastStep = step;

		Player player = client.getLocalPlayer();
		WorldPoint start = player == null ? null : player.getWorldLocation();
		boolean atMine = start != null && sceneTracker.isAtMine(start);
		TileObject destination = destinationObject(step);
		WorldPoint end = pathEnd(destination, step, start);
		WorldView worldView = client.getTopLevelWorldView();
		int agility = client.getRealSkillLevel(Skill.AGILITY);
		List<WorldPoint> path = pathRouter.pathTo(
			worldView,
			start,
			end,
			step,
			agility,
			resolvedMode,
			atMine,
			config.showPath() || config.showMinimapPath());
		RcPathRouter.ClickTarget click = pathRouter.nextClick(step, destination, path, start, atMine);
		currentAction = new HelperAction(
			step,
			detailFor(step, snapshot),
			path,
			click.getObject(),
			click.getTile(),
			colorFor(step));
	}

	private TileObject destinationObject(RotationStep step)
	{
		switch (step)
		{
			case MINE_FIRST:
			case MINE_SECOND:
				return sceneTracker.chooseRunestone();
			case GO_DARK_FIRST:
			case GO_DARK_SECOND:
				return sceneTracker.getDarkAltar();
			case GO_ALTAR:
			case CRAFT_FRAGMENTS:
			case CRAFT_REMAINING:
				return sceneTracker.altarFor(resolvedMode);
			case CHISEL_AT_ALTAR:
				return null;
			case CHISEL_AND_RETURN:
			case RETURN_TO_MINE:
				Player p = client.getLocalPlayer();
				if (p != null && sceneTracker.isAtMine(p.getWorldLocation()) && step == RotationStep.CHISEL_AND_RETURN)
				{
					return null;
				}
				return sceneTracker.chooseRunestone();
			default:
				return null;
		}
	}

	private WorldPoint pathEnd(TileObject destination, RotationStep step, WorldPoint start)
	{
		if (destination != null)
		{
			return destination.getWorldLocation();
		}
		if (sceneTracker.isAtMine(start) && (step == RotationStep.MINE_FIRST || step == RotationStep.MINE_SECOND
			|| step == RotationStep.CHISEL_AND_RETURN || step == RotationStep.RETURN_TO_MINE))
		{
			return null;
		}
		return fallbackTile(step);
	}

	private WorldPoint fallbackTile(RotationStep step)
	{
		switch (step)
		{
			case MINE_FIRST:
			case MINE_SECOND:
			case CHISEL_AND_RETURN:
			case RETURN_TO_MINE:
				return ZeahRcArea.MINE_STAND;
			case GO_DARK_FIRST:
			case GO_DARK_SECOND:
				return ZeahRcArea.DARK_ALTAR;
			case GO_ALTAR:
			case CRAFT_FRAGMENTS:
			case CRAFT_REMAINING:
			case CHISEL_AT_ALTAR:
				return resolvedMode == RcMode.SOUL ? ZeahRcArea.SOUL_ALTAR : ZeahRcArea.BLOOD_ALTAR;
			default:
				return null;
		}
	}

	private Color colorFor(RotationStep step)
	{
		switch (step)
		{
			case GO_ALTAR:
			case CRAFT_FRAGMENTS:
			case CRAFT_REMAINING:
				return resolvedMode.getColor();
			default:
				return step.getColor();
		}
	}

	private RcMode resolveMode()
	{
		if (config.mode() != RcMode.AUTO)
		{
			return config.mode();
		}
		return client.getRealSkillLevel(Skill.RUNECRAFT) >= SOUL_LEVEL ? RcMode.SOUL : RcMode.BLOOD;
	}

	private RotationStep inferStep(InventorySnapshot inv)
	{
		boolean atAltar = sceneTracker.isNearAltar(resolvedMode, AT_ALTAR_TILES);
		boolean nearAltar = atAltar || sceneTracker.isNearAltar(resolvedMode, NEAR_ALTAR_TILES);
		Player player = client.getLocalPlayer();
		boolean atMine = player != null && sceneTracker.isAtMine(player.getWorldLocation());
		boolean hasFrags = inv.getFragments() > 0;
		boolean hasDark = inv.getDarkBlocks() > 0;
		boolean hasDense = inv.getDenseBlocks() > 0;
		boolean inventoryFull = inv.getEmptySlots() == 0;
		boolean fullFragmentStack = inv.getFragments() >= FULL_FRAGMENTS;

		if (atAltar)
		{
			if (hasFrags)
			{
				return RotationStep.CRAFT_FRAGMENTS;
			}
			if (hasDark)
			{
				return RotationStep.CHISEL_AT_ALTAR;
			}
			return RotationStep.RETURN_TO_MINE;
		}

		if (hasFrags && hasDark && (inventoryFull || fullFragmentStack || nearAltar))
		{
			return RotationStep.GO_ALTAR;
		}
		if (hasDense && inventoryFull)
		{
			return fullFragmentStack || hasFrags ? RotationStep.GO_DARK_SECOND : RotationStep.GO_DARK_FIRST;
		}
		if (hasDark && !fullFragmentStack)
		{
			return RotationStep.CHISEL_AND_RETURN;
		}
		if (hasFrags && !hasDark && !hasDense)
		{
			return atMine ? RotationStep.MINE_SECOND : RotationStep.RETURN_TO_MINE;
		}
		if (!atMine && hasDense && !fullFragmentStack)
		{
			return RotationStep.GO_DARK_FIRST;
		}
		return RotationStep.MINE_FIRST;
	}

	private static boolean isTripCompleteTransition(RotationStep from, RotationStep to)
	{
		return (from == RotationStep.CRAFT_REMAINING || from == RotationStep.RETURN_TO_MINE)
			&& to == RotationStep.MINE_FIRST;
	}

	private String detailFor(RotationStep step, InventorySnapshot inv)
	{
		String rune = resolvedMode == RcMode.SOUL ? "soul" : "blood";
		switch (step)
		{
			case MINE_FIRST:
				return "Fill your first inventory";
			case GO_DARK_FIRST:
				return "Click the Dark Altar to venerate all dense blocks";
			case CHISEL_AND_RETURN:
				return "Use chisel on dark blocks while running back to the mine";
			case MINE_SECOND:
				return "Fill your second inventory";
			case GO_DARK_SECOND:
				return "Venerate the second inventory at the Dark Altar";
			case GO_ALTAR:
				return "Carry fragments + dark blocks to the " + rune + " altar";
			case CRAFT_FRAGMENTS:
				return "Click the " + rune + " altar to craft your fragments";
			case CHISEL_AT_ALTAR:
				return "Chisel the remaining dark blocks into fragments";
			case CRAFT_REMAINING:
				return "Click the " + rune + " altar again for the second batch";
			case RETURN_TO_MINE:
				return "Take the shortcut back to the dense essence mine";
			default:
				return step.getLabel();
		}
	}
}
