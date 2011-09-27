package se.flightplanner;

import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import se.flightplanner.MapCache.Key;
import se.flightplanner.Project.iMerc;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.SystemClock;
import android.util.Log;

public class GetMapBitmap {
	private MapCache mapcache;
	private int maxzoomlevel;
	public GetMapBitmap(MapCache mapcache,int maxzoomlevel)
	{
		this.maxzoomlevel=maxzoomlevel;
		this.mapcache=mapcache;
	}
	static public class BitmapRes
	{
		public Bitmap b;
		public Rect rect;
	}
	BitmapRes getBitmap(iMerc m, int zoomlevel)
	{
		return collectUsableBitmap(m,zoomlevel);
	}
	/**
	 * Doesn't load anything from disk.
	 */
	private BitmapRes collectUsableBitmap(iMerc m, int desiredzoomlevel)
	{
		for(int zoomlevel=desiredzoomlevel;zoomlevel>=0;--zoomlevel)
		{
			iMerc curm=Project.imerc2imerc(m,desiredzoomlevel,zoomlevel);
			curm=new iMerc(curm.getX() & (~255),curm.getY() & (~255));
			boolean should_be_there=false;
			if (zoomlevel==desiredzoomlevel || desiredzoomlevel>maxzoomlevel && zoomlevel==maxzoomlevel)
				should_be_there=true;
			Bitmap b=getBitmapImpl(curm,zoomlevel,should_be_there);
			if (b!=null)
			{
				iMerc gotten=Project.imerc2imerc(curm,zoomlevel,desiredzoomlevel);
				int zoomgap=(desiredzoomlevel-zoomlevel);
				//int gapfactor=1<<zoomgap;
				int gottensize=256<<zoomgap;
				int usablesize=256>>zoomgap;
				int dx=(int)(((long)256*(long)(m.getX()-gotten.getX()))/gottensize);
				int dy=(int)(((long)256*(long)(m.getY()-gotten.getY()))/gottensize);
				BitmapRes res=new BitmapRes();
				res.b=b;
				res.rect=new Rect(dx,dy,dx+usablesize,dy+usablesize);
				return res;
			}
		}
		for(int zoomlevel=desiredzoomlevel+1;zoomlevel<=maxzoomlevel;++zoomlevel)
		{
			int zoomgap=(zoomlevel-desiredzoomlevel);
			int gapfactor=1<<zoomgap;
			if (gapfactor>4)
				return null;
			iMerc basecurm=Project.imerc2imerc(m,desiredzoomlevel,zoomlevel);
			iMerc curm=new iMerc(basecurm);
			Bitmap out=null;
			Canvas canvas=null;
			for(int j=0;j<gapfactor;++j)
			{
				for (int i=0;i<gapfactor;++i)
				{
					curm=new iMerc(basecurm.getX()+i*256,basecurm.getY()+j*256);
					Bitmap b=getBitmapImpl(curm,zoomlevel,false);
					if (b!=null)
					{
						if (out==null)
						{
							out = Bitmap.createBitmap(256,256,b.getConfig());
							canvas = new Canvas(out);
						}
						
						Rect src=new Rect(0,0,256,256);
						Rect dst=new Rect();
						dst.left=(i*256)/gapfactor;
						dst.right=dst.left+256/gapfactor;
						dst.top=(j*256)/gapfactor;
						dst.bottom=dst.top+256/gapfactor;						
						canvas.drawBitmap(b,src,dst,null);			
						//mapcache.eject(new Key(curm,zoomlevel));
					}
					
				}
			}
			if (out!=null)
			{
				mapcache.inject(m, desiredzoomlevel, out,true);
				BitmapRes res=new BitmapRes();
				res.b=out;
				res.rect=new Rect(0,0,256,256);
				return res;				
			}
		}
		return null;
	}
	
	private Bitmap getBitmapImpl(iMerc m, int zoomlevel,boolean backgroundload)
	{
		MapCache.Payload l = mapcache.query(m, zoomlevel,backgroundload);
		if (l!=null)
		{
			long now=SystemClock.uptimeMillis();
			l.lastuse=now;
			return l.b;
		}
		return null;
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
