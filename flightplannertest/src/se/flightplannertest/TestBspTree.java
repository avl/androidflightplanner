package se.flightplannertest;

import static org.junit.Assert.*;

import java.util.ArrayList;

import junit.framework.Assert;

import org.junit.Test;

import se.flightplanner.vector.BspTree;
import se.flightplanner.vector.Vector;
import se.flightplanner.vector.BspTree.Item;

public class TestBspTree {

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

}
