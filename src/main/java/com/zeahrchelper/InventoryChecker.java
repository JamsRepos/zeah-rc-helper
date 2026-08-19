package com.zeahrchelper;

import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;

@Singleton
public class InventoryChecker
{
	private static final int INVENTORY_SIZE = 28;

	private final Client client;

	@Inject
	InventoryChecker(Client client)
	{
		this.client = client;
	}

	public InventorySnapshot scan()
	{
		int dense = 0;
		int dark = 0;
		int fragments = 0;
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
				int qty = Math.max(1, item.getQuantity());
				if (id == ItemID.ARCEUUS_ESSENCE_BLOCK)
				{
					dense += qty;
				}
				else if (id == ItemID.ARCEUUS_ESSENCE_BLOCK_DARK)
				{
					dark += qty;
				}
				else if (id == ItemID.BIGBLANKRUNE)
				{
					fragments += qty;
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
