package se.flightplanner2.intercept;

import se.flightplanner2.Project;
import se.flightplanner2.vector.Vector;

public class FlightPathTurnSegment {
	float dt;
	public FlightPathTurnSegment(float dt)
	{
		this.dt=dt;
	}
	public StateVector execute(StateVector state)
	{
		if (Math.abs(state.roll)<1e-10)
		{
			StateVector res=new StateVector(state);
			res.roll=0;
			res.integrate_postime(dt);
			return res;			
		}
		float rate=Helpers.get_turn_rate_deg(state.speed, state.roll);
		float radius = Helpers.getTurnRadius(state,rate);
		//float circum=radius*2*(float)Math.PI;
		float turnang=rate*dt;
		float new_hdg=(float)((360.0+state.hdg+turnang)%360);
		
		Vector Y=Project.heading2vector(state.hdg); //forward
		Vector X=Y.rot90l(); //to the right (yes, the rotX-functions have left-handed coordinate systems (like the mercator grid))
		
		Vector turncenter=(rate>0) ? state.pos.plus(X.mul(radius)) : state.pos.minus(X.mul(radius));
		double abs_angle=(rate>0) ? Math.PI-Math.abs(turnang/(180.0/Math.PI)) : 0+Math.abs(turnang/(180.0/Math.PI));
		
		
		Vector newpos=turncenter.plus(
				X.mul(radius*Math.cos(abs_angle)).plus(
				Y.mul(radius*Math.sin(abs_angle))
						));
		
		StateVector res=new StateVector(state);
		res.pos=newpos;
		res.hdg=new_hdg;
		return res;
	}

}
