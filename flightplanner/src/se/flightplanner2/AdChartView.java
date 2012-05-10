package se.flightplanner2;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import se.flightplanner2.BackgroundMapLoader.UpdatableUI;
import se.flightplanner2.GetMapBitmap.BitmapRes;
import se.flightplanner2.Project.LatLon;
import se.flightplanner2.Project.iMerc;
import se.flightplanner2.vector.Vector;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.location.Location;
import android.os.Environment;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;


public class AdChartView extends View implements UpdatableUI {

	private MapCache mapcache;
	private BackgroundMapLoader loader;
	private ArrayList<Blob> blobs;
	public double[][] A; //2x2 matrix with airport chart projection scale/rotation latlon -> image pixels
	public double[] T; //2 vector with airport chart projection translation
	private GetMapBitmap bitmaps;
	private int level;
	
	private enum State
	{
		IDLE,
		BEGIN_DRAG,
		DRAGGING,
		PINCHING,
		DEAD,
		STONEDEAD
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
	private Vector userPosition=null;
	private Float userHdgRad;
	private float beginPinchDist=1;
	private SimpleOnGestureListener	gest=new SimpleOnGestureListener();
	
	public int get_chart_width(){return chart_width;}
	public int get_chart_height(){return chart_height;}
	
	@Override
	public boolean onTouchEvent(MotionEvent ev)
	{
		float x=ev.getX();
		float y=ev.getY();
		//Log.i("fplan.chart","Dragging"+x+" "+y+": "+ev.getAction()+"state:"+state);
		if (ev.getAction()==MotionEvent.ACTION_MOVE || ev.getAction()==MotionEvent.ACTION_DOWN)
		{
			
			if (ev.getPointerCount()>1)
			{
				
				if (state!=State.STONEDEAD)
				{
					float dx=ev.getX(0)-ev.getX(1);
					float dy=ev.getY(0)-ev.getY(1);
					float dist=(float) Math.sqrt(dx*dx+dy*dy);
					if (state!=State.PINCHING)
					{
						state=State.PINCHING;
						beginPinchDist=dist;
						if (beginPinchDist<1e-3)
						{
							state=State.DEAD;						
						}
					}
					else
					{
						float delta=dist/beginPinchDist;
						int nlevel=level;
						if (delta<0.85)
						{
							nlevel-=1;
							if (nlevel<2)
								nlevel=2;
						}
						else
						if (delta>1.15)
						{
							nlevel+=1;
							if (nlevel>=maxzoomgui)
								nlevel=maxzoomgui;
						}
						if (nlevel!=level)
						{
							float avgx=0.5f*(ev.getX(0)+ev.getX(1));
							float avgy=0.5f*(ev.getY(0)+ev.getY(1));
							zoom(scroll_x+(int)avgx,scroll_y+(int)avgy,nlevel);							
							state=State.STONEDEAD;
							invalidate();
						}
					}
				}
				
			}
			else
			{
				if (state==State.STONEDEAD)
					state=State.DEAD;
				if (state!=State.DEAD)
				{
					if (state==State.PINCHING)
						state=State.DEAD;
					else if (state==State.IDLE)
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
			}
		}
		if (ev.getAction()==MotionEvent.ACTION_UP)
		{
			if (state==State.DEAD || state==State.PINCHING || state==State.STONEDEAD)
				state=State.IDLE;
			if (state==State.BEGIN_DRAG)
			{
				int nlevel=(level+1);
				if (nlevel>maxzoomgui)
					nlevel=2;
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
		if (delta<0)
		{
			scroll_x+=last_width/2;
			scroll_y+=last_height/2;
			scroll_x>>=-delta;
			scroll_y>>=-delta;
			scroll_x-=last_width/2;
			scroll_y-=last_height/2;
		}
		else 
		{
			scroll_x=x;
			scroll_y=y;
			scroll_x<<=delta;
			scroll_y<<=delta;
			scroll_x-=last_width/2;
			scroll_y-=last_height/2;
		}
		level=newlevel;
		clamp_scroll();
	}

	private void clamp_scroll() {
		int maxx=(chart_width>>(maxzoomgui-level))-last_width;
		int maxy=(chart_height>>(maxzoomgui-level))-last_height;
		if (scroll_x>maxx) scroll_x=maxx;
		if (scroll_y>maxy) scroll_y=maxy;
		if (scroll_x<0) scroll_x=0;
		if (scroll_y<0) scroll_y=0;
	}
	private BearingSpeedCalc calc;
	private boolean fail_get_width=true;
	public boolean failed_to_get_width()
	{
		return fail_get_width;
	}
	int maxzoomdata;
	int maxzoomgui;
	public AdChartView(Context context,String chartname) throws IOException {
		super(context);
		loader=null;
		mapcache=new MapCache();
		level=2;
		blobs=new ArrayList<Blob>();
		maxzoomdata=4;
		maxzoomgui=6;
		bitmaps=new GetMapBitmap(mapcache,maxzoomdata);
		File extpath = Environment.getExternalStorageDirectory();
		curLostSignalRunnable=null;
		lostSignalTimer=new Handler();
		calc=new BearingSpeedCalc();
		pospaint=new Paint();
		pospaint.setStyle(Style.FILL_AND_STROKE);		
		pospaint.setARGB(128, 0, 0, 255);
		pospaint.setStrokeWidth(2.0f);
		
		File chartprojpath = new File(extpath,
				Config.path+chartname+".proj");
		DataInputStream ds=new DataInputStream(
				new FileInputStream(chartprojpath));
		A=new double[2][2];
		T=new double[2];
		boolean isfloat=(chartprojpath.length()==6*4);
		if (isfloat)
		{
			A[0][0]=ds.readFloat();
			A[1][0]=ds.readFloat();
			A[0][1]=ds.readFloat();
			A[1][1]=ds.readFloat();
			T[0]=ds.readFloat();
			T[1]=ds.readFloat();
			chart_width=3000; //this won't happen
			chart_height=3000;
		}
		else
		{
			A[0][0]=ds.readDouble();
			A[1][0]=ds.readDouble();
			A[0][1]=ds.readDouble();
			A[1][1]=ds.readDouble();
			T[0]=ds.readDouble();
			T[1]=ds.readDouble();		
			chart_width=3000;
			chart_height=3000;
			try{
				chart_width=ds.readInt();
				chart_height=ds.readInt();
				fail_get_width=false;
			}catch(Throwable e)
			{
				Log.i("fplan.adchart","Failed to get real width/height");
				e.printStackTrace();
			}
			
		}
		chart_width<<=(maxzoomgui-maxzoomdata);
		chart_height<<=(maxzoomgui-maxzoomdata);
		ds.close();
		//Log.i("fplan.adchart","loaded matrix:"+A[0][0]+","+A[1][0]+","+A[0][1]+","+A[1][1]);
		//Log.i("fplan.adchart","loaded vector:"+T[0]+", "+T[1]);
		
		for(int i=0;i<5;++i)
		{
			Integer is=new Integer(5-i-1);

			File chartpath = new File(extpath,
					Config.path+chartname+"-"+is.toString()+".bin");
			Blob blob=new Blob(chartpath.getAbsolutePath(),256);
			blobs.add(blob);
			/*Log.i("fplan.adchart","Dimensions of level "+is+" "+
					"x1:"+blob.getX1()+
					"y1:"+blob.getY1()+
					"x2:"+blob.getX2()+
					"y2:"+blob.getY2()
					);*/
		}
		
	}
	@Override
	protected void onDraw(Canvas canvas) {
				
		int width=getRight()-getLeft();		
		int height=getBottom()-getTop();
		last_width=width;
		last_height=height;
		clamp_scroll();
		
		int required_cachesize=((width+255+255)/256)*((height+255+255)/256);
		//TODO: Limit required cache size to never be bigger than total number of available tiles.
				
		//Log.i("fplan.adchart","Re-drawing chart!");
		mapcache.forgetqueries();
		Paint white=new Paint();
		white.setColor(Color.WHITE);
		white.setStyle(Style.FILL_AND_STROKE);		

		
		///canvas.drawColor(Color.BLACK);
		
		for(int x=0;x<chart_width>>(maxzoomgui-level);x+=256)
		{
			for(int y=0;y<chart_height>>(maxzoomgui-level);y+=256)
			{
				int tx=x-scroll_x;
				int ty=y-scroll_y;
				if (tx>-256 && ty>-256 && tx<width && ty<height)
				{
					iMerc pos=new iMerc(x,y);
					BitmapRes b = bitmaps.getBitmap(pos, level);
					if (b!=null)
					{
						RectF trg = new RectF(tx+getLeft(),ty+getTop(),
								tx+256+getLeft(),ty+256+getTop());
						Rect src = b.rect;
						canvas.drawBitmap(b.b, src, trg, null);
					}
					else
					{
						canvas.drawRect(getLeft()+tx, getTop()+ty, tx+256, ty+256, white);
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
				//Log.i("fplan.adchart","Start a background task again");
			}
			else
			{
				//Log.i("fplan.adchart","Map download still in progress");
			}
		}
		
		if (userPosition!=null)
		{
			int zoomgap=maxzoomgui-maxzoomdata;
			float px=((int)(userPosition.x*Math.pow(2, zoomgap))>>(maxzoomgui-level))-scroll_x+getLeft();
			float py=((int)(userPosition.y*Math.pow(2, zoomgap))>>(maxzoomgui-level))-scroll_y+getTop();
			if (userHdgRad!=null)
			{
				float rad=userHdgRad;
				px+=20.0f*Math.cos(rad);
				py+=20.0f*Math.sin(rad);
				RectF r=new RectF(px-40,py-40,px+40,py+40);
				canvas.drawArc(r,rad*(float)(180.0/Math.PI)+180.0f-20f,40,true,pospaint);
				//canvas.drawText("Hdg:"+rad*180/Math.PI, px+50, py+50, pospaint);
				//Log.i("fplan.adchart","Drawing arc at "+px+" "+py);
			}
			else
			{
				//Log.i("fplan.adchart","Drawing circle at "+px+" "+py);
				canvas.drawCircle(px,py, 15, pospaint);
			}
		}
		
	
	}
	@Override
	public void updateUI(boolean done) {
		if (done)
		{
			loader=null;
			//Log.i("fplan.adchart","Background task finished");
			invalidate();
		}
	}

	double offlat,offlon;
	public void update_location(Location inputlocation) {
		
		Location location=calc.calcBearingSpeed(inputlocation);
		
		double lat=location.getLatitude();
		double lon=location.getLongitude();
		if (DataDownloader.chartGpsDebugMode())
		{
			//Forcefully move us to middle of Arlanda airport,
			//So that we can easily test moving around there without
			//actually being there :-).
			if (offlat==0)
			{
				offlat=lat;
				offlon=lon;
			}
			lat=lat-offlat+59.652011;
			lon=lon-offlon+17.918701;
		}
		//        (lat)
		//P = A * (   )
		//        (lon)
		//
		//Px = A[0][0]*lat + A[0][1]*lon
		//Py = A[1][0]*lat + A[1][1]*lon
		{
			double mlat=lat-T[0];
			double mlon=lon-T[1];
			double px=A[0][0]*mlat + A[0][1]*mlon; 
			double py=A[1][0]*mlat + A[1][1]*mlon;
			userPosition=new Vector(px,py);
			//Log.i("fplan.adchart","Lat lon "+lat+","+lon+" mlat: "+mlat+" mlon: "+mlon+" converted to: "+px+","+py);
		}
		userHdgRad=null;
		if (location.hasBearing())
		{
//			Log.i("fplan.adchart","Has bearing:"+location.getBearing());
			float hdg=location.getBearing();
			double aimlat=lat+1e-3*Math.cos(hdg/(180.0/Math.PI));
			double aimlon=lon+1e-3*Math.sin(hdg/(180.0/Math.PI));
			double aimmlat=aimlat-T[0];
			double aimmlon=aimlon-T[1];
			double aimpx=A[0][0]*aimmlat + A[0][1]*aimmlon; 
			double aimpy=A[1][0]*aimmlat + A[1][1]*aimmlon;
			double dx=aimpx-userPosition.x;
			double dy=aimpy-userPosition.y;
			userHdgRad=(float)Math.atan2(dy,dx);
			
		}
		//Log.i("fplan.adchart","Position: "+lat+", "+lon);
		//Log.i("fplan.adchart","Pixels: "+userPosition.x+", "+userPosition.y);
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
	public boolean haveGeoLocation() {
		if (A[0][0]==0 &&
			A[1][0]==0 &&
			A[0][1]==0 &&
			A[1][1]==0)
			return false;
			
		return true;
	}
		


}
