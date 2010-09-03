package se.flightplanner;

import java.util.ArrayList;

public class AirspaceLookup {

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
