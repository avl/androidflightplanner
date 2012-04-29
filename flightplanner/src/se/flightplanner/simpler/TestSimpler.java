package se.flightplanner.simpler;

import java.util.ArrayList;
import java.util.HashMap;


import junit.framework.TestCase;

import se.flightplanner.AirspaceArea;
import se.flightplanner.AirspaceAreaTree;
import se.flightplanner.AirspaceLookupIf;
import se.flightplanner.Project;
import se.flightplanner.Project.LatLon;
import se.flightplanner.simpler.AirspaceLayout;
import se.flightplanner.simpler.AirspaceLayout.Measurer;
import se.flightplanner.simpler.AirspaceLayout.Row;
import se.flightplanner.simpler.AirspaceLayout.Rows;
import se.flightplanner.simpler.Common;
import se.flightplanner.simpler.Common.Rect;
import se.flightplanner.simpler.FindNearby;
import se.flightplanner.simpler.Common.Compartment;
import se.flightplanner.simpler.FindNearby.FoundAirspace;
import se.flightplanner.vector.Polygon;
import se.flightplanner.vector.Vector;

public class TestSimpler extends TestCase {

	public void testSimple1()
	{
		ArrayList<AirspaceArea> areas=new ArrayList<AirspaceArea>();

		addArea(areas, new LatLon[]{
				new LatLon(59,18),
				new LatLon(60,18),				
				new LatLon(60,19),
				new LatLon(59,19)
		}, "TestOmråde");
		addArea(areas, new LatLon[]{
				new LatLon(58.7,18.4),
				new LatLon(59.7,18.4),				
				new LatLon(59.7,18.6),
				new LatLon(58.7,18.6)
		}, "TestOmråde2");

		final AirspaceAreaTree tree=new AirspaceAreaTree(areas);
		
		FindNearby nearby=new FindNearby(new AirspaceLookupIf()
		{
			@Override
			public AirspaceAreaTree getAreas() {
				return tree;
			}
		},new LatLon(58.5,18.5),0);
		
		HashMap<Common.Compartment,ArrayList<FoundAirspace> > ret=nearby.get_spaces();
		assertEquals(2,ret.get(Compartment.AHEAD).size());
		FoundAirspace fa=ret.get(Compartment.AHEAD).get(0);
		System.out.println("FA Pie: "+fa.pie);
		System.out.println("FA distance: "+fa.distance);
		System.out.println("FA Name: "+fa.area.name);
		assertEquals("TestOmråde2",fa.area.name);
		assertTrue(fa.pie.getA()<350);
		assertTrue(fa.pie.getB()>10);
		
		AirspaceLayout layout=new AirspaceLayout(new Measurer() {
			@Override
			public Rect measure(AirspaceArea area) {
				return new Rect(0,0,100,50);
			}
		},nearby);
		
		layout.update(1000, 1000);
		Rows rows=layout.getRows(Compartment.AHEAD);
		assertEquals(1,rows.rows.size());
		
		Row row=rows.rows.get(0);
		assertTrue(row.open);
		assertEquals(2,row.cells.size());
		assertEquals("TestOmråde2",row.cells.get(0).area.area.name);
		System.out.println("Layout result:"+row);
		
		
		
		
	}

	private void addArea(ArrayList<AirspaceArea> areas, LatLon[] edges,
			String areaname) {
		AirspaceArea area;
		area=new AirspaceArea();
		area.name=areaname;
		ArrayList<Vector> points=new ArrayList<Vector>();
		for(LatLon ll:edges)
		{
			points.add(Project.latlon2mercvec(ll, 13));
		}
		area.poly=new Polygon(points);
		
		
		areas.add(area);
	}
}
