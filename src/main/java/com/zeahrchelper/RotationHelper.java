package com.zeahrchelper;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;

@Singleton
public class RotationHelper
{
	private static final int FULL_FRAGMENTS = 100;
	private static final int SOUL_LEVEL = 90;
	private static final int SHORTCUT_AGILITY = 73;

	private final Client client;
	private final ZeahRcHelperConfig config;
	private final InventoryChecker inventoryChecker;
	private final ReminderService reminderService;

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
		ReminderService reminderService)
	{
		this.client = client;
		this.config = config;
		this.inventoryChecker = inventoryChecker;
		this.reminderService = reminderService;
	}

	public void reset()
	{
		currentAction = HelperAction.idle();
		tripsCompleted = 0;
		lastStep = RotationStep.IDLE;
	}

	public void update()
	{
		if (!ZeahRcArea.isInArceuusRc(client))
		{
			currentAction = HelperAction.idle();
			reminderService.update(snapshot, resolvedMode, false);
			return;
		}

		resolvedMode = resolveMode();
		snapshot = inventoryChecker.scan();
		reminderService.update(snapshot, resolvedMode, true);

		if (!config.enableHelper())
		{
			currentAction = HelperAction.idle();
			return;
		}

		RotationStep step = inferStep(snapshot);
		if (isTripCompleteTransition(lastStep, step))
		{
			tripsCompleted++;
		}
		lastStep = step;

		List<WorldPoint> tiles = new ArrayList<>(step.highlightTiles(resolvedMode));
		if (client.getRealSkillLevel(Skill.AGILITY) < SHORTCUT_AGILITY)
		{
			tiles.remove(ZeahRcArea.SHORTCUT);
		}
		String detail = detailFor(step, snapshot);
		currentAction = new HelperAction(step, detail, tiles, step.getColor());
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
		boolean atAltar = ZeahRcArea.isAtCraftAltar(client, resolvedMode);
		boolean atMine = ZeahRcArea.isAtMine(client);

		if (atAltar && inv.getFragments() > 0)
		{
			return inv.getDarkBlocks() > 0 ? RotationStep.CRAFT_FRAGMENTS : RotationStep.CRAFT_REMAINING;
		}
		if (atAltar && inv.getDarkBlocks() > 0)
		{
			return RotationStep.CHISEL_AT_ALTAR;
		}
		if (atAltar && inv.getFragments() == 0 && inv.getDarkBlocks() == 0 && inv.getDenseBlocks() == 0)
		{
			return RotationStep.RETURN_TO_MINE;
		}

		if (inv.getFragments() >= FULL_FRAGMENTS && inv.getDarkBlocks() > 0)
		{
			return RotationStep.GO_ALTAR;
		}
		if (inv.getFragments() >= FULL_FRAGMENTS && inv.getDenseBlocks() > 0 && inv.getEmptySlots() == 0)
		{
			return RotationStep.GO_DARK_SECOND;
		}
		if (inv.getFragments() >= FULL_FRAGMENTS && inv.getDenseBlocks() == 0 && inv.getDarkBlocks() == 0)
		{
			return RotationStep.MINE_SECOND;
		}
		if (inv.getDarkBlocks() > 0 && inv.getFragments() == 0)
		{
			return RotationStep.CHISEL_AND_RETURN;
		}
		if (inv.getDenseBlocks() > 0 && inv.getEmptySlots() == 0 && inv.getFragments() == 0)
		{
			return RotationStep.GO_DARK_FIRST;
		}
		if (!atMine && inv.getDenseBlocks() > 0 && inv.getFragments() == 0)
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
				return "Fill your inventory with dense essence (" + inv.getDenseBlocks() + " so far)";
			case GO_DARK_FIRST:
				return "Click the Dark Altar to venerate all dense blocks";
			case CHISEL_AND_RETURN:
				return "Use chisel on dark blocks while running back to the mine";
			case MINE_SECOND:
				return "Mine another inventory — keep the fragment stack";
			case GO_DARK_SECOND:
				return "Venerate the second inventory at the Dark Altar";
			case GO_ALTAR:
				return "Run to the " + rune + " altar with fragments + dark blocks";
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
