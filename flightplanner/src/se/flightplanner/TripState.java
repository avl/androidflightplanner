package se.flightplanner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

import se.flightplanner.Project.Merc;
import se.flightplanner.vector.BoundingBox;
import se.flightplanner.vector.Line;
import se.flightplanner.vector.Vector;
import se.flightplanner.vector.Polygon.InsideResult;
import android.location.Location;
import android.text.format.Time;
import android.util.Log;

public class TripState {

	private TripData tripdata;
	private AirspaceLookup lookup;
	/**
	 * The waypoint which is our current target.
	 * Starts out as waypoint 1, (the second one in the list),
	 * except if there is only one waypoint, when the target will
	 * always be 0.
	 */
	private int target_wp;  
	private int current_warning;
	static final double corridor_width=2.0; //nominal width of corridor of flight
	static final double lookahead_length=25.0; //how far ahead to look for zones etc.
	TripState(TripData trip,AirspaceLookup plookup)
	{
		lookup=plookup;
		tripdata=trip;
		current_warning=-1;
		target_wp=0;
		warnings=new ArrayList<WarningEvent>();
		/*
		for(int i=0;i<tripdata.waypoints.size();++i)
		{
			Merc m1=Project.latlon2merc(tripdata.waypoints.get(i).latlon,13);
			Merc m2=Project.latlon2merc(tripdata.waypoints.get((i+1)%tripdata.waypoints.size()).latlon,13);
			Vector mv1=new Vector(m1.x,m1.y);
			Vector mv2=new Vector(m2.x,m2.y);
			double nm=Project.approx_scale((mv1.gety()+mv2.gety())*0.5,13,1.5*corridor_width);
			Line line=new Line(mv1,mv2);
			BoundingBox bb=line.boundingBox().expand(nm);
		}*/
	}
	ArrayList<WarningEvent> warnings;
	
	static public interface WarningEvent
	{
		public int getWhen();
		public String getTitle();
		public String[] getDetails();
		public double getDistance();
		public boolean isApproxSamePos(Vector v,double epsilon);
		public void updateArea(int when, double distnm);
	}
	static public class AirspaceWarning implements WarningEvent
	{
		Vector point;
		int when;
		double distance;
		String title;
		String[] details;
		public double getDistance()
		{
			return distance;
		}

