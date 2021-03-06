package se.flightplanner2;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;

import se.flightplanner2.AdChartLoader.AdChartOwner;
import se.flightplanner2.Airspace.ChartInfo;
import se.flightplanner2.Airspace.VariantInfo;
import se.flightplanner2.BackgroundMapLoader.UpdatableUI;
import se.flightplanner2.GuiSituation.GuiClientInterface;
import se.flightplanner2.Project.LatLon;
import se.flightplanner2.Project.Merc;
import se.flightplanner2.Project.iMerc;
import se.flightplanner2.Timeout.DoSomething;
import se.flightplanner2.vector.BoundingBox;
import android.app.Activity;
import android.content.Context;
import android.drm.DrmStore.RightsStatus;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.location.Location;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

public class MovingMap extends View implements UpdatableUI,GuiClientInterface,MainMapIf, AdChartOwner {
	private TripData tripdata;
	private TripState tripstate;
	private AirspaceLookup lookup;
	private Location lastpos;
	private int gps_sat_cnt;
	private int gps_sat_fix_cnt;
	private long last_real_position;
	private MapDrawer drawer;
	private String download_status;
	private boolean download_dismissable;
	private Timeout dismiss_timeout;
	private MapCache mapcache;
	private BackgroundMapLoader loader;
	private GetMapBitmap bitmaps;
	private ArrayList<Blob> blobs;
	private boolean defnorthup=false;
	private float x_dpmm;
	private float y_dpmm;
	private int batt=-1;
	private boolean charging=false;
	private float screen_size_x,screen_size_y;
	private FlightPathLogger fplog;
	private MovingMapOwner owner;
	private ElevBitmapCache elevbmc;
	private int detaillevel;
	private String[] prox_warning; //about nearby airspaces
	private boolean terrwarn;
	interface MovingMapOwner
	{
		public void cancelMapDownload();

		public void doShowExtended(Place[] places);

		public void showAirspaces();

		public void toggleAltimeter();
	}
	
	@Override
	public void doInvalidate() //for GUI-client
	{
		invalidate();
		//Log.i("fplan.mmupd","doInvalidate called anyway");
		handler.removeCallbacks(invalidate_within_runner);
		next_invalidate_time=Long.MAX_VALUE; 
	}
	private AdChartLoader adloader=null;
	public MovingMap(Context context,DisplayMetrics metrics, FlightPathLogger fplog,MovingMapOwner owner,
			TripState ptripstate,String storage)
	{
		super(context);
		this.owner=owner;
		this.fplog=fplog;
		this.storage=storage;
		
		dismiss_timeout=new Timeout();
		BearingSpeedCalc bearingspeed=new BearingSpeedCalc();
		
		reinit_bmc();
		
		lastpos=bearingspeed.calcBearingSpeed(null);
		float dot_per_mm_y=metrics.ydpi/25.4f;
		y_dpmm=dot_per_mm_y;
		float dot_per_mm_x=metrics.xdpi/25.4f;
		x_dpmm=dot_per_mm_x;
		
		
		
		screen_size_x=metrics.widthPixels/dot_per_mm_x;
		screen_size_y=metrics.heightPixels/dot_per_mm_y;
		
		this.tripstate=ptripstate;
		
		//float bigtextsize=dot_per_mm_y*2.kf; //6.5 mm text size
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
	
	public void update_tripdata(TripData ptripdata,TripState newstate)
	{
		tripdata=ptripdata;
		tripstate=newstate;//new TripState(tripdata);
		if (lastpos!=null)
			tripstate.updatemypos(lastpos);
		if (gui!=null)
			gui.updateTripState(tripstate);
		doInvalidate();
	}
	
	private int lastwidth,lastheight; 
	protected void onDraw(Canvas canvas) {
		int width=getRight()-getLeft();
		int height=getBottom()-getTop();
		
		if (gui==null || drawer==null)
		{
				
			drawer=new MapDrawer(x_dpmm,y_dpmm,screen_size_x,screen_size_y);
			if (getBottom()<50 || getRight()<50)
				throw new RuntimeException("The screen is way too small");

			gui=new GuiSituation(this,drawer.getNumInfoLines(height),lastpos,
					width,height,tripstate,lookup);
			lastwidth=width;
			lastheight=height;
			gui.setnorthup(defnorthup);			
		}
		if (width!=lastwidth || height!=lastheight)
		{
			lastwidth=width;
			lastheight=height;
			gui.sizechange(width,height,drawer.getNumInfoLines(height));
		}
		
		/*
		if (getLeft()!=0)
			canvas.translate(-getLeft(), 0);
		if (getTop()!=0)
			canvas.translate(0,-getTop());
		*/
		
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
			gui.updatePos(lastpos);
			GetMapBitmap usebitmaps=bitmaps;
			if (MapDetailLevels.getMaxLevelFromDetail(detaillevel)<0)
				usebitmaps=null;
			drawer.draw_actual_map(tripdata, tripstate,
					lookup,
					canvas,					
					screenExtent,lastpos,
					usebitmaps, //isDragging
					gui,
					last_real_position,
					download_status,
					gui.getCurrentInfo(),
					this,
					prox_warning,
					gps_sat_cnt,gps_sat_fix_cnt,
					elevbmc,terrwarn,batt,charging,adloader,
					chosen_ad_maps,chosen_ad_map_i,chosen_ad_map_when,
					last_cvr_amp,sideview,
					altimeter_setting,pressure_hpa,
					qnh
					);
			
			if (mapcache!=null)
			{
				mapcache.garbageCollect();
				if (mapcache.haveUnsatisfiedQueries())
				{
					if (loader==null)
					{
						loader=new BackgroundMapLoader(blobs, mapcache, this);
						loader.run();
						//Log.i("fplan.bitmap","Start a background task again");
					}
					else
					{
						//loader.cancel(true);
						//Log.i("fplan.bitmap","Cancel running background task, need a new.");
					}
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
	private int last_cvr_amp;
	private String storage;
	public void gps_update(Location loc,boolean terrwarn,int cvr_amp)
	{
		this.terrwarn=terrwarn;
		this.last_cvr_amp=cvr_amp;
		if (loc==null) return;
		
		//lastpos=bearingspeed.calcBearingSpeed(loc);
		lastpos=loc;
		if (gui!=null)
			gui.updatePos(lastpos);
		//Log.i("fplan.sensor","in gps_update, bearing: "+lastpos.getBearing());
		last_real_position=SystemClock.uptimeMillis();
		tripstate.updatemypos(lastpos);
		LatLon latlon=new LatLon(lastpos.getLatitude(),lastpos.getLongitude());
		iMerc merc17=Project.latlon2imerc(latlon,17);
		try {
			int altfeet=(int)(lastpos.getAltitude()/0.3048);
			fplog.log(merc17, lastpos.getTime(), (int)(lastpos.getSpeed()*3.6f/1.852f), lookup,altfeet,storage);
		} catch (IOException e) {
			e.printStackTrace();
		}
		doInvalidate();
		if (curLostSignalRunnable!=null)
			lostSignalTimer.removeCallbacks(curLostSignalRunnable);
		curLostSignalRunnable=new Runnable() {			
			@Override	
			public void run() {
				Log.i("fplan","Lost signal runnable called.");
				gps_disabled();
			}
		};
		lostSignalTimer.postDelayed(curLostSignalRunnable, 10000);		
	}
	
	public void gps_disabled() {
		last_real_position=0;
		doInvalidate();		
	}

	public void zoom(int zd) {		
		if (gui!=null) gui.changeZoom(zd);		
	}
	
	public void update_airspace(Airspace pairspace, AirspaceLookup plookup,int newdetaillevel,boolean northup,boolean sideview) {
		defnorthup=northup;
		
		lookup=plookup;
		detaillevel=newdetaillevel;
		this.sideview=sideview;
		if (gui!=null)
		{
			gui.updateLookup(lookup);
			gui.updateTripState(tripstate);
			gui.setnorthup(defnorthup);
		}
		reinit_bmc();
		doInvalidate();
	}
	private void reinit_bmc() {
		elevbmc=new ElevBitmapCache(new ElevBitmapCache.ClientIf() {			
			@Override
			public void updated(boolean fully_updated) {
				if (fully_updated)
					MovingMap.this.doInvalidate();
				else
					MovingMap.this.invalidate_within(750);				
			}

			@Override
			public File getStorage() {
				return Storage.getStorage(storage);
			}
		});
		GlobalGetElev.get_elev=elevbmc;
	}

	public void onSideKey(int i) {
			
		if (gui!=null)
			gui.onInfoPanelBrowse(i);
		doInvalidate();
		
	}

	/**
	 * Used by background map loader to reload UI
	 * when finished loading a map, and upon
	 * progress.
	 */
	@Override
	public void updateUI(boolean done) {
		//Log.i("fplan.bitmap","updateUI: done="+done);
		if (done)
		{
			this.loader=null;
			if (mapcache!=null && mapcache.haveUnsatisfiedQueries())
			{
				//Log.i("fplan.bitmap","Restart background task in updateUI");
				loader=new BackgroundMapLoader(blobs,mapcache,this);
				loader.run();
			}
			if (mapcache==null || !mapcache.haveUnsatisfiedQueries())
				doInvalidate();
			else
				invalidate_within(750);
		}
	}
	public void set_download_status(String prog, boolean dismissable) {
		download_status=prog;
		download_dismissable=dismissable;
		if (dismissable)
		{
			dismiss_timeout.timeout(new DoSomething(){
				@Override
				public void run() {
					download_status="";
					doInvalidate();
				}
			}, 10000);
		}
		doInvalidate();
	}
	public void enableTerrainMap(boolean b) {
		if (b==false)
		{
			if (mapcache!=null)
				mapcache.shutdown();
			mapcache=null;
			blobs=null;
			bitmaps=null;
			doInvalidate();
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
					
					File extpath = Storage.getStorage(storage);
					File path = new File(extpath,
							Config.path+"level" + i);
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
					bitmaps=new GetMapBitmap(mapcache);
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
		doInvalidate();
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
	
	@Override
	public void cancelMapDownload() {
		Log.i("fplan","cancel map download:");
		if (download_dismissable)
		{
			download_status="";
			doInvalidate();
		}
		else
		{		
			owner.cancelMapDownload();
		}
	}
	public void releaseMemory()
	{
		if (bitmaps!=null)
			bitmaps.releaseMemory();
		stop();
	}
	public void stop()
	{
		if (drawer!=null) 
			drawer.stop();		
	}
	private boolean sideview;
	@Override
	public void update_detail(int det,boolean northup,boolean sideview) {
		detaillevel=det;
		this.defnorthup=northup;
		if (gui!=null)
			gui.setnorthup(defnorthup);
		this.sideview=sideview;
		doInvalidate();
	}
	@Override
	public void thisSetContentView(Activity nav) {
		nav.setContentView(this);		
	}
	@Override
	public void doShowExtended(Place[] places) {
		this.owner.doShowExtended(places);
		
	}
	@Override
	public void proxwarner_update(String[] warning) {
		prox_warning=warning;		
		
	}
	@Override
	public void showAirspaces()
	{
		this.owner.showAirspaces();
	}
	
	private Handler handler=new Handler();
	private long next_invalidate_time=Long.MAX_VALUE;
	private Runnable invalidate_within_runner=new Runnable()
	{
		@Override
		public void run() {
			MovingMap.this.invalidate();
			long now=SystemClock.elapsedRealtime();
			long delta=30000;
			next_invalidate_time=now+delta;
			handler.postDelayed(this,delta);
			//Log.i("fplan.mmupd","Delayed invalidate running");			
		}
	};
	private void invalidate_within(int ms)
	{
		long now=SystemClock.elapsedRealtime();
		long scheduled_delta=next_invalidate_time-now;
		if (scheduled_delta<ms) return;
		next_invalidate_time=now+ms;
		handler.removeCallbacks(invalidate_within_runner);
		handler.postDelayed(invalidate_within_runner,ms);
		//Log.i("fplan.mmupd","Posting a delayed invalidate in "+ms+"ms");
	}
	@Override
	public void set_gps_sat_cnt(int satcnt,int satfixcnt) {
		if (gps_sat_cnt!=satcnt)
		{
			gps_sat_cnt=satcnt;
			gps_sat_fix_cnt=satfixcnt;
			invalidate_within(500);
		}		
	}
	@Override
	public void set_battery_level(int level,boolean plugged) {
		batt=level;
		charging=plugged;
		invalidate_within(1500);
	}

	@Override
	public void adChartLoadFinish(boolean done) {
		invalidate_within(1000);
	}

	
	private int cur_toggle_map=-1;
	private String[] chosen_ad_maps;
	private int chosen_ad_map_i;
	private long chosen_ad_map_when;
	private double pressure_hpa;
	private String altimeter_setting;
	private int qnh;
	@Override
	public void selectChart(String chart) {
		releaseMemory();
		adloader=new AdChartLoader(chart,this,storage);
		gui.chartMode(true,adloader.getChartCenter(),adloader.getChartUpBearing(),
				adloader.best_zoomlevel(getRight()-getLeft()),false);
		if (!gui.isCentered()) drawer.find_me(gui,lastpos,getRight()-getLeft(),getBottom()-getTop());
		doInvalidate();
	}

	@Override
	public void clear_map_toggle_list()
	{
		chosen_ad_maps=null;
	}
	@Override
	public void toggle_map() {
		if (lookup==null) return;
		cur_toggle_map+=1;
		ArrayList<VariantInfo> avail=new ArrayList<VariantInfo>();
		ArrayList<SigPoint> sps=new ArrayList<SigPoint>();
		ArrayList<String> names=new ArrayList<String>();
		for(SigPoint sp:lookup.majorAirports.findall(BoundingBox.nearby(new LatLon(lastpos), 30)))
		{
			
			if (sp.extra!=null && sp.extra.icao!=null)
			{
				ChartInfo cif=lookup.getChartInfo(sp.extra.icao);
				if (cif!=null && cif.getVariants()!=null)
					for(VariantInfo variant:cif.getVariants())
					{
						if (lookup.haveproj(variant.chartname))
						{
							avail.add(variant);
							sps.add(sp);
							names.add(sp.name+" "+Airspace.getHumanReadableVariant(variant));
							
						}
					}
			}
		}
		names.add("base map");
		names.add("elevation map");
		chosen_ad_maps=names.toArray(new String[]{});
		
		if (cur_toggle_map>=avail.size())
		{
			chosen_ad_map_i=cur_toggle_map;
			chosen_ad_map_when=SystemClock.elapsedRealtime();
			if (adloader!=null)
				adloader.releaseMemory();
			adloader=null;
			boolean elevonly=false;
			if (cur_toggle_map>=avail.size()+1)
			{
				elevonly=true;
				cur_toggle_map=-1;
			}
			gui.chartMode(false,null,0,0,elevonly);
			if (!gui.isCentered()) drawer.find_me(gui,lastpos,getRight()-getLeft(),getBottom()-getTop());
		}
		else
		{
			releaseMemory();
			chosen_ad_map_i=cur_toggle_map;
			chosen_ad_map_when=SystemClock.elapsedRealtime();
			adloader=new AdChartLoader(avail.get(cur_toggle_map).chartname,this,storage);
			gui.chartMode(true,adloader.getChartCenter(),adloader.getChartUpBearing(),
					adloader.best_zoomlevel(getRight()-getLeft()),false);
			if (!gui.isCentered()) drawer.find_me(gui,lastpos,getRight()-getLeft(),getBottom()-getTop());
		}
		
		doInvalidate();
		
	}

	@Override
	public void pressure_update(double pressure_value) {
		this.pressure_hpa=pressure_value;
		invalidate_within(1000);		
	}

	@Override
	public void toggleAltimeter() {
		this.owner.toggleAltimeter();
	}

	@Override
	public void set_altimeter(String altimeterSetting,int qnh) {
		this.altimeter_setting=altimeterSetting;
		this.qnh=qnh;
		invalidate_within(1000);		
	}
	
}
