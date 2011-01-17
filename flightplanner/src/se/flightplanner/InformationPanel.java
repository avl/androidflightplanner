package se.flightplanner;

import se.flightplanner.vector.Vector;

public interface InformationPanel {

	public abstract String getTitle();

	public abstract String[] getDetails();

	public abstract String[] getExtraDetails();

	public abstract Vector getPoint();

	public abstract double getDistance();

	public abstract long getWhen();

	public abstract boolean hasLeft();
	public abstract void left();

	public abstract boolean hasRight();
	public abstract void right();

	public abstract void updatemypos(Vector latlon2mercvec, double d);


}