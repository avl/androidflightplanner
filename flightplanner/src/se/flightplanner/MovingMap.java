package se.flightplanner;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import se.flightplanner.BackgroundMapLoader.UpdatableUI;
import se.flightplanner.GetMapBitmap.BitmapRes;
import se.flightplanner.Project.LatLon;
import se.flightplanner.Project.Merc;
import se.flightplanner.Project.iMerc;
import se.flightplanner.TripData.Waypoint;
import se.flightplanner.TripState.WarningEvent;
import se.flightplanner.vector.BoundingBox;
import se.flightplanner.vector.Vector;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
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
	private int background;
	private int foreground;
	private int zoomlevel;
	private Paint textpaint;
	private Paint bigtextpaint;
	private Paint linepaint;
	private Paint thinlinepaint;
	private Paint trippaint;
	private Paint seltrippaint;
	private Paint arrowpaint;
	private Paint backgroundpaint;
	private long last_real_position;
	private int lastcachesize; //bitmap cache
	private String download_status;
	BearingSpeedCalc bearingspeed;
	//private CountDownTimer timer;
	//private TimeZone utctz;
	private MapCache mapcache;
	private BackgroundMapLoader loader;
	private GetMapBitmap bitmaps;
	private ArrayList<Blob> blobs;
	
	private Handler lostSignalTimer;
	private Runnable curLostSignalRunnable;
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
		
		
		if (y>b-0.2*h)
		{
			if (x<0.33*w)
				sideways(-1);
			else
			if (x>r-0.33*w)
				sideways(+1);
			else
			{
				if (extrainfo)
					hideextrainfo();
				else
					showextrainfo();
			}
		}
		else
		{
			/*
			
			if (lastpos!=null)
			{
				Transform tf = getTransform();
				Merc m=tf.screen2merc(new Vector(x,y));
				LatLon point=Project.merc2latlon(m,zoomlevel);
				tripstate.showInfo(point,new LatLon(lastpos.getLatitude(),lastpos.getLongitude()));
				invalidate();
			}
			*/
		}
	}
	private Transform getTransform() {
		if (lastpos!=null)
		{
			Merc mypos;
			float hdg=0;
			if (drag_center!=null)
			{
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
			return new Transform(mypos,getArrow(),(float)hdg,zoomlevel);
		}
		else
		{
			return new Transform(new Merc(128<<zoomlevel,128<<zoomlevel),getArrow(),0,zoomlevel);			
		}
	}
	private boolean extrainfo;
	private void hideextrainfo() {
		extrainfo=false;		
		invalidate();
	}
	private void showextrainfo() {
		extrainfo=true;
		invalidate();
		
	}
	float x_dpmm;
	float y_dpmm;
	public MovingMap(Context context,DisplayMetrics metrics)
	{
		super(context);
		float dot_per_mm_y=metrics.ydpi/25.4f;
		y_dpmm=dot_per_mm_y;
		float dot_per_mm_x=metrics.xdpi/25.4f;
		x_dpmm=dot_per_mm_x;
		
		float bigtextsize=dot_per_mm_y*2.7f; //6.5 mm text size
		float textsize=dot_per_mm_y*2.0f; //6.5 mm text size
		 
		last_real_position=0;
		loader=null;

		enableTerrainMap(true);
		bearingspeed=new BearingSpeedCalc();
		//utctz = TimeZone.getTimeZone("UTC");		 
		zoomlevel=9;
		background=Color.BLACK;
		foreground=Color.WHITE;
		lostSignalTimer=new Handler();

		bigtextpaint = new Paint();
		bigtextpaint.setAntiAlias(true);
		bigtextpaint.setStrokeWidth(5);
		bigtextpaint.setColor(foreground);
		bigtextpaint.setStrokeCap(Paint.Cap.ROUND);
		bigtextpaint.setTextSize(bigtextsize);
		bigtextpaint.setTypeface(Typeface.create(Typeface.SANS_SERIF,
                                           Typeface.NORMAL));
		textpaint = new Paint();
		textpaint.setAntiAlias(true);
		textpaint.setStrokeWidth(5);
		textpaint.setColor(foreground);
		textpaint.setStrokeCap(Paint.Cap.ROUND);
		textpaint.setTextSize(textsize);
		textpaint.setTypeface(Typeface.create(Typeface.SANS_SERIF,
                                           Typeface.NORMAL));
		linepaint = new Paint();
		linepaint.setAntiAlias(true);
		linepaint.setStrokeWidth(5);
		linepaint.setColor(Color.RED);
		linepaint.setStrokeCap(Paint.Cap.ROUND);

		thinlinepaint = new Paint();
		thinlinepaint.setAntiAlias(true);
		thinlinepaint.setStrokeWidth(2);
		thinlinepaint.setStyle(Style.STROKE);
		thinlinepaint.setColor(Color.RED);
		thinlinepaint.setStrokeCap(Paint.Cap.ROUND);

		trippaint = new Paint();
		trippaint.setAntiAlias(true);
		trippaint.setStrokeWidth(5);
		trippaint.setColor(Color.YELLOW);
		trippaint.setStrokeCap(Paint.Cap.ROUND);

		seltrippaint = new Paint();
		seltrippaint.setAntiAlias(true);
		seltrippaint.setStrokeWidth(5);
		seltrippaint.setColor(Color.WHITE);
		seltrippaint.setStrokeCap(Paint.Cap.ROUND);

		backgroundpaint = new Paint();
		backgroundpaint.setStyle(Style.FILL);
		backgroundpaint.setARGB(0xa0,0,0,0);


		arrowpaint = new Paint();
		arrowpaint.setAntiAlias(true);
		arrowpaint.setStyle(Style.FILL);
		arrowpaint.setStrokeWidth(5);
		arrowpaint.setColor(Color.WHITE);
		arrowpaint.setStrokeCap(Paint.Cap.ROUND);
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
        canvas.drawColor(background);
		if (tripdata==null)
		{
			//canvas.drawText("No trip loaded.", this.getLeft(), this.getTop()+textpaint.getTextSize(), textpaint);
			//return;
		}
		if (lastpos!=null)
		{
			canvas.translate(this.getLeft(),this.getTop());
			draw_actual_map(canvas,
					this.getRight()-this.getLeft(),
					this.getBottom()-this.getTop());

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
	
	class Transform
	{
		public Transform(
				Merc mypos,
				Vector arrow,
				float hdg,
				int zoomlevel)
		{
			this.mypos=mypos;
			this.hsizex=arrow.x;
			this.hsizey=arrow.y;
			this.hdg=(float)hdg;
			this.hdgrad=(float)(hdg*(Math.PI/180.0));
			this.zoomlevel=zoomlevel;
		}
		public double hsizex; //x position of observer in screen coordinates
		public double hsizey; //y position of observer in screen coordinates
		Merc mypos; //Position of user
		public float hdgrad; //heading of user, in radians
		public float hdg;
		int zoomlevel;
		/// Convert from merc to screen coordinates with 
		/// north up on map
		public Merc getPos()
		{
			return mypos;
		}
		public float getHdg()
		{
			return hdg;
		}
		public float getHdgRad()
		{
			return hdgrad;
		}
		public Vector merc2northscreen(Merc m)
		{
			return new Vector(m.x-mypos.x+hsizex,m.y-mypos.y+hsizey);
		}
		/// Convert from screen coordinates with 
		/// north up on map to merc.
		public Merc northscreen2merc(Vector n)
		{
			/*
			s.x=m.x-mypos.x+hsizex
			s.y=-m.y+mypos.y+hsizey
			m.x=s.x+mypos.x-hsizex
			m.y=mypos.y+hsizey-s.y
			*/
			return new Merc(n.getx()+mypos.x-hsizex,n.gety()+mypos.y-hsizey);
		}
		private Vector northscreen2screen(Vector n)
		{
			Vector c=new Vector(n.getx()-hsizex,n.gety()-hsizey);
			Vector r=c.unrot(hdgrad);
			return new Vector(r.getx()+hsizex,r.gety()+hsizey);
		}
		private Vector screen2northscreen(Vector s)
		{
			Vector c=new Vector(s.getx()-hsizex,s.gety()-hsizey);
			Vector r=c.rot(hdgrad);
			return new Vector(r.getx()+hsizex,r.gety()+hsizey);
		}
		public Merc screen2merc(Vector s)
		{
			return northscreen2merc(screen2northscreen(s));
		}
		public Vector merc2screen(Merc m)
		{
			return northscreen2screen(merc2northscreen(m));
		}
	}
	private Vector getArrow()
	{
		int xsize=this.getRight()-this.getLeft();
		int ysize=this.getBottom()-this.getTop();
		return new Vector(xsize/2,ysize/2+ysize/4);		
	}
	private boolean tileOnScreen(float cx,float cy,Transform tf)
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
	private void draw_actual_map(Canvas canvas, int sizex, int sizey) {
		if (zoomlevel>13) throw new RuntimeException("zoomlevel must be <=13");
		//int zoomgap=13-zoomlevel;
		//Merc mypos=Project.latlon2merc(new LatLon(lastpos.getLatitude(),lastpos.getLongitude()),zoomlevel);
		//Merc mypos13=Project.latlon2merc(new LatLon(lastpos.getLatitude(),lastpos.getLongitude()),13);
		Transform tf = getTransform();
				
		Vector arrow=getArrow();
		Merc screen_center=tf.screen2merc(new Vector(sizex/2,sizey/2));
		Merc screen_center13=Project.merc2merc(
				screen_center,
				zoomlevel,
				13);		
		
		double fivenm13=Project.approx_scale(screen_center13.y,13,10);
		double diagonal13;		
		{
			int zoomgap13=13-zoomlevel;
			diagonal13=((1<<zoomgap13)*(Math.sqrt(arrow.x*arrow.x+arrow.y*arrow.y)+50))+1;
		}
		BoundingBox bb13=new BoundingBox(
				screen_center13.x,screen_center13.y,
				screen_center13.x,screen_center13.y).expand(diagonal13);

		BoundingBox smbb13=new BoundingBox(screen_center13.x,screen_center13.y,
				screen_center13.x,screen_center13.y).expand(fivenm13);
				
		//bb13=new BoundingBox(-1e20,-1e20,1e20,1e20);		
		/*ShapeDrawable mDrawable = new ShapeDrawable(new OvalShape());
		mDrawable.getPaint().setColor(0xff74AC23);
		mDrawable.setBounds(x, y, x + width, y + height);
		mDrawable.draw(canvas);*/
		if (bitmaps!=null)
		{
			iMerc centertile=new iMerc(
					(int)screen_center.x&(~255),
					(int)screen_center.y&(~255));
			int diagonal=(int) Math.sqrt((sizex/2)*(sizex/2)+(sizey*sizey*3)/5);
			int minus=(diagonal+255)/256;
			int tot=2*minus+1;
			iMerc topleft=new iMerc(centertile.getX() - (256*minus),centertile.getY()-256*minus);
			int cachesize=tot*tot;
			mapcache.forgetqueries();
			//Vector v=tf.merc2screen(
			//		new Merc(topleft.x,topleft.y));
			float hdg=(float)(tf.hdgrad*(180.0/Math.PI));
			
			//float pivotx=(this.getLeft()+this.getRight())/2;
			//float pivoty=this.getTop()-(sizey/2+sizey/4);
			for(int j=0;j<tot;++j)
			{
				
				for(int i=0;i<tot;++i)
				{
					iMerc cur=new iMerc(topleft.getX()+256*i,topleft.getY()+256*j);
					if (cur.getX()<0 || cur.getY()<0)
						continue;
					Vector v=tf.merc2screen(
							new Merc(cur.getX(),cur.getY()));
					if (!tileOnScreen((float)v.x,(float)v.y,tf))
						continue;
					BitmapRes b=null;
					//Log.i("fplan","Bitmap for "+cur);
					b = bitmaps.getBitmap(cur,zoomlevel,cachesize);
					if (b!=null && b.b!=null)
					{
						//float px=(float)(v.x+i*256);
						//float py=(float)(v.y+j*256);
						
						//Log.i("fplan","Drawing bitmap at "+v.x+","+v.y);
						canvas.save();
						RectF trg=new RectF((float)v.x,(float)v.y,(float)v.x+256,(float)v.y+256);
						Rect src=b.rect;
						canvas.rotate(-hdg,(float)v.x,(float)v.y);
						canvas.drawBitmap(b.b, src,trg, null);						
						canvas.restore();
					}
				}
			}
			if (mapcache.haveUnsatisfiedQueries())
			{
				if (loader==null)
				{
					loader=new BackgroundMapLoader(blobs, mapcache, this,cachesize);
					loader.run();
					Log.i("fplan.bitmap","Start a background task again");
				}
				else
				{
					//loader.cancel(true);
					//Log.i("fplan.bitmap","Cancel running background task, need a new.");
				}
			}
			lastcachesize=cachesize;
			
		}
		
		
		//sigPointTree.verify();
		if (zoomlevel>=8 && lookup!=null)
		{
			for(AirspaceArea as:lookup.areas.get_areas(bb13))
			{/*
				boolean all_left=true;
				boolean all_right=true;
				boolean all_above=true;
				boolean all_below=true;
				int l=as.points*/
				ArrayList<Vector> vs=new ArrayList<Vector>();
				for(LatLon latlon : as.points)
				{
					Merc m=Project.latlon2merc(latlon,zoomlevel);
					Vector v=tf.merc2screen(m);
					vs.add(v);
				}
				for(int i=0;i<vs.size();++i)
				{
					Vector a=vs.get(i);
					Vector b=vs.get((i+1)%vs.size());
					canvas.drawLine((float)a.getx(),(float)a.gety(),(float)b.getx(),(float)b.gety(),linepaint);
				}
			}
		}
		if (zoomlevel>=9 && lookup!=null)
		{
			for(SigPoint sp : lookup.allOthers.findall(bb13))
			{
				//Merc m=Project.merc2merc(sp.pos,13,zoomlevel);
				Merc m=Project.latlon2merc(sp.latlon,zoomlevel);
				//new Merc(sp.pos.x/(1<<zoomgap),
				//		sp.pos.y/(1<<zoomgap));
				Vector p=tf.merc2screen(m);
				//Log.i("fplan",String.format("dxsigp: %s: %f %f",sp.name,px,py));
				//textpaint.setARGB(0, 255,255,255);
				textpaint.setARGB(0xff, 0xff, 0xa0, 0xa0);
				linepaint.setARGB(0xff, 0xff, 0xa0, 0xa0);
				renderText(canvas, p, sp.name);
			}
	
			for(SigPoint sp : lookup.allObst.findall(smbb13))
			{
				/*
				double x=sp.pos.x/(1<<zoomgap);
				double y=sp.pos.y/(1<<zoomgap);
				//Log.i("fplan",String.format("sigp: %s: %f %f",sp.name,sp.pos.x,sp.pos.y));
				double px=rot_x(x-center.x,y-center.y)+ox;
				double py=rot_y(x-center.x,y-center.y)+oy;
				*/
				//Merc m=Project.merc2merc(sp.pos,13,zoomlevel);
				Merc m=Project.latlon2merc(sp.latlon,zoomlevel);
				Vector p=tf.merc2screen(m);
				
				//Log.i("fplan",String.format("dxsigp: %s: %f %f",sp.name,px,py));
				//textpaint.setARGB(0, 255,255,255);
				textpaint.setARGB(0xff, 0xff, 0xa0, 0xff);
				linepaint.setARGB(0xff, 0xff, 0xa0, 0xff);
				renderText(canvas, p, String.format("%.0fft",sp.alt));
				//canvas.drawText(String.format("%.0fft",sp.alt), (float)(p.x), (float)(p.y), textpaint);			
				//canvas.drawPoint((float)p.x,(float)p.y,linepaint);
			}
		}
		if (zoomlevel>=9 && lookup!=null)
		{
			for(SigPoint sp : lookup.allAirfields.findall(bb13))
			{
				//Merc m=Project.merc2merc(sp.pos,13,zoomlevel);
				Merc m=Project.latlon2merc(sp.latlon,zoomlevel);
				Vector p=tf.merc2screen(m);
				String text;
				if (zoomlevel>=9)
					text=sp.name;
				else
					text=sp.name;
				//Log.i("fplan",String.format("airf: %s: %f %f",sp.name,p.x,p.y));
				
				textpaint.setColor(Color.GREEN);
				linepaint.setColor(Color.GREEN);
				renderText(canvas, p, text);
			}
		}
		
		if (tripdata!=null && tripdata.waypoints.size()>=2)
		{
			float[] lines=new float[4*(tripdata.waypoints.size()-1)];
			int curwp=-1;
			if (tripstate!=null)
				curwp=tripstate.get_target();
			for(int i=0;i<tripdata.waypoints.size()-1;++i)
			{
				//String part=tripdata.waypoints.get(i+1).legpart;
				if (i==0)
				{
					Waypoint wp1=tripdata.waypoints.get(i);
					Merc m1=Project.latlon2merc(wp1.latlon,zoomlevel);
					Vector p1=tf.merc2screen(m1);
					lines[4*i]=(float) p1.x;
					lines[4*i+1]=(float) p1.y;
				}
				else
				{
					lines[4*i]=lines[4*i-2];
					lines[4*i+1]=lines[4*i-1];
				}				
				Waypoint wp2=tripdata.waypoints.get(i+1);
				Merc m2=Project.latlon2merc(wp2.latlon,zoomlevel);
				Vector p2=tf.merc2screen(m2);
				lines[4*i+2]=(float) p2.x;
				lines[4*i+3]=(float) p2.y;
		
			}
			for(int i=0;i<tripdata.waypoints.size()-1;++i)
			{
				Paint p;
				if (i==curwp-1) p=trippaint;
				else p=trippaint;
				canvas.drawLine(
						lines[4*i+0],
						lines[4*i+1],
						lines[4*i+2],
						lines[4*i+3],						
						p);	
			}
			textpaint.setColor(Color.WHITE);
			for(Waypoint wp : tripdata.waypoints)
			{
				if (wp.lastsub==0)
					continue; //Only draw actual waypoints, not climb- or descent-events. 
				Merc m=Project.latlon2merc(wp.latlon,zoomlevel);
				Vector p=tf.merc2screen(m);
				//double px=rot_x(m.x-center.x,m.y-center.y)+ox;
				//double py=rot_y(m.x-center.x,m.y-center.y)+oy;
				textpaint.setColor(Color.WHITE);
				linepaint.setColor(Color.WHITE);
				renderText(canvas, p, wp.name);
			}
		}
		boolean havefix=lastpos.getTime()>3600*24*10*1000 && SystemClock.uptimeMillis()-last_real_position<5000;
		
		if (!havefix)
		{
			linepaint.setStrokeWidth(15);
			linepaint.setARGB(190,255,200,128);
			canvas.drawLine(getLeft(),getTop(),getRight(),getBottom(),linepaint);
			canvas.drawLine(getLeft(),getBottom(),getRight(),getTop(),linepaint);
			linepaint.setStrokeWidth(5);
			linepaint.setColor(Color.RED);			
		}
		
		if (tripstate!=null)
		{
			WarningEvent we=tripstate.getCurrentWarning();
			if (we!=null)
			{
				
				Merc me=Project.merc2merc(new Merc(we.getPoint()),13,zoomlevel);
				if (me!=null)
				{
					//float px=(float)rot_x(p.getx()-center.x,p.gety()-center.y)+ox;
					//float py=(float)rot_y(p.getx()-center.x,p.gety()-center.y)+oy;
					Vector p=tf.merc2screen(me);
					thinlinepaint.setColor(Color.BLUE);
					canvas.drawCircle((float)p.x,(float)p.y,10.0f,thinlinepaint);
				}
				
				float tsy=bigtextpaint.getTextSize()+2;
				float y=this.getBottom();//
				float rx=this.getRight();
				bigtextpaint.setColor(Color.WHITE);
				int when=we.getWhen();
				String whenstr;
				//Log.i("fplan","When: "+when);
				whenstr=fmttime(when);
				
/*				
#error: Move position into base class of WarningEvent
#Then move calculation of distance also to baseclass.
#don't store time and distance, just store position.
#Let moving map calculate time, based on distance (or even based on position?)
*/			
				
				String[] details;
				if (extrainfo)
					details=we.getExtraDetails();
				else
					details=we.getDetails();
				int lines=1+details.length;
				int y1=(int)(y-tsy*(lines-1))-2;
				
				RectF r=new RectF(0, getBottom()-tsy*lines-4, getRight(), getBottom());
				canvas.drawRect(r, backgroundpaint);
				addTextIfFits(canvas, "1222.2nm", r, String.format("%.1fnm",we.getDistance()), y1, bigtextpaint);
				//canvas.drawText(String.format("%.1fnm",we.getDistance()), 2,y1,bigtextpaint);
				addTextIfFits(canvas, "T:22:22", r, whenstr, y1, bigtextpaint);
				//canvas.drawText(String.format("T:%s",whenstr), 70,y1,bigtextpaint);
				addTextIfFits(canvas, null, r, we.getTitle(), y1, bigtextpaint);
				//canvas.drawText(we.getTitle(), 140,y1,bigtextpaint);
				for(int i=0;i<lines-1;++i)
				{
					canvas.drawText(details[i], 2,y1+tsy+i*tsy,bigtextpaint);					
				}
				//canvas.drawText(String.format("%03.0f°",lastpos.getBearing()), 50, y, bigtextpaint);
				//canvas.drawText(String.format("%.0fkt",lastpos.getSpeed()*3.6/1.852),150,y,textpaint);
				
			}
		}
		
		if (drag_center==null)
		{
			THe drag-stuff must be in zoomlevel independent units!!!
			Also, we should draw an arrow even when dragging. Anything else is inconsistent.
			arrowpaint.setColor(Color.BLACK);
			Path path=new Path();
			path.moveTo((int)arrow.x-10,(int)arrow.y+2);
			path.lineTo((int)arrow.x+10,(int)arrow.y+2);
			path.lineTo((int)arrow.x,(int)arrow.y-15);
			path.close();
			canvas.drawPath(path,arrowpaint);
			canvas.drawRect((int)arrow.x-2,(int)0,(int)arrow.x+2,(int)arrow.y,arrowpaint);
			arrowpaint.setColor(Color.WHITE);
			path=new Path();
			path.moveTo((int)arrow.x-7,(int)arrow.y);
			path.lineTo((int)arrow.x+7,(int)arrow.y);
			path.lineTo((int)arrow.x,(int)arrow.y-12);
			path.close();
			canvas.drawPath(path,arrowpaint);
			canvas.drawRect((int)arrow.x-1,(int)0,(int)arrow.x+1,(int)arrow.y,arrowpaint);
		}
		
		else
		{
			if (lastpos!=null)
			{
				Merc pos=Project.latlon2merc(new LatLon(lastpos.getLatitude(),lastpos.getLongitude()),zoomlevel);
				Merc dest;
				if (lastpos!=null && lastpos.hasBearing())
				{
					float hdg=lastpos.getBearing();
					Vector d=new Vector(0,-(50<<(13-zoomlevel)));
					Vector d2=d.rot((hdg)/(180.0f/Math.PI));					
					dest=new Merc(pos.x+d2.x,pos.y+d2.y);
				}
				else
				{
					dest=pos;
				}
				Vector screenpos=tf.merc2screen(pos);
				Vector screenpos2=tf.merc2screen(dest);
				arrowpaint.setColor(Color.BLACK);
				arrowpaint.setStrokeWidth(4);
				canvas.drawCircle((float)screenpos.x, (float)screenpos.y, 9, arrowpaint);
				canvas.drawLine((float)screenpos.x,(float)screenpos.y,(float)screenpos2.x,(float)screenpos2.y,arrowpaint);
				arrowpaint.setColor(Color.WHITE);
				arrowpaint.setStrokeWidth(2);
				canvas.drawCircle((float)screenpos.x, (float)screenpos.y, 7, arrowpaint);
				canvas.drawLine((float)screenpos.x,(float)screenpos.y,(float)screenpos2.x,(float)screenpos2.y,arrowpaint);

			}
			
		}
		
		linepaint.setColor(Color.RED);
		float y=bigtextpaint.getTextSize();
		bigtextpaint.setColor(Color.WHITE);
		RectF r=new RectF(0, 0, getRight(), y+2);
		canvas.drawRect(r, backgroundpaint);
		addTextIfFits(canvas,"222°.",r,String.format("%03.0f°",lastpos.getBearing()),y,bigtextpaint);
		addTextIfFits(canvas,"222kt",r,String.format("%.0fkt",lastpos.getSpeed()*3.6/1.852),y,bigtextpaint);
		
		//canvas.drawText(String.format("%03.0f°",lastpos.getBearing()), 40, y, bigtextpaint);
		//canvas.drawText(String.format("%.0fkt",lastpos.getSpeed()*3.6/1.852),100,y,bigtextpaint);
		int td=tripstate.get_time_to_destination();
		//canvas.drawText(fmttime(td),150,y,bigtextpaint);
		addTextIfFits(canvas,"122:22",r,fmttime(td),y,bigtextpaint);
		if (havefix) //if significantly after 1970-0-01
		{
			Date d=new Date(lastpos.getTime());
			SimpleDateFormat formatter = new SimpleDateFormat("hhmmss");
			//canvas.drawText("FIX:"+formatter.format(d)+"Z",220,y,bigtextpaint);
			addTextIfFits(canvas,"FIX:222222Z",r,"FIX:"+formatter.format(d)+"Z",y,bigtextpaint);
		}
		else
		{
			//canvas.drawText("NOFIX",220,y,bigtextpaint);
			addTextIfFits(canvas,"FIX:222222Z",r,"NOFIX",y,bigtextpaint);
		}
		///canvas.drawText(String.format("Z%d",zoomlevel), 0,y,bigtextpaint);
		addTextIfFits(canvas,"Z13",r,String.format("Z%d",zoomlevel),y,bigtextpaint);
		
		if (download_status!=null && !download_status.equals(""))
		{
			float y2=(y+bigtextpaint.getTextSize()*1.1f);
			canvas.drawRect(0, y+2, getRight(), y2, backgroundpaint);
			canvas.drawText("Download:"+download_status,getLeft()+3,y2,bigtextpaint);			
		}
		
	}
	private void addTextIfFits(Canvas canvas,String sizetext, RectF r, String realtext,float y,
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
	private void renderText(Canvas canvas, Vector p, String text) {
		Rect rect=new Rect();					
		textpaint.getTextBounds(text, 0, text.length(),rect);
		int adj=(rect.bottom-rect.top)/3;
		rect.left+=p.x-1+7;
		rect.right+=p.x+1+7;
		rect.top+=p.y-2+adj;
		rect.bottom+=p.y+3+adj;
		canvas.drawRect(rect, backgroundpaint);
		canvas.drawText(text, (float)(p.x+7), (float)(p.y+adj), textpaint);
		linepaint.setStrokeWidth(10);
		int c=linepaint.getColor();
		linepaint.setColor(Color.BLACK);
		canvas.drawPoint((float)p.x,(float)p.y,linepaint);
		linepaint.setColor(c);
		linepaint.setStrokeWidth(5);
		canvas.drawPoint((float)p.x,(float)p.y,linepaint);
	}
	private String fmttime(int when) {
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
		if (tripstate!=null)
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
			/*
			if (i==-1)
				tripstate.left();
			if (i==1)
				tripstate.right();
			*/
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
	
	
	enum GuiState
	{
		IDLE, //Normal
		MAYBE_DRAG, //about to start dragging
		DRAGGING
	}
	GuiState state=GuiState.IDLE;
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
	float dragstartx,dragstarty;	
	Merc drag_center;
	Merc drag_base;
	float drag_heading;
	
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
			float thresh=5.0f*x_dpmm;
			if (dist>thresh*thresh)
			{
				drag_base=tf.getPos();
				drag_heading=tf.getHdg();
				state=GuiState.DRAGGING;
			}
		}
			break;
		case DRAGGING:
			float deltax1=x-dragstartx;
			float deltay1=y-dragstarty;
			float hdgrad=tf.getHdgRad();
			float deltax=(float)(Math.cos(hdgrad)*deltax1-Math.sin(hdgrad)*deltay1);
			float deltay=(float)(Math.sin(hdgrad)*deltax1+Math.cos(hdgrad)*deltay1);
			drag_center=new Merc(drag_base.x-deltax,drag_base.y-deltay);
			invalidate();
			break;
		}
		
	}
	
}
