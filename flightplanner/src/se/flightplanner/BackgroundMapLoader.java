package se.flightplanner;


import java.io.IOException;
import java.util.ArrayList;

import se.flightplanner.MapCache.Key;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.util.Log;

public class BackgroundMapLoader extends AsyncTask<Key,BackgroundMapLoader.LoadedBitmap,Void>{

	MapCache mapcache;
	ArrayList<Blob> blobs;
	long prev;
	UpdatableUI ui;
	boolean change;
	boolean running;
	int cachesize;
	
	BackgroundMapLoader(ArrayList<Blob> blobs,MapCache mapcache,UpdatableUI ui,int cachesize)
	{
		this.blobs=blobs;
		this.mapcache=mapcache;
		this.ui=ui;
		this.cachesize=cachesize;
		change=false;
		running=true;
	}
	public void run()
	{
		Key[] keys=mapcache.get_and_reset_queryhistory(4);
		
		if (keys.length>0)
			execute(keys);	
		
	}
	static public interface UpdatableUI
	{
		public void updateUI(boolean done);
	}
	static public class LoadedBitmap
	{
		Key key;
		Bitmap bitmap;
		public LoadedBitmap(Key key,Bitmap bitmap)
		{
			this.key=key;
			this.bitmap=bitmap;
		}
	}
	@Override
	protected Void doInBackground(Key... keys) {
		
		for (Key key:keys)
		{
			if (Thread.currentThread().isInterrupted())
				break;
			if (key.getZoomlevel()<0 || key.getZoomlevel()>=blobs.size())
			{
				Log.i("fplan.bitmap","Publish a fake bitmap (1):"+key.getPos());
				publishProgress(new LoadedBitmap(key,null));
			}
			else
			{
				try {
					Log.i("fplan.bitmap","Publish a real bitmap:"+key.getPos());
					publishProgress(new LoadedBitmap(key,blobs.get(key.getZoomlevel()).get_bitmap(key.getPos())));
				} catch (Throwable e) {
					Log.e("fplan","Background loading of tile "+key.getPos()+" failed."+e);
					e.printStackTrace();
				}				
			}
			
		}
		return null;
	}
	@Override
	protected void onPreExecute()
	{
		prev=SystemClock.uptimeMillis();
	}
	@Override
    protected void onProgressUpdate(LoadedBitmap... progress) {
		for(LoadedBitmap bm:progress)
    	{
			mapcache.garbageCollect(cachesize);
			Log.i("fplan.bitmap","Injected a non-fake bitmap"+bm.key.getPos());
    		mapcache.inject(bm.key.getPos(),bm.key.getZoomlevel(),bm.bitmap,false);
    		change=true;
    	}
		if (change)
		{
			long now=SystemClock.uptimeMillis();
			if (now-prev>1000)
			{
				ui.updateUI(false);
				change=false;
			}
			prev=now;
			
		}
    }
	@Override
    protected void onPostExecute(Void result) 
    {
		Log.i("fplan.bitmap","onPostExecute");
		if (running)
			ui.updateUI(true);
		running=false;
		change=false;
    }
	@Override
    protected void onCancelled() 
    {
		Log.i("fplan.bitmap","onCancelled");
		if (running)
			ui.updateUI(true);
			running=false;
		change=false;
    }
}

	

