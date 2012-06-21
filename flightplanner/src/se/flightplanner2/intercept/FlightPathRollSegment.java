package se.flightplanner2.intercept;

import se.flightplanner2.vector.Vector;
import android.location.Location;

public class FlightPathRollSegment {
	float endbank;
	static final float rollrate=5; //deg / second
	public FlightPathRollSegment(float endbank)
	{
		this.endbank=endbank;
	}
	StateVector execute(StateVector prev)
	{		
		int hang_prot=0;
		float start_t=prev.time;
		float step=0.1f;
		StateVector cur=new StateVector(prev);
		while(hang_prot<10000)
		{			
			++hang_prot;
			float rolldelta=endbank-cur.roll;
			float currollrate=rollrate;
			if (rolldelta<0) currollrate=-rollrate;
			float need_t=rolldelta/currollrate;
			float t=need_t;
			if (t>step) t=step;
			boolean finished=false;
			cur.roll+=currollrate*t;
			if (t>=need_t-1e-2)
			{
				cur.roll=endbank;
				finished=true;
			}
			cur.hdg+=Helpers.get_turn_rate_deg(cur.speed,cur.roll)*t;
			cur.integrate_postime(t);			
			if (finished)
				return cur;
		}
		return cur;		
	}
	
}
