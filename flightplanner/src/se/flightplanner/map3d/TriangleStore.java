package se.flightplanner.map3d;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import android.util.Log;

import se.flightplanner.Project.Merc;
import se.flightplanner.Project.iMerc;
import se.flightplanner.map3d.Triangle;

public class TriangleStore {
	private ShortBuffer buf;
	private LinkedList<Triangle> free;
	private HashSet<Triangle> used;
	private ArrayList<Triangle> all;
	public TriangleStore(int capacity)
	{
		free=new LinkedList<Triangle>();
		if (capacity<=0 || capacity>=32000)
			throw new RuntimeException("Bad triangle count for TriangleStore");
		ByteBuffer bytebuf= ByteBuffer.allocateDirect(capacity*2*3);
		bytebuf.order(ByteOrder.nativeOrder());
		buf=bytebuf.asShortBuffer();
		used=new HashSet<Triangle>();
		all=new ArrayList<Triangle>();
		all.ensureCapacity(capacity);
		for(short i=0;i<capacity;++i)
		{
			Triangle t=new Triangle(i);
			all.add(t);
			free.add(t);
		}
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
	class Indices
	{
		public ShortBuffer buf;
		public int tricount;
	}
	public static class DbgTriangle2D
	{
		private iMerc[] pos=new iMerc[3]; 
		private int[] alt=new int[3]; /*Not part of identity, just 'payload'*/
		public DbgTriangle2D()
		{
		}
		public DbgTriangle2D(iMerc a,iMerc b,iMerc c)
		{
			pos[0]=a;
			pos[1]=b;
			pos[2]=c;
			sort();
		}
		public DbgTriangle2D(int ax,int ay,int bx,int by,int cx,int cy)
		{
			pos[0]=new iMerc(ax,ay);
			pos[1]=new iMerc(bx,by);
			pos[2]=new iMerc(cx,cy);
			sort();
		}
		private void sort()
		{
			int minx=(1<<30),miny=(1<<30);
			int besti=0;
			for(int i=0;i<3;++i)
			{
				if (pos[i].x<minx || (minx==pos[i].x && pos[i].y<miny)) 
				{
					minx=pos[i].x;
					miny=pos[i].y;
					besti=i;
				}
			}
			iMerc[] out_pos=new iMerc[3]; 
			int[] out_alt=new int[3];
			int in_i=besti;
			for(int out_i=0;out_i<3;++out_i,++in_i)
			{
				in_i%=3;
				out_pos[out_i]=pos[in_i];
				out_alt[out_i]=alt[in_i];
			}
			pos=out_pos;
			alt=out_alt;			
		}
		@Override
		public String toString()
		{
			StringBuilder b=new StringBuilder();
			b.append("DbgTriangle2D(");
			for(int i=0;i<3;++i)
			{
				b.append(" "+pos[i]);
			}
			b.append(")");
			return b.toString();
		}
		@Override
		public boolean equals(Object oo)
		{
			DbgTriangle2D o=(DbgTriangle2D)oo;
			for(int i=0;i<3;++i)
				if (!pos[i].equals(o.pos[i]))
					return false;
			return true;
		}
		@Override
		public int hashCode()
		{
			int ret=0;
			for(int i=0;i<3;++i)
				ret+=pos[i].hashCode();
			return ret;
		}
	}
	public HashSet<DbgTriangle2D> dbgGetTriangles2D(HashMap<Short,Vertex> vertices)
	{
		HashSet<DbgTriangle2D> ret=new HashSet<DbgTriangle2D>();
		for(Triangle t : all)
		{
			if (!t.isUsed()) continue;
			DbgTriangle2D dt=new DbgTriangle2D();
			for(int i=0;i<3;++i)
			{
				Vertex v=vertices.get(t.idx[i]);
				dt.pos[i]=v.getimerc();
				dt.alt[i]=v.getLastElev();
			}
			dt.sort();
			ret.add(dt);
		}
		return ret;
	}
	public Indices getIndexForRender()
	{
		buf.position(0);
		Indices ret=new Indices();
		ret.buf=buf;
		ret.tricount=0;
		int cnt=0;
		for(Triangle t:all)
		{
			if (!t.isUsed()) continue;
			buf.put(t.getidx(0));
			buf.put(t.getidx(1));
			buf.put(t.getidx(2));
			Log.i("fplan","Index nr #"+cnt+": "+t.getidx(0)+","+t.getidx(1)+","+t.getidx(2));
			ret.tricount+=1;
			cnt+=1;
		}
		buf.position(0);
		return ret;
	}
}
