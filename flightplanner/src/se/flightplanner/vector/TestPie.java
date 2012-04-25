package se.flightplanner.vector;

import se.flightplanner.vector.Polygon.SectorResult;
import junit.framework.TestCase;

public class TestPie extends TestCase {

	public void testPie1()
	{
		Pie p=new Pie(0,45);
		assertTrue(p.check_intersect(new Pie(0,1)));
		assertTrue(p.check_intersect(new Pie(44,45)));
		assertTrue(p.check_intersect(new Pie(-100,1)));
		assertTrue(p.check_intersect(new Pie(-1,46)));
		assertTrue(p.check_intersect(new Pie(44,100)));
		assertTrue(p.check_intersect(new Pie(44,370)));
		assertTrue(p.check_intersect(new Pie(48,44)));
		assertTrue(p.check_intersect(new Pie(44,1)));
		assertFalse(p.check_intersect(new Pie(46,100)));
		assertFalse(p.check_intersect(new Pie(350,359)));
	}
	public void testPie2()
	{
		Pie p=new Pie(0,0);
		assertTrue(p.check_intersect(new Pie(-1,1)));
		assertTrue(p.check_intersect(new Pie(355,350)));
	}
	public void testPie3()
	{
		Pie p=new Pie(350,365);
		assertTrue(p.check_intersect(new Pie(-1,1)));
		assertTrue(p.check_intersect(new Pie(200,351)));
		assertTrue(p.check_intersect(new Pie(4,6)));
		assertFalse(p.check_intersect(new Pie(6,349)));		
		assertTrue(p.check_intersect(new Pie(6,351)));
		assertEquals(15.0,new Pie(350,365).size());		
	}
	
	public void testBoundPie1()
	{
		BoundPie bp=new BoundPie(new Vector(0,0),new Pie(90,135));
		Line a;
		a=new Line(0,10,20,10);assertTrue(new Line(10,10,20,10).almostEquals(bp.cut(a),1e-6));
		
		
		a=new Line(10,5,10,8);assertEquals(a,bp.cut(a));
		a=new Line(10,0,10,20);assertTrue(new Line(10,0,10,10).almostEquals(bp.cut(a),1e-6));

		a=new Line(10,5,10,20);assertTrue(new Line(10,5,10,10).almostEquals(bp.cut(a),1e-6));

		a=new Line(10,-5,10,20);assertTrue(new Line(10,0,10,10).almostEquals(bp.cut(a),1e-6));

		assertNull(bp.cut(new Line(10,20,10,30)));
		assertNull(bp.cut(new Line(-10,5,-10,10)));
		
		
	}
	public void testBoundPie2()
	{
		BoundPie bp=new BoundPie(new Vector(10,0),new Pie(90,135));
		Line a=new Line(20,0,20,20);
		assertTrue(new Line(20,0,20,10).almostEquals(bp.cut(a),1e-6));
	}
	private void roundtrip(Vector v)
	{
		double a=v.hdg();
		assertTrue(v.almostEquals(Vector.fromhdg(a),0.1));
	}
	public void testVec1()
	{
		roundtrip(new Vector(0,1));
		roundtrip(new Vector(1,0));
		roundtrip(new Vector(-1,0));
		roundtrip(new Vector(0,-1));
		roundtrip(new Vector(0.707,-0.707));
		roundtrip(new Vector(-0.707,-0.707));
		roundtrip(new Vector(-0.707,0.707));
		roundtrip(new Vector(0.707,0.707));
	}
	public void testBoundPie3()
	{
		BoundPie bp=new BoundPie(new Vector(0,0),new Pie(225,270));
		Line a=new Line(-10,0,-10,20);
		Line r=bp.cut(a);
		assertTrue(new Line(-10,0,-10,10).almostEquals(r,1e-6));
	}
	public void testBoundPie4()
	{
		BoundPie bp=new BoundPie(new Vector(0,0),new Pie(0,270));
		Line a=new Line(10,-10,-100,-10);
		Line r=bp.cut(a);
		assertTrue(new Line(10,-10,0,-10).almostEquals(r,1e-6));
		assertNull(bp.cut(new Line(-10,-10,-5,-5)));
	}
	public void testBoundPie5()
	{
		BoundPie bp=new BoundPie(new Vector(0,0),new Pie(0,90));
		assertNull(bp.cut(new Line(-10,-2,2,10)));
	}
	public void testBoundPie6()
	{
		assertTrue(new Pie(300,330).isInPie(new Vector(-11,-11)));		
		BoundPie bp=new BoundPie(new Vector(11,11),new Pie(300,330));
		Line a=new Line(0,0,-10,0);
		Line r=bp.cut(a);
		assertNotNull(r);
	}
	public void testPoly1()
	{
		Polygon p=new Polygon(new Vector[]{
				new Vector(1,1),
				new Vector(10,1),
				new Vector(10,10),
				new Vector(1,10)});
		
		BoundPie pie=new BoundPie(new Vector(0,0),new Pie(130,140));
		SectorResult sr=p.sector(pie);
		assertEquals(Math.sqrt(2),sr.nearest_distance_to_center,1e-6);
		assertFalse(sr.inside);		
	}
	public void testPoly2()
	{
		Polygon p=new Polygon(new Vector[]{
				new Vector(0,0),
				new Vector(10,0),
				new Vector(10,10),
				new Vector(0,10)});
		
		BoundPie pie=new BoundPie(new Vector(5,11),new Pie(330,30));
		SectorResult sr=p.sector(pie);
		assertEquals(1,sr.nearest_distance_to_center,1e-6);
		assertFalse(sr.inside);		
	}
	public void testPoly3()
	{
		Polygon p=new Polygon(new Vector[]{
				new Vector(0,0),
				new Vector(10,0),
				new Vector(10,10),
				new Vector(0,10)});
		
		BoundPie pie=new BoundPie(new Vector(11,11),new Pie(90,180));
		SectorResult sr=p.sector(pie);
		assertTrue(sr.nearest_distance_to_center>1e10);
	}
	public void testPoly4()
	{
		Polygon p=new Polygon(new Vector[]{
				new Vector(0,0),
				new Vector(10,0),
				new Vector(10,10),
				new Vector(0,10)});
		
		BoundPie pie=new BoundPie(new Vector(11,11),new Pie(300,330));
		SectorResult sr=p.sector(pie);
		assertEquals(Math.sqrt(2),sr.nearest_distance_to_center,1e-7);
	}

