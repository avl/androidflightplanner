package se.flightplanner;

import java.util.ArrayList;

import se.flightplanner.Project.LatLon;
import se.flightplanner.vector.Vector;

public class AirspacePointInfo implements InformationPanel
{
	private Vector point;
	private String[] details;
	private String[] extra;
	private double distance;
	private int when;
	private boolean hasextra;
	/**
	 * 
	 * @param is_direct True if this is a simple waypoint, which is the next in the trip, and 
	 * we want the ETA calculated naively from the current actual groundspeed, using a direct track..
	 */
	public AirspacePointInfo(LatLon about,AirspaceLookup lookup)
	{
		ArrayList<String> details2 = new ArrayList<String>(); 
		ArrayList<String> extra2 = new ArrayList<String>();
		point=Project.latlon2mercvec(about,13);
		hasextra=lookup.get_airspace_details(1.0,
				point,details2,extra2);			
		details=details2.toArray(new String[details2.size()]);
		extra=extra2.toArray(new String[extra2.size()]);
		when=0;
		distance=0;
	}
	
	/**
	 * @param mypos Position in merc13 coordinates.
	 * @param actualGs GS in knots
	 */
	public void updatemypos(Vector mypos, double actualGs) {
		double onenm=Project.approx_scale(point.plus(mypos).mul(0.5).gety(),13,1.0);
		this.distance=point.minus(mypos).length()/onenm; //in nautical miles.
		if (actualGs>1e-3)
			this.when=(int)(3600.0*distance/actualGs);
		else
			this.when=3600*9999;
	}
	@Override
	public String getTitle()
	{
		return "Airspace";
	}
	@Override
	public String[] getDetails()
	{
		return details;
	}
	@Override
	public String[] getExtraDetails()
	{
		return extra;
	}
	@Override
	public Vector getPoint() {
		if (point==null)
			throw new RuntimeException("point is null");
		return point;
	}
	@Override
	public double getDistance() {
		return distance;
	}
	@Override
	public long getWhen() {
		return when;
	}
	@Override
	public boolean hasLeft() {
		return false;
	}
	@Override
	public void left() {		
	}
	@Override
	public boolean hasRight() {
		return false;
	}
	@Override
	public void right() {		
	}
	@Override
	public boolean getHasExtraInfo() {
		return hasextra;
	}

}