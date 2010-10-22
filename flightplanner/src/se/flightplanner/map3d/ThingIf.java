package se.flightplanner.map3d;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;

import se.flightplanner.Project.iMerc;

public interface ThingIf {

	public abstract int getZoomlevel();

	public abstract Vertex getCorner(int i);

	public abstract iMerc getPos();
	public abstract int getBoxSize();
	public abstract boolean isReleased();

	/*
	private int getZoomLevel() {
		return zoomlevel;
	}*/
	/*private ArrayList<Vertex> getBaseVertices() {
		return base_vertices;
	}*/

	public abstract boolean isSubsumed();

	public abstract boolean isCorner(Vertex v);

	/**
	 * Return -1 if vertex isn't along any edge for Thing
	 */
	public abstract int getSide(Vertex v);

	public abstract float bumpiness();

	public abstract float getDistance(iMerc obs, int observerHeight);

	/**
	 * Create four sub-things from this thing.
	 * The subthings are not yet stitched to the parent (this) 
	 * neighbors. The subthings are also lacking elevation (will
	 * be provided to all new subthings of an iteration, after they have
	 * all been created).
	 */
	public abstract void subsume(ArrayList<ThingIf> newThings,
			VertexStore vstore, Stitcher stitcher, ElevationStore estore);

	public abstract void unsubsume(VertexStore vstore, Stitcher st,ArrayList<ThingIf> removed_things,TriangleStore tristore);

	/**
	 * Called when the life of a Thing ends. 
	 * @param neededStitching The vertices of the Thing are added to this set, 
	 * iff they are still used by some other thing. Used by higher up code
	 * to determine if the unsubsumed thing above needs stitching. This is 
	 * a slightly clever hack, and you might need to think about it to see
	 * why it works. (Clue: Why were the vertices not unused, and what are
	 * the criteria for needing stitching?) 
	 */
	public abstract void release(HashSet<Vertex> neededStitching,
			VertexStore vstore, Stitcher st,ArrayList<ThingIf> removed_things,TriangleStore tristore);

	public abstract void triangulate(TriangleStore tristore,VertexStore vstore);
	public abstract ThingIf getChild(int i,int j);
	/**
	 * The following events may happen for a vertex:
	 * 
	 * - First corner usage
	 *   * Simple - VertexStore detects
	 * - Subsequent corner usage
	 *   * Also handled by vertex store
	 * - Subsequent corner disuse
	 *   * VertexStore decreases refcount
	 * - Final disuse (destroy vertex)
	 *   * VertexStore decreases refcount to 0.
	 *     - Now must hunt all shared references
	 *       - Find adjacent edges on all zoomlevels above.
	 *       - There can be only one adjacent edge needing stitching, per vertex (one out of two possible for each vertex).
	 *       - In practice at most two corner-vertices will need stitching? (don't use this fact)
	 *  
	 * - oncreate sharing to adjacent edge
	 *   * Must hunt all shared edges
	 *     - simply add vertex to every shared edge
	 *   
	 * - stop sharing because of subsumed adjacent edge
	 *   * whenever subsuming thing:
	 *     - Remove all stitch-vertices for Thing. (easy) 
	 * 
	 * - start sharing because of unsubsumed thing
	 *   * Most difficult. Could be a huge number of adjacent edges,
	 *     in principle. Algorithm:
	 *     - Each vertex that is to start being shared *must* be either:
	 *       a) Already shared to one of the children of the unsubsuming thing
	 *       b) One of the corners of the unsubsuming thing's children.
	 *     Simply try to share all surviving vertices of all the children. Those
	 *     who aren't on the edge won't be added as shared.
	 *  
	 * - ondestroy stop sharing
	 *   * Destroyed exactly when last thing which has vertex as corner is destroyed.
	 *     - *MUST* find all sharings.
	 *       * Hunt all shared edges
	 *         - Purge vertex from each such edge.
	 * 
	 * 
	 */
	public abstract void shareVertex(VertexStore vstore, Vertex v, boolean share);

	public abstract ThingIf getParent();

	public abstract void debugDump(Writer f) throws IOException;

	public abstract String getPosStr();

	public abstract void calcElevs1(TriangleStore tristore, VertexStore vstore);
	public abstract void calcElevs2(TriangleStore tristore, VertexStore vstore);

	public abstract void adjustRefine(float refine);

	public abstract ArrayList<ThingIf> getAllChildren();

}