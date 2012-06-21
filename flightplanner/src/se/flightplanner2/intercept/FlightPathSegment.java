package se.flightplanner2.intercept;

public interface FlightPathSegment {
	public StateVector execute(StateVector state);
}
