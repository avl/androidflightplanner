package se.flightplanner2;

import java.util.Date;
import java.util.WeakHashMap;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import se.flightplanner2.GlobalPosition.PositionIf;
import se.flightplanner2.GlobalPosition.PositionSubscriberIf;
import se.flightplanner2.Project.LatLon;
import se.flightplanner2.Project.Merc;

public class GlobalPositionImpl implements PositionIf, LocationListener {
	
	private WeakHashMap<PositionSubscriberIf,Void> subs=new WeakHashMap<GlobalPosition.PositionSubscriberIf, Void>();
	
	private Location last_location;
	private BearingSpeedCalc bearingspeed=new BearingSpeedCalc();
	private long when;
	private LocationManager locman;
	private Handler handler;
	public GlobalPositionImpl(LocationManager locman)
	{
		this.locman=locman;
		locman.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0,this);
		last_location=bearingspeed.calcBearingSpeed(null);
		handler=new Handler();
		
	}
	private Runnable lost_gps=new Runnable()
	{
		@Override
		public void run() {
			for(PositionSubscriberIf pos:subs.keySet())
				pos.gps_disabled();			
		}		
	};
	public void stop()
	{
		handler.removeCallbacks(lost_gps);
		handler.removeCallbacks(debugRunnable);
		locman.removeUpdates(this);
	}
	public void onLocationChanged(Location loc) {		
		if (debugRunner) return;
		onLocationChangedImpl(loc);
	}
	public void onLocationChangedImpl(Location loc) {
		Location location = bearingspeed.calcBearingSpeed(loc);
		last_location = location;
		//Log.i("fplan.sensor","Number of subscribers: "+subs.size());
		for(PositionSubscriberIf pos:subs.keySet())
			pos.gps_update(last_location);
		when=SystemClock.elapsedRealtime();
		handler.removeCallbacks(lost_gps);
		handler.postDelayed(lost_gps, 7500);
		
	}

	@Override
	public void onProviderDisabled(String provider) {
		if (debugRunner) return;
		for(PositionSubscriberIf pos:subs.keySet())
			pos.gps_disabled();
	}

	@Override
	public void onProviderEnabled(String provider) {
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		if (status != LocationProvider.AVAILABLE)
			{
			for(PositionSubscriberIf pos:subs.keySet())
				pos.gps_disabled();			
			}
		

	}	
	@Override
	public Location getLastPosition() {
		return last_location;
	}

	@Override
	public long getLastPositionUpdate() {
		return when;
	}

	@Override
	public void registerSubscriber(PositionSubscriberIf ref) {
		subs.put(ref,null);
		
	}
	@Override
	public void unRegisterSubscriber(PositionSubscriberIf ref) {
		subs.remove(ref);
		
	}
	private Runnable debugRunnable;
	private boolean debugRunner=false;
	private Merc debugMerc;
	private float debugSpeed;
	private float debugHdgRad;
	private float debugTurn;
	private float debugElev;
	
	@Override
	public void setDebugTurn(float f) {
		debugTurn=f;
		if (!debugRunner) enableDriving();
	}
	@Override
	public void setDebugSpeed(float f) {
		debugSpeed=f;
		if (!debugRunner) enableDriving();
	}
	
	public void enableDriving() 
	{
		if (!Config.debugMode()) return;
		if (debugRunner==true)
			return;
		handler.removeCallbacks(lost_gps);
		debugRunner=true;
		if (debugMerc==null)
		{
			debugMerc=Project.latlon2merc(new LatLon(
					last_location.getLatitude(),last_location.getLongitude()), 13);
		}
		debugRunnable=new Runnable() {
			BearingSpeedCalc bearingspeed=new BearingSpeedCalc();
			@Override	
			public void run() {
				//Log.i("fplan.sensor","Debugspeed: "+debugSpeed+" debugHdg: "+debugHdgRad+" debugTurn: "+debugTurn);
				
				
				float debugSpeedMerc=(float)(Project.approx_scale(debugMerc, 13, debugSpeed)/3600.0);
				
				debugMerc=new
					Merc(debugMerc.x+debugSpeedMerc*Math.cos(debugHdgRad+Math.PI/2.0),
							debugMerc.y+debugSpeedMerc*Math.sin(debugHdgRad+Math.PI/2.0));
				debugHdgRad+=debugTurn/(180.0/Math.PI);
				LatLon l=Project.merc2latlon(debugMerc, 13);
				Location loc=new Location("gps");
				//, "alt": 30, "lon": 
				loc.setLatitude(l.lat);
				loc.setLongitude(l.lon);
				loc.setAltitude(debugElev);
				Date d = new Date();
				loc.setTime(d.getTime());
				loc.removeBearing();
				loc.removeSpeed();
				
				onLocationChangedImpl(loc);									
				handler.postDelayed(debugRunnable, 1000);
			}
		};
		handler.postDelayed(debugRunnable, 1000);		
	}
	@Override
	public void debugElev(float i) {
		debugElev+=i;
	}
	

}
