package se.flightplannertest.map3d;

import junit.framework.Assert;

import org.junit.Test;

import se.flightplanner.map3d.AltParser;

public class TestAirspaceDrawer {

	@Test
	public void testAltParser()
	{
		AltParser altp=new AltParser();
		Assert.assertEquals(4500, altp.parseAlt("4500"));
		Assert.assertEquals(4500, altp.parseAlt("4500ft"));
		Assert.assertEquals(4500, altp.parseAlt("4500 ft"));
		Assert.assertEquals(4500, altp.parseAlt("4500 ft asdf"));
		Assert.assertEquals(4500, altp.parseAlt("fl45"));
		Assert.assertEquals(4500, altp.parseAlt("FL 045"));
		Assert.assertEquals(4500, altp.parseAlt("FL 045 asdf"));
		Assert.assertEquals(4500, altp.parseAlt("gtrsdf msl 4500 ft asdf"));
		Assert.assertEquals(4000, altp.parseAlt("gtrsdf msl fl 40 ft asdf"));
	}
}