	public void testPoly5()
	{
		Polygon p=new Polygon(new Vector[]{
				new Vector(0,0),
				new Vector(10,0),
				new Vector(10,10),
				new Vector(0,10)});
		
		BoundPie pie=new BoundPie(new Vector(-1,5),new Pie(80,100));
		SectorResult sr=p.sector(pie);
		assertEquals(80.0,sr.pie.getA());
		assertEquals(100.0,sr.pie.getB());
		assertEquals(1.0,sr.nearest_distance_to_center);
	}
	public void testPoly6()
	{
		Polygon p=new Polygon(new Vector[]{
				new Vector(0,0),
				new Vector(10,0),
				new Vector(10,10),
				new Vector(0,10)});
		
		BoundPie pie=new BoundPie(new Vector(-100,5),new Pie(80,100));
		SectorResult sr=p.sector(pie);
		assertTrue(80.0<sr.pie.getA());
		assertTrue(100.0>sr.pie.getB());
		assertEquals(100.0,sr.nearest_distance_to_center);
	}
	public void testPoly7()
	{
		Polygon p=new Polygon(new Vector[]{
				new Vector(0,0),
				new Vector(10,0),
				new Vector(10,10),
				new Vector(0,10)});
		
		BoundPie pie=new BoundPie(new Vector(5,5),new Pie(80,100));
		SectorResult sr=p.sector(pie);
		assertTrue(sr.inside);
	}
	public void testPoly8()
	{
		Polygon p=new Polygon(new Vector[]{
				new Vector(0,0),
				new Vector(10,0),
				new Vector(1,1),
				new Vector(0,10)});
		
		BoundPie pie=new BoundPie(new Vector(5,5),new Pie(180,90));
		SectorResult sr=p.sector(pie);
		assertFalse(sr.inside);
		assertEquals(405.0,sr.pie.getB());
		assertEquals(225.0,sr.pie.getA());
		
	}
	public void testPoly9()
	{
		Polygon p=new Polygon(new Vector[]{
				new Vector(0,0),
				new Vector(10,0),
				new Vector(1,1),
				new Vector(0,10)});
		
		BoundPie pie=new BoundPie(new Vector(5,5),new Pie(90,360));
		SectorResult sr=p.sector(pie);
		assertFalse(sr.inside);
		assertEquals(360.0,sr.pie.getB());
		assertEquals(225.0,sr.pie.getA());
		
	}
		
	
	
	
}
