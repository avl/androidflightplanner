package se.flightplanner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import se.flightplanner.Project.iMerc;
import android.graphics.Bitmap;
import android.os.SystemClock;

public class GetMapBitmap {
	Blob blob;
	public class Payload
	{
		public long lastuse;
		Bitmap b;
	}
	HashMap<iMerc,Payload> map;
	public GetMapBitmap(Blob blob_)
	{
		blob=blob_;
	}
	Bitmap getBitmap(iMerc m) throws IOException
	{
		long now=SystemClock.uptimeMillis();
		Payload l=map.get(m);
		if (l!=null)
		{
			l.lastuse=now;
			return l.b;
		}
		ArrayList<iMerc> deletelist=new ArrayList<iMerc>();
		for(Entry<iMerc, Payload> e:map.entrySet())
		{
			if (now-e.getValue().lastuse>30000)				
			deletelist.add(e.getKey());
		}
		for(iMerc d:deletelist)
			map.remove(d);
		Payload p=new Payload();
		p.lastuse=now;
		p.b=blob.get_bitmap(m);
		map.put(m,p);
		return p.b;
	}
	
	
}
