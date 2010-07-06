package se.flightplanner.vector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class QuadTreeWithPayload {

	static private class AxisSorter implements Comparator<Vector>
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
		public int compare(Vector a,Vector b)
		{
			if (axis==0)
				return fcompare(a.getx(),b.getx());
			else
				return fcompare(a.gety(),b.gety());
		}
	}
	@SuppressWarnings("unchecked")
	public QuadTreeWithPayload(
			ArrayList<Vector> points)
	{
		if (points.size()==0) throw new RuntimeException("empty point sequence to QuadTree");
		ArrayList<Vector> xsorted=points;
		ArrayList<Vector> ysorted=(ArrayList<Vector>)xsorted.clone();
		Collections.sort(xsorted,new AxisSorter(0));
		Collections.sort(ysorted,new AxisSorter(1));
		Vector pivot=new Vector(xsorted.get(xsorted.size()/2).getx(),ysorted.get(ysorted.size()/2).gety());
		ArrayList<ArrayList<Vector>> quadrants=new ArrayList<ArrayList<Vector>>();
		for(int i=0;i<4;++i)
			quadrants.add(new ArrayList<Vector>());
		for(Vector vec:points)
		{
			int quad=0;
			if (vec.getx()>=pivot.getx())
				quad+=1;
			if (vec.gety()>=pivot.gety())
				quad+=2;
			quadrants.get(quad).add(vec);
		}
		
		
	}
	
}
