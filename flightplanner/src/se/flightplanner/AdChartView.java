package se.flightplanner;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import se.flightplanner.BackgroundMapLoader.UpdatableUI;
import se.flightplanner.GetMapBitmap.BitmapRes;
import se.flightplanner.Project.LatLon;
import se.flightplanner.Project.iMerc;
import se.flightplanner.SigPoint.Chart;
import se.flightplanner.vector.Vector;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.location.Location;
import android.os.Environment;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewDebug.FlagToString;

public class AdChartView extends View implements UpdatableUI {

	private MapCache mapcache;
	private BackgroundMapLoader loader;
	private ArrayList<Blob> blobs;
	private GetMapBitmap bitmaps;
	private Chart chart;
	private int level;
	
	private enum State
	{
		IDLE,
		BEGIN_DRAG,
		DRAGGING
	}
	private State state=State.IDLE;
	private float downx;
	private float downy;
	private int start_scroll_x;
	private int start_scroll_y;
	private int scroll_x;
	private int scroll_y;

	private int chart_width;
	private int chart_height;
	private Runnable curLostSignalRunnable;
	private Handler lostSignalTimer;
	
	private int last_width; //last screen width
	private int last_height;  //last screen height
	
	private Paint pospaint;
	private Vector userPosition;
	private Float userHdgRad;
	
	
	@Override
	public boolean onTouchEvent(MotionEvent ev)
	{
		
		float x=ev.getX();
		float y=ev.getY();
		Log.i("fplan.chart","Dragging"+x+" "+y+": "+ev.getAction());
		if (ev.getAction()==MotionEvent.ACTION_MOVE || ev.getAction()==MotionEvent.ACTION_DOWN)
		{
			if (state==State.IDLE)
			{
				state=State.BEGIN_DRAG;
				downx=x;
				downy=y;
				start_scroll_x=scroll_x;
				start_scroll_y=scroll_y;
			}
			else if (state==State.BEGIN_DRAG)
			{
				if ((x-downx)*(x-downx)+(y-downy)*(y-downy) > 20*20)
					state=State.DRAGGING;										
			}
			else
			{				
				scroll_x=(int)(start_scroll_x-(x-downx));
				scroll_y=(int)(start_scroll_y-(y-downy));
				clamp_scroll();
				invalidate();
			}
		}
		if (ev.getAction()==MotionEvent.ACTION_UP)
		{
			if (state==State.BEGIN_DRAG)
			{
				int nlevel=(level+2)%3;
				zoom(scroll_x+(int)downx,scroll_y+(int)downy,nlevel);
				state=State.IDLE;
				invalidate();
			}
			if (state==State.DRAGGING)
			{
				state=State.IDLE;
				scroll_x=(int)(start_scroll_x-(x-downx));
				scroll_y=(int)(start_scroll_y-(y-downy));
				

				clamp_scroll();
				
				invalidate();
			}
		}
		return true;
			
	}

	private void zoom(int x, int y, int newlevel) {
		if (newlevel<0) newlevel=0;
		int delta=newlevel-level;
		if (delta==0) return;
		if (delta>0)
		{
			scroll_x+=last_width/2;
			scroll_y+=last_height/2;
			scroll_x>>=delta;
			scroll_y>>=delta;
			scroll_x-=last_width/2;
			scroll_y-=last_height/2;
		}
		else 
		{
			scroll_x=x;
			scroll_y=y;
			scroll_x<<=-delta;
			scroll_y<<=-delta;
			scroll_x-=last_width/2;
			scroll_y-=last_height/2;
		}
		level=newlevel;
		clamp_scroll();
	}

	private void clamp_scroll() {
		int maxx=(chart_width>>level)-last_width+50;
		int maxy=(chart_height>>level)-last_height+50;
		if (scroll_x>maxx) scroll_x=maxx;
		if (scroll_y>maxy) scroll_y=maxy;
		if (scroll_x<0) scroll_x=0;
		if (scroll_y<0) scroll_y=0;
	}
	private BearingSpeedCalc calc;

