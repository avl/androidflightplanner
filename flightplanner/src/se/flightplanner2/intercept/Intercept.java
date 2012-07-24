package se.flightplanner2.intercept;

import se.flightplanner2.intercept.SecantSolver.Function;

public class Intercept {

	
	static public class Candidate
	{
		public float x;
		public Result y;
	}
	float clampdelta(float delta)
	{
		delta=delta%360.0f;
		if (delta>180)
			delta-=360;
		if (delta<-180)
			delta+=360;
		return delta;	
	}
	Result fast(final StateVector initial)
	{
		float aimpoint=(float)Math.abs(initial.pos.x)+1;
		float step=6;
		float bestgood=1e30f;
		Candidate best=null;
		Result p30=trySolution(initial,clampdelta(30-initial.hdg),true);
		Result m30=trySolution(initial,clampdelta(-30-initial.hdg),true);
		if (p30.straight>0 && m30.straight>0)
		{
			if (Math.abs(m30.sv.pos.x)<0.1f)
				return m30;
			if (Math.abs(p30.sv.pos.x)<0.1f)
				return p30;			
		}
		Float lastxpos=null;
		for(float x=-330;x<=331;x+=5)
		{
			float targ=(initial.hdg+x)%360.0f;
			if (targ<0) targ+=360;
			if (targ>90 && targ<270) continue;
			
			Result y=trySolution(initial,x,false);
			float good=goodness(y.sv,targ);
			System.out.println("Value of "+x+" => Good == "+good+" x=="+y.sv.pos.x);
			if (good<bestgood && (Math.abs(y.sv.pos.x)<1e-4 || (lastxpos!=null) && ((float)lastxpos<0)!=(y.sv.pos.x<0)))
			{
				bestgood=good;
				best=new Candidate();
				best.x=x;
				best.y=y;
			}
			lastxpos=(float)y.sv.pos.x;
			//x+=step;			
		}
		
		
		SecantSolver sc=new SecantSolver(new Function()
		{
			@Override
			public float calc(float x) {
				Result y=trySolution(initial,x,false);
				System.out.println("Secant solver trying: "+x+" result: "+y.sv.pos.x);
				return (float)y.sv.pos.x;
			}
		});
		System.out.println("Secant solving between: "+(best.x-step)+" -- "+(best.x+step)+ " since best at "+best.x+" had 'Y' of : "+best.y.sv.pos.x);
		
		float solx=sc.solve(best.x-step,best.x+step);
		
		//Result cand1=trySolution(initial,0.04f);
		Result soly=trySolution(initial,solx,false);
		
		return soly;
	}
	public static class Result
	{
		@Override
		public String toString() {
			return "Result [sv=" + sv + ", firsthdg=" + firsthdg
					+ ", straight=" + straight + "]";
		}
		StateVector sv;
		float firsthdg;
		float straight;
	}

	private Result trySolution(StateVector initial, float hdgDelta,boolean allow_straight) {
		if (hdgDelta==0)
			System.out.println("");
		FlightPathCompositeTurnSegment t1=new FlightPathCompositeTurnSegment(hdgDelta,initial.speed,initial.roll);						
		StateVector cur=new StateVector(initial);
		StateVector cand=t1.execute(cur);
		float hdgdelta2=0-cand.hdg;
		if (hdgdelta2<-180) 
			hdgdelta2+=360;
		if (hdgdelta2>180) 
			hdgdelta2-=360;
		FlightPathCompositeTurnSegment t2=new FlightPathCompositeTurnSegment(hdgdelta2,initial.speed,0);
		StateVector cand2=t2.execute(cand);
		float straight;
		if (allow_straight)
		{
			float off=(float)cand2.pos.x;
			float xdelta=Math.abs((float)(cur.speed*Math.sin(cand.hdg/(180/Math.PI))));
			if (xdelta<0.001)
				straight=0;
			else
				straight=3600f*Math.abs(off)/xdelta;
		}
		else
		{
			straight=0;
		}
			
		
		FlightPathSegment[] segs=
			new FlightPathSegment[]
				{
					t1,
					new FlightPathStraightSegment(straight),
					t2
				};
		cur=new StateVector(initial);
		
		for(FlightPathSegment seg:segs)
		{
			cur=seg.execute(cur);
		}
		Result r=new Result();
		r.firsthdg=hdgDelta;
		r.straight=straight;
		r.sv=cur;
		return r;
	}

	private float goodness(StateVector cur,float first_hdg_targ) {
		float hdelta=clampdelta(cur.hdg);
		first_hdg_targ=clampdelta(first_hdg_targ);
		float nominal_hdg_targ=30;
		if (first_hdg_targ<0)
			nominal_hdg_targ=-30;
			
		
		return (float)(Math.pow(Math.abs(cur.pos.x*100),2)+Math.pow(Math.abs(hdelta),2)+
				(float)(Math.pow(first_hdg_targ*(nominal_hdg_targ-first_hdg_targ),2)));
	}
	
}
