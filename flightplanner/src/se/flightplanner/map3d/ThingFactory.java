package se.flightplanner.map3d;

import se.flightplanner.Project.iMerc;
import se.flightplanner.map3d.Stitcher;

public interface ThingFactory {

	public ThingIf createThing(VertexStore vstore, ElevationStore estore, int zoomlevel,
			iMerc m,Stitcher st);
}
