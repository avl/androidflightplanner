package se.flightplannertest;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;

import junit.framework.Assert;

import org.junit.Test;

import se.flightplanner.vector.BoundingBox;
import se.flightplanner.vector.BspTree;
import se.flightplanner.vector.Vector;
import se.flightplanner.vector.BspTree.Item;

public class TestBspTree {
	@Test 
	public void testEmptyTree()
	{
		BspTree bsp=new BspTree(new Item[]{});
		Assert.assertEquals(0,bsp.findall(0,0,100,100).size());
		Assert.assertNull(bsp.find_item_dominating(new BoundingBox(0,0,100,100)));
	}
	@Test
	public void testFindall() {
		Item[] items=new Item[4];
		items[0]=makeitem("A", new Vector(0,0));
		items[1]=makeitem("B", new Vector(1,0));
		items[2]=makeitem("C", new Vector(2,0));
		items[3]=makeitem("D", new Vector(3,0));
		BspTree bsp=new BspTree(
			items,0,items.length,0);
		ArrayList<Item> res=new ArrayList<Item>();
		bsp.findall(0.5,-1,1.5,1,res);
		Assert.assertTrue(res.size()==1);
		Assert.assertEquals("B",res.get(0).payload());

		ArrayList<Item> res2=new ArrayList<Item>();
		bsp.findall(-0.5,-1,4.5,1,res2);
		Assert.assertTrue(res2.size()==4);
		Assert.assertEquals("B",res2.get(1).payload());
	}
	@Test public void testFindAllBruteForce()
	{
		Random rg=new Random(42);
		final int cnt=1000;
		Item[] vs=new Item[cnt];
		for(int i=0;i<cnt;++i)
		{
			final int ii=i;
			final Vector v=new Vector(rg.nextDouble()*1000,rg.nextDouble()*1000);
			//System.out.println("Generated "+v);
			vs[i]=new Item()
			{
				public Object payload() {
					return ii;
				}
				public Vector vec() {
					return v;
				}				
			};
		}
		BspTree bsp=new BspTree(vs);
		for(int i=0;i<4;++i)
		{
			Vector dx1=new Vector(rg.nextDouble()*2000-500,rg.nextDouble()*2000-500);
			Vector dx2=new Vector(rg.nextDouble()*2000-500,rg.nextDouble()*2000-500);
			BoundingBox bb=new BoundingBox(dx1,dx1.plus(dx2));
			//System.out.println("BB: "+bb);
			ArrayList<Item> was=bsp.findall(bb);
			ArrayList<Item> should=new ArrayList<Item>();
			for(int j=0;j<cnt;++j)
				if (bb.covers(vs[j].vec()))
				{
					should.add(vs[j]);
				}
			Assert.assertEquals(was.size(),should.size());
			Comparator<Item> cmp=new Comparator<Item>(){
				public int compare(Item arg0, Item arg1) {
					Vector v1=arg0.vec();
					Vector v2=arg1.vec();
					if (v1.getx()<v2.getx()) return -1;
					if (v1.getx()>v2.getx()) return +1;
					if (v1.gety()<v2.gety()) return -1;
					if (v1.gety()>v2.gety()) return +1;
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
				///System.out.println("Comparing "+was.get(j).vec()+" and "+should.get(j).vec());
				Assert.assertTrue(was.get(j).vec().almostEquals(should.get(j).vec(), 0.01));
			}
			
		}
	}

	@Test
	public void testFindall2() {
		Item[] items=new Item[4];
		items[0]=makeitem("A", new Vector(0,0));
		items[1]=makeitem("B", new Vector(1,0));
		items[2]=makeitem("C", new Vector(1,1));
		items[3]=makeitem("D", new Vector(0,1));
		BspTree bsp=new BspTree(
			items,0,items.length,0);
		ArrayList<Item> res=new ArrayList<Item>();
		bsp.findall(0.5,0.5,1.5,1.5,res);
		Assert.assertTrue(res.size()==1);
		Assert.assertEquals("C",res.get(0).payload());
	}
	@Test
	public void testFindall3() {
		Item[] items=new Item[4];
		items[0]=makeitem("A", new Vector(0,0));
		items[1]=makeitem("B", new Vector(1,0));
		items[2]=makeitem("C", new Vector(1,1));
		items[3]=makeitem("D", new Vector(0,1));
		BspTree bsp=new BspTree(
			items,0,items.length,0);
		ArrayList<Item> res=new ArrayList<Item>();
		bsp.findall(0.5,0.5,1.5,0.9,res);
		Assert.assertTrue(res.size()==0);
		Assert.assertEquals(4,bsp.findall(-1,-1,2,2).size());
	}
	
	private Item makeitem(final Object payload, final Vector vec) {
		Item temp=new Item()
		{
			@Override
			public Object payload() {
				// TODO Auto-generated method stub
				return payload;
			}
			@Override
			public Vector vec() {
				// TODO Auto-generated method stub
				return vec;
			}	
		};
		return temp;
	}
	@Test
	public void testBoundingBox()
	{
		BoundingBox b1=new BoundingBox(0,0,1,1);
		BoundingBox b2=new BoundingBox(0.5,0.5,1.5,1.5);
		BoundingBox b3=new BoundingBox(-0.5,-0.5,1.5,1.5);
		Assert.assertTrue(b1.overlaps(b2));
		Assert.assertTrue(b2.overlaps(b1));
		Assert.assertTrue(b3.covers(b1));
		Assert.assertTrue(b3.covers(b2));
		Assert.assertFalse(b1.covers(b3));
		Assert.assertFalse(b2.covers(b3));
	}

	@Test
	public void testFindall4() {
		Item[] items=new Item[4];
		items[0]=makeitem("A", new Vector(0,0));
		items[1]=makeitem("B", new Vector(0,1));
		items[2]=makeitem("C", new Vector(1,0));
		items[3]=makeitem("D", new Vector(1,1));
		BspTree bsp=new BspTree(
			items,0,items.length,0);
		//ArrayList<Item> res=new ArrayList<Item>();
		Item dom=bsp.find_item_dominating(
				new BoundingBox(-10,-10,-9,-9));
		Assert.assertEquals("A", dom.payload());

		Item dom2=bsp.find_item_dominating(
				new BoundingBox(-10,-10,10,10));
		Assert.assertEquals("C", dom2.payload());
		
		
		//System.out.println("Found item:"+dom2.payload());
		ArrayList<Item> overlapping=bsp.items_whose_dominating_area_overlaps(new BoundingBox(-10,-10,-9,-9));
		System.out.println(overlapping.get(0).payload());
		System.out.println(overlapping.get(1).payload());
		System.out.println(overlapping.get(2).payload());
		Assert.assertEquals(3, overlapping.size());
		Assert.assertEquals("C",overlapping.get(0).payload());
		Assert.assertEquals("B",overlapping.get(1).payload());
		Assert.assertEquals("A",overlapping.get(2).payload());
		
		ArrayList<Item> ov2=bsp.items_whose_dominating_area_overlaps(new BoundingBox(-10,-10,10,10));
		Assert.assertEquals(4,ov2.size());

		ArrayList<Item> ov3=bsp.items_whose_dominating_area_overlaps(new BoundingBox(-10,9,-9,10));
		Assert.assertEquals(2,ov3.size());
		
	}

	
}
