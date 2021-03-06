package se.flightplanner2;

import java.util.ArrayList;
import java.util.Collections;

import android.util.Log;

import se.flightplanner2.vector.BoundingBox;
import se.flightplanner2.vector.BspTree;
import se.flightplanner2.vector.Vector;
import se.flightplanner2.vector.BspTree.Item;

public class AirspaceSigPointsTree {

	static public class Wrapper implements BspTree.Item
	{
		SigPoint payload;
		Vector where;
		public Object payload() {
			return payload;
		}
		public Vector vec() {
			return where;
		}
	}
	BspTree bsp;
	public AirspaceSigPointsTree(ArrayList<SigPoint> spacepoints)
	{
		Wrapper[] wps=new Wrapper[spacepoints.size()];
		int idx=0;
		for(SigPoint point:spacepoints)
		{
			Wrapper w=new Wrapper();
			w.payload=point;
			w.where=new Vector(point.pos.x,point.pos.y);
			//Log.i("fplan","Adding point: "+point.name+" at "+w.where);
			wps[idx++]=w;
		}
		bsp=new BspTree(wps);
	}
	public ArrayList<SigPoint> findall(BoundingBox box)
	{
		ArrayList<SigPoint> res=new ArrayList<SigPoint>();
		for(Item item:bsp.findall(box.x1,box.y1,box.x2,box.y2))
		{
			SigPoint point=(SigPoint)item.payload();
			res.add(point);
		}		
		///Collections.sort(res);
		return res;
	}
	public void verify() {
		bsp.verify(null);
		
	}
	public ArrayList<SigPoint> getall() {
		ArrayList<SigPoint> ret=new ArrayList<SigPoint>();
		for(Item item:bsp.getall())
			ret.add((SigPoint)item.payload());
		return ret;
	}
}
