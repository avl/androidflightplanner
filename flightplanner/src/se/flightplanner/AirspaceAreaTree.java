package se.flightplanner;

import java.io.Serializable;
import java.util.ArrayList;

import se.flightplanner.vector.BBTree;
import se.flightplanner.vector.BoundingBox;
import se.flightplanner.vector.Vector;
import se.flightplanner.vector.BBTree.Item;

public class AirspaceAreaTree implements Serializable {

	private static final long serialVersionUID = 5554335431911565203L;
	private BBTree tree;
	public class Wrapper implements BBTree.Item,Serializable
	{
		private static final long serialVersionUID = -5472167214569625920L;
		public BoundingBox bb;
		public AirspaceArea space;
		public BoundingBox bb() {
			return bb;
		}
		public Object payload() {
			return space;
		}
	}
	/**
	 * Boundingbox coords are zoomlevel 13 mercator coords.
	 * @param bb
	 * @return
	 */
	public ArrayList<AirspaceArea> get_areas(BoundingBox bb)
	{
		ArrayList<AirspaceArea> result=new ArrayList<AirspaceArea>();
		for(Object obj:tree.overlapping(bb))
		{
			Wrapper wrap=(Wrapper)obj;
			result.add(wrap.space);
		}
		return result;
	}
	public AirspaceAreaTree(ArrayList<AirspaceArea> airspace_areas)
	{
		ArrayList<Item> treeitems=new ArrayList<Item>();
		for(AirspaceArea area:airspace_areas)
		{
			if (area.poly.get_points().size()<3)
				throw new RuntimeException("Too few points in airspace");
			BoundingBox bb=new BoundingBox(1e30,1e30,-1e30,-1e30);
			for(Vector v : area.poly.get_points())
			{
				if (v.getx()<bb.x1) bb.x1=v.getx();
				if (v.getx()>bb.x2) bb.x2=v.getx();
				if (v.gety()<bb.y1) bb.y1=v.gety();
				if (v.gety()>bb.y2) bb.y2=v.gety();
			}
			Wrapper wrap=new Wrapper();
			wrap.bb=bb;
			wrap.space=area;
			//System.out.println("Adding area/poly: "+bb);
			treeitems.add(wrap);
		}
		tree=new BBTree(treeitems,0.1);
	}
}
