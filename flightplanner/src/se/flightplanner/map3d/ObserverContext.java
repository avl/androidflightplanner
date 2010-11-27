package se.flightplanner.map3d;

import java.util.ArrayList;

import se.flightplanner.AirspaceArea;
import se.flightplanner.AirspaceLookup;
import se.flightplanner.Project;
import se.flightplanner.Project.iMerc;
import se.flightplanner.map3d.AirspaceDrawer.DrawnAirspace;
import se.flightplanner.vector.BoundingBox;

public class ObserverContext {
	AirspaceLookup lookup;
	
	/**
	 * Elements of this class are never changed after having been created.
	 * Thus, they are "thread safe".
	 */
	public static class ObserverState
	{
		public ArrayList<AirspaceArea> spaces;
		public iMerc pos;		
	}
	volatile ObserverState curstate;
	ObserverContext(AirspaceLookup lookup)
	{
		this.lookup=lookup;
	}
	void update(iMerc pos)
	{
		ObserverState newstate=new ObserverState();
		newstate.pos=pos;
		BoundingBox bb=new BoundingBox(pos.x,pos.y,pos.x,pos.y);
		bb=bb.expand(Project.approx_scale(pos.y, 13, 20));
		newstate.spaces=lookup.areas.get_areas(bb);
		curstate=newstate;
	}
	
	/**
	 * This may be called from any state. The returned
	 * object is never updated or changed.
	 */
	public ObserverState getState() {
		return curstate;
	}
	

}
