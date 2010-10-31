package se.flightplanner.map3d;

import se.flightplanner.Project.iMerc;
import se.flightplanner.map3d.ElevationStore.Elev;

public interface ElevationStoreIf {

	public abstract Elev get(iMerc pos, int hlevel);

}