package se.flightplanner.map3d;

import java.util.ArrayList;

public interface Stitcher {

	
	/**
	 * The given vertex has been created, and needs to be stitched into
	 * any edge passing through it. The given vertex belongs to a Thing
	 * of level 'level'. 
	 * If dostitch is false, unstitch vertex rather than stitch it.
	 * A vertex is only ever unstitched if it is destroyed.
	 * @param parent 
	 */
	public void stitch(Vertex v,int level,ThingIf parent, boolean dostitch);
	
}
