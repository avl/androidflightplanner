package se.flightplannertest.map3d;

import junit.framework.Assert;

import org.junit.Test;

import se.flightplanner.map3d.LodCalc;

public class TestLodcalc {

	@Test
	public void testLodcalc()
	{
		LodCalc c=new LodCalc(100, 10);
		float refine;
		
		//500 pixels away, there's an object with an unevenness of
		//100 pixels. We assume 90 deg FOV, which means that the unevenness
		//takes up 20% = 20 pixels. The tolerance is 10 pixels.		
		refine=c.needRefining(100, 500);
		System.out.println("Refine:"+refine);
		Assert.assertTrue(refine>=1.0);

		refine=c.needRefining(1, 1000);
		System.out.println("Refine:"+refine);
		Assert.assertTrue(refine<=0.0);

		refine=c.needRefining(100, 1100); //100/1100*100 = 9 pixels
		System.out.println("Refine:"+refine);
		Assert.assertTrue(refine<1.0);
		Assert.assertTrue(refine>0.5);
		/*
		for(int i=0;i<3;++i)
		{
			int dist=5000;
			int size=10+100*i;
			while(dist>0)
			{
				dist-=50;				
				refine=c.needRefining(size, dist); //100/1100*100 = 9 pixels
				System.out.println("Size "+size+" Dist "+dist+", refine=:"+refine);
				
			}
		}
		*/
	}
	
}
