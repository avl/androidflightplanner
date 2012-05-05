package se.flightplanner2;

import java.security.acl.LastOwnerException;
import java.util.ArrayList;
import java.util.Date;

import se.flightplanner2.Project.LatLon;
import se.flightplanner2.Project.Merc;
import se.flightplanner2.TripData.Waypoint;
import se.flightplanner2.vector.Line;
import se.flightplanner2.vector.Vector;
import android.location.Location;
import android.util.Log;

public class TripState implements InformationPanel {

	static private class WaypointInfo
	{
		public String point_title;
		public String leg_title;
		public String[] details;
		public float distance;
		public boolean skipped;
		public Date passed;
		public Date eta2;
		
	}
	private Location last_location;
	private LatLon lastpos;
	private String[] extradummy;
	private TripData tripdata;
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
	TripState(TripData trip)
	{
		waypointEvents=new ArrayList<WaypointInfo>();
		tripdata=trip;
		current_waypoint_idx=0;
		target_wp=0;
		extradummy=new String[]{};
	}
	public int get_time_to_destination() {
		return time_to_destination;
	}
	/*public double get_distance_to_destination() {
		return distance_to_destination;
	}*/
	public double get_actual_gs() {
		return actual_gs;
	}
	private int time_to_destination;
	//private double distance_to_destination;
	private double actual_gs;
	
