package se.flightplanner.vector;

import java.util.ArrayList;

import se.flightplanner.vector.BspTree.BoundingBox;
import se.flightplanner.vector.BspTree.Item;

public class BBTree {

	static public interface Item
	{
		/// Upper left corner of bounding box 
		public Vector p1();
		/// Lower right corner of bounding box
		public Vector p2();		
		public Object payload();
	}
	static public class BspAdapter implements BspTree.Item
	{
		private Object payloadObj;
		private Vector vecObj;
		BspAdapter(Vector v,Object pl)
		{
			vecObj=v;
			payloadObj=pl;
		}
		public Object payload() {
			return payloadObj;
		}
		public ArrayList<Item> items;
		public Vector vec() {
			// TODO Auto-generated method stub
			return vecObj;
		}		
	}
	private BspTree bsp;
	public BBTree(ArrayList<Item> items,double epsilon)
	{
		BspTree.Item[] itarr=new BspTree.Item[items.size()*2];
		int idx=0;
		for(Item item:items)
		{
			BspAdapter ad1=new BspAdapter(item.p1(),item.payload());
			BspAdapter ad2=new BspAdapter(item.p2(),item.payload());			
			itarr[idx++]=ad1;
			itarr[idx++]=ad2;
			
		}
		bsp=new BspTree(itarr,0,itarr.length,0);
		for(Item item:items)
		{
			BspAdapter dom=(BspAdapter)bsp.find_item_dominating(new BoundingBox(
					item.p1().getx()-epsilon,
					item.p1().gety()-epsilon,
					item.p2().getx()+epsilon,
					item.p2().gety()+epsilon));
			dom.items.add(item);
			
		}
	}
	
	
}
