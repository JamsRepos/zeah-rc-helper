package com.zeahrchelper;

import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.DecorativeObject;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.GroundObject;
import net.runelite.api.Player;
import net.runelite.api.Scene;
import net.runelite.api.Tile;
import net.runelite.api.TileObject;
import net.runelite.api.WallObject;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.gameval.VarbitID;

/**
 * Tracks Arceuus RC scene objects from spawn/despawn (and a one-shot scan on login).
 */
@Singleton
public class SceneTracker
{
	private static final int RUNESTONE_NEAR_TILES = 4;

	private final Client client;

	@Getter
	private TileObject runestoneNorth;
	@Getter
	private TileObject runestoneSouth;
	@Getter
	private TileObject darkAltar;
	@Getter
	private TileObject bloodAltar;
	@Getter
	private TileObject soulAltar;
	@Getter
	private TileObject shortcut73;
	@Getter
	private TileObject shortcut69;
	@Getter
	private TileObject shortcut52Outer;
	@Getter
	private TileObject shortcut52Inner;
	@Getter
	private TileObject shortcut49;

	@Inject
	SceneTracker(Client client)
	{
		this.client = client;
	}

	public void reset()
	{
		runestoneNorth = null;
		runestoneSouth = null;
		darkAltar = null;
		bloodAltar = null;
		soulAltar = null;
		shortcut73 = null;
		shortcut69 = null;
		shortcut52Outer = null;
		shortcut52Inner = null;
		shortcut49 = null;
	}

	public void onSpawn(TileObject object)
	{
		try
		{
			if (object == null)
			{
				return;
			}
			assign(object, false);
		}
		catch (Exception ex)
		{
			// Simulated scene events during startup should never break the plugin.
		}
	}

	public void onDespawn(TileObject object)
	{
		try
		{
			if (object == null)
			{
				return;
			}
			assign(object, true);
		}
		catch (Exception ex)
		{
			// ignore
		}
	}

	public void scanScene()
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		WorldView worldView = client.getTopLevelWorldView();
		if (worldView == null)
		{
			return;
		}

		Scene scene = worldView.getScene();
		if (scene == null)
		{
			return;
		}

		Tile[][][] tiles = scene.getTiles();
		if (tiles == null)
		{
			return;
		}

		int z = worldView.getPlane();
		if (z < 0 || z >= tiles.length || tiles[z] == null)
		{
			return;
		}

