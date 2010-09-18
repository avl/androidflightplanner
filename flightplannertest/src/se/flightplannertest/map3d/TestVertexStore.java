package se.flightplannertest.map3d;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

import org.junit.Assert;
import org.junit.Test;

import se.flightplanner.Project.iMerc;
import se.flightplanner.map3d.Vertex;
import se.flightplanner.map3d.VertexStore;

public class TestVertexStore {

	@Test
	public void testVertexStore1()
	{
		Vertex va=new Vertex((short)0);
		Vertex vb=new Vertex((short)1);
		assertEquals(va,vb);
		VertexStore vstore=new VertexStore(5);
		iMerc im=new iMerc(256,256);
		Vertex v=vstore.obtain(im, (byte)13);
		assertEquals(v.getIndex(),0);
		assertEquals(v.getx(),256);
		assertEquals(v.gety(),256);
		Vertex v2=vstore.obtain(im, (byte)13);
		assertEquals(v,v2);
		vstore.decrement(v);
		assertEquals(v2.getimerc(),im);
		
		vstore.decrement(v2);
		
		Assert.assertFalse(v2.getimerc().equals(im));
	}
	@Test 
	public void testVertexStoreCapacity1()
	{
		VertexStore vstore=new VertexStore(5);
		for(int i=0;i<5;++i)
		{
			iMerc im=new iMerc(256+256*i,256);
			vstore.obtain(im, (byte)13);
		}
		boolean exception=false;
		try
		{
			vstore.obtain(new iMerc(0,0),(byte)13);
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
		VertexStore vstore=new VertexStore(5);
		Vertex v=null;
		for(int i=0;i<10;++i)
		{
			iMerc im=new iMerc(256,256);
			Vertex v2=vstore.obtain(im, (byte)13);
			if (v==null) v=v2;
			assertEquals(v,v2);
		}
	}
}
