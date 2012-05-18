package se.flightplanner2;

import se.flightplanner2.Project.Merc;
import se.flightplanner2.vector.Vector;

public class MercTransformFactory implements TransformFactoryIf {

	@Override
	public TransformIf create(Merc mypos, Vector arrow, float hdg) {
		return new Transform(mypos,arrow,hdg);
	}

}
