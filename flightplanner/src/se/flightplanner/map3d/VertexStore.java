package se.flightplanner.map3d;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import android.util.Log;

import se.flightplanner.Project.Merc;
import se.flightplanner.Project.iMerc;

public class VertexStore {

	private FloatBuffer buf;
	private ByteBuffer colors;
	private LinkedList<Vertex> free;
	private HashMap<iMerc,Vertex> used;
	private ArrayList<Vertex> all;
	
	public HashMap<Short,Vertex> dbgGetVertices(){
		HashMap<Short,Vertex> h=new HashMap<Short,Vertex>();
		for(Vertex v:used.values())
		{
			h.put(new Short(v.getPointer()), v);
		}
		return h;
	}
	public HashSet<iMerc> dbgGetIMercSet(){
		HashSet<iMerc> h=new HashSet<iMerc>();
		for(Vertex v:used.values())
		{
			h.add(v.getimerc());
		}
		return h;
	}
	
	///If this returns true,
	///then the vertex was actually "destroyed"
	///and MUST be removed from all stitches before
	///next frame!
	public boolean decrement(Vertex v)
	{
		if (v.decrementUsage())
		{
			short ptr=v.getPointer();
			used.remove(v.getimerc());
			//Some explanation for the next line.
			//Initially it was aimed for not having to
			//use new in the render loop, for vertices. 
			//The idea being that the same objects could be recycled
			//over and over. However, not using new turned out to
			//be too restrictive. And the Vertex objects aren't
			//more expensive than any other. Presently the 'neededStitching'
			//hack in Thing is dependent on Vertices not being recycled.
			//More precisely, the hack counts on Vertexes living indefinitely with
			//count = 0 after having been removed from the VertexStore here
			//in this routine.
			Vertex n=new Vertex(ptr);
			n.setWhat("Previously "+v.getWhat()+" at: "+v.getx()+","+v.gety());
			free.add(n);
			all.set(ptr,n);
			return true;
		}
		return false;
	}
	/**
	 * 
	 * @param p Position of vertex
	 * @param zoomlevel The zoomlevel of the owning Things.
	 * @return New or reused Vertex
	 */
	public Vertex obtain(iMerc p,byte zoomlevel,String what)
	{
		Vertex already=used.get(p);
		if (already!=null) 
		{
			already.incrementUsage();
			return already;
		}
		if (free.isEmpty()) throw new RuntimeException("No more vertices available!");
		Vertex shiny=free.poll();
		
		used.put(p, shiny);
		shiny.deploy(p.x,p.y,zoomlevel,what);
		//System.out.println("Obtained new "+shiny);
		return shiny;
	}
	public Vertex obtaindbg(iMerc iMerc, byte b) {
		Vertex dbg=new Vertex((short)-1);
		dbg.deploy(iMerc.x,iMerc.y,b,"debug");
		return dbg;
	}		
	public VertexStore(int capacity)
	{
		if (capacity<=0 || capacity>32000)
			throw new RuntimeException("Invalid capacity for VertexStore:"+capacity);
		free=new LinkedList<Vertex>();
		used=new HashMap<iMerc,Vertex>(capacity);
		ByteBuffer bytebuf= ByteBuffer.allocateDirect(capacity*4*3);
		bytebuf.order(ByteOrder.nativeOrder());
		colors=ByteBuffer.allocateDirect(capacity*4);
		colors.order(ByteOrder.nativeOrder());
		buf=bytebuf.asFloatBuffer();
		all=new ArrayList<Vertex>();
		all.ensureCapacity(capacity);
		for(short i=0;i<capacity;++i)
		{
			Vertex n=new Vertex(i);
			all.add(n);			
			free.add(n);
		}		
	}
	public int usedVertices() {
		return used.size();
	}
	static class VertAndColor
	{
		public FloatBuffer vertices;
		public ByteBuffer colors;
	}
	HashSet<Vertex> dbgGetAllUsed()
	{
		HashSet<Vertex> ret=new HashSet<Vertex>();
		for(int i=0;i<all.size();++i)
		{
			Vertex v=all.get(i);
			if (v.isUsed())
			{
				ret.add(v);
			}
		}
		return ret;		
	}
	VertAndColor getVerticesReadyForRender(iMerc observer,int altitude)
	{
		buf.position(0);
		colors.position(0);
		for(int i=0;i<all.size();++i)
		{
			Vertex v=all.get(i);
			float x=0,y=0,z=0;
			byte r=0,g=0,b=0;
			int calzraw=-999;
			if (v.isUsed())
			{
				calzraw=v.calcZ();
				int calz=(int)(calzraw);
				v.resetElev();
				z=calz-altitude;			
				x=v.getx()-observer.x;
				y=v.gety()-observer.y;
				
				x*=0.01;
				y*=0.01;
				z*=0.01;
				g=(byte)(calzraw);//-1;//(byte)z;
				if (calzraw<50)
				{
					b=(byte)-1;
					g=0;
				}
				//g=(byte)((i*8)%256);//(byte)z;
				//b=(byte)((i*64)%256);//(byte)z;
				//Log.i("fplan","Rendered vertex #"+i+": "+x+","+y+","+z+" rawZ:"+calzraw);
			}
			buf.put(x);
			buf.put(y);
			buf.put(z);
			
			colors.put(r);
			colors.put(g);
			colors.put(b);
			colors.put((byte)-1);
		}
		buf.position(0);
		colors.position(0);
		VertAndColor va=new VertAndColor();
		va.colors=colors;
		va.vertices=buf;
		return va;
	}
	public void debugDump(Writer f) throws IOException {
		f.write("\"vertices\":[\n");
		int cnt=0;
		for(Vertex v:all)
		{
			if (cnt!=0) f.write(" , ");
			v.debugDump(f);
			++cnt;
		}
		f.write("]\n");
	}
	public boolean isUsed(short idx) {
		if (idx<0 || idx>=all.size()) throw new RuntimeException("idx out of bounds");
		return all.get(idx).isUsed();
	}
	
}
