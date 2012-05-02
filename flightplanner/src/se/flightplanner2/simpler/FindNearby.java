package se.flightplanner2.simpler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import android.util.Log;

import se.flightplanner2.AirspaceArea;
import se.flightplanner2.AirspaceLookupIf;
import se.flightplanner2.Project;
import se.flightplanner2.Project.LatLon;
import se.flightplanner2.Project.Merc;
import se.flightplanner2.simpler.Common.Compartment;
import se.flightplanner2.vector.BoundPie;
import se.flightplanner2.vector.BoundingBox;
import se.flightplanner2.vector.Pie;
import se.flightplanner2.vector.Polygon.SectorResult;

public class FindNearby {
	public static class FoundAirspace
	{
		public FoundAirspace(double dist,AirspaceArea area,Pie pie,float bearing,float relbearing)
		{
			assert pie!=null;
			this.distance=dist;
			this.area=area;
			this.pie=pie;
			this.bearing=bearing;
			this.relbearing=relbearing;
		}
		public double distance;
		public float bearing;
		public float relbearing;
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
		if (hdg<0) hdg+=360;
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
		    	present.add(new FoundAirspace(res.nearest_distance_to_center, a,res.pie.swingLeft(hdg),0,0));
		    	continue;		    	
		    }		    
		    if (res.nearest_distance_to_center<dist_merc)
		    {
		    	//Log.i("fplan","Ahead pie:"+aheadpie+" res bearing: "+res.bearing);
	    		ahead.add(new FoundAirspace(res.nearest_distance_to_center, a,res.pie.swingLeft(hdg),res.bearing,(float)(res.bearing-hdg+360.0)%360.0f));
		    }
		    	
			res=a.poly.sector(leftpie,dist_merc);
			if (res.nearest_distance_to_center<dist_merc)			
				left.add(new FoundAirspace(res.nearest_distance_to_center, a,res.pie.swingLeft(hdg),res.bearing,(float)(res.bearing-hdg+360.0)%360.0f));

			res=a.poly.sector(rightpie,dist_merc);
			if (res.nearest_distance_to_center<dist_merc)
				right.add(new FoundAirspace(res.nearest_distance_to_center, a,res.pie.swingLeft(hdg),res.bearing,(float)(res.bearing-hdg+360.0)%360.0f));
		}
		
		//Purge duplicates
		for(FoundAirspace ah:ahead)
		{
			for(int i=left.size()-1;i>=0;--i)
			{
				FoundAirspace l=left.get(i);
				if (l.area==ah.area && bearingdist(l.bearing,ah.bearing)<30 && ah.distance*0.75<l.distance)
					left.remove(i);
			}
			for(int i=right.size()-1;i>=0;--i)
			{
				FoundAirspace l=right.get(i);
				if (l.area==ah.area && bearingdist(l.bearing,ah.bearing)<30 && ah.distance*0.75<l.distance)
					right.remove(i);
			}
			
		}
		
		sortall();
	}
	private float bearingdist(float a, float b) {
		float delta=(float)Math.abs(a-b)%360.0f;
		if (delta>180)
			delta=360-delta;
				
		return delta;
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
