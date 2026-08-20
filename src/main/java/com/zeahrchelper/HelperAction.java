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
	/** After this tile the path continues on the other side of a shortcut; do not draw through the hop. */
	WorldPoint pathGapAfter;
	TileObject highlightObject;
	WorldPoint highlightTile;
	Color color;

	public static HelperAction idle()
	{
		return new HelperAction(RotationStep.IDLE, "Waiting…", Collections.emptyList(), null, null, null, Color.GRAY);
	}
}
