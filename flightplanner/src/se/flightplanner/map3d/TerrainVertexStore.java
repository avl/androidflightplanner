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
import java.util.Random;

import android.util.Log;

import se.flightplanner.Project.Merc;
import se.flightplanner.Project.iMerc;

public class TerrainVertexStore {

	HashMap<iMerc,Vertex> used;
	private Random random=new Random();
	int texture_zoomlevel;	
	VertexStore3D vstore3d;
	
	public HashMap<Short,Vertex> dbgGetVertices(){
		HashMap<Short,Vertex> h=new HashMap<Short,Vertex>();
		for(Vertex v:used.values())
		{
			h.put(new Short(v.getPointer()), v);
		}
		return h;
	}
	public HashMap<iMerc,Vertex> dbgGetVerticesMap(){
		HashMap<iMerc,Vertex> h=new HashMap<iMerc,Vertex>();
		for(Vertex v:used.values())
		{
			h.put(v.getimerc(),v);
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
		boolean removed=vstore3d.decrement(v);
		if (removed)
			used.remove(v.getimerc());
		return removed;
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
		Vertex shiny=vstore3d.alloc();
		used.put(p, shiny);
		
		int texture_shift=(13-texture_zoomlevel);
		int tx=p.getX()>>(texture_shift);
		int ty=p.getY()>>(texture_shift);
		tx&=511;				
		ty&=511;
		if (tx>256)
			tx=512-tx;
		if (ty>256)
			ty=512-ty;
		float u=tx/256.0f;
		float v=ty/256.0f;				
		
		shiny.deploy(p.getX(),p.getY(),zoomlevel,what,u,v);
		//System.out.println("Obtained new "+shiny);
		return shiny;
	}
	public Vertex obtaindbg(iMerc iMerc, byte b) {
		Vertex dbg=new Vertex((short)-1);
		dbg.deploy(iMerc.getX(),iMerc.getY(),b,"debug",0,0);
		return dbg;
	}		
	public TerrainVertexStore(int capacity,int texture_zoomlevel,VertexStore3D vs3d)
	{
		this.vstore3d=vs3d;
		this.texture_zoomlevel=texture_zoomlevel;
		used=new HashMap<iMerc,Vertex>();
		
	}
	public int usedVertices() {
		return used.size();
	}
	static class VertAndColor
	{
		public FloatBuffer vertices;
		public FloatBuffer texcoords;
		public ByteBuffer colors;
	}
	public void debugDump(Writer f) throws IOException {
		this.vstore3d.debugDump(f);
	}
	public int getFreeVertices() {
		return vstore3d.getFreeVertices();
	}
	
	
}
