package se.flightplanner2;

import android.location.Location;
import android.os.SystemClock;
import se.flightplanner2.GlobalGetElev.GetElevation;
import se.flightplanner2.Project.LatLon;
import se.flightplanner2.Project.Merc;
import se.flightplanner2.Project.iMerc;
import se.flightplanner2.vector.Vector;

public class ElevationProfile {

	int[] last;
	long lastret;
	int lastzoomlevel;
	/*!
	 * Get height profile for the given locs, or if 
	 * less than 3 seconds have elapsed since last call,
	 * simply return the exact same array again.
	 */
	public int[] getProfile(Location lastpos, float howfar, int steps,long max_age,int zoomlevel) {
		if (last!=null && zoomlevel==lastzoomlevel && SystemClock.elapsedRealtime()-lastret<max_age)
			return last;
		lastzoomlevel=zoomlevel;
		iMerc[] locs=makeelevpath(lastpos,howfar,steps);

		GetElevation getter=GlobalGetElev.get_elev;
		if (getter==null)
		{
			last=new int[]{};
			lastret=SystemClock.elapsedRealtime();
			return last;
		}
		int[] ret=new int[locs.length];
		for(int i=0;i<locs.length;++i)
		{
			LatLon pos=Project.imerc2latlon(locs[i], 13);
			ret[i]=getter.get_elev_ft(pos, 8, 1);
		}
		last=ret;
		lastret=SystemClock.elapsedRealtime();
		return ret;
	}
	private iMerc[] makeelevpath(Location lastpos, float howfar, int steps) {
		Merc merc=Project.latlon2merc(new LatLon(lastpos), 13);
		float chunknm=(float)Project.approx_scale(merc, 13, howfar/steps);
		Vector delta=Project.heading2vector(lastpos.getBearing()).mul(chunknm);
		 
		Vector cur=merc.toVector();
		iMerc[] locs=new iMerc[steps];
		for(int i=0;i<steps;++i)
		{
			locs[i]=new iMerc(cur.x,cur.y);
			cur.x+=delta.x;
			cur.y+=delta.y;
		}
		return locs;
	}

}