		for (Tile[] row : tiles[z])
		{
			if (row == null)
			{
				continue;
			}
			for (Tile tile : row)
			{
				scanTile(tile);
			}
		}
	}

	public boolean isNorthMineable()
	{
		return mineableFromVarbits(true);
	}

	public boolean isSouthMineable()
	{
		return mineableFromVarbits(false);
	}

	public TileObject chooseRunestone()
	{
		Player player = client.getLocalPlayer();
		if (isMining(player))
		{
			return null;
		}

		WorldPoint loc = player == null ? null : player.getWorldLocation();
		boolean northDense = isNorthMineable();
		boolean southDense = isSouthMineable();

		if (northDense && southDense)
		{
			boolean atNorth = loc != null && distanceTo(runestoneNorth, loc) <= RUNESTONE_NEAR_TILES;
			return atNorth ? runestoneNorth : runestoneSouth;
		}
		if (southDense)
		{
			return runestoneSouth;
		}
		if (northDense)
		{
			return runestoneNorth;
		}
		return null;
	}

	private static boolean isMining(Player player)
	{
		if (player == null)
		{
			return false;
		}
		return player.getAnimation() != -1;
	}

	public TileObject altarFor(RcMode mode)
	{
		return mode == RcMode.SOUL ? soulAltar : bloodAltar;
	}

	public boolean isNearAltar(RcMode mode, int tiles)
	{
		Player player = client.getLocalPlayer();
		if (player == null)
		{
			return false;
		}
		WorldPoint loc = player.getWorldLocation();
		if (distanceTo(altarFor(mode), loc) <= tiles)
		{
			return true;
		}
		WorldPoint fallback = mode == RcMode.SOUL ? ZeahRcArea.SOUL_ALTAR : ZeahRcArea.BLOOD_ALTAR;
		return loc.distanceTo(fallback) <= tiles;
	}

	/** True when the Blood Altar's tile lies inside the base 104x104 scene (not the extended band). */
	public boolean isBloodAltarInScene()
	{
		WorldView wv = client.getTopLevelWorldView();
		if (wv == null)
		{
			return false;
		}
		WorldPoint p = ZeahRcArea.BLOOD_ALTAR;
		return WorldPoint.isInScene(wv, p.getX(), p.getY());
	}

	public TileObject objectForShortcutId(int id)
	{
		if (id == ObjectID.ARCHEUUS_RUNESTONE_SHORTCUT_BOULDER)
		{
			return shortcut49;
		}
		if (id == AgilityShortcut.NORTH_OBJECT_ID)
		{
			return shortcut69;
		}
		if (id == ObjectID.ARCHEUUS_RUNESTONE_SHORTCUT_MIDGREY_BOTTOM)
		{
			return shortcut52Inner;
		}
		if (id == ObjectID.ARCHEUUS_RUNESTONE_SHORTCUT_MIDGREY_TOP)
		{
			return shortcut52Outer;
		}
		if (id == ObjectID.ARCHEUUS_RUNESTONE_SHORTCUT_GREY_TOP
			|| id == ObjectID.ARCHEUUS_RUNESTONE_SHORTCUT_GREY_BOTTOM
			|| id == ObjectID.ARCHEUUS_RUNESTONE_SHORTCUT_GREY_MIDDLE)
		{
			return shortcut73;
		}
		return null;
	}

	public boolean isAtMine(WorldPoint loc)
	{
		if (loc == null)
		{
			return false;
		}
		if (distanceTo(runestoneNorth, loc) <= 8 || distanceTo(runestoneSouth, loc) <= 8)
		{
			return true;
		}
		return loc.distanceTo(ZeahRcArea.RUNESTONE_SOUTH) <= 10
			|| loc.distanceTo(ZeahRcArea.RUNESTONE_NORTH) <= 10;
	}

	static int distanceTo(TileObject object, WorldPoint loc)
	{
		if (object == null || loc == null)
		{
			return Integer.MAX_VALUE;
		}
		if (object instanceof GameObject)
		{
			GameObject go = (GameObject) object;
			WorldArea area = new WorldArea(go.getWorldLocation(), Math.max(1, go.sizeX()), Math.max(1, go.sizeY()));
			return area.distanceTo(loc);
		}
		return object.getWorldLocation().distanceTo(loc);
	}

	private void scanTile(Tile tile)
	{
		if (tile == null)
		{
			return;
		}
		GameObject[] objects = tile.getGameObjects();
		if (objects != null)
		{
			for (GameObject object : objects)
			{
				onSpawn(object);
			}
		}
		DecorativeObject deco = tile.getDecorativeObject();
		if (deco != null)
		{
			onSpawn(deco);
		}
		GroundObject ground = tile.getGroundObject();
		if (ground != null)
		{
			onSpawn(ground);
		}
		WallObject wall = tile.getWallObject();
		if (wall != null)
		{
			onSpawn(wall);
		}
	}

	private void assign(TileObject object, boolean despawn)
	{
		int id = object.getId();
		switch (id)
		{
			// The base objects persist through depletion; the varbits carry the mineable state.
			case ObjectID.ARCEUUS_RUNESTONE_BASE_1:
				runestoneNorth = despawn ? clear(runestoneNorth, object) : object;
				break;
			case ObjectID.ARCEUUS_RUNESTONE_BASE_2:
				runestoneSouth = despawn ? clear(runestoneSouth, object) : object;
				break;
			case ObjectID.ARCEUUS_ALTAR:
			case ObjectID.ARCHEUS_ALTAR_DARK:
				darkAltar = despawn ? clear(darkAltar, object) : prefer(darkAltar, object);
				break;
			case ObjectID.ARCHEUS_ALTAR_BLOOD:
				bloodAltar = despawn ? clear(bloodAltar, object) : prefer(bloodAltar, object);
				break;
			case ObjectID.ARCHEUS_ALTAR_SOUL:
				soulAltar = despawn ? clear(soulAltar, object) : prefer(soulAltar, object);
				break;
			case ObjectID.ARCHEUUS_RUNESTONE_SHORTCUT_GREY_TOP:
			case ObjectID.ARCHEUUS_RUNESTONE_SHORTCUT_GREY_BOTTOM:
			case ObjectID.ARCHEUUS_RUNESTONE_SHORTCUT_GREY_MIDDLE:
				shortcut73 = despawn ? clear(shortcut73, object) : preferNear(shortcut73, object, ZeahRcArea.SHORTCUT);
				break;
			case AgilityShortcut.NORTH_OBJECT_ID:
				shortcut69 = despawn ? clear(shortcut69, object) : preferNear(shortcut69, object, ZeahRcArea.NORTH_SHORTCUT);
				break;
			case ObjectID.ARCHEUUS_RUNESTONE_SHORTCUT_MIDGREY_TOP:
				shortcut52Outer = despawn ? clear(shortcut52Outer, object) : preferNear(shortcut52Outer, object, ZeahRcArea.EAST_SHORTCUT);
				break;
			case ObjectID.ARCHEUUS_RUNESTONE_SHORTCUT_MIDGREY_BOTTOM:
				shortcut52Inner = despawn ? clear(shortcut52Inner, object) : preferNear(shortcut52Inner, object, ZeahRcArea.EAST_SHORTCUT);
				break;
			case ObjectID.ARCHEUUS_RUNESTONE_SHORTCUT_BOULDER:
				shortcut49 = despawn ? clear(shortcut49, object) : preferNear(shortcut49, object, ZeahRcArea.BOULDER_SHORTCUT);
				break;
			default:
				break;
		}
	}

	private static TileObject prefer(TileObject current, TileObject candidate)
	{
		return current == null ? candidate : current;
	}

	private static TileObject preferNear(TileObject current, TileObject candidate, WorldPoint expected)
	{
		if (current == null)
		{
			return candidate;
		}
		int currentDist = distanceToPoint(current, expected);
		int candidateDist = distanceToPoint(candidate, expected);
		return candidateDist < currentDist ? candidate : current;
	}

	private static int distanceToPoint(TileObject object, WorldPoint expected)
	{
		if (object == null || expected == null)
		{
			return Integer.MAX_VALUE;
		}
		WorldPoint loc = object.getWorldLocation();
		return loc == null ? Integer.MAX_VALUE : loc.distanceTo(expected);
	}

	private boolean mineableFromVarbits(boolean north)
	{
		int value = client.getVarbitValue(north ? VarbitID.ARCEUUS_RUNESTONE_1 : VarbitID.ARCEUUS_RUNESTONE_2);
		return value == 0;
	}

	private static TileObject clear(TileObject current, TileObject gone)
	{
		if (current == null || gone == null)
		{
			return current;
		}
		if (current == gone)
		{
			return null;
		}
		WorldPoint a = templatePointSafe(current);
		WorldPoint b = templatePointSafe(gone);
		if (current.getId() == gone.getId() && a != null && a.equals(b))
		{
			return null;
		}
		return current;
	}

	private static WorldPoint templatePointSafe(TileObject object)
	{
		try
		{
			return object.getWorldLocation();
		}
		catch (Exception ex)
		{
			return null;
		}
	}
}
