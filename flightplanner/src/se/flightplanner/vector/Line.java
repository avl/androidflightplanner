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
}
