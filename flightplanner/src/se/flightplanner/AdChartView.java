package se.flightplanner;

import java.io.IOException;
import java.util.ArrayList;

import se.flightplanner.BackgroundMapLoader.UpdatableUI;
import se.flightplanner.GetMapBitmap.BitmapRes;
import se.flightplanner.Project.iMerc;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.Log;
import android.view.View;

public class AdChartView extends View implements UpdatableUI {

	MapCache mapcache;
	BackgroundMapLoader loader;
	ArrayList<Blob> blobs;
	GetMapBitmap bitmaps;
	int level;
	public AdChartView(Context context,String mapname) throws IOException {
		super(context);
		loader=null;
		mapcache=new MapCache();
		level=0;
		blobs=new ArrayList<Blob>();
		bitmaps=new GetMapBitmap(mapcache,blobs.size()+1);
		for(int i=0;i<5;++i)
		{
			Integer is=new Integer(i);
			Blob blob=new Blob(mapname+"-"+is.toString()+".bin",256);
			blobs.add(blob);
		}
		
	}
	@Override
	protected void onDraw(Canvas canvas) {
		
		int width=canvas.getWidth();		
		int height=canvas.getHeight();
		int required_cachesize=((width+255+255)/256)*((height+255+255)/256);
		//TODO: Limit required cache size to never be bigger than total number of available tiles.
				
		mapcache.forgetqueries();
		
		for(int x=0;x<width;x+=256)
		{
			for(int y=0;y<height;y+=256)
			{
				iMerc pos=new iMerc(x,y);
				BitmapRes b = bitmaps.getBitmap(pos, level);
				if (b!=null)
				{
					canvas.drawBitmap(b.b, x,y,null);					
				}								
			}
		}		
		
		if (mapcache.haveUnsatisfiedQueries())
		{
			if (loader==null)
			{
				loader=new BackgroundMapLoader(blobs, mapcache, this,required_cachesize);
				loader.run();
				Log.i("fplan.adbitmap","Start a background task again");
			}
		}
		
	
	}
	@Override
	public void updateUI(boolean done) {
		invalidate();
		
	}
		


}
