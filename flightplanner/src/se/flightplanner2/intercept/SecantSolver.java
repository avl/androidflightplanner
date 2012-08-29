package se.flightplanner2.intercept;

public class SecantSolver {

	static public interface Function
	{
		float calc(float x);
	}
	Function func;
	public SecantSolver(Function func)
	{
		this.func=func;
	}
	float solve(float ix0,float ix1)
	{
		float x0=ix0;
		float x1=ix1;
		int i=0;
		float x=0.5f*(x0+x1);
		for(;;)
		{
			++i;
			if (Math.abs(x0-x1)>1e-6)
			{
				float y0=func.calc(x0);
				float y1=func.calc(x1);
				if (!(Math.abs(y1-y0)>1e-6))
					return x;
				x=x1-y1*((x1-x0)/(y1-y0));
				if (x<ix0) x=ix0;
				if (x>ix1) x=ix1;
				x0=x1;
				x1=x;
				if (i>100) 
					return x;
			}
			else
			{
				return x;
			}			
		}
				
	}
	
}
