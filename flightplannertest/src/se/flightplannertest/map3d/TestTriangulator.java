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
	
	
	@Test
	public void testTriangulator4()
	{
		Polygon p=new Polygon(
				new Vector[]{
						new Vector(1154674.000000,605505.978995),
						new Vector(1156665.000000,605897.016690),
						new Vector(1159237.000000,612689.020044),
						new Vector(1157453.000000,615867.996373),
						new Vector(1154385.000000,616134.991650),
						new Vector(1154385.000000,617657.021489),
						new Vector(1151957.000000,617657.021489),
						new Vector(1151957.000000,616802.003594),
						new Vector(1151666.000000,616802.003594),
						new Vector(1151666.000000,616326.013745),
						new Vector(1151957.000000,616326.013745),
						new Vector(1151957.000000,615468.010725),
						new Vector(1153171.000000,615468.010725),
						new Vector(1153220.000000,614894.003430),
						new Vector(1152152.000000,614703.018669),
						new Vector(1151666.000000,614128.007440),
						new Vector(1152152.000000,611146.989521),
						new Vector(1154480.000000,611146.989521),
						new Vector(1154092.000000,610019.017214),
						new Vector(1154868.000000,607898.996289)	
				});
		
		doTestTriangularizationCase(p, null);
	}
	private void doTestTriangularizationCase(Polygon p,
			ArrayList<SimpleTriangle> expect) {
		ArrayList<SimpleTriangle> out=new ArrayList<SimpleTriangle>();
		PolygonTriangulator.triangulate(p, out);
		if (expect==null)
			return;
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
