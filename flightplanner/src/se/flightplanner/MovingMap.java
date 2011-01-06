package se.flightplanner;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

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
import android.graphics.Typeface;
import android.graphics.Paint.Style;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.location.Location;
import android.location.LocationManager;
import android.text.format.Time;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class MovingMap extends View {
	private TripData tripdata;
	private TripState tripstate;
	private AirspaceLookup lookup;
	private Location mylocation;
	private Location lastpos;
	private int background;
	private int foreground;
	private int zoomlevel;
	private Paint textpaint;
	private Paint linepaint;
	private Paint thinlinepaint;
	private Paint trippaint;
	private Paint seltrippaint;
	private Paint arrowpaint;
	private Paint backgroundpaint;
	BearingSpeedCalc bearingspeed;
	//private CountDownTimer timer;
	//private TimeZone utctz;
	
	@Override
	public boolean onTouchEvent(MotionEvent ev)
	{
		float x=ev.getX();
		float y=ev.getY();
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
			if (lastpos!=null)
			{
				Transform tf = getTransform();
				Merc m=tf.screen2merc(new Vector(x,y));
				LatLon point=Project.merc2latlon(m,zoomlevel);
				tripstate.showInfo(point,new LatLon(lastpos.getLatitude(),lastpos.getLongitude()));
				invalidate();
			}
		}
		return false;
	}
	private Transform getTransform() {
		if (lastpos!=null)
		{
			Merc mypos=Project.latlon2merc(new LatLon(lastpos.getLatitude(),lastpos.getLongitude()),zoomlevel);
			double hdg=0;
			if (lastpos!=null && lastpos.hasBearing())
			{
				hdg=lastpos.getBearing();
			}		
			return new Transform(mypos,getArrow(),hdg,zoomlevel);
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
	GetMapBitmap bitmaps;
	public MovingMap(Context context)
	{
		super(context);
		Blob blob;
		String path="/sdcard/level10";
		try {
			blob = new Blob(path,256);
			bitmaps=new GetMapBitmap(blob);
		} catch (IOException e) {
			System.out.println("Failed opening terrain bitmap. Check file:"+path);
			e.printStackTrace();
		}
		
		bearingspeed=new BearingSpeedCalc();
		//utctz = TimeZone.getTimeZone("UTC");		 
		zoomlevel=9;
		background=Color.BLACK;
		foreground=Color.WHITE;
		textpaint = new Paint();
		textpaint.setAntiAlias(true);
		textpaint.setStrokeWidth(5);
		textpaint.setColor(foreground);
		textpaint.setStrokeCap(Paint.Cap.ROUND);
		textpaint.setTextSize(16);
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
		mylocation=null;
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
		else if (mylocation==null)
		{
			//canvas.drawText("No GPS-fix.", this.getLeft(), this.getTop()+textpaint.getTextSize(), textpaint);			
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
				double hdg,
				int zoomlevel)
		{
			this.mypos=mypos;
			this.hsizex=arrow.x;
			this.hsizey=arrow.y;
			this.hdgrad=hdg*(Math.PI/180.0);
			this.zoomlevel=zoomlevel;
		}
		double hsizex; //x position of observer in screen coordinates
		double hsizey; //y position of observer in screen coordinates
		Merc mypos; //Position of user
		double hdgrad; //heading of user, in radians
		int zoomlevel;
		/// Convert from merc to screen coordinates with 
		/// north up on map
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
	private void draw_actual_map(Canvas canvas, int sizex, int sizey) {
		if (zoomlevel>13) throw new RuntimeException("zoomlevel must be <=13");
		//int zoomgap=13-zoomlevel;
		//Merc mypos=Project.latlon2merc(new LatLon(lastpos.getLatitude(),lastpos.getLongitude()),zoomlevel);
		Merc mypos13=Project.latlon2merc(new LatLon(lastpos.getLatitude(),lastpos.getLongitude()),13);
		Transform tf = getTransform();
				
		Vector arrow=getArrow();
		Merc screen_center=tf.screen2merc(new Vector(sizex/2,sizey/2));
		Merc screen_center13=Project.merc2merc(
				screen_center,
				zoomlevel,
				13);		
		double fivenm13=Project.approx_scale(mypos13.y,13,10);
		double diagonal13;		
		{
			int zoomgap13=13-zoomlevel;
			diagonal13=((1<<zoomgap13)*(Math.sqrt(arrow.x*arrow.x+arrow.y*arrow.y)+50))+1;
		}
		BoundingBox bb13=new BoundingBox(
				screen_center13.x,screen_center13.y,
				screen_center13.x,screen_center13.y).expand(diagonal13);

		BoundingBox smbb13=new BoundingBox(mypos13.x,mypos13.y,
				mypos13.x,mypos13.y).expand(fivenm13);
				
		//bb13=new BoundingBox(-1e20,-1e20,1e20,1e20);		
		/*ShapeDrawable mDrawable = new ShapeDrawable(new OvalShape());
		mDrawable.getPaint().setColor(0xff74AC23);
		mDrawable.setBounds(x, y, x + width, y + height);
		mDrawable.draw(canvas);*/
		if (zoomlevel==10 && bitmaps!=null)
		{
			iMerc topleft=new iMerc(
					(int)screen_center.x&(~255),
					(int)screen_center.y&(~255));
			topleft.x-=256;
			topleft.y-=256;
			canvas.save();
			Vector v=tf.merc2screen(
					new Merc(topleft.x,topleft.y));
			float hdg=(float)(tf.hdgrad*(180.0/Math.PI));
			canvas.rotate(hdg,(float)(v.x),(float)(v.y));
			for(int j=0;j<3;++j)
			{
				for(int i=0;i<3;++i)
				{
					iMerc cur=new iMerc(topleft.x+256*i,topleft.y+256*j);
					Bitmap b=null;
					try {
						b = bitmaps.getBitmap(cur);
					} catch (IOException e) {
						e.printStackTrace();
					}
					if (b!=null)
					{
						canvas.drawBitmap(b, (float)(v.x+i*256), (float)(v.y+j*256), null);						
					}
				}
			}
			canvas.restore();
			
		}
		
		
		//sigPointTree.verify();
		if (zoomlevel>=8)
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
		if (zoomlevel>=9)
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
				canvas.drawText(sp.name, (float)(p.x), (float)(p.y), textpaint);			
				linepaint.setARGB(0xff, 0xff, 0xa0, 0xa0);
				canvas.drawPoint((float)p.x,(float)p.y,linepaint);
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
				canvas.drawText(String.format("%.0fft",sp.alt), (float)(p.x), (float)(p.y), textpaint);			
				linepaint.setARGB(0xff, 0xff, 0xa0, 0xff);
				canvas.drawPoint((float)p.x,(float)p.y,linepaint);
			}
			
			for(SigPoint sp : lookup.allAirfields.findall(bb13))
			{
				//Merc m=Project.merc2merc(sp.pos,13,zoomlevel);
				Merc m=Project.latlon2merc(sp.latlon,zoomlevel);
				Vector p=tf.merc2screen(m);
				//Log.i("fplan",String.format("airf: %s: %f %f",sp.name,p.x,p.y));
				
				textpaint.setColor(Color.GREEN);
				canvas.drawText(sp.name, (float)(p.x), (float)(p.y), textpaint);
				linepaint.setColor(Color.GREEN);
				canvas.drawPoint((float)p.x,(float)p.y,linepaint);
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
				if (i==curwp-1) p=seltrippaint;
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
				canvas.drawText(wp.name, (float)(p.x), (float)(p.y), textpaint);
			}
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
				
				float tsy=textpaint.getTextSize()+2;
				float y=this.getBottom();//
				float rx=this.getRight();
				textpaint.setColor(Color.WHITE);
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
				
				canvas.drawRect(0, y-tsy*lines-4, rx, y, backgroundpaint);
				canvas.drawText(String.format("%.1fnm",we.getDistance()), 2,y1,textpaint);
				canvas.drawText(String.format("T:%s",whenstr), 70,y1,textpaint);
				canvas.drawText(we.getTitle(), 140,y1,textpaint);
				for(int i=0;i<lines-1;++i)
				{
					canvas.drawText(details[i], 2,y1+tsy+i*tsy,textpaint);					
				}
				//canvas.drawText(String.format("%03.0f°",lastpos.getBearing()), 50, y, textpaint);
				//canvas.drawText(String.format("%.0fkt",lastpos.getSpeed()*3.6/1.852),150,y,textpaint);
				
			}
		}

		linepaint.setColor(Color.RED);
		float y=textpaint.getTextSize();
		textpaint.setColor(Color.WHITE);
		canvas.drawRect(0, 0, getRight(), y+2, backgroundpaint);
		canvas.drawText(String.format("Z%d",zoomlevel), 0,y,textpaint);
		canvas.drawText(String.format("%03.0f°",lastpos.getBearing()), 40, y, textpaint);
		canvas.drawText(String.format("%.0fkt",lastpos.getSpeed()*3.6/1.852),100,y,textpaint);
		int td=tripstate.get_time_to_destination();
		canvas.drawText(fmttime(td),150,y,textpaint);
		if (lastpos.getTime()>3600*24*10*1000) //if significantly after 1970-0-01
		{
			Date d=new Date(lastpos.getTime());
			SimpleDateFormat formatter = new SimpleDateFormat("hhmmss");
			canvas.drawText("FIX:"+formatter.format(d)+"Z",220,y,textpaint);
		}
		else
		{
			canvas.drawText("NOFIX",220,y,textpaint);
		}
		
		
		Path path=new Path();
		path.moveTo((int)arrow.x-7,(int)arrow.y);
		path.lineTo((int)arrow.x+7,(int)arrow.y);
		path.lineTo((int)arrow.x,(int)arrow.y-12);
		path.close();
		canvas.drawPath(path,arrowpaint);
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
		if (tripstate!=null)
			tripstate.update_target(lastpos);
		invalidate();
	}
	public void gps_disabled() {
		mylocation=null;
		invalidate();		
	}

	public void zoom(int zd) {
		zoomlevel+=zd;
		if (zoomlevel<0)
			zoomlevel=0;
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
			if (i==1)
				tripstate.right();
			invalidate();
		}
		
	}
}
