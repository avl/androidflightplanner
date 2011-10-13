package se.flightplanner;

import java.util.ArrayList;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;

import se.flightplanner.Project.iMerc;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.util.Log;

public class MapCache {
	static public class Key
	{
		public iMerc getPos() {
			return pos;
		}
		public int getZoomlevel() {
			return zoomlevel;
		}
		private iMerc pos;
		private int zoomlevel;
		public Key(iMerc pos_, int zoomlevel_) {
			pos=pos_;
			zoomlevel=zoomlevel_;			
		}
		public String toString()
		{
			return "Key("+pos+",zoom="+zoomlevel+")";
		}
		@Override
		public int hashCode()
		{
			int code=pos.hashCode()+zoomlevel*2713;
			return code;
		}
		@Override
		public boolean equals(Object oo)
		{
			Key o=(Key)oo;
			boolean b= o.pos.equals((Object)this.pos) && o.zoomlevel==this.zoomlevel;
			return b;
		}		
	}
	static public class Payload
	{
		private long lastuse;
		Bitmap b;
		boolean fake;
	}
	private HashMap<Key,MapCache.Payload> map;
	private HashSet<Key> faked;
	boolean isShutdown;
	public MapCache()
	{
		map=new HashMap<Key, MapCache.Payload>();
		queryhistory=new HashSet<Key>();
		faked=new HashSet<Key>();
		isShutdown=false;
	}
	synchronized public void inject(iMerc m, int zoomlevel, Bitmap out,boolean fake) {
		if (isShutdown)
		{
			out.recycle();
			return;
		}
		Key k=new Key(m,zoomlevel);
		//Log.i("fplan.bitmap","Injected bitmap"+k+" fake:"+fake);
		long now=SystemClock.uptimeMillis();
		MapCache.Payload p=new MapCache.Payload();
		p.lastuse=now;
		p.b=out;
		p.fake=fake;
		map.put(k,p);
		if (p.fake)
			faked.add(k);
	}
	private HashSet<Key> queryhistory;
	synchronized boolean haveUnsatisfiedQueries()
	{
		return queryhistory.size()>0;
	}
	/*!
	 * This method gets a work list of maximum 4 items,
	 * which need to be loaded by a background loader
	 * somehow.
	 */
	synchronized Key[] get_and_reset_queryhistory(int count)
	{
		HashSet<Key> keys=new HashSet<Key>();
		HashSet<Key> intersection = new HashSet<Key>(faked);
		intersection.retainAll(queryhistory);
		//intersection now contains faked items which
		//are also being asked for.

		//load faked items from query history before items
		//which don't have a fake. 
		for(int j=0;j<2;++j)
		{
			if (keys.size()>=4) break;
			Iterator<Key> it;
			if (j==0) 
				it=intersection.iterator();
			else
				it=queryhistory.iterator();
			for(;;)
			{
				if (keys.size()>=4) break;
				if (!it.hasNext())
					break;
				Key val=it.next();
				keys.add(val);
			}
		}
		for(Key key:keys)
		{
			boolean removed=queryhistory.remove(key);
			//Remove any present fake value.
			MapCache.Payload l=map.get(key);
			/*TODO: Do this: if (l!=null && l.fake)
				eject(key);*/
			if (!removed)
				throw new RuntimeException("query history remove failed: "+key);
		}
		return keys.toArray(new Key[]{});
	}
	synchronized public void eject(Key d) {
		//Log.i("fplan.bitmap","Ejecting bitmap "+d);
		MapCache.Payload p=map.get(d);
		if (p!=null)
		{
			if (p.b!=null)
			{
				p.b.recycle();
				p.b=null;
			}
		}
		faked.remove(d);
		map.remove(d);
	}	
	synchronized public MapCache.Payload query(iMerc m, int zoomlevel, boolean backgroundload) {
		Key key=new Key(m,zoomlevel);
		//Log.i("fplan.adchart","Queried: "+m.getX()+","+m.getY());
		MapCache.Payload l=map.get(key);
		if (l==null || l.fake)
		{
			if (backgroundload)
			{
				//Log.i("fplan.adchart","Missing, adding to queryhistory: "+m.getX()+","+m.getY()+" zoom: "+zoomlevel+" curr size: "+map.size());
				queryhistory.add(key);
			}
		}
		if (l!=null)
			l.lastuse=SystemClock.uptimeMillis();
		return l;
	}
	synchronized public void garbageCollect(int cachesize) {
		long now=SystemClock.uptimeMillis();		
		while(map.size()>cachesize)
		{
			ArrayList<Key> deletelist=new ArrayList<Key>();
			long oldest_age=0;
			Key oldest=null;
			//Log.i("fplan.bitmap","garbageCollect, cache size: "+cachesize+" map: "+map.size());
			for(Entry<Key, MapCache.Payload> e:map.entrySet())
			{
				long age=now-e.getValue().lastuse;
				if (e.getValue().fake)
					age+=10000;
				if (age>30000)				
					deletelist.add(e.getKey());
				if (map.size()>cachesize)
				{
					if (age>oldest_age)
					{
						oldest_age=age;
						oldest=e.getKey();
					}
				}
			}
			if (oldest!=null)
			{
				//Log.i("fplan.drawmap","Oldest age:"+oldest_age);
				deletelist.add(oldest);
			}
			for(Key d:deletelist)
			{
				//Log.i("fplan.drawmap","Ejecting:"+d);
				eject(d);
			}
		}
	}
	public void forgetqueries() {
		queryhistory.clear();
	}
	public void shutdown() {
		isShutdown=true;
		ArrayList<Key> deletelist=new ArrayList<MapCache.Key>();
		for (Key k:map.keySet())
			deletelist.add(k);
		for(Key key : deletelist)
		{
			eject(key);
		}
		queryhistory.clear();
		faked.clear();
		
	
	}
	public void releaseMemory() {
		ArrayList<Key> deletelist=new ArrayList<Key>();
		for (Key k:map.keySet())
			deletelist.add(k);
		for(Key key : deletelist)
			eject(key);
		queryhistory.clear();
		faked.clear();
	}

}
