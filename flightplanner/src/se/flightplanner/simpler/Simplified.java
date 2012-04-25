package se.flightplanner.simpler;

import java.util.ArrayList;
import java.util.HashMap;

import se.flightplanner.AirspaceArea;
import se.flightplanner.AirspaceLookup;
import se.flightplanner.Project;
import se.flightplanner.Project.LatLon;
import se.flightplanner.Project.Merc;
import se.flightplanner.vector.BoundPie;
import se.flightplanner.vector.BoundingBox;
import se.flightplanner.vector.Pie;
import se.flightplanner.vector.Polygon.SectorResult;

public class Simplified {
	static enum Compartment {
		LEFT,AHEAD,RIGHT,PRESENT
	}
	
	private HashMap<Compartment,ArrayList<AirspaceArea> > spaces;
	private AirspaceLookup lookup;
	public Simplified(AirspaceLookup l,LatLon startpos,float hdg)
	{
		this.lookup=l;
		double dist_nm=50;
		Merc mpos=Project.latlon2merc(startpos, 13);
		ArrayList<AirspaceArea> allareas=l.areas.get_areas(new BoundingBox(mpos.toVector(),Project.approx_scale(mpos, 13,dist_nm)));
        ArrayList<AirspaceArea> left=spaces.put(Compartment.LEFT,new ArrayList<AirspaceArea>());
        ArrayList<AirspaceArea> ahead=spaces.put(Compartment.AHEAD,new ArrayList<AirspaceArea>());
        ArrayList<AirspaceArea> right=spaces.put(Compartment.RIGHT,new ArrayList<AirspaceArea>());
        ArrayList<AirspaceArea> present=spaces.put(Compartment.PRESENT,new ArrayList<AirspaceArea>());
		
		BoundPie aheadpie=new BoundPie(mpos.toVector(),new Pie(hdg-45,hdg+45));
		BoundPie leftpie=new BoundPie(mpos.toVector(),new Pie(hdg-100,hdg-45));
		BoundPie rightpie=new BoundPie(mpos.toVector(),new Pie(hdg+45,hdg+100));
		
		for(AirspaceArea a:allareas)			
		{
		    SectorResult res=a.poly.sector(aheadpie);
		    if (res==null) continue;
		    if (res==null || res.nearest_distance_to_center>dist_nm) continue;
		    if (res.inside)
		    {
		    	present.add(a);
		    	continue;
		    }
	    	ahead.add(a);
		    	

		}
        		
	}
	
}
