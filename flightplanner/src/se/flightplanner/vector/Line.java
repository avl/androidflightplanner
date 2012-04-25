package se.flightplanner.vector;

public class Line {
	private Vector a;
	private Vector b;

	/**
	 * Create a line from pa -> pb.
	 * The Vectors are not copied. 
	 */
	public Line(Vector pa, Vector pb) {
		a = pa;
		b = pb;
	}
	public Vector dir()
	{
		return b.minus(a).normalized();
	}
	/**
	 * If the line intersects a horizontal line with y-coordinate y, return the
	 * intersection point of the line with the horizontal line. Otherwise return
	 * null. If the line almost intersects, it is counted as intersecting. The
	 * epsilon is 1e-6*y.
	 * 
	 * If there are multiple intersection points (that is, if this line is also
	 * horizontal), return the point closest to nearx.
	 */
	public Vector approx_intersect_horiz_line(double y, double nearx) {
		double epsilon = (1e-6) * Math.abs(y) + 1e-9;
		if (Math.max(a.gety(), b.gety()) > y - epsilon
				&& Math.min(a.gety(), b.gety()) < y + epsilon) {
			if (Math.abs(a.gety() - b.gety()) < epsilon) { // this line is also
															// horizontal
				if (a.getx() < nearx && b.getx() < nearx)
					return new Vector(Math.max(a.getx(), b.getx()), y);
				if (a.getx() > nearx && b.getx() > nearx)
					return new Vector(Math.min(a.getx(), b.getx()), y);
				return new Vector(nearx, y);
			}
			double along = (y - a.gety()) / (b.gety() - a.gety());
			double dx = along * (b.getx() - a.getx());
			double x = a.getx() + dx;
			return new Vector(x, y);
		}
		return null;
	}
	public BoundingBox boundingBox()
	{
		return new BoundingBox(
				Math.min(a.getx(),b.getx()),
				Math.min(a.gety(),b.gety()),
				Math.max(a.getx(),b.getx()),
				Math.max(a.gety(),b.gety())
				);
	}
	/**
	 * return which side of the line the point p is. -1 = left +1 = right (or on
	 * line)
	 */
	public int side(Vector p) {
		Vector backrel = p.minus(a);
		Vector right = (b.minus(a)).normalized().rot90r();
		if (right.scalarprod(backrel) >= 0)
			return 1;
		else
			return -1;
	}

	/**
	 * Return the point on the line which is closest to the given point p.
	 * If one of the endpoints is the closest, that same vector will be returned,
	 * not a copy.
	 */
	public Vector closest(Vector p) {
		Vector backrel = p.minus(b);
		Vector frontrel = p.minus(a);
		Vector dir = b.minus(a);
		if (frontrel.scalarprod(dir) < 0) {
			return a;
		}
		if (backrel.scalarprod(dir) > 0) {
			return b;
		}
		Vector dir2 = dir.normalized();
		double along = dir2.scalarprod(frontrel);
		return a.plus(dir2.mul(along));
	}
	public double distance(Vector p)
	{
		Vector clo=closest(p);
		return clo.minus(p).length();
	}
	/**
	 * Which direction does the bend of the line a-b-c, at 
	 * point c, bend?
	 * -1 = left
	 * +1 = right
	 * @return
	 */
	static public int bend_dir(Vector a,Vector b,Vector c)
	{
		Line l=new Line(a,b);
		if (l.side(c)==-1)
			return -1;
		else
			return +1;
	}
	
