package se.flightplanner.simpler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import se.flightplanner.AirspaceArea;
import se.flightplanner.AirspaceLookupIf;
import se.flightplanner.Project;
import se.flightplanner.Project.LatLon;
import se.flightplanner.Project.Merc;
import se.flightplanner.simpler.Common.Compartment;
import se.flightplanner.vector.BoundPie;
import se.flightplanner.vector.BoundingBox;
import se.flightplanner.vector.Pie;
import se.flightplanner.vector.Polygon.SectorResult;

public class FindNearby {
	public static class FoundAirspace
	{
		public FoundAirspace(double dist,AirspaceArea area,Pie pie)
		{
			this.distance=dist;
			this.area=area;
			this.pie=pie;
		}
		public double distance;
		public AirspaceArea area;
		public Pie pie;
	}
	private HashMap<Common.Compartment,ArrayList<FoundAirspace> > spaces=new HashMap<Common.Compartment, ArrayList<FoundAirspace>>();
	
	
	public HashMap<Common.Compartment,ArrayList<FoundAirspace> > get_spaces()
	{
		return spaces;
	}
	
	private AirspaceLookupIf lookup;
	private double dist_nm=50;
	public FindNearby(AirspaceLookupIf l,LatLon startpos,float hdg)
	{
		this.lookup=l;
		
		update(startpos, hdg);
        		
	}
	private void update(LatLon pos, float hdg) {
		spaces.clear();
		Merc mpos=Project.latlon2merc(pos, 13);
		
		double dist_merc=Project.approx_scale(mpos, 13,dist_nm);
		ArrayList<AirspaceArea> allareas=lookup.getAreas().get_areas(new BoundingBox(mpos.toVector(),dist_merc));
        ArrayList<FoundAirspace> left=putcomp(Common.Compartment.LEFT);
        ArrayList<FoundAirspace> ahead=putcomp(Common.Compartment.AHEAD);
        ArrayList<FoundAirspace> right=putcomp(Common.Compartment.RIGHT);
        ArrayList<FoundAirspace> present=putcomp(Common.Compartment.PRESENT);
		
		BoundPie aheadpie=new BoundPie(mpos.toVector(),Common.getPie(Compartment.AHEAD).swingRight(hdg));
		BoundPie leftpie=new BoundPie(mpos.toVector(),Common.getPie(Compartment.LEFT).swingRight(hdg));
		BoundPie rightpie=new BoundPie(mpos.toVector(),Common.getPie(Compartment.RIGHT).swingRight(hdg));
		
		for(AirspaceArea a:allareas)			
		{
		    SectorResult res=a.poly.sector(aheadpie,dist_merc);
		    if (res==null) continue; //polygon has no points
		    if (res.inside)
		    {
		    	present.add(new FoundAirspace(res.nearest_distance_to_center, a,res.pie.swingLeft(hdg)));
		    	continue;		    	
		    }		    
		    if (res.nearest_distance_to_center<dist_merc)
	    		ahead.add(new FoundAirspace(res.nearest_distance_to_center, a,res.pie.swingLeft(hdg)));
		    	
			res=a.poly.sector(leftpie,dist_merc);
			if (res.nearest_distance_to_center<dist_merc)
				left.add(new FoundAirspace(res.nearest_distance_to_center, a,res.pie.swingLeft(hdg)));

			res=a.poly.sector(rightpie,dist_merc);
			if (res.nearest_distance_to_center<dist_merc)
				right.add(new FoundAirspace(res.nearest_distance_to_center, a,res.pie.swingLeft(hdg)));
		}
		
		sortall();
	}
	private ArrayList<FoundAirspace> putcomp(Common.Compartment comp) {
		ArrayList<FoundAirspace> ret=new ArrayList<FoundAirspace>();
		spaces.put(comp,ret);		
		return ret;
	}
	
	private void sortall() {
		for(ArrayList<FoundAirspace> x:spaces.values())
		{
			
			Collections.sort(x,new Comparator<FindNearby.FoundAirspace>(){
				@Override
				public int compare(FoundAirspace lhs, FoundAirspace rhs) {
					if (lhs.distance<rhs.distance) return -1;
					if (lhs.distance>rhs.distance) return +1;
					return Double.compare(lhs.area.poly.get_area(), rhs.area.poly.get_area());
				}
			});
		}		
	}
	
}
