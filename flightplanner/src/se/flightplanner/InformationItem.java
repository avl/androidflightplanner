package se.flightplanner;

import se.flightplanner.vector.Vector;

public class InformationItem
{
	private String title;
	private Vector point;
	private String[] details;
	private String[] extra;
	private double distance;
	private String kind;
	private int when;
	public InformationItem(String kind,
			String title,String[] details,String[] extra,
			Vector point,Vector pos_now,double speed)
	{
		this.kind=kind;
		this.title=title;
		this.details=details;
		this.extra=extra;
		setPoint(point,pos_now,speed);
	}
	public String getKind()
	{
		return kind;
	}
	public InformationItem(String kind,
			String title,String[] details,String[] extra,Vector point)
	{
		this.kind=kind;
		this.title=title;
		this.details=details;
		this.extra=extra;
		this.point=point;
	}
	void update(double distance,int when)
	{
		this.distance=distance;
		this.when=when;
	}
	public void updatemypos(Vector mypos, double actualGs) {
		double onenm=Project.approx_scale(point.plus(mypos).mul(0.5).gety(),13,1.0);
		this.distance=point.minus(mypos).length()/onenm;
		if (actualGs>1e-3)
			this.when=(int)(3600.0*distance/actualGs);
		else
			this.when=3600*9999;			
	}
	private void setPoint(Vector point,Vector pos_now,double speed)
	{
		this.point=point;
		updatemypos(pos_now,speed);
	}
	public String getTitle()
	{
		return title;
	}
	public String[] getDetails()
	{
		return details;
	}
	public String[] getExtraDetails()
	{
		return extra;
	}
	public Vector getPoint() {
		return point;
	}
	public double getDistance() {
		return distance;
	}
	public int getWhen() {
		return when;
	}
}