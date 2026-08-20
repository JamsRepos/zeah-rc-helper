package com.zeahrchelper;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.util.Text;

@Singleton
public class InventoryChecker
{
	private static final int INVENTORY_SIZE = 28;
	private static final int FRAGMENTS_PER_BLOCK = 4;
	private static final int MAX_FRAGMENTS = 111;
	private static final int TYPICAL_FULL_STACK = 108;
	private static final Pattern COUNT_MANY = Pattern.compile(
		"this stack of fragments is roughly equivalent to (\\d+) pieces? of essence",
		Pattern.CASE_INSENSITIVE);
	private static final Pattern COUNT_ONE = Pattern.compile(
		"this stack of fragments is roughly equivalent to one piece of essence",
		Pattern.CASE_INSENSITIVE);

	private final Client client;

	private int trackedFragments;
	private int lastDarkBlocks = -1;

	@Inject
	InventoryChecker(Client client)
	{
		this.client = client;
	}

	public void reset()
	{
		trackedFragments = 0;
		lastDarkBlocks = -1;
	}

	public void onChatMessage(ChatMessage event)
	{
		String message = Text.removeTags(event.getMessage());
		if (COUNT_ONE.matcher(message).find())
		{
			trackedFragments = 1;
			return;
		}
		Matcher many = COUNT_MANY.matcher(message);
		if (many.find())
		{
			try
			{
				trackedFragments = Math.min(MAX_FRAGMENTS, Integer.parseInt(many.group(1)));
			}
			catch (NumberFormatException ignored)
			{
				// Keep the previous estimate.
			}
		}
	}

	public InventorySnapshot scan()
	{
		int dense = 0;
		int dark = 0;
		int fragmentQty = 0;
		boolean hasFragmentItem = false;
		int empty = 0;
		boolean chisel = false;
		boolean pickaxe = false;
		boolean inactiveEssence = false;
		boolean activeEssence = false;
		boolean lanternInv = false;
		int lanternId = -1;

		ItemContainer inventory = client.getItemContainer(InventoryID.INV);
		if (inventory == null)
		{
			empty = INVENTORY_SIZE;
		}
		else
		{
			Item[] items = inventory.getItems();
			for (int i = 0; i < INVENTORY_SIZE; i++)
			{
				Item item = i < items.length ? items[i] : null;
				if (item == null || item.getId() < 0)
				{
					empty++;
					continue;
				}

				int id = item.getId();
				int qty = Math.max(0, item.getQuantity());
				if (id == ItemID.ARCEUUS_ESSENCE_BLOCK)
				{
					dense += Math.max(1, qty);
				}
				else if (id == ItemID.ARCEUUS_ESSENCE_BLOCK_DARK)
				{
					dark += Math.max(1, qty);
				}
				else if (id == ItemID.BIGBLANKRUNE)
				{
					hasFragmentItem = true;
					fragmentQty += qty;
				}
				else if (id == ItemID.CHISEL)
				{
					chisel = true;
				}
				else if (id == ItemID.BLOOD_ESSENCE_INACTIVE)
				{
					inactiveEssence = true;
				}
				else if (id == ItemID.BLOOD_ESSENCE_ACTIVE)
				{
					activeEssence = true;
				}
				else if (isAbyssalLantern(id))
				{
					lanternInv = true;
					lanternId = id;
				}
				else if (isPickaxe(id))
				{
					pickaxe = true;
				}
			}
		}

		int widgetQty = fragmentQuantityFromWidget();
		int fragments = resolveFragmentCount(hasFragmentItem, fragmentQty, widgetQty, dark, empty);

		ItemContainer equipment = client.getItemContainer(InventoryID.WORN);
		if (equipment != null)
		{
			Item shield = equipment.getItem(EquipmentInventorySlot.SHIELD.getSlotIdx());
			if (shield != null && isAbyssalLantern(shield.getId()))
			{
				lanternId = shield.getId();
			}
			Item weapon = equipment.getItem(EquipmentInventorySlot.WEAPON.getSlotIdx());
			if (weapon != null && isPickaxe(weapon.getId()))
			{
				pickaxe = true;
			}
		}

		boolean lanternEquipped = lanternId != -1 && equipmentHasLantern(equipment);

		return new InventorySnapshot(
			dense,
			dark,
			fragments,
			empty,
			chisel,
			pickaxe,
			inactiveEssence,
			activeEssence,
			lanternEquipped,
			lanternInv,
			lanternId);
	}

