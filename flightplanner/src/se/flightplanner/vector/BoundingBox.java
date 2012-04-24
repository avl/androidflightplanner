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
		return String.format("BB(%.2f,%.2f,%.2f,%.2f)", x1,y1,x2,y2);
	}
	public BoundingBox(Vector center,double size)
	{
		x1=center.getx()-size;
		y1=center.gety()-size;
		x2=center.getx()+size;
		y2=center.gety()+size;		
	}
	public BoundingBox expand(double h)
	{
		return new BoundingBox(x1-h,y1-h,x2+h,y2+h);
	}
	public static BoundingBox point(Vector v)
	{
		return new BoundingBox(v.getx(),v.gety(),v.getx(),v.gety());
	}
	public static BoundingBox aroundpoint(Vector v,double size)
	{
		return new BoundingBox(v.getx()-size,v.gety()-size,v.getx()+size,v.gety()+size);
	}
	
	public BoundingBox(double px1,double py1,double px2,double py2)
	{
		x1=px1;
		y1=py1;
		x2=px2;
		y2=py2;			
	}
	public BoundingBox(Vector lowerleft, Vector upperright) {
		x1=lowerleft.getx();
		y1=lowerleft.gety();
		x2=upperright.getx();
		y2=upperright.gety();
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
	public boolean covers(Vector vec) {
		return vec.getx()>=x1 && vec.getx()<=x2 &&
			vec.gety()>=y1 && vec.gety()<=y2;
	}
	public Vector lowerleft() {
		return new Vector(x1,y1);
	}
	public Vector upperright() {
		return new Vector(x2,y2);
	}
	public boolean almostEquals(BoundingBox bb, double d) {		
		return lowerleft().almostEquals(bb.lowerleft(), d) &&
			upperright().almostEquals(bb.upperright(), d);
		
	}
}