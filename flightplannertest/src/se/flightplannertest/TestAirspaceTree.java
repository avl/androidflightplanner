package se.flightplannertest;

import static org.junit.Assert.*;

import java.util.ArrayList;

import junit.framework.Assert;

import org.junit.Test;

import se.flightplanner.Airspace;
import se.flightplanner.AirspaceArea;
import se.flightplanner.AirspaceAreaTree;
import se.flightplanner.vector.BoundingBox;
import se.flightplanner.vector.Polygon;
import se.flightplanner.vector.Vector;

public class TestAirspaceTree {

	@Test
	public void testGetAreas1() {
		Airspace space = create_space(new Vector[]{
				new Vector(0,0),new Vector(10,0),new Vector(20,0)});
		AirspaceAreaTree tree=new AirspaceAreaTree(space.getSpaces());
		ArrayList<AirspaceArea> areas=tree.get_areas(new BoundingBox(-1,-1,1,1));
		Assert.assertEquals(0,tree.get_areas(new BoundingBox(-10,-10,-9,-9)).size());
		Assert.assertEquals(1,tree.get_areas(new BoundingBox(10,0.5,11,0.6)).size());
		Assert.assertEquals(2,tree.get_areas(new BoundingBox(10,0.5,21,0.6)).size());
		Assert.assertEquals(0,tree.get_areas(new BoundingBox(22,0.5,23,0.6)).size());
		Assert.assertEquals(1,areas.size());
		Assert.assertTrue(new Vector(0,0).almostEquals(areas.get(0).getPoly().get_points().get(0),1e-5));
	}
	@Test
	public void testGetAreas2() {
		Airspace space = create_space(new Vector[]{
				new Vector(0,0),new Vector(10,0),new Vector(20,0)});
		AirspaceAreaTree tree=new AirspaceAreaTree(space.getSpaces());
		ArrayList<AirspaceArea> areas=tree.get_areas(new BoundingBox(-1,-1,0.1,0.1));
		Assert.assertEquals(1,areas.size());
		Assert.assertTrue(new Vector(0,0).almostEquals(areas.get(0).getPoly().get_points().get(0),1e-5));
	}
	@Test
	public void testGetAreas3() {
		Airspace space = create_space(new Vector[]{
				new Vector(0,0),new Vector(10,0),new Vector(20,0)});
		AirspaceAreaTree tree=new AirspaceAreaTree(space.getSpaces());
		ArrayList<AirspaceArea> areas=tree.get_areas(new BoundingBox(-10,-10,40,20));
		Assert.assertEquals(3,areas.size());
		Assert.assertTrue(new Vector(10,0).almostEquals(areas.get(0).getPoly().get_points().get(0),1e-5));
		//System.out.println("Poly 2:"+areas.get(1).getPoly().get_points().get(0));
		Assert.assertTrue(new Vector(20,0).almostEquals(areas.get(1).getPoly().get_points().get(0),1e-5));
		Assert.assertTrue(new Vector(0,0).almostEquals(areas.get(2).getPoly().get_points().get(0),1e-5));
	}

	private Airspace create_space(Vector[] off) {
		ArrayList<AirspaceArea> areas=new ArrayList<AirspaceArea>();
		Polygon polyA=new Polygon(new Vector[]{
				new Vector(0,0).plus(off[0]),new Vector(1,0).plus(off[0]),new Vector(1,1).plus(off[0])	
			});
		Polygon polyB=new Polygon(new Vector[]{
				new Vector(0,0).plus(off[1]),new Vector(1,0).plus(off[1]),new Vector(1,1).plus(off[1])	
			});
		Polygon polyC=new Polygon(new Vector[]{
				new Vector(0,0).plus(off[2]),new Vector(1,0).plus(off[2]),new Vector(1,1).plus(off[2])	
			});
		areas.add(new AirspaceArea("A",polyA));	
		areas.add(new AirspaceArea("B",polyB));
		areas.add(new AirspaceArea("C",polyC));
		Airspace space=new Airspace(areas);
		return space;
	}
}
