package se.flightplanner2;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import se.flightplanner2.Project.iMerc;

public class ElevBitmapCache {
	public static interface ClientIf
	{
		public void updated(); //Called when background loading of a requested tile has finished.
	}

	private ClientIf client;
	static final int tilesize=256; 
	private Blob[] blobs;
	private int cur_elev;
	private int zoomlevel;
	private int orig_zoomlevel;
	
	public ElevBitmapCache(ClientIf client)
	{
		this.client=client;
		File extpath = Environment.getExternalStorageDirectory();
		blobs=new Blob[Config.max_zoomlevel+1];
		for(int i=0;i<blobs.length;++i)
		{
			File path = new File(extpath,
					Config.path+"elev"+"level" + i);			
			if (path.exists())
			{				
				try{
					blobs[i]=new Blob(path.getAbsolutePath(),tilesize);
					Log.i("fplan.terr","Success initializing elev blob zoomlevel "+i);
				}
				catch(Throwable e)
				{
					e.printStackTrace();
					Log.i("fplan.terr","Couldn't load zoomlevel: "+i);
				}
			}				
			else			
			{
				Log.i("fplan.terr","Couldn't load zoomlevel: "+i+" path "+path+" doesn't exist");				
			}
		}
		cur_elev=0;
	}
	
	
	private class CachedItem
	{
		iMerc pos;
		int elev;
		Bitmap bm;	//set to null if error
		int zoomlevel;
		boolean used;
		@Override
 		public String toString()
		{
			return "CachedItem("+pos+" elev: "+elev+" bm: "+bm+" zoomlevel: "+zoomlevel+" used: "+used+")";
		}
	}
	private HashMap<iMerc,CachedItem> items=new HashMap<Project.iMerc, ElevBitmapCache.CachedItem>();
	private HashSet<iMerc> backlog=new HashSet<Project.iMerc>();
	
	static final int max_zoom_level=Config.max_elev_zoomlevel;
	
	static public class BMResult
	{
		Bitmap bm;
		Rect r;
	}
	public BMResult query2(iMerc upperleft)
	{
		if (orig_zoomlevel==zoomlevel)
		{
			BMResult bmr=new BMResult(); 
			bmr.bm=query(upperleft);
			bmr.r=new Rect(0,0,256,256);			
			return bmr;
		}
		if (orig_zoomlevel<zoomlevel)
			return null; //unexpected
		int gap=orig_zoomlevel-zoomlevel;
		int ts=256>>gap;
		iMerc in_parent_ref=Project.imerc2imerc(upperleft,orig_zoomlevel,zoomlevel);
		int parent_corner_x=in_parent_ref.getX()&(~0xff);
		int parent_corner_y=in_parent_ref.getY()&(~0xff);
		iMerc parent_coord=new iMerc(parent_corner_x,parent_corner_y);
		int par_off_x=in_parent_ref.getX()&0xff;
		int par_off_y=in_parent_ref.getY()&0xff;
		Bitmap bm=query(parent_coord);	
		BMResult bmr=new BMResult(); 
		bmr.bm=bm;
		bmr.r=new Rect(par_off_x,par_off_y,par_off_x+ts,par_off_y+ts);
		return bmr;		
		
	}
	
	private Bitmap query(iMerc m)
	{
		if (stopping) return null;
			
		CachedItem res=items.get(m);
		if (res==null || tooBigElevDiff(res))
		{
			Log.i("fplan.terr","No hit for "+m+" or too big elev diff");
			backlog.add(m);
			return null;
		}
		res.used=true;
		Log.i("fplan.terr","Found cache hit for "+m+" res: "+res);
		if (nearlyTooBigElevDiff(res))
		{
			Log.i("fplan.terr","But elev diff is growing big for "+m);
			backlog.add(m);
		}
		return res.bm;
	}
	
	private boolean tooBigElevDiff(CachedItem res) {
		if (Math.abs(res.elev-cur_elev)>50)
			return true;
		return false;
	}
	private boolean nearlyTooBigElevDiff(CachedItem res) {
		if (Math.abs(res.elev-cur_elev)>25)
			return true;
		return false;
	}

	public void start_frame(int zoomlevel,int cur_elev)
	{
		orig_zoomlevel=zoomlevel;
		if (zoomlevel>max_zoom_level)
			zoomlevel=max_zoom_level;
		if (stopping) return;
		if (this.zoomlevel!=zoomlevel)
		{
			Log.i("fplan.terr","Zoomlevel change detected, new level: "+zoomlevel);
			this.zoomlevel=zoomlevel;
			items.clear();
		}
		this.cur_elev=cur_elev;
		backlog.clear();
		for(CachedItem ci:items.values())		
			ci.used=false;
	}
	public void delete_all_unused()
	{
		if (stopping) return;
		ArrayList<iMerc> allitems=new ArrayList<Project.iMerc>();
		allitems.addAll(items.keySet());
		for(iMerc k:allitems)
		{
			CachedItem ci=items.get(k);
			if (ci!=null && !ci.used)
			{
				Log.i("fplan.terr","Item unused: "+ci.pos);
				items.remove(k);
			}
			else
			{
				Log.i("fplan.terr","Item was used: "+ci.pos);				
			}
		}
	}
	
