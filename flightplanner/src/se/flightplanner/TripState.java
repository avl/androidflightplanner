package se.flightplanner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

import se.flightplanner.Project.Merc;
import se.flightplanner.TripData.Waypoint;
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
	public int get_time_to_destination() {
		return time_to_destination;
	}
	public double get_distance_to_destination() {
		return distance_to_destination;
	}
	public double get_expected_gs() {
		return expected_gs;
	}
	public double get_actual_gs() {
		return actual_gs;
	}
	private int time_to_destination;
	private double distance_to_destination;
	private double expected_gs;
	private double actual_gs;
	
	static public interface WarningEvent
	{
		public int getWhen();
		public String getTitle();
		public String[] getDetails();
		public double getDistance();
		public Vector getPoint();
		public boolean isApprox(WarningEvent warningEvent);
		public String[] getExtraDetails();
	}
	static public class AirspaceWarning implements WarningEvent
	{
		Vector point;
		int when;
		double distance;
		String title;
		String[] details;
		String[] extradetails;
		public double getDistance()
		{
			return distance;
		}
		public Vector getPoint()
		{
			return point;
		}
		public AirspaceWarning(Vector ppoint,int pwhen,double pdistance,String ptitle,String[] pdetails,String[] pextradetails)
		{
			point=ppoint;
			when=pwhen;
			distance=pdistance;
			title=ptitle;
			details=pdetails;
			extradetails=pextradetails;
		}
		public String[] getDetails() {
			return details;
		}
		public String[] getExtraDetails()
		{
		    return extradetails;
		}
		public String getTitle() {
			return title;
		}
		public int getWhen() {
			return when;
		}

		public boolean isApprox(WarningEvent warningEvent) {
			String[] odet=warningEvent.getDetails();
			if (details.length!=odet.length) return false;
			for(int i=0;i<details.length;++i)
			{
				if (!details[i].equals(odet[i])) return false;
			}
			return title.equals(warningEvent.getTitle());
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
		expected_gs=0.0;
		actual_gs=0.0;
		time_to_destination=0;
		distance_to_destination=0.0;
		Vector heading=Project.heading2vector(mylocation.getBearing());
		//Log.i("fplan","Heading: "+heading);
		final Vector mypos=Project.latlon2mercvec(mylocation.getLatitude(),mylocation.getLongitude(),13);					
		double nm=Project.approx_scale(mypos.gety(),13,corridor_width);
		double onenm=Project.approx_scale(mypos.gety(),13,1.0);
		double nmfuture=onenm*lookahead_length;
		double abit=0.01*onenm;
		
		Vector futureend=mypos.plus(heading.mul(nmfuture));
		Line future=new Line(mypos,futureend);
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
			warnings=new ArrayList<WarningEvent>();
			ArrayList<String> details = new ArrayList<String>(); 
			ArrayList<String> extradetails = new ArrayList<String>(); 
			get_airspace_details(abit,
					mypos,details,extradetails);			
			if (details.size()==0)
			{
				details.add("0 ft-FL 095: Uncontrolled Airspace");
				extradetails.add("0 ft-FL 095: Uncontrolled Airspace");
			}
			warnings.add(
				new AirspaceWarning(mypos,0,0,
						"Current Position",
						details.toArray(new String[details.size()]),
						extradetails.toArray(new String[extradetails.size()])
						));
		}

		
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
				Vector just_a_bit_in=point.plus(heading.mul(2.0*abit));
				ArrayList<String> details = new ArrayList<String>();
				ArrayList<String> extradetails = new ArrayList<String>();
				get_airspace_details(abit,
						just_a_bit_in,details,extradetails);
				
				warnings.add(
					new AirspaceWarning(point,when,distnm,
							"New Airspace",
							details.toArray(new String[details.size()]),
							extradetails.toArray(new String[extradetails.size()])
							));
				
			}
		}
		
		if (tripdata!=null)
		{
			double best_points=-1e30;
			int best_points_i=0;
			for(int i=0;i<tripdata.waypoints.size()-1;++i)
			{
				Waypoint wp=tripdata.waypoints.get(i);
				Merc m1=Project.latlon2merc(tripdata.waypoints.get(i).latlon,13);
				Merc m2=Project.latlon2merc(tripdata.waypoints.get(i+1).latlon,13);
				Vector mv1=new Vector(m1.x,m1.y);
				Vector mv2=new Vector(m2.x,m2.y);
				Vector dir=mv2.minus(mv1).normalized();
				Line line=new Line(mv1,mv2);
				double rightheading=dir.scalarprod(heading);
				double distance=line.distance(mypos);
				
				double tpoints=0;
				tpoints-=(distance)/nm;
				tpoints+=rightheading;
				if (i==target_wp)
					tpoints+=0.1; //get an extra point for the current waypoint
				if (i==target_wp+1)
					tpoints+=0.2; //get even more extra points for the next waypoint.
				Log.i("fplan","Item "+wp.name+" head: "+rightheading+" dist: "+distance+" points: "+tpoints);
				if (tpoints>best_points || best_points_i==-1)
				{
					best_points=tpoints;
					best_points_i=i+1;
				}
			}
			target_wp=best_points_i;
			Log.i("fplan","New best target_wp: "+target_wp+" waypoints: "+tripdata.waypoints.size());
			double accum_time=0;
			double accum_distance=0;
			if (tripdata.waypoints.size()>0)
			{
				Waypoint cur;
				if (target_wp+1<tripdata.waypoints.size())
					cur=tripdata.waypoints.get(target_wp+1);
				else
					cur=tripdata.waypoints.get(0);
				expected_gs=cur.gs;
				actual_gs=mylocation.getSpeed()*3.6/1.852;
			}
			/*
			Waypoint prevwp=null;
			if (tripdata.waypoints.size()>=1)
			{
				prevwp=tripdata.waypoints.get(0);
				if (target_wp>0 && target_wp<=tripdata.waypoints.size())					
					prevwp=tripdata.waypoints.get(target_wp-1);
			}*/
			for(int i=target_wp;i<tripdata.waypoints.size();++i)
			{
				final Waypoint wp=tripdata.waypoints.get(i);
				Merc m1=Project.latlon2merc(wp.latlon,13);
				final Vector mv1=new Vector(m1.x,m1.y);				
				double tdistance=0;
				double used_gs;
				if (i==target_wp)
				{
					tdistance=mypos.minus(mv1).length()/onenm;
					used_gs=actual_gs;
				}
				else
				{
					tdistance=wp.d;
					used_gs=wp.gs;
				}
				accum_distance+=tdistance;
				final double distance=accum_distance;
				double ttimesec;
				if (used_gs>1e-3)
					ttimesec=(3600.0*tdistance)/used_gs;
				else
					ttimesec=3600*9999;
				accum_time+=ttimesec;								
				Log.i("fplan","accum_time:"+accum_time);
				final int timesec=(int)accum_time;
				String whatdesc="";
				String ttitle="Unknown";
				Waypoint nextwp=tripdata.waypoints.get(tripdata.waypoints.size()-1);
				if (i+1<tripdata.waypoints.size()) nextwp=tripdata.waypoints.get(i+1);

				if (wp.lastsub!=0)
				{
					ttitle=wp.name;
					whatdesc=String.format("%.0f ft",wp.endalt);
				}
				else
				{
					if (wp.what.equals("climb"))
					{
						ttitle="Climb complete";
						whatdesc="Altitude "+String.format("%.0f ft",wp.endalt);
					}
					if (wp.what.equals("cruise"))
					{
						Log.i("fplan","Nextwp:"+nextwp.what);
						if (nextwp.what.equals("climb"))
						{
							ttitle="Start climb";
							whatdesc="To "+nextwp.name+" "+String.format("%.0f ft",nextwp.endalt);
						}
						else
						if (nextwp.what.equals("descent"))
						{
							ttitle="Start descent";
							whatdesc="To "+nextwp.name+" "+String.format("%.0f ft",nextwp.endalt);
						}
					}
					if (wp.what.equals("descent"))
					{
						ttitle="Descent complete";
						whatdesc="Altitude "+String.format("%.0f ft",wp.endalt);
					}
				}
				
			
				
				final String title=ttitle;

				final String[] details=new String[]{
						whatdesc
				};
				warnings.add(new WarningEvent()
				{
					public String[] getDetails() {
						return details;
					}
					public double getDistance() {
						return distance;
					}
					public String getTitle() {
						return title;
					}
					public int getWhen() {
						return timesec;
					}
					public boolean isApprox(WarningEvent warningEvent) {
						String[] odet=warningEvent.getDetails();
						if (odet.length!=details.length) return false;
						for(int i=0;i<details.length;++i)
							if (!odet[i].equals(details[i])) return false;
						return wp.name.equals(warningEvent.getTitle());
					}
					public Vector getPoint() {
						// TODO Auto-generated method stub
						return mv1;
					}
					public String[] getExtraDetails() {
						return details;
					}
				});				
			}
			time_to_destination=(int) accum_time;
			distance_to_destination=accum_distance;
		}
		else
		{
			target_wp=-1;
		}
		if (warnings!=null)
			Collections.sort(warnings,new Comparator<WarningEvent>()
					{
						public int compare(WarningEvent arg0, WarningEvent arg1) {
							double dist0=arg0.getPoint().minus(mypos).length();
							double dist1=arg1.getPoint().minus(mypos).length();
							if (dist0<dist1) return -1;
							if (dist0>dist1) return 1;
							return 0;
						}
				
					});
		autoselect_warnings();
	
	}

	ArrayList<WarningEvent> warnings_seen;
	private void autoselect_warnings() {
		if (current_warning>=warnings.size() || current_warning<0)
			current_warning=-1;
		
		if (current_warning>0)
		{
			if (warnings_seen!=null)
			{
				for(int i=0;i<current_warning;++i)
				{
					if (i>=warnings_seen.size() || !warnings_seen.get(i).isApprox(warnings.get(i)))
					{
						if (i<warnings_seen.size())
							Log.i("fplan","Warning "+warnings_seen.get(i).getDetails()+" == " +
								warnings.get(i).getDetails()+" => "+warnings_seen.get(i).isApprox(warnings.get(i)));
						warnings_seen=null;
						current_warning=i;
						break;
					}
				}
			}
			if (warnings_seen==null)
			{
				warnings_seen=new ArrayList<WarningEvent>();
				for(int i=0;i<warnings.size();++i)
				{
					warnings_seen.add(warnings.get(i));
				}
				
			}
		}
		
		Log.i("fplan","Autoselect warnings:"+current_warning);
	}

	private void get_airspace_details(double abit,
			Vector just_a_bit_in,ArrayList<String> details,ArrayList<String> extradetails) {
		for(AirspaceArea inarea:lookup.areas.get_areas(BoundingBox.aroundpoint(just_a_bit_in, abit)))
		{
			
			InsideResult r=inarea.poly.inside(just_a_bit_in);
			//double cd=r.closest.minus(point).length();
			if (r.isinside) //our polygons are clockwise, because the Y-axis points down - this inverts the meaning of inside and outside
			{ //If _INSIDE_ polygon
				String det=inarea.floor+"-"+inarea.ceiling+": "+inarea.name;
				details.add(det);
				extradetails.add(det);
				for(String fre : inarea.freqs)
				{
					if (fre.length()>0)
					{
						Log.i("fplan","Adding airspace detail "+fre);
						extradetails.add(fre);
					}
				}
			}
		}
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
