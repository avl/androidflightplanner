package se.flightplanner;

import java.util.ArrayList;

import se.flightplanner.Airspace.AirspaceArea;
import se.flightplanner.Project.LatLon;
import se.flightplanner.Project.Merc;
import se.flightplanner.TripData.Waypoint;
import se.flightplanner.vector.Vector;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.Paint.Style;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.location.Location;
import android.location.LocationManager;
import android.view.View;

public class MovingMap extends View {
	private TripData tripdata;
	private Airspace airspace;
	private Location mylocation;
	private Location lastpos;
	private int background;
	private int foreground;
	private int zoomlevel;
	private Paint textpaint;
	private Paint linepaint;
	public MovingMap(Context context)
	{
		super(context);
		zoomlevel=2;
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
		//textpaint.set
		mylocation=null;
		lastpos=null;
		tripdata=null;		
	}
	
	public void update_tripdata(TripData ptripdata)
	{
		tripdata=ptripdata;
		invalidate();
	}
	protected void onDraw(Canvas canvas) {
        canvas.drawColor(background);
		if (tripdata==null)
		{
			canvas.drawText("No trip loaded.", this.getLeft(), this.getTop()+textpaint.getTextSize(), textpaint);
			return;
		}
		else if (mylocation==null)
		{
			canvas.drawText("No GPS-fix.", this.getLeft(), this.getTop()+textpaint.getTextSize(), textpaint);			
		}
		canvas.drawText("Zoom:"+zoomlevel, this.getLeft(), this.getTop()+textpaint.getTextSize()+80, textpaint);			
		if (lastpos!=null)
		{
			canvas.drawText(String.format("Pos: %.4f,%.4f hdg: %03.0f %.1f km/h",lastpos.getLatitude(),lastpos.getLongitude(),lastpos.getBearing(),lastpos.getSpeed()*3.6), this.getLeft(), this.getTop()+textpaint.getTextSize(), textpaint);
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
	private void draw_actual_map(Canvas canvas, int sizex, int sizey) {
		Merc center=Project.latlon2merc(new LatLon(lastpos.getLatitude(),lastpos.getLongitude()),zoomlevel);
		int ox=sizex/2;
		int oy=sizey/2;
		
		for(Waypoint wp : tripdata.waypoints)
		{
			if (wp.lastsub==0)
				continue; //Only draw actual waypoints, not climb- or descent-events. 
			Merc m=Project.latlon2merc(wp.latlon,zoomlevel);
			double px=rot_x(m.x-center.x,m.y-center.y)+ox;
			double py=rot_y(m.x-center.x,m.y-center.y)+oy;
			canvas.drawText(wp.name, (int)(px), (int)(py), textpaint);
		}
		for(AirspaceArea as:airspace.getSpaces())
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
		if (tripdata.waypoints.size()>=2)
		{
			float[] lines=new float[4*(tripdata.waypoints.size()-1)];
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
			canvas.drawLines(lines,linepaint);
		}
	}

	private double rot_x(double x,double y) {
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
	}
	

	public void gps_update(Location loc)
	{
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
				double tt=90-(Math.atan2(-dy,dx)*180.0/Math.PI);
				if (tt<0) tt+=360.0;
				mylocation.setBearing((float) tt);
			}
		}
		lastpos=mylocation;
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
		if (zoomlevel>15)
			zoomlevel=15;		
		invalidate();		
	}

	public void update_airspace(Airspace pairspace) {
		airspace=pairspace;
		invalidate();
	}
}
