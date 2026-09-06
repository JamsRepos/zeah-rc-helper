package com.zeahrchelper;

import java.awt.Color;
import java.util.Collections;
import java.util.List;
import lombok.Value;
import net.runelite.api.TileObject;
import net.runelite.api.coords.WorldPoint;

@Value
public class HelperAction
{
	RotationStep step;
	String detail;
	List<WorldPoint> path;
	TileObject highlightObject;
	WorldPoint highlightTile;
	Color color;
	/** Short canvas label on the highlight (e.g. "Stand Here", "Click Here"). */
	String worldHint;

	public static HelperAction idle()
	{
		return new HelperAction(
			RotationStep.IDLE, "Waiting…", Collections.emptyList(), null, null, Color.GRAY, null);
	}
}
