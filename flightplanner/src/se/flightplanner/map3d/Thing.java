package se.flightplanner.map3d;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;

import junit.framework.Assert;

import se.flightplanner.Project;
import se.flightplanner.Project.Merc;
import se.flightplanner.Project.iMerc;
import se.flightplanner.map3d.ElevationStore.Elev;

public class Thing implements ThingIf {

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
	/* (non-Javadoc)
	 * @see se.flightplanner.map3d.ThingIf#getZoomlevel()
	 */
	public int getZoomlevel()
	{
		return zoomlevel;
	}
	private Elev elev;
	
	private iMerc pos; //upper left corner
	private ThingIf parent;
	Vertex center_vertex;
	private ArrayList<Vertex> base_vertices; //lower-left,lower-right,upper-right, upper left
	//private Vertex center; //Always present, used to judge bumpiness
	private ArrayList<HashMap<Vertex,Counter>> edges; //edge numbering: lower, right, upper, left
	private ArrayList<Triangle> triangles;
	//private boolean subsumed; //if the current thing has been replaced by smaller things
	private boolean need_retriangulation; //set whenever edge vertices are added.
	private boolean known_ready;//ready when all vertices have received their elevation
	private boolean deployed; //set to true when a thing becomes visible. Implies that parent->subsumed is true.
	private ArrayList<Thing> children; //order: upper row first; left-right, then second row; left-right.

	/* (non-Javadoc)
	 * @see se.flightplanner.map3d.ThingIf#getCorner(int)
	 */
	public Vertex getCorner(int i)
	{
		return base_vertices.get(i);
	}
	/* (non-Javadoc)
	 * @see se.flightplanner.map3d.ThingIf#getPos()
	 */
	public iMerc getPos()
	{
		return pos;
	}
	/*
	private int getZoomLevel() {
		return zoomlevel;
	}*/
	/*private ArrayList<Vertex> getBaseVertices() {
		return base_vertices;
	}*/
	
