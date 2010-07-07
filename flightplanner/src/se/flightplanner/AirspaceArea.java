/**
 * 
 */
package se.flightplanner;

import java.io.Serializable;
import java.util.ArrayList;

import se.flightplanner.Project.LatLon;
import se.flightplanner.vector.Polygon;

public class AirspaceArea implements Serializable 
{
	private static final long serialVersionUID = -4964236460301544582L;
	Polygon poly;
	String name;
	ArrayList<LatLon> points;
	ArrayList<String> freqs;
	String floor;
	String ceiling;
	public Polygon getPoly(){return poly;}
	public AirspaceArea(){}
	public AirspaceArea(String pname,Polygon ppoly)
	{
		poly=ppoly;
		name=pname;
	}
}