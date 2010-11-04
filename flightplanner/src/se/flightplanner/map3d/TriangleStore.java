package se.flightplanner.map3d;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map.Entry;

import android.util.Log;

import se.flightplanner.Project.Merc;
import se.flightplanner.Project.iMerc;
import se.flightplanner.map3d.Triangle;

public class TriangleStore {
	private ShortBuffer mainbuf;
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
		mainbuf=bytebuf.asShortBuffer();
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
	public static interface RenderTexCb
	{
		public void renderTex(Texture tex,Indices ind); 
	}
	public void getIndexForRender(VertexStore vstore,RenderTexCb cb)
	{
		HashMap<Texture,ArrayList<Triangle>> s=new HashMap<Texture,ArrayList<Triangle>>();
		for(Triangle t:all)
		{
			if (!t.isUsed()) continue;
			Texture cp=t.getTexture();
			//cp=null;
			ArrayList<Triangle> tris=s.get(cp);
			if (tris==null)
			{
				tris=new ArrayList<Triangle>();
				s.put(cp,tris);
			}
			tris.add(t);
		}
		for(Entry<Texture, ArrayList<Triangle>> ent : s.entrySet())
		{
			Indices ind=new Indices();
			mainbuf.position(0);
			ind.buf=mainbuf;
			ind.tricount=0;
			for(Triangle t:ent.getValue())
			{
				for(int i=0;i<3;++i)
					if (!vstore.isUsed(t.getidx(i)))
						throw new RuntimeException("Triangle contains index which points to a presently non-used vertex! ("+t.getidx(i)+")");
				ind.buf.put(t.getidx(0));
				ind.buf.put(t.getidx(1));
				ind.buf.put(t.getidx(2));
				//Log.i("fplan","Index nr #"+cnt+": "+t.getidx(0)+","+t.getidx(1)+","+t.getidx(2));
				ind.tricount+=1;
			}			
			mainbuf.position(0);
			cb.renderTex(ent.getKey(),ind);

		}
		
/*
		HashMap<Texture,int[]> tex2cnt=new HashMap<Texture,int[]>(); 
		for(Triangle t:all)
		{
			if (!t.isUsed()) continue;
			Texture cp=t.getTexture();
			int[] cnts=tex2cnt.get(cp);
			if (cnts==null)
			{
				cnts=new int[1];
				cnts[0]=1;
				tex2cnt.put(cp, cnts);
			}
			else
			{
				cnts[0]+=1;
			}			
		}
		HashMap<Texture,Indices> tex2buf=new HashMap<Texture,Indices>();
		int idx=0;
		for(Entry<Texture, int[]> cnt : tex2cnt.entrySet())
		{
			Indices ind=new Indices();
			mainbuf.position(3*idx);
			ind.buf=mainbuf.slice();
			ind.buf.position(0);
			ind.tricount=cnt.getValue()[0];
			tex2buf.put(cnt.getKey(),ind);
			idx+=ind.tricount;
		}
		


		for(Triangle t:all)
		{
			if (!t.isUsed()) continue;
			for(int i=0;i<3;++i)
				if (!vstore.isUsed(t.getidx(i)))
					throw new RuntimeException("Triangle contains index which points to a presently non-used vertex! ("+t.getidx(i)+")");
			Indices ind=tex2buf.get(t.getTexture());
			ind.buf.put(t.getidx(0));
			ind.buf.put(t.getidx(1));
			ind.buf.put(t.getidx(2));
			//Log.i("fplan","Index nr #"+cnt+": "+t.getidx(0)+","+t.getidx(1)+","+t.getidx(2));
			ind.tricount+=1;

		}
		for(Entry<Texture,Indices> ent:tex2buf.entrySet())
			ent.getValue().buf.position(0);
		return tex2buf;
		
		*/
	}
	public void debugDump(Writer f) throws IOException {
		f.write("\"triangles\":[\n");
		int cnt=0;
		for(Triangle t:all)
		{
			if (!t.isUsed()) continue;
			if (cnt!=0)
				f.write(",\n");
			f.write("{");
			f.write("  \"nr\" : "+t.pointer+",\n");
			f.write("  \"vertices\" : [ ");
			for(int i=0;i<3;++i)
			{
				if (i!=0) f.write(",");
				f.write(""+t.getidx(i));
			}
			f.write(" ]\n");
			f.write("}");
			++cnt;
		}
		f.write("]\n");
	}
	public int getFreeTriangles() {
		// TODO: Check if this is inefficient?
		return free.size();
	}
}