	/* (non-Javadoc)
	 * @see se.flightplanner.map3d.ThingIf#isSubsumed()
	 */
	public boolean isSubsumed() {
		return children!=null;
	}
	/* (non-Javadoc)
	 * @see se.flightplanner.map3d.ThingIf#isCorner(se.flightplanner.map3d.Vertex)
	 */
	public boolean isCorner(Vertex v) {
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
	/* (non-Javadoc)
	 * @see se.flightplanner.map3d.ThingIf#getSide(se.flightplanner.map3d.Vertex)
	 */
	public int getSide(Vertex v) {
		int side=-1;
		if (!(v.getx()>=pos.x && v.getx()<=pos.x+size && v.gety()>=pos.y && v.gety()<=pos.y+size))
			return -1;
		if (v.getx()==pos.x) side=3;
		if (v.getx()==pos.x+size) side=1;
		if (v.gety()==pos.y) side=2;
		if (v.gety()==pos.y+size) side=0;
		assert (side>=0 && side<4) || (side==-1);
		return side;
	}
	/* (non-Javadoc)
	 * @see se.flightplanner.map3d.ThingIf#bumpiness()
	 */
	public float bumpiness() {
		
		return elev.hiElev-elev.loElev;
	}
	/* (non-Javadoc)
	 * @see se.flightplanner.map3d.ThingIf#getDistance(se.flightplanner.Project.iMerc, int)
	 */
	public float getDistance(iMerc obs,int observerHeight) {
		iMerc pos1=pos;
		iMerc pos2=new iMerc(pos1.x+size,pos1.y+size);
		int x=0;
		int y=0;
		
		if(obs.x<pos1.x)
			x=pos1.x;
		else if (obs.x>=pos2.x) x=pos2.x;
		else x=obs.x;
		
		if(obs.y<pos1.y)
			y=pos1.y;
		else if (obs.y>=pos2.y) y=pos2.y;
		else y=obs.y;
		
		float a=(obs.x-x);
		float b=(obs.y-y);
		float c=feet2merc(obs,observerHeight-elev.hiElev);
		float dist=(float)Math.sqrt((a*a+b*b+c*c));
		return dist;
	}
	
	private float feet2merc(iMerc pos,int feet) {
		return Project.approx_ft_pixels(pos, 13)*feet;
	}
	/* (non-Javadoc)
	 * @see se.flightplanner.map3d.ThingIf#subsume(java.util.ArrayList, se.flightplanner.map3d.VertexStore, se.flightplanner.map3d.Stitcher, se.flightplanner.map3d.ElevationStore)
	 */
	public void subsume(ArrayList<ThingIf> newThings,VertexStore vstore,Stitcher stitcher,ElevationStore estore)
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
	/* (non-Javadoc)
	 * @see se.flightplanner.map3d.ThingIf#unsubsume(se.flightplanner.map3d.VertexStore, se.flightplanner.map3d.Stitcher)
	 */
	public void unsubsume(VertexStore vstore,Stitcher st)
	{
		if (children==null)
			return; //already unsubsumed
		//Start showing again.
		
		//Note that if the mid-edge-vertices aren't usage=1, that
		//means that someone else is still using them, and the unsubsumed parent Thing
		//must stitch them.
		
		
		HashSet<Vertex> neededStitching=new HashSet<Vertex>();
		for(ThingIf child : children)
		{
			child.release(neededStitching,vstore,st);
		}
		for(Vertex v : neededStitching)
		{
			shareVertex(vstore,v,true);
		}
		children=null;
		
	}
	
	/* (non-Javadoc)
	 * @see se.flightplanner.map3d.ThingIf#release(java.util.HashSet, se.flightplanner.map3d.VertexStore, se.flightplanner.map3d.Stitcher)
	 */
	public void release(HashSet<Vertex> neededStitching,VertexStore vstore,Stitcher st) {
		for(Vertex v:base_vertices)
		{
			boolean unused=vstore.decrement(v);
			//System.out.println("Freeing "+v);
			if (unused)
			{
				//System.out.println("ACtually freed "+v);
				st.stitch(v,this.zoomlevel,false);
			}
			else
			{
				neededStitching.add(v);
			}
		}
		if (center_vertex!=null)
		{
			vstore.decrement(center_vertex); //can never be stitched anywhere.
		}
		if (children!=null)
		{
			for(ThingIf child:children)
			{
				child.release(neededStitching,vstore,st);				
			}
		}
	}
	public Thing(iMerc pos,ThingIf parent,int zoomlevel,VertexStore vstore,ElevationStore elevStore, Stitcher stitcher)
	{
		int zoomgap=13-zoomlevel;
		this.size=64<<zoomgap;
		this.pos=pos;
		this.parent=parent;
		this.zoomlevel=zoomlevel;
		this.triangles=new ArrayList<Triangle>();
		this.elev=elevStore.get(pos,zoomlevel);
		if (this.elev==null)
			this.elev=new Elev((short)0,(short)0);
		base_vertices=new ArrayList<Vertex>();
		for(int i=0;i<4;++i)
		{
			iMerc p=new iMerc(pos);
			switch(i)
			{
			case 0: break;
			case 1: p.x+=size; break;
			case 2: p.y+=size; break;
			case 3: p.y+=size; p.x+=size; break;
			}
			Vertex v=vstore.obtain(p,(byte)zoomlevel);
			//System.out.println("Added base "+v);
			base_vertices.add(v);
			//System.out.println("Created v "+v.getimerc());
			stitcher.stitch(v,zoomlevel,true);
		}
		need_retriangulation=true;
	}
	
	private void releaseTriangles(TriangleStore tristore)
	{
		for(Triangle t:triangles)
			tristore.release(t);
		triangles.clear();
	}
	
	private static Comparator<Vertex> cmp0=new Comparator<Vertex>(){
		public int compare(Vertex a, Vertex b) {								
			return a.getx()-b.getx();
		}					
	};
	private static Comparator<Vertex> cmp1=new Comparator<Vertex>(){
		public int compare(Vertex a, Vertex b) {								
			return b.gety()-a.gety();
		}					
	};
	private static Comparator<Vertex> cmp2=new Comparator<Vertex>(){
		public int compare(Vertex a, Vertex b) {								
			return b.getx()-a.getx();
		}					
	};
	private static Comparator<Vertex> cmp3=new Comparator<Vertex>(){
		public int compare(Vertex a, Vertex b) {								
			return a.gety()-b.gety();
		}					
	};
	private static ArrayList<Comparator<Vertex> > cmps=new ArrayList<Comparator<Vertex>>();
	static {
		cmps.add(cmp0);
		cmps.add(cmp1);
		cmps.add(cmp2);
		cmps.add(cmp3);
	}
	/* (non-Javadoc)
	 * @see se.flightplanner.map3d.ThingIf#triangulate(se.flightplanner.map3d.TriangleStore)
	 */
	public void triangulate(TriangleStore tristore)
	{
		if (!need_retriangulation) return;
		int vcnt=0;
		if (edges!=null)
			for(int i=0;i<4;++i)
				vcnt+=edges.get(i).size();
		releaseTriangles(tristore);
		if (vcnt==0)
		{
			Vertex v0=base_vertices.get(0);
			Vertex v1=base_vertices.get(1);
			Vertex v2=base_vertices.get(2);			
			Vertex v3=base_vertices.get(3);			
			addTri(tristore, v0, v2, v1);
			addTri(tristore, v2, v3, v1);
		}
		else
		{
			for(int i=0;i<4;++i)
			{
				ArrayList<Vertex> edgelist=new ArrayList<Vertex>();
				switch(i)
				{
					case 0:	edgelist.add(base_vertices.get(2));
							edgelist.add(base_vertices.get(3));break;
					case 1:	edgelist.add(base_vertices.get(3));
							edgelist.add(base_vertices.get(1));break;
					case 2:	edgelist.add(base_vertices.get(1));
							edgelist.add(base_vertices.get(0));break;
					case 3:	edgelist.add(base_vertices.get(0));
							edgelist.add(base_vertices.get(2));break;
				}
				edgelist.addAll(edges.get(i).keySet());
				Collections.sort(edgelist,cmps.get(i));
				for(int j=0;j<edgelist.size();++j)
					System.out.println("Edge "+i+" has vertex "+edgelist.get(j));
				for(int j=0;j+1<edgelist.size();++j)
				{
					addTri(tristore, center_vertex, edgelist.get(j) , edgelist.get(j+1));
				}
			}
		}
	}
	private void addTri(TriangleStore tristore, Vertex v1, Vertex v2, Vertex v3) {
		Triangle t1=tristore.alloc();
		t1.assign(v1,v2,v3);
		triangles.add(t1);
	}
	
	/* (non-Javadoc)
	 * @see se.flightplanner.map3d.ThingIf#shareVertex(se.flightplanner.map3d.VertexStore, se.flightplanner.map3d.Vertex, boolean)
	 */
	public void shareVertex(VertexStore vstore,Vertex v, boolean share)
	{
		boolean unshare=!share;
		if (isCorner(v))
			return; //vertex is one of the base(=corner) vertices, 			
		int side = getSide(v);
		if (side==-1)
			return;
		if (unshare && edges==null)
			return; //nothing to unshare
		if (edges==null && share)
		{
			edges=new ArrayList<HashMap<Vertex,Counter>>();
			for(int i=0;i<4;++i)
				edges.add(new HashMap<Vertex,Counter>());
		}
		HashMap<Vertex,Counter> hm=edges.get(side);
		Counter c=hm.get(v);
		if (c==null)
		{
			if (unshare)
			{
				//trying to unshare a vertex we don't even own.
				return;
			}
			c=new Counter(1);
			hm.put(v, c);
			
			iMerc centerpos=pos.copy();
			centerpos.x+=size/2;
			centerpos.y+=size/2;			
			center_vertex=vstore.obtain(centerpos, (byte)zoomlevel);
			
			need_retriangulation=true;
		}
		else
		{
			if (unshare)
			{
				c.cnt-=1;
				if (c.cnt<=0)
				{
					hm.remove(v);
					vstore.decrement(center_vertex);
					if (hm.isEmpty())
						edges=null;
					need_retriangulation=true;
				}
			}
			else
			{
				Assert.assertTrue(false); //shouldn't happen, should it?
				c.cnt+=1;
			}
		}
	}
	
	/*
	public void delVertex(Vertex v)
	{
		if (isCorner(v))
			return; //none of the children of a Thing shares its corner, and the parent is _always_ more longer lived, so a delVertex is never for a corner.  			
		int side=getSide(v);
		if (side==-1) //not possibly part of Thing, since it isn't on things edge.
			return;
		assert side>=0 && side<4;
		if (edges==null)
			return; //not possibly part of thing, since thing has no stitches
		HashMap<Vertex,Counter> hm=edges.get(side);
		Counter c=hm.get(v);
		if (c==null) return;
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
	}*/

	
}
