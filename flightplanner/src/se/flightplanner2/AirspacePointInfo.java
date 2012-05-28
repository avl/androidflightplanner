package se.flightplanner2;

import java.util.ArrayList;
import java.util.Date;

import android.location.Location;

import se.flightplanner2.AirspaceLookup.AirspaceDetails;
import se.flightplanner2.GlobalGetElev.GetElevation;
import se.flightplanner2.Project.LatLon;
import se.flightplanner2.Project.Merc;
import se.flightplanner2.vector.Vector;

public class AirspacePointInfo implements InformationPanel
{
	private LatLon pos;
	private Vector point;
	private String[] details;
	private String[] extra;
	//private double distance;	
	private boolean hasextra;
	private Place[] extended;
	private Location lastloc;
	private int elev;
	
	/**
	 * 
	 * @param is_direct True if this is a simple waypoint, which is the next in the trip, and 
	 * we want the ETA calculated naively from the current actual groundspeed, using a direct track..
	 */
	public AirspacePointInfo(final LatLon about,AirspaceLookup lookup,long marker_size13)
	{
		ArrayList<String> details2 = new ArrayList<String>(); 
		ArrayList<String> extra2 = new ArrayList<String>();
		pos=about;
		point=Project.latlon2mercvec(about,13);
		double pixels=Project.approx_scale(point.y, 13, 1.0);
		AirspaceDetails extras=lookup.get_airspace_details(pixels,marker_size13,
				point,details2,extra2);
		hasextra=extras.hasextra;
		
		
		Place[] places=new Place[extras.extended_icaos.length];
		for(int i=0;i<places.length;++i)
			places[i]=new AirportPlace(extras.extended_icaos[i],lookup);
		
		extended=places;
		if (extended.length==0)
		{
			final String human="Selected Point";
			//final DetailedPlace dp=new NakedDetailedPlace(human, about);			
			extended=new Place[]{
					new Place()
					{
						@Override
						public SigPoint getAerodrome() {
							return null;
						}
						@Override
						public String getHumanName() {
							return human;
						}
						@Override
						public DetailedPlace getDetailedPlace() {						
							return null;
						}
						@Override
						public LatLon getLatLon() {
							return about;
						}
					}
			};
		}
		details=details2.toArray(new String[details2.size()]);
		extra=extra2.toArray(new String[extra2.size()]);
		
		GetElevation gete=GlobalGetElev.get_elev;
		if (gete!=null)
		{
			elev=gete.get_elev_ft(about,13,Math.max((int)marker_size13/2,1));
		}		
	}
	
	/**
	 * @param mypos Position in merc13 coordinates.
	 * @param actualGs GS in knots
	 */
	@Override
	public void updatemypos(Location location) {
		lastloc=location;
		/*
		this.distance=Project.exacter_distance(
				Project.merc2latlon(new Merc(mypos), 13),
				Project.merc2latlon(new Merc(point), 13));
		if (actualGs>1e-3)
			this.when=(int)(3600.0*distance/actualGs);
		else
			this.when=3600*9999;
		*/
	}
	@Override
	public String getPointTitle()
	{
		return "Elev "+elev+"ft";
	}
	@Override
	public String getLegTitle()
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
		if (lastloc==null) return -1;
		double distance=Project.exacter_distance(
				new LatLon(lastloc),
				pos);
		return distance;
	}
	@Override
	public double getHeading() {
		return Project.bearing(
				new LatLon(lastloc),
				pos);
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

	@Override
	public Place[] getHasExtendedInfo() {		
		return extended;
	}

	@Override
	public boolean getSkipped() {
		return false;
	}

	@Override
	public Date getPassed() {
		return null;
	}

	@Override
	public Date getETA2() {
		LatLon pointloc=pos;
		if (lastloc==null)
			return null;
		float actual_gs=lastloc.getSpeed()*3.6f/1.852f;
		if (actual_gs<1.0)
			return null;
		float distance=(float)Project.exacter_distance(pointloc,
				new LatLon(lastloc));
		float deltah=distance/actual_gs;
		return new Date(new Date().getTime()+(long)(deltah*3600l*1000l));
	}

	@Override
	public boolean getEmpty() {
		return false;
	}


}