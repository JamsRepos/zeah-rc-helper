package com.zeahrchelper;

import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;

@Singleton
public class ChangelogService
{
	private final Client client;
	private final ZeahRcHelperConfig config;
	private final ConfigManager configManager;
	private final ChatMessageManager chatMessageManager;
	private boolean announced;

	@Inject
	ChangelogService(
		Client client,
		ZeahRcHelperConfig config,
		ConfigManager configManager,
		ChatMessageManager chatMessageManager)
	{
		this.client = client;
		this.config = config;
		this.configManager = configManager;
		this.chatMessageManager = chatMessageManager;
	}

	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			maybeAnnounce();
		}
	}

	public void maybeAnnounce()
	{
		if (announced || client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}
		List<Changelog.Release> unseen = Changelog.unseenSince(config.seenChangelogVersion());
		if (unseen.isEmpty())
		{
			announced = true;
			return;
		}
		announced = true;
		for (Changelog.Release release : unseen)
		{
			chat("Zeah RC Helper " + release.version + " — what's new:");
			for (String note : release.notes)
			{
				chat("• " + note);
			}
		}
		configManager.setConfiguration(
			ZeahRcHelperConfig.GROUP,
			ZeahRcHelperConfig.SEEN_CHANGELOG_VERSION_KEY,
			Changelog.VERSION);
	}

	public void reset()
	{
		announced = false;
	}

	private void chat(String message)
	{
		String formatted = new ChatMessageBuilder()
			.append(ChatColorType.HIGHLIGHT)
			.append("[Zeah RC] " + message)
			.build();
		chatMessageManager.queue(QueuedMessage.builder()
			.type(ChatMessageType.CONSOLE)
			.runeLiteFormattedMessage(formatted)
			.build());
	}
}
