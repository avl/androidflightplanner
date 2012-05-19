package se.flightplanner2;

import java.util.ArrayList;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;

import se.flightplanner2.Project.iMerc;
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
		Bitmap b;
		boolean fake;
		boolean used;
		public boolean only_fake_available;
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
	synchronized public void inject(iMerc m, int zoomlevel, Bitmap out,boolean fake,boolean only_fake_avail) {
		if (isShutdown)
		{
			if (out!=null)
				out.recycle();
			return;
		}
		Key k=new Key(m,zoomlevel);
		//Log.i("fplan.adchart","Injected bitmap"+k+" fake: "+fake+" only fake-avail: "+only_fake_avail);
		long now=SystemClock.uptimeMillis();
		MapCache.Payload p=new MapCache.Payload();
		p.used=true;
		p.b=out;
		p.fake=fake;
		p.only_fake_available=only_fake_avail;
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
		HashSet<Key> intersection2 = new HashSet<Key>(faked);
		intersection2.retainAll(queryhistory);
		
		ArrayList<Key> intersection=new ArrayList<MapCache.Key>();
		intersection.addAll(intersection2);
		Collections.sort(intersection,new Comparator<Key>(){
			@Override
			public int compare(Key lhs, Key rhs) {
				if (lhs.zoomlevel<rhs.zoomlevel)
					return -1;
				if (lhs.zoomlevel>rhs.zoomlevel)
					return +1;
				return 0;
			}
		});
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
			//MapCache.Payload l=map.get(key);
			/*TODO: Do this: if (l!=null && l.fake)
				eject(key);*/
			if (!removed)
				throw new RuntimeException("query history remove failed: "+key);
		}
		return keys.toArray(new Key[]{});
	}
	public int mapsize()
	{
		int cnt=0;
		for(MapCache.Payload p:map.values())
			if (p.b!=null) ++cnt;
		return cnt;
		
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
	synchronized public MapCache.Payload query(iMerc m, int zoomlevel,boolean background_load) {
		
		
		if (zoomlevel!=0)
		{
			Key keepalive_parent=new Key(Project.imerc2imerc(m, zoomlevel, zoomlevel-1),zoomlevel-1);
			MapCache.Payload l=map.get(keepalive_parent);
			if (l!=null)
				l.used=true;
		}
		
		
		Key key=new Key(m,zoomlevel);
		//Log.i("fplan.adchart","Queried: "+m.getX()+","+m.getY());
		
		
		
		MapCache.Payload l=map.get(key);
		if (background_load)
		{
			//Log.i("fplan.bitmap","Considering adding to queryhist: "+m+" zoom: "+zoomlevel);
			if (l==null || (l.fake && !l.only_fake_available))
			{
				//Log.i("fplan.adchart","Missing, adding to queryhistory: "+m.getX()+","+m.getY()+" zoom: "+zoomlevel+" curr size: "+mapsize()+" history size: "+queryhistory.size());
				queryhistory.add(key);
			}
		}
		if (l!=null)
			l.used=true;
		return l;
	}
	synchronized public void garbageCollect() {
		ArrayList<Key> deletelist=new ArrayList<Key>();
		//Log.i("fplan.bitmap","garbageCollect, cache size: "+cachesize+" map: "+map.size());
		for(Entry<Key, MapCache.Payload> e:map.entrySet())
		{
			if (!e.getValue().used)
				deletelist.add(e.getKey());
			e.getValue().used=false;
		}
		for(Key d:deletelist)
		{
			//Log.i("fplan.adchart","Ejecting:"+d);				
			eject(d);
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
