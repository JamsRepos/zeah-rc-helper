package com.zeahrchelper;

/**
 * Where the path to the next destination is drawn. Click highlights are separate and stay on.
 */
public enum PathDisplay
{
	FLOOR_AND_MINIMAP("Floor & minimap"),
	FLOOR("Floor only"),
	MINIMAP("Minimap only"),
	OFF("Off");

	private final String label;

	PathDisplay(String label)
	{
		this.label = label;
	}

	public boolean showsFloor()
	{
		return this == FLOOR_AND_MINIMAP || this == FLOOR;
	}

	public boolean showsMinimap()
	{
		return this == FLOOR_AND_MINIMAP || this == MINIMAP;
	}

	public boolean isOff()
	{
		return this == OFF;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
