package se.flightplanner2;

import java.util.Comparator;
import java.util.TreeSet;

import android.graphics.Rect;
import android.util.Log;

public class DeclutterTree {

	
	TreeSet<Rect> objs;
	static public class DeclutterComparator implements Comparator<Rect>
	{
		@Override
		public int compare(Rect a, Rect b) {
			if (a.top<b.top) return -1;
			if (a.top>b.top) return +1;
			if (a.left<b.left) return -1;
			if (a.left>b.left) return +1;
			return 0;
		}		
	}
	int maxtextsize;
	public DeclutterTree(int maxtextsize)
	{
		this.maxtextsize=maxtextsize;
		objs=new TreeSet<Rect>(new DeclutterComparator());
	}
	public boolean checkAndAdd(Rect cur)
	{
		for(Rect o : objs.subSet(new Rect(0,cur.top-maxtextsize+1,0,0), new Rect(0,cur.bottom+maxtextsize,0,0)))
		{
			if (Rect.intersects(o,cur))
			{
				return false;
			}
		}
		objs.add(cur);
		return true;
	}
	
}
