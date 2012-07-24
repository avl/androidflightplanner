package se.flightplanner2.intercept;

import se.flightplanner2.intercept.TurnPlanner.Res;

public class FlightPathCompositeTurnSegment implements FlightPathSegment {

	
	int turn_direction;
	float muchness;
	FlightPathRollSegment roll_in;
	FlightPathTurnSegment turn;
	FlightPathRollSegment roll_out;
	
	
	private boolean donothing;
	public FlightPathCompositeTurnSegment(float hdgdelta,float speed,float initial_roll)
	{
		{
			TurnPlanner tp=new TurnPlanner();
			Res res=tp.getStrategy1(hdgdelta, initial_roll, speed);		
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
		StateVector sv1=prev;
		
		//Todo: Analytical FlightPathRoll
		//Also: If input bank is bigger than nominal, begin by just aiming for nominal bank, same direction.
		//System.out.println("Before: "+sv1.hdg);
		StateVector sv2=roll_in.execute(prev);
		//System.out.println("Step 1: "+sv2.hdg+ " delta: "+(sv2.hdg-sv1.hdg));
		StateVector sv3=turn.execute(sv2);
		//System.out.println("Step 2: "+sv3.hdg+ " delta: "+(sv3.hdg-sv2.hdg));
		StateVector sv4=roll_out.execute(sv3);
		//System.out.println("Step 3: "+sv4.hdg+ " delta: "+(sv4.hdg-sv3.hdg));
		return sv4;
	}
	
}
