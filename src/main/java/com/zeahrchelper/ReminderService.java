package com.zeahrchelper;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.gameval.ItemID;

@Singleton
public class ReminderService
{
	private static final Pattern ESSENCE_CHARGES = Pattern.compile("Your blood essence has (\\d+) charges remaining", Pattern.CASE_INSENSITIVE);

	private final Client client;
	private final ZeahRcHelperConfig config;

	@Getter
	private final List<String> warnings = new ArrayList<>();

	@Getter
	private Integer bloodEssenceCharges;

	@Getter
	private boolean idle;

	private Instant lastMoveAt = Instant.now();
	private WorldPoint lastTile;

	@Inject
	ReminderService(Client client, ZeahRcHelperConfig config)
	{
		this.client = client;
		this.config = config;
	}

	public void reset()
	{
		warnings.clear();
		bloodEssenceCharges = null;
		idle = false;
		lastMoveAt = Instant.now();
		lastTile = null;
	}

	public void onChatMessage(ChatMessage event)
	{
		if (event.getMessage() == null)
		{
			return;
		}
		String message = event.getMessage().replaceAll("<[^>]+>", " ");
		Matcher matcher = ESSENCE_CHARGES.matcher(message);
		if (matcher.find())
		{
			bloodEssenceCharges = Integer.parseInt(matcher.group(1));
		}
	}

	public void update(InventorySnapshot inv, RcMode mode, boolean inArea)
	{
		warnings.clear();
		idle = false;

		if (!inArea || inv == null)
		{
			return;
		}

		updateIdle();

		if (!inv.isHasChisel())
		{
			warnings.add("Bring a chisel");
		}
		if (!inv.isHasPickaxe())
		{
			warnings.add("Bring a pickaxe (worn or in inventory)");
		}

		if (config.lanternReminder())
		{
			addLanternWarnings(inv, mode);
		}

		if (config.bloodEssenceReminder() && mode == RcMode.BLOOD)
		{
			addEssenceWarnings(inv);
		}

		if (idle)
		{
			warnings.add("Idle — " + (lastStepHint()));
		}
	}

	private String lastStepHint()
	{
		return "click the highlighted tile to continue";
	}

	private void addLanternWarnings(InventorySnapshot inv, RcMode mode)
	{
		if (!inv.isLanternEquipped())
		{
			if (inv.isLanternInInventory())
			{
				warnings.add("Equip your abyssal lantern (shield slot)");
			}
			else
			{
				warnings.add("Bring and equip your abyssal lantern");
			}
			return;
		}

		if (!config.lanternLogCheck())
		{
			return;
		}

		int id = inv.getLanternItemId();
		if (id == ItemID.ABYSSAL_LANTERN)
		{
			warnings.add(mode == RcMode.BLOOD
				? "Light the lantern with blisterwood logs (20% more bloods), or magic/redwood"
				: "Light the lantern with magic logs (10% more runes) or redwood");
			return;
		}

		boolean useful = isUsefulLantern(id, mode);
		if (!useful)
		{
			if (mode == RcMode.BLOOD)
			{
				warnings.add("Lantern logs don't help bloods — use blisterwood (20%), magic (10%), or redwood");
			}
			else
			{
				warnings.add("Lantern logs don't help souls — use magic (10%) or redwood (willow 5% also works)");
			}
		}
	}

	/**
	 * Logs that actually boost Zeah RC after the Aug 2026 lantern change:
	 * willow +5% runes, blisterwood +20% bloods, magic +10% runes, redwood = oak+willow.
	 */
	static boolean isUsefulLantern(int id, RcMode mode)
	{
		if (id == ItemID.ABYSSAL_LANTERN_MAGIC || id == ItemID.ABYSSAL_LANTERN_REDWOOD
			|| id == ItemID.ABYSSAL_LANTERN_WILLOW)
		{
			return true;
		}
		return mode == RcMode.BLOOD && id == ItemID.ABYSSAL_LANTERN_BLISTERWOOD;
	}

	private void addEssenceWarnings(InventorySnapshot inv)
	{
		if (inv.isHasActiveBloodEssence())
		{
			if (bloodEssenceCharges != null && bloodEssenceCharges <= config.bloodEssenceLowCharges())
			{
				warnings.add("Blood essence low — " + bloodEssenceCharges + " charges left");
			}
			return;
		}

		if (inv.isHasInactiveBloodEssence())
		{
			warnings.add("Activate your blood essence (+50% blood runes)");
			return;
		}

		warnings.add("Bring activated blood essence for +50% blood runes");
	}

	private void updateIdle()
	{
		if (client.getLocalPlayer() == null)
		{
			return;
		}
		WorldPoint now = client.getLocalPlayer().getWorldLocation();
		if (lastTile == null || now == null || lastTile.distanceTo(now) > 0)
		{
			lastTile = now;
			lastMoveAt = Instant.now();
			idle = false;
			return;
		}
		idle = Duration.between(lastMoveAt, Instant.now()).getSeconds() >= config.idleReminderSeconds();
	}
}
