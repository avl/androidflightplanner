package se.flightplanner.vector;

public class Pie
{
    private double a,b;

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
    
    public double size()
    {
        double size=b-a;
        return size;        
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
        if (o.b<=a || o.a>=b) return false;
        return true;
    }

	public Line getLine(int i) {
		Vector p;
		if (i==0) p=Vector.fromhdg(a); 
		else p=Vector.fromhdg(b);
		return new Line(new Vector(0,0),p);	
	}

	
    public boolean isInPie(double ang)
    {
    	if (ang>a && ang<b)
    		return true;
    	return false;
    }
	public boolean isInPie(Vector v) {
		return isInPie(v.hdg());
	}



};

