package se.flightplanner2;

import se.flightplanner2.Project.LatLon;

public class AirportPlace implements Place {
	private SigPoint sp;
	public AirportPlace(String icao,AirspaceLookup lookup)
	{
		sp=lookup.getByIcao(icao);
	}
	@Override
	public SigPoint getAerodrome() {
		return sp;
	}
	@Override
	public String getHumanName() {
		return sp.name;
	}
	@Override
	public DetailedPlace getDetailedPlace() {
		return null;
	}
	@Override
	public LatLon getLatLon() {
		return sp.latlon;
	}
}
