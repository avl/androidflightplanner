package se.flightplanner.map3d;

import java.util.ArrayList;
import java.util.HashMap;

import se.flightplanner.Project.Merc;
import se.flightplanner.Project.iMerc;

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
	private iMerc pos;
	private Thing parent;
	private ArrayList<Vertex> base_vertices; //lower-left,lower-right,upper-right, upper left
	private Vertex center; //Always present, used to judge bumpiness
	private ArrayList<HashMap<Vertex,Counter>> edges;
	private ArrayList<Triangle> triangles;
	private boolean subsumed; //if the current thing has been replaced by smaller things
	private boolean need_retriangulation; //set whenever edge vertices are added.
	private boolean known_ready;//ready when all vertices have received their elevation
	private boolean deployed; //set to true when a thing becomes visible. Implies that parent->subsumed is true. 

	public float bumpiness() {
		
		return 0;
	}
	
	
	public boolean checkReady()
	{
		if (known_ready)
			return true;
		for(Vertex v:base_vertices)
		{
			if (!v.hasElev())
				return false;
		}
		return true;
	}
	iMerc getPos()
	{
		return pos;
	}
	public Thing(iMerc pos,int size,Thing parent,int zoomlevel,VertexStore vstore)
	{
		this.pos=pos;
		this.subsumed=false;
		this.size=size;
		this.parent=parent;
		this.zoomlevel=zoomlevel;
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
			Vertex v=vstore.obtain(p);
			base_vertices.add(v);
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
	public void addVertex(int side,Vertex v)
	{
		assert side>=0 && side<4;
		HashMap<Vertex,Counter> hm=edges.get(side);
		Counter c=hm.get(v);
		if (c==null)
		{
			c=new Counter(1);
			hm.put(v, c);
		}
		else
		{
			c.cnt+=1;
		}
		need_retriangulation=true;
	}
	public void delVertex(int side,Vertex v)
	{
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
	
}
