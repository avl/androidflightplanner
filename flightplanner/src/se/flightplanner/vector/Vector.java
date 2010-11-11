package se.flightplanner.vector;

import java.io.Serializable;

public class Vector implements Serializable {
	
	private static final long serialVersionUID = -299765696841273686L;
	public double x; //Public for performance (or does it not matter?)
	public double y;
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
	public boolean equals(Vector vo)
	{
		Vector v=(Vector)vo;
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
	public double taxinorm() {
		return Math.abs(x)+Math.abs(y);
	}
	
	public Vector rot(double rad) {
		double nx=Math.cos(rad)*x - Math.sin(rad)*y;
		double ny=Math.sin(rad)*x + Math.cos(rad)*y;
		return new Vector(nx,ny);
	}
	public Vector unrot(double rad) {
		rad=-rad;
		double nx=Math.cos(rad)*x - Math.sin(rad)*y;
		double ny=Math.sin(rad)*x + Math.cos(rad)*y;
		return new Vector(nx,ny);
	}
	/*
	private double rot_y(double x,double y) {
		double rad=0;
		if (lastpos!=null && lastpos.hasBearing())
		{
			rad=(-Math.PI/180.0)*lastpos.getBearing();
		}
		return Math.sin(rad)*x + Math.cos(rad)*y;
	}*/
	
}
