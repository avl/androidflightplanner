package se.flightplanner;

import java.util.ArrayList;

import se.flightplanner.vector.BoundingBox;
import se.flightplanner.vector.Vector;
import se.flightplanner.vector.Polygon.InsideResult;

public class AirspaceLookup {
	public void get_airspace_details(double abit,
		Vector just_a_bit_in,ArrayList<String> details,ArrayList<String> extradetails) {
		for(AirspaceArea inarea:areas.get_areas(BoundingBox.aroundpoint(just_a_bit_in, abit)))
		{
			
			InsideResult r=inarea.poly.inside(just_a_bit_in);
			//double cd=r.closest.minus(point).length();
			if (r.isinside) //our polygons are clockwise, because the Y-axis points down - this inverts the meaning of inside and outside
			{ //If _INSIDE_ polygon
				String det=inarea.floor+"-"+inarea.ceiling+": "+inarea.name;
				details.add(det);
				extradetails.add(det);
				for(String fre : inarea.freqs)
				{
					if (fre.length()>0)
					{
						//Log.i("fplan","Adding airspace detail "+fre);
						extradetails.add(fre);
					}
				}
			}
		}
		if (details.size()==0)
		{
			details.add("0 ft-FL 095: Uncontrolled Airspace");
			extradetails.add("0 ft-FL 095: Uncontrolled Airspace");
		}
		
	}

	public AirspaceLookup(Airspace airspace) {
		
		ArrayList<AirspaceArea> areaarr;
		if (airspace==null)
			areaarr=new ArrayList<AirspaceArea>();
		else
			areaarr=airspace.getSpaces();
		areas=new AirspaceAreaTree(areaarr);
		ArrayList<SigPoint> pointarr;
		if (airspace==null)
			pointarr=new ArrayList<SigPoint>();
		else
			pointarr=airspace.getPoints();
		ArrayList<SigPoint> airfields=new ArrayList<SigPoint>();
		ArrayList<SigPoint> others=new ArrayList<SigPoint>();
		ArrayList<SigPoint> obsts=new ArrayList<SigPoint>();
		for(SigPoint po: pointarr)
		{
			if (po.kind.equals("airport"))
				airfields.add(po);
			else
			if (po.kind.equals("obstacle"))
				obsts.add(po);
			else
				others.add(po);					
		}
		allAirfields=new AirspaceSigPointsTree(airfields);
		allObst=new AirspaceSigPointsTree(obsts);
		allOthers=new AirspaceSigPointsTree(others);
		// TODO Auto-generated constructor stub
	}
	public AirspaceAreaTree areas;
	public AirspaceSigPointsTree allAirfields;
	public AirspaceSigPointsTree allObst;
	public AirspaceSigPointsTree allOthers;
}
