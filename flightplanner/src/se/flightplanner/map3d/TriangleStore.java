package se.flightplanner.map3d;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.HashMap;
import java.util.LinkedList;

import se.flightplanner.Project.Merc;

public class TriangleStore {
	private ShortBuffer buf;
	private LinkedList<Triangle> free;
	public TriangleStore(int capacity)
	{
		if (capacity<=0 || capacity>=32000)
			throw new RuntimeException("Bad triangle count for TriangleStore");
		ByteBuffer bytebuf= ByteBuffer.allocateDirect(capacity*2*3);
		buf=bytebuf.asShortBuffer();
		for(short i=0;i<capacity;++i)
			free.add(new Triangle(i));		
	}
	public Triangle alloc()
	{
		if (free.isEmpty()) throw new RuntimeException("Out of triangles");
		Triangle t=free.poll();
		return t;
	}
	public void release(Triangle t)
	{
		t.reset();
		free.add(t);
	}

}
