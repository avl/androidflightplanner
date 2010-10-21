package se.flightplanner;

import java.io.IOException;

import se.flightplanner.map3d.ElevationStore;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.Paint.Style;
import android.location.Location;
import android.opengl.GLSurfaceView;
import android.view.View;

public class MovingMap3D extends GLSurfaceView {
	private Location lastpos;
	private BearingSpeedCalc bs;
	MovingMap3DRenderer rend;
	public MovingMap3D(Context context)
	{
		super(context);
		bs=new BearingSpeedCalc();
		setKeepScreenOn(true);
		rend=new MovingMap3DRenderer();
		this.setRenderer(rend);
	}

	public void zoom(int i) {
		// TODO Auto-generated method stub
		rend.zoom(i);
		
	}

	public void sideways(int i) {
		// TODO Auto-generated method stub
		rend.sideways(i);
	}
	private Airspace airspace;
	private AirspaceLookup lookup;
	private TripData tripdata;
	private ElevationStore estore;
	public void update_tripdata(TripData tripdata) {
		// TODO Auto-generated method stub
		this.tripdata=tripdata;
		rend.update_tripdata(this.tripdata);
		
	}

	public void update_airspace(Airspace airspace, AirspaceLookup lookup) {
		// TODO Auto-generated method stub
		this.airspace=airspace;
		this.lookup=lookup;
		if (airspace!=null && lookup!=null && estore!=null)
			rend.update(airspace,lookup,estore);
	}

	public void gps_update(Location location) {
		lastpos=bs.calcBearingSpeed(location);
		rend.setpos(lastpos);
	}

	public void gps_disabled() {
		// TODO Auto-generated method stub
		
	}

	public void update_estore(ElevationStore estore) {
		this.estore=estore;
		if (airspace!=null && lookup!=null && estore!=null)
			rend.update(airspace,lookup,estore);
	}

	public void debugdump() throws IOException {
		rend.debugdump();
		
	}

}
