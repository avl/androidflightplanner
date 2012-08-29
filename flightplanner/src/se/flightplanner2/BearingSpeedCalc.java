package se.flightplanner2;

import se.flightplanner2.Project.LatLon;
import se.flightplanner2.Project.Merc;
import android.location.Location;
import android.os.SystemClock;
import android.util.Log;

public class BearingSpeedCalc {
	Location lastpos=null;
	long last_actual_time=0;
	int strangecount=0;
	Location calcBearingSpeed(Location loc) {
		if (loc==null)
		{ //just for debug
			if (lastpos!=null)
				return lastpos;
			loc=new Location("gps");
			//, "alt": 30, "lon":
			
			
			//Arlanda:
			loc.setLatitude(59.653347);
			loc.setLongitude(17.911091);
			loc.setBearing(45);
			//Åre:
			//loc.setLatitude(63.4306);
			//loc.setLongitude(13.093);
			//loc.setBearing(45);

			//Norway somewhere
			//loc.setAltitude(1500);
			//loc.setLatitude(61.417632);
			//loc.setLongitude(8.481445);
			//loc.setBearing(270);
			//loc.setSpeed(50);
		}
		Location mylocation=loc;
		//Log.i("fplan.bs","myloc has speed:"+mylocation.hasSpeed()+" lastpos: "+lastpos);
		if (lastpos!=null)
		{
			Merc prev=Project.latlon2merc(new LatLon(lastpos.getLatitude(),lastpos.getLongitude()),13);
			Merc cur=Project.latlon2merc(new LatLon(mylocation.getLatitude(),mylocation.getLongitude()),13);
			Merc mid=new Merc(0.5*(prev.x+cur.x),0.5*(prev.y+cur.y));
			double time_passed=(mylocation.getTime()-lastpos.getTime())/1000.0;
			double mercs_per_nm=Project.approx_scale(mid, 13, 1.0);
			double dx=cur.x-prev.x;
			double dy=cur.y-prev.y;
			
			//Log.i("fplan.bs","myloc has speed (2):"+mylocation.hasSpeed()+" lastpos: "+lastpos);
			
			if (true)
			{
				double diffmercs=Math.sqrt(dx*dx+dy*dy);
				double dist_nm=diffmercs/mercs_per_nm;
				
				long now=SystemClock.elapsedRealtime();
				double actual_delta_h=(now-last_actual_time)/(3600.0*1000);
				if (actual_delta_h<3/3600.0) actual_delta_h=3/3600.0;//maybe it's been a few seconds, we could have missed because of scheduling jitter (at most)
				double actual_speed=dist_nm/actual_delta_h;
				if (actual_speed>3000.0) //>3000kt is waaaaay faster than we could realistically have moved.
				{
					++strangecount;
					if (strangecount<10) //require 10 samples of new extreme position before accepting.
						return lastpos;
				}
				strangecount=0;
				
				double dist_m=dist_nm*1852.0;
				double speed=0;
				//Log.i("fplan.bs","dist_m:"+dist_m+" time:"+time_passed);
				if (!mylocation.hasSpeed())
				{
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
