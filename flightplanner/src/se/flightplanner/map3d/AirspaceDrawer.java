package se.flightplanner.map3d;

import java.util.ArrayList;

import se.flightplanner.AirspaceArea;
import se.flightplanner.vector.SimpleTriangle;

public class AirspaceDrawer {

	static class DrawnAirspace
	{
		AirspaceArea source;
		float floor;
		float ceiling;
		ArrayList<SimpleTriangle> triangles;
	}
}
