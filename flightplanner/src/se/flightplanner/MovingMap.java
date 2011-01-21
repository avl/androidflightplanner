package se.flightplanner;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import se.flightplanner.BackgroundMapLoader.UpdatableUI;
import se.flightplanner.GuiSituation.GuiClientInterface;
import se.flightplanner.MapDrawer.DrawResult;
import se.flightplanner.Project.LatLon;
import se.flightplanner.Project.Merc;
import se.flightplanner.Project.iMerc;
import se.flightplanner.vector.Vector;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.Paint.Style;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.location.Location;
import android.location.LocationManager;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.text.format.Time;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class MovingMap extends View implements UpdatableUI,GuiClientInterface {
	private TripData tripdata;
	private TripState tripstate;
	private AirspaceLookup lookup;
	private Location lastpos;
	private long last_real_position;
	private MapDrawer drawer;
	private int lastcachesize; //bitmap cache
	private String download_status;
	private BearingSpeedCalc bearingspeed;
	private MapCache mapcache;
	private BackgroundMapLoader loader;
	private GetMapBitmap bitmaps;
	private ArrayList<Blob> blobs;
	private float x_dpmm;
	private float y_dpmm;
	private FlightPathLogger fplog;
	public void doInvalidate()
	{
		invalidate();
	}
	public MovingMap(Context context,DisplayMetrics metrics, FlightPathLogger fplog)
	{
		super(context);
		this.fplog=fplog;
		bearingspeed=new BearingSpeedCalc();
		lastpos=bearingspeed.calcBearingSpeed(null);
		float dot_per_mm_y=metrics.ydpi/25.4f;
		y_dpmm=dot_per_mm_y;
		float dot_per_mm_x=metrics.xdpi/25.4f;
		x_dpmm=dot_per_mm_x;
		
		tripstate=new TripState(null,null);
		
		//float bigtextsize=dot_per_mm_y*2.7f; //6.5 mm text size
		//float textsize=dot_per_mm_y*1.75f; //6.5 mm text size
		 
		last_real_position=0;
		loader=null;

		enableTerrainMap(true);
		//utctz = TimeZone.getTimeZone("UTC");		 
		lostSignalTimer=new Handler();

		//textpaint.set
		tripdata=null;
		

		setKeepScreenOn(true);
	}
	
	public void update_tripdata(TripData ptripdata)
	{
		tripdata=ptripdata;
		tripstate=new TripState(tripdata,lookup);
		if (lastpos!=null)
			tripstate.update_target(lastpos);		
		invalidate();
	}
	protected void onDraw(Canvas canvas) {
		
		if (gui==null || drawer==null)
		{
			if (getLeft()!=0)
				throw new RuntimeException("This app doesn't work in a subwindow 1");
			if (getTop()!=0)
				throw new RuntimeException("This app doesn't work in a subwindow 2");
			drawer=new MapDrawer(x_dpmm,y_dpmm);
			if (getBottom()<50 || getRight()<50)
				throw new RuntimeException("The screen is way too small");

			gui=new GuiSituation(this,drawer.getNumInfoLines(getBottom()-getTop()),lastpos,
					getRight()-getLeft(),getBottom()-getTop());
			
		}
        canvas.drawColor(Color.BLACK);
		if (tripdata==null)
		{
			//canvas.drawText("No trip loaded.", this.getLeft(), this.getTop()+textpaint.getTextSize(), textpaint);
			//return;
		}
		if (lastpos!=null)
		{
			//canvas.translate(this.getLeft(),this.getTop());
			if (mapcache!=null)
				mapcache.forgetqueries();
			Rect screenExtent=new Rect(
					getLeft(),getTop(),
					getRight(),getBottom());
			if (currentInfo!=null)
				currentInfo.updatemypos(Project.latlon2mercvec(
						new LatLon(lastpos.getLatitude(),lastpos.getLongitude()), 13)
							, lastpos.getSpeed() * 3.6 / 1.852);
			
			DrawResult res=drawer.draw_actual_map(tripdata, tripstate,
					lookup,
					canvas,					
					screenExtent,lastpos,
					bitmaps, //isDragging
					gui,
					last_real_position,
					download_status,
					currentInfo
					);
			lastcachesize=res.lastcachesize;
			if (mapcache!=null && mapcache.haveUnsatisfiedQueries())
			{
				if (loader==null)
				{
					loader=new BackgroundMapLoader(blobs, mapcache, this,lastcachesize);
					loader.run();
					Log.i("fplan.bitmap","Start a background task again");
				}
				else
				{
					//loader.cancel(true);
					//Log.i("fplan.bitmap","Cancel running background task, need a new.");
				}
			}
			

		}
        /*
		int x = 10;
		int y = 10;
		int width = 300;
		int height = 50;
		ShapeDrawable mDrawable = new ShapeDrawable(new OvalShape());
		mDrawable.getPaint().setColor(0xff74AC23);
		mDrawable.setBounds(x, y, x + width, y + height);
		mDrawable.draw(canvas);
        */
		
		//canvas.drawText("ÅÄÖ", 10, this.getTop()+textpaint.getTextSize(), textpaint);
		//canvas.drawText("TRIP", 10, this.getBottom(), textpaint);
		//canvas.drawText("TRIP", 10, 100, textpaint);
	}
	
	public void gps_update(Location loc)
	{
		 
		
		lastpos=bearingspeed.calcBearingSpeed(loc);
		if (gui!=null)
			gui.updatePos(lastpos);
		last_real_position=SystemClock.uptimeMillis();
		tripstate.update_target(lastpos);
		LatLon latlon=new LatLon(lastpos.getLatitude(),lastpos.getLongitude());
		iMerc merc17=Project.latlon2imerc(latlon,17);
		try {
			fplog.log(merc17, lastpos.getTime(), (int)(lastpos.getSpeed()*3.6f/1.852f), lookup);
		} catch (IOException e) {
			e.printStackTrace();
		}
		invalidate();
		if (curLostSignalRunnable!=null)
			lostSignalTimer.removeCallbacks(curLostSignalRunnable);
		curLostSignalRunnable=new Runnable() {			
			@Override	
			public void run() {
				Log.i("fplan","Lost signal runnable called.");
				gps_disabled();
			}
		};
		lostSignalTimer.postDelayed(curLostSignalRunnable, 5000);		
	}
	
	public void gps_disabled() {
		last_real_position=0;
		invalidate();		
	}

	public void zoom(int zd) {
		
		if (debugRunnner)
		{
			debugSpeed+=zd*1;
		}
		else
		{
			if (gui!=null) gui.changeZoom(zd);
		}
		
	}
	public void update_airspace(Airspace pairspace, AirspaceLookup plookup) {
		lookup=plookup;
		tripstate=new TripState(tripdata,lookup);
		if (lastpos!=null)
			tripstate.update_target(lastpos);
		invalidate();
	}

	public void onSideKey(int i) {
			
		if (debugRunnner)
		{
			debugHdgRad+=i*(10.0f*Math.PI/180.0f);
		}
		else
		{				
			sideways(i);
		}
		
	}
	public void sideways(int i) {
		if (tripstate!=null)
		{
			if (i==-1)
			{
				if (currentInfo==null || !currentInfo.hasLeft())
				{
					currentInfo=null;
				}
				else
				{						
					currentInfo.left();
				}
			}
			else if (i==1)
			{
				if (currentInfo==null)
				{
					currentInfo=tripstate;
				}
				else
				{
					if (currentInfo.hasRight())
						tripstate.right();
				}
			}
		}
		
		///lastpos.setBearing((lastpos.getBearing()+i*5)%360);
		invalidate();
	}
	@Override
	public void updateUI(boolean done) {
		Log.i("fplan.bitmap","updateUI: done="+done);
		if (done)
		{
			this.loader=null;
			if (mapcache!=null && mapcache.haveUnsatisfiedQueries())
			{
				Log.i("fplan.bitmap","Restart background task in updateUI");
				loader=new BackgroundMapLoader(blobs,mapcache,this,lastcachesize);
				loader.run();
			}
		}
		invalidate();
	}
	public void set_download_status(String prog) {
		// TODO Auto-generated method stub
		download_status=prog;
		invalidate();
	}
	public void enableTerrainMap(boolean b) {
		if (b==false)
		{
			mapcache.shutdown();
			mapcache=null;
			blobs=null;
			bitmaps=null;
			invalidate();
			//there may be a loader active, updating the mapcache we should have freed
			//if this happens, there is no problem, the request will be ignored.
		}
		else
		{
			if (mapcache!=null)
				mapcache.shutdown();
			mapcache=new MapCache();
			blobs=new ArrayList<Blob>();
			try 
			{				
				int maxzoomlevel=0;
				for(int i=0;i<=13;++i)
				{
					
					File extpath = Environment.getExternalStorageDirectory();
					File path = new File(extpath,
							"/Android/data/se.flightplanner/files/level" + i);
					if (!path.exists())
						break;
					Log.i("fplan","Reading map from "+path.toString());
					blobs.add(new Blob(path.toString(),256));
					maxzoomlevel=i;
				}
				if (maxzoomlevel==0)
				{
					blobs=null;
					bitmaps=null;					
				}
				else
				{					
					bitmaps=new GetMapBitmap(mapcache,maxzoomlevel);
				}
			} catch (IOException e) {
				//System.out.println("Failed opening terrain bitmap. Check file:"+path);
				Log.e("fplan",e.toString());
				e.printStackTrace();
				blobs=null;
				bitmaps=null;
			}
			if (blobs!=null)
				Log.i("fplan","blobs loaded ok;"+blobs.size());					
		}
		invalidate();
	}
	
	
	@Override
	public boolean onTouchEvent(MotionEvent ev)
	{		
		if (gui!=null)
			return gui.handleOnTouch(ev,x_dpmm,y_dpmm);
		else
			return true;
	}
	private Handler lostSignalTimer;
	private Runnable curLostSignalRunnable;
	
	private GuiSituation gui;
	InformationPanel currentInfo;
	public void showInfo(LatLon about) {
		// TODO Auto-generated method stub
		ArrayList<String> details = new ArrayList<String>(); 
		ArrayList<String> extradetails = new ArrayList<String>();
		Vector point=Project.latlon2mercvec(about,13);
		lookup.get_airspace_details(1.0,
				point,details,extradetails);			
		//Log.i("fplan","Actual GS for Current Position: "+actual_gs);
		currentInfo=new AirspacePointInfo("fixed","Airspace",
				details.toArray(new String[details.size()]),
				extradetails.toArray(new String[extradetails.size()]),
				point,true);
		
	}

	public InformationPanel getCurrentWarning() {
		return currentInfo;
	}
	
	private Handler debugTimer;
	private Runnable debugRunnable;
	private boolean debugRunnner;
	private Merc debugMerc;
	private float debugSpeed;
	private float debugHdgRad;
	public void enableDriving(boolean debugdrive) 
	{
		if (debugdrive)
		{
			debugRunnner=true;
			if (debugMerc==null)
			{
				debugMerc=Project.latlon2merc(new LatLon(
						lastpos.getLatitude(),lastpos.getLongitude()), 13);
			}
			final MovingMap outer=this;
			if (debugTimer==null)
			{
				debugTimer=new Handler();
			}			
			debugRunnable=new Runnable() {			
				@Override	
				public void run() {
					if (outer.debugRunnable==null)
						return; //stop
					debugMerc=new
						Merc(debugMerc.x+debugSpeed*Math.cos(debugHdgRad+Math.PI/2.0),
								debugMerc.y+debugSpeed*Math.sin(debugHdgRad+Math.PI/2.0));
					LatLon l=Project.merc2latlon(debugMerc, 13);
					Location loc=new Location("gps");
					//, "alt": 30, "lon": 
					loc.setLatitude(l.lat);
					loc.setLongitude(l.lon);
					loc.removeBearing();
					loc.removeSpeed();
					Date d = new Date();
					loc.setTime(d.getTime());
					outer.gps_update(loc);					
					debugTimer.postDelayed(debugRunnable, 1000);
				}
			};
			debugTimer.postDelayed(debugRunnable, 1000);		
		}
		else
		{
			debugRunnner=false;
			debugRunnable=null;
		}
	}
	
}
