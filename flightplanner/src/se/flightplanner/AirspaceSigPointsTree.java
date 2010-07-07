package se.flightplanner;

import java.util.ArrayList;

import se.flightplanner.vector.BoundingBox;
import se.flightplanner.vector.BspTree;
import se.flightplanner.vector.Vector;
import se.flightplanner.vector.BspTree.Item;

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
		return res;
	}
}
