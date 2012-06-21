package se.flightplanner2.intercept;

public class FlightPathStraightSegment implements FlightPathSegment  {
	float dt;
	public FlightPathStraightSegment(float dt)
	{		
		this.dt=dt;
	}
	@Override
	public StateVector execute(StateVector state)
	{
		if (state.roll!=0)
			throw new RuntimeException("Straight segment can't be used in roll");
		StateVector res=new StateVector(state);
		res.integrate_postime(dt);
		return res;
	}
}
