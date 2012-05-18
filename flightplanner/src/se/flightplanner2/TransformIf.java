package se.flightplanner2;

import se.flightplanner2.Project.Merc;
import se.flightplanner2.vector.Vector;

interface TransformIf {

	/// Convert from merc to screen coordinates with 
	/// north up on map
	public abstract Merc getPos();
	
	public abstract float getHdg();

	public abstract float getHdgRad();

	public abstract Vector merc2northscreen(Merc m);

	/// Convert from screen coordinates with 
	/// north up on map to merc.
	public abstract Merc northscreen2merc(Vector n);

	public abstract Merc screen2merc(Vector s);

	public abstract Vector merc2screen(Merc m);

}