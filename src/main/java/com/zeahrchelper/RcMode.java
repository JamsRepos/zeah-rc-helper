package com.zeahrchelper;

import java.awt.Color;
import lombok.Getter;

@Getter
public enum RcMode
{
	BLOOD("Blood", new Color(196, 42, 50)),
	SOUL("Soul", new Color(58, 186, 198)),
	AUTO("Auto", Color.WHITE);

	private final String label;
	private final Color color;

	RcMode(String label, Color color)
	{
		this.label = label;
		this.color = color;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
