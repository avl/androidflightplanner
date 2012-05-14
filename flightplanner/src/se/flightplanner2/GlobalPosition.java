package se.flightplanner2;

import se.flightplanner2.Project.LatLon;
import android.location.Location;

public class GlobalPosition {

	static public interface PositionSubscriberIf
	{
		public void gps_update(Location newpos);
		public void gps_disabled();		
	}
	
	static public interface PositionIf
	{
		public Location getLastPosition();
		/**
		 * @return time is in same base as SystemClock.elapsedTime()
		 */
		public long getLastPositionUpdate(); 
		
		/**
		 * @param ref Stores weak reference to this. Called on GPS position updates.
		 * @return
		 */		
		public void registerSubscriber(PositionSubscriberIf ref);
		public void unRegisterSubscriber(PositionSubscriberIf ref);

		public void setDebugTurn(float f);
		public void setDebugSpeed(float f);
		public void debugElev(float i);
	
	}
	public static PositionIf pos;
	
	public static void registerSubscriber(PositionSubscriberIf ref)
	{
		if (pos!=null) pos.registerSubscriber(ref);
	}
	public static void unRegisterSubscriber(PositionSubscriberIf ref)
	{
		if (pos!=null) pos.unRegisterSubscriber(ref);		
	}
	public static Location getLastPosition()
	{
		if (pos!=null) return pos.getLastPosition();
		return null;
	}
	public static long getLastPositionUpdate()
	{
		if (pos!=null) return pos.getLastPositionUpdate();
		return 0;
	}
	public static LatLon getLastLatLonPosition() {
		return new LatLon(getLastPosition());
	}
	
	
}
