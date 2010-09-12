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
	private ElevationReader elevReader;
	
	public void decomission(Vertex v)
	{
		short ptr=v.getPointer();
		if (v.decrease())
		{
			used.remove(v.getimerc());
			free.add(
				new Vertex(
						0,0,(short)0,ptr
						));
		}
	}
	public Vertex obtain(iMerc p)
	{
		Vertex already=used.get(p);
		if (already!=null) 
		{
			already.increase();
			return already;
		}
		if (free.isEmpty()) throw new RuntimeException("No more vertices available!");
		Vertex shiny=free.poll();
		shiny.deploy(p.x,p.y);
		elevReader.queue(shiny);
		return shiny;
	}
	public VertexStore(int capacity,ElevationReader elevReader)
	{
		this.elevReader=elevReader;
		if (capacity<=0 || capacity>32000)
			throw new RuntimeException("Invalid capacity for VertexStore:"+capacity);
		free=new LinkedList<Vertex>();
		used=new HashMap<iMerc,Vertex>(capacity);
		ByteBuffer bytebuf= ByteBuffer.allocateDirect(capacity*4*3);
		buf=bytebuf.asFloatBuffer();
		for(short i=0;i<capacity;++i)
		{
			free.add(
					new Vertex(
							0,0,(short)0,i
							));
		}		
	}		
}
