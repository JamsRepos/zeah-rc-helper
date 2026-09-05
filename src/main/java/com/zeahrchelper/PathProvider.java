package com.zeahrchelper;

/**
 * Which plugin works out and draws the path to the next destination.
 */
public enum PathProvider
{
	PLUGIN("Plugin lines"),
	SHORTEST_PATH("Shortest Path");

	private final String label;

	PathProvider(String label)
	{
		this.label = label;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
