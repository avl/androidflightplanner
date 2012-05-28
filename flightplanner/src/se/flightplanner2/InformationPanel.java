package se.flightplanner2;

import java.util.Date;

import android.location.Location;

import se.flightplanner2.vector.Vector;

public interface InformationPanel {

	public abstract String getLegTitle();
	public abstract String getPointTitle();

	public abstract String[] getDetails();

	public abstract String[] getExtraDetails();

	public abstract Vector getPoint();

	public abstract double getDistance();

	public abstract boolean getSkipped();
	public abstract Date getPassed();
	public abstract Date getETA2();

	public abstract boolean hasLeft();
	public abstract void left();

	public abstract boolean hasRight();
	public abstract void right();

	/**
	 */
	public abstract void updatemypos(Location loc);

	public abstract boolean getHasExtraInfo();

	public abstract Place[] getHasExtendedInfo();
	/**
	 * True if this represents an attempt to view waypoint data
	 * when there are no waypoints. 
	 */
	boolean getEmpty();
	public abstract double getHeading();


}