package com.zeahrchelper;

import java.awt.Color;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.TileObject;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;

@Slf4j
@Singleton
public class RotationHelper
{
	private static final int SOUL_LEVEL = 90;
	private static final int AT_ALTAR_TILES = 12;
	private static final int NEAR_ALTAR_TILES = 24;

	private final Client client;
	private final ZeahRcHelperConfig config;
	private final InventoryChecker inventoryChecker;
	private final ReminderService reminderService;
	private final SceneTracker sceneTracker;
	private final RcPathRouter pathRouter;
	private final ShortestPathBridge shortestPathBridge;

	@Getter
	private HelperAction currentAction = HelperAction.idle();

	@Getter
	private InventorySnapshot snapshot = new InventorySnapshot(
		0, 0, 0, 28, false, false, false, false, false, false, -1, true, false, 0, 0, false);

	@Getter
	private RcMode resolvedMode = RcMode.BLOOD;

	@Getter
	private final SessionStats sessionStats;

	private RotationStep lastStep = RotationStep.IDLE;
	/** True after the player clicks the Blood Altar during GO_ALTAR — hide Stand Here while walking. */
	private boolean bloodAltarClickCommitted;
	/** True once the Second Load has actually been mined this Trip - there is no Third Load. */
	private boolean secondLoadReached;

	@Inject
	RotationHelper(
		Client client,
		ZeahRcHelperConfig config,
		InventoryChecker inventoryChecker,
		ReminderService reminderService,
		SceneTracker sceneTracker,
		RcPathRouter pathRouter,
		ShortestPathBridge shortestPathBridge,
		SessionStats sessionStats)
	{
		this.client = client;
		this.config = config;
		this.inventoryChecker = inventoryChecker;
		this.reminderService = reminderService;
		this.sceneTracker = sceneTracker;
		this.pathRouter = pathRouter;
		this.shortestPathBridge = shortestPathBridge;
		this.sessionStats = sessionStats;
	}

	public void reset()
	{
		currentAction = HelperAction.idle();
		lastStep = RotationStep.IDLE;
		bloodAltarClickCommitted = false;
		secondLoadReached = false;
		sessionStats.clearBaselines();
		pathRouter.reset();
		shortestPathBridge.clear();
		inventoryChecker.reset();
	}

	public int getTripsCompleted()
	{
		return sessionStats.getTripsCompleted();
	}

	public int getBloodRunesCrafted()
	{
		return sessionStats.getBloodRunesCrafted();
	}

	public int getSoulRunesCrafted()
	{
		return sessionStats.getSoulRunesCrafted();
	}

	public int getTotalTrips()
	{
		return sessionStats.getTotalTrips();
	}

	public int getTotalBloodRunes()
	{
		return sessionStats.getTotalBloodRunes();
	}

	public int getTotalSoulRunes()
	{
		return sessionStats.getTotalSoulRunes();
	}

	/** Re-read lifetime totals after login when the RS profile is available. */
	public void reloadSessionTotals()
	{
		sessionStats.reloadTotalsFromProfile();
	}

	/** Zero session and lifetime trip/rune counters. */
	public void resetAllCounters()
	{
		sessionStats.resetAllCounters();
	}

	/**
	 * Inventory changed mid-tick (craft completes). Counts runes using fragment consumption
	 * so we do not wait for the next game tick.
	 */
	public void onInventoryChanged()
	{
		snapshot = inventoryChecker.scan();
		sessionStats.noteRuneInventory(
			snapshot.getBloodRunes(), snapshot.getSoulRunes(), snapshot.isHasFragments());
	}

	/** Player clicked the Blood Altar — drop the stand-tile hint for the rest of this GO_ALTAR step. */
	public void onBloodAltarClicked()
	{
		if (lastStep == RotationStep.GO_ALTAR && resolvedMode == RcMode.BLOOD)
		{
			bloodAltarClickCommitted = true;
		}
	}

