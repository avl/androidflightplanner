package se.flightplannertest;

import junit.framework.Assert;

import org.junit.Test;

import se.flightplanner.vector.Vector;

public class TestVector {

	@Test
	public void testGetx() {
		Assert.assertEquals(new Vector(42,2).getx(), 42.0);
	}

	@Test
	public void testGety() {
		Assert.assertEquals(new Vector(43,3).getx(), 43.0);
		
	}

	@Test
	public void testEquals()
	{
		Assert.assertTrue(new Vector(1,0).equals(new Vector(1,0)));
		Assert.assertTrue(new Vector(1,0).almostEquals(new Vector(1.000000001,0),1e-4));
	}
	@Test
	public void testLength() {
		Assert.assertEquals(4.0,new Vector(4,0).length(), 4.0);
		Assert.assertEquals(3.0,new Vector(0,3).length(), 3.0);
		Assert.assertEquals(5.0,new Vector(4,3).length(), 1e-5);
	}

	@Test
	public void testNormalized() {
		Assert.assertEquals(1.0,new Vector(8,0).normalized().getx());
		Assert.assertEquals(1.0,new Vector(0,1).normalized().gety());
	}

	@Test
	public void testRot90l() {
		Assert.assertEquals(1.0,new Vector(1,0).rot90l().gety(),1e-10);
		Assert.assertEquals(0.0,new Vector(1,0).rot90l().getx(),1e-10);
		Assert.assertEquals(-1.0,new Vector(0,1).rot90l().getx(),1e-10);
		Assert.assertEquals(0.0,new Vector(0,1).rot90l().gety(),1e-10);

	}

	@Test
	public void testRot90r() {
		Assert.assertEquals(-1.0,new Vector(1,0).rot90r().gety(),1e-10);
		Assert.assertEquals(0.0,new Vector(1,0).rot90r().getx(),1e-10);
		Assert.assertEquals(1.0,new Vector(0,1).rot90r().getx(),1e-10);
		Assert.assertEquals(0.0,new Vector(0,1).rot90r().gety(),1e-10);
	}

	@Test
	public void testPlus() {
		Assert.assertTrue(
			new Vector(3,4).plus(new Vector(2,3)).almostEquals(new Vector(5,7),1e-10));
	}

	@Test
	public void testMinus() {
		Assert.assertTrue(
				new Vector(3,4).minus(new Vector(2,3)).almostEquals(new Vector(1,1),1e-10));
	}

	@Test
	public void testMul() {
		Assert.assertTrue(
				new Vector(3,4).mul(2).almostEquals(new Vector(6,8),1e-10));
	}

	@Test
	public void testScalarprod() {
		Assert.assertEquals(1.0*3.0+2.0*4.0,new Vector(1,2).scalarprod(new Vector(3,4)));
	}

}
