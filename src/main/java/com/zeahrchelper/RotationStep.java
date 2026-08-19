package com.zeahrchelper;

import java.awt.Color;
import java.util.List;
import lombok.Getter;
import net.runelite.api.coords.WorldPoint;

/**
 * Click-here steps for the standard Arceuus blood/soul rotation.
 */
@Getter
public enum RotationStep
{
	MINE_FIRST("Mine dense essence (first inventory)", Color.CYAN),
	GO_DARK_FIRST("Venerate at the Dark Altar", new Color(160, 80, 255)),
	CHISEL_AND_RETURN("Chisel into fragments, return to the mine", Color.ORANGE),
	MINE_SECOND("Mine dense essence (second inventory)", Color.CYAN),
	GO_DARK_SECOND("Venerate the second inventory", new Color(160, 80, 255)),
	GO_ALTAR("Run to the altar", new Color(220, 40, 40)),
	CRAFT_FRAGMENTS("Craft fragments at the altar", new Color(220, 40, 40)),
	CHISEL_AT_ALTAR("Chisel remaining dark blocks", Color.ORANGE),
	CRAFT_REMAINING("Craft the remaining fragments", new Color(220, 40, 40)),
	RETURN_TO_MINE("Return to the dense essence mine", Color.CYAN),
	IDLE("Waiting…", Color.GRAY);

	private final String label;
	private final Color color;

	RotationStep(String label, Color color)
	{
		this.label = label;
		this.color = color;
	}

	public List<WorldPoint> highlightTiles(RcMode resolvedMode)
	{
		switch (this)
		{
			case MINE_FIRST:
			case MINE_SECOND:
				return ZeahRcArea.RUNESTONES;
			case GO_DARK_FIRST:
			case GO_DARK_SECOND:
				return List.of(ZeahRcArea.DARK_ALTAR);
			case CHISEL_AND_RETURN:
			case RETURN_TO_MINE:
				return List.of(ZeahRcArea.SHORTCUT, ZeahRcArea.MINE_STAND);
			case GO_ALTAR:
			case CRAFT_FRAGMENTS:
			case CRAFT_REMAINING:
				return List.of(resolvedMode == RcMode.SOUL ? ZeahRcArea.SOUL_ALTAR : ZeahRcArea.BLOOD_ALTAR);
			case CHISEL_AT_ALTAR:
				return List.of(resolvedMode == RcMode.SOUL ? ZeahRcArea.SOUL_ALTAR : ZeahRcArea.BLOOD_ALTAR);
			default:
				return List.of();
		}
	}
}
