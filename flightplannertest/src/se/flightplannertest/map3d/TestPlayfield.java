package se.flightplannertest.map3d;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

import junit.framework.Assert;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Test;

import se.flightplanner.Project;
import se.flightplanner.Project.LatLon;
import se.flightplanner.Project.iMerc;
import se.flightplanner.map3d.ElevationStore;
import se.flightplanner.map3d.LodCalc;
import se.flightplanner.map3d.Playfield;
import se.flightplanner.map3d.Thing;
import se.flightplanner.map3d.ThingFactory;
import se.flightplanner.map3d.ThingIf;
import se.flightplanner.map3d.TriangleStore;
import se.flightplanner.map3d.Vertex;
import se.flightplanner.map3d.VertexStore;
import se.flightplanner.map3d.Stitcher;

public class TestPlayfield {

	public static void main(String[] args) throws FileNotFoundException, IOException {
		TestPlayfield pf=new TestPlayfield();
		pf.testPlayfieldVirtualThing();
	}
	@Test
	public void testPlayfieldVirtualThing() throws FileNotFoundException, IOException
	{
		final int pgap=13-5;
		final int parentsize=64<<pgap;
		iMerc p1=new iMerc(0,0);
		iMerc p2=new iMerc(parentsize*2,parentsize);
		VertexStore vstore=new VertexStore(100);
		ElevationStore estore=TestElevMap.getSampleEstore();
		TriangleStore tristore=new TriangleStore(100);
		LodCalc lc=new LodCalc(100, 10);
		final Mockery mock=new Mockery();
		final ThingIf parent=mock.mock(ThingIf.class,"parent");
		final ThingIf neighbor=mock.mock(ThingIf.class,"neighbor");
		final ThingIf[] children=new ThingIf[4];
		children[0]=mock.mock(ThingIf.class,"child0");
		children[1]=mock.mock(ThingIf.class,"child1");
		children[2]=mock.mock(ThingIf.class,"child2");
		children[3]=mock.mock(ThingIf.class,"child3");
		final ThingIf[] allmocks=new ThingIf[6];
		final iMerc observerpos=new iMerc(0,0);
		final short observerheight=1000;
		allmocks[0]=parent;
		allmocks[1]=neighbor;
		final ThingIf[] parents=new ThingIf[]{parent,neighbor};
		for(int i=0;i<4;++i)
			allmocks[i+2]=children[i];
		for(final ThingIf t:parents)
		{
	        mock.checking(new Expectations() {{
	        	allowing(t).bumpiness();will(returnValue(100.0f));
        		allowing(t).getZoomlevel();will(returnValue(5));
	        	allowing(t).isSubsumed();will(returnValue(false));
        		if (t==parent)
        		{
        			//the parent is close, to absolutely positively force a refine.
        			allowing(t).getDistance(observerpos,observerheight);will(returnValue(0.0f));
        		}
        		else
        		{
        			//make the non-parent be very very far away
        			allowing(t).getDistance(observerpos,observerheight);will(returnValue((float)(64<<13)));
        		}
        		
	        }});
		}
		int chcnt=0;
		for(final ThingIf t:children)
		{
			final int dx=chcnt%2;
			final int dy=chcnt/2;
			++chcnt;
			final int gap=13-6;
			final int childsize=64<<gap;
	        mock.checking(new Expectations() {{
	        	allowing(t).bumpiness();will(returnValue(0.0f));
	        	allowing(t).isSubsumed();will(returnValue(false));
        		allowing(t).getZoomlevel();will(returnValue(6));
    			allowing(t).getPos();will(returnValue(new iMerc(dx*childsize,dy*childsize)));
    			allowing(t).getDistance(observerpos,observerheight);will(returnValue((float)(2.0*childsize)));

	        }});
		}
		
		final int[] cnt=new int[1];
		ThingFactory thingf=new ThingFactory()
		{
			@Override
			public ThingIf createThing(VertexStore vstore, ElevationStore estore,
					int i, iMerc m,Stitcher st) {
				++cnt[0];
				if (cnt[0]-1==0) return parent;
				if (cnt[0]-1==1) return neighbor;
				if (cnt[0]-1<6)
					return children[cnt[0]-2];
				throw new RuntimeException("Created more things than expected");
			}
		};
        System.out.println("Bumpiness:"+parent.bumpiness());
        System.out.println("Unexpected:"+parent.getZoomlevel());
		
		Playfield play=new Playfield(p1,p2,vstore,tristore,lc,estore,thingf);
		Assert.assertEquals(2,cnt[0]);
		iMerc observer=new iMerc(0,0);
		
		play.changeLods(observer, (short)1000, vstore, estore);
		Assert.assertEquals(6,cnt[0]);
		mock.assertIsSatisfied();
	}
	
	
	@Test
	public void testPlayfieldSimpleIntegration() throws FileNotFoundException, IOException
	{
		iMerc p1=Project.latlon2imerc(new LatLon(59,17), 13);
		iMerc p2=Project.latlon2imerc(new LatLon(57,19), 13);
		VertexStore vstore=new VertexStore(100);
		ElevationStore estore=TestElevMap.getSampleEstore();
		TriangleStore tristore=new TriangleStore(100);
		LodCalc lc=new LodCalc(100, 10);
		ThingFactory thingf=new ThingFactory()
		{
			@Override
			public ThingIf createThing(VertexStore vstore, ElevationStore estore,
					int i, iMerc m,Stitcher st) {
				Thing t=new Thing(m,null,i,vstore,estore,st);
				return t;
			}
			
		};
		Playfield play=new Playfield(p1,p2,vstore,tristore,lc,estore,thingf);
		iMerc observer=Project.latlon2imerc(new LatLon(57.5,18.4), 13);
		play.changeLods(observer, (short)1000, vstore, estore);
		
		
	}
}
