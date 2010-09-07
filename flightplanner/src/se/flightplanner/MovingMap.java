package se.flightplanner;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import se.flightplanner.Project.LatLon;
import se.flightplanner.Project.Merc;
import se.flightplanner.TripData.Waypoint;
import se.flightplanner.TripState.WarningEvent;
import se.flightplanner.vector.BoundingBox;
import se.flightplanner.vector.Vector;
import android.content.Context;
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
	private Airspace airspace;
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
	private TimeZone utctz;
	
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
		}
		return false;
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
	public MovingMap(Context context)
	{
		super(context);
		
		utctz = TimeZone.getTimeZone("UTC");		 
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
		arrowpaint.setColor(Color.BLUE);
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
	
	
	int hsizex; //x position of observer in screen coordinates
	int hsizey; //y position of observer in screen coordinates
	Merc mypos; //Position of user
	double hdgrad; //heading of user, in radians
	/// Convert from merc to screen coordinates with 
	/// north up on map
	private Vector merc2northscreen(Merc m,int zoomlevel)
	{
		return new Vector(m.x-mypos.x+hsizex,-(m.y-mypos.y)+hsizey);
	}
	/// Convert from screen coordinates with 
	/// north up on map to merc.
	private Merc northscreen2merc(Vector n,int zoomlevel)
	{
		/*
		s.x=m.x-mypos.x+hsizex
		s.y=-m.y+mypos.y+hsizey
		m.x=s.x+mypos.x-hsizex
		m.y=mypos.y+hsizey-s.y
		*/
		return new Merc(n.getx()+mypos.x-hsizex,mypos.y+hsizey-n.gety());
	}
	private Vector northscreen2screen(Vector n,int zoomlevel)
	{
		Vector c=new Vector(n.getx()-hsizex,n.gety()-hsizey);
		Vector r=c.unrot(hdgrad);
		return new Vector(r.getx()+hsizex,r.gety()+hsizey);
	}
	private Vector screen2northscreen(Vector s,int zoomlevel)
	{
		Vector c=new Vector(s.getx()-hsizex,s.gety()-hsizey);
		Vector r=c.rot(hdgrad);
		return new Vector(r.getx()+hsizex,r.gety()+hsizey);
	}
	
	private void draw_actual_map(Canvas canvas, int sizex, int sizey) {
		if (zoomlevel>13) throw new RuntimeException("zoomlevel must be <=13");
		int zoomgap=13-zoomlevel;
		Merc center=Project.latlon2merc(new LatLon(lastpos.getLatitude(),lastpos.getLongitude()),zoomlevel);
		Merc center13=Project.latlon2merc(new LatLon(lastpos.getLatitude(),lastpos.getLongitude()),13);
		int ox=sizex/2;
		int oy=sizey/2;
		hdgrad=0;
		if (lastpos!=null && lastpos.hasBearing())
		{
			hdgrad=(-Math.PI/180.0)*lastpos.getBearing();
		}
		
		double nm=Project.approx_scale(center13.y,13,5);
		double diagonal13=((1<<zoomgap)*(Math.sqrt(ox*ox+oy*oy)+50))+1;
		BoundingBox bb13=new BoundingBox(
				center13.x,center13.y,
				center13.x,center13.y).expand(diagonal13);

		BoundingBox smbb13=new BoundingBox(center13.x,center13.y,
				center13.x,center13.y).expand(nm);
				
		//bb13=new BoundingBox(-1e20,-1e20,1e20,1e20);		
		/*ShapeDrawable mDrawable = new ShapeDrawable(new OvalShape());
		mDrawable.getPaint().setColor(0xff74AC23);
		mDrawable.setBounds(x, y, x + width, y + height);
		mDrawable.draw(canvas);*/

		
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
					double px=rot_x(m.x-center.x,m.y-center.y)+ox;
					double py=rot_y(m.x-center.x,m.y-center.y)+oy;
					vs.add(new Vector(px,py));
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
				double x=sp.pos.x/(1<<zoomgap);
				double y=sp.pos.y/(1<<zoomgap);
				//Log.i("fplan",String.format("sigp: %s: %f %f",sp.name,sp.pos.x,sp.pos.y));
				double px=rot_x(x-center.x,y-center.y)+ox;
				double py=rot_y(x-center.x,y-center.y)+oy;
				//Log.i("fplan",String.format("dxsigp: %s: %f %f",sp.name,px,py));
				//textpaint.setARGB(0, 255,255,255);
				textpaint.setARGB(0xff, 0xff, 0xa0, 0xa0);
				canvas.drawText(sp.name, (float)(px), (float)(py), textpaint);			
				linepaint.setARGB(0xff, 0xff, 0xa0, 0xa0);
				canvas.drawPoint((float)px,(float)py,linepaint);
			}
	
			for(SigPoint sp : lookup.allObst.findall(smbb13))
			{
				double x=sp.pos.x/(1<<zoomgap);
				double y=sp.pos.y/(1<<zoomgap);
				//Log.i("fplan",String.format("sigp: %s: %f %f",sp.name,sp.pos.x,sp.pos.y));
				double px=rot_x(x-center.x,y-center.y)+ox;
				double py=rot_y(x-center.x,y-center.y)+oy;
				//Log.i("fplan",String.format("dxsigp: %s: %f %f",sp.name,px,py));
				//textpaint.setARGB(0, 255,255,255);
				textpaint.setARGB(0xff, 0xff, 0xa0, 0xff);
				canvas.drawText(String.format("%s %.0fft",sp.name,sp.alt), (float)(px), (float)(py), textpaint);			
				linepaint.setARGB(0xff, 0xff, 0xa0, 0xff);
				canvas.drawPoint((float)px,(float)py,linepaint);
			}
			
			for(SigPoint sp : lookup.allAirfields.findall(bb13))
			{
				double x=sp.pos.x/(1<<zoomgap);
				double y=sp.pos.y/(1<<zoomgap);
				//Log.i("fplan",String.format("sigp: %s: %f %f",sp.name,sp.pos.x,sp.pos.y));
				double px=rot_x(x-center.x,y-center.y)+ox;
				double py=rot_y(x-center.x,y-center.y)+oy;
				//Log.i("fplan",String.format("dxsigp: %s: %f %f",sp.name,px,py));
				textpaint.setColor(Color.GREEN);
				canvas.drawText(sp.name, (float)(px), (float)(py), textpaint);
				linepaint.setColor(Color.GREEN);
				canvas.drawPoint((float)px,(float)py,linepaint);
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
				String part=tripdata.waypoints.get(i+1).legpart;
				if (i==0)
				{
					Waypoint wp1=tripdata.waypoints.get(i);
					Merc m1=Project.latlon2merc(wp1.latlon,zoomlevel);
					double x=m1.x-center.x;
					double y=m1.y-center.y;
					lines[4*i]=(float) rot_x(x,y)+ox;
					lines[4*i+1]=(float) rot_y(x,y)+oy;
				}
				else
				{
					lines[4*i]=lines[4*i-2];
					lines[4*i+1]=lines[4*i-1];
				}				
				Waypoint wp2=tripdata.waypoints.get(i+1);
				Merc m2=Project.latlon2merc(wp2.latlon,zoomlevel);
				double x=m2.x-center.x;
				double y=m2.y-center.y;
				lines[4*i+2]=(float) rot_x(x,y)+ox;
				lines[4*i+3]=(float) rot_y(x,y)+oy;
		
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
				double px=rot_x(m.x-center.x,m.y-center.y)+ox;
				double py=rot_y(m.x-center.x,m.y-center.y)+oy;
				canvas.drawText(wp.name, (int)(px), (int)(py), textpaint);
			}
		}
		if (tripstate!=null)
		{
			WarningEvent we=tripstate.getCurrentWarning();
			if (we!=null)
			{
				
				Vector p=Project.merc2merc(we.getPoint(),13,zoomlevel);
				if (p!=null)
				{
					float px=(float)rot_x(p.getx()-center.x,p.gety()-center.y)+ox;
					float py=(float)rot_y(p.getx()-center.x,p.gety()-center.y)+oy;
					thinlinepaint.setColor(Color.BLUE);
					canvas.drawCircle(px,py,10.0f,thinlinepaint);
				}
				
				float tsy=textpaint.getTextSize()+2;
				float y=this.getBottom();//
				float rx=this.getRight();
				textpaint.setColor(Color.WHITE);
				int when=we.getWhen();
				String whenstr;
				Log.i("fplan","When: "+when);
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
		path.moveTo(ox-5,oy);
		path.lineTo(ox+5,oy);
		path.lineTo(ox,oy-10);
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
		if (loc==null)
		{ //just for debug
			loc=new Location("gps");
			//, "alt": 30, "lon": 
			loc.setLatitude(59.458333333299997);
			loc.setLongitude(17.706666666699999);
			loc.setBearing(150);
			loc.setSpeed(50);
		}
		mylocation=loc;
		if (lastpos!=null && (!mylocation.hasSpeed() || !mylocation.hasBearing()))
		{
			Merc prev=Project.latlon2merc(new LatLon(lastpos.getLatitude(),lastpos.getLongitude()),13);
			Merc cur=Project.latlon2merc(new LatLon(mylocation.getLatitude(),mylocation.getLongitude()),13);
			Merc mid=new Merc(0.5*(prev.x+cur.x),0.5*(prev.y+cur.y));
			double time_passed=(mylocation.getTime()-lastpos.getTime())/1000.0;
			double mercs_per_nm=Project.approx_scale(mid, 13, 1.0);
			double dx=cur.x-prev.x;
			double dy=cur.y-prev.y;
			if (!mylocation.hasSpeed())
			{
				double diffmercs=Math.sqrt(dx*dx+dy*dy);
				double dist_nm=diffmercs/mercs_per_nm;
				double dist_m=dist_nm*1852.0;
				double speed=0;
				if (time_passed!=0)
				{				
					speed=dist_m/time_passed;
				}
				else
				{
					speed=lastpos.getSpeed();
				}
				mylocation.setSpeed((float) speed);
			}
			if (!mylocation.hasBearing())
			{
				double tt = Project.vector2heading(dx, dy);
				mylocation.setBearing((float) tt);
			}
		}
		lastpos=mylocation;
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
