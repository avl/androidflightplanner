package se.flightplanner;

import java.util.ArrayList;
import java.util.Date;

import se.flightplanner.Project.Merc;
import se.flightplanner.TripData.Waypoint;
import se.flightplanner.vector.Line;
import se.flightplanner.vector.Vector;
import android.location.Location;
import android.util.Log;

public class TripState implements InformationPanel {

	static private class WaypointInfo
	{
		public String title;
		public String[] details;
		public float distance;
		public long when;
	}
	private String[] extradummy;
	private TripData tripdata;
	private AirspaceLookup lookup;
	/**
	 * The waypoint which is our current target.
	 * Starts out as waypoint 1, (the second one in the list),
	 * except if there is only one waypoint, when the target will
	 * always be 0.
	 */
	private int target_wp;  
	/**
	 * The current waypoint selected by the user.
	 */
	private int current_waypoint_idx;
	/**
	 * One for each waypoint.
	 */
	private ArrayList<WaypointInfo> waypointEvents;
	static private final double corridor_width=2.0; //nominal width of corridor of flight
	TripState(TripData trip,AirspaceLookup plookup)
	{
		lookup=plookup;
		waypointEvents=new ArrayList<WaypointInfo>();
		tripdata=trip;
		current_waypoint_idx=0;
		target_wp=0;
		extradummy=new String[]{};
	}
	public int get_time_to_destination() {
		return time_to_destination;
	}
	public double get_distance_to_destination() {
		return distance_to_destination;
	}
	public double get_actual_gs() {
		return actual_gs;
	}
	private int time_to_destination;
	private double distance_to_destination;
	private double actual_gs;
	
