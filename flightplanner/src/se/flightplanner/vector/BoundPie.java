package se.flightplanner.vector;

public class BoundPie {

	public Pie pie;
	public Vector pos;
	
	public BoundPie(Vector pos,Pie pie)
	{
		this.pie=pie;
		this.pos=pos;	
	}
	
	
	public Line cut(Line tline)
	{
		Line line=tline.moved(pos.negated());
		
		boolean isinA=pie.isInPie(line.getv1());
		boolean isinB=pie.isInPie(line.getv2());
		if (isinA && isinB)
			return tline; //line is completely in pie
		
		Line lim0=pie.getLine(0);
		Line lim1=pie.getLine(1);
		Vector i0=line.intersect_inf2(line, lim0);
		Vector i1=line.intersect_inf2(line, lim1);
		
		Line cand;
		todo write unit tests for this and Simplified and etcs
		if (!isinA && !isinB)
		{
			if (i0==null || i1==null) return null;
			cand=new Line(i0,i1);
		}
		else 
		{
			if (i0!=null && i1!=null) return null;
			if (i0==null && i1==null) return null;
			Vector i;
			if (i0!=null) i=i0; else i=i1;
			if (!isinA)  //A is needed from outside pie
			{
				if (i0==null || i1!=null) return null;
				cand=new Line(i,line.getv2());
			}
			else
			{//B is needed from outside pie
				if (i1==null || i0!=null) return null;
				cand=new Line(line.getv1(),i);
			}
		}
		
		return cand;
		
		
		
		
	}
	
	
}
