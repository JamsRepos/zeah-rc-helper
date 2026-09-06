package com.zeahrchelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Value;
import net.runelite.api.TileObject;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

@Singleton
public class RcPathRouter
{
	private static final int AT_TARGET_TILES = 1;

	private final SceneTracker sceneTracker;

	private List<WorldPoint> cachedPath = Collections.emptyList();
	private WorldPoint cachedEnd;
	private RotationStep cachedStep = RotationStep.IDLE;
	private int cachedAgility = -1;

	@Inject
	RcPathRouter(SceneTracker sceneTracker)
	{
		this.sceneTracker = sceneTracker;
	}

	void reset()
	{
		clearCache();
	}

	private void clearCache()
	{
		cachedPath = Collections.emptyList();
		cachedEnd = null;
		cachedStep = RotationStep.IDLE;
		cachedAgility = -1;
	}

	List<WorldPoint> pathTo(
		WorldView worldView,
		WorldPoint start,
		WorldPoint end,
		RotationStep step,
		int agility,
		RcMode mode,
		boolean atMine,
		boolean showPath,
		boolean keepBloodAltarLoaded)
	{
		if (!showPath || start == null || end == null || start.distanceTo(end) <= AT_TARGET_TILES)
		{
			clearCache();
			return Collections.emptyList();
		}

		if (cachedStep == step && cachedAgility == agility && end.equals(cachedEnd) && cachedPath.size() > 1)
		{
			List<WorldPoint> trimmed = trimPath(cachedPath, start);
			if (trimmed != null)
			{
				cachedPath = trimmed;
				return Pathfinder.simplify(trimmed);
			}
		}

		List<WorldPoint> path;
		if (AgilityShortcut.WEST_73.enabled(agility, step, start, mode, atMine))
		{
			path = routeViaShortcut(worldView, start, end, AgilityShortcut.WEST_73);
		}
		else if (useSoulApproach(step, start, mode, atMine))
		{
			path = waypointApproachPath(worldView, start, end, ZeahRcArea.SOUL_APPROACH);
		}
		else if (useBloodApproach(step, start, mode))
		{
			path = waypointApproachPath(worldView, start, end, ZeahRcArea.BLOOD_APPROACH);
		}
		else if (keepBloodAltarLoaded && useDarkApproach(step, start, mode))
		{
			path = waypointApproachPath(
				worldView,
				start,
				end,
				List.of(ZeahRcArea.DARK_APPROACH),
				transports(agility, step, start, mode, atMine));
		}
		else
		{
			path = Pathfinder.find(worldView, start, end, transports(agility, step, start, mode, atMine));
		}

		cachedPath = path;
		cachedEnd = end;
		cachedStep = step;
		cachedAgility = agility;
		return Pathfinder.simplify(path);
	}

	ClickTarget nextClick(RotationStep step, TileObject destination, List<WorldPoint> path, WorldPoint player, boolean atMine)
	{
		if (step == RotationStep.CHISEL_AT_ALTAR || step == RotationStep.IDLE)
		{
			return ClickTarget.none();
		}
		if (player == null)
		{
			return ClickTarget.of(destination, null);
		}
		if (destination == null && needsRunestone(step) && atMine)
		{
			return ClickTarget.none();
		}
		if (destination != null && SceneTracker.distanceTo(destination, player) <= AT_TARGET_TILES)
		{
			return ClickTarget.of(destination, null);
		}

		AgilityShortcut.Hop hop = AgilityShortcut.firstHop(path, player);
		if (hop != null)
		{
			TileObject shortcut = objectForHop(hop);
			if (shortcut != null)
			{
				return ClickTarget.of(shortcut, null);
			}
			return ClickTarget.of(null, hop.getFrom());
		}
		return ClickTarget.of(destination, null);
	}

	static boolean needsRunestone(RotationStep step)
	{
		return step == RotationStep.MINE_FIRST
			|| step == RotationStep.MINE_SECOND
			|| step == RotationStep.RETURN_TO_MINE
			|| step == RotationStep.CHISEL_AND_RETURN;
	}

