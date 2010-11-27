package se.flightplanner;

import java.io.IOException;

import se.flightplanner.map3d.ElevationStoreIf;
import se.flightplanner.map3d.TextureStore;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.Paint.Style;
import android.location.Location;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class MovingMap3D extends GLSurfaceView {
	private Location lastpos;
	private BearingSpeedCalc bs;
	MovingMap3DRenderer rend;
	public MovingMap3D(Context context)
	{
		super(context);
		Bitmap fontbitmap = BitmapFactory.decodeResource(context.getResources(),
				   R.drawable.font);		
		bs=new BearingSpeedCalc();
		setKeepScreenOn(true);
		rend=new MovingMap3DRenderer(fontbitmap);
		this.setRenderer(rend);
	}
	@Override
	public boolean onTouchEvent(MotionEvent ev)
	{
		float x=ev.getX();
		float y=ev.getY();
		//Log.d("fplan","Touch event!"+x+","+y);
		
		
		if (ev.getAction()==MotionEvent.ACTION_DOWN ||
			ev.getAction()==MotionEvent.ACTION_MOVE)	
			rend.onTouchEvent(x,y);	
		else
		if (ev.getAction()==MotionEvent.ACTION_UP)
			rend.onFingerUp(x,y);
					
		return true;
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
	private ElevationStoreIf estore;
	private TextureStore tstore;
	
	public void update_tripdata(TripData tripdata) {
		// TODO Auto-generated method stub
		this.tripdata=tripdata;
		rend.update_tripdata(this.tripdata);
		
	}

	public void update_airspace(Airspace airspace, AirspaceLookup lookup) {
		// TODO Auto-generated method stub
		this.airspace=airspace;
		this.lookup=lookup;
		if (airspace!=null && lookup!=null && estore!=null && tstore!=null)
			rend.update(airspace,lookup,estore,tstore);
	}

	public void gps_update(Location location) {
		lastpos=bs.calcBearingSpeed(location);
		rend.setpos(lastpos);
	}

	public void gps_disabled() {
		// TODO Auto-generated method stub
		
	}

	public void update_stores(ElevationStoreIf estore, TextureStore tstore) {
		this.estore=estore;
		this.tstore=tstore;
		if (airspace!=null && lookup!=null && estore!=null && tstore!=null)
			rend.update(airspace,lookup,estore,tstore);
	}

	public void debugdump() throws IOException {
		rend.debugdump();
		
	}

}
