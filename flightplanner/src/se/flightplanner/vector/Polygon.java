package se.flightplanner.vector;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public class Polygon implements Serializable {

	private static final long serialVersionUID = -4421004724531424354L;
	private ArrayList<Vector> points;
	
	/**
	 * Get the points of the Polygon.
	 * The returned points is not a copy, but
	 * the internal list kept by the Polygon object.
	 * If you modify the returned arraylist, the
	 * polygon changes!
	 */
	public ArrayList<Vector> get_points()
	{
		return points;
	}
	/**
	 * Points must form a counter-clockwise, non-intersecting polygon.
	 * An implicit edge is formed between the last and first point.
	 */
	public Polygon(ArrayList<Vector> ppoints)
	{
		if (ppoints.size()==0)
			throw new RuntimeException("Empty polygons (without points) are not supported");
		points=ppoints;
	}
	/**
	 * Points must form a counter-clockwise, non-intersecting polygon.
	 * An implicit edge is formed between the last and first point.
	 */
	public Polygon(Vector[] ppoints)
	{		
		if (ppoints.length==0)
			throw new RuntimeException("Empty polygons (without points) are not supported");
		points=new ArrayList<Vector>();
		for(Vector v:ppoints)
			points.add(v);
	}
	
	/**
	 * Return the area of the polygon.
	 * @return
	 */
	public double calc_area()
	{
	    double sum=0;
        for(int i=0;i<points.size();++i)
        {
            boolean up=false;
            double x1=points.get(i).getx();
            double y1=points.get(i).gety();
            double x2=points.get((i+1)%points.size()).getx();
            double y2=points.get((i+1)%points.size()).gety();
            if (y1<y2)
            {
                up=true;
            }
            double contrib=(y2-y1)*0.5*(x2+x1);
            sum+=contrib;       
        }
        return sum;
	}

	static public class InsideResult
	{
		public boolean isinside;
		public Vector closest;
		public InsideResult(Vector closest_edgepoint,boolean pisinside)
		{
			closest=closest_edgepoint;
			isinside=pisinside;
		}
	}
	public boolean is_inside(Vector p)
	{
		InsideResult res=inside(p);
		return res.isinside;
	}
	public Vector closest(Vector p)
	{
		InsideResult res=inside(p);
		return res.closest;
	}
	public InsideResult inside(Vector p)
	{
		Line closest_line=null;
		int closest_point=-1;
		double closest_dist=1e30;
		Vector closest_vec=null;
		for(int i=0;i<points.size();++i)
		{
			Vector a=points.get(i);
			Vector b=points.get((i+1)%points.size());
			Line l=new Line(a,b);
			Vector curvec=l.closest(p);
			double dist=curvec.minus(p).length();
			if (closest_vec==null || dist<closest_dist)
			{
				closest_vec=curvec;
				closest_dist=dist;
				if (curvec==a)
				{
					closest_point=i;
					closest_line=null;
				}
				else
				if (curvec==b)
				{
					closest_point=i+1;
					closest_line=null;
				}
				else
				{
					closest_point=-1;
					closest_line=l;
				}
			}				
		}
		if (closest_line!=null)
		{
			if (closest_line.side(p)==-1)
				return new InsideResult(closest_vec,true); //is inside
			return new InsideResult(closest_vec,false); //is outside
		}
		else if (closest_point!=-1)
		{
			int i=closest_point;
			Vector a=points.get((i+points.size()-1)%points.size());
			Vector b=points.get(i);
			Vector c=points.get((i+1)%points.size());
			Line l1=new Line(a,b);
			Line l2=new Line(b,c);
			boolean inside=false;
			int bend=Line.bend_dir(a, b, c);
			if (bend==-1)
			{ //left bend (convex)
				inside = l1.side(p)==-1 && l2.side(p)==-1;
			}
			else
			{ //right bend (concave)
				inside = l1.side(p)==-1 || l2.side(p)==-1;				
			}
			return new InsideResult(closest_vec,inside);
		}
		throw new RuntimeException("Unexpected error in Polygon");
	}
}