	static public Vector intersect(Line l1,Line l2)
	{
		/*
x1=a+b*i
y1=c+d*i
x2=e+f*j
y2=g+h*j


a+b*i=e+f*j
c+d*i=g+h*j

a+b*i-e-f*j=0
c+d*i-g-h*j=0

a-e+b*i-f*j=0
c-g+d*i-h*j=0

b*i-f*j=-(a-e)
d*i-h*j=-(c-g)

b*i-f*j=-(a-e)
d*i-h*j=-(c-g)

d*i=-(c-g)+h*j
i=(-(c-g)+h*j)/d


b*(-(c-g)+h*j)/d-f*j=-(a-e)

b*(-(c-g)/d+h*j/d)-f*j=-(a-e)
b*-(c-g)/d+b*h*j/d-f*j=-(a-e)
b*-(c-g)/d+j*(b*h/d-f)=-(a-e)
j*(b*h/d-f)=-(a-e)+b*(c-g)/d
j=(-(a-e)+b*(c-g)/d)/(b*h/d-f)
j=(-(a-e)*d+b*(c-g))/(b*h-f*d)
j=(b*(c-g)-(a-e)*d)/(b*h-f*d)

Q=(b*h-f*d)
j=(b*(c-g)-(a-e)*d)/Q

a+b*i=e+f*j
b*i=e+f*j-a
i=(e+f*j-a)/b

		 */
		
		double a=l1.a.getx(); 
		double b=(l1.b.getx()-l1.a.getx());
		double c=l1.a.gety();
		double d=(l1.b.gety()-l1.a.gety());

		double e=l2.a.getx(); 
		double f=(l2.b.getx()-l2.a.getx());
		double g=l2.a.gety();
		double h=(l2.b.gety()-l2.a.gety());
		double Q=(b*h-f*d);
		//System.out.println("Q="+Q);
		if (Math.abs(Q)<1e-10)
			return null; //lines are almost parallel
		double j=(b*(c-g)-(a-e)*d)/Q;
		double i=0;
		//System.out.println("b="+Q+" d="+d);
		if (Math.abs(b)>1e-10)
			i=(e+f*j-a)/b;
		else
		if (Math.abs(d)>1e-10)
			i=(g+h*j-c)/d;
		else
			return null; //Both b and d are close to 0 -> line is extremely short
		//System.out.println("i="+i+" j="+j);
		if (i>=-1e-6 && i<=1+1e-6 &&
			j>=-1e-6 && j<=1+1e-6)
		{
			Vector res=new Vector(
				a+b*i,
				c+d*i);
			return res;
		}
		return null;
	}

	/*!
	 * Intersect with an infinitely long line 2 (the line2
	 * is thought about as starting at its starting point, then
	 * continuing forever in the direction toward its second point).
	 */
	static public Vector intersect_inf2(Line l1,Line l2)
	{
		/*
x1=a+b*i
y1=c+d*i
x2=e+f*j
y2=g+h*j


a+b*i=e+f*j
c+d*i=g+h*j

a+b*i-e-f*j=0
c+d*i-g-h*j=0

a-e+b*i-f*j=0
c-g+d*i-h*j=0

b*i-f*j=-(a-e)
d*i-h*j=-(c-g)

b*i-f*j=-(a-e)
d*i-h*j=-(c-g)

d*i=-(c-g)+h*j
i=(-(c-g)+h*j)/d


b*(-(c-g)+h*j)/d-f*j=-(a-e)

b*(-(c-g)/d+h*j/d)-f*j=-(a-e)
b*-(c-g)/d+b*h*j/d-f*j=-(a-e)
b*-(c-g)/d+j*(b*h/d-f)=-(a-e)
j*(b*h/d-f)=-(a-e)+b*(c-g)/d
j=(-(a-e)+b*(c-g)/d)/(b*h/d-f)
j=(-(a-e)*d+b*(c-g))/(b*h-f*d)
j=(b*(c-g)-(a-e)*d)/(b*h-f*d)

Q=(b*h-f*d)
j=(b*(c-g)-(a-e)*d)/Q

a+b*i=e+f*j
b*i=e+f*j-a
i=(e+f*j-a)/b

		 */
		
		double a=l1.a.getx(); 
		double b=(l1.b.getx()-l1.a.getx());
		double c=l1.a.gety();
		double d=(l1.b.gety()-l1.a.gety());

		double e=l2.a.getx(); 
		double f=(l2.b.getx()-l2.a.getx());
		double g=l2.a.gety();
		double h=(l2.b.gety()-l2.a.gety());
		double Q=(b*h-f*d);
		//System.out.println("Q="+Q);
		if (Math.abs(Q)<1e-10)
			return null; //lines are almost parallel
		double j=(b*(c-g)-(a-e)*d)/Q;
		double i=0;
		//System.out.println("b="+Q+" d="+d);
		if (Math.abs(b)>1e-10)
			i=(e+f*j-a)/b;
		else
		if (Math.abs(d)>1e-10)
			i=(g+h*j-c)/d;
		else
			return null; //Both b and d are close to 0 -> line is extremely short
		//System.out.println("i="+i+" j="+j);
		if (i>=-1e-6 && i<=1+1e-6 &&
			j>=-1e-6)
		{
			Vector res=new Vector(
				a+b*i,
				c+d*i);
			return res;
		}
		return null;
	}
	
	public Line moved(Vector pos) {		
		return new Line(a.plus(pos),b.plus(pos));
	}
	public Vector getv1() {
		return a;
	}
	public Vector getv2() {
		return b;
	}
}
