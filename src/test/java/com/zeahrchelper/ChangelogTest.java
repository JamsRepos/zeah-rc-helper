package com.zeahrchelper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ChangelogTest
{
	@Test
	public void changelogIsOncePerVersion() throws IOException
	{
		assertFalse(Changelog.RELEASES.isEmpty());
		Changelog.Release latest = Changelog.RELEASES.get(Changelog.RELEASES.size() - 1);
		assertEquals(Changelog.VERSION, latest.version);
		assertFalse(latest.notes.isEmpty());
		for (int i = 1; i < Changelog.RELEASES.size(); i++)
		{
			assertTrue(Changelog.compareVersions(
				Changelog.RELEASES.get(i).version,
				Changelog.RELEASES.get(i - 1).version) > 0);
		}
		assertEquals(Changelog.RELEASES.size(), Changelog.unseenSince("").size());
		if (Changelog.RELEASES.size() > 1)
		{
			List<Changelog.Release> afterFirst =
				Changelog.unseenSince(Changelog.RELEASES.get(0).version);
			assertEquals(Changelog.RELEASES.size() - 1, afterFirst.size());
			assertEquals(Changelog.VERSION, afterFirst.get(afterFirst.size() - 1).version);
		}
		assertTrue(Changelog.unseenSince(Changelog.VERSION).isEmpty());
		assertTrue(Changelog.isUnseen(""));
		assertTrue(Changelog.isUnseen("1.0.0"));
		assertFalse(Changelog.isUnseen(Changelog.VERSION));
		assertTrue(Changelog.compareVersions("1.10.0", "1.9.0") > 0);
		assertEquals(Changelog.VERSION, pluginPropertyVersion());
		assertEquals(Changelog.VERSION, gradleVersion());
	}

	private static String pluginPropertyVersion() throws IOException
	{
		Properties properties = new Properties();
		properties.load(Files.newBufferedReader(Path.of("runelite-plugin.properties"), StandardCharsets.UTF_8));
		return properties.getProperty("version");
	}

	private static String gradleVersion() throws IOException
	{
		for (String line : Files.readAllLines(Path.of("build.gradle"), StandardCharsets.UTF_8))
		{
			String trimmed = line.trim();
			if (trimmed.startsWith("version = '") && trimmed.contains("Changelog.VERSION"))
			{
				int start = trimmed.indexOf('\'') + 1;
				int end = trimmed.indexOf('\'', start);
				return trimmed.substring(start, end);
			}
		}
		throw new AssertionError("build.gradle is missing version = '...' // Keep Changelog.VERSION");
	}
}
