package se.flightplannertest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;

import junit.framework.Assert;

import org.junit.Test;
import se.flightplanner.vector.BBTree;
import se.flightplanner.vector.BoundingBox;
import se.flightplanner.vector.BspTree;
import se.flightplanner.vector.Vector;
import se.flightplanner.vector.BBTree.Item;

public class TestBBTree {

	@Test
	public void testBBTree1()
	{
		ArrayList<Item> items=new ArrayList<Item>();
		additem(items,new BoundingBox(0,0,1,1),"A");
		additem(items,new BoundingBox(10,10,11,11),"B");
		BBTree bbt=new BBTree(items,0.01);		
		ArrayList<Item> found1=bbt.overlapping(new BoundingBox(0.25,0.25,0.75,0.75));
		Assert.assertEquals(1,found1.size());
		Assert.assertEquals("A", found1.get(0).payload());

		ArrayList<Item> found2=bbt.overlapping(new BoundingBox(0.25,0.25,0.75,0.75));
		Assert.assertEquals(1,found2.size());
		Assert.assertEquals("A", found2.get(0).payload());

		ArrayList<Item> found3=bbt.overlapping(new BoundingBox(5,5,9,9));
		Assert.assertEquals(0,found3.size());

		ArrayList<Item> found4=bbt.overlapping(new BoundingBox(5,5,11,11));
		Assert.assertEquals(1,found4.size());		
		Assert.assertEquals("B", found4.get(0).payload());

		ArrayList<Item> found5=bbt.overlapping(new BoundingBox(-5,-5,11,11));
		Assert.assertEquals(2,found5.size());		
		Assert.assertEquals("B", found5.get(0).payload());
		Assert.assertEquals("A", found5.get(1).payload());		
	}
	@Test public void testFindAllBruteForce()
	{
		Random rg=new Random(42);
		final int cnt=4000;
		ArrayList<Item> vs=new ArrayList<Item>();
		for(int i=0;i<cnt;++i)
		{
			final int ii=i;
			Vector dx1=new Vector(rg.nextDouble()*2000-500,rg.nextDouble()*2000-500);
			Vector dx2=new Vector(rg.nextDouble()*2000-500,rg.nextDouble()*2000-500);
			final BoundingBox bb=new BoundingBox(dx1,dx1.plus(dx2));
			//System.out.println("Generated "+v);
			vs.add(new Item() {				
				@Override
				public Object payload() {
					return bb;
				}				
				@Override
				public BoundingBox bb() {
					// TODO Auto-generated method stub
					return bb;
				}
			});
		}
		BBTree bbt=new BBTree(vs,0.1);
		for(int i=0;i<100;++i)
		{
			Vector dx1=new Vector(rg.nextDouble()*2000-500,rg.nextDouble()*2000-500);
			Vector dx2=new Vector(rg.nextDouble()*2000-500,rg.nextDouble()*2000-500);
			BoundingBox bb=new BoundingBox(dx1,dx1.plus(dx2));
			System.out.println("BB: "+bb);
			
			ArrayList<Item> was=bbt.overlapping(bb);
			ArrayList<Item> should=new ArrayList<Item>();
			for(int j=0;j<cnt;++j)
				if (bb.overlaps(vs.get(j).bb()))
				{
					should.add(vs.get(j));
				}
			Assert.assertEquals(was.size(),should.size());
			Comparator<Item> cmp=new Comparator<Item>(){
				public int compare(Item arg0, Item arg1) {
					BoundingBox bb1=arg0.bb();
					BoundingBox bb2=arg1.bb();
					Vector[] v1s=new Vector[]{bb1.lowerleft(),bb1.upperright()};
					Vector[] v2s=new Vector[]{bb2.lowerleft(),bb2.upperright()};
					for(int i=0;i<2;++i)
					{
						Vector v1=v1s[i];
						Vector v2=v2s[i];
						if (v1.getx()<v2.getx()) return -1;
						if (v1.getx()>v2.getx()) return +1;
						if (v1.gety()<v2.gety()) return -1;
						if (v1.gety()>v2.gety()) return +1;
					}
					return 0;
				}};
			Collections.sort(should,cmp);
			Collections.sort(was,cmp);
			for(Item item:should)
			{
				//System.out.println("Should: "+item.vec());
			}
			for(Item item:was)
			{
				//System.out.println("Was: "+item.vec());
			}
			for(int j=0;j<was.size();++j)
			{
				//System.out.println("Comparing "+was.get(j).bb()+" and "+should.get(j).bb());
				Assert.assertTrue(was.get(j).bb().almostEquals(should.get(j).bb(), 0.01));
			}
			
		}
	}


	private void additem(ArrayList<Item> items, final BoundingBox boundingBox,
			final String string) {
		items.add(new Item()
			{
				@Override
				public BoundingBox bb() {
					return boundingBox;
				}
				@Override
				public Object payload() {
					return string;
				}				
			}
		);
		
	}
}