	private int resolveFragmentCount(boolean hasFragmentItem, int containerQty, int widgetQty, int dark, int empty)
	{
		if (!hasFragmentItem)
		{
			trackedFragments = 0;
			lastDarkBlocks = dark;
			return 0;
		}

		int visible = Math.max(containerQty, widgetQty);
		if (visible > 1)
		{
			trackedFragments = Math.min(MAX_FRAGMENTS, visible);
		}
		else if (lastDarkBlocks >= 0 && dark < lastDarkBlocks)
		{
			trackedFragments = Math.min(MAX_FRAGMENTS, trackedFragments + FRAGMENTS_PER_BLOCK * (lastDarkBlocks - dark));
		}
		else if (trackedFragments <= 1 && empty == 0 && dark > 0)
		{
			// Second inventory: fragment stack + full bag of dark. Quantity is hidden as 1.
			trackedFragments = TYPICAL_FULL_STACK;
		}
		else if (trackedFragments <= 0)
		{
			trackedFragments = Math.max(1, visible);
		}

		lastDarkBlocks = dark;
		return trackedFragments;
	}

	private int fragmentQuantityFromWidget()
	{
		Widget container = client.getWidget(InterfaceID.Inventory.ITEMS);
		if (container == null)
		{
			return 0;
		}
		Widget[] children = container.getDynamicChildren();
		if (children == null)
		{
			return 0;
		}
		int total = 0;
		for (int i = 0; i < children.length; i++)
		{
			Widget child = children[i];
			if (child != null && child.getItemId() == ItemID.BIGBLANKRUNE)
			{
				total += Math.max(0, child.getItemQuantity());
			}
		}
		return total;
	}

	private boolean equipmentHasLantern(ItemContainer equipment)
	{
		if (equipment == null)
		{
			return false;
		}
		Item shield = equipment.getItem(EquipmentInventorySlot.SHIELD.getSlotIdx());
		return shield != null && isAbyssalLantern(shield.getId());
	}

	static boolean isAbyssalLantern(int id)
	{
		return id == ItemID.ABYSSAL_LANTERN
			|| id == ItemID.ABYSSAL_LANTERN_NORMAL
			|| id == ItemID.ABYSSAL_LANTERN_NORMAL_BLUE
			|| id == ItemID.ABYSSAL_LANTERN_NORMAL_RED
			|| id == ItemID.ABYSSAL_LANTERN_NORMAL_WHITE
			|| id == ItemID.ABYSSAL_LANTERN_NORMAL_PURPLE
			|| id == ItemID.ABYSSAL_LANTERN_NORMAL_GREEN
			|| id == ItemID.ABYSSAL_LANTERN_OAK
			|| id == ItemID.ABYSSAL_LANTERN_WILLOW
			|| id == ItemID.ABYSSAL_LANTERN_MAPLE
			|| id == ItemID.ABYSSAL_LANTERN_YEW
			|| id == ItemID.ABYSSAL_LANTERN_BLISTERWOOD
			|| id == ItemID.ABYSSAL_LANTERN_MAGIC
			|| id == ItemID.ABYSSAL_LANTERN_REDWOOD;
	}

	private boolean isPickaxe(int id)
	{
		ItemComposition def = client.getItemDefinition(id);
		if (def == null || def.getName() == null)
		{
			return false;
		}
		String name = def.getName().toLowerCase();
		return name.contains("pickaxe") || name.contains("pick axe");
	}
}
