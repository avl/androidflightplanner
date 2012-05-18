package se.flightplanner2;

import se.flightplanner2.Project.LatLon;

public interface Place {

	SigPoint getAerodrome();
	String getHumanName();
	LatLon getLatLon();
	DetailedPlace getDetailedPlace();
	

}
