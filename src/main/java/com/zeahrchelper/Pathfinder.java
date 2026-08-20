package com.zeahrchelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import net.runelite.api.CollisionData;
import net.runelite.api.CollisionDataFlag;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

/**
 * Scene-local A* with optional agility-shortcut transports.
 */
final class Pathfinder
{
	private static final int MAX_NODES = 6000;
	private static final int WALKABLE_SEARCH = 6;
	private static final int TRANSPORT_REACH = 1;
	/** Consecutive tiles farther than this are an agility hop, not a walk. */
	static final int HOP_TILES = 2;

	private Pathfinder()
	{
	}

	static final class Transport
	{
		final WorldPoint from;
		final WorldPoint to;

		Transport(WorldPoint from, WorldPoint to)
		{
			this.from = from;
			this.to = to;
		}
	}

	static List<WorldPoint> find(WorldView worldView, WorldPoint start, WorldPoint end, List<Transport> transports)
	{
		if (worldView == null || start == null || end == null || start.getPlane() != end.getPlane())
		{
			return Collections.emptyList();
		}

		CollisionData[] maps = worldView.getCollisionMaps();
		if (maps == null)
		{
			return Collections.emptyList();
		}
		int plane = worldView.getPlane();
		if (plane < 0 || plane >= maps.length || maps[plane] == null)
		{
			return Collections.emptyList();
		}

		int[][] flags = maps[plane].getFlags();
		if (flags == null || flags.length == 0)
		{
			return Collections.emptyList();
		}

		LocalPoint startLocal = LocalPoint.fromWorld(worldView, start);
		if (startLocal == null)
		{
			return Collections.emptyList();
		}

		LocalPoint endLocal = LocalPoint.fromWorld(worldView, end);
		if (endLocal == null)
		{
			WorldPoint clamped = clampToScene(worldView, start, end);
			endLocal = clamped == null ? null : LocalPoint.fromWorld(worldView, clamped);
		}
		if (endLocal == null)
		{
			return Collections.emptyList();
		}

		int size = flags.length;
		int sx = startLocal.getSceneX();
		int sy = startLocal.getSceneY();
		int[] goal = nearestWalkable(flags, endLocal.getSceneX(), endLocal.getSceneY(), size);
		if (!inBounds(sx, sy, size) || goal == null)
		{
			return Collections.emptyList();
		}
		int ex = goal[0];
		int ey = goal[1];
		if (sx == ex && sy == ey)
		{
			return Collections.emptyList();
		}

		int[][] hops = sceneTransports(worldView, transports, flags, size);

		boolean[][] seen = new boolean[size][size];
		PriorityQueue<Node> open = new PriorityQueue<>(Comparator.comparingInt(n -> n.f));
		open.add(new Node(sx, sy, 0, heuristic(sx, sy, ex, ey, hops), null));
		seen[sx][sy] = true;

		int visited = 0;
		Node found = null;
		while (!open.isEmpty() && visited < MAX_NODES)
		{
			Node cur = open.poll();
			visited++;
			if (cur.x == ex && cur.y == ey)
			{
				found = cur;
				break;
			}

			for (int dx = -1; dx <= 1; dx++)
			{
				for (int dy = -1; dy <= 1; dy++)
				{
					if (dx == 0 && dy == 0)
					{
						continue;
					}
					tryAdd(open, seen, flags, size, cur, cur.x + dx, cur.y + dy, dx, dy, true, ex, ey, hops);
				}
			}

			if (hops.length > 0)
			{
				for (int i = 0; i < hops.length; i++)
				{
					int[] hop = hops[i];
					if (Math.max(Math.abs(cur.x - hop[0]), Math.abs(cur.y - hop[1])) <= TRANSPORT_REACH)
					{
						tryAdd(open, seen, flags, size, cur, hop[2], hop[3], 0, 0, false, ex, ey, hops);
					}
				}
			}
		}

		if (found == null)
		{
			return Collections.emptyList();
		}

		List<WorldPoint> path = new ArrayList<>();
		for (Node n = found; n != null; n = n.parent)
		{
			path.add(WorldPoint.fromScene(worldView, n.x, n.y, plane));
		}
		Collections.reverse(path);
		return path;
	}

