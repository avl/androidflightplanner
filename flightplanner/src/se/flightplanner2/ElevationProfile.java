package se.flightplanner2;

import se.flightplanner2.GlobalGetElev.GetElevation;
import se.flightplanner2.Project.LatLon;
import se.flightplanner2.Project.iMerc;

public class ElevationProfile {

	public int[] getProfile(iMerc[] locs) {
		// TODO Auto-generated method stub
		GetElevation getter=GlobalGetElev.get_elev;
		if (getter==null)
			return new int[]{};
		int[] ret=new int[locs.length];
		for(int i=0;i<locs.length;++i)
		{
			LatLon pos=Project.imerc2latlon(locs[i], 13);
			ret[i]=getter.get_elev_ft(pos, 8, 1);
		}
		return ret;
	}

}
