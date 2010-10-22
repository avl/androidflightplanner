package se.flightplanner.map3d;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;

import android.util.Log;

import junit.framework.Assert;

import se.flightplanner.Project;
import se.flightplanner.Project.Merc;
import se.flightplanner.Project.iMerc;
import se.flightplanner.map3d.ElevationStore.Elev;

public class Thing implements ThingIf {

	/*private static class Counter
	{
		public int cnt;
		public Counter(int start)
		{
			cnt=start;
		}
	}*/
	private int zoomlevel;
	private float refine;
	
	private int size; //of box's side, in merc units
	/* (non-Javadoc)
	 * @see se.flightplanner.map3d.ThingIf#getZoomlevel()
	 */
	public int getZoomlevel()
	{
		return zoomlevel;
	}
	private Elev elev;
	boolean released;
	private iMerc pos; //upper left corner
	private ThingIf parent;
	Vertex center_vertex;
	
	private ArrayList<Vertex> base_vertices; //lower-left,lower-right,upper-right, upper left
	//private Vertex center; //Always present, used to judge bumpiness
	private ArrayList<HashSet<Vertex>> edges; //edge numbering: lower, right, upper, left
	private ArrayList<Triangle> triangles;
	//private boolean subsumed; //if the current thing has been replaced by smaller things
	private boolean need_retriangulation; //set whenever edge vertices are added.
	private boolean known_ready;//ready when all vertices have received their elevation
	private boolean deployed; //set to true when a thing becomes visible. Implies that parent->subsumed is true.
	private ArrayList<ThingIf> children; //order: upper row first; left-right, then second row; left-right.

	public HashSet<Vertex> getEdgeVertices()
	{
		HashSet<Vertex> ret=new HashSet<Vertex>();
		if (edges!=null)
		{
			for(HashSet<Vertex> vs : edges)
			{
				for(Vertex v:vs)
				{
					ret.add(v);
				}
			}
		}
		return ret;
	}
	public HashSet<Vertex> getCornersAndCenter()
	{
		HashSet<Vertex> ret=new HashSet<Vertex>();
		if (base_vertices!=null)
			for(Vertex b:base_vertices)
			{
				ret.add(b);
			}
		if (center_vertex!=null)
			ret.add(center_vertex);
		return ret;
	}
	public ArrayList<ThingIf> getAllChildren()
	{
		return children;
	}
	public ThingIf getChild(int i,int j)
	{
		if (i<0 || i>=2) throw new RuntimeException("Bad i-value in Thing.getChild");
		if (j<0 || j>=2) throw new RuntimeException("Bad j-value in Thing.getChild");
		int idx=i+2*j;
		return children.get(idx);
	}

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
	
	public boolean isSubsumed() {
		return children!=null;
	}
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
		else if (obs.x>=pos2.x) 
			x=pos2.x;
		else 
			x=obs.x;
		
		if(obs.y<pos1.y)
			y=pos1.y;
		else if (obs.y>=pos2.y) 
			y=pos2.y;
		else 
			y=obs.y;
		
