package com.zeahrchelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Player-facing notes per plugin version. {@link #VERSION} must match
 * {@code version} in {@code build.gradle} and {@code runelite-plugin.properties}.
 * Append a {@link Release} on each bump so skipped Hub versions still get their notes.
 */
final class Changelog
{
	static final String VERSION = "1.0.3";

	static final List<Release> RELEASES = List.of(
		new Release("1.0.2",
			"Status panel is one compact overlay with labeled Dense, Dark, Fragments, Trips, and Essence.",
			"Gear reminders now appear in that panel instead of a second overlay.",
			"Bloods and Souls use their rune colours on the method name and altar path."
		),
		new Release("1.0.3",
			"Renamed to Jam's Arceuus Runecrafting in the plugin panel and Hub."
		)
	);

	private Changelog()
	{
	}

	static boolean isUnseen(String seenVersion)
	{
		return !unseenSince(seenVersion).isEmpty();
	}

	static List<Release> unseenSince(String seenVersion)
	{
		String seen = seenVersion == null ? "" : seenVersion;
		List<Release> unseen = new ArrayList<>();
		for (Release release : RELEASES)
		{
			if (compareVersions(release.version, seen) > 0)
			{
				unseen.add(release);
			}
		}
		return Collections.unmodifiableList(unseen);
	}

	static int compareVersions(String left, String right)
	{
		int[] a = parseVersion(left);
		int[] b = parseVersion(right);
		int n = Math.max(a.length, b.length);
		for (int i = 0; i < n; i++)
		{
			int av = i < a.length ? a[i] : 0;
			int bv = i < b.length ? b[i] : 0;
			if (av != bv)
			{
				return Integer.compare(av, bv);
			}
		}
		return 0;
	}

	private static int[] parseVersion(String version)
	{
		if (version == null || version.isEmpty())
		{
			return new int[0];
		}
		String[] parts = version.split("\\.");
		int[] values = new int[parts.length];
		for (int i = 0; i < parts.length; i++)
		{
			try
			{
				values[i] = Integer.parseInt(parts[i]);
			}
			catch (NumberFormatException ex)
			{
				values[i] = 0;
			}
		}
		return values;
	}

	static final class Release
	{
		final String version;
		final List<String> notes;

		Release(String version, String... notes)
		{
			this.version = version;
			this.notes = List.of(notes);
		}
	}
}
