package se.flightplanner2;

import java.util.ArrayList;

import android.location.Location;

import se.flightplanner2.Project.LatLon;
import se.flightplanner2.Project.Merc;
import se.flightplanner2.simpler.Common;
import se.flightplanner2.simpler.Common.Compartment;
import se.flightplanner2.simpler.FindNearby.FoundAirspace;
import se.flightplanner2.vector.BoundPie;
import se.flightplanner2.vector.BoundingBox;
import se.flightplanner2.vector.Pie;
import se.flightplanner2.vector.Polygon.SectorResult;

public class AirspaceProximityDetector {

	private AirspaceLookup lookup;
	private String warning;
	private float dist;
	private float time;
	private float warn_time;
	private String[] areanames;
	public AirspaceProximityDetector(AirspaceLookup lookup,float warn_time)
	{
		this.warn_time=warn_time;
		this.lookup=lookup;
	}
	private String shortify(String f,int len)
	{
		if (f.length()>len+3)
			f=f.substring(0,len)+"...";
		return f;
	}
	public void run(Location loc)
	{
		final float gs=(float)(loc.getSpeed()*3.6/1.852);
		warning=null;
		dist=-1;
		time=1e10f;
		areanames=null;
		if (gs<2)
		{
			return;
		}
		float hdg=loc.getBearing();
		LatLon pos=new LatLon(loc);
		double dist_nm=Math.max(3, gs*(warn_time/60.0f));
		Merc mpos=Project.latlon2merc(pos, 13);
		float onenm=(float) Project.approx_scale(mpos, 13, 1.0);
		
		double dist_merc=Project.approx_scale(mpos, 13,dist_nm);
		
		ArrayList<AirspaceArea> allareas=lookup.getAreas().get_areas(new BoundingBox(mpos.toVector(),dist_merc));
		Pie aheadpie=new Pie(355,5);
		aheadpie=aheadpie.swingRight(hdg);
		BoundPie boundaheadpie=new BoundPie(mpos.toVector(),aheadpie);
		
		float closest_dist=1e10f;
		ArrayList<AirspaceArea> areas=new ArrayList<AirspaceArea>();
		for(AirspaceArea a:allareas)			
		{
			if (a.cleared) continue;		
		    SectorResult res=a.poly.sector(boundaheadpie,dist_merc);
		    if (res.inside) continue; //Don't warn for airspaces we're already in.
		    if (res==null) continue; 
		    if (res.nearest_distance_to_center>dist_merc)
		    	continue;
		    float distnm=(float)res.nearest_distance_to_center/onenm;
		    if (Math.abs(distnm-closest_dist)<0.05f)
		    {
		    	areas.add(a);
		    }
		    else if(distnm<closest_dist)
		    {
		    	closest_dist=distnm;
		    	areas=new ArrayList<AirspaceArea>();
		    	areas.add(a);
		    }
		}
		
		if (areas.size()>0)
		{
			areanames=new String[areas.size()];
			for(int i=0;i<areas.size();++i)
				areanames[i]=areas.get(i).name;
			String when;
	    	time=60.0f*closest_dist/gs;
	    	
	    	if (time<1)
	    		when=String.format("<1min");
	    	else
	    		when=String.format("%.0fmin",Math.floor(time));
		    if (areas.size()==1)
		    {
		    	warning=shortify(areas.get(0).name,20)+". "+when;
		    }
		    else
		    {
		    	
		    	warning=shortify(areas.get(0).name,10)+" + "+(areas.size()-1)+" more. "+when;
		    }
		    dist=closest_dist;
		}
	    		
		
	}
	public float getTimeLeft()
	{
		return time;
	}
	public float getDistLeft()
	{
		return dist;
	}
	public boolean isWarning() {
		return warning!=null;
	}
	public String getWarning() {
		return warning;
	}
	public void update_lookup(AirspaceLookup lookup) {
		this.lookup=lookup;
		
	}
	public String[] getAreanames()
	{
		if (areanames==null) return new String[]{};
		return areanames;
	}
}
