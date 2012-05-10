package se.flightplanner2;

import se.flightplanner2.Project.LatLon;

public class GlobalGetElev {

	static public interface GetElevation
	{
		/**
		 * Warning, this may be slow, since it may have to hit the disk.
		 * Will return Short.MAX_VALUE if an error occurs. 
		 */
		public short get_elev_ft(LatLon pos);
	}
	static public GetElevation get_elev;
}
