package se.flightplanner;

import se.flightplanner.Project.LatLon;
import se.flightplanner.Project.Merc;
import android.location.Location;

public class BearingSpeedCalc {
	Location lastpos=null;
	Location calcBearingSpeed(Location loc) {
		if (loc==null)
		{ //just for debug
			loc=new Location("gps");
			//, "alt": 30, "lon": 
			loc.setLatitude(59.458333333299997);
			loc.setLongitude(17.706666666699999);
			loc.setBearing(150);
			loc.setSpeed(50);
		}
		Location mylocation=loc;
		if (lastpos!=null && (!mylocation.hasSpeed() || !mylocation.hasBearing()))
		{
			Merc prev=Project.latlon2merc(new LatLon(lastpos.getLatitude(),lastpos.getLongitude()),13);
			Merc cur=Project.latlon2merc(new LatLon(mylocation.getLatitude(),mylocation.getLongitude()),13);
			Merc mid=new Merc(0.5*(prev.x+cur.x),0.5*(prev.y+cur.y));
			double time_passed=(mylocation.getTime()-lastpos.getTime())/1000.0;
			double mercs_per_nm=Project.approx_scale(mid, 13, 1.0);
			double dx=cur.x-prev.x;
			double dy=cur.y-prev.y;
			if (!mylocation.hasSpeed())
			{
				double diffmercs=Math.sqrt(dx*dx+dy*dy);
				double dist_nm=diffmercs/mercs_per_nm;
				double dist_m=dist_nm*1852.0;
				double speed=0;
				if (time_passed!=0)
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
				double tt = Project.vector2heading(dx, dy);
				mylocation.setBearing((float) tt);
			}
		}
		return mylocation;
	}

}
