package se.flightplanner2.vector;

public class SimpleTriangle {

	public SimpleTriangle() {
		a=b=c=null;
	}
	public SimpleTriangle(Vector pa, Vector pb, Vector pc) {
		a=pa;
		b=pb;
		c=pc;
	}
	public String toString()
	{
		return "SimpleTriangle("+a+","+b+","+c+")";
	}
	public Vector a;
	public Vector b;
	public Vector c;
	public Vector get(int j) {
		switch(j)
		{
		case 0: return a;
		case 1: return b;
		case 2: return c;
		}
		return null;
	}
}
