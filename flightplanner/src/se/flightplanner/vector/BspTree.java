package se.flightplanner.vector;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

public class BspTree implements Serializable {

	private static final long serialVersionUID = 4620924507085436649L;

	public static interface Item
	{
		public Vector vec();
		public Object payload();
	}
	
	static private class AxisSorter implements Comparator<Item>
	{
		int axis;
		AxisSorter(int paxis)
		{
			axis=paxis;
		}
		static int fcompare(double a,double b)
		{
			if (a<b) return -1;
			if (b>a) return +1;
			return 0;
		}
		public int compare(Item a,Item b)
		{
			if (axis==0)
				return fcompare(a.vec().getx(),b.vec().getx());
			else
				return fcompare(a.vec().gety(),b.vec().gety());
		}
	}
	private BspTree a;
	private BspTree b;
	private Item pivot;
	private int axis;
	public BspTree(Item[] items)
	{
		init(items,0,items.length,0);
	}
	public BspTree(Item[] points,int idx1,int idx2,int startaxis)
	{
		init(points, idx1, idx2, startaxis);
	}
	private void init(Item[] points, int idx1, int idx2, int startaxis) {
		if (idx2-idx1<=0)
			throw new RuntimeException("no points in BspTree");
		Arrays.sort(points,idx1,idx2,new AxisSorter(startaxis));
		int mididx=idx1+(idx2-idx1)/2;
		pivot=points[mididx];
		axis=startaxis;
		if (mididx>idx1)
			a=new BspTree(points,idx1,mididx,(startaxis+1)%2);
		if (mididx+1<idx2)
			b=new BspTree(points,mididx+1,idx2,(startaxis+1)%2);
	}
	public ArrayList<Item> findall(double x1,double y1,double x2,double y2)
	{
		ArrayList<Item> items=new ArrayList<Item>();
		findall(x1,y1,x2,y2,items);
		return items;
	}
	
	public ArrayList<Item> items_whose_dominating_area_overlaps(BoundingBox box)
	{
		ArrayList<Item> out=new ArrayList<Item>();
		BoundingBox curbox=new BoundingBox(-1e30,-1e30,1e30,1e30);
		items_whose_dominating_area_overlaps_impl(box,curbox,out);
		return out;
	}
	public void items_whose_dominating_area_overlaps_impl(BoundingBox box,BoundingBox curbox,ArrayList<Item> out)
	{
		if (!box.overlaps(curbox))
			return;
		System.out.println(String.format("Considering box %s (belonging to %s)",curbox,pivot.payload()));
		out.add(pivot);
		//System.out.println(String.format("exploring %s, bounding box %s",pivot.vec(),curbox));
		BoundingBox left=curbox;
		BoundingBox right=curbox.clone();
		if (axis==0)
		{
			left.x2=pivot.vec().getx();			
			right.x1=pivot.vec().getx();			
		}
		else
		{
			left.y2=pivot.vec().gety();
			right.y1=pivot.vec().gety();			
		}
		if (a!=null)
			a.items_whose_dominating_area_overlaps_impl(box,left,out);
		if (b!=null)
			b.items_whose_dominating_area_overlaps_impl(box,right,out);
	}
	
	//finds the highest (closest to root) item in the 
    //tree whose two belonging sub-boxes taken together
	//completely cover the given box.
	public Item find_item_dominating(BoundingBox box)
	{ 
		BoundingBox curbox=new BoundingBox(-1e30,-1e30,1e30,1e30);
		return find_item_dominating_impl(box,curbox);
	}
	private Item find_item_dominating_impl(BoundingBox box,BoundingBox curbox)
	{ 
		//System.out.println(String.format("finding %s, bounding box %s",pivot.vec(),curbox));
		if (curbox.covers(box)==false)
			return null;
		BoundingBox left=curbox;
		BoundingBox right=curbox.clone();
		if (axis==0)
		{
			left.x2=pivot.vec().getx();			
			right.x1=pivot.vec().getx();			
		}
		else
		{
			left.y2=pivot.vec().gety();
			right.y1=pivot.vec().gety();			
		}
		Item leftdom=null;
		Item rightdom=null;
		if (a!=null) leftdom=a.find_item_dominating_impl(box,left);
		if (b!=null) rightdom=b.find_item_dominating_impl(box,left);

		if (leftdom!=null) return leftdom;
		if (rightdom!=null) return rightdom;
		return pivot;
	}
	
	public void findall(double x1,double y1,double x2,double y2,ArrayList<Item> items)
	{
		Vector pivotvec=pivot.vec();
		if (pivotvec.getx()>=x1 && pivotvec.getx()<x2 &&
			pivotvec.gety()>=y1 && pivotvec.gety()<y2)
		{
			items.add(pivot);
		}
		if (axis==0)
		{
			double pivotval=pivotvec.getx();
			if (x1<=pivotval && a!=null)
				a.findall(x1, y1, x2, y2, items);
			if (pivotval<=x2 && b!=null)
				b.findall(x1, y1, x2, y2, items);
		}
		else
		{
			double pivotval=pivotvec.gety();
			if (y1<=pivotval && a!=null)
				a.findall(x1, y1, x2, y2, items);
			if (pivotval<=y2 && b!=null)
				b.findall(x1, y1, x2, y2, items);
		}			
	}
	
}