	/**
	 * Determines the most plausible current target.
	 * Considers the current 'target_wp' (not selecting
	 * previously passed targets, if multiple targets are plausible).
	 * It never goes to an already visited target, since that could
	 * potentially send us flying in a loop until we run out of gas ... :-)
	 */
	@Override
	public void updatemypos(Location mylocation) {
		if (mylocation==null)
			return;
		last_location=mylocation;
		lastpos=new LatLon(last_location);
		///update_target(l);		
		actual_gs=mylocation.getSpeed()*3.6/1.852;
				
		time_to_destination=0;
		//distance_to_destination=0.0;
		Vector heading=Project.heading2vector(mylocation.getBearing());

		LatLon myposlatlon=new LatLon(mylocation.getLatitude(),mylocation.getLongitude());
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
					info.point_title=getPointTitleImpl(i);
					info.leg_title=getLegTitleImpl(i);
					info.details=getDetailsImpl(i);
					waypointEvents.add(info);
					
				}
			}
			double best_points=-1e30;
			int best_points_i=0;
			boolean skipped_landing=false;
			for(int i=0;i<tripdata.waypoints.size()-1;++i)
			{
				//Waypoint wp=tripdata.waypoints.get(i);
				Merc m1=Project.latlon2merc(tripdata.waypoints.get(i).latlon,13);
				Merc m2=Project.latlon2merc(tripdata.waypoints.get(i+1).latlon,13);
				Vector mv1=new Vector(m1.x,m1.y);
				Vector mv2=new Vector(m2.x,m2.y);
				Vector dir=mv2.minus(mv1).normalized();
				Line line=new Line(mv1,mv2);
				double rightheading=dir.scalarprod(heading);
				if (rightheading<0)
					rightheading*=10; //big penalty in going backwards
				double distance=line.distance(mypos);
				
				if (i+1>=target_wp)
				{
					Waypoint wp=tripdata.waypoints.get(i+1);
					WaypointInfo we=waypointEvents.get(i+1);
					if (wp.land_at_end && we.passed==null)
						skipped_landing=true;						
				}
				
				double tpoints=0;
				tpoints-=(distance)/nm;
				tpoints+=rightheading;
				if (i+1>target_wp && skipped_landing)
				{
					tpoints-=5; //Gravely penalize not landing 
				}
					
				if (i+1==target_wp)
				{
					//Log.i("fplan","rightheading bonus:"+rightheading+" distance penalty: "+distance/nm);
					tpoints+=0.125; //get an extra point for the current waypoint
				}
				if (i+1==target_wp+1)
					tpoints+=0.1875; //get even more extra points for the next waypoint.
				if (i+1<target_wp)
					tpoints-=0.1;//small penalty for going back
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
						if (old_wp.land_at_end)
						{
							if (actual_gs<20)
							{
								we.eta2=null;
								we.passed=new Date();
								we.skipped=false;
							}							
						}
						else
						{
							we.eta2=null;
							we.passed=new Date();
							we.skipped=false;							
						}
					}
					else
					{
						we.skipped=true;
						we.eta2=null;
						we.passed=null;
					}
				}
				if (current_waypoint_idx<=target_wp)
					current_waypoint_idx=best_points_i;
				target_wp=best_points_i;
			}
			//Log.i("fplan","New best target_wp: "+target_wp+" waypoints: "+tripdata.waypoints.size());
			double accum_time=0;
			double accum_time_to_destination=0;
			boolean landed=false;
			double accum_distance=0;
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
				double tdistance=0;
				double used_gs;
				if (i==target_wp)
				{
					tdistance=Project.exacter_distance(myposlatlon,wp.latlon);
					used_gs=wp.gs;//actual_gs;
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
				if (!landed)
					accum_time_to_destination+=ttimesec;
				if (wp.land_at_end)
					landed=true;
				//Log.i("fplan","accum_time:"+accum_time);
				int timesec=(int)accum_time;
				if (i>=waypointEvents.size())
					continue;
				WaypointInfo we=waypointEvents.get(i);
				we.distance=(float)distance;
				long now=new Date().getTime();
				we.eta2=new Date(now+(long)timesec*1000);
				we.skipped=false;
			}
			time_to_destination=(int)(accum_time_to_destination+0.5);
			//distance_to_destination=accum_distance;
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
			if (current_waypoint_idx<-1)
				current_waypoint_idx=-1;
		}
		else
		{
			current_waypoint_idx=0;
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
		if (current_waypoint_idx>0 || (current_waypoint_idx==-1 && target_wp>0)) return true;
		return false;
	}
	public void left() {
		if (current_waypoint_idx==-1 && target_wp>0)
			current_waypoint_idx=target_wp-1;
		else if (current_waypoint_idx==target_wp)
			current_waypoint_idx=-1;
		else if (current_waypoint_idx>0)
			current_waypoint_idx-=1;			
	}
	public boolean hasRight()
	{
		if (waypointEvents==null || waypointEvents.size()<=1) return false;
		if (current_waypoint_idx<waypointEvents.size()-1) return true;
		return false;
	}
	public void right() {
		if (waypointEvents==null || waypointEvents.size()<=1) return;
		if (current_waypoint_idx==-1)
			current_waypoint_idx=target_wp;
		else
		if (current_waypoint_idx==target_wp-1)
			current_waypoint_idx=-1;
		else
			current_waypoint_idx+=1;
		
		if (current_waypoint_idx>=waypointEvents.size())
			current_waypoint_idx=waypointEvents.size()-1;
		
	}
	
	public String getPointTitleImpl(int i) {
		if (tripdata==null || tripdata.waypoints.size()==0) 
			return "No Waypoints!";
		if (i>=tripdata.waypoints.size())
			return "Unknown";
		if (i==-1)
			return "Your Position";
		Waypoint wp=tripdata.waypoints.get(i);
		if (i==0)
			return wp.name;
		Waypoint next=null;
		if (i<tripdata.waypoints.size()-1)
			next=tripdata.waypoints.get(i+1);		
		if (next==null)
		{
			return wp.name;
		}
		else
		if (wp.lastsub!=0)
		{
			return wp.name;
		}
		else
		{
			if (wp.what.equals("descent"))
				return "BOD "+wp.name;
			else if (wp.what.equals("climb"))
				return "TOC "+wp.name;
			else
			{
				if (next.what.equals("descent"))
					return "TOD "+wp.name;
				else if (next.what.equals("climb"))
					return "BOC "+wp.name;
				else
					return "Enroute "+wp.name;
			}			
		}
	}
	public String getLegTitleImpl(int i) {
		if (tripdata==null || tripdata.waypoints.size()==0) 
			return "";
		if (i>=tripdata.waypoints.size())
			return "";
		Waypoint wp=tripdata.waypoints.get(i);
		if (i==-1)
			return "-";
		if (i<=0)
			return "Start of journey";
		Waypoint prev=tripdata.waypoints.get(i-1);
		Waypoint next=null;
		if (i<tripdata.waypoints.size()-1)
			next=tripdata.waypoints.get(i+1);
		boolean prevlanding=prev==null || prev.land_at_end;
		boolean nowlanding=wp.land_at_end || i==tripdata.waypoints.size()-1;
		if (wp.what.equals("climb"))
		{
			if (prevlanding) return "Takeoff & Climb to "+wp.altitude;
			if (wp.lastsub!=0)
				return "Climb to "+((next!=null) ? next.altitude : "?");
			return "Climb to "+wp.altitude;
			
		}
		else if (wp.what.equals("descent"))
		{
			if (nowlanding) return "Descend for landing";
			if (wp.lastsub!=0)
				return "Descend to "+((next!=null) ? next.altitude : "?");
			return "Descend to "+wp.altitude;			
		}
		else
		{ //cruise
			return "Cruise at "+wp.altitude;
		}
		
	}	
	
	public String[] getDetailsImpl(int i) {		
		if (tripdata==null || i>=tripdata.waypoints.size())
			return new String[]{};
		/*
		  
		 
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
		*/
		String whatdesc=getLegTitleImpl(i);
		
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
		if (i==-1)
			return Project.latlon2mercvec(lastpos, 13);
		Waypoint wp=tripdata.waypoints.get(i);
		return Project.latlon2mercvec(wp.latlon, 13);
	}
	@Override
	public double getDistance() {
		int i=current_waypoint_idx;
		if (tripdata==null || i>=waypointEvents.size())
			return 0;
		if (i==-1) return 0;
		return waypointEvents.get(i).distance;
	}
	@Override
	public Date getPassed() {
		int i=current_waypoint_idx;
		if (tripdata==null || i>=waypointEvents.size())
			return null;
		if (i==-1) return null;
		return waypointEvents.get(i).passed;
	}
	@Override
	public boolean getEmpty() {
		return waypointEvents==null || waypointEvents.size()==0;
	}
	
	@Override
	public boolean getSkipped() {
		int i=current_waypoint_idx;
		if (tripdata==null || i>=waypointEvents.size())
			return true;
		if (i==-1) return false;
		return waypointEvents.get(i).skipped;
	}

	protected Date getETA() {
		if (last_location==null) return null;
		if (tripdata==null || current_waypoint_idx>=tripdata.waypoints.size())
			return null;
		if (current_waypoint_idx==-1) return new Date();
		float actual_gs=last_location.getSpeed()*3.6f/1.852f;
		float d=(float) Project.exacter_distance(
				new LatLon(last_location), 
				tripdata.waypoints.get(current_waypoint_idx).latlon);
		if (actual_gs<1.0)
			return null;
		float deltah=d/actual_gs;
		long now=new Date().getTime();
		return new Date((long)(now+deltah*1000l*3600l));
	}
	
	@Override
	public Date getETA2() {
		int i=current_waypoint_idx;
		if (tripdata==null || i>=waypointEvents.size())
			return new Date();
		if (current_waypoint_idx==-1) return new Date();
		return waypointEvents.get(i).eta2;
	}
	@Override
	public String getPointTitle() {
		int i=current_waypoint_idx;
		if (tripdata==null || waypointEvents.size()==0)
			return "No Waypoints!";
		if (current_waypoint_idx==-1) return "Your Position";
		if (i>=waypointEvents.size())
			return "Unknown";
		if (i==target_wp)
			return "*"+waypointEvents.get(i).point_title;
		return waypointEvents.get(i).point_title;
	}
	@Override
	public String getLegTitle() {
		int i=current_waypoint_idx;
		if (tripdata==null || waypointEvents.size()==0)
			return "No Waypoints!";
		if (current_waypoint_idx==-1) return "-";
		if (i>=waypointEvents.size())
			return "Unknown";
		if (i==target_wp)
			return "*"+waypointEvents.get(i).leg_title;
		return waypointEvents.get(i).leg_title;
	}
	@Override
	public String[] getDetails() {
		int i=current_waypoint_idx;
		if (tripdata==null || i>=waypointEvents.size())
			return extradummy;
		if (i==-1) return new String[]{};
		return waypointEvents.get(i).details;
	}
	@Override
	public boolean getHasExtraInfo() {
		return false;
	}
	private Place place=new Place()
	{
		@Override
		public SigPoint getAerodrome() {
			return null;
		}
		@Override
		public String getHumanName() {
			int i=current_waypoint_idx;
			if (tripdata==null || i>=tripdata.waypoints.size())
				return "?";;
			if (i==-1)
				return "Your Position";
			Waypoint wp=tripdata.waypoints.get(i);
			return wp.name;
		}
		@Override
		public DetailedPlace getDetailedPlace() {
			return new DetailedPlace()
			{
				@Override
				public double getDistance() {
					return TripState.this.getDistance();
				}
				@Override
				public Date getETA2() {					
					return TripState.this.getETA2();
				}
				@Override
				public Date getETA() {
					return TripState.this.getETA();
				}
				@Override
				public void update_pos(Location location) {
					TripState.this.updatemypos(location);					
				}
				@Override
				public String getName() {
					return TripState.this.getPointTitle();
				}
				@Override
				public boolean hasPlanned() {
					return true;
				}
				@Override
				public Date getPlanned() {
					int i=current_waypoint_idx;
					if (tripdata==null || i>=tripdata.waypoints.size())
						return null;
					Log.i("fplan","Getplanned: "+i);
					if (i==-1)
					{
						if (target_wp>=tripdata.waypoints.size()) return null;
						Waypoint wp=tripdata.waypoints.get(target_wp);
						float distnm=(float)Project.exacter_distance(lastpos, wp.latlon);
						float gs=(float)wp.gs;
						if (gs<1) return null;
						long time=(long)((3600*distnm)/gs);						
						return new Date((wp.arrive_dt-time)*1000);						
					}
					Waypoint wp=tripdata.waypoints.get(i);
					return new Date(wp.arrive_dt*1000);
				}
				@Override
				public LatLon getPos() {
					int i=current_waypoint_idx;
					if (tripdata==null || i>=tripdata.waypoints.size())
						return null;
					if (i==-1) return lastpos;
					Waypoint wp=tripdata.waypoints.get(i);
					return wp.latlon;
				}
				@Override
				public Float getPlannedFuel() {
					int i=current_waypoint_idx;
					if (tripdata==null || i>=tripdata.waypoints.size())
						return null;
					if (i==-1)
					{
						if (tripdata.waypoints.size()==0) return null;
						if (target_wp==0)
						{
							return tripdata.waypoints.get(0).endfuel;
						}
						Waypoint wp=tripdata.waypoints.get(target_wp);
						float distnm=(float)Project.exacter_distance(lastpos, wp.latlon);
						
						float fuel1=tripdata.waypoints.get(target_wp-1).endfuel;
						float fuel2=wp.endfuel;
						float ratio_left=(wp.d>0.01) ? (float)(distnm/wp.d) : 0.0f;
						float fuel=ratio_left*fuel1+(1.0f-ratio_left)*fuel2;
						return fuel;
								
					}
					Waypoint wp=tripdata.waypoints.get(i);
					return wp.endfuel;
				}
				@Override
				public Float getPlannedGs() {
					int i=current_waypoint_idx;
					if (i==-1) i=target_wp;
					if (tripdata==null || i>=tripdata.waypoints.size())
						return null;
					Waypoint wp=tripdata.waypoints.get(i);
					return (float) wp.gs;
				}
				@Override
				public boolean hasPrevNext() {
					return true;
				}
				@Override
				public void prev() {
					left();					
				}
				@Override
				public void next() {
					right();					
				}
				@Override
				public boolean is_own_position() {
					return current_waypoint_idx==-1;
				}				
			};
		}
	};
	private Place[] places=new Place[]{place};
	@Override
	public Place[] getHasExtendedInfo() {	
		return places;
	}
	public void reupdate() {
		if (last_location!=null)
			updatemypos(last_location);		
	}
	
		
}