	/**
	 * If the goal is outside the loaded scene, walk toward it as far as the scene allows.
	 */
	static WorldPoint clampToScene(WorldView worldView, WorldPoint start, WorldPoint end)
	{
		if (worldView == null || start == null || end == null)
		{
			return null;
		}
		if (LocalPoint.fromWorld(worldView, end) != null)
		{
			return end;
		}

		int dx = end.getX() - start.getX();
		int dy = end.getY() - start.getY();
		int steps = Math.max(Math.abs(dx), Math.abs(dy));
		if (steps <= 0)
		{
			return null;
		}

		WorldPoint last = null;
		for (int i = 1; i <= steps; i++)
		{
			WorldPoint point = new WorldPoint(
				start.getX() + dx * i / steps,
				start.getY() + dy * i / steps,
				start.getPlane());
			if (LocalPoint.fromWorld(worldView, point) == null)
			{
				break;
			}
			last = point;
		}
		return last;
	}

	static boolean isHop(WorldPoint a, WorldPoint b)
	{
		return a != null && b != null && a.distanceTo(b) > HOP_TILES;
	}

	static int firstHopIndex(List<WorldPoint> path)
	{
		if (path == null || path.size() < 2)
		{
			return -1;
		}
		for (int i = 0; i < path.size() - 1; i++)
		{
			if (isHop(path.get(i), path.get(i + 1)))
			{
				return i;
			}
		}
		return -1;
	}

	static List<WorldPoint> simplify(List<WorldPoint> path)
	{
		if (path == null || path.size() < 3)
		{
			return path == null ? Collections.emptyList() : path;
		}

		List<WorldPoint> out = new ArrayList<>();
		out.add(path.get(0));
		for (int i = 1; i < path.size() - 1; i++)
		{
			WorldPoint prev = out.get(out.size() - 1);
			WorldPoint cur = path.get(i);
			WorldPoint next = path.get(i + 1);
			int dx1 = cur.getX() - prev.getX();
			int dy1 = cur.getY() - prev.getY();
			int dx2 = next.getX() - cur.getX();
			int dy2 = next.getY() - cur.getY();
			// Keep agility hop endpoints so the line goes to the shortcut, then continues.
			if (isHop(prev, cur) || isHop(cur, next) || dx1 * dy2 != dy1 * dx2)
			{
				out.add(cur);
			}
		}
		out.add(path.get(path.size() - 1));
		return out;
	}

	private static void tryAdd(
		PriorityQueue<Node> open,
		boolean[][] seen,
		int[][] flags,
		int size,
		Node cur,
		int nx,
		int ny,
		int dx,
		int dy,
		boolean walking,
		int ex,
		int ey,
		int[][] hops)
	{
		if (!inBounds(nx, ny, size) || seen[nx][ny] || (nx == cur.x && ny == cur.y))
		{
			return;
		}
		if (walking && !canMove(flags, cur.x, cur.y, dx, dy, size))
		{
			return;
		}
		if (!walking && (flags[nx][ny] & CollisionDataFlag.BLOCK_MOVEMENT_FULL) != 0)
		{
			int[] walkable = nearestWalkable(flags, nx, ny, size);
			if (walkable == null)
			{
				return;
			}
			nx = walkable[0];
			ny = walkable[1];
			if (seen[nx][ny] || (nx == cur.x && ny == cur.y))
			{
				return;
			}
		}
		seen[nx][ny] = true;
		int step = walking ? ((dx != 0 && dy != 0) ? 3 : 2) : 2;
		int g = cur.g + step;
		open.add(new Node(nx, ny, g, g + heuristic(nx, ny, ex, ey, hops), cur));
	}

