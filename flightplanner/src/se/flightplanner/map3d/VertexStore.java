package se.flightplanner.map3d;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import se.flightplanner.Project.Merc;
import se.flightplanner.Project.iMerc;

public class VertexStore {

	private FloatBuffer buf;
	private LinkedList<Vertex> free;
	private HashMap<iMerc,Vertex> used;
	
	public HashMap<Short,Vertex> dbgGetVertices(){
		HashMap<Short,Vertex> h=new HashMap<Short,Vertex>();
		for(Vertex v:used.values())
		{
			h.put(new Short(v.getPointer()), v);
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
			free.add(new Vertex(ptr));
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
	public Vertex obtain(iMerc p,byte zoomlevel)
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
		shiny.deploy(p.x,p.y,zoomlevel);
		//System.out.println("Obtained new "+shiny);
		return shiny;
	}
	public Vertex obtaindbg(iMerc iMerc, byte b) {
		Vertex dbg=new Vertex((short)-1);
		dbg.deploy(iMerc.x,iMerc.y,b);
		return dbg;
	}		
	public VertexStore(int capacity)
	{
		if (capacity<=0 || capacity>32000)
			throw new RuntimeException("Invalid capacity for VertexStore:"+capacity);
		free=new LinkedList<Vertex>();
		used=new HashMap<iMerc,Vertex>(capacity);
		ByteBuffer bytebuf= ByteBuffer.allocateDirect(capacity*4*3);
		buf=bytebuf.asFloatBuffer();
		for(short i=0;i<capacity;++i)
		{
			free.add(new Vertex(i));
		}		
	}
	public int usedVertices() {
		return used.size();
	}
}
