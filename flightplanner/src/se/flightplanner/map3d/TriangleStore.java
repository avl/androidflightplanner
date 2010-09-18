package se.flightplanner.map3d;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import se.flightplanner.Project.Merc;
import se.flightplanner.map3d.Triangle;

public class TriangleStore {
	private ShortBuffer buf;
	private LinkedList<Triangle> free;
	private HashSet<Triangle> used;
	public TriangleStore(int capacity)
	{
		free=new LinkedList<Triangle>();
		if (capacity<=0 || capacity>=32000)
			throw new RuntimeException("Bad triangle count for TriangleStore");
		ByteBuffer bytebuf= ByteBuffer.allocateDirect(capacity*2*3);
		buf=bytebuf.asShortBuffer();
		used=new HashSet<Triangle>();
		for(short i=0;i<capacity;++i)
			free.add(new Triangle(i));		
	}
	public Triangle alloc()
	{
		if (free.isEmpty()) throw new RuntimeException("Out of triangles");
		Triangle t=free.poll();
		used.add(t);
		return t;
	}
	public void release(Triangle t)
	{
		t.reset();
		used.remove(t);
		free.add(t);
	}
	public ArrayList<Triangle> dbgGetTriangles() {

		ArrayList<Triangle> tris=new ArrayList<Triangle>();
		tris.addAll(used);
		return tris;
	}

}
