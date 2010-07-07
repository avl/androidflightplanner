package se.flightplannertest;

import java.util.ArrayList;

import junit.framework.Assert;

import org.junit.Test;
import se.flightplanner.vector.BBTree;
import se.flightplanner.vector.BoundingBox;
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
