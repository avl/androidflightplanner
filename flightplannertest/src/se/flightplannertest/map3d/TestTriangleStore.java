package se.flightplannertest.map3d;

import org.junit.Assert;
import org.junit.Test;

import se.flightplanner.map3d.Triangle;
import se.flightplanner.map3d.TriangleStore;

public class TestTriangleStore {

	@Test
	public void testTriStore1()
	{
		TriangleStore tristore=new TriangleStore(2);
		Triangle tri=tristore.alloc();
		Assert.assertEquals(0,tri.getPointer());
		Triangle tri2=tristore.alloc();
		Assert.assertEquals(1,tri2.getPointer());
		boolean gotexception=false;
		try
		{
			tristore.alloc();
		}
		catch(RuntimeException thr)
		{
			if (thr.getMessage().contains("Out of triangles"))
				gotexception=true;
		}
		Assert.assertTrue(gotexception);
		
		Assert.assertTrue(tristore.dbgGetTriangles().size()==2);
		tristore.release(tri);
		tristore.release(tri2);
		Assert.assertTrue(tristore.dbgGetTriangles().size()==0);
	}
}
