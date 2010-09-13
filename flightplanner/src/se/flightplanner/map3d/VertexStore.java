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
	
	public void decomission(Vertex v)
	{
		short ptr=v.getPointer();
		used.remove(v.getimerc());
		free.add(new Vertex(ptr));
	}
	public Vertex obtain(iMerc p,byte zoomlevel)
	{
		Vertex already=used.get(p);
		if (already!=null) 
		{
			return already;
		}
		if (free.isEmpty()) throw new RuntimeException("No more vertices available!");
		Vertex shiny=free.poll();
		shiny.deploy(p.x,p.y,zoomlevel);
		return shiny;
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
}
