package se.flightplanner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

import se.flightplanner.Project.LatLon;
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
	private int current_warning_idx;
	private InformationItem current_warning_obj;
	ArrayList<InformationItem> wp_warnings;
	static final double corridor_width=2.0; //nominal width of corridor of flight
	static final double lookahead_length=25.0; //how far ahead to look for zones etc.
	TripState(TripData trip,AirspaceLookup plookup)
	{
		lookup=plookup;
		wp_warnings=new ArrayList<InformationItem>();
		tripdata=trip;
		current_warning_idx=-1;
		current_warning_obj=null;
		target_wp=0;
		warnings=new ArrayList<InformationItem>();
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
	ArrayList<InformationItem> warnings;
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
		if (lookup!=null)
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
		
		warnings=new ArrayList<InformationItem>();

		
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
					new InformationItem("fixed","New Airspace",
							details.toArray(new String[details.size()]),
							extradetails.toArray(new String[extradetails.size()]),
							point,mypos,speed
							));
				
			}
		}
		
		if (tripdata!=null)
		{
			if (wp_warnings.size()!=tripdata.waypoints.size())
			{
				wp_warnings.clear();
				
				for(int i=0;i<tripdata.waypoints.size();++i)
				{
					Waypoint wp=tripdata.waypoints.get(i);
					String whatdesc="";
					String ttitle="Unknown";
					LatLon latlon=tripdata.waypoints.get(i).latlon;
					Vector m1=Project.latlon2mercvec(latlon,13);
					Waypoint nextwp=null;
					if (i+1<tripdata.waypoints.size()) nextwp=tripdata.waypoints.get(i+1);
					/*Waypoint prevwp=tripdata.waypoints.get(0);
					if (i>0) prevwp=tripdata.waypoints.get(i-1);
					*/
					
					if (i==0)
					{
						ttitle="Depart for "+wp.name;
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
					
				
					
					final String title=ttitle;

					final String[] details;
					if (whatdesc.equals(""))
					{
						details=new String[]{};
					}
					else
					{
						details=new String[]{whatdesc};
					};
					
					wp_warnings.add(new InformationItem("trip",
							title,details,details,m1
							));
					
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
			target_wp=best_points_i;
			//Log.i("fplan","New best target_wp: "+target_wp+" waypoints: "+tripdata.waypoints.size());
			double accum_time=0;
			double accum_distance=0;
			if (tripdata.waypoints.size()>0)
			{
				Waypoint cur;
				if (target_wp+1<tripdata.waypoints.size())
					cur=tripdata.waypoints.get(target_wp+1);
				else
					cur=tripdata.waypoints.get(tripdata.waypoints.size()-1);
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
				double distance=accum_distance;
				double ttimesec;
				if (used_gs>1e-3)
					ttimesec=(3600.0*tdistance)/used_gs;
				else
					ttimesec=3600*9999;
				accum_time+=ttimesec;								
				//Log.i("fplan","accum_time:"+accum_time);
				int timesec=(int)accum_time;
				if (i>=wp_warnings.size())
					continue;
				InformationItem we=wp_warnings.get(i);
				we.update(distance,timesec);
				warnings.add(we);
			}
			time_to_destination=(int) accum_time;
			distance_to_destination=accum_distance;
		}
		else
		{
			target_wp=-1;
		}

		if (warnings!=null)
		{
			ArrayList<String> details = new ArrayList<String>(); 
			ArrayList<String> extradetails = new ArrayList<String>(); 
			get_airspace_details(abit,
					mypos,details,extradetails);			
			//Log.i("fplan","Actual GS for Current Position: "+actual_gs);
			InformationItem cp=new InformationItem("curpos","Current Position",
					details.toArray(new String[details.size()]),
					extradetails.toArray(new String[extradetails.size()]),
					mypos,
					mypos,actual_gs);
			warnings.add(cp);
			if (current_warning_obj!=null && current_warning_obj.getKind().equals("curpos"))
				current_warning_obj=cp;
		}
		
		if (warnings!=null)
		{
			Collections.sort(warnings,new Comparator<InformationItem>()
					{
						public int compare(InformationItem arg0, InformationItem arg1) {
							double dist0=arg0.getDistance();
							double dist1=arg1.getDistance();
							if (dist0<dist1) return -1;
							if (dist0>dist1) return 1;
							return 0;
						}
				
					});
			if (current_warning_obj!=null)
			{
				double curr_warn_dist=current_warning_obj.getDistance();
				//Log.i("fplan","Curr_warn_dist:"+curr_warn_dist);
				for(int i=0;i<warnings.size();++i)
				{
					double dist=warnings.get(i).getDistance();
					//Log.i("fplan","Warning "+i+" has dist: "+dist);
					if (dist>=curr_warn_dist-1e-3)
					{
						if (dist>curr_warn_dist+0.5 && i!=0)						
							current_warning_idx=i-1;
						else
							current_warning_idx=i;
						break;
					}
				}
			}
		}
		if (current_warning_obj!=null && !current_warning_obj.getKind().equals("trip"))
		{
			current_warning_obj.updatemypos(mypos, actual_gs);
		}
		autoselect_warnings();
	
	}

	ArrayList<InformationItem> warnings_seen;
	private void autoselect_warnings() {
		if (current_warning_idx>=warnings.size())
			current_warning_idx=warnings.size()-1;
		if (current_warning_idx<0)
			current_warning_idx=0;
				
		//Log.i("fplan","Autoselect warnings:"+current_warning_idx);
	}

	void showInfo(LatLon about,LatLon mypos)
	{
		ArrayList<String> details = new ArrayList<String>(); 
		ArrayList<String> extradetails = new ArrayList<String>();
		Vector point=Project.latlon2mercvec(about,13);
		get_airspace_details(1.0,
				point,details,extradetails);			
		//Log.i("fplan","Actual GS for Current Position: "+actual_gs);
		InformationItem cp=new InformationItem("fixed","Airspace",
				details.toArray(new String[details.size()]),
				extradetails.toArray(new String[extradetails.size()]),
				point,
				Project.latlon2mercvec(mypos,13),actual_gs);
		current_warning_obj=cp;
		current_warning_idx=0;
	}
	private void get_airspace_details(double abit,
			Vector just_a_bit_in,ArrayList<String> details,ArrayList<String> extradetails) {
		if (lookup!=null)
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
							//Log.i("fplan","Adding airspace detail "+fre);
							extradetails.add(fre);
						}
					}
				}
			}
		if (details.size()==0)
		{
			details.add("0 ft-FL 095: Uncontrolled Airspace");
			extradetails.add("0 ft-FL 095: Uncontrolled Airspace");
		}
		
	}

	public int get_target() {
		return target_wp;
	}
	
	public InformationItem getCurrentWarning()
	{
		return current_warning_obj;
	}

	public boolean hasLeft()
	{
		if (warnings.size()<=1) return false;
		if (current_warning_idx>0) return true;
		return false;
	}
	public void left() {
		current_warning_idx-=1;
		if (current_warning_idx<=-1)
			current_warning_idx=-1;
		if (current_warning_idx>=0)
			current_warning_obj=warnings.get(current_warning_idx);
		else
			current_warning_obj=null;
	}
	public boolean hasRight()
	{
		if (warnings.size()<=1) return false;
		if (current_warning_idx<warnings.size()-1) return true;
		return false;
	}
	public void right() {
		current_warning_idx+=1;
		if (current_warning_idx>=warnings.size())
			current_warning_idx=warnings.size()-1;
		if (current_warning_idx>=0)
			current_warning_obj=warnings.get(current_warning_idx);
		else
			current_warning_obj=null;
	}
	
		
}
