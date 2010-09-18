package se.flightplannertest.map3d;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

import junit.framework.Assert;

import org.junit.Test;

import se.flightplanner.Project.iMerc;
import se.flightplanner.map3d.ElevationStore;
import se.flightplanner.map3d.Stitcher;
import se.flightplanner.map3d.Thing;
import se.flightplanner.map3d.ThingIf;
import se.flightplanner.map3d.Triangle;
import se.flightplanner.map3d.TriangleStore;
import se.flightplanner.map3d.Vertex;
import se.flightplanner.map3d.VertexStore;

public class TestThing {

	abstract static class MyStitcher implements Stitcher
	{
	}
	@Test
	public void testThingTriangulateComplex() throws FileNotFoundException, IOException
	{
		final HashSet<Vertex> stitchrequired=new HashSet<Vertex>();
		final HashSet<iMerc> stitchrequiredmerc=new HashSet<iMerc>();
		Stitcher st=new MyStitcher()
		{
			@Override
			public void stitch(Vertex v, int level, boolean unstitch) {
				stitchrequired.add(v);
				stitchrequiredmerc.add(v.getimerc());
			}		
		};
		VertexStore vstore=new VertexStore(100);
		ElevationStore estore=TestElevMap.getSampleEstore();
		int size13=64;
		ThingIf t1=new Thing(
				new iMerc(0,0),null,12,vstore,estore,st
				);
		ThingIf t2=new Thing(
				new iMerc(2*size13,size13),null,13,vstore,estore,st
				); //will require t1 to have stitches!
		Assert.assertTrue(stitchrequiredmerc.contains(new iMerc(128,64)));
		for(Vertex s : stitchrequired)
		{
			t1.shareVertex(vstore, s, true);
		}
						
		TriangleStore tristore=new TriangleStore(10);
		t1.triangulate(tristore);
		ArrayList<Triangle> tris=tristore.dbgGetTriangles();
		HashMap<Short,Vertex> map=vstore.dbgGetVertices();
		Assert.assertEquals(5,tris.size());
		System.out.println("Number of vertices in use: "+map.size());
		System.out.println("First idx: "+tris.get(0).getidx(0));
		for(Entry<Short,Vertex> v : map.entrySet())
		{
			System.out.println("Vertex: "+v.getValue()+" idx "+v.getKey());
		}
		System.out.println("Get: "+map.get((short)7));
		
		boolean seen[]=new boolean[]{false,false,false,false,false};
		for(int i=0;i<5;++i)
		{
			iMerc p0=map.get(tris.get(i).getidx(0)).getimerc();
			iMerc p1=map.get(tris.get(i).getidx(1)).getimerc();
			iMerc p2=map.get(tris.get(i).getidx(2)).getimerc();
			System.out.println("Tri: "+p0.x+","+p0.y+" "+p1.x+","+p1.y+" "+p2.x+","+p2.y);
			if ((p0.x==64) &&
					(p0.y==64) &&
					(p1.x==0) &&
					(p1.y==128) &&
					(p2.x==128) &&
					(p2.y==128)) seen[0]=true;
			if ((p0.x==64) &&
					(p0.y==64) &&
					(p1.x==128) &&
					(p1.y==128) &&
					(p2.x==128) &&
					(p2.y==64)) seen[1]=true;
			if ((p0.x==64) &&
					(p0.y==64) &&
					(p1.x==128) &&
					(p1.y==64) &&
					(p2.x==128) &&
					(p2.y==0)) seen[2]=true;
			if ((p0.x==64) &&
					(p0.y==64) &&
					(p1.x==128) &&
					(p1.y==0) &&
					(p2.x==0) &&
					(p2.y==0)) seen[3]=true;
			if ((p0.x==64) &&
					(p0.y==64) &&
					(p1.x==0) &&
					(p1.y==0) &&
					(p2.x==0) &&
					(p2.y==128)) seen[4]=true;
		}
		for(int i=0;i<seen.length;++i)
			Assert.assertTrue(seen[i]);
				
	}	
	
	@Test
	public void testThingTriangulateSimple() throws FileNotFoundException, IOException
	{
		Stitcher st=new MyStitcher()
		{
			@Override
			public void stitch(Vertex v, int level, boolean unstitch) {
				// TODO Auto-generated method stub
				
			}		
		};
		VertexStore vstore=new VertexStore(100);
		ElevationStore estore=TestElevMap.getSampleEstore();
		int size=64;
		ThingIf t=new Thing(
				new iMerc(256,256),null,13,vstore,estore,st
				);
		TriangleStore tristore=new TriangleStore(10);
		t.triangulate(tristore);
		ArrayList<Triangle> tris=tristore.dbgGetTriangles();
		Assert.assertEquals(2,tris.size());
		Assert.assertTrue(tris.get(0).getidx(0)==0);
		Assert.assertTrue(tris.get(0).getidx(1)==2);
		Assert.assertTrue(tris.get(0).getidx(2)==1);
		Assert.assertTrue(tris.get(1).getidx(0)==2);
		Assert.assertTrue(tris.get(1).getidx(1)==3);
		Assert.assertTrue(tris.get(1).getidx(2)==1);
	}

	
	
