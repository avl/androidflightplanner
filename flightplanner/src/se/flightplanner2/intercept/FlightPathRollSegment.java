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
		float step=0.01f;
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
				t=need_t;
				finished=true;
			}
			cur.hdg+=Helpers.get_turn_rate_deg(cur.speed,cur.roll)*t;
			cur.integrate_postime(t);			
			if (finished)
				break;
		}
		
		{
			StateVector ocur=new StateVector(prev);
			float rolldelta=endbank-prev.roll;
			float currollrate=rollrate;
			float t=Math.abs(rolldelta)/rollrate;
			if (rolldelta<0) currollrate=-rollrate;
			float S=prev.speed/3600.0f; //NM per sec
			float A=(float)(currollrate*Math.PI/180.0);
			float N=(float)(prev.roll*Math.PI/180.0);
			float K=(float) (1093.694384449244/(prev.speed)*(Math.PI/180.0f)); //([turn rad]/sec)/[roll rad]
			
			//float px0=(float) ();
			
			double blaha=Math.cos(K*(N + A*t))  - Math.cos(K*(N));
			double blaha2=2*Math.sin((1/2.0)*K*(A*t+N*2))*Math.sin(-(1/2.0)*K*A*t);
			System.out.println("Blaha 1: "+blaha+" Blaha 2:"+blaha2);
			float px=(float) (-S* Math.signum(N+A*t)*(blaha)/(A*K));
			
			float py0=(float)(-S*Math.sin(K*(N))/(A*K));
			float py= (float)(-S*Math.sin(K*(N + A*t))/(A*K) - py0);
			
			System.out.println("Start bank: "+prev.roll+" Endbank: "+endbank);			
			System.out.println("px: "+px+" py: "+py);
			//System.out.println("px0: "+px0+" py0: "+py0);
			Vector Y=Vector.fromhdg(ocur.hdg+180);
			Vector X=Vector.fromhdg(ocur.hdg+90);
			
			float prev_to_0=TurnPlanner.getTurn((float)TurnPlanner.bank2time(Math.abs(prev.roll), rollrate), rollrate, prev.speed);
			float targ_to_0=TurnPlanner.getTurn((float)TurnPlanner.bank2time(Math.abs(endbank), rollrate), rollrate, prev.speed);
			float tot_hdg_delta;
			if (Math.abs(endbank)>Math.abs(prev.roll))
			{ //end bank dominates
				tot_hdg_delta=(targ_to_0-prev_to_0);
				if (endbank<0)
					tot_hdg_delta=-tot_hdg_delta;				
			}
			else
			{
				tot_hdg_delta=(prev_to_0-targ_to_0);
				if (prev.roll<0)
					tot_hdg_delta=-tot_hdg_delta;								
			}
			
			ocur.hdg+=tot_hdg_delta;
			ocur.time+=t;
			ocur.roll=endbank;
			ocur.pos=X.mul(px).plus(Y.mul(py));
			System.out.println("Prev-0: "+prev_to_0);
			System.out.println("Targ-0: "+targ_to_0);
			System.out.println("Hdg delta: "+tot_hdg_delta);
			System.out.println("Take #1:\n"+cur+"\nTake #2:\n"+ocur+"\n---------------------\n\n");
		}
		
		
		return cur;		
	}
	
}
