package se.flightplannertest.map3d;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

import org.junit.Assert;
import org.junit.Test;

import se.flightplanner.Project.iMerc;
import se.flightplanner.map3d.Vertex;
import se.flightplanner.map3d.TerrainVertexStore;
import se.flightplanner.map3d.VertexStore3D;

public class TestVertexStore {

	@Test
	public void testVertexStore1()
	{
		Vertex va=new Vertex((short)0);
		Vertex vb=new Vertex((short)1);
		assertEquals(va,vb);
		VertexStore3D vs3d=new VertexStore3D(1000);
		TerrainVertexStore vstore=new TerrainVertexStore(5,0,vs3d);
		iMerc im=new iMerc(256,256);
		Vertex v=vstore.obtain(im, (byte)13,"debug1");
		assertEquals(v.getPointer(),0);
		assertEquals(v.getx(),256);
		assertEquals(v.gety(),256);
		Vertex v2=vstore.obtain(im, (byte)13,"debug2");
		assertEquals(v,v2);
		vstore.decrement(v);
		assertEquals(v2.getimerc(),im);
		
		vstore.decrement(v2);
		
		Assert.assertFalse(v2.isUsed());
	}
	@Test 
	public void testVertexStoreCapacity1()
	{
		VertexStore3D vs3d=new VertexStore3D(5);
		TerrainVertexStore vstore=new TerrainVertexStore(5,0,vs3d);
		for(int i=0;i<5;++i)
		{
			iMerc im=new iMerc(256+256*i,256);
			vstore.obtain(im, (byte)13,"debug3");
		}
		boolean exception=false;
		try
		{
			vstore.obtain(new iMerc(0,0),(byte)13,"debug4");
		}
		catch (RuntimeException e)
		{
			assertTrue(e.getMessage().contains("No more vertices"));
			assertFalse(e.getMessage().contains("No more vetrices"));
			exception=true;
		}
		assert(exception);
		
	}
	@Test 
	public void testVertexStoreCapacity2()
	{
		VertexStore3D vs3d=new VertexStore3D(1000);
		TerrainVertexStore vstore=new TerrainVertexStore(5,0,vs3d);
		Vertex v=null;
		for(int i=0;i<10;++i)
		{
			iMerc im=new iMerc(256,256);
			Vertex v2=vstore.obtain(im, (byte)13,"debug5");
			if (v==null) v=v2;
			assertEquals(v,v2);
		}
	}
}
