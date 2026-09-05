package com.zeahrchelper;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PathDisplayMigrationTest
{
	@Test
	public void oldCheckboxPairsMapToTheDropdown()
	{
		assertEquals(PathDisplay.FLOOR_AND_MINIMAP, PathDisplayMigration.displayFor(true, true));
		assertEquals(PathDisplay.FLOOR, PathDisplayMigration.displayFor(true, false));
		assertEquals(PathDisplay.MINIMAP, PathDisplayMigration.displayFor(false, true));
		assertEquals(PathDisplay.OFF, PathDisplayMigration.displayFor(false, false));
	}

	@Test
	public void displayKnowsWhichSurfacesItDraws()
	{
		assertTrue(PathDisplay.FLOOR_AND_MINIMAP.showsFloor());
		assertTrue(PathDisplay.FLOOR_AND_MINIMAP.showsMinimap());
		assertTrue(PathDisplay.FLOOR.showsFloor());
		assertFalse(PathDisplay.FLOOR.showsMinimap());
		assertFalse(PathDisplay.MINIMAP.showsFloor());
		assertTrue(PathDisplay.MINIMAP.showsMinimap());
		assertFalse(PathDisplay.OFF.showsFloor());
		assertFalse(PathDisplay.OFF.showsMinimap());
		assertTrue(PathDisplay.OFF.isOff());
	}
}
