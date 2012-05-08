package se.flightplanner2;

import java.util.ArrayList;

public interface AirspaceLookupIf {

	public abstract AirspaceAreaTree getAreas();

	public abstract ArrayList<AirspaceArea> getAllAirspace();

}