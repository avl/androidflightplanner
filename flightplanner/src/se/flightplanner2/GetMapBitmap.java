package se.flightplanner2;

import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import se.flightplanner2.MapCache.Key;
import se.flightplanner2.Project.iMerc;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.SystemClock;
import android.util.Log;

public class GetMapBitmap {
	private MapCache mapcache;
	public GetMapBitmap(MapCache mapcache)
	{
		this.mapcache=mapcache;
	}
	static public class BitmapRes
	{
		public Bitmap b;
		public Rect rect;
	}
	BitmapRes getBitmap(iMerc m, int zoomlevel)
	{
		//Log.i("fplan.bitmap","Getting bitmap at "+m+" zoom: "+zoomlevel);
		return collectUsableBitmap(m,zoomlevel);
	}
	/**
	 * Doesn't load anything from disk.
	 */
	private BitmapRes collectUsableBitmap(iMerc m, int desiredzoomlevel)
	{
		//boolean only_fake=true;
		for(int zoomlevel=desiredzoomlevel;zoomlevel>=0;--zoomlevel)
		{
			iMerc curm=Project.imerc2imerc(m,desiredzoomlevel,zoomlevel);
			curm=new iMerc(curm.getX() & (~255),curm.getY() & (~255));
			/*
			boolean should_be_there=false;
			if (desiredzoomlevel>maxzoomlevel)
			{
				if (zoomlevel==maxzoomlevel)
					should_be_there=true;
			}
			else
			{
				if (zoomlevel==desiredzoomlevel)
					should_be_there=true;
			}
			*/
			//if (zoomlevel==desiredzoomlevel || desiredzoomlevel>maxzoomlevel && zoomlevel==maxzoomlevel)
			//	should_be_there=true;
			//Log.i("fplan","Request for tile at "+desiredzoomlevel+" now at "+zoomlevel+" should be there: "+should_be_there);
			MapCache.Payload b=getBitmapImpl(curm,zoomlevel,true);
			if (b!=null && b.b!=null)
			{
				iMerc gotten=Project.imerc2imerc(curm,zoomlevel,desiredzoomlevel);
				int zoomgap=(desiredzoomlevel-zoomlevel);
				//int gapfactor=1<<zoomgap;
				int gottensize=256<<zoomgap;
				int usablesize=256>>zoomgap;
				int dx=(int)(((long)256*(long)(m.getX()-gotten.getX()))/gottensize);
				int dy=(int)(((long)256*(long)(m.getY()-gotten.getY()))/gottensize);
				BitmapRes res=new BitmapRes();
				res.b=b.b;
				res.rect=new Rect(dx,dy,dx+usablesize,dy+usablesize);
				
				return res;
			}
			//if (b==null || !b.only_fake_available)
			//	only_fake=false;
		}
		
		return null;
	}
	
	private MapCache.Payload getBitmapImpl(iMerc m, int zoomlevel,boolean background_load)
	{
		return mapcache.query(m, zoomlevel, background_load);
	}
	/*
	ArrayList<Blob> blobs;
	void loadBitmap(iMerc m,int zoomlevel,int cachesize) throws IOException
	{		
		mapcache.garbageCollect(cachesize);
		Bitmap bmap=blobs.get(zoomlevel).get_bitmap(m);
		mapcache.inject(m,zoomlevel,bmap,false);
	}
	*/
	public void releaseMemory() {
		mapcache.releaseMemory();
	}
	
	
}
