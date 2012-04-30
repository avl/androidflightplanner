package se.flightplanner.simpler;

import se.flightplanner.simpler.Common.Compartment;
import se.flightplanner.vector.Pie;

public class Common {

	public static class Rect
	{
		public int left;
		public int top;
		public int right;
		public int bottom;
		public String toString()
		{
			return "Rect("+left+","+top+","+right+","+bottom+")";
		}
		public Rect(int x1,int y1,int x2,int y2)
		{
			this.left=x1;
			this.top=y1;
			this.right=x2;
			this.bottom=y2;
			if (x1>x2) throw new RuntimeException("internal error x1>x2");
			if (y1>y2) throw new RuntimeException("internal error x1>x2");
		}
		public int width(){return right-left;}
		public int height(){return bottom-top;}
		public void offset(int dx, int dy) {
			left+=dx;
			right+=dx;
			top+=dy;
			bottom+=dy;
		}		
		
	}
	public static enum Compartment {
		LEFT,AHEAD,RIGHT,PRESENT
	}

	
	public static Pie getPie(Compartment comp) {
		if (comp==Compartment.AHEAD) return new Pie(-30,+30);
		if (comp==Compartment.LEFT) return new Pie(-100,-29);
		if (comp==Compartment.RIGHT) return new Pie(+29,+100);
		if (comp==Compartment.PRESENT) return new Pie(0,360);
		throw new RuntimeException("Unknown compartment: "+comp);
	}

}
