package se.flightplanner.map3d;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import android.os.SystemClock;

import se.flightplanner.AirspaceArea;
import se.flightplanner.AirspaceLookup;
import se.flightplanner.Project;
import se.flightplanner.SpaceStats;
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
		public int heading;
	}
	volatile ObserverState curstate;
	ObserverContext(AirspaceLookup lookup)
	{
		this.lookup=lookup;
	}
	void update(iMerc pos,int heading)
	{
		ObserverState newstate=new ObserverState();
		newstate.pos=pos;
		newstate.heading=heading;
		BoundingBox bb=new BoundingBox(pos.x,pos.y,pos.x,pos.y);
		bb=bb.expand(Project.approx_scale(pos.y, 13, 20));
		newstate.spaces=lookup.areas.get_areas(bb);
		long now = SystemClock.uptimeMillis();
		for(AirspaceArea area : newstate.spaces)
		{
			SpaceStats stats=area.dyninfo;
			if (stats==null || now-stats.updated>100)
				area.dyninfo = stats = SpaceStats.getStats(pos, area);			
		}
		Collections.sort(newstate.spaces,new Comparator<AirspaceArea>()
				{
					public int compare(AirspaceArea arg0, AirspaceArea arg1) {
						if (arg0.dyninfo.dist<arg1.dyninfo.dist)
							return -1;
						if (arg0.dyninfo.dist>arg1.dyninfo.dist)
							return +1;
						return 0;
					}
				});
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
