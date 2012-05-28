package se.flightplanner2;

import java.util.Date;

import se.flightplanner2.Project.LatLon;

import android.location.Location;

public interface DetailedPlace {

	double getDistance();

	/**
	 * Expected time over this place, if the aircraft is flown at
	 * planned GS, along the planned route.
	 * @return
	 */
	Date getETA2();

	/**
	 * Expected time over this place, if the aircraft is flown at
	 * directly toward this point, at the current actual GS.
	 * @return
	 */
	Date getETA();

	void update_pos(Location location);

	String getName();

	boolean hasPlanned();

	Date getPlanned();

	LatLon getPos();

	Float getPlannedFuel();

	Float getPlannedGs();
	
	String getPlannedAlt();
	
	boolean hasPrevNext();
	void prev();
	void next();
	boolean is_own_position();

	Date getPassed();
	boolean hasPassed();
}
