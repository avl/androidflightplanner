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
import android.os.SystemClock;
import android.util.Log;
import se.flightplanner2.ElevBitmapCache.Mode;
import se.flightplanner2.GlobalGetElev.GetElevation;
import se.flightplanner2.Project.LatLon;
import se.flightplanner2.Project.iMerc;

public class ElevBitmapCache implements GetElevation {
	public static interface ClientIf
	{
		public void updated(boolean fully_updated); //Called when background loading of a requested tile has finished.

		public File getStorage();
	}
	static public enum Mode
	{
		WARNING,
		EXPLORE
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
		File extpath = client.getStorage();
		blobs=new Blob[Config.max_zoomlevel+1];
		for(int i=0;i<blobs.length;++i)
		{
			File path = new File(extpath,
					Config.path+"elev"+"level" + i);			
			if (path.exists())
			{				
				try{
					blobs[i]=new Blob(path.getAbsolutePath(),tilesize);
					//Log.i("fplan.terr","Success initializing elev blob zoomlevel "+i);
				}
				catch(Throwable e)
				{
					e.printStackTrace();
					Log.i("fplan.terr","Couldn't load zoomlevel: "+i);
				}
			}				
			else			
			{
				//Log.i("fplan.terr","Couldn't load zoomlevel: "+i+" path "+path+" doesn't exist");				
			}
		}
		cur_elev=0;
	}
	
	
	static private class CachedItem
	{
		iMerc pos;
		int elev;
		Bitmap bm;	//set to null if error
		int zoomlevel;
		boolean used;
		Mode mode;
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
			bmr.r=new Rect(0,0,tilesize,tilesize);			
			return bmr;
		}
		if (orig_zoomlevel<zoomlevel)
			return null; //unexpected
		int gap=orig_zoomlevel-zoomlevel;
		int ts=tilesize>>gap;
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
			//Log.i("fplan.terr","No hit for "+m+" or too big elev diff");
			backlog.add(m);
			return null;
		}
		res.used=true;
		//Log.i("fplan.terr","Found cache hit for "+m+" res: "+res);
		if (nearlyTooBigElevDiff(res))
		{
			//Log.i("fplan.terr","But elev diff is growing big for "+m);
			backlog.add(m);
		}
		return res.bm;
	}
	
	private boolean tooBigElevDiff(CachedItem res) {
		if (Math.abs(res.elev-cur_elev)>500)
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
			//Log.i("fplan.terr","Zoomlevel change detected, new level: "+zoomlevel);
			this.zoomlevel=zoomlevel;
			for(CachedItem item:items.values())
				if (item.bm!=null)
					item.bm.recycle();
			items.clear();
		}
		this.cur_elev=cur_elev;
		backlog.clear();
		for(CachedItem ci:items.values())		
			ci.used=false;
	}
	public void purge_all()
	{
		if (cur_bg!=null)
			cur_bg.cancel(false);
		if (stopping) return;
		for(CachedItem ci:items.values())
		{
			if (ci!=null)
			{
				if (ci.bm!=null)
					ci.bm.recycle();
			}
		}
		items.clear();
		
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
				//Log.i("fplan.terr","Item unused: "+ci.pos);
				if (ci.bm!=null)
					ci.bm.recycle();
				items.remove(k);
			}
			else
			{
				//Log.i("fplan.terr","Item was used: "+ci.pos);				
			}
		}
	}
	static class Range
	{
		public Range(int margin,int r,int g,int b)
		{
			this.margin=margin;
			this.r=r;this.g=g;this.b=b;
		}
		int margin;
		int r,g,b;
	}
	
	private class BackgroundLoader extends AsyncTask<iMerc, Void, CachedItem>
	{
		Blob blob;
		int curelev;
		int zoomlevel;
		Mode mode;
		public BackgroundLoader(Blob blob,int curelev,int zoomlevel,Mode mode)
		{
			this.curelev=curelev;
			this.blob=blob;
			this.zoomlevel=zoomlevel;
			this.mode=mode;
		}
		@Override
		protected CachedItem doInBackground(iMerc... params) {
			if (params.length!=1) return null;
			
			try {				
				int[] output=new int[tilesize*tilesize];
				byte[] bytes=blob.get_tile(params[0],2*tilesize*tilesize);
				
				short[] shorts;
				if (bytes==null) 
				{
					shorts=new short[tilesize*tilesize];
					for(int i=0;i<shorts.length;++i)
						shorts[i]=0x7fff;					
				}
				else
				{
					if (bytes.length!=tilesize*tilesize*2)
					{
						Log.i("fplan.terr","Unexpected size of elev blob read: "+bytes.length);
						throw new IOException("Bad size of elev blob: "+bytes.length);
					}					//Log.i("fplan.err","Bytes for tile: "+bytes.length);
					ByteBuffer buffer = ByteBuffer.wrap( bytes );
					buffer.order( ByteOrder.BIG_ENDIAN );
					ShortBuffer shortbuf = buffer.asShortBuffer( );
					if (!shortbuf.hasArray())
					{
						shorts=new short[tilesize*tilesize];
						//Log.i("fplan.terr","Could not view bytebuffer directly as short[]");
						shortbuf.get(shorts);
					}
					else
					{
						//Log.i("fplan.terr","Yes, Could view bytebuffer directly as short[]");
						shorts=shortbuf.array();
					}
				}
				int outi=0;
				if (mode==Mode.WARNING)
				{
					for(int i=0;i<shorts.length;i+=1)
					{
						short celev1=shorts[i]; 
						int margin=this.curelev-celev1;													
						output[outi]=colorize_margin_warn(margin);											
						outi+=1;
					}
				}
				else
				{
					for(int i=0;i<shorts.length;i+=1)
					{
						short celev1=shorts[i]; 
						int margin=this.curelev-celev1;													
						output[outi]=colorize_margin_expl(margin);											
						outi+=1;
					}					
				}
				
				Bitmap bm=Bitmap.createBitmap(output, tilesize, tilesize, Bitmap.Config.ARGB_4444);
				CachedItem ret=new CachedItem();
				ret.bm=bm;
				ret.elev=this.curelev;
				ret.pos=params[0];
				ret.used=true;
				ret.zoomlevel=zoomlevel;
				ret.mode=mode;
				//Log.i("fplan.terr","Created a bitmap for: "+ret.pos);				
				return ret;
				
			} catch (IOException e) {
				e.printStackTrace();
				CachedItem ret=new CachedItem();
				ret.bm=null;
				ret.elev=this.curelev;
				ret.pos=params[0];
				ret.used=true;
				ret.zoomlevel=zoomlevel;
				return ret;
			}						
		}	
		private int colorize_margin_warn(int margin) {
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
		Range[] ranges=new Range[]{
				new Range(-7000,  255,192,192),
				new Range(250,  255,0,0),
				new Range(750,     255,255,0),
				new Range(1250, 0,255,0),
				new Range(4000, 0,0,255),
				new Range(7000, 0,0,0),
		};
		
		private int colorize_margin_expl(int margin) {
			Range prev=ranges[0];
			if (margin<prev.margin)
				return Color.rgb(prev.r,prev.g,prev.b);
			for(int i=1;i<ranges.length;++i)
			{
				Range next=ranges[i];
				if (margin<next.margin)
				{
					int delta=margin-prev.margin;
					delta<<=8;
					delta/=(next.margin-prev.margin);
					int r=(prev.r*(256-delta)+next.r*delta)>>8;
					int g=(prev.g*(256-delta)+next.g*delta)>>8;
					int b=(prev.b*(256-delta)+next.b*delta)>>8;
					return Color.rgb(r, g, b);  
				}				
				prev=next;
			}
			return Color.rgb(255,255,255);
		}
		@Override
		protected void onPostExecute(CachedItem result)
		{
			if (result!=null && result.zoomlevel==ElevBitmapCache.this.zoomlevel &&
					result.mode==ElevBitmapCache.this.mode)
			{
				//Log.i("fplan.terr","Injecting: "+result);				
				ElevBitmapCache.this.items.put(result.pos, result);
				ElevBitmapCache.this.backlog.remove(result.pos);
				if (result.bm!=null)
					ElevBitmapCache.this.client.updated(ElevBitmapCache.this.backlog.size()==0);
			}
			ElevBitmapCache.this.cur_bg=null;
			ElevBitmapCache.this.schedule_background_tasks();
		}
	}	
	private boolean stopping=false;
	public void scheduleStop()
	{
		//Log.i("fplan.terr","Stopping");				
		stopping=true;
		if (cur_bg!=null)
			cur_bg.cancel(false);
		cur_bg=null;
		
	}
	private BackgroundLoader cur_bg=null;
	public void schedule_background_tasks()
	{
		if (stopping) return;
		if (cur_bg!=null)
			{}//Log.i("fplan.terr","Background task already running");				
		if (cur_bg==null && backlog.size()>0)
		{
			iMerc worst=null;
			int worstdiff=0;
			for(iMerc bl:backlog)
			{
				CachedItem item=items.get(bl);
				if (item==null)
				{
					worst=bl;break;
				}
				int diff=Math.abs(item.elev-cur_elev);
				if (worst==null || diff>worstdiff)
				{
					worstdiff=diff;
					worst=bl;
				}
			}			
			if (worst!=null)
			{
				iMerc cur=worst;
				if (zoomlevel>=0 && zoomlevel<blobs.length && blobs[zoomlevel]!=null)
				{
					//Log.i("fplan.terr","Starting new loader for "+cur);												
					cur_bg=new BackgroundLoader(blobs[zoomlevel],cur_elev,zoomlevel,mode);
					cur_bg.execute(cur);
				}
				else
										
				{
					//Log.i("fplan.terr","Zoomlevel out of range: "+zoomlevel);												
				}
			}
		}
		else
		{
			//Log.i("fplan.terr","Nothing can/needs be done. Backlog: "+backlog.size());							
		}
	}
	
	
	HashMap<iMerc,Short> pt_elev_cache=new HashMap<iMerc,Short>();
	int last_cached_zoomlevel;
	//setPixels (int[] pixels, int offset, int stride, int x, int y, int width, int height)
	@Override
	public short get_elev_ft(LatLon pos,int zoomlevel,int pixelradius) {
		long bef=SystemClock.elapsedRealtime();
		int pixels=6;
		while (zoomlevel>0 && pixels<pixelradius)
		{
			zoomlevel-=1;
			pixels<<=1;
		}
		int samplebox=6;
		if (zoomlevel>Config.max_elev_zoomlevel)
		{
			int diff=zoomlevel-Config.max_elev_zoomlevel;
			samplebox>>=diff;
			if (samplebox<1) samplebox=1;
			zoomlevel=Config.max_elev_zoomlevel;
		}
		
		
		iMerc m=Project.latlon2imerc(pos, zoomlevel);
		m.x-=samplebox/2;
		m.y-=samplebox/2;
		
		
		if (zoomlevel!=last_cached_zoomlevel)
			pt_elev_cache.clear();
		last_cached_zoomlevel=zoomlevel;
		Short cval=pt_elev_cache.get(m);
		
		if (cval!=null) {
			//Log.i("fplan.mmupd","Elev Cache hit, cache size: "+pt_elev_cache.size()+" coord: "+m+" pos: "+pos);
			return cval;
		}
		iMerc orig=new iMerc(m);
		short maxval=Short.MIN_VALUE;
		for(int ix=0;ix<samplebox;++ix)
			for(int iy=0;iy<samplebox;++iy)
			{
				m.x=orig.x+ix;
				m.y=orig.y+iy;
				short val1=get_elev_ft_uncached(m,zoomlevel);
				if (val1>maxval)
					maxval=val1;
			}
		short val=maxval;
		cval=val;
		if (pt_elev_cache.size()>250)
			pt_elev_cache.clear();
		pt_elev_cache.put(orig,cval);
		long aft=SystemClock.elapsedRealtime();
		//Log.i("fplan.mmupd","Time to get elev ft: "+(aft-bef)+"ms, cache size: "+pt_elev_cache.size());
		return val;		
	}	
	private short get_elev_ft_uncached(iMerc m,int zoom) {
		
		int x=m.getX()&(~0xff);
		int y=m.getY()&(~0xff);
		int xoff=m.getX()&0xff;
		int yoff=m.getY()&0xff;
		if (zoom<0 || zoom>=blobs.length)
			return Short.MAX_VALUE;
		
		Blob blob=blobs[zoom];
		if (blob==null) return Short.MAX_VALUE;
		byte[] bs;
		try {
			bs = blob.get_data_at(new iMerc(x,y),2*(xoff+256*yoff),2);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return Short.MAX_VALUE;
		}
		if (bs==null)
			return Short.MAX_VALUE;
		int lsb=bs[1]&0xff;
		int msb=bs[0]&0xff;
		short elev=(short)(lsb+(msb<<8));
		return elev;
	}

	private Mode mode=Mode.WARNING;
	public void setMode(Mode mode_) {
		if (this.mode!=mode_)
			purge_all();
		//Log.i("fplan.elev","Mode:"+mode_);
		this.mode=mode_;		
	}
	
}
