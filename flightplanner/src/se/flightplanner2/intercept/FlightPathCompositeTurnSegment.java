package se.flightplanner2.intercept;

import se.flightplanner2.intercept.TurnPlanner.Res;

public class FlightPathCompositeTurnSegment implements FlightPathSegment {

	
	int turn_direction;
	float muchness;
	FlightPathRollSegment roll_in;
	FlightPathTurnSegment turn;
	FlightPathRollSegment roll_out;
	
	
	private boolean donothing;
	public FlightPathCompositeTurnSegment(float hdgdelta,float speed)
	{
		if (Math.abs(hdgdelta)<0.5)
		{
			donothing=true;
		}
		else
		{
			TurnPlanner tp=new TurnPlanner();
			Res res=tp.calc(speed, hdgdelta);		
			roll_in=new FlightPathRollSegment(res.rolldeg);
			turn=new FlightPathTurnSegment(res.keep_turn);
			roll_out=new FlightPathRollSegment(0);
		}
		
	}
	
	@Override
	public StateVector execute(StateVector prev)
	{
		if (donothing)
			return new StateVector(prev);
		return roll_out.execute(turn.execute(roll_in.execute(prev)));
	}
	
}
