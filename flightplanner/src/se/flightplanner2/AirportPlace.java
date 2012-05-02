package se.flightplanner2;

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
}
