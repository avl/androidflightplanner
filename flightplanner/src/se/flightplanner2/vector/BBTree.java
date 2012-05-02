package se.flightplanner2.vector;

import java.io.Serializable;
import java.util.ArrayList;

import se.flightplanner2.vector.BspTree.Item;

public class BBTree implements Serializable {
	private static final long serialVersionUID = -5257173690706220669L;
	static public interface Item
	{
		BoundingBox bb();
		public Object payload();
	}
	static public class BspAdapter implements BspTree.Item, Serializable
	{
		private static final long serialVersionUID = -1853586680748452653L;
		private Vector vecObj;
		public ArrayList<Item> items;
		BspAdapter(Vector v)
		{
			vecObj=v;
			items=new ArrayList<Item>();
		}
		final public Object payload() {
			return this;
		}
		public Vector vec() {
			// TODO Auto-generated method stub
			return vecObj;
		}		
	}
	
	public ArrayList<Item> overlapping(BoundingBox box)
	{
		ArrayList<Item> result=new ArrayList<Item>();
		
		for(BspTree.Item bspitem : bsp.items_whose_dominating_area_overlaps(box))
		{
			BspAdapter payload=(BspAdapter)bspitem.payload();
			for(Item item:payload.items)
				if (item.bb().overlaps(box))
					result.add(item);
		}
		return result;
	}
	
	private BspTree bsp;
	public BBTree(ArrayList<Item> items,double epsilon)
	{
		BspTree.Item[] itarr=new BspTree.Item[items.size()*2];
		int idx=0;
		for(Item item:items)
		{
			BoundingBox bb=item.bb();
			BspAdapter ad1=new BspAdapter(new Vector(bb.x1,bb.y1));
			BspAdapter ad2=new BspAdapter(new Vector(bb.x2,bb.y2));			
			itarr[idx++]=ad1;
			itarr[idx++]=ad2;
			
		}
		bsp=new BspTree(itarr,0,itarr.length,0);
		for(Item item:items)
		{
			BoundingBox bb=item.bb();
			BspAdapter dom=(BspAdapter)bsp.find_item_dominating(new BoundingBox(
					bb.x1-epsilon,
					bb.y1-epsilon,
					bb.x2+epsilon,
					bb.y2+epsilon));
			dom.items.add(item);
			
		}
	}
	
	
}
