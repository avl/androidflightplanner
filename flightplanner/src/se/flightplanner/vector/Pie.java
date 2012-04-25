package se.flightplanner.vector;

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
    	while (a<0) a+=360;
    	while (b<0) b+=360;
        this.a=a%360.0;
        this.b=b%360.0;
        if (this.b<this.a)
            this.b+=360.0;
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



};

