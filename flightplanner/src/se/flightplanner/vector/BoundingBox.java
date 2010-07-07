/**
 * 
 */
package se.flightplanner.vector;

import java.io.Serializable;

public class BoundingBox implements Cloneable, Serializable
{
	private static final long serialVersionUID = 2356282658637933688L;
	public double x1;
	public double y1;
	public double x2;
	public double y2;
	public String toString()
	{
		return String.format("BB(%.2g,%.2g,%.2g,%.2g)", x1,y1,x2,y2);
	}
	public BoundingBox(double px1,double py1,double px2,double py2)
	{
		x1=px1;
		y1=py1;
		x2=px2;
		y2=py2;			
	}
	public BoundingBox clone()
	{
		BoundingBox ret=new BoundingBox(x1,y1,x2,y2);
		return ret;
	}
	public boolean covers(BoundingBox other)
	{
		if (x1<=other.x1 && x2>=other.x2 &&
			y1<=other.y1 && y2>=other.y2)
			return true;
		return false;
	}
	public boolean overlaps(BoundingBox other)
	{
		if (x1<=other.x2 && x2>=other.x1 &&
			y1<=other.y2 && y2>=other.y1)
			return true;
		return false;
	}
}