	@Test
	public void testThingSubsume() throws FileNotFoundException, IOException
	{
		VertexStore vstore=new VertexStore(100);
		final HashSet<Vertex> stitched=new HashSet<Vertex>();
		final HashSet<Vertex> unstitched=new HashSet<Vertex>();
		iMerc[] ims=new iMerc[]{
				new iMerc(64,0),new iMerc(0,64),new iMerc(128,64),new iMerc(64,128)
		};
		for(iMerc im:ims)
		{
			stitched.add(vstore.obtaindbg(im,(byte)13));
			unstitched.add(vstore.obtaindbg(im,(byte)13));
		}
		Stitcher st=new MyStitcher()
		{
			@Override
			public void stitch(Vertex v, int level, boolean stitch) {
				if (!stitch)
					unstitched.remove(v);
				else
					stitched.remove(v);
			}		
		};
		ElevationStore estore=TestElevMap.getSampleEstore();
		int zoomlevel=12;
		int size=64;
		ThingIf t=new Thing(
				new iMerc(0,0),null,zoomlevel,vstore,estore,st
				);
		Assert.assertTrue(t.isCorner(vstore.obtaindbg(new iMerc(0,0), (byte)zoomlevel)));
		Assert.assertTrue(t.isCorner(vstore.obtaindbg(new iMerc(128,0), (byte)zoomlevel)));
		Assert.assertTrue(t.getCorner(0).dbgUsage()==1);
		
		ArrayList<ThingIf> newThings=new ArrayList<ThingIf>();
		t.subsume(newThings, vstore, st, estore);
		Assert.assertTrue(t.getCorner(0).dbgUsage()==2);
		Assert.assertTrue(t.getCorner(1).dbgUsage()==2);
		
		Assert.assertTrue(stitched.size()==0); //all have occurred

		Assert.assertTrue(newThings.get(0).isCorner(vstore.obtaindbg(new iMerc(0,0), (byte)zoomlevel)));
		Assert.assertTrue(newThings.get(0).isCorner(vstore.obtaindbg(new iMerc(64,0), (byte)zoomlevel)));
		Assert.assertTrue(!newThings.get(0).isCorner(vstore.obtaindbg(new iMerc(128,0), (byte)zoomlevel)));
		Assert.assertTrue(t.isSubsumed());
		t.unsubsume(vstore,st);
		Assert.assertTrue(t.getCorner(0).dbgUsage()==1);
		Assert.assertFalse(t.isSubsumed());		
		HashSet<Vertex> ns=new HashSet<Vertex>();
		t.release(ns,vstore,st);
		Assert.assertEquals(0,vstore.usedVertices());
	}
	@Test
	public void testThingBasic() throws FileNotFoundException, IOException
	{
		Stitcher st=new MyStitcher()
		{
			@Override
			public void stitch(Vertex v, int level, boolean unstitch) {
				
			}		
		};
		VertexStore vstore=new VertexStore(100);
		ElevationStore estore=TestElevMap.getSampleEstore();
		int size=64;
		ThingIf t=new Thing(
				new iMerc(256,256),null,13,vstore,estore,st
				);
		
		Assert.assertTrue(t.getCorner(0).dbgUsage()==1);
		Assert.assertTrue(t.getCorner(3).dbgUsage()==1);
		Assert.assertTrue(t.getPos().x==256);
		Assert.assertTrue(t.getPos().y==256);
		Assert.assertFalse(t.isSubsumed());
		Vertex v=vstore.obtaindbg(new iMerc(256,256),(byte)5);
		Assert.assertTrue(t.isCorner(v));
		v=vstore.obtaindbg(new iMerc(256+size,256),(byte)5);
		Assert.assertTrue(t.isCorner(v));
		v=vstore.obtaindbg(new iMerc(256,256+size),(byte)5);
		Assert.assertTrue(t.isCorner(v));
		v=vstore.obtaindbg(new iMerc(256+2*size,256),(byte)5);
		Assert.assertFalse(t.isCorner(v));

		
		v=vstore.obtaindbg(new iMerc(256+32,256),(byte)5);
		Assert.assertEquals(t.getSide(v),2);
		v=vstore.obtaindbg(new iMerc(256,256+32),(byte)5);
		Assert.assertEquals(t.getSide(v),3);
		v=vstore.obtaindbg(new iMerc(256+32,256+size),(byte)5);
		//System.out.println("side: "+t.getSide(v));
		Assert.assertEquals(t.getSide(v),0);
		v=vstore.obtaindbg(new iMerc(256+size,256+32),(byte)5);
		Assert.assertEquals(t.getSide(v),1);

		float dist=t.getDistance(new iMerc(512,256), 0);
		//System.out.println("Distance: "+dist);
		
		Vertex ulcorn=t.getCorner(0);
		Assert.assertEquals(ulcorn.dbgUsage(),1);
		HashSet<Vertex> stitched=new HashSet<Vertex>();
		t.release(stitched, vstore, st);
		Assert.assertEquals(0,stitched.size()); //None of the vertices of the released thing need to be stitched into the things parent (mostly since it has no parent)
		Assert.assertEquals(0,ulcorn.dbgUsage()); //All of the vertices are unallocated
		
		
	}
}
