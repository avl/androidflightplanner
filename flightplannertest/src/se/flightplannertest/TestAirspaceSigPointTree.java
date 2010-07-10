package se.flightplannertest;

import static org.junit.Assert.*;


import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FilterReader;
import java.io.InputStreamReader;
import java.util.ArrayList;

import junit.framework.Assert;

import org.junit.Test;

import se.flightplanner.Airspace;
import se.flightplanner.AirspaceSigPointsTree;
import se.flightplanner.Project;
import se.flightplanner.SigPoint;
import se.flightplanner.Project.LatLon;
import se.flightplanner.Project.Merc;
import se.flightplanner.vector.BoundingBox;

public class TestAirspaceSigPointTree {
	@Test
	public void testFindall() throws Exception {
		
		
		StringBuilder fakedata=new StringBuilder();
		BufferedReader fr=new BufferedReader(
				new InputStreamReader(
						new FileInputStream("/home/anders/workspace/flightplannertest/airspaces.json"),
						"UTF-8"
					));
		for(;;)
		{
			String r=fr.readLine();
			if (r==null) break;
			fakedata.append(r);
			fakedata.append("\n");
		}				
		Airspace s=Airspace.download(fakedata.toString());
		int zoomlevel=8;
		int zoomgap=13-zoomlevel;
		Merc center13=Project.latlon2merc(new LatLon(59.4,17.9),13);
		int ox=320/2;
		int oy=480/2;
		double diagonal13=((1<<zoomgap)*(Math.sqrt(ox*ox+oy*oy)+50))+1;
		BoundingBox bb13=new BoundingBox(
				center13.x-diagonal13,center13.y-diagonal13,
				center13.x+diagonal13,center13.y+diagonal13);
		AirspaceSigPointsTree tree=new AirspaceSigPointsTree(s.getPoints());
		ArrayList<SigPoint> items=tree.findall(bb13);
		Assert.assertTrue(items.size()>=2);

	}
}
