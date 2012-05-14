package se.flightplanner2;

import java.util.Date;

import se.flightplanner2.Project.LatLon;
import android.location.Location;

public class NakedDetailedPlace implements DetailedPlace {
	private LatLon pos;
	private LatLon lastloc;
	private Location last_location;
	private String name;
	public NakedDetailedPlace(String name,LatLon pos)
	{
		this.name=name;
		this.pos=pos;
	}
	
	@Override
	public double getDistance() {
		if (lastloc==null) return -1;
		float d=(float) Project.exacter_distance(
				lastloc, 
				pos);
		return d;
	}

	@Override
	public Date getETA2() {
		return getETA();
	}

	@Override
	public Date getETA() {
		if (lastloc==null) return null;
		float actual_gs=last_location.getSpeed()*3.6f/1.852f;
		float d=(float) Project.exacter_distance(
				lastloc, 
				pos);
		if (actual_gs<1.0)
			return null;
		double deltah=d/actual_gs;
		long now=new Date().getTime();
		return new Date((long)(now+deltah*1000l*3600l));
	}

	@Override
	public void update_pos(Location location) {
		last_location=location;
		lastloc=new LatLon(last_location);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public boolean hasPlanned() {
		return false;
	}

	@Override
	public Date getPlanned() {
		return null;
	}

	@Override
	public LatLon getPos() {
		return pos;
	}

	@Override
	public Float getPlannedFuel() {
		return null;
	}

	@Override
	public Float getPlannedGs() {
		return null;
	}

	@Override
	public boolean hasPrevNext() {
		return false;
	}

	@Override
	public void prev() {
		
	}

	@Override
	public void next() {
		
	}

	@Override
	public boolean is_own_position() {
		return false;
	}

	@Override
	public Date getPassed() {
		return null;
	}

	@Override
	public boolean hasPassed() {
		return false;
	}
	

}
