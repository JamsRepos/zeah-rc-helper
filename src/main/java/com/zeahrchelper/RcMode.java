package com.zeahrchelper;

public enum RcMode
{
	BLOOD("Blood"),
	SOUL("Soul"),
	AUTO("Auto");

	private final String label;

	RcMode(String label)
	{
		this.label = label;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
