package se.flightplanner.map3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import se.flightplanner.Project.Merc;
import se.flightplanner.Project.iMerc;
import se.flightplanner.map3d.ElevationStore.Elev;

public class Thing {

	private static class Counter
	{
		public int cnt;
		public Counter(int start)
		{
			cnt=start;
		}
	}
	private int zoomlevel;
	private int size; //of box's side, in merc units
	
	private Elev elev;
	
	private iMerc pos; //upper left corner
	private Thing parent;
	private ArrayList<Vertex> base_vertices; //lower-left,lower-right,upper-right, upper left
	//private Vertex center; //Always present, used to judge bumpiness
	private ArrayList<HashMap<Vertex,Counter>> edges; //edge numbering: lower, right, upper, left
	private ArrayList<Triangle> triangles;
	//private boolean subsumed; //if the current thing has been replaced by smaller things
	private boolean need_retriangulation; //set whenever edge vertices are added.
	private boolean known_ready;//ready when all vertices have received their elevation
	private boolean deployed; //set to true when a thing becomes visible. Implies that parent->subsumed is true.
	private ArrayList<Thing> children; //order: upper row first; left-right, then second row; left-right.

	
	public boolean isSubsumed() {
		return children!=null;
	}
	/**
	 * Create four sub-things from this thing.
	 * The subthings are not yet stitched to the parent (this) 
	 * neighbors. The subthings are also lacking elevation (will
	 * be provided to all new subthings of an iteration, after they have
	 * all been created).
	 */
	public void subsume(ArrayList<Thing> newThings,VertexStore vstore,Stitcher stitcher,ElevationStore estore)
	{
		if (children!=null) return; //Already subsumed
		children=new ArrayList<Thing>();
		for(int cury=pos.y;cury<pos.y+2*size;cury+=size)
		{
			for(int curx=pos.x;curx<pos.x+2*size;curx+=size)
			{
				Thing child=new Thing(new iMerc(curx,cury),this,zoomlevel+1,vstore,estore,stitcher);
				children.add(child);
				newThings.add(child);
			}
		}
	}
	private int getZoomLevel() {
		return zoomlevel;
	}
	private ArrayList<Vertex> getBaseVertices() {
		return base_vertices;
	}
	public void unsubsume()
	{
		if (children==null)
			return; //already unsubsumed
		//Start showing again.
		//Note that if the mid-edge-vertices aren't usage=1, that
		//means that someone else is still using them, and the unsubsumed parent Thing
		//must stitch them.
		HashSet<Vertex> needStitching=new HashSet<Vertex>();
		for(Thing child : children)
		{
			child.release(needStitching);
		}
		for(Vertex v : needStitching)
		{
			shareVertex(v,false);
		}
		children=null;
		
	}
	
	/**
	 * Called when the life of a Thing ends. 
	 * @param needStitching The vertices of the Thing are added to this set. Used by higher up code to determine if the unsubsumed thing above needs stitching.
	 */
	private void release(HashSet<Vertex> needStitching) {
		for(Vertex v:base_vertices)
		{
			needStitching.add(v);
		}
		if (children!=null)
		{
			for(Thing child:children)
			{
				child.release(needStitching);				
			}
		}
	}
	iMerc getPos()
	{
		return pos;
	}
	public Thing(iMerc pos,Thing parent,int zoomlevel,VertexStore vstore,ElevationStore elevStore, Stitcher stitcher)
	{
		int zoomgap=13-zoomlevel;
		this.size=256<<zoomgap;
		this.pos=pos;
		this.parent=parent;
		this.zoomlevel=zoomlevel;
		this.elev=elevStore.get(pos,zoomlevel);
		if (this.elev==null)
			this.elev=new Elev((short)0,(short)0);
		base_vertices=new ArrayList<Vertex>();
		for(int i=0;i<4;++i)
		{
			iMerc p=new iMerc(pos);
			switch(i)
			{
			case 0: p.y+=size; break;
			case 1: p.y+=size; p.x+=size; break;
			case 2: p.x+=size; break;
			case 3: break;
			}
			Vertex v=vstore.obtain(p,(byte)zoomlevel);
			base_vertices.add(v);
			stitcher.stitch(v,zoomlevel,true);
		}
	}
	
	private void releaseTriangles(TriangleStore tristore)
	{
		for(Triangle t:triangles)
			tristore.release(t);
		triangles.clear();
	}
	public void triangulate(TriangleStore tristore)
	{
		if (!need_retriangulation) return;
		int vcnt=0;
		for(int i=0;i<4;++i)
			vcnt+=edges.get(i).size();
		releaseTriangles(tristore);
		if (vcnt==0)
		{
			Triangle t1=tristore.alloc();
			t1.assign(base_vertices.get(0),base_vertices.get(0),base_vertices.get(0));
		}
	}
	
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
	public void shareVertex(Vertex v, boolean unshare)
	{
		if (isCorner(v))
			return; //vertex is one of the base(=corner) vertices, 			
		int side = getSide(v);
		if (side==-1)
			return;
		HashMap<Vertex,Counter> hm=edges.get(side);
		Counter c=hm.get(v);
		if (c==null)
		{
			if (unshare)
			{
				//trying to unshare a vertex we don't even own.
				return;
			}
			need_retriangulation=true;
			c=new Counter(1);
			hm.put(v, c);
		}
		else
		{
			if (unshare)
			{
				c.cnt-=1;
				if (c.cnt<=0)
				{
					hm.remove(v);
				}
			}
			else
			{
				c.cnt+=1;
			}
		}
	}
	public void delVertex(Vertex v)
	{
		if (isCorner(v))
			return; //none of the children of a Thing shares its corner, and the parent is _always_ more longer lived, so a delVertex is never for a corner.  			
		int side=getSide(v);
		if (side==-1) //not possibly part of Thing, since it isn't on things edge.
			return;
		assert side>=0 && side<4;
		HashMap<Vertex,Counter> hm=edges.get(side);
		Counter c=hm.get(v);
		if (c.cnt<=1)
		{
			assert c.cnt==1;
			hm.remove(v);
		}
		else
		{
			c.cnt-=1;
		}
		need_retriangulation=true;
	}
	private boolean isCorner(Vertex v) {
		boolean isbase=false;
		for(Vertex base_ : base_vertices)
		{
			if (v.equals(base_))
			{
				isbase=true;
				break;
			}
		}
		return isbase;
	}

	/**
	 * Return -1 if vertex isn't along any edge for Thing
	 */
	private int getSide(Vertex v) {
		int side=-1;
		assert(v.getx()>=pos.x && v.getx()<pos.x+size && v.gety()>=pos.y && v.gety()<pos.y+size);
		if (v.getx()==pos.x) side=3;
		if (v.getx()==pos.x+size) side=1;
		if (v.gety()==pos.y) side=2;
		if (v.gety()==pos.y+size) side=0;
		assert (side>=0 && side<4) || (side==-1);
		return side;
	}

	public float bumpiness() {
		
		return elev.hiElev-elev.loElev;
	}
	public float getDistance(iMerc observer,int observerHeight) {
		float a=(observer.x-pos.x);
		float b=(observer.y-pos.y);
		float c=observerHeight-elev.hiElev;
		float dist=(float)Math.sqrt((a*a+b*b+c*c));
		return dist;
	}

	
}