	public void update()
	{
		if (!ZeahRcArea.isInArceuusRc(client))
		{
			currentAction = HelperAction.idle();
			bloodAltarClickCommitted = false;
			pathRouter.reset();
			shortestPathBridge.clear();
			snapshot = inventoryChecker.scan();
			sessionStats.noteRuneInventory(
				snapshot.getBloodRunes(), snapshot.getSoulRunes(), snapshot.isHasFragments());
			sessionStats.onGameTick();
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
			shortestPathBridge.clear();
			sessionStats.noteRuneInventory(
				snapshot.getBloodRunes(), snapshot.getSoulRunes(), snapshot.isHasFragments());
			sessionStats.onGameTick();
			return;
		}

		boolean atAltar = sceneTracker.isNearAltar(resolvedMode, AT_ALTAR_TILES);
		boolean nearAltar = atAltar || sceneTracker.isNearAltar(resolvedMode, NEAR_ALTAR_TILES);
		Player player = client.getLocalPlayer();
		WorldPoint start = player == null ? null : player.getWorldLocation();
		boolean atMine = start != null && sceneTracker.isAtMine(start);

		RotationStep step = RotationLogic.infer(snapshot, atAltar, nearAltar, atMine, lastStep, secondLoadReached);
		if (RotationLogic.isTripCompleteTransition(lastStep, step))
		{
			sessionStats.noteTripComplete();
			secondLoadReached = false;
		}
		if (RotationLogic.marksSecondLoadReached(step))
		{
			secondLoadReached = true;
		}
		sessionStats.noteRuneInventory(
			snapshot.getBloodRunes(), snapshot.getSoulRunes(), snapshot.isHasFragments());
		sessionStats.onGameTick();
		if (step != RotationStep.GO_ALTAR)
		{
			bloodAltarClickCommitted = false;
		}
		lastStep = step;

		boolean bloodAltarInScene = sceneTracker.isBloodAltarInScene();
		BloodAltarReach.State altarReach = BloodAltarReach.evaluate(
			step,
			resolvedMode,
			start,
			bloodAltarInScene,
			bloodAltarClickCommitted);
		if (step == RotationStep.GO_ALTAR && resolvedMode == RcMode.BLOOD
			&& altarReach != BloodAltarReach.State.READY)
		{
			WorldView diagnosticView = client.getTopLevelWorldView();
			log.debug(
				"GO_ALTAR not ready: reach={} altarInScene={} bloodAltarSpawned={} reachTo={} tile={}"
					+ " sceneBase=({},{}) sceneSize=({},{})",
				altarReach, bloodAltarInScene, sceneTracker.getBloodAltar() != null,
				start == null ? -1 : BloodAltarReach.reachTo(start), start,
				diagnosticView == null ? -1 : diagnosticView.getBaseX(),
				diagnosticView == null ? -1 : diagnosticView.getBaseY(),
				diagnosticView == null ? -1 : diagnosticView.getSizeX(),
				diagnosticView == null ? -1 : diagnosticView.getSizeY());
		}

		TileObject destination = destinationObject(step, altarReach);
		WorldPoint end = pathEnd(destination, step, start, altarReach);
		WorldView worldView = client.getTopLevelWorldView();
		int agility = client.getRealSkillLevel(Skill.AGILITY);
		Color color = colorFor(step);
		boolean ownPath = !config.pathDisplay().isOff() && !shortestPathBridge.isDriving();
		List<WorldPoint> path = pathRouter.pathTo(
			worldView,
			start,
			end,
			step,
			agility,
			resolvedMode,
			atMine,
			ownPath,
			resolvedMode == RcMode.BLOOD);
		RcPathRouter.ClickTarget click = nextClick(step, destination, path, start, atMine, altarReach);
		shortestPathBridge.update(shortestPathTarget(end, step, altarReach), color);
		currentAction = new HelperAction(
			step,
			detailFor(step, altarReach),
			path,
			click.getObject(),
			click.getTile(),
			color,
			worldHintFor(altarReach));
	}

	private RcPathRouter.ClickTarget nextClick(
		RotationStep step,
		TileObject destination,
		List<WorldPoint> path,
		WorldPoint start,
		boolean atMine,
		BloodAltarReach.State altarReach)
	{
		if (altarReach == BloodAltarReach.State.STAND_THEN_CLICK)
		{
			return RcPathRouter.ClickTarget.of(null, ZeahRcArea.DARK_APPROACH);
		}
		return pathRouter.nextClick(step, destination, path, start, atMine);
	}

	/**
	 * Shortest Path targets must be walkable. Object SW tiles (runestones, altars) are often
	 * collision-blocked, which makes SP report "Destination could not be reached".
	 */
	private WorldPoint shortestPathTarget(WorldPoint end, RotationStep step, BloodAltarReach.State altarReach)
	{
		if (altarReach == BloodAltarReach.State.STAND_THEN_CLICK)
		{
			return ZeahRcArea.DARK_APPROACH;
		}
		if (end == null)
		{
			return null;
		}
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
				return end;
		}
	}

	private TileObject destinationObject(RotationStep step, BloodAltarReach.State altarReach)
	{
		if (altarReach == BloodAltarReach.State.STAND_THEN_CLICK)
		{
			return null;
		}
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
				if (step == RotationStep.CHISEL_AND_RETURN)
				{
					Player p = client.getLocalPlayer();
					TileObject stone = sceneTracker.chooseRunestone();
					// Loose isAtMine includes the west-73 landing — only drop the target when
					// actually next to a runestone.
					if (p != null && stone != null
						&& SceneTracker.distanceTo(stone, p.getWorldLocation()) <= 1)
					{
						return null;
					}
				}
				return sceneTracker.chooseRunestone();
			default:
				return null;
		}
	}

	private WorldPoint pathEnd(
		TileObject destination,
		RotationStep step,
		WorldPoint start,
		BloodAltarReach.State altarReach)
	{
		if (altarReach == BloodAltarReach.State.STAND_THEN_CLICK)
		{
			return ZeahRcArea.DARK_APPROACH;
		}
		if (destination != null)
		{
			return destination.getWorldLocation();
		}
		// Only mining steps clear the path on loose isAtMine. Chisel/return must keep pathing
		// after the west-73 landing (inside the mine radius but still short of the stones).
		if (sceneTracker.isAtMine(start)
			&& (step == RotationStep.MINE_FIRST || step == RotationStep.MINE_SECOND))
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

	private static String worldHintFor(BloodAltarReach.State altarReach)
	{
		if (altarReach == BloodAltarReach.State.STAND_THEN_CLICK)
		{
			return "Stand Here";
		}
		if (altarReach == BloodAltarReach.State.READY)
		{
			return "Click Here";
		}
		return null;
	}

	private String detailFor(RotationStep step, BloodAltarReach.State altarReach)
	{
		if (altarReach == BloodAltarReach.State.STAND_THEN_CLICK)
		{
			return "Stand Here, then click the Blood Altar";
		}
		if (altarReach == BloodAltarReach.State.READY)
		{
			return "Click the Blood Altar";
		}
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