	public AdChartView(Context context,Chart chart) throws IOException {
		super(context);
		loader=null;
		this.chart=chart;
		mapcache=new MapCache();
		level=2;
		blobs=new ArrayList<Blob>();
		bitmaps=new GetMapBitmap(mapcache,blobs.size()+1);
		File extpath = Environment.getExternalStorageDirectory();
		curLostSignalRunnable=null;
		lostSignalTimer=new Handler();
		chart_width=chart.width;
		chart_height=chart.height;
		calc=new BearingSpeedCalc();
		pospaint=new Paint();
		pospaint.setStyle(Style.FILL_AND_STROKE);		
		pospaint.setARGB(128, 0, 0, 255);
		pospaint.setStrokeWidth(2.0f);
		
		File chartprojpath = new File(extpath,
				"/Android/data/se.flightplanner/files/"+chart.name+".proj");
		DataInputStream ds=new DataInputStream(
				new FileInputStream(chartprojpath));
		chart.A=new double[2][2];
		chart.T=new double[2];
		boolean isfloat=(chartprojpath.length()==6*4);
		if (isfloat)
		{
			chart.A[0][0]=ds.readFloat();
			chart.A[1][0]=ds.readFloat();
			chart.A[0][1]=ds.readFloat();
			chart.A[1][1]=ds.readFloat();
			chart.T[0]=ds.readFloat();
			chart.T[1]=ds.readFloat();
		}
		else
		{
			chart.A[0][0]=ds.readDouble();
			chart.A[1][0]=ds.readDouble();
			chart.A[0][1]=ds.readDouble();
			chart.A[1][1]=ds.readDouble();
			chart.T[0]=ds.readDouble();
			chart.T[1]=ds.readDouble();			
		}
		ds.close();
		Log.i("fplan.adchart","loaded matrix:"+chart.A[0][0]+","+chart.A[1][0]+","+chart.A[0][1]+","+chart.A[1][1]);
		Log.i("fplan.adchart","loaded vector:"+chart.T[0]+", "+chart.T[1]);
		
		for(int i=0;i<5;++i)
		{
			Integer is=new Integer(i);

			File chartpath = new File(extpath,
					"/Android/data/se.flightplanner/files/"+chart.name+"-"+is.toString()+".bin");
			Blob blob=new Blob(chartpath.getAbsolutePath(),256);
			blobs.add(blob);
			Log.i("fplan.adchart","Dimensions of level "+i+" "+
					"x1:"+blob.getX1()+
					"y1:"+blob.getY1()+
					"x2:"+blob.getX2()+
					"y2:"+blob.getY2()
					);
		}
		
	}
	@Override
	protected void onDraw(Canvas canvas) {
		
		int width=canvas.getWidth();		
		int height=canvas.getHeight();
		last_width=width;
		last_height=height;
		int required_cachesize=((width+255+255)/256)*((height+255+255)/256);
		//TODO: Limit required cache size to never be bigger than total number of available tiles.
				
		Log.i("fplan.adchart","Re-drawing chart!");
		mapcache.forgetqueries();
		
		
		
		for(int x=0;x<chart_width>>level;x+=256)
		{
			for(int y=0;y<chart_height>>level;y+=256)
			{
				int tx=x-scroll_x;
				int ty=y-scroll_y;
				if (tx>-256 && ty>-256 && tx<width && ty<height)
				{
					iMerc pos=new iMerc(x,y);
					BitmapRes b = bitmaps.getBitmap(pos, level);
					if (b!=null)
					{
						canvas.drawBitmap(b.b, tx,ty,null);					
					}
				}
			}
		}		
		
		if (mapcache.haveUnsatisfiedQueries())
		{
			if (loader==null)
			{
				loader=new BackgroundMapLoader(blobs, mapcache, this,required_cachesize);
				loader.run();
				Log.i("fplan.adchart","Start a background task again");
			}
			else
			{
				Log.i("fplan.adchart","Map download still in progress");
			}
		}
		
		if (userPosition!=null)
		{
			float px=((int)userPosition.x>>level)-scroll_x;
			float py=((int)userPosition.y>>level)-scroll_y;
			if (userHdgRad!=null)
			{
				float rad=userHdgRad;
				px+=20.0f*Math.cos(rad);
				py+=20.0f*Math.sin(rad);
				RectF r=new RectF(px-40,py-40,px+40,py+40);
				canvas.drawArc(r,rad*(float)(180.0/Math.PI)+180.0f-20f,40,true,pospaint);
				//canvas.drawText("Hdg:"+rad*180/Math.PI, px+50, py+50, pospaint);
			}
			else
			{
				canvas.drawCircle(px,py, 15, pospaint);
			}
		}
		
	
	}
	@Override
	public void updateUI(boolean done) {
		loader=null;
		Log.i("fplan.adchart","Background task finished");
		invalidate();
	}

	double offlat,offlon;
	public void update_location(Location inputlocation) {
		
		Location location=calc.calcBearingSpeed(inputlocation);
		
		double lat=location.getLatitude();
		double lon=location.getLongitude();
		if (DataDownloader.debugMode())
		{
			//Forcefully move us to middle of Arlanda airport,
			//So that we can easily test moving around there without
			//actually being there :-).
			if (offlat==0)
			{
				offlat=lat;
				offlon=lon;
			}
			lat=lat-offlat+59.649583;
			lon=lon-offlon+17.935867;
		}
		//        (lat)
		//P = A * (   )
		//        (lon)
		//
		//Px = A[0][0]*lat + A[0][1]*lon
		//Py = A[1][0]*lat + A[1][1]*lon
		{
			double mlat=lat-chart.T[0];
			double mlon=lon-chart.T[1];
			double px=chart.A[0][0]*mlat + chart.A[0][1]*mlon; 
			double py=chart.A[1][0]*mlat + chart.A[1][1]*mlon;
			userPosition=new Vector(px,py);
		}
		userHdgRad=null;
		if (location.hasBearing())
		{
			Log.i("fplan.adchart","Has bearing:"+location.getBearing());
			float hdg=location.getBearing();
			double aimlat=lat+1e-3*Math.cos(hdg/(180.0/Math.PI));
			double aimlon=lon+1e-3*Math.sin(hdg/(180.0/Math.PI));
			double aimmlat=aimlat-chart.T[0];
			double aimmlon=aimlon-chart.T[1];
			double aimpx=chart.A[0][0]*aimmlat + chart.A[0][1]*aimmlon; 
			double aimpy=chart.A[1][0]*aimmlat + chart.A[1][1]*aimmlon;
			double dx=aimpx-userPosition.x;
			double dy=aimpy-userPosition.y;
			userHdgRad=(float)Math.atan2(dy,dx);
			
		}
		Log.i("fplan.adchart","Position: "+lat+", "+lon);
		Log.i("fplan.adchart","Pixels: "+userPosition.x+", "+userPosition.y);
		if (curLostSignalRunnable!=null)
			lostSignalTimer.removeCallbacks(curLostSignalRunnable);
		
		curLostSignalRunnable=new Runnable() {			
			@Override	
			public void run() {
				no_location_fix();
			}
		};
		lostSignalTimer.postDelayed(curLostSignalRunnable, 5000);	
		invalidate();

		
	}

	public void no_location_fix() {
		// TODO Auto-generated method stub
		userPosition=null;
		invalidate();
	}
		


}
