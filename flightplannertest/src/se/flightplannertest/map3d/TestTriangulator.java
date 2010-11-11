package se.flightplannertest.map3d;

import java.util.ArrayList;

import junit.framework.Assert;

import org.junit.Test;

import se.flightplanner.vector.Polygon;
import se.flightplanner.vector.PolygonTriangulator;
import se.flightplanner.vector.SimpleTriangle;
import se.flightplanner.vector.Vector;

public class TestTriangulator {
	
	@Test
	public void testTriangulator1()
	{
		Polygon p=new Polygon(
				new Vector[]{
					new Vector(0,0),	
					new Vector(1,0),	
					new Vector(1,1),	
					new Vector(0,1)	
				});
		Assert.assertFalse("Not outside!",p.is_inside(new Vector(-.5,.5)));
		Assert.assertFalse("Not outside!",p.is_inside(new Vector(1.1,.5)));
		System.out.println("Is inside:"+p.is_inside(new Vector(.5,.5)));
		Assert.assertTrue("Not inside!",!!p.is_inside(new Vector(.5,.5)));
		
		ArrayList<SimpleTriangle> expect=new ArrayList<SimpleTriangle>();
		expect.add(new SimpleTriangle(
				new Vector(0,0),
				new Vector(1,0),
				new Vector(1,1)));
		expect.add(new SimpleTriangle(
				new Vector(0,0),
				new Vector(1,1),
				new Vector(0,1)));
		
		doTestTriangularizationCase(p, expect);

	}
	@Test
	public void testTriangulator2()
	{
		Polygon p=new Polygon(
				new Vector[]{
					new Vector(0,0),	
					new Vector(1,0),	
					new Vector(1,1),	
					new Vector(0.875,0.125)	
				});
		ArrayList<SimpleTriangle> expect=new ArrayList<SimpleTriangle>();
		expect.add(new SimpleTriangle(
				new Vector(1,0),
				new Vector(1,1),
				new Vector(0.875,0.125)));
		expect.add(new SimpleTriangle(
				new Vector(1,0),
				new Vector(0.875,0.125),
				new Vector(0,0)));
		
		doTestTriangularizationCase(p, expect);

	}
	@Test
	public void testTriangulator3()
	{
		Polygon p=new Polygon(
				new Vector[]{
					new Vector(0,0),	
					new Vector(0.25,0),	
					new Vector(0.5,0),	
					new Vector(1,0),	
					new Vector(1,1),	
					new Vector(0.875,0.125)	
				});
		ArrayList<SimpleTriangle> expect=new ArrayList<SimpleTriangle>();
		expect.add(new SimpleTriangle(
				new Vector(1,0),
				new Vector(1,1),
				new Vector(0.875,0.125)));
		expect.add(new SimpleTriangle(
				new Vector(1,0),
				new Vector(0.875,0.125),
				new Vector(0.5,0)
				));
		expect.add(new SimpleTriangle(
				new Vector(0.875,0.125),
				new Vector(0.0,0.0),
				new Vector(0.25,0)
				));
		expect.add(new SimpleTriangle(
				new Vector(0.875,0.125),
				new Vector(0.25,0.0),
				new Vector(0.5,0)
				));
		
		doTestTriangularizationCase(p, expect);

	}

	private void doTestTriangularizationCase(Polygon p,
			ArrayList<SimpleTriangle> expect) {
		ArrayList<SimpleTriangle> out=new ArrayList<SimpleTriangle>();
		PolygonTriangulator.triangulate(p, out);
		if (expect.size()!=out.size())
		{
			System.out.println("----------------");
			for(SimpleTriangle t:out)
				System.out.println("Actual:"+t);
		}
			
		Assert.assertEquals(expect.size(), out.size());		
		for(int i=0;i<expect.size();++i)
		{
			for(int j=0;j<3;++j)
			{
				Vector should=expect.get(i).get(j);
				Vector v=out.get(i).get(j);
				if (!should.equals(v))
				{
					System.out.println("Polygon #"+i+" vertex #"+j+": Should:"+should+", is:"+v);
					throw new RuntimeException("Failure");
				}					
			}
		}
	}

}
