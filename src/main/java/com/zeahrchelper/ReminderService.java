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
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.util.Text;

@Singleton
public class ReminderService
{
	private static final Pattern ESSENCE_CHARGES = Pattern.compile(
		"Your blood essence has (\\d{1,4}) charges? remaining",
		Pattern.CASE_INSENSITIVE);
	private static final Pattern ESSENCE_EXTRACT = Pattern.compile(
		"You manage to extract power from the Blood Essence and craft (\\d{1,3}) extra runes?",
		Pattern.CASE_INSENSITIVE);
	private static final String ESSENCE_ACTIVATE = "You activate the blood essence.";
	private static final int MAX_BLOOD_ESSENCE_CHARGES = 1000;
	/** Matches RuneLite Item Charges plugin RS-profile key. */
	private static final String ITEM_CHARGE_GROUP = "itemCharge";
	private static final String ITEM_CHARGE_BLOOD_ESSENCE = "bloodEssence";

	private final Client client;
	private final ZeahRcHelperConfig config;
	private final ConfigManager configManager;

	@Getter
	private final List<String> warnings = new ArrayList<>();

	@Getter
	private Integer bloodEssenceCharges;

	@Getter
	private boolean idle;

	private Instant lastMoveAt = Instant.now();
	private WorldPoint lastTile;

	@Inject
	ReminderService(Client client, ZeahRcHelperConfig config, ConfigManager configManager)
	{
		this.client = client;
		this.config = config;
		this.configManager = configManager;
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
		String message = Text.removeTags(event.getMessage());
		Matcher check = ESSENCE_CHARGES.matcher(message);
		if (check.find())
		{
			setBloodEssenceCharges(Integer.parseInt(check.group(1)));
			return;
		}
		Matcher extract = ESSENCE_EXTRACT.matcher(message);
		if (extract.find())
		{
			int used = Integer.parseInt(extract.group(1));
			int current = bloodEssenceCharges != null
				? bloodEssenceCharges
				: readItemChargeCharges();
			if (current >= 0)
			{
				setBloodEssenceCharges(Math.max(0, current - used));
			}
			return;
		}
		if (message.contains(ESSENCE_ACTIVATE))
		{
			setBloodEssenceCharges(MAX_BLOOD_ESSENCE_CHARGES);
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
		syncBloodEssenceCharges(inv);

		if (config.lanternReminder())
		{
			addGearWarnings(inv, mode);
		}

		if (config.bloodEssenceReminder() && mode == RcMode.BLOOD)
		{
			addEssenceWarnings(inv);
		}

	}

	private void addGearWarnings(InventorySnapshot inv, RcMode mode)
	{
		if (!inv.isHasChisel())
		{
			warnings.add("Bring a chisel");
		}
		if (!inv.isHasPickaxe())
		{
			warnings.add("Bring a pickaxe (worn or in inventory)");
		}
		addLanternWarnings(inv, mode);
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

	private void syncBloodEssenceCharges(InventorySnapshot inv)
	{
		if (!inv.isHasActiveBloodEssence())
		{
			if (!inv.isHasInactiveBloodEssence())
			{
				bloodEssenceCharges = null;
			}
			return;
		}
		if (bloodEssenceCharges != null)
		{
			return;
		}
		int stored = readItemChargeCharges();
		if (stored >= 0)
		{
			bloodEssenceCharges = stored;
		}
	}

	private void setBloodEssenceCharges(int charges)
	{
		bloodEssenceCharges = Math.max(0, Math.min(MAX_BLOOD_ESSENCE_CHARGES, charges));
	}

	private int readItemChargeCharges()
	{
		try
		{
			Integer stored = configManager.getRSProfileConfiguration(
				ITEM_CHARGE_GROUP,
				ITEM_CHARGE_BLOOD_ESSENCE,
				Integer.class);
			return stored != null ? stored : -1;
		}
		catch (Exception ex)
		{
			return -1;
		}
	}

	private void updateIdle()
	{
		if (client.getLocalPlayer() == null)
		{
			return;
		}

		// Standing still while mining or chiseling is not idle.
		if (isBusy())
		{
			lastTile = client.getLocalPlayer().getWorldLocation();
			lastMoveAt = Instant.now();
			idle = false;
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

	private boolean isBusy()
	{
		Player player = client.getLocalPlayer();
		if (player == null)
		{
			return false;
		}
		return player.getAnimation() != -1
			|| player.getPoseAnimation() != player.getIdlePoseAnimation();
	}
}