	/**
	 * Determines the most plausible current target.
	 * Considers the current 'target_wp' (not selecting
	 * previously passed targets, if multiple targets are plausible).
	 * It never goes to an already visited target, since that could
	 * potentially send us flying in a loop until we run out of gas ... :-)
	 */
	public void update_target(Location mylocation)
	{		
		actual_gs=0.0;
		time_to_destination=0;
		distance_to_destination=0.0;
		Vector heading=Project.heading2vector(mylocation.getBearing());

		final Vector mypos=Project.latlon2mercvec(mylocation.getLatitude(),mylocation.getLongitude(),13);					
		double nm=Project.approx_scale(mypos.gety(),13,corridor_width);
		double onenm=Project.approx_scale(mypos.gety(),13,1.0);

		
		if (tripdata!=null)
		{
			if (waypointEvents.size()!=tripdata.waypoints.size())
			{
				waypointEvents.clear();
				for(int i=0;i<tripdata.waypoints.size();++i)
				{
					WaypointInfo info=new WaypointInfo();
					info.title=getTitleImpl(i);
					info.details=getDetailsImpl(i);
					waypointEvents.add(info);
					
				}
			}
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
				if (i+1==target_wp)
					tpoints+=0.125; //get an extra point for the current waypoint
				if (i+1==target_wp+1)
					tpoints+=0.1875; //get even more extra points for the next waypoint.
				//Log.i("fplan","Item "+wp.name+" head: "+rightheading+" dist: "+distance+" points: "+tpoints);
				if (tpoints>best_points || best_points_i==-1)
				{
					best_points=tpoints;
					best_points_i=i+1;
				}
			}
			if (target_wp!=best_points_i)
			{
				//Update target_wp
				int old_target=target_wp;
				if (old_target>=0 && old_target<waypointEvents.size())
				{
					Waypoint old_wp=tripdata.waypoints.get(old_target);
					WaypointInfo we=waypointEvents.get(old_target);
					Log.i("fplan.skip","Skip"+Project.latlon2mercvec(old_wp.latlon,13).minus(mypos).length()+
							" limit:"+corridor_width*onenm);
					if (Project.latlon2mercvec(old_wp.latlon,13).minus(mypos).length()<
							corridor_width*onenm)
					{
						we.when=new Date().getTime();					
					}
					else
					{
						we.when=0;
					}
				}
				if (current_waypoint_idx<=target_wp)
					current_waypoint_idx=best_points_i;
				target_wp=best_points_i;
			}
			//Log.i("fplan","New best target_wp: "+target_wp+" waypoints: "+tripdata.waypoints.size());
			double accum_time=0;
			double accum_distance=0;
			actual_gs=mylocation.getSpeed()*3.6/1.852;
			/*
			Waypoint prevwp=null;
			if (tripdata.waypoints.size()>=1)
			{
				prevwp=tripdata.waypoints.get(0);
				if (target_wp>0 && target_wp<=tripdata.waypoints.size())					
					prevwp=tripdata.waypoints.get(target_wp-1);
			}*/
			for(int i=0;i<target_wp;++i)
			{
				if (i>=tripdata.waypoints.size()) break;
				WaypointInfo we=waypointEvents.get(i);
				we.distance=-1;
			}
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
				double distance=accum_distance;
				double ttimesec;
				if (used_gs>1e-3)
					ttimesec=(3600.0*tdistance)/used_gs;
				else
					ttimesec=3600*9999;
				accum_time+=ttimesec;								
				//Log.i("fplan","accum_time:"+accum_time);
				int timesec=(int)accum_time;
				if (i>=waypointEvents.size())
					continue;
				WaypointInfo we=waypointEvents.get(i);
				we.distance=(float)distance;
				we.when=timesec;
			}
			time_to_destination=(int) accum_time;
			distance_to_destination=accum_distance;
		}
		else
		{
			waypointEvents=null;
			target_wp=0;
		}
		if (waypointEvents!=null)
		{
			if (current_waypoint_idx>=waypointEvents.size())
				current_waypoint_idx=current_waypoint_idx-1;
			if (current_waypoint_idx<0)
				current_waypoint_idx=0;
		}
		else
		{
			current_waypoint_idx=0;
			TODO: Fix browsing of waypoints.
		}

	}


	public int get_target() {
		return target_wp;
	}
	
	public boolean hasAnyWaypoints()
	{
		if (tripdata==null || waypointEvents==null) return false;
		if (waypointEvents.size()==0) return false;
		return true;
	}
	public boolean hasLeft()
	{
		if (waypointEvents==null || waypointEvents.size()<=1) return false;
		if (current_waypoint_idx>0) return true;
		return false;
	}
	public void left() {
		current_waypoint_idx-=1;
		if (current_waypoint_idx<0)
			current_waypoint_idx=0;
	}
	public boolean hasRight()
	{
		if (waypointEvents==null || waypointEvents.size()<=1) return false;
		if (current_waypoint_idx<waypointEvents.size()-1) return true;
		return false;
	}
	public void right() {
		if (waypointEvents==null || waypointEvents.size()<=1) return;
		current_waypoint_idx+=1;
		if (current_waypoint_idx>=waypointEvents.size())
			current_waypoint_idx=waypointEvents.size()-1;
	}
	
	public String getTitleImpl(int i) {
		if (tripdata==null || tripdata.waypoints.size()==0) 
			return "No Waypoints!";
		if (i>=tripdata.waypoints.size())
			return "Unknown";
		Waypoint wp=tripdata.waypoints.get(i);
		String ttitle="Unknown";
		if (i==0)
		{
			ttitle="Depart from "+wp.name;
		}
		else
		if (i==tripdata.waypoints.size()-1)
		{
			ttitle="Arrive "+wp.name;
		}
		else
		if (wp.lastsub!=0)
		{
			ttitle=wp.name;
		}
		else
		{
			ttitle="Enroute "+wp.name;					
		}		
		return ttitle;
	}
	
	public String[] getDetailsImpl(int i) {		
		if (tripdata==null || i>=tripdata.waypoints.size())
			return new String[]{};
		
		Waypoint nextwp=null;
		Waypoint wp=tripdata.waypoints.get(i);
		if (i+1<tripdata.waypoints.size()) nextwp=tripdata.waypoints.get(i+1);		
		String whatdesc;
		if (nextwp==null)
		{
			whatdesc="";
		}
		else
		{
			if (nextwp.what.equals("climb"))
			{
				whatdesc=String.format("Begin climb from %.0f ft to %.0f ft",nextwp.startalt,nextwp.endalt);
			}
			else
			if (nextwp.what.equals("descent"))
			{
				whatdesc=String.format("Begin descent from %.0f ft to %.0f ft",nextwp.startalt,nextwp.endalt);
			}
			else
			{
				if (wp.what.equals("cruise"))
					whatdesc=String.format("Level flight %.0f ft",nextwp.endalt);
				else
					whatdesc=String.format("Level-out at %.0f ft",nextwp.endalt);
			}
		}
		
		
		final String[] details;
		if (whatdesc.equals(""))
		{
			details=new String[]{};
		}
		else
		{
			details=new String[]{whatdesc};
		};
		return details;
	}
	@Override
	public String[] getExtraDetails() {

		return extradummy;
	}
	@Override
	public Vector getPoint() {
		int i=current_waypoint_idx;
		if (tripdata==null || i>=tripdata.waypoints.size())
			return null;;
		Waypoint wp=tripdata.waypoints.get(i);
		return Project.latlon2mercvec(wp.latlon, 13);
	}
	@Override
	public double getDistance() {
		int i=current_waypoint_idx;
		if (tripdata==null || i>=waypointEvents.size())
			return 0;
		return waypointEvents.get(i).distance;
	}
	@Override
	public long getWhen() {
		int i=current_waypoint_idx;
		if (tripdata==null || i>=waypointEvents.size())
			return 0;
		return waypointEvents.get(i).when;
	}
	@Override
	public String getTitle() {
		int i=current_waypoint_idx;
		if (tripdata==null || waypointEvents.size()==0)
			return "No Waypoints!";
		if (i>=waypointEvents.size())
			return "Unknown";
		if (i==target_wp)
			return "*"+waypointEvents.get(i).title;
		return waypointEvents.get(i).title;
	}
	@Override
	public String[] getDetails() {
		int i=current_waypoint_idx;
		if (tripdata==null || i>=waypointEvents.size())
			return extradummy;
		return waypointEvents.get(i).details;
	}
	@Override
	public void updatemypos(Vector latlon2mercvec, double d) {
		//Not needed, since TripState is updated through other means
		//(it has to be, since it needs to be updated regardless of
		//wether it is being shown as an InformationPanel or not)
		
	}
	
		
}