	private class BackgroundLoader extends AsyncTask<iMerc, Void, CachedItem>
	{
		Blob blob;
		int curelev;
		int zoomlevel;
		public BackgroundLoader(Blob blob,int curelev,int zoomlevel)
		{
			this.curelev=curelev;
			this.blob=blob;
			this.zoomlevel=zoomlevel;
		}
		@Override
		protected CachedItem doInBackground(iMerc... params) {
			if (params.length!=1) return null;
			
			try {				
				int[] output=new int[tilesize*tilesize];
				byte[] bytes=blob.get_tile(params[0]);
				short[] shorts;
				if (bytes==null) 
				{
					shorts=new short[tilesize*tilesize*2];
					for(int i=0;i<shorts.length;++i)
						shorts[i]=0x7fff;					
				}
				else
				{
					Log.i("fplan.err","Bytes for tile: "+bytes.length);
					ByteBuffer buffer = ByteBuffer.wrap( bytes );
					buffer.order( ByteOrder.BIG_ENDIAN );
					ShortBuffer shortbuf = buffer.asShortBuffer( );
					if (!shortbuf.hasArray())
					{
						shorts=new short[tilesize*tilesize*2];
						Log.i("fplan.terr","Could not view bytebuffer directly as short[]");
						shortbuf.get(shorts);
					}
					else
					{
						Log.i("fplan.terr","Yes, Could view bytebuffer directly as short[]");
						shorts=shortbuf.array();
					}
				}
				int outi=0;
				for(int i=1;i<shorts.length;i+=2)
				{
					short celev1=shorts[i];
					short celev2dummy=shorts[i-1];
					if (i<10)
					{
						Log.i("fplan.terr","Read alts: "+celev1+" and "+celev2dummy);
					}
					if (celev1<0)
					{
						output[outi]=Color.argb(0x80,0xff,0,0);
	
					}
					else
					{
						int margin=this.curelev-celev1;													
						output[outi]=colorize_margin(margin);						
					}
					
					outi+=1;
				}		
				Bitmap bm=Bitmap.createBitmap(output, tilesize, tilesize, Bitmap.Config.ARGB_4444);
				CachedItem ret=new CachedItem();
				ret.bm=bm;
				ret.elev=this.curelev;
				ret.pos=params[0];
				ret.used=true;
				ret.zoomlevel=zoomlevel;
				Log.i("fplan.terr","Created a bitmap for: "+ret.pos);				
				return ret;
				
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}						
		}	
		private int colorize_margin(int margin) {
			if (margin<=500)
				return Color.argb(0x80,0xff,0,0);
			if (margin<500+512)
			{
				margin-=500;
				margin/=2; //range now 0..255
				return Color.argb(0x80,0xff,margin,0);				
			}
			if (margin<500+512+512)
			{
				margin-=500+512;
				margin/=2; //range now 0..255
				return Color.argb(0x80-margin/2,0xff,0xff,0);								
			}
			return Color.argb(0,0,0,0);
		}
		@Override
		protected void onPostExecute(CachedItem result)
		{
			if (result.zoomlevel==ElevBitmapCache.this.zoomlevel)
			{
				Log.i("fplan.terr","Injecting: "+result);				
				items.put(result.pos, result);
				if (result.bm!=null)
					client.updated();
				backlog.remove(result.pos);
			}
			cur_bg=null;
			schedule_background_tasks();
		}
	}	
	private boolean stopping=false;
	public void scheduleStop()
	{
		Log.i("fplan.terr","Stopping");				
		stopping=true;
		cur_bg.cancel(false);
		cur_bg=null;
		
	}
	private BackgroundLoader cur_bg=null;
	public void schedule_background_tasks()
	{
		if (stopping) return;
		if (cur_bg!=null)
			Log.i("fplan.terr","Background task already running");				
		if (cur_bg==null && backlog.size()>0)
		{
			Iterator<iMerc> it=backlog.iterator();
			if (it.hasNext())
			{
				iMerc cur=it.next();
				if (zoomlevel>=0 && zoomlevel<blobs.length && blobs[zoomlevel]!=null)
				{
					Log.i("fplan.terr","Starting new loader for "+cur);												
					cur_bg=new BackgroundLoader(blobs[zoomlevel],cur_elev,zoomlevel);
					cur_bg.execute(cur);
				}
				else
				{
					Log.i("fplan.terr","Zoomlevel out of range: "+zoomlevel);												
				}
			}
		}
		else
		{
			Log.i("fplan.terr","Nothing can/needs be done. Backlog: "+backlog.size());							
		}
	}
	//setPixels (int[] pixels, int offset, int stride, int x, int y, int width, int height)
	
}