	static List<Pathfinder.Transport> transports(
		int agility,
		RotationStep step,
		WorldPoint player,
		RcMode mode,
		boolean atMine)
	{
		List<Pathfinder.Transport> out = new ArrayList<>();
		for (AgilityShortcut shortcut : AgilityShortcut.values())
		{
			if (!shortcut.enabled(agility, step, player, mode, atMine))
			{
				continue;
			}
			out.add(new Pathfinder.Transport(shortcut.getFrom(), shortcut.getTo()));
			if (shortcut.isBidirectional())
			{
				out.add(new Pathfinder.Transport(shortcut.getTo(), shortcut.getFrom()));
			}
		}
		return out;
	}

	static boolean useSoulApproach(RotationStep step, WorldPoint start, RcMode mode, boolean atMine)
	{
		if (step != RotationStep.GO_ALTAR || mode != RcMode.SOUL || start == null || atMine)
		{
			return false;
		}
		return start.getY() >= 3875 && start.getX() <= 1796;
	}

	static boolean useBloodApproach(RotationStep step, WorldPoint start, RcMode mode)
	{
		if (step != RotationStep.GO_ALTAR || mode != RcMode.BLOOD || start == null)
		{
			return false;
		}
		if (start.distanceTo(ZeahRcArea.BLOOD_ALTAR) <= 12)
		{
			return false;
		}
		return start.getX() <= 1745;
	}

	/**
	 * Blood trips reach the Dark Altar via {@link ZeahRcArea#DARK_APPROACH} so the scene
	 * rebuild fires there and keeps the Blood Altar loaded for a long-range click. Off once the
	 * player is within four tiles of the approach tile, which includes standing at the Dark Altar.
	 */
	static boolean useDarkApproach(RotationStep step, WorldPoint start, RcMode mode)
	{
		if (step != RotationStep.GO_DARK_FIRST && step != RotationStep.GO_DARK_SECOND)
		{
			return false;
		}
		if (mode != RcMode.BLOOD || start == null)
		{
			return false;
		}
		return start.distanceTo(ZeahRcArea.DARK_APPROACH) > 4;
	}

	private TileObject objectForHop(AgilityShortcut.Hop hop)
	{
		int id = AgilityShortcut.objectIdForHop(hop.getFrom(), hop.getTo());
		return id < 0 ? null : sceneTracker.objectForShortcutId(id);
	}

	private List<WorldPoint> routeViaShortcut(
		WorldView worldView,
		WorldPoint start,
		WorldPoint end,
		AgilityShortcut shortcut)
	{
		List<WorldPoint> path = new ArrayList<>();
		WorldPoint origin = shortcut.getFrom();
		WorldPoint landing = shortcut.getTo();
		if (start.distanceTo(origin) <= AT_TARGET_TILES)
		{
			path.add(origin);
		}
		else
		{
			List<WorldPoint> toOrigin = Pathfinder.find(worldView, start, origin, Collections.emptyList());
			if (!toOrigin.isEmpty())
			{
				appendLeg(path, toOrigin);
			}
			else
			{
				path.add(start);
			}
			if (path.isEmpty() || !path.get(path.size() - 1).equals(origin))
			{
				path.add(origin);
			}
		}
		if (!path.get(path.size() - 1).equals(landing))
		{
			path.add(landing);
		}
		List<WorldPoint> onward = Pathfinder.find(worldView, landing, end, Collections.emptyList());
		if (!onward.isEmpty())
		{
			appendLeg(path, onward);
		}
		else if (end != null && !end.equals(landing))
		{
			path.add(end);
		}
		return path;
	}

	private List<WorldPoint> waypointApproachPath(
		WorldView worldView,
		WorldPoint start,
		WorldPoint end,
		List<WorldPoint> waypoints)
	{
		return waypointApproachPath(worldView, start, end, waypoints, Collections.emptyList());
	}

