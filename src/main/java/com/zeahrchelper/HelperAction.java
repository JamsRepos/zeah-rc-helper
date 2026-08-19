package com.zeahrchelper;

import java.awt.Color;
import java.util.List;
import lombok.Value;
import net.runelite.api.coords.WorldPoint;

@Value
public class HelperAction
{
	RotationStep step;
	String detail;
	List<WorldPoint> highlightTiles;
	Color color;

	public static HelperAction idle()
	{
		return new HelperAction(RotationStep.IDLE, "Waiting…", List.of(), Color.GRAY);
	}
}
