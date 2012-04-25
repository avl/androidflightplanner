package se.flightplanner.vector;

public class BoundPie {

	public Pie pie;
	public Vector pos;
	
	public BoundPie(Vector pos,Pie pie)
	{
		this.pie=pie;
		this.pos=pos;	
	}
	
	@Override
	public String toString()
	{
		return "BoundPie("+pos+","+pie+")";
	}
	
	
	
	/*!
	 * A peculiarity of this function:
	 * If the given line ends in the sector, but traverses
	 * outside it for any distance, the return value will still
	 * be the unaltered string. That is, this function never
	 * splits lines into multiple pieces, even when the non-convexness
	 * of the sector would make that be expected.
	 */
	public Line cut(Line tline)
	{
		Line line=tline.moved(pos.negated());
		
		boolean isinA=pie.isInPie(line.getv1(),0);
		boolean isinB=pie.isInPie(line.getv2(),0);
		if (isinA && isinB)
			return tline; //line is completely in pie
		
		Line lim0=pie.getLine(0);
		Line lim1=pie.getLine(1);
		//System.out.println("Intersecting "+line+" and lim1: "+lim0);
		Vector i0=Line.intersect_inf2(line, lim0);
		//System.out.println("Intersecting "+line+" and lim1: "+lim1);
		Vector i1=Line.intersect_inf2(line, lim1);
		//System.out.println("Intersections: "+i0+","+i1+" isinA: "+isinA+" isinB: "+isinB);
		Line cand;
		if (!isinA && !isinB)
		{
			if (i0==null || i1==null) return null;
			cand=new Line(i0,i1);
		}
		else 
		{
			if (i0!=null && i1!=null) 
			{
				//only one of the line ends is in the sector, yet
				//there are two intersections. This is geometrically
				//impossible, but will occur due to numerical problems.
				//when it occurs, one of the points, either i0 or i1 is a 
				//false point, which is very nearly identical to one of the line
				//ends (which is thus 'on the boundary' of the sector).
				//We disregard this point.
				double i0dist=Math.min(i0.minus(line.getv1()).length(),i0.minus(line.getv2()).length());
				double i1dist=Math.min(i1.minus(line.getv1()).length(),i1.minus(line.getv2()).length());
				if (i0dist<i1dist) //disregard that of i0/i1 which is closest to a line end.
					i0=null; 
				else
					i1=null;				
			}
			if (i0==null && i1==null) return null;
			Vector i;
			if (i0!=null) i=i0; else i=i1;
			if (!isinA)  //A is needed from outside pie
			{
				cand=new Line(i,line.getv2());
			}
			else
			{//B is needed from outside pie
				cand=new Line(line.getv1(),i);
			}
		}
		
		return cand.moved(pos);
		
		
		
		
	}
	
	
}
