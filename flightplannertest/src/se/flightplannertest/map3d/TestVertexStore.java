package se.flightplannertest.map3d;

import org.junit.Test;

import se.flightplanner.Project.iMerc;
import se.flightplanner.map3d.Vertex;
import se.flightplanner.map3d.VertexStore;

public class TestVertexStore {

	@Test
	public void testVertexStore1()
	{
		VertexStore vstore=new VertexStore(5);
		iMerc im=new iMerc(256,256);
		Vertex v=vstore.obtain(im, (byte)13);
		assert v.getIndex()==0;
		assert v.getx()==256;
		assert v.gety()==256;
		
		Vertex v2=vstore.obtain(im, (byte)13);
		assert v==v2;
		vstore.decrement(v);
		assert v2.getimerc().equals(im);
		
		vstore.decrement(v2);
		
		assert !v2.getimerc().equals(im);
	}
}