		float a=(obs.x-x);
		float b=(obs.y-y);
		float c=feet2merc(obs,observerHeight-elev.hiElev);
		float dist=(float)Math.sqrt((a*a+b*b+c*c));
		return dist;
	}
	
	private float feet2merc(iMerc pos,int feet) {
		return Project.approx_ft_pixels(pos, 13)*feet;
	}
	public String toString()
	{
		StringBuilder b=new StringBuilder();
		b.append("Thing(");
		b.append(""+pos.x);
		b.append(","+pos.y);		
		b.append(",zoom="+zoomlevel);
		b.append(")");
		return b.toString();
	}
	/* (non-Javadoc)
	 * @see se.flightplanner.map3d.ThingIf#subsume(java.util.ArrayList, se.flightplanner.map3d.VertexStore, se.flightplanner.map3d.Stitcher, se.flightplanner.map3d.ElevationStore)
	 */
	public void subsume(ArrayList<ThingIf> newThings,VertexStore vstore,Stitcher stitcher,ElevationStore estore)
	{
		if (children!=null) 
			return; //Already subsumed
		children=new ArrayList<ThingIf>();
		need_retriangulation=true;
		for(int cury=pos.y;cury<pos.y+size;cury+=size/2)
		{
			for(int curx=pos.x;curx<pos.x+size;curx+=size/2)
			{
				Thing child=new Thing(new iMerc(curx,cury),this,zoomlevel+1,vstore,estore,stitcher);
				if (edges!=null)
				{
					for(HashSet<Vertex> vs:edges)
					{
						for(Vertex v:vs)
						{
							child.shareVertex(vstore, v, true);
						}
					}
				}
				children.add(child);
				newThings.add(child);
			}
		}
		edges=null;
	}
	/* (non-Javadoc)
	 * @see se.flightplanner.map3d.ThingIf#unsubsume(se.flightplanner.map3d.VertexStore, se.flightplanner.map3d.Stitcher)
	 */
	public void unsubsume(VertexStore vstore,Stitcher st,ArrayList<ThingIf> removed_things,TriangleStore tristore)
	{
		if (children==null)
			return; //already unsubsumed
		//Start showing again.
		if (edges!=null)
			throw new RuntimeException("Bad state of edges in subsumed Thing");
		//Note that if the mid-edge-vertices aren't usage=1, that
		//means that someone else is still using them, and the unsubsumed parent Thing
		//must stitch them.
		
		
		need_retriangulation=true;
		
		Vertex old_center=center_vertex;
		if (center_vertex!=null)
		{
			if (vstore.decrement(center_vertex))
				st.stitch(center_vertex,this.zoomlevel,parent,false);
		}
		center_vertex=null;
		
		HashSet<Vertex> neededStitching=new HashSet<Vertex>();
		for(ThingIf child : children)
		{
			child.release(neededStitching,vstore,st,removed_things,tristore);
		}
		children=null;
		if (old_center!=null && old_center.isUsed())
		{
			throw new RuntimeException("center_vertex still used after child-thing release.");
		}
		
		//Now, implement the clever neededStitching unsubsume hack!
		//(Described elsewhere)
		for(Vertex v : neededStitching)
		{
			if (v.isUsed()==false)
				throw new RuntimeException("Thing "+this+" attempt to shareVertex "+v+" as part of neededStitching-hack");
			this.shareVertex(vstore,v,true);
		}
	}
	
	public boolean isReleased()
	{
		return released;
	}
	
	/** 
	 * Call when a Thing is going away. This is not subsumption, this is destruction.
	 * I.e, this Thing has too high detail to even exist in the scene at all.
	 * 
	 * Things that need to be done when this happens:
	 * * Decrement use count on the Thing's vertices. Some of those may go away.
	 * * The set of vertices 
	 * * The Thing needs to be taken out of the Playfield. The responsibility for this rests with the
	 *   caller. However, the array removed_things will have 'this' appended.
	 * 
	 */
	public void release(HashSet<Vertex> neededStitching,VertexStore vstore,Stitcher st,ArrayList<ThingIf> removed_things,TriangleStore tristore) {

		Vertex old_center=center_vertex;
		if (center_vertex!=null)
		{
			//can never be stitched anywhere.
			if (vstore.decrement(center_vertex))
				st.stitch(center_vertex,this.zoomlevel,parent,false);
			//Center vertex can still be used by children at this point.
		}
		center_vertex=null;
		
		if (children!=null)
		{
			for(ThingIf child:children)
			{
				child.release(neededStitching,vstore,st,removed_things,tristore);				
			}
		}
		children=null;
		if (old_center!=null && old_center.isUsed())
		{ //This test can simply be removed if Vertex object reusing is to be employed in the future
			throw new RuntimeException("center_vertex should be unused after all child-things have been released. Used:"+old_center.dbgUsage());
		}
		
		this.released=true;
		if (edges!=null)
		{
			for(HashSet<Vertex> edge : edges)
			{
				for (Vertex v : edge)
				{
					//TODO: IS the "neededStitching"-hack really worth it?
					//Might it be better to simply dive depth-first into the 
					//Thing-tree and find all potential Edge-vertices?
					if (v.isUsed())
						neededStitching.add(v);
				}
			}
		}
		
		for(Vertex v:base_vertices)
		{
			boolean unused=vstore.decrement(v);
			//System.out.println("Freeing "+v);
			if (unused)
			{
				//this vertex is no longer used by the this-object,
				//but also, it is not used by any object at all, thus it
				//can safely be removed without any worries. It needs to
				//be unstitched if it is stitched somewhere.
				st.stitch(v,this.zoomlevel,parent,false);
				//This vertex no longer needs to be stitched into the parent,
				//since it no long exists!
				neededStitching.remove(v); 
			}
			else
			{
				//this vertex was a base_vertex of this object, which it is no longer (since
				//this object is going away).
				//However, it is still used by bigger objects.
				//WARNING "CLEVER" HACK AHEAD!
				//The fact that this vertex is still used while the thing that owns it goes away
				//means that this is a vertex still used by some other Thing. That thing may
				//simply be the parent, in which case nothing needs to be done.
				//BUT! It may also be a fine (high detail) neighbor, in which case this
				//vertex needs to be _stitched_ into the parent Thing! So the caller will
				//attempt to do that stitching into the parent, for the vertices added
				//to "neededStitching".			
				neededStitching.add(v);
			}
		}
		base_vertices=null;
		removed_things.add(this);
		
		releaseTriangles(tristore);
	}
	public Thing(iMerc pos,ThingIf parent,int zoomlevel,VertexStore vstore,ElevationStore elevStore, Stitcher stitcher)
	{
		int zoomgap=13-zoomlevel;
		this.size=64<<zoomgap;
		this.pos=pos;
		this.parent=parent;
		this.zoomlevel=zoomlevel;
		this.triangles=new ArrayList<Triangle>();
		this.elev=elevStore.get(pos,zoomlevel-6);
		this.released=false;
		Elev oelev=this.elev;
		if (this.elev==null)
			this.elev=new Elev((short)0,(short)0);
		//Log.i("fplan","Elevstore got hi-elev "+elev.hiElev+" loelev:"+elev.loElev+"for pos "+pos+" zoom "+zoomlevel+" elevobj="+oelev);
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
			Vertex v=vstore.obtain(p,(byte)zoomlevel,"Base vertex "+i+" of thing "+this);
			//System.out.println("Added base "+v);
			base_vertices.add(v);
			//System.out.println("Created v "+v.getimerc());
			stitcher.stitch(v,zoomlevel,parent,true);
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

	public void calcElevs1(TriangleStore tristore, VertexStore vstore)
	{
		if (base_vertices==null || isReleased())
			throw new RuntimeException("calcElevs called for released Thing");
		if (refine==0.0f) throw new RuntimeException("refine is 0.0");
		short ref=(short)(100*refine);
		if (ref<0) ref=1;
		for(int i=0;i<4;++i)
		{
			Vertex v=base_vertices.get(i);
			v.contribElev((short)(elev.hiElev*refine),ref);			
		}
		
	}
	public void calcElevs2(TriangleStore tristore, VertexStore vstore)
	{
		if (base_vertices==null || isReleased())
			throw new RuntimeException("calcElevs called for released Thing2");
		if (center_vertex!=null)
		{
			int upperright_height=base_vertices.get(1).calcZ();
			int lowerleft_height=base_vertices.get(2).calcZ();
			int middleheight=(upperright_height+lowerleft_height)/2;
			center_vertex.contribElev((short)middleheight,(short)100);			
		}
	}
	public void adjustRefine(float refine)
	{
		this.refine=refine;
	}
	public void triangulate(TriangleStore tristore,VertexStore vstore)
	{
		if (base_vertices==null || isReleased())
			throw new RuntimeException("triangulate called for released Thing");
		//if (false && !need_retriangulation) return;
		releaseTriangles(tristore);
		//System.out.println("Released tris for "+this);
		if (children!=null)
			return; //this block is subsumed! It thus has no own triangles.
			
		int vcnt=0;
		if (edges!=null)
			for(int i=0;i<4;++i)
				vcnt+=edges.get(i).size();
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
				if (base_vertices.get(i).isUsed()==false)
					throw new RuntimeException("Base vertex "+i+" of thing "+this+" has unused vertex: "+base_vertices.get(i));	
			}
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
				edgelist.addAll(edges.get(i));
				Collections.sort(edgelist,cmps.get(i));
				for(int j=0;j<edgelist.size();++j)
				{
					//System.out.println("Edge "+i+" has vertex "+edgelist.get(j));
					if (edgelist.get(j).isUsed()==false)
						throw new RuntimeException("Edge "+i+" of thing "+this+" has unused vertex: "+edgelist.get(j));
				}
				
				if (center_vertex==null)
				{
					iMerc centerpos=pos.copy();
					//System.out.println("Creating center vertex for Thing "+this+" which is at "+pos);
					centerpos.x+=size/2;
					centerpos.y+=size/2;			
					//System.out.println("Creating center vertex for Thing "+this+" at "+centerpos);
					center_vertex=vstore.obtain(centerpos, (byte)zoomlevel,"Center vertex for "+this);
					/// not needed - contrib elev is now done *after* triangulation center_vertex.contribElev((short)(elev.hiElev),(short)100);
					if (center_vertex.dbgUsage()!=1)
						throw new RuntimeException("When creating new center vertex during rendering, created center_vertex was not singularly owned!");
				}				
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
	boolean haveSharedVertex()
	{
		if (edges==null) return false;
		for(HashSet<Vertex> vs : edges)
			if (vs.size()>0)
				return true;
		return false;
	}
	public int getBoxSize()
	{
		return size;
	}
	public void shareVertex(VertexStore vstore,Vertex v, boolean share)
	{
		if (base_vertices==null)
		{
			System.out.println("Unexpected happened");
		}
		if (isSubsumed())
		{
			if (!share)
				assertNotInEdge(v);
			return;
		}
		boolean unshare=!share;
		if (isCorner(v))
		{
			if (!share)
				assertNotInEdge(v);
			return; //vertex is one of the base(=corner) vertices,
		}
		int side = getSide(v);
		if (side==-1)
		{
			if (!share)
				assertNotInEdge(v);
			return;
		}
		if (unshare && edges==null)
			return; //nothing to unshare
		if (share && edges==null)
		{
			edges=new ArrayList<HashSet<Vertex>>();
			for(int i=0;i<4;++i)
				edges.add(new HashSet<Vertex>());
		}
		HashSet<Vertex> hm=edges.get(side);
		if (share)
		{
			hm.add(v);
			need_retriangulation=true; //TODO: Only set need_retriangulation if something actually changed
		}
		else
		{
			hm.remove(v);
			need_retriangulation=true; //TODO: Only set need_retriangulation if something actually changed
		}
		if (!share)
			assertNotInEdge(v);
		
		boolean haveShared=haveSharedVertex();
		/*if (haveShared && center_vertex==null)
		{
			iMerc centerpos=pos.copy();
			//System.out.println("Creating center vertex for Thing "+this+" which is at "+pos);
			centerpos.x+=size/2;
			centerpos.y+=size/2;			
			System.out.println("Creating center vertex for Thing "+this+" at "+centerpos);
			center_vertex=vstore.obtain(centerpos, (byte)zoomlevel,"Center vertex for "+this);
			need_retriangulation=true;
		}*/
		if (!haveShared)
		{
			edges=null;
			need_retriangulation=true;
		}
		/*if (!haveShared && center_vertex!=null)	
		{			
			vstore.decrement(center_vertex);
			//TODO:should it be unused here as well?
			center_vertex=null;
			need_retriangulation=true;
		}*/
	}

	private void assertNotInEdge(Vertex v) {
		if (edges!=null)
		{
			for(int i=0;i<edges.size();++i)
			{
				for(Vertex t:edges.get(i))
				{
					if (t.equals(v))
						throw new RuntimeException("Thing edge unexpectedly contains vertex: "+v+" (namely, it contains : "+t+")");
				}
			}
		}
	}

	public ThingIf getParent() {
		return parent;
	}

	public void debugDump(Writer f) throws IOException {
		f.write("{\n");
		f.write("  \"pos\" : \""+getPosStr()+"\",\n");
		f.write("  \"base_vertices\" : [\n  ");
		if (base_vertices.size()>0)
		{
			for(int i=0;i<this.base_vertices.size();++i)
			{
				if (i!=0) f.write(" , ");
				f.write(""+base_vertices.get(i).getIndex());
			}
		}
		f.write("],\n");
		if (center_vertex!=null)
			f.write("  \"center_vertex\" : "+center_vertex.getIndex()+",\n");
		else
			f.write("  \"center_vertex\" : null,\n");
		f.write("  \"triangles\" : [\n");
		for(int i=0;i<triangles.size();++i)
		{
			if (i!=0) f.write(" , ");
			f.write(""+triangles.get(i).getPointer());
		}
		f.write("],\n");
		f.write("  \"parent\" : "+((parent==null) ? "null,\n" : ("\""+parent.getPosStr()+"\",\n")));

		f.write("  \"children\":[\n");
		if (children!=null)
		{
			for(int i=0;i<children.size();++i)
			{
				if (i!=0) f.write(" , ");
				f.write("\""+children.get(i).getPosStr()+"\"");
			}
		}
		f.write("],\n");
		
		f.write("  \"edges\":[\n");
		if (edges!=null)
		{
			for(int i=0;i<edges.size();++i)
			{
				if (i!=0) f.write(" , ");
				f.write("[");
				int cnt=0;
				for(Vertex edgev : edges.get(i))
				{
					if (cnt!=0) f.write(" , ");
					f.write("      "+edgev.getIndex());
					++cnt;
				}						
				f.write("]\n");
			}
		}
		f.write("  ]\n");
		f.write("}\n");
	}

	public String getPosStr() {
		return ""+pos.x+","+pos.y+","+zoomlevel;
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
