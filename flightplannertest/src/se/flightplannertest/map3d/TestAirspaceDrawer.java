package se.flightplannertest.map3d;

import junit.framework.Assert;

import org.junit.Test;

import se.flightplanner.Project;
import se.flightplanner.Project.iMerc;
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
	@Test
	public void testProjectFtPixels()
	{
		double one_naut_in_pixels=Project.approx_scale(620000, 13, 1.0);
		double one_feet_in_pixels1=(one_naut_in_pixels/1852.0)*0.3048;
		double one_feet_in_pixels2=Project.approx_ft_pixels(new iMerc(0,620000),13);
		Assert.assertEquals(one_feet_in_pixels1, one_feet_in_pixels2, 1e-6);
		//System.out.println("Way 1: "+one_feet_in_pixels1+" Way 2: "+one_feet_in_pixels2);
	}
}
