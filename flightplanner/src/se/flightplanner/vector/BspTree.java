package se.flightplanner.vector;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

import android.util.Log;

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
			if (a>b) return +1;
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
			return; //no points
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
		if (pivot==null) return; //no items
		if (!box.overlaps(curbox))
			return;
		//System.out.println(String.format("Considering box %s (belonging to %s)",curbox,pivot.payload()));
		out.add(pivot);
		//System.out.println(String.format("exploring %s, bounding box %s",pivot.vec(),curbox));
		BoundingBox left=curbox.clone();
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

	
	//finds the lowest (furthest from root) item in the 
    //tree whose two belonging sub-boxes taken together
	//completely cover the given box.
	public Item find_item_dominating(BoundingBox box)
	{ 
		if (pivot==null) return null; //no items
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
	public void verify(BoundingBox bb)
	{
		if (bb==null)
		{
			bb=new BoundingBox(-1e30,-1e30,1e30,1e30);
		}
		Vector pivotvec=pivot.vec();
		if (!(pivotvec.getx()>=bb.x1-1e-4 && pivotvec.getx()<=bb.x2+1e-4 &&
			pivotvec.gety()>=bb.y1-1e-4 && pivotvec.gety()<=bb.y2+1e-4))
			throw new RuntimeException("pivotvec "+pivotvec+" not within limits "+bb);
		BoundingBox bb1;
		BoundingBox bb2;
		if (axis==0)
		{
			bb1=bb.clone();
			bb1.x2=pivotvec.getx();
			bb2=bb.clone();
			bb2.x1=pivotvec.getx();
		}
		else
		{
			bb1=bb.clone();
			bb1.y2=pivotvec.gety();
			bb2=bb.clone();
			bb2.y1=pivotvec.gety();			
		}
		if (a!=null) a.verify(bb1);
		if (b!=null) b.verify(bb2);
		
	}
	public void findall(double x1,double y1,double x2,double y2,ArrayList<Item> items)
	{
		if (pivot==null) return; //no items
		Vector pivotvec=pivot.vec();
		if (pivotvec.getx()>=x1 && pivotvec.getx()<x2 &&
			pivotvec.gety()>=y1 && pivotvec.gety()<y2)
		{
			items.add(pivot);
		}
		/*
		if (a!=null)
			a.findall(x1,y1,x2,y2,items);
		if (b!=null)
			b.findall(x1,y1,x2,y2,items);
		*/
		double epsilon=1e-3;
		if (axis==0)
		{
			double pivotval=pivotvec.getx();
			if (a!=null)
			{
				if (x1-epsilon<=pivotval)
				{
					a.findall(x1, y1, x2, y2, items);
				}
				else
				{
					//Log.i("fplan","Ignoring left subtree of pivot "+pivotvec);
				}
			}
			if (b!=null)
			{
				if (pivotval-epsilon<=x2)
				{
					b.findall(x1, y1, x2, y2, items);
				}
				else
				{
					//Log.i("fplan","Ignoring right subtree of pivot "+pivotvec);					
				}
			}
		}
		else
		{
			double pivotval=pivotvec.gety();
			if (a!=null)
			{
				if (y1-epsilon<=pivotval)
				{
					a.findall(x1, y1, x2, y2, items);
				}
				else
				{					
					//Log.i("fplan","Ignoring lower subtree of pivot "+pivotvec);					
				}
			}
			if (b!=null)
			{
				if (pivotval-epsilon<=y2)
				{
					b.findall(x1, y1, x2, y2, items);
				}
				else
				{
					//Log.i("fplan","Ignoring upper subtree of pivot "+pivotvec);										
				}
			}
		}
					
	}
	public ArrayList<Item> findall(BoundingBox b) {

		return findall(b.x1,b.y1,b.x2,b.y2);
	}
	
}
