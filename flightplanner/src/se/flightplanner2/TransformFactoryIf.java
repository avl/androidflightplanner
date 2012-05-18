package se.flightplanner2;

import se.flightplanner2.Project.Merc;
import se.flightplanner2.vector.Vector;

public interface TransformFactoryIf {

	/*
	 * 
	 * mypos = position of interest in Merc13 coordinates
	 * arrow = position of interest in screen-coordinates
	 * hdg = rotation around position of interest
	 */
	TransformIf create(Merc mypos,Vector arrow,float hdg);
}

