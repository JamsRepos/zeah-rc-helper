package com.zeahrchelper;

import java.util.ArrayList;
import java.util.Collections;
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
	private static final int SHORTCUT_73 = 73;
	private static final int SHORTCUT_69 = 69;
	private static final int SHORTCUT_52 = 52;
	private static final int SHORTCUT_49 = 49;
	private static final int AT_TARGET_TILES = 1;
	private static final int PATH_DRIFT_TILES = 3;
	private static final int AT_ALTAR_TILES = 12;
	private static final int NEAR_ALTAR_TILES = 24;

	private final Client client;
	private final ZeahRcHelperConfig config;
	private final InventoryChecker inventoryChecker;
	private final ReminderService reminderService;
	private final SceneTracker sceneTracker;

	@Getter
	private HelperAction currentAction = HelperAction.idle();

	@Getter
	private InventorySnapshot snapshot = new InventorySnapshot(0, 0, 0, 28, false, false, false, false, false, false, -1);

	@Getter
	private RcMode resolvedMode = RcMode.BLOOD;

	@Getter
	private int tripsCompleted;

	private RotationStep lastStep = RotationStep.IDLE;
	private List<WorldPoint> cachedPath = Collections.emptyList();
	private WorldPoint cachedEnd;
	private RotationStep cachedStep = RotationStep.IDLE;
	private int cachedAgility = -1;
	private WorldPoint pathGapAfter;

	@Inject
	RotationHelper(
		Client client,
		ZeahRcHelperConfig config,
		InventoryChecker inventoryChecker,
		ReminderService reminderService,
		SceneTracker sceneTracker)
	{
		this.client = client;
		this.config = config;
		this.inventoryChecker = inventoryChecker;
		this.reminderService = reminderService;
		this.sceneTracker = sceneTracker;
	}

	public void reset()
	{
		currentAction = HelperAction.idle();
		tripsCompleted = 0;
		lastStep = RotationStep.IDLE;
		clearPathCache();
		inventoryChecker.reset();
	}

	public void update()
	{
		if (!ZeahRcArea.isInArceuusRc(client))
		{
			currentAction = HelperAction.idle();
			clearPathCache();
			reminderService.update(snapshot, resolvedMode, false);
			return;
		}

		resolvedMode = resolveMode();
		snapshot = inventoryChecker.scan();
		reminderService.update(snapshot, resolvedMode, true);

		if (!config.enableHelper())
		{
			currentAction = HelperAction.idle();
			clearPathCache();
			return;
		}

		RotationStep step = inferStep(snapshot);
		if (isTripCompleteTransition(lastStep, step))
		{
			tripsCompleted++;
		}
		lastStep = step;

		TileObject destination = destinationObject(step);
		List<WorldPoint> path = pathTo(destination, step);
		TileObject nextClick = nextClick(step, destination, cachedPath);
		WorldPoint highlightTile = fallbackClickTile(step, destination, nextClick, cachedPath);
		currentAction = new HelperAction(step, detailFor(step, snapshot), path, pathGapAfter, nextClick, highlightTile, step.getColor());
	}

	private void clearPathCache()
	{
		cachedPath = Collections.emptyList();
		cachedEnd = null;
		cachedStep = RotationStep.IDLE;
		cachedAgility = -1;
		pathGapAfter = null;
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

	private TileObject nextClick(RotationStep step, TileObject destination, List<WorldPoint> path)
	{
		Player player = client.getLocalPlayer();
		if (player == null)
		{
			return destination;
		}

		if (destination == null && needsRunestone(step) && sceneTracker.isAtMine(player.getWorldLocation()))
		{
			return null;
		}

		if (destination != null && SceneTracker.distanceTo(destination, player.getWorldLocation()) <= AT_TARGET_TILES)
		{
			return destination;
		}

		if (step == RotationStep.CHISEL_AT_ALTAR)
		{
			return null;
		}

		TileObject onPath = shortcutOnPath(path);
		if (onPath != null && SceneTracker.distanceTo(onPath, player.getWorldLocation()) > AT_TARGET_TILES)
		{
			return onPath;
		}

		TileObject shortcut = shortcutTowards(destination, player.getWorldLocation(), step);
		if (shortcut != null && SceneTracker.distanceTo(shortcut, player.getWorldLocation()) > AT_TARGET_TILES)
		{
			return shortcut;
		}
		return destination;
	}

	private WorldPoint fallbackClickTile(RotationStep step, TileObject destination, TileObject nextClick, List<WorldPoint> path)
	{
		if (nextClick != null)
		{
			return null;
		}
		Player player = client.getLocalPlayer();
		if (player == null)
		{
			return null;
		}

		if (needsRunestone(step) && destination == null && sceneTracker.isAtMine(player.getWorldLocation()))
		{
			return null;
		}

		WorldPoint hop = firstTransportTile(path);
		if (hop != null)
		{
			return nearestShortcutTile(hop);
		}
		if (shouldUseBoulderShortcut(player.getWorldLocation(), destination, step)
			&& player.getWorldLocation().distanceTo(ZeahRcArea.BOULDER_SHORTCUT) > AT_TARGET_TILES)
		{
			return ZeahRcArea.BOULDER_SHORTCUT;
		}
		if (shouldUseNorthShortcut(player.getWorldLocation(), destination, step)
			&& player.getWorldLocation().distanceTo(ZeahRcArea.NORTH_SHORTCUT) > AT_TARGET_TILES)
		{
			return ZeahRcArea.NORTH_SHORTCUT;
		}
		return null;
	}

	private static boolean needsRunestone(RotationStep step)
	{
		return step == RotationStep.MINE_FIRST
			|| step == RotationStep.MINE_SECOND
			|| step == RotationStep.RETURN_TO_MINE
			|| step == RotationStep.CHISEL_AND_RETURN;
	}

	private static boolean isBusy(Player player)
	{
		if (player == null)
		{
			return false;
		}
		return player.getAnimation() != -1
			|| player.getPoseAnimation() != player.getIdlePoseAnimation();
	}

	private List<WorldPoint> pathTo(TileObject destination, RotationStep step)
	{
		if (!config.showPath())
		{
			clearPathCache();
			return Collections.emptyList();
		}

		Player player = client.getLocalPlayer();
		if (player == null)
		{
			return Collections.emptyList();
		}

		WorldPoint start = player.getWorldLocation();
		if (destination != null && SceneTracker.distanceTo(destination, start) <= AT_TARGET_TILES)
		{
			clearPathCache();
			return Collections.emptyList();
		}

		WorldPoint end = pathEnd(destination, step, start);
		if (end == null || start.distanceTo(end) <= AT_TARGET_TILES)
		{
			clearPathCache();
			return Collections.emptyList();
		}

		int agility = client.getRealSkillLevel(Skill.AGILITY);
		if (cachedStep == step && cachedAgility == agility && end.equals(cachedEnd) && cachedPath.size() > 1)
		{
			List<WorldPoint> trimmed = trimPath(cachedPath, start);
			if (trimmed != null)
			{
				cachedPath = trimmed;
				rememberPathGap(trimmed);
				return Pathfinder.simplify(trimmed);
			}
		}

		WorldView worldView = client.getTopLevelWorldView();
		List<WorldPoint> path;
		if (shouldUseNorthShortcut(start, destination, step)
			&& start.distanceTo(ZeahRcArea.NORTH_SHORTCUT) > AT_TARGET_TILES)
		{
			path = pathViaNorthShortcut(worldView, start, end);
		}
		else if (useSoulApproach(step, start))
		{
			pathGapAfter = null;
			path = soulApproachPath(worldView, start, end);
		}
		else if (useBloodApproach(step, start))
		{
			pathGapAfter = null;
			path = bloodApproachPath(worldView, start, end);
		}
		else
		{
			path = Pathfinder.find(worldView, start, end, agilityTransports(agility, step, start));
			if (path.isEmpty())
			{
				WorldPoint clamped = Pathfinder.clampToScene(worldView, start, end);
				if (clamped != null && start.distanceTo(clamped) > AT_TARGET_TILES)
				{
					path = new ArrayList<>();
					path.add(start);
					path.add(clamped);
				}
			}
			pathGapAfter = null;
			path = routeToShortcutThenOnward(worldView, start, end, path);
		}
		cachedPath = path;
		cachedEnd = end;
		cachedStep = step;
		cachedAgility = agility;
		return Pathfinder.simplify(path);
	}

	/**
	 * Walk to the shortcut object first, then continue from the landing tile. The hop itself is
	 * left as a gap so the line meets the highlighted obstacle instead of cutting through it.
	 */
	private List<WorldPoint> routeToShortcutThenOnward(
		WorldView worldView,
		WorldPoint start,
		WorldPoint end,
		List<WorldPoint> path)
	{
		int hopAt = Pathfinder.firstHopIndex(path);
		if (hopAt < 0)
		{
			pathGapAfter = null;
			return path;
		}

		WorldPoint landing = path.get(hopAt + 1);
		WorldPoint shortcut = shortcutWorldPointNear(path.get(hopAt));
		if (shortcut == null)
		{
			shortcut = path.get(hopAt);
		}

		List<WorldPoint> toShortcut;
		if (start.distanceTo(shortcut) <= AT_TARGET_TILES)
		{
			toShortcut = new ArrayList<>();
			toShortcut.add(shortcut);
		}
		else
		{
			toShortcut = Pathfinder.find(worldView, start, shortcut, Collections.emptyList());
			if (toShortcut.isEmpty())
			{
				toShortcut = new ArrayList<>(path.subList(0, hopAt + 1));
			}
			else
			{
				toShortcut = new ArrayList<>(toShortcut);
			}
			if (toShortcut.isEmpty() || !toShortcut.get(toShortcut.size() - 1).equals(shortcut))
			{
				toShortcut.add(shortcut);
			}
		}

		List<WorldPoint> onward = Pathfinder.find(worldView, landing, end, Collections.emptyList());
		if (onward.isEmpty())
		{
			onward = new ArrayList<>(path.subList(hopAt + 1, path.size()));
		}
		else
		{
			onward = new ArrayList<>(onward);
		}
		if (onward.isEmpty() || onward.get(0).distanceTo(landing) > 0)
		{
			onward.add(0, landing);
		}

		List<WorldPoint> combined = new ArrayList<>(toShortcut.size() + onward.size());
		combined.addAll(toShortcut);
		combined.addAll(onward);
		pathGapAfter = shortcut;
		return combined;
	}

	private List<WorldPoint> pathViaNorthShortcut(WorldView worldView, WorldPoint start, WorldPoint end)
	{
		WorldPoint rocks = anchored(sceneTracker.getShortcut69(), ZeahRcArea.NORTH_SHORTCUT);
		WorldPoint mineSide = offset(rocks, 1, -5);
		List<WorldPoint> toRocks = walkOrDirect(worldView, start, rocks);
		List<WorldPoint> onward = Pathfinder.find(worldView, mineSide, end, Collections.emptyList());
		if (onward.isEmpty())
		{
			onward = walkOrDirect(worldView, mineSide, end);
		}
		List<WorldPoint> combined = new ArrayList<>();
		appendLeg(combined, toRocks);
		if (combined.isEmpty() || !combined.get(combined.size() - 1).equals(rocks))
		{
			combined.add(rocks);
		}
		appendLeg(combined, onward);
		pathGapAfter = rocks;
		return combined;
	}

	private boolean useSoulApproach(RotationStep step, WorldPoint start)
	{
		if (step != RotationStep.GO_ALTAR || resolvedMode != RcMode.SOUL || start == null)
		{
			return false;
		}
		if (sceneTracker.isAtMine(start))
		{
			return false;
		}
		return start.getY() >= 3875 && start.getX() <= 1796;
	}

	private boolean useBloodApproach(RotationStep step, WorldPoint start)
	{
		if (step != RotationStep.GO_ALTAR || resolvedMode != RcMode.BLOOD || start == null)
		{
			return false;
		}
		// Until the southern ridge overlooking the Blood Altar, guide south with waypoints.
		return start.getY() >= 3836 && start.getX() <= 1745;
	}

	/**
	 * Follow the north-east crystal path until the Soul Altar is in the loaded scene, then A* the rest.
	 */
	private List<WorldPoint> soulApproachPath(WorldView worldView, WorldPoint start, WorldPoint end)
	{
		return waypointApproachPath(worldView, start, end, ZeahRcArea.SOUL_APPROACH);
	}

	/**
	 * Follow the Dark Altar → Blood Altar ridge south until A* can finish to the altar.
	 */
	private List<WorldPoint> bloodApproachPath(WorldView worldView, WorldPoint start, WorldPoint end)
	{
		return waypointApproachPath(worldView, start, end, ZeahRcArea.BLOOD_APPROACH);
	}

	private List<WorldPoint> waypointApproachPath(
		WorldView worldView,
		WorldPoint start,
		WorldPoint end,
		List<WorldPoint> waypoints)
	{
		int from = firstRemainingWaypoint(start, waypoints);
		List<WorldPoint> path = new ArrayList<>();
		WorldPoint cursor = start;
		for (int i = from; i < waypoints.size(); i++)
		{
			WorldPoint waypoint = waypoints.get(i);
			appendLeg(path, walkOrDirect(worldView, cursor, waypoint));
			cursor = waypoint;
		}

		List<WorldPoint> finish = Pathfinder.find(worldView, cursor, end, Collections.emptyList());
		if (finish.isEmpty())
		{
			WorldPoint clamped = Pathfinder.clampToScene(worldView, cursor, end);
			if (clamped != null && cursor.distanceTo(clamped) > AT_TARGET_TILES)
			{
				appendLeg(path, List.of(cursor, clamped));
			}
			else if (path.isEmpty() && start.distanceTo(cursor) > AT_TARGET_TILES)
			{
				path.add(start);
				path.add(cursor);
			}
		}
		else
		{
			appendLeg(path, finish);
		}
		return path;
	}

	private static int firstRemainingWaypoint(WorldPoint start, List<WorldPoint> waypoints)
	{
		int best = 0;
		int bestDist = Integer.MAX_VALUE;
		for (int i = 0; i < waypoints.size(); i++)
		{
			int dist = start.distanceTo(waypoints.get(i));
			if (dist < bestDist)
			{
				bestDist = dist;
				best = i;
			}
		}
		if (best < waypoints.size() - 1 && start.distanceTo(waypoints.get(best + 1)) <= bestDist)
		{
			return best + 1;
		}
		if (bestDist <= 4)
		{
			return Math.min(best + 1, waypoints.size());
		}
		return best;
	}

	private List<WorldPoint> walkOrDirect(WorldView worldView, WorldPoint from, WorldPoint to)
	{
		List<WorldPoint> leg = Pathfinder.find(worldView, from, to, Collections.emptyList());
		if (!leg.isEmpty())
		{
			return leg;
		}
		List<WorldPoint> direct = new ArrayList<>(2);
		direct.add(from);
		direct.add(to);
		return direct;
	}

	private static void appendLeg(List<WorldPoint> path, List<WorldPoint> leg)
	{
		if (leg == null || leg.isEmpty())
		{
			return;
		}
		int start = 0;
		if (!path.isEmpty() && path.get(path.size() - 1).equals(leg.get(0)))
		{
			start = 1;
		}
		for (int i = start; i < leg.size(); i++)
		{
			path.add(leg.get(i));
		}
	}

	private void rememberPathGap(List<WorldPoint> path)
	{
		WorldPoint hop = firstTransportTile(path);
		if (hop == null)
		{
			pathGapAfter = null;
			return;
		}
		WorldPoint shortcut = shortcutWorldPointNear(hop);
		pathGapAfter = shortcut != null ? shortcut : hop;
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

	private static List<WorldPoint> trimPath(List<WorldPoint> path, WorldPoint start)
	{
		int best = 0;
		int bestDist = Integer.MAX_VALUE;
		for (int i = 0; i < path.size(); i++)
		{
			int dist = path.get(i).distanceTo(start);
			if (dist < bestDist)
			{
				bestDist = dist;
				best = i;
			}
		}
		if (bestDist > PATH_DRIFT_TILES)
		{
			return null;
		}
		if (best == 0)
		{
			return path;
		}
		return new ArrayList<>(path.subList(best, path.size()));
	}

	private List<Pathfinder.Transport> agilityTransports(int agility, RotationStep step, WorldPoint start)
	{
		List<Pathfinder.Transport> transports = new ArrayList<>();
		boolean goingToCraftAltar = step == RotationStep.GO_ALTAR;
		boolean atMine = start != null && sceneTracker.isAtMine(start);
		boolean eastOfMine = start != null && start.getX() > 1768;
		// 69 is mine ↔ Dark Altar only. Soul runs leave the Dark Altar north-east around the rocks.
		if (agility >= SHORTCUT_69 && !goingToCraftAltar)
		{
			WorldPoint rocks = anchored(sceneTracker.getShortcut69(), ZeahRcArea.NORTH_SHORTCUT);
			WorldPoint mineSide = offset(rocks, 1, -5);
			WorldPoint darkSide = offset(rocks, -6, 5);
			WorldPoint boulderSide = offset(rocks, 6, 6);
			transports.add(new Pathfinder.Transport(mineSide, darkSide));
			transports.add(new Pathfinder.Transport(darkSide, mineSide));
			transports.add(new Pathfinder.Transport(boulderSide, mineSide));
		}
		if (agility >= SHORTCUT_73 && !goingToCraftAltar)
		{
			WorldPoint rocks = anchored(sceneTracker.getShortcut73(), ZeahRcArea.SHORTCUT);
			transports.add(new Pathfinder.Transport(offset(rocks, -5, -2), offset(rocks, 5, 0)));
		}
		boolean northOfMine = start != null && start.getY() > 3875;
		if (agility >= SHORTCUT_52 && !northOfMine && (!goingToCraftAltar || atMine || eastOfMine))
		{
			WorldPoint inner = anchored(sceneTracker.getShortcut52Inner(), offset(ZeahRcArea.EAST_SHORTCUT, -3, 0));
			WorldPoint outer = anchored(sceneTracker.getShortcut52Outer(), offset(ZeahRcArea.EAST_SHORTCUT, 5, 0));
			transports.add(new Pathfinder.Transport(inner, outer));
			transports.add(new Pathfinder.Transport(outer, inner));
		}
		if (agility >= SHORTCUT_49 && !goingToCraftAltar)
		{
			WorldPoint boulder = anchored(sceneTracker.getShortcut49(), ZeahRcArea.BOULDER_SHORTCUT);
			transports.add(new Pathfinder.Transport(offset(boulder, 1, 6), offset(boulder, -2, -5)));
		}
		return transports;
	}

	private static WorldPoint worldPoint(TileObject object, WorldPoint fallback)
	{
		if (object == null)
		{
			return fallback;
		}
		WorldPoint loc = object.getWorldLocation();
		return loc != null ? loc : fallback;
	}

	private static WorldPoint anchored(TileObject object, WorldPoint expected)
	{
		WorldPoint loc = worldPoint(object, expected);
		return loc.distanceTo(expected) <= 6 ? loc : expected;
	}

	private static WorldPoint offset(WorldPoint base, int dx, int dy)
	{
		return new WorldPoint(base.getX() + dx, base.getY() + dy, base.getPlane());
	}

	private WorldPoint fallbackTile(RotationStep step)
	{
		List<WorldPoint> tiles = step.highlightTiles(resolvedMode);
		return tiles.isEmpty() ? null : tiles.get(0);
	}

	private TileObject shortcutTowards(TileObject destination, WorldPoint player, RotationStep step)
	{
		if (player == null)
		{
			return null;
		}

		int agility = client.getRealSkillLevel(Skill.AGILITY);

		// 73 west scramble is one-way, Blood Altar → mine only.
		if (agility >= SHORTCUT_73 && isFromBloodAltar(player) && isNear(sceneTracker.getShortcut73(), ZeahRcArea.SHORTCUT))
		{
			return sceneTracker.getShortcut73();
		}

		// 49 boulder is one-way Soul Altar approach → mine.
		if (shouldUseBoulderShortcut(player, destination, step) && isNear(sceneTracker.getShortcut49(), ZeahRcArea.BOULDER_SHORTCUT))
		{
			return sceneTracker.getShortcut49();
		}

		// 69 north scramble: mine ↔ Dark Altar.
		if (shouldUseNorthShortcut(player, destination, step) && isNear(sceneTracker.getShortcut69(), ZeahRcArea.NORTH_SHORTCUT))
		{
			return sceneTracker.getShortcut69();
		}

		boolean destIsSoul = (step == RotationStep.GO_ALTAR && resolvedMode == RcMode.SOUL)
			|| (destination != null && destination.getWorldLocation() != null
			&& destination.getWorldLocation().distanceTo(ZeahRcArea.SOUL_ALTAR) <= 10);
		boolean destIsMine = returningToMine(destination, step);
		boolean destIsDark = destination != null && destination.getWorldLocation() != null
			&& destination.getWorldLocation().distanceTo(ZeahRcArea.DARK_ALTAR) <= 10;
		boolean eastOfMine = player.getX() > 1768;

		if (agility >= SHORTCUT_52 && destIsSoul && sceneTracker.isAtMine(player)
			&& sceneTracker.getShortcut52Inner() != null)
		{
			return sceneTracker.getShortcut52Inner();
		}

		if (agility >= SHORTCUT_52 && eastOfMine && player.getY() < 3875)
		{
			if (player.getX() < 1772 && sceneTracker.getShortcut52Inner() != null)
			{
				return sceneTracker.getShortcut52Inner();
			}
			if (sceneTracker.getShortcut52Outer() != null && (destIsMine || destIsSoul || destIsDark))
			{
				return sceneTracker.getShortcut52Outer();
			}
		}

		return null;
	}

	private boolean shouldUseNorthShortcut(WorldPoint player, TileObject destination, RotationStep step)
	{
		if (player == null || client.getRealSkillLevel(Skill.AGILITY) < SHORTCUT_69)
		{
			return false;
		}
		if (isFromBloodAltar(player) || isFromSoulApproach(player))
		{
			return false;
		}

		boolean atMine = sceneTracker.isAtMine(player);
		boolean destIsDark = step == RotationStep.GO_DARK_FIRST || step == RotationStep.GO_DARK_SECOND
			|| (destination != null && destination.getWorldLocation() != null
			&& destination.getWorldLocation().distanceTo(ZeahRcArea.DARK_ALTAR) <= 12);
		boolean destIsMine = returningToMine(destination, step);

		return (atMine && destIsDark) || (isNorthOfMineShortcut(player) && destIsMine);
	}

	private boolean shouldUseBoulderShortcut(WorldPoint player, TileObject destination, RotationStep step)
	{
		return player != null
			&& client.getRealSkillLevel(Skill.AGILITY) >= SHORTCUT_49
			&& isFromSoulApproach(player)
			&& returningToMine(destination, step);
	}

	private boolean returningToMine(TileObject destination, RotationStep step)
	{
		if (step == RotationStep.RETURN_TO_MINE || step == RotationStep.CHISEL_AND_RETURN
			|| step == RotationStep.MINE_FIRST || step == RotationStep.MINE_SECOND)
		{
			return true;
		}
		return destination != null && sceneTracker.isAtMine(destination.getWorldLocation());
	}

	private static boolean isFromBloodAltar(WorldPoint player)
	{
		return player.getX() < 1743 && player.getY() < 3860;
	}

	private static boolean isFromSoulApproach(WorldPoint player)
	{
		// East of the boulder only. West of it (after the hop) uses the 69 rocks into the mine.
		return player.getX() > 1780 || (player.getY() > 3883 && player.getX() > 1776);
	}

	private static boolean isNorthOfMineShortcut(WorldPoint player)
	{
		return player.getY() > 3875 && player.getX() >= 1710 && player.getX() <= 1778;
	}

	private TileObject shortcutOnPath(List<WorldPoint> path)
	{
		WorldPoint hop = firstTransportTile(path);
		return hop == null ? null : nearestShortcutObject(hop);
	}

	private WorldPoint shortcutWorldPointNear(WorldPoint hop)
	{
		TileObject object = nearestShortcutObject(hop);
		if (object != null && object.getWorldLocation() != null)
		{
			return object.getWorldLocation();
		}
		return nearestShortcutTile(hop);
	}

	private TileObject nearestShortcutObject(WorldPoint hop)
	{
		if (hop == null)
		{
			return null;
		}
		TileObject[] shortcuts = {
			sceneTracker.getShortcut49(),
			sceneTracker.getShortcut69(),
			sceneTracker.getShortcut73(),
			sceneTracker.getShortcut52Outer(),
			sceneTracker.getShortcut52Inner()
		};
		TileObject best = null;
		int bestDist = 8;
		for (int i = 0; i < shortcuts.length; i++)
		{
			int dist = SceneTracker.distanceTo(shortcuts[i], hop);
			if (dist < bestDist)
			{
				best = shortcuts[i];
				bestDist = dist;
			}
		}
		return best;
	}

	private static WorldPoint firstTransportTile(List<WorldPoint> path)
	{
		int hopAt = Pathfinder.firstHopIndex(path);
		return hopAt < 0 ? null : path.get(hopAt);
	}

	private static WorldPoint nearestShortcutTile(WorldPoint hop)
	{
		WorldPoint[] tiles = {
			ZeahRcArea.BOULDER_SHORTCUT,
			ZeahRcArea.NORTH_SHORTCUT,
			ZeahRcArea.SHORTCUT,
			ZeahRcArea.EAST_SHORTCUT
		};
		WorldPoint best = hop;
		int bestDist = Integer.MAX_VALUE;
		for (int i = 0; i < tiles.length; i++)
		{
			int dist = hop.distanceTo(tiles[i]);
			if (dist < bestDist)
			{
				best = tiles[i];
				bestDist = dist;
			}
		}
		return bestDist <= 8 ? best : hop;
	}

	private static boolean isNear(TileObject object, WorldPoint expected)
	{
		if (object == null || expected == null)
		{
			return false;
		}
		WorldPoint loc = object.getWorldLocation();
		return loc != null && loc.distanceTo(expected) <= 6;
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
