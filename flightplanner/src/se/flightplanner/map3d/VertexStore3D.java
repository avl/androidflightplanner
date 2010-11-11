package se.flightplanner.map3d;

import java.io.IOException;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;

import se.flightplanner.Project.iMerc;
import se.flightplanner.map3d.TerrainVertexStore.VertAndColor;

public class VertexStore3D {

	private ByteBuffer colors;
	private FloatBuffer vertexbuf;
	private FloatBuffer texcoordbuf;
	private LinkedList<Vertex> free;
	private HashMap<iMerc,Vertex> used;
	private ArrayList<Vertex> all;
	private Random random=new Random();
	public VertexStore3D(int capacity)
	{
		free=new LinkedList<Vertex>();
		used=new HashMap<iMerc,Vertex>(capacity);
		if (capacity<=0 || capacity>32000)
			throw new RuntimeException("Invalid capacity for VertexStore:"+capacity);

		colors=ByteBuffer.allocateDirect(capacity*4);
		colors.order(ByteOrder.nativeOrder());
		
		ByteBuffer bytebuf= ByteBuffer.allocateDirect(capacity*4*3);
		bytebuf.order(ByteOrder.nativeOrder());
		vertexbuf=bytebuf.asFloatBuffer();
		
		ByteBuffer bytebuf2= ByteBuffer.allocateDirect(capacity*4*2);
		bytebuf2.order(ByteOrder.nativeOrder());
		texcoordbuf=bytebuf2.asFloatBuffer();
			
		all=new ArrayList<Vertex>();
		all.ensureCapacity(capacity);
		for(short i=0;i<capacity;++i)
		{
			Vertex n=new Vertex(i);
			all.add(n);			
			free.add(n);
		}		
		
	}
	VertAndColor getVerticesReadyForRender(TerrainVertexStore terrainVertexStore, iMerc observer, int altitude)
	{
		vertexbuf.position(0);
		texcoordbuf.position(0);
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
				
				z=-1.0f*(calz-altitude);			
				x=(v.getx()-observer.x);
				y=(v.gety()-observer.y);
				
				texcoordbuf.put(v.getu());
				texcoordbuf.put(v.getv());
				
				x*=0.01;
				y*=0.01;
				z*=0.01;
				if (calzraw<50)
				{
					b=(byte)-1;
					g=0;
				}
				if (calzraw>255)
				{
					int t=calzraw-255;
					g=(byte)-1;
					r+=t;
				}
				else
				{
					g=(byte)(calzraw);//-1;//(byte)z;					
				}
				//g=(byte)((i*8)%256);//(byte)z;
				//b=(byte)((i*64)%256);//(byte)z;
				//Log.i("fplan","Rendered vertex #"+i+": "+x+","+y+","+z+" rawZ:"+calzraw);
			}
			else
			{
				texcoordbuf.put(0.0f);
				texcoordbuf.put(0.0f);				
			}
			vertexbuf.put(x);
			vertexbuf.put(y);
			vertexbuf.put(z);
	
			/*
			colors.put(r);
			colors.put(g);
			colors.put(b);
			colors.put((byte)-1); //alpha
			*/
			colors.put((byte)v.r);
			colors.put((byte)v.g);
			colors.put((byte)v.b);
			colors.put((byte)-1); //alpha
			/*
			colors.put((byte)-1);
			colors.put((byte)-1);
			colors.put((byte)-1);
			colors.put((byte)-1);
			*/
		}
		vertexbuf.position(0);
		colors.position(0);
		texcoordbuf.position(0);
		VertAndColor va=new VertAndColor();
		va.colors=colors;
		va.vertices=vertexbuf;
		va.texcoords=texcoordbuf;
		return va;
	}

	public boolean decrement(TerrainVertexStore terrainVertexStore, Vertex v)
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

	public Vertex alloc() {
		// TODO Auto-generated method stub
		if (free.isEmpty()) throw new RuntimeException("No more vertices available!");
		Vertex f=free.poll();
		return f;
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
	public int getFreeVertices() {
		return free.size(); //TODO: Check if this is inefficient
	}
	
}
