package se.flightplanner2.intercept;

public class Intercept {

	
	
	void verySlow(StateVector initial)
	{
		float aimpoint=(float)Math.abs(initial.pos.x)+1;
		float bestgood=1e30f;
		StateVector best=null;
		float bestTurnOne=0;
		float bestTurnTwo=0;
		float bestStraight=0;
		float bestIntermedHdg=0;
		float bestTime=0;
		int bestSign=0;
		FlightPathSegment[] bestsegs=null;
		for(int turndir=-1;turndir<2;turndir+=2)
		{
			for(float hdgDelta=-270;hdgDelta<270;hdgDelta+=1)
			{
				//float thdg=(initial.hdg+hdgDelta)%360;
				//if (thdg>180) thdg-=360;
				//if (thdg<-45 || thdg>45) continue;
				//System.out.println("hdgdelta:"+hdgDelta);
				
				//float hdgDelta=-135;
				//System.out.println("Complete: "+hdgDelta);
				{
					//for(float straight=0;straight<=600;straight+=10)
					{
						
						FlightPathCompositeTurnSegment t1=new FlightPathCompositeTurnSegment(hdgDelta,initial.speed);
						
						StateVector cur=new StateVector(initial);
						StateVector cand=t1.execute(cur);
						float hdgdelta2=0-cand.hdg;
						if (hdgdelta2<-180) 
							hdgdelta2+=360;
						if (hdgdelta2>180) 
							hdgdelta2-=360;
						FlightPathCompositeTurnSegment t2=new FlightPathCompositeTurnSegment(hdgdelta2,initial.speed);
						StateVector cand2=t2.execute(cand);
						float off=(float)cand2.pos.x;
						float xdelta=Math.abs((float)(cur.speed*Math.sin(cand.hdg/(180/Math.PI))));
						float straight;
						if (xdelta<0.001)
							straight=0;
						else
							straight=3600f*Math.abs(off)/xdelta;										
						
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
						float curgood=goodness(cur,-aimpoint);
						if (curgood<bestgood)
						{
							bestgood=curgood;
							bestsegs=segs;
							best=cur;
							bestTurnOne=hdgDelta;
							bestTurnTwo=0;
							bestSign=turndir;
							bestStraight=straight;
							bestIntermedHdg=cand.hdg;
							bestTime=cur.time;
						}
						
					}
				}
			}
		}
		StateVector cur2=new StateVector(initial);
		System.out.println("Initial:"+initial);
		for(FlightPathSegment seg:bestsegs)
		{
			cur2=seg.execute(cur2);
			System.out.println("Step:"+cur2);
		}
		System.out.println("Best result: "+best);
		System.out.println("Best param: One: "+bestTurnOne+" Straight: " +bestStraight +"Two: "+bestTurnTwo+" sign: "+bestSign+" In-between hdg: "+bestIntermedHdg+" time:"+bestTime);
		
		
		
	}

	private float goodness(StateVector cur,float aimpoint) {
		float hdelta=cur.hdg;
		if (hdelta>180) hdelta-=360;
		if (hdelta<-180) hdelta+=360;
		return (float)(Math.pow(Math.abs(cur.pos.x*10),2)+Math.pow(Math.abs(hdelta),2)+
				(float)(Math.pow(cur.pos.y-aimpoint,2)));
	}
	
}