	private static int[][] sceneTransports(WorldView worldView, List<Transport> transports, int[][] flags, int size)
	{
		if (transports == null || transports.isEmpty())
		{
			return new int[0][];
		}
		List<int[]> hops = new ArrayList<>();
		for (int i = 0; i < transports.size(); i++)
		{
			Transport transport = transports.get(i);
			LocalPoint from = LocalPoint.fromWorld(worldView, transport.from);
			LocalPoint to = LocalPoint.fromWorld(worldView, transport.to);
			if (from == null || to == null)
			{
				continue;
			}
			int[] origin = nearestWalkable(flags, from.getSceneX(), from.getSceneY(), size);
			int[] dest = nearestWalkable(flags, to.getSceneX(), to.getSceneY(), size);
			if (origin == null || dest == null || (origin[0] == dest[0] && origin[1] == dest[1]))
			{
				continue;
			}
			hops.add(new int[]{origin[0], origin[1], dest[0], dest[1]});
		}
		return hops.toArray(new int[0][]);
	}

	private static int[] nearestWalkable(int[][] flags, int x, int y, int size)
	{
		if (inBounds(x, y, size) && (flags[x][y] & CollisionDataFlag.BLOCK_MOVEMENT_FULL) == 0)
		{
			return new int[]{x, y};
		}
		for (int r = 1; r <= WALKABLE_SEARCH; r++)
		{
			for (int dx = -r; dx <= r; dx++)
			{
				for (int dy = -r; dy <= r; dy++)
				{
					if (Math.abs(dx) != r && Math.abs(dy) != r)
					{
						continue;
					}
					int nx = x + dx;
					int ny = y + dy;
					if (inBounds(nx, ny, size) && (flags[nx][ny] & CollisionDataFlag.BLOCK_MOVEMENT_FULL) == 0)
					{
						return new int[]{nx, ny};
					}
				}
			}
		}
		return inBounds(x, y, size) ? new int[]{x, y} : null;
	}

	private static int heuristic(int x, int y, int ex, int ey, int[][] hops)
	{
		int best = octile(x, y, ex, ey);
		for (int i = 0; i < hops.length; i++)
		{
			int[] hop = hops[i];
			int via = octile(x, y, hop[0], hop[1]) + 2 + octile(hop[2], hop[3], ex, ey);
			if (via < best)
			{
				best = via;
			}
		}
		return best;
	}

	private static int octile(int x, int y, int ex, int ey)
	{
		int dx = Math.abs(x - ex);
		int dy = Math.abs(y - ey);
		return 2 * Math.max(dx, dy) + Math.min(dx, dy);
	}

	private static boolean inBounds(int x, int y, int size)
	{
		return x >= 0 && y >= 0 && x < size && y < size;
	}

	private static boolean canMove(int[][] flags, int x, int y, int dx, int dy, int size)
	{
		int nx = x + dx;
		int ny = y + dy;
		if (!inBounds(nx, ny, size))
		{
			return false;
		}

		if (dx != 0 && dy != 0)
		{
			return canMove(flags, x, y, dx, 0, size) && canMove(flags, x, y, 0, dy, size)
				&& (flags[nx][ny] & CollisionDataFlag.BLOCK_MOVEMENT_FULL) == 0;
		}

		if ((flags[nx][ny] & CollisionDataFlag.BLOCK_MOVEMENT_FULL) != 0)
		{
			return false;
		}

		int cur = flags[x][y];
		int dest = flags[nx][ny];
		if (dx == 1)
		{
			return (cur & CollisionDataFlag.BLOCK_MOVEMENT_EAST) == 0
				&& (dest & CollisionDataFlag.BLOCK_MOVEMENT_WEST) == 0;
		}
		if (dx == -1)
		{
			return (cur & CollisionDataFlag.BLOCK_MOVEMENT_WEST) == 0
				&& (dest & CollisionDataFlag.BLOCK_MOVEMENT_EAST) == 0;
		}
		if (dy == 1)
		{
			return (cur & CollisionDataFlag.BLOCK_MOVEMENT_NORTH) == 0
				&& (dest & CollisionDataFlag.BLOCK_MOVEMENT_SOUTH) == 0;
		}
		return (cur & CollisionDataFlag.BLOCK_MOVEMENT_SOUTH) == 0
			&& (dest & CollisionDataFlag.BLOCK_MOVEMENT_NORTH) == 0;
	}

	private static final class Node
	{
		final int x;
		final int y;
		final int g;
		final int f;
		final Node parent;

		Node(int x, int y, int g, int f, Node parent)
		{
			this.x = x;
			this.y = y;
			this.g = g;
			this.f = f;
			this.parent = parent;
		}
	}
}
