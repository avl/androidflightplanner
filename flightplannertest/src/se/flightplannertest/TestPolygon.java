package se.flightplannertest;

import java.util.ArrayList;

import junit.framework.Assert;

import org.junit.Test;

import se.flightplanner.vector.Polygon;
import se.flightplanner.vector.Vector;

public class TestPolygon {

	
	@Test
	public void testCalc_area() 
	{
		ArrayList<Vector> vecs=new ArrayList<Vector>();
		vecs.add(new Vector(0,0));
		vecs.add(new Vector(1,0));
		vecs.add(new Vector(1,1));
		vecs.add(new Vector(0,1));
		Polygon p=new Polygon(vecs);
		Assert.assertEquals(1.0,p.calc_area(),1e-9);
	}

	@Test
	public void testInside() {
		ArrayList<Vector> vecs=new ArrayList<Vector>();
		vecs.add(new Vector(0,0));
		vecs.add(new Vector(1,0));
		vecs.add(new Vector(1,1));
		vecs.add(new Vector(0,1));
		Polygon p=new Polygon(vecs);
		Assert.assertTrue(p.is_inside(new Vector(0.5,0.5)));
		Assert.assertTrue(p.is_inside(new Vector(0.9,0.9)));
		Assert.assertTrue(p.is_inside(new Vector(0.1,0.9)));
		Assert.assertFalse(p.is_inside(new Vector(1.1,0.5)));
		Assert.assertFalse(p.is_inside(new Vector(1.1,1.0)));
		Assert.assertFalse(p.is_inside(new Vector(1.1,1.1)));
		Assert.assertFalse(p.is_inside(new Vector(-0.1,0.5)));
		Assert.assertFalse(p.is_inside(new Vector(-0.1,1.0)));
		Assert.assertFalse(p.is_inside(new Vector(-0.1,1.1)));
		Assert.assertFalse(p.is_inside(new Vector(0.5,1.1)));
		Assert.assertFalse(p.is_inside(new Vector(0.5,-0.1)));
	}
	@Test
	public void testInside2() {
		ArrayList<Vector> vecs=new ArrayList<Vector>();
		vecs.add(new Vector(0,0));
		vecs.add(new Vector(10,0));
		vecs.add(new Vector(10,10));
		vecs.add(new Vector(5,6));
		vecs.add(new Vector(0,10));
		Polygon p=new Polygon(vecs);
		Assert.assertTrue(p.is_inside(new Vector(5,5.9)));
		Assert.assertTrue(p.is_inside(new Vector(6,5.9)));
		Assert.assertTrue(p.is_inside(new Vector(4,5.9)));
		Assert.assertTrue(p.is_inside(new Vector(6,6.1)));
		Assert.assertTrue(p.is_inside(new Vector(5,5.9)));
		Assert.assertTrue(p.is_inside(new Vector(6,5.9)));
		Assert.assertTrue(p.is_inside(new Vector(4,5.9)));
		Assert.assertTrue(!p.is_inside(new Vector(5,6.1)));
		Assert.assertTrue(p.closest(new Vector(-0.1,-0.1)).almostEquals(new Vector(0,0), 1e-9));
		Assert.assertTrue(p.closest(new Vector(10.1,10.1)).almostEquals(new Vector(10,10), 1e-9));
		Assert.assertTrue(p.closest(new Vector(5,5)).almostEquals(new Vector(5,6), 1e-9));
	}
}
