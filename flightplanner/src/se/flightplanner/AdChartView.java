package se.flightplanner;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import se.flightplanner.BackgroundMapLoader.UpdatableUI;
import se.flightplanner.GetMapBitmap.BitmapRes;
import se.flightplanner.Project.iMerc;
import se.flightplanner.SigPoint.Chart;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.location.Location;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class AdChartView extends View implements UpdatableUI {

	MapCache mapcache;
	BackgroundMapLoader loader;
	ArrayList<Blob> blobs;
	GetMapBitmap bitmaps;
	Chart chart;
	int level;
	enum State
	{
		IDLE,
		DRAGGING
	}
	State state=State.IDLE;
	float downx;
	float downy;
	int start_scroll_x;
	int start_scroll_y;
	int scroll_x;
	int scroll_y;
	
	int last_width; //last screen width
	int last_height;  //last screen height
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
				state=State.DRAGGING;
				downx=x;
				downy=y;
				start_scroll_x=scroll_x;
				start_scroll_y=scroll_y;
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
	int chart_width;
	int chart_height;

	private void clamp_scroll() {
		if (scroll_x<0) scroll_x=0;
		if (scroll_y<0) scroll_y=0;
		if (scroll_x>chart_width-last_width) scroll_x=chart_width-last_width;
		if (scroll_y>chart_height-last_height) scroll_y=chart_height-last_height;
	}

	public AdChartView(Context context,Chart chart) throws IOException {
		super(context);
		loader=null;
		this.chart=chart;
		mapcache=new MapCache();
		level=0;
		blobs=new ArrayList<Blob>();
		bitmaps=new GetMapBitmap(mapcache,blobs.size()+1);
		File extpath = Environment.getExternalStorageDirectory();
		chart_width=chart.width;
		chart_height=chart.height;		
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
		
		
		
		for(int x=0;x<chart_width;x+=256)
		{
			for(int y=0;y<chart_height;y+=256)
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
		
	
	}
	@Override
	public void updateUI(boolean done) {
		loader=null;
		Log.i("fplan.adchart","Background task finished");
		invalidate();
	}

	public void update_location(Location location) {
		double lat=location.getLatitude();
		double lon=location.getLongitude();
		//        (lat)
		//P = A * (   )
		//        (lon)
		//
		//Px = A[0][0]*lat + A[0][1]*lon
		//Py = A[1][0]*lat + A[1][1]*lon
		double px=chart.A[0][0]*lat + chart.A[0][1]*lon; 
		double py=chart.A[1][0]*lat + chart.A[1][1]*lon;
		asdf
	}

	public void no_location_fix() {
		// TODO Auto-generated method stub
		
	}
		


}
