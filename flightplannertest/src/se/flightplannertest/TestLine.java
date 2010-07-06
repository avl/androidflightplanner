package se.flightplannertest;


import junit.framework.Assert;
import org.junit.Test;
import se.flightplanner.vector.Line;
import se.flightplanner.vector.Vector;

public class TestLine {

	@Test
	public void testApprox_intersect_horiz_line1a() {
		Line l1=new Line(new Vector(0,0),new Vector(2,2));
		Vector v=l1.approx_intersect_horiz_line(1.0, 0);
		Assert.assertTrue(v.almostEquals(new Vector(1,1),1e-8));
	}
	@Test
	public void testApprox_intersect_horiz_line1b() {
		Line l1=new Line(new Vector(2,2),new Vector(0,0));
		Vector v=l1.approx_intersect_horiz_line(1.0, 0);
		Assert.assertTrue(v.almostEquals(new Vector(1,1),1e-8));
	}

	@Test
	public void testApprox_intersect_horiz_line2() {
		Line l1=new Line(new Vector(0,0),new Vector(5,0));
		Vector v=l1.approx_intersect_horiz_line(0.0, -5);
		//System.out.println(v);
		Assert.assertTrue(v.almostEquals(new Vector(0,0),1e-8));
	}
	@Test
	public void testApprox_intersect_horiz_line3() {
		Line l1=new Line(new Vector(0,0),new Vector(5,0));
		Vector v=l1.approx_intersect_horiz_line(0.0, 2.5);
		Assert.assertTrue(v.almostEquals(new Vector(2.5,0),1e-8));
	}
	@Test
	public void testApprox_intersect_horiz_line4() {
		Line l1=new Line(new Vector(0,0),new Vector(5,0));
		Vector v=l1.approx_intersect_horiz_line(0.0, 7);
		Assert.assertTrue(v.almostEquals(new Vector(5,0),1e-8));
	}
	
	

	@Test
	public void testSide() {
		Line l1=new Line(new Vector(0,0),new Vector(5,0));
		Assert.assertEquals(-1,l1.side(new Vector(2.5,1)));
		Assert.assertEquals(+1,l1.side(new Vector(2.5,-1)));
	}

	@Test
	public void testClosest() {
		Line l1=new Line(new Vector(0,0),new Vector(5,0));
		Assert.assertTrue(l1.closest(new Vector(-1,1)).almostEquals(new Vector(0,0), 1e-10));
		Assert.assertTrue(l1.closest(new Vector( 6,1)).almostEquals(new Vector(5,0), 1e-10));
		//System.out.println("out:"+l1.closest(new Vector(1,1)));
		Assert.assertTrue(l1.closest(new Vector( 1,1)).almostEquals(new Vector(1,0), 1e-10));
		Assert.assertTrue(l1.closest(new Vector( 4,1)).almostEquals(new Vector(4,0), 1e-10));
		Assert.assertTrue(l1.closest(new Vector( 0,1)).almostEquals(new Vector(0,0), 1e-10));
		
		Assert.assertTrue(l1.closest(new Vector( 5,1)).almostEquals(new Vector(5,0), 1e-10));

	}
	@Test
	public void testBend()
	{
		Assert.assertTrue(Line.bend_dir(new Vector(0,0),new Vector(1,0),new Vector(1,1))==-1);
		Assert.assertTrue(Line.bend_dir(new Vector(0,0),new Vector(1,0),new Vector(1,-1))==1);
		Assert.assertTrue(Line.bend_dir(new Vector(0,0),new Vector(1,0),new Vector(100,-0.01))==1);
		Assert.assertTrue(Line.bend_dir(new Vector(0,0),new Vector(1,0),new Vector(100,+0.01))==-1);
	}
	
	@Test
	public void testIntersect1()
	{
		Line l1=new Line(new Vector(0,0),new Vector(5,0));
		Line l2=new Line(new Vector(2,-2),new Vector(2,2));
		Vector p1=Line.intersect(l1, l2);
		Vector p2=Line.intersect(l2, l1);
		Assert.assertTrue(p1.almostEquals(new Vector(2,0), 1e-9));		
		Assert.assertTrue(p2.almostEquals(new Vector(2,0), 1e-9));		
	}
	@Test
	public void testIntersect2()
	{
		Line l1=new Line(new Vector(0,0),new Vector(5,0));
		Line l2=new Line(new Vector(-2,-2),new Vector(-2,2));
		Vector p1=Line.intersect(l1, l2);
		Vector p2=Line.intersect(l2, l1);
		Assert.assertNull(p1);		
		Assert.assertNull(p2);		
	}
	@Test
	public void testIntersect3()
	{
		Line l1=new Line(new Vector(0,5),new Vector(5,0));
		Line l2=new Line(new Vector(0,0),new Vector(5,5));
		Vector p1=Line.intersect(l1, l2);
		Vector p2=Line.intersect(l2, l1);
		Assert.assertTrue(p1.almostEquals(new Vector(2.5,2.5), 1e-9));		
		Assert.assertTrue(p2.almostEquals(new Vector(2.5,2.5), 1e-9));		
	}
}
