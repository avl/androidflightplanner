package se.flightplanner2;

import java.io.Serializable;
import java.security.acl.LastOwnerException;
import java.util.ArrayList;
import java.util.Date;

import se.flightplanner2.Project.LatLon;
import se.flightplanner2.Project.Merc;
import se.flightplanner2.TripData.Waypoint;
import se.flightplanner2.vector.BoundingBox;
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
	static private final float corridor_width=2.0f; //nominal width of corridor of flight
	
	static private class EnrouteSigPoints
	{
		NextSigPoints nesp;
		SigPoint sp;
		public LatLon latlon;
		public float ratio;
		public int target_wp;
		public Date passed;
		public String name;
		public boolean landing;
		public String toString()
		{
			return "SP: "+name+" ratio: "+ratio+" target_wp "+target_wp+" passed: "+passed;
		}
	}
	private ArrayList<EnrouteSigPoints> enroute=new ArrayList<TripState.EnrouteSigPoints>();
	
	
	static public class NextSigPoints implements Serializable
	{
		public Date eta;
		public Date passed;
		public LatLon latlon;
		public String name;
	}
	
	/*!
	 * Based on time to cur wp, not on position
	 */
	public float cur_wp_along()
	{
		if (target_wp<=0 || target_wp>=tripdata.waypoints.size()) return 0;
		Waypoint w1=tripdata.waypoints.get(target_wp-1);
		Waypoint w2=tripdata.waypoints.get(target_wp);
		float along_a=(float)Project.exacter_distance(w1.latlon, lastpos);
		//float along_b=(float)Project.exacter_distance(w2.latlon, lastpos);
		float along=along_a/((float)w2.d);		
		return along;
	}
	/*!
	 * Based on closest point on line (target_wp-1) -> (target_wp)
	 */
	public float cur_wp_along2(LatLon forpos)
	{
		if (target_wp==0) return 1.0f;
		if (target_wp<=0 || target_wp>=tripdata.waypoints.size()) return 0;
		Waypoint w1=tripdata.waypoints.get(target_wp-1);
		Waypoint w2=tripdata.waypoints.get(target_wp);
		Vector m1=Project.latlon2mercvec(w1.latlon, 13);
		Vector m2=Project.latlon2mercvec(w2.latlon, 13);		
		Line line=new Line(m1,m2);
		Vector merc=Project.latlon2mercvec(forpos, 13);					
		Vector closest=line.closest(merc);
		
		float along_a=(float)Project.exacter_distance(w1.latlon, Project.mercvec2latlon(closest,13));
		//float along_b=(float)Project.exacter_distance(w2.latlon, lastpos);
		float along=along_a/((float)w2.d);		
		return along;
	}
	
	public void update_ensp(NextSigPoints nesp)
	{
		for(EnrouteSigPoints ensp:enroute)
		{
			if (ensp.latlon.equals(nesp.latlon))
			{
				nesp.eta=getEta(ensp);
				nesp.passed=ensp.passed;
				return;
			}
		}
		nesp.eta=null;
		nesp.passed=null;
	}
	public ArrayList<NextSigPoints> get_remaining_ensps()
	{
		ArrayList<NextSigPoints> ret=new ArrayList<TripState.NextSigPoints>();
		float along=cur_wp_along();
		for(EnrouteSigPoints ensp:enroute)
		{
			//Log.i("fplan.dp","Cur along: "+along+" considering ensp: "+ensp);
			if (ensp.target_wp>target_wp || ensp.target_wp==target_wp && ensp.ratio>along)
			{
				NextSigPoints nesp=ensp.nesp;
				nesp.latlon=ensp.latlon;
				nesp.name=ensp.name;
				nesp.passed=ensp.passed;
				nesp.eta=getEta(ensp);
				Log.i("fplan","Adding ensp, eta. "+nesp.eta);
				ret.add(nesp);
			}
		}
		return ret;		
	}
	public NextSigPoints getSPInfo(SigPoint sp)
	{
		if (target_wp<=0 || target_wp>=tripdata.waypoints.size()) return null;
		Waypoint w1=tripdata.waypoints.get(target_wp-1);
		Waypoint w2=tripdata.waypoints.get(target_wp);
		EnrouteSigPoints ensp=null;
		
		float along=cur_wp_along();
		for(EnrouteSigPoints cand:enroute)
		{
			Log.i("fplan.dp","Cand enr sig point: "+cand);
			if (cand.sp==sp) 
			{
				ensp=cand;
				if (ensp.target_wp>target_wp || (ensp.target_wp==target_wp && ensp.ratio>=along))
					break; //un-passed one
			}			
		}
		Log.i("fplan.dp","Enroute sig point: "+ensp);
		if (ensp==null) return null;
		NextSigPoints ret=ensp.nesp;
		ret.latlon=ensp.latlon;
		ret.name=ensp.name;
		if (ensp.target_wp<target_wp || (ensp.target_wp==target_wp && ensp.ratio<along))
		{
			Log.i("fplan.dp","has been pased");
			//this has been passed, or should have been passed
			if (ensp.passed!=null)
			{
				ret.passed=ensp.passed;
				return ret;
			}
			return ret; //neither passed or planned to be passed (missed)			
		}
		
		ret.eta=getEta(ensp);
		return ret;
	}
	static public class BugInfo
	{
		public float hdg;
		public float bank;
	}
	public BugInfo getBug() {
		if (target_wp<0 || target_wp>=tripdata.waypoints.size()) return null;
		
				
		
		float curbughdg = (float)getBugHdgImpl(lastpos);
		
		Merc lastposmerc=Project.latlon2merc(lastpos, 17);
		double onesec_speed=Project.approx_scale(lastposmerc, 17, actual_gs)/3600.0;
		Vector delta=Project.heading2vector(actual_hdg); 
		lastposmerc.x+=3*onesec_speed*delta.x; //look 3 seconds ahead
		lastposmerc.y+=3*onesec_speed*delta.y;		
		float nextbughdg = (float)getBugHdgImpl(Project.merc2latlon(lastposmerc,17));
					
		//Log.i("fplan.hdg","Heading bug at: "+curbughdg+" estimated next second at: "+nextbughdg);
		
		//spede
		/*
		float bank_angle=(float)Math.atan2(event.values[1],event.values[2]);
		if (speed>1 && Math.abs(bank_angle)>0.01)
		{
			Log.i("fplan.sensor","Bank angle:"+(bank_angle*180.0/Math.PI));
			float one_g_lift=(float)Math.cos(bank_angle);
			Log.i("fplan.sensor","one g lift: "+one_g_lift);
			float needed_gs=(1.0f/one_g_lift);
			Log.i("fplan.sensor","Needed gs: "+needed_gs);
			float acceleration=Math.abs(9.82f*needed_gs*(float)Math.sin(bank_angle));
			Log.i("fplan.sensor","Radial acc m/s/s: "+acceleration);
			Log.i("fplan.sensor","Velocity m/s: "+speed);
			//float acceleration=speed*speed/R;
			//float R*acceleration=speed*speed;
			float R=speed*speed/acceleration;
			float orbital_circumference=(float)(R*Math.PI*2.0f);
			float orbital_period=orbital_circumference/speed;
			turn_rate=360.0f/orbital_period;
			if (bank_angle>0) turn_rate=-turn_rate;
			*/
			
		float turn_rate=(float) (nextbughdg-actual_hdg);
		
		float abs_turn_rate=(float)Math.abs(turn_rate);
		/*
		float standard_bank=30;
		float roll_rate=10;
		float standard_rate = Project.getTurnRate((float)actual_gs, standard_bank);
		float nominal_seconds=(float) (Math.abs(actual_hdg-curbughdg)/standard_rate);
		float roll_seconds=nominal_seconds;
		float time_for_roll=roll_seconds*roll_rate;
		float target_correction_roll;
		if (time_for_roll>standard_bank/roll_rate)
			target_correction_roll=standard_bank;
		else
			target_correction_roll=time_for_roll*roll_rate;
		*/
		
		
		float bank=0;
		if (abs_turn_rate>1e-4)
		{
			float speed=((float)actual_gs*1.852f)/3.6f;
			float orbital_period=360.0f/abs_turn_rate;
			float orbital_circumference=orbital_period*speed;
			float R=orbital_circumference/((float)Math.PI*2.0f);
			float acceleration=(float)(speed*speed/R);
			//triangle:			
			//A = 9.82
			//B = acceleration
			bank=(float)((180.0f/Math.PI)*Math.atan2(acceleration,9.82f));
			if (bank>30)
				bank=30;
			if (turn_rate<0)
				bank=-bank;
			//Log.i("fplan.hdg","Speed: "+speed+" R: "+R+" acc: "+acceleration+" bank: "+bank);
		}
		
		
		BugInfo binfo=new BugInfo();
		
		
		binfo.bank=bank;
		binfo.hdg=curbughdg;
		return binfo;		
	}
	private double getBugHdgImpl(LatLon forpos) {
		double bughdg;
		double direct_hdg=Project.bearing(forpos, tripdata.waypoints.get(target_wp).latlon);
		{
			//Waypoint w1=tripdata.waypoints.get(i-1);
			Waypoint w2=tripdata.waypoints.get(target_wp);
			Vector merc=Project.latlon2mercvec(forpos, 13);
	
			double remain_advance=(double)corridor_width*0.4f;
			double along=cur_wp_along2(forpos);
			int curi=target_wp;
			//Log.i("fplan.hdg","target_wp: "+target_wp+" along: "+along);
			while(remain_advance>=0f)
			{
				//Log.i("fplan.hdg","iterate target_wp: "+curi+" along: "+along);
				if (w2.d<1e-3)
					along=10;
				else
				{				
					double new_along=along+(double)(remain_advance/w2.d);
					if (new_along>1) new_along=1;
					double advance_along=new_along-along;
					remain_advance-=advance_along*w2.d;
					along=new_along;
				}
				//Log.i("fplan.hdg","iterate-step target_wp: "+curi+" along: "+along);
				if (along>=1.0f-1e-4)
				{
					if (curi+1>=tripdata.waypoints.size())
					{
						along=1;
						break;
					}
					along=0;
					curi+=1;
					w2=tripdata.waypoints.get(curi);
					continue;
				}
				else
				{
					break;
				}
			}
			Vector respos;
			if (curi==0)
			{
				Waypoint w1=tripdata.waypoints.get(0);
				respos=Project.latlon2mercvec(w1.latlon, 13);
			}
			else
			{
				Waypoint w1=tripdata.waypoints.get(curi-1);
				Vector m1=Project.latlon2mercvec(w1.latlon, 13);
				Vector m2=Project.latlon2mercvec(w2.latlon, 13);
				respos=new Vector(
						(1.0f-along)*m1.x+along*m2.x,
						(1.0f-along)*m1.y+along*m2.y);
					
			}
				
			
			double next_d=(double)Project.exacter_distance(forpos, tripdata.waypoints.get(target_wp).latlon);
			
			double hdg=(double)Project.vector2heading(respos.minus(merc));
			
			//maximum miss-angle is that which gives (in radians)
			//(miss_angle) * distance = corridor_width
			//but to be conservative, we use:
			//(miss_angle) * distance = 0.8f*corridor_width
			//which yields
			//miss_angle = 0.8f*corridor_width/distance
			//miss_angle_degrees= (180.0f/Math.PI)*0.8f*corridor_width/distance
			if (next_d<1.5f*corridor_width)
			{
				double close=0;
				if (next_d<corridor_width)
					close=1;
				else
					close=(double)(1.5f*corridor_width-next_d)/(0.5f*corridor_width);
				
				double max_miss_angle=180;
				if (next_d>=0.5f*corridor_width)			
					max_miss_angle=(double)((180.0f/Math.PI)*0.4f*corridor_width/next_d);
				
				double miss_angle=hdg-direct_hdg;
				while (miss_angle>180) miss_angle-=360;
				while (miss_angle<-180) miss_angle+=360;
				
				//Log.i("fplan.hdg","direct_hdg: "+direct_hdg+" heading corridor ahead: "+hdg+" Max miss angle: "+max_miss_angle+" miss_angle: "+miss_angle);
				if (Math.abs(miss_angle)>max_miss_angle)
				{
					if (miss_angle>0) miss_angle=max_miss_angle;
					else miss_angle=-max_miss_angle;				
				}			
				bughdg=close*(direct_hdg+miss_angle)+(1.0f-close)*hdg;
			}
			else
			{
				bughdg=hdg;
			}
		}
		return bughdg;
	}
	private Date getEta(EnrouteSigPoints ensp) {
		Log.i("fplan.dp","getEta()");
		if (target_wp<0 || target_wp>=tripdata.waypoints.size()) return null;		
		if (ensp.target_wp==target_wp || ensp.target_wp==0)
		{
			Waypoint w2=tripdata.waypoints.get(ensp.target_wp);
			Log.i("fplan.dp","not been pased, cur leg, w2 gs"+w2.gs);
			
			float dist=(float)Project.exacter_distance(lastpos, ensp.latlon);
			if (w2.gs<1) return null;
			double time_hour=dist/w2.gs;
			double time_ms=time_hour*3600.0*1000.0;
			return new Date(new Date().getTime()+(long)time_ms);
		}
		if (waypointEvents.size()!=tripdata.waypoints.size())
			return null; //should never happen
		if (ensp.target_wp>=tripdata.waypoints.size() || ensp.target_wp<1)
			return null;
		
		Date eta;		
		WaypointInfo we1=waypointEvents.get(ensp.target_wp-1);
		WaypointInfo we2=waypointEvents.get(ensp.target_wp);
		Log.i("fplan.dp","not been pased, future legs: "+we1.eta2+" - "+we2.eta2);
		if (we1.eta2==null || we2.eta2==null)
		{
			Log.i("fplan.dp","we1.eta2==null || we2.eta2==null");
			eta=null;
		}
		else
		{
			long newtime=we1.eta2.getTime()+(long)(ensp.ratio*(double)(we2.eta2.getTime()-we1.eta2.getTime()));
			
			eta=new Date(newtime);;
		}
		return eta;
	}
	
	
	TripState(TripData trip)
	{
		waypointEvents=new ArrayList<WaypointInfo>();
		tripdata=trip;
		current_waypoint_idx=0;
		target_wp=0;
		extradummy=new String[]{};
		
		if (trip!=null && trip.waypoints!=null)
		{
			AirspaceLookup lookup=GlobalLookup.lookup;
			for(int i=1;i<trip.waypoints.size();++i)
			{
				Waypoint w1=trip.waypoints.get(i-1);
				Waypoint w2=trip.waypoints.get(i);
				Vector m1=Project.latlon2mercvec(w1.latlon, 13);
				Vector m2=Project.latlon2mercvec(w2.latlon, 13);
				Line line=new Line(m1,m2);
				double cutoff=Project.approx_scale(m1.gety(),13,0.5);
				BoundingBox bb=new BoundingBox(m1,m2);
				bb=bb.expand(50);			
				for(SigPoint sp:lookup.allSigPoints.findall(bb))
				{
					Vector sppos=sp.pos.toVector();
					Vector clo=line.closest(sppos);
					LatLon clolatlon=Project.mercvec2latlon(clo,13);
					float dist=(float)clo.minus(sppos).length();
					if (dist<=cutoff)
					{
						float along_a=(float)Project.exacter_distance(w1.latlon, clolatlon);
						float along_b=(float)Project.exacter_distance(w2.latlon, clolatlon);
						float along=along_a/(along_a+along_b);
						EnrouteSigPoints ensp=new EnrouteSigPoints();
						ensp.nesp=new NextSigPoints();
						ensp.ratio=along;
						ensp.sp=sp;
						ensp.latlon=sp.latlon;
						ensp.target_wp=i;
						ensp.name=sp.name;
						ensp.landing=false;
						enroute.add(ensp);
					}
				}
				if (w2.lastsub!=0)
				{
					EnrouteSigPoints ensp2=new EnrouteSigPoints();
					ensp2.nesp=new NextSigPoints();
					ensp2.ratio=1;
					ensp2.target_wp=i;
					ensp2.name=w2.name;			
					ensp2.latlon=w2.latlon;
					ensp2.landing=w2.land_at_end;
					enroute.add(ensp2);
				}
			}
		}
		
	}
	
	static public class NextLanding
	{
		String where;
		Date when;
		Date planned;
	}
	
	public NextLanding getNextLanding()
	{
		if (waypointEvents==null || waypointEvents.size()==0) return null;
		for(int i=target_wp;i<waypointEvents.size();++i)
		{
			WaypointInfo esp=waypointEvents.get(i);
			if (i==waypointEvents.size()-1 || tripdata.waypoints.get(i).land_at_end)
			{
				NextLanding nl=new NextLanding();
				nl.where=esp.point_title;;
				nl.when=esp.eta2;
				nl.planned=new Date(tripdata.waypoints.get(i).arrive_dt*1000l);
				return nl;
			}
		}
		return null;
	}
	public NextLanding getPrevTakeoff()
	{
		if (waypointEvents==null || waypointEvents.size()==0) return null;
		for(int i=target_wp-1;i>=0;--i)
		{
			if (i>=waypointEvents.size()) continue;
			WaypointInfo esp=waypointEvents.get(i);
			if (i==0 || tripdata.waypoints.get(i).land_at_end)
			{
				NextLanding nl=new NextLanding();
				nl.where=esp.point_title;;
				nl.when=esp.passed;
				nl.planned=new Date(tripdata.waypoints.get(i).depart_dt*1000l);
				return nl;
			}
		}
		return null;
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
	private double actual_hdg;
	
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
		actual_hdg=mylocation.getBearing();
				
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
					tpoints-=2; //Gravely penalize not landing 
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
					if (Project.latlon2mercvec(old_wp.latlon,13).minus(mypos).length()>
							corridor_width*onenm*1.25)
					{
						we.skipped=true;
						we.eta2=null;
						we.passed=null;
					}
				}
				
				if (current_waypoint_idx==target_wp && target_wp!=best_points_i)
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
			
			if (target_wp>=0 && target_wp<tripdata.waypoints.size())
			{
				for(int checkpass=Math.max(0,target_wp-1);checkpass<=target_wp;++checkpass)
				{
					Waypoint wp=tripdata.waypoints.get(checkpass);
					double distnm=Project.exacter_distance(myposlatlon,wp.latlon);
					if (distnm<corridor_width)
					{
						WaypointInfo we=waypointEvents.get(checkpass);
						if (wp.land_at_end)
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
				}
			}
			
			for(int i=0;i<target_wp;++i)
			{
				if (i>=tripdata.waypoints.size()) break;
				WaypointInfo we=waypointEvents.get(i);
				we.distance=-1;
			}
			long now=new Date().getTime();
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
		if (tripdata==null || i>=waypointEvents.size() || i<0)
			return null;
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
				@Override
				public Date getPassed() {
					return TripState.this.getPassed();
				}
				@Override
				public boolean hasPassed() {
					return !is_own_position();
				}				
			};
		}
		@Override
		public LatLon getLatLon() {
			int i=current_waypoint_idx;
			if (tripdata==null || i>=tripdata.waypoints.size())
				return null;
			if (i==-1) return lastpos;
			Waypoint wp=tripdata.waypoints.get(i);
			return wp.latlon;
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
	public String get_registration() {
		return tripdata.aircraft;
	}
	public String get_atsradioname() {
		return tripdata.atsradiotype;
	}
	
		
}
