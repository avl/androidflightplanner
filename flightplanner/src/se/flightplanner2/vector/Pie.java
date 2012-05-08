package se.flightplanner2.vector;

import android.util.Log;

public class Pie
{
    private double a,b;

    public double getA(){return a;} 
    public double getB(){return b;}
    
    /*!
     * Boundary of pie in degrees.
     */
    public Pie(double a,double b)
    {
        if (a<-1e6 || b<-1e6) throw new RuntimeException("Very negative angles are not allowed");
        while(b<a) b+=360.0;
        double size=b-a;
    	while (a<0) a+=360;    	
        this.a=a%360.0;
        this.b=a+size;        
    }
    
    @Override
    public String toString()
    {
    	return "Pie("+a+","+b+")";
    }
    
    public double size()
    {
        return b-a;        
    }
    
    /**
     * Returns null if intersection would be a zero-size pie.
     */
    public Pie intersect(Pie o)
    {
        if (o.b<=a || o.a>=b) return null;
        double na=Math.max(a,o.a);
        double nb=Math.min(b,o.b);
        return new Pie(na,nb);
    }

    public boolean check_intersect(Pie o)
    {
    	if (size()<o.size())
    		return o.check_intersect_with_smaller(this);
    	return check_intersect_with_smaller(o);
    }
    private boolean check_intersect_with_smaller(Pie o)
    {
    	if (isInPie(o.a, 1e-8)) return true;
    	if (isInPie(o.b, 1e-8)) return true;
    	return false;
    }

    /**
     * From origo and in direction of side i of Pie.
     */
	public Line getLine(int i) {
		Vector p;
		if (i==0) p=Vector.fromhdg(a); 
		else p=Vector.fromhdg(b);
		return new Line(new Vector(0,0),p);	
	}

	
    public boolean isInPie(double ang)
    {
    	return isInPie(ang,0);
    }
    public boolean isInPie(double ang,double epsilon)
    {
        if (ang<-1e6) throw new RuntimeException("Very negative angles are not allowed");
        while (ang<0) ang+=360.0;
        ang=ang%360.0;
    	if (ang>=a-epsilon && ang<=b+epsilon)
    		return true;
    	if (ang>=a-360-epsilon && ang<=b-360+epsilon)
    		return true;
    	return false;
    }
	public boolean isInPie(Vector v) {
		return isInPie(v.hdg());
	}
	public boolean isInPie(Vector v,double epsilon) {
		return isInPie(v.hdg(),epsilon);
	}
	public Pie swingRight(float x) {
		if (x<0) return swingLeft(-x);
		Pie pie=new Pie(a,b);
		pie.a+=x;
		pie.b+=x;
		if (pie.a>360 && pie.b>360)
		{
			pie.a-=360;
			pie.b-=360;
		}
		//Log.i("fplan","Swung "+this+" right by "+x+" yielding "+pie);
		return pie;
	}
	public Pie swingLeft(float x) {
		if (x<0) return swingRight(-x);
		Pie pie=new Pie(a,b);
		pie.a-=x;
		pie.b-=x;
		if (pie.a<0 || pie.b<0)
		{
			pie.a+=360;
			pie.b+=360;
		}
		return pie;
	}
	public boolean isAtAllRightOf(Pie pie) {
		//returns true if 'this' can be conceivably considered
		//to be to the right of 'pie'.
		//this is determined by simply looking at the midpoints of the two
		//pies. This can return true even if they overlap.
		double thismid=0.5*(a+b);
		double piemid=0.5*(pie.a+pie.b);
		double delta=thismid-piemid;
		
		while(delta>180) delta-=360;
		while(delta<-180) delta+=360;
		return delta>=0;
	}
	public double getSize() {

		return b-a;
	}
	public double getCenter() {		
		return 0.5*(a+b);
	}
	public float getRelativePos(double x) {
		double center=getCenter();
		double off=x-center;
		while (off>180) off-=360;
		while (off<-180) off+=360;
		double size=getSize();
		double rel=off/size+0.5;
		if (rel>1) rel=1;
		if (rel<0) rel=0;
		//Log.i("fplan"," - PIe: "+this+" relpos of "+x+" = "+rel+" off: "+off);
		return (float)rel;
	}



};

