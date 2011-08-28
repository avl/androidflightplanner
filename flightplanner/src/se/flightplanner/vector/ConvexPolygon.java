package se.flightplanner.vector;

public class ConvexPolygon {

	private Vector[] vs;
	public ConvexPolygon(Vector[] vs)
	{
		this.vs=vs;
	}
	public boolean inside(Vector x)
	{
		for(int i=0;i<4;++i)
		{
			int next_i=(i+1)%4;
			Vector a=vs[i];
			Vector b=vs[next_i];
			
			Vector inward=b.minus(a).rot90l();
			Vector cur=x.minus(a);
			if (inward.scalarprod(cur)<0)
				return false;							
		}
		return true;
	}
	
}
