package se.flightplanner;

import se.flightplanner.vector.Vector;

public class AirspacePointInfo implements InformationPanel
{
	private String title;
	private Vector point;
	private String[] details;
	private String[] extra;
	private double distance;
	private int when;
	
	private boolean is_direct;
	/**
	 * 
	 * @param is_direct True if this is a simple waypoint, which is the next in the trip, and 
	 * we want the ETA calculated naively from the current actual groundspeed, using a direct track..
	 */
	public AirspacePointInfo(
			String title,String[] details,String[] extra,Vector point,
			boolean is_direct)
	{
		this.title=title;
		this.details=details;
		this.extra=extra;
		this.point=point;
		if (point==null)
			throw new RuntimeException("point is null");

		this.is_direct=is_direct;
	}
	void update(double distance,int when)
	{
		this.distance=distance;
		this.when=when;
	}
	/**
	 * @param mypos Position in merc13 coordinates.
	 * @param actualGs GS in knots
	 */
	public void updatemypos(Vector mypos, double actualGs) {
		if (is_direct)
		{
			double onenm=Project.approx_scale(point.plus(mypos).mul(0.5).gety(),13,1.0);
			this.distance=point.minus(mypos).length()/onenm; //in nautical miles.
			if (actualGs>1e-3)
				this.when=(int)(3600.0*distance/actualGs);
			else
				this.when=3600*9999;
		}
	}
	@Override
	public String getTitle()
	{
		return title;
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
}