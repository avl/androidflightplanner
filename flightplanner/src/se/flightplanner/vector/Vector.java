package se.flightplanner.vector;

public class Vector {
	private double x; 
	private double y;
	public Vector()
	{
		x=0;y=0;
	}
	public double getx(){return x;}
	public double gety(){return y;}
	public Vector(double px,double py)
	{
		x=px;
		y=py;
	}
	public boolean equals(Vector v)
	{
		return x==v.x && y==v.y;
	}
	public boolean almostEquals(Vector v,double epsilon)
	{
		double dx=Math.abs(x-v.x);
		double dy=Math.abs(y-v.y);
		return dx<epsilon && dy<epsilon;
	}
	public String toString()
	{
		return String.format("Vector(%f,%f)", x,y);
	}
	public double length()
	{
		return Math.sqrt(x*x+y*y);
	}
	public Vector normalized()
	{
		double l=length();
		if (l==0) return new Vector(1,0);
		double il=1.0/l;
		return new Vector(il*x,il*y);
	}
	public Vector rot90l()
	{
		return new Vector(-y,x);
	}
	public Vector rot90r()
	{
		return new Vector(y,-x);
	}
	public Vector plus(Vector o)
	{
		return new Vector(x+o.x,y+o.y);
	}
	public Vector minus(Vector o)
	{
		return new Vector(x-o.x,y-o.y);
	}
	public Vector mul(double o)
	{
		return new Vector(o*x,o*y);
	}
	public double scalarprod(Vector o)
	{
		return x*o.x+y*o.y;
	}
}
