package se.flightplanner;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import se.flightplanner.BackgroundMapLoader.UpdatableUI;
import se.flightplanner.MapDrawer.DrawResult;
import se.flightplanner.Project.LatLon;
import se.flightplanner.Project.Merc;
import se.flightplanner.Timeout.DoSomething;
import se.flightplanner.vector.Vector;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
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

public class MovingMap extends View implements UpdatableUI {
	private TripData tripdata;
	private TripState tripstate;
	private AirspaceLookup lookup;
	private Location lastpos;
	private int zoomlevel;
	private long last_real_position;
	private MapDrawer drawer;
	private int lastcachesize; //bitmap cache
	private String download_status;
	private BearingSpeedCalc bearingspeed;
	//private CountDownTimer timer;
	//private TimeZone utctz;
	private MapCache mapcache;
	private BackgroundMapLoader loader;
	private GetMapBitmap bitmaps;
	private ArrayList<Blob> blobs;
	private float x_dpmm;
	private float y_dpmm;
	
	public interface Clickable
	{
		Rect getRect();
		void onClick();
	}
	private ArrayList<Clickable> clickables;
	
	private Handler lostSignalTimer;
	private Runnable curLostSignalRunnable;
	private Timeout drag_timeout;
	
	
	@Override
	public boolean onTouchEvent(MotionEvent ev)
	{		
		float x=ev.getX();
		Transform tf = getTransform();
		float y=ev.getY();
		if (ev.getAction()==MotionEvent.ACTION_DOWN ||
			ev.getAction()==MotionEvent.ACTION_MOVE)	
			onTouchFingerDown(tf,x,y);	
		else
		if (ev.getAction()==MotionEvent.ACTION_UP)
			onTouchFingerUp(tf,x,y);
		return true;
	}			
	void onClassicalClick(float x,float y)
	{
		float b=getBottom();
		float r=getRight();
		float w=getRight()-getLeft();
		float h=getBottom()-getTop();
		Rect fat_finger=new Rect((int)x,(int)y,(int)x,(int)y);
		grow(fat_finger,(int)(w*0.075f));
		for(Clickable click : clickables)
		{			
			if (Rect.intersects(fat_finger,click.getRect()))
			{
				click.onClick();
				return;
			}
		}
		
		if (y>b-0.2*h)
		{
			if (x<0.33*w)
				sideways(-1);
			else
			if (x>r-0.33*w)
				sideways(+1);
			else
			{
				cycleextrainfo();
			}
		}
		else
		{
			Transform tf = getTransform();
			Merc m=tf.screen2merc(new Vector(x,y));
			LatLon point=Project.merc2latlon(m,zoomlevel);
			tripstate.showInfo(point,new LatLon(lastpos.getLatitude(),lastpos.getLongitude()));
			invalidate();
		}
	}
	Transform getTransform() {
		int xsize=getRight()-getLeft();
		int ysize=getBottom()-getTop();
		if (lastpos!=null)
		{
			Merc mypos;
			float hdg=0;
			if (drag_center13!=null)
			{
				Merc drag_center=Project.merc2merc(drag_center13,13,zoomlevel);
				mypos=new Merc(drag_center.x,drag_center.y);
				hdg=drag_heading;
			}
			else
			{
				mypos=Project.latlon2merc(new LatLon(lastpos.getLatitude(),lastpos.getLongitude()),zoomlevel);
				if (lastpos!=null && lastpos.hasBearing())
				{
					hdg=lastpos.getBearing();
				}				
			}
			return new Transform(mypos,drawer.getArrow(xsize,ysize,drag_center13!=null),(float)hdg,zoomlevel);
		}
		else
		{
			return new Transform(new Merc(128<<zoomlevel,128<<zoomlevel),drawer.getArrow(xsize,ysize,drag_center13!=null),0,zoomlevel);			
		}
	}
	private boolean extrainfo;
	private int extrainfolineoffset;
	private void cycleextrainfo() {
		InformationItem we=tripstate.getCurrentWarning();
		if (we!=null)
		{
			String[] details;
			int maxlines=drawer.getNumInfoLines(getBottom()-getTop());
			if (extrainfo)
				details=we.getExtraDetails();
			else
				details=we.getDetails();
	
			Log.i("fplan","Cycling!");
			int lines=details.length;
			if (lines-extrainfolineoffset<=maxlines)
			{ //the set previously visible included the last ones, so let's move on.
				extrainfo=!extrainfo;
				extrainfolineoffset=0;
				if (extrainfo)
					details=we.getExtraDetails();
				else
					details=we.getDetails();
			}
			else
			{
				extrainfolineoffset+=maxlines;
			}
			invalidate();
		}
	}
	public MovingMap(Context context,DisplayMetrics metrics)
	{
		super(context);
		tripstate=new TripState(null,null);
		drag_timeout=new Timeout();
		clickables=new ArrayList<MovingMap.Clickable>();
		float dot_per_mm_y=metrics.ydpi/25.4f;
		y_dpmm=dot_per_mm_y;
		float dot_per_mm_x=metrics.xdpi/25.4f;
		x_dpmm=dot_per_mm_x;
		drawer=new MapDrawer(x_dpmm,y_dpmm);
		
		float bigtextsize=dot_per_mm_y*2.7f; //6.5 mm text size
		float textsize=dot_per_mm_y*1.75f; //6.5 mm text size
		 
		last_real_position=0;
		loader=null;

		enableTerrainMap(true);
		bearingspeed=new BearingSpeedCalc();
		//utctz = TimeZone.getTimeZone("UTC");		 
		zoomlevel=9;
		lostSignalTimer=new Handler();

		//textpaint.set
		lastpos=null;
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
			DrawResult res=drawer.draw_actual_map(this, canvas,
					this.getRight()-this.getLeft(),
					this.getBottom()-this.getTop(),					
					extrainfo,extrainfolineoffset,
					drag_center13!=null, //isDragging
					tripdata,
					tripstate,
					lookup,
					lastpos,
					zoomlevel,
					last_real_position, //timestamp
					bitmaps,
					clickables,
					getTransform(),
					download_status,
					screenExtent
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
	
	boolean tileOnScreen(float cx,float cy,Transform tf)
	{
		float maxdiag=363;
		if (cx+maxdiag<getLeft()) return false;
		if (cx-maxdiag>getRight()) return false;
		if (cy+maxdiag<getTop()) return false;
		if (cy-maxdiag>getBottom()) return false;

		Vector base=new Vector(cx,cy);
		Vector v=new Vector();
		int sidex=0;
		int sidey=0;
		for(int j=0;j<2;++j)
		{
			for(int i=0;i<2;++i)
			{
				v.x=256*i;
				v.y=256*j;
				Vector r=v.rot(tf.hdgrad);
				r.x+=base.x;
				r.y+=base.y;
				int cursidex=0;
				int cursidey=0;
				if (v.x<getLeft()) cursidex=-1;
				if (v.x>getRight()) cursidex=1;
				if (v.y<getTop()) cursidey=-1;
				if (v.y>getBottom()) cursidey=1;
				if (cursidex==0 && cursidey==0)
					return true;
				if (i==0 && j==0)
				{
					sidex=cursidex;
					sidey=cursidey;
				}
				if (cursidex==0 || cursidex!=sidex)
					sidex=0;
				if (cursidey==0 || cursidey!=sidey)
					sidey=0;
			}
		}
		if (sidex!=0 || sidey!=0)
			return false;
		return true;
		
	}
	void grow(Rect r,int howmuch)
	{
		r.left-=howmuch;
		r.right+=howmuch;
		r.top-=howmuch;
		r.bottom+=howmuch;
	}
	void addTextIfFits(Canvas canvas,String sizetext, RectF r, String realtext,float y,
			Paint tp) {
		if (sizetext==null)
		{
			canvas.drawText(realtext, r.left, y, tp);
			r.left=r.right+1;			
		}
		else
		{			
			Rect rect=new Rect();					
			tp.getTextBounds(sizetext, 0, sizetext.length(),rect);
			if (r.left+(rect.right-rect.left)<r.right)
			{
				canvas.drawText(realtext, r.left, y, tp);
				r.left+=(rect.right-rect.left)+1.0f*x_dpmm;
			}
			else
			{
				r.left=r.right+1; //definitely out of space now!
			}
		}

	}
	String fmttime(int when) {
		if (when==0 || when>3600*24*10)
			return "--:--";
		return String.format("%d:%02d",when/60,when%60);
	}

/*	private double rot_x(double x,double y) {
		double rad=0;
		if (lastpos!=null && lastpos.hasBearing())
		{
			rad=(-Math.PI/180.0)*lastpos.getBearing();
		}
		return Math.cos(rad)*x - Math.sin(rad)*y;
	}
	private double rot_y(double x,double y) {
		double rad=0;
		if (lastpos!=null && lastpos.hasBearing())
		{
			rad=(-Math.PI/180.0)*lastpos.getBearing();
		}
		return Math.sin(rad)*x + Math.cos(rad)*y;
	}*/
	
	public void gps_update(Location loc)
	{
		 
		
		lastpos=bearingspeed.calcBearingSpeed(loc);
		last_real_position=SystemClock.uptimeMillis();
		tripstate.update_target(lastpos);
		invalidate();
		if (curLostSignalRunnable!=null)
			lostSignalTimer.removeCallbacks(curLostSignalRunnable);
		curLostSignalRunnable=new Runnable() {			
			@Override	
			public void run() {
				Log.i("fplan","Lost signal runnable called.");
				last_real_position=0;
				invalidate();
			}
		};
		lostSignalTimer.postDelayed(curLostSignalRunnable, 5000);		
	}
	
	public void gps_disabled() {
		last_real_position=0;
		invalidate();		
	}

	public void zoom(int zd) {
		
		onTouchAbort();
		state=GuiState.IDLE;
		
		zoomlevel+=zd;
		if (zoomlevel<4)
			zoomlevel=4;
		else
		if (zoomlevel>13)
			zoomlevel=13;		
		invalidate();		
	}

	
	public void update_airspace(Airspace pairspace, AirspaceLookup plookup) {
		lookup=plookup;
		tripstate=new TripState(tripdata,lookup);
		if (lastpos!=null)
			tripstate.update_target(lastpos);
		invalidate();
	}

	public void sideways(int i) {
		if (tripstate!=null)
		{
			
			if (i==-1)
				tripstate.left();
			else if (i==1)
				tripstate.right();
			
			lastpos.setBearing((lastpos.getBearing()+i*5)%360);
			invalidate();
		}
		
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
				for(int i=0;i<=10;++i)
				{
					
					File extpath = Environment.getExternalStorageDirectory();
					File path = new File(extpath,
							"/Android/data/se.flightplanner/files/level" + i);
					Log.i("fplan","Reading map from "+path.toString());
					blobs.add(new Blob(path.toString(),256));
				}
				bitmaps=new GetMapBitmap(mapcache);
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
	
	
	private enum GuiState
	{
		IDLE, //Normal
		MAYBE_DRAG, //about to start dragging
		DRAGGING
	}
	private GuiState state=GuiState.IDLE;
	public void onTouchAbort()
	{ 
		//this is called when zoomlevel is changed, which can happen
		//while dragging, in principle.
		state=GuiState.IDLE;
	}
	public void onNorthUp()
	{
		drag_heading=0;
		invalidate();
	}
	public void doCenterDragging()
	{
		state=GuiState.IDLE;
		drag_center13=null;
		drag_base13=null;
		invalidate();
	}
	public void onTouchFingerUp(Transform tf,float x, float y) {
		switch(state)
		{
		case IDLE:
			break;
		case MAYBE_DRAG:
			onClassicalClick(x, y);
			state=GuiState.IDLE;
			break;
		case DRAGGING:
			state=GuiState.IDLE;
			break;
		}
	}
	private float dragstartx,dragstarty;	
	private Merc drag_center13;
	private Merc drag_base13;
	private float drag_heading;
	public class DragTimeout implements DoSomething
	{
		@Override
		public void run() {
			doCenterDragging();
		}	
	}
	public void onTouchFingerDown(Transform tf,float x, float y) {
		switch(state)
		{
		case IDLE:
			dragstartx=x;
			dragstarty=y;
			state=GuiState.MAYBE_DRAG;
			break;
		case MAYBE_DRAG:
		{
			float dist=(dragstartx-x)*(dragstartx-x)+(dragstarty-y)*(dragstarty-y);
			float thresh=4.0f*x_dpmm;
			if (dist>thresh*thresh)
			{
				dragstartx=x;
				dragstarty=y;
				float h=0.25f*(getBottom()-getTop());
				float hdgrad=tf.getHdgRad();
				Merc t=new Merc(tf.getPos().x,tf.getPos().y);
				
				if (drag_center13==null)
				{
					float deltax=(float)(Math.sin(hdgrad)*h);
					float deltay=-(float)(Math.cos(hdgrad)*h);
					t.x+=deltax;
					t.y+=deltay;
				}
				drag_base13=Project.merc2merc(t, zoomlevel, 13);
				drag_heading=tf.getHdg();
				state=GuiState.DRAGGING;
				resetDragTimeout();
			}
		}
			break;
		case DRAGGING:
			float deltax1=(x-dragstartx)*(1<<(13-zoomlevel));
			float deltay1=(y-dragstarty)*(1<<(13-zoomlevel));
			float hdgrad=tf.getHdgRad();
			float deltax=(float)(Math.cos(hdgrad)*deltax1-Math.sin(hdgrad)*deltay1);
			float deltay=(float)(Math.sin(hdgrad)*deltax1+Math.cos(hdgrad)*deltay1);
			drag_center13=new Merc(drag_base13.x-deltax,drag_base13.y-deltay);
			resetDragTimeout();
			invalidate();
			break;
		}
		
	}
	private void resetDragTimeout() {
		drag_timeout.timeout(new DragTimeout(), 30000);
	}
	
}
