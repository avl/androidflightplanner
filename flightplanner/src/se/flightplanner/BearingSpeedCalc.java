package se.flightplanner;

import se.flightplanner.Project.LatLon;
import se.flightplanner.Project.Merc;
import android.location.Location;
import android.util.Log;

public class BearingSpeedCalc {
	Location lastpos=null;
	Location calcBearingSpeed(Location loc) {
		if (loc==null)
		{ //just for debug
			loc=new Location("gps");
			//, "alt": 30, "lon": 
			loc.setLatitude(59.458333333299997);
			loc.setLongitude(17.706666666699999);
			loc.setBearing(45);
			loc.setSpeed(50);
		}
		Location mylocation=loc;
		//Log.i("fplan.bs","myloc has speed:"+mylocation.hasSpeed()+" lastpos: "+lastpos);
		if (lastpos!=null && (!mylocation.hasSpeed() || !mylocation.hasBearing()))
		{
			Merc prev=Project.latlon2merc(new LatLon(lastpos.getLatitude(),lastpos.getLongitude()),13);
			Merc cur=Project.latlon2merc(new LatLon(mylocation.getLatitude(),mylocation.getLongitude()),13);
			Merc mid=new Merc(0.5*(prev.x+cur.x),0.5*(prev.y+cur.y));
			double time_passed=(mylocation.getTime()-lastpos.getTime())/1000.0;
			double mercs_per_nm=Project.approx_scale(mid, 13, 1.0);
			double dx=cur.x-prev.x;
			double dy=cur.y-prev.y;
			//Log.i("fplan.bs","myloc has speed (2):"+mylocation.hasSpeed()+" lastpos: "+lastpos);
			
			if (!mylocation.hasSpeed())
			{
				double diffmercs=Math.sqrt(dx*dx+dy*dy);
				double dist_nm=diffmercs/mercs_per_nm;
				double dist_m=dist_nm*1852.0;
				double speed=0;
				//Log.i("fplan.bs","dist_m:"+dist_m+" time:"+time_passed);
				if (time_passed!=0 && time_passed<5.0f) //don't calculate speed after a too long interruption.
				{				
					speed=dist_m/time_passed;
				}
				else
				{
					if (lastpos.hasSpeed())
						speed=lastpos.getSpeed();
					else
						speed=0.0;
				}
				mylocation.setSpeed((float) speed);
			}
			if (!mylocation.hasBearing())
			{
				if (time_passed<5.0f)
				{
					double tt = Project.vector2heading(dx, dy);
					mylocation.setBearing((float) tt);
				}
				else
				{
					if (lastpos.hasBearing())
						mylocation.setBearing(lastpos.getBearing());
					else
						mylocation.setBearing(0.0f);
				}
				Log.i("fplan.bs","bearing:"+mylocation.getBearing());
			}
				
		}
		if (!mylocation.hasSpeed() || Math.abs(mylocation.getSpeed())<1.25)
		{
			if (lastpos!=null && lastpos.hasBearing())
				mylocation.setBearing(lastpos.getBearing());
			else
				mylocation.setBearing(0.0f);
		}
		lastpos=new Location(mylocation);
		return mylocation;
	}

}
