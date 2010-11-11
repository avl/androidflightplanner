package se.flightplanner.vector;

import java.util.ArrayList;

public class PolygonTriangulator {

	static public void triangulate(Polygon p,ArrayList<SimpleTriangle> out)	
	{
		int nump=p.numPoints();
		ArrayList<Vector> ps=p.get_points();
		if (nump<3) throw new RuntimeException("Polygon must have at least 3 points");
		if (nump==3)
		{
			SimpleTriangle sim=new SimpleTriangle();
			sim.a=ps.get(0);
			sim.b=ps.get(1);
			sim.c=ps.get(2);
			out.add(sim);
			return;
		}
		for(int attempt=0;attempt<nump;++attempt)
		{
			for(int sign=0;sign<2;++sign)
			{
				for(int i1=0;i1<nump;++i1)
				{
					int addi=attempt * (sign==0 ? -1 : 1);
					int idealIdx=(i1+nump/2);
					int testi=(idealIdx+addi);
					if (testi<0) testi+=nump;
					testi%=nump;
					if (testi==i1 || (testi+1)%nump==i1 || (i1+1)%nump==testi) continue;
					if (!intersectbadly(p,i1,testi))
					{
						ArrayList<Vector> firstv=new ArrayList<Vector>();
						ArrayList<Vector> secondv=new ArrayList<Vector>();
						for(int j=i1;;++j)
						{
							j%=nump;
							firstv.add(ps.get(j));
							if (j==testi) break;
						}
						secondv.add(ps.get(i1));
						for(int j=testi;;++j)
						{
							j%=nump;
							if (j==i1) break;
							secondv.add(ps.get(j));
						}
						
						Polygon first=new Polygon(firstv);
						Polygon second=new Polygon(secondv);
						triangulate(first,out);
						triangulate(second,out);
						return;
					}
				}
			}
		}
		throw new RuntimeException("Triangle was impossible to triangulate - no interior line could be drawn without intersecting an edge! This is geometrically impossible, so this must be a bug.");
	}

	private static boolean intersectbadly(Polygon p, int a, int b) {
		Vector end1=p.get_points().get(a);
		Vector end2=p.get_points().get(b);
		Line l=new Line(end1,end2);
		int num=p.numPoints();
		if (a==b) throw new RuntimeException("a==b");
		if ((a+1)%num==b || (b+1)%num==a)
			throw new RuntimeException("bad a b values2");
		if (a<0 || a>=num || b<0 || b>=num)
			throw new RuntimeException("Bad a b values");
		for(int i=0;i<num;++i)
		{
			if (i==a || i==b || (i+1)%num==a || (i+1)%num==b || (b+1)%num==i || (a+1)%num==i)
				continue;
			Line l2=new Line(p.get_points().get(i),p.get_points().get((i+1)%num));
			Vector v=Line.intersect(l,l2);
			if (v==null) continue; //don't intersect at all
			//they do intersect. But maybe not by more than a tiny amount?
			double diff1=v.minus(end1).length();
			double diff2=v.minus(end2).length();
			if (diff1<1e-3 || diff2<1e-3)
				continue;
			return true; //Bad intersection found.
		}
		Vector mid=end1.plus(end2).mul(0.5);
		if (!p.is_inside(mid))
		{
			//System.out.println("Checking intersection "+a+" / "+b+" but midpoint is outside...:"+mid);
			return true; //the candidate line is entirely outside the polygon!
		}
		//System.out.println("Checking intersection "+a+" / "+b+" midpoint is inside...:"+mid);
		return false;
	}
}