		public AirspaceWarning(Vector ppoint,int pwhen,double pdistance,String ptitle,String[] pdetails)
		{
			point=ppoint;
			when=pwhen;
			distance=pdistance;
			title=ptitle;
			details=pdetails;
		}
		public String[] getDetails() {
			return details;
		}
		public String getTitle() {
			return title;
		}
		public int getWhen() {
			return when;
		}
		public boolean isApproxSamePos(Vector v,double epsilon) {
			return v.minus(point).taxinorm()<=epsilon;
		}
		public void updateArea(int pwhen,double distnm) {
			when=pwhen;
			distance=distnm;
		}
	}
	
	
	/**
	 * Determines the most plausible current target.
	 * Considers the current 'target_wp' (not selecting
	 * previously passed targets, if multiple targets are plausible).
	 * It never goes to an already visited target, since that could
	 * potentially send us flying in a loop until we run out of gas ... :-)
	 */
	public void update_target(Location mylocation)
	{
		
		Vector heading=Project.heading2vector(mylocation.getBearing());
		//Log.i("fplan","Heading: "+heading);
		final Vector mypos=Project.latlon2mercvec(mylocation.getLatitude(),mylocation.getLongitude(),13);					
		double nm=Project.approx_scale(mypos.gety(),13,corridor_width);
		double onenm=Project.approx_scale(mypos.gety(),13,1.0);
		double nmfuture=onenm*lookahead_length;
		double abit=0.01*onenm;
		
		Vector futureend=mypos.plus(heading.mul(nmfuture));
		Line future=new Line(mypos,futureend);
		int found_warnings=0;
		ArrayList<Vector> points=new ArrayList<Vector>();
		for(AirspaceArea area:lookup.areas.get_areas(future.boundingBox()))
		{
			for(Line line:area.poly.getLines())
			{
				Vector point=Line.intersect(future, line);
				//Log.i("fplan","Area "+area.name+" isect "+point);
				if (point!=null)
					points.add(point);
			}
		}
		Collections.sort(points,new Comparator<Vector>() {
			public int compare(Vector object1, Vector object2) {
				double dista=object1.minus(mypos).taxinorm();
				double distb=object2.minus(mypos).taxinorm();
				if (dista<distb) return -1;
				if (dista>distb) return +1;
				return 0;
			}			
		});
		
		{
			if (warnings==null || warnings.size()<=0)
			{
				warnings=new ArrayList<WarningEvent>();
				warnings.add(null);
			}
			ArrayList<String> details = get_airspace_details(abit,
					mypos);			
			if (details.size()==0)
			{
				details.add("0 ft-FL 095: Uncontrolled Airspace");
			}
			warnings.set(0,
				new AirspaceWarning(mypos,0,0,
						"Current Position",
						details.toArray(new String[details.size()])));
			found_warnings+=1;			
		}

		int idx=1;
		
		Vector prevpoint=null;
		for(Vector point : points)
		{
			if (prevpoint!=null)
			{
				if (prevpoint.minus(point).taxinorm()<abit)
					continue;
			}
			prevpoint=point;
			//Log.i("fplan","Considering area "+area.name+" point "+point);
			if (point!=null)
			{
				
				double distnm=point.minus(mypos).length()/onenm;
				double speed=mylocation.getSpeed()*3.6/1.852;
				int when;
				if (speed<1.0)
				{
					when=0;
				}
				else
				{
					when=(int)((3600.0*distnm)/speed);//time in seconds						
				}
				if (warnings.size()>idx && warnings.get(idx).isApproxSamePos(point,abit))
				{
					warnings.get(idx).updateArea(when,distnm);						
					found_warnings+=1;
				}
				else
				{
					Vector just_a_bit_in=point.plus(heading.mul(2.0*abit));
										
					ArrayList<String> details = get_airspace_details(abit,
							just_a_bit_in);
					
					if (current_warning!=-1 && current_warning>idx)
						current_warning=idx;
					while(warnings.size()>idx)
						warnings.remove(warnings.size()-1);
					warnings.add(
						new AirspaceWarning(point,when,distnm,
								"New Airspace",
								details.toArray(new String[details.size()])));
					found_warnings+=1;
				}
				
			}
			++idx;
		}
		while(warnings.size()>found_warnings)
			warnings.remove(warnings.size()-1);
		if (current_warning>=warnings.size())
			current_warning=-1;
		if (warnings.size()>0 && current_warning==-1)
			current_warning=0;
		
		if (tripdata!=null)
		{
			double best_points=0;
			int best_points_i=-1;
			for(int i=0;i<tripdata.waypoints.size();++i)
			{
				Merc m1=Project.latlon2merc(tripdata.waypoints.get(i).latlon,13);
				Merc m2=Project.latlon2merc(tripdata.waypoints.get((i+1)%tripdata.waypoints.size()).latlon,13);
				Vector mv1=new Vector(m1.x,m1.y);
				Vector mv2=new Vector(m2.x,m2.y);
				Vector dir=mv2.minus(mv1).normalized();
				Line line=new Line(mv1,mv2);
				double rightheading=dir.scalarprod(heading);
				double distance=line.distance(mypos);
				
				if (distance>nm)
					distance=nm;
				double tpoints=0;
				tpoints+=(nm-distance)/nm;
				tpoints+=rightheading;
				if (i==target_wp)
					tpoints+=0.1; //get an extra point for the current waypoint
				if (i==target_wp+1)
					tpoints+=0.2; //get even more extra points for the next waypoint.
				if (best_points<tpoints || best_points_i==-1)
				{
					best_points=tpoints;
					best_points_i=i;
				}
			}
			if (best_points_i!=-1)
				target_wp=best_points_i;
		}
		else
		{
			target_wp=-1;
		}
	
	
	}

	private ArrayList<String> get_airspace_details(double abit,
			Vector just_a_bit_in) {
		ArrayList<String> details=new ArrayList<String>();
		for(AirspaceArea inarea:lookup.areas.get_areas(BoundingBox.aroundpoint(just_a_bit_in, abit)))
		{
			
			InsideResult r=inarea.poly.inside(just_a_bit_in);
			//double cd=r.closest.minus(point).length();
			if (r.isinside) //our polygons are clockwise, because the Y-axis points down - this inverts the meaning of inside and outside
			{ //If _INSIDE_ polygon
				details.add(inarea.floor+"-"+inarea.ceiling+": "+inarea.name);
			}
		}
		return details;
	}

	public int get_target() {
		return target_wp;
	}
	
	public WarningEvent getCurrentWarning()
	{
		if (current_warning!=-1)
			return warnings.get(current_warning);
		return null;
	}

	public void left() {
		if (current_warning>=0)
			current_warning-=1;
	}

	public void right() {
		if (current_warning<warnings.size()-1)
			current_warning+=1;
	}
	
		
}
