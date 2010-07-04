package se.flightplanner;

import android.location.Location;

public class TripState {

	private TripData tripdata;
	
	/**
	 * The waypoint which is our current target.
	 * Starts out as waypoint 1, (the second one in the list),
	 * except if there is only one waypoint, when the target will
	 * always be 0.
	 */
	private int target_wp;  
	
	/**
	 * Determines the most plausible current target.
	 * Considers the current 'target_wp' (not selecting
	 * previously passed targets, if multiple targets are plausible).
	 * It never goes to an already visited target, since that could
	 * potentially send us flying in a loop until we run out of gas ... :-)
	 */
	public void update_target(Location mylocation)
	{
		
	}
	
	
		
}
