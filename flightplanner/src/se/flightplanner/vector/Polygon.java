package se.flightplanner.vector;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public class Polygon implements Serializable {

	private static final long serialVersionUID = -4421004724531424354L;
	private ArrayList<Vector> points;

	public int numPoints()
	{
		return points.size();
	}
	public String toString()
	{
		StringBuilder b=new StringBuilder();
		b.append("Polygon(");
		for(Vector v : points)
			b.append(" "+v.toString());
		b.append(" )");
		return b.toString();
	}
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
	public ArrayList<Line> getLines() {
		ArrayList<Line> res=new ArrayList<Line>();
		for(int i=0;i<points.size();++i)
		{
			Vector a=points.get(i);
			Vector b=points.get((i+1)%points.size());
			Line l=new Line(a,b);
			res.add(l);
		}
		return res;
	}
	public static class SectorResult
	{		
		public double nearest_distance_to_center; //of sector		
		public boolean inside; //if center is inside area
		public Pie pie;
	}
	public SectorResult sector(BoundPie center)
	{
	    if (points.size()==0)
	        return null;
	    SectorResult res=new SectorResult();
        InsideResult ir=inside(center.pos);
        res.inside=ir.isinside;
        if (res.inside)
        {
        	res.nearest_distance_to_center=0;
        	res.pie=new Pie(0,360);
        	return res;
        }
        ArrayList<Line> lines=new ArrayList<Line>();
        for(int i=0;i<points.size();++i)
        {
        	Line l=new Line(points.get(i),points.get((i+1)%points.size()));
        	Line out=center.cut(l);
        	//System.out.println("Cut "+l+" to "+out+" ("+center+")");
        	if (out!=null)
        		lines.add(out);
        }
        if (lines.size()==0)
        {
        	res.nearest_distance_to_center=1e30;
        	return res;
        }
        
                
        Vector delta=lines.get(0).getv1().minus(center.pos);
        double cur_a=delta.hdg();
        double anglea=cur_a;
        double angleb=cur_a;
        double dist=1e30;
        
        
        
        for(int i=0;i<lines.size();++i)
        {
        	Line l=lines.get(i);
        	System.out.println("Filtered line: "+l);
        	double curdist=l.distance(center.pos);
        	if (curdist<dist)
        		dist=curdist;
        	for(Vector p:new Vector[]{l.getv1(),l.getv2()})
        	{
        		System.out.println("The p: "+p);
	            delta=p.minus(center.pos);
	            double x=delta.hdg();
	            double turn=x-cur_a;
	            System.out.println("Pre-step: cur_a: "+cur_a+" x: "+x+" turn: "+turn+" state: "+anglea+" "+angleb);
	        	    if (turn<-180) turn+=360;
	            if (turn>180) turn-=360;
	            if (turn<-1e-8) //left
	            {
	            	if (!in(anglea,angleb,x))
	            		anglea=x;
	            }
	           	else if (turn>1e-8)
	           	{
	            	if (!in(anglea,angleb,x))
	            		angleb=x;           		
	           	}
	            cur_a=x;
        	}
        }
        res.pie=new Pie(anglea,angleb);
        //System.out.println("Resulting pie: "+res.pie);
        res.nearest_distance_to_center=dist;
        return res;
               
	}
	private boolean in(double anglea, double angleb, double x) {
		if (anglea<angleb)
		{
			return x>=anglea && x<=angleb;
		}
		if (anglea>angleb)
		{
			return x>=anglea || x<=angleb;
		}
		
		return false;
	}
	
}