	private List<WorldPoint> waypointApproachPath(
		WorldView worldView,
		WorldPoint start,
		WorldPoint end,
		List<WorldPoint> waypoints,
		List<Pathfinder.Transport> transports)
	{
		int from = firstRemainingWaypoint(start, waypoints);
		List<WorldPoint> path = new ArrayList<>();
		WorldPoint cursor = start;
		for (int i = from; i < waypoints.size(); i++)
		{
			WorldPoint waypoint = waypoints.get(i);
			List<WorldPoint> leg = Pathfinder.find(worldView, cursor, waypoint, transports);
			if (!leg.isEmpty())
			{
				appendLeg(path, leg);
				cursor = waypoint;
				continue;
			}
			if (worldView == null || LocalPoint.fromWorld(worldView, waypoint) == null)
			{
				if (path.isEmpty() && !start.equals(waypoint))
				{
					path.add(start);
				}
				if (path.isEmpty() || !path.get(path.size() - 1).equals(waypoint))
				{
					path.add(waypoint);
				}
				cursor = waypoint;
			}
		}

		List<WorldPoint> finish = Pathfinder.find(worldView, cursor, end, transports);
		if (!finish.isEmpty())
		{
			appendLeg(path, finish);
		}
		else if (end != null && (worldView == null || LocalPoint.fromWorld(worldView, end) == null))
		{
			if (path.isEmpty())
			{
				path.add(start);
			}
			if (!path.get(path.size() - 1).equals(end))
			{
				path.add(end);
			}
		}
		return path;
	}

	static int firstRemainingWaypoint(WorldPoint start, List<WorldPoint> waypoints)
	{
		if (waypoints == null || waypoints.isEmpty())
		{
			return 0;
		}

		int last = waypoints.size() - 1;
		if (start.distanceTo(waypoints.get(last)) <= 4)
		{
			return waypoints.size();
		}

		int bestSeg = 0;
		int bestDist = start.distanceTo(waypoints.get(0));
		int bestT = 0;

		for (int i = 0; i < waypoints.size() - 1; i++)
		{
			int[] proj = projectOntoSegment(start, waypoints.get(i), waypoints.get(i + 1));
			if (proj[1] < bestDist)
			{
				bestDist = proj[1];
				bestSeg = i;
				bestT = proj[0];
			}
		}

		if (bestSeg == 0 && bestT == 0)
		{
			return 0;
		}
		if (bestT >= 1000 || start.distanceTo(waypoints.get(bestSeg + 1)) <= 4)
		{
			return Math.min(bestSeg + 2, waypoints.size());
		}
		return bestSeg + 1;
	}

	/**
	 * @return {t permille along A→B (0-1000), Chebyshev distance to the segment}
	 */
	static int[] projectOntoSegment(WorldPoint point, WorldPoint a, WorldPoint b)
	{
		int dx = b.getX() - a.getX();
		int dy = b.getY() - a.getY();
		int den = dx * dx + dy * dy;
		if (den == 0)
		{
			return new int[]{0, point.distanceTo(a)};
		}
		long t = (long) (point.getX() - a.getX()) * dx + (long) (point.getY() - a.getY()) * dy;
		if (t <= 0)
		{
			return new int[]{0, point.distanceTo(a)};
		}
		if (t >= den)
		{
			return new int[]{1000, point.distanceTo(b)};
		}
		int px = a.getX() + (int) (t * dx / den);
		int py = a.getY() + (int) (t * dy / den);
		int dist = Math.max(Math.abs(point.getX() - px), Math.abs(point.getY() - py));
		return new int[]{(int) (t * 1000 / den), dist};
	}

	static List<WorldPoint> trimPath(List<WorldPoint> path, WorldPoint start)
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
		if (bestDist > AgilityShortcut.PATH_DRIFT_TILES)
		{
			return null;
		}
		if (best == 0)
		{
			return path;
		}
		return new ArrayList<>(path.subList(best, path.size()));
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

	@Value
	static class ClickTarget
	{
		TileObject object;
		WorldPoint tile;

		static ClickTarget none()
		{
			return new ClickTarget(null, null);
		}

		static ClickTarget of(TileObject object, WorldPoint tile)
		{
			return new ClickTarget(object, tile);
		}
	}
}
