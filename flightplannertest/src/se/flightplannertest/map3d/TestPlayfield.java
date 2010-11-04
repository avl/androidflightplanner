package se.flightplannertest.map3d;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

import junit.framework.Assert;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Test;

import se.flightplanner.Project;
import se.flightplanner.Project.LatLon;
import se.flightplanner.Project.iMerc;
import se.flightplanner.map3d.ElevationStoreIf;
import se.flightplanner.map3d.LodCalc;
import se.flightplanner.map3d.Playfield;
import se.flightplanner.map3d.TextureStore;
import se.flightplanner.map3d.Thing;
import se.flightplanner.map3d.ThingFactory;
import se.flightplanner.map3d.ThingIf;
import se.flightplanner.map3d.TriangleStore;
import se.flightplanner.map3d.Vertex;
import se.flightplanner.map3d.VertexStore;
import se.flightplanner.map3d.Stitcher;
import se.flightplanner.map3d.ElevationStore.Elev;
import se.flightplanner.map3d.TriangleStore.DbgTriangle2D;

public class TestPlayfield {

	public static void main(String[] args) throws FileNotFoundException, IOException {
		TestPlayfield pf=new TestPlayfield();
		pf.testPlayfieldSimpleIntegration2b();
	}
	//@Test
	public void testPlayfieldVirtualThing() throws FileNotFoundException, IOException
	{
		final int pgap=13-5;
		final int parentsize=64<<pgap;
		iMerc p1=new iMerc(0,0);
		iMerc p2=new iMerc(parentsize*2,parentsize);
		final VertexStore vstore=new VertexStore(100,0);
		final ElevationStoreIf estore=TestElevMap.getSampleEstore();
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
		
		final int[] cnt=new int[1];
		ThingFactory thingf=new ThingFactory()
		{
			@Override
			public ThingIf createThing(VertexStore vstore, TextureStore tstore,ElevationStoreIf estore,
					int i, iMerc m,Stitcher st) {
				++cnt[0];
				if (cnt[0]-1==0) return parent;
				if (cnt[0]-1==1) return neighbor;
				if (cnt[0]-1<6)
					return children[cnt[0]-2];
				throw new RuntimeException("Created more things than expected");
			}
		};
		
		
		final ThingIf[] parents=new ThingIf[]{parent,neighbor};
		for(int i=0;i<4;++i)
			allmocks[i+2]=children[i];
		for(final ThingIf t:parents)
		{
	        mock.checking(new Expectations() {{
	        	allowing(t).bumpiness();will(returnValue(100.0f));
        		allowing(t).getZoomlevel();will(returnValue(5));
	        	allowing(t).isSubsumed();will(returnValue(false));
	        	allowing(t).adjustRefine(1.0f);
	        	allowing(t).getRefine();will(returnValue(1.0f));
	        	ArrayList<ThingIf> cr=new ArrayList<ThingIf>();
	        	for(ThingIf t:children)
	        		cr.add(t);
	        	allowing(t).getAllChildren();will(returnValue(cr));
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
	        	allowing(t).adjustRefine(1.0f);
        		allowing(t).getZoomlevel();will(returnValue(6));
    			allowing(t).getPos();will(returnValue(new iMerc(dx*childsize,dy*childsize)));
    			allowing(t).getDistance(observerpos,observerheight);will(returnValue((float)(2.0*childsize)));
	        }});
		}
		TextureStore tstore=null;
		final Playfield play=new Playfield(p1,p2,vstore,tstore,tristore,estore,thingf);
		
		for(final ThingIf t:parents)
		{
	        mock.checking(new Expectations() {{
	        	allowing(t).subsume(new ArrayList<ThingIf>(), vstore, play, estore);
	        }});
		}

		
		Assert.assertEquals(2,cnt[0]);
		iMerc observer=new iMerc(0,0);
		
		play.changeLods(observer, (short)1000, vstore, estore,lc,0);
		mock.assertIsSatisfied();
	}
	
	
	//@Test
	public void testPlayfieldSimpleIntegration() throws FileNotFoundException, IOException
	{
		iMerc p1=Project.latlon2imerc(new LatLon(59,17), 13);
		iMerc p2=Project.latlon2imerc(new LatLon(57,19), 13);
		VertexStore vstore=new VertexStore(1000,0);
		ElevationStoreIf estore=TestElevMap.getSampleEstore();
		TriangleStore tristore=new TriangleStore(1000);
		LodCalc lc=new LodCalc(100, 1000);
		ThingFactory thingf=new ThingFactory()
		{
			@Override
			public ThingIf createThing(VertexStore vstore, TextureStore tstore,ElevationStoreIf estore,
					int i, iMerc m,Stitcher st) {
				Thing t=new Thing(m,null,i,vstore,estore,st);
				return t;
			}
			
		};
		Playfield play=new Playfield(p1,p2,vstore,null,tristore,estore,thingf);
		iMerc observer=Project.latlon2imerc(new LatLon(57.5,18.4), 13);
		play.changeLods(observer, (short)1000, vstore, estore, lc, 0);
	}
	//@Test
	public void testPlayfieldSimpleIntegration2() throws FileNotFoundException, IOException
	{
		iMerc p1=new iMerc(0,0);
		iMerc p2=new iMerc(16384,16384);
		VertexStore vstore=new VertexStore(1000,0);
		ElevationStoreIf estore=TestElevMap.getSampleEstore();
		TriangleStore tristore=new TriangleStore(1000);

		ThingFactory thingf=new ThingFactory()
		{
			@Override
			public ThingIf createThing(VertexStore vstore, TextureStore tstore,ElevationStoreIf estore,
					int i, iMerc m,Stitcher st) {
				Thing t=new Thing(m,null,i,vstore,estore,st);
				return t;
			}
			
		};
		Playfield play=new Playfield(p1,p2,vstore,null,tristore,estore,thingf);
		Assert.assertTrue(play.dbgGetThing(new iMerc(0,0), 5)!=null);
		Assert.assertTrue(play.dbgGetThing(new iMerc(16384,0), 5)==null);
		Assert.assertTrue(play.dbgGetThing(new iMerc(8192,8192), 5)==null);
		Assert.assertTrue(play.dbgGetThing(new iMerc(8192,8192), 6)==null);
		
		play.prepareForRender();
		Assert.assertTrue(vstore.dbgGetIMercSet().contains(new iMerc(0,0)));
		Assert.assertTrue(vstore.dbgGetIMercSet().contains(new iMerc(16384,0)));
		Assert.assertTrue(vstore.dbgGetIMercSet().contains(new iMerc(0,16384)));
		Assert.assertTrue(vstore.dbgGetIMercSet().contains(new iMerc(16384,16384)));
		HashSet<DbgTriangle2D> tris=tristore.dbgGetTriangles2D(vstore.dbgGetVertices());
		Assert.assertTrue(tris.contains(new DbgTriangle2D(
				0,0,
				0,16384,
				16384,0)));
		Assert.assertTrue(tris.contains(new DbgTriangle2D(
				0,16384,
				16384,16384,
				16384,0)));
		
		play.explicitSubsume(new iMerc(0,0),5,vstore, estore, true);
		play.prepareForRender();
		tris=tristore.dbgGetTriangles2D(vstore.dbgGetVertices());
		Assert.assertFalse(tris.contains(new DbgTriangle2D( //Big tri is subsumed
				0,16384,
				16384,16384,
				16384,0)));
		Assert.assertTrue(tris.contains(new DbgTriangle2D(
				0,8192,
				8192,8192,
				8192,0)));
		Assert.assertTrue(tris.contains(new DbgTriangle2D(
				0,0,
				0,8192,
				8192,0)));
		
		Assert.assertEquals(8,tris.size());
		Assert.assertTrue(vstore.dbgGetIMercSet().contains(new iMerc(0,0)));
		Assert.assertTrue(vstore.dbgGetIMercSet().contains(new iMerc(8192,8192)));
		Assert.assertTrue(vstore.dbgGetIMercSet().contains(new iMerc(0,8192)));
		Assert.assertTrue(vstore.dbgGetIMercSet().contains(new iMerc(16384,16384)));

		Assert.assertTrue(play.dbgGetThing(new iMerc(8192,8192), 5)==null);
		Assert.assertTrue(play.dbgGetThing(new iMerc(8192,8192), 6)!=null);
		

		
		play.explicitSubsume(new iMerc(0,0), 6, vstore, estore, true);
		play.prepareForRender();
		Assert.assertTrue(play.dbgGetThing(new iMerc(4096,4096), 7)!=null);
		Assert.assertTrue(vstore.dbgGetIMercSet().contains(new iMerc(4096,4096)));
		Assert.assertTrue(vstore.dbgGetIMercSet().contains(new iMerc(0,4096)));
		Assert.assertTrue(vstore.dbgGetIMercSet().contains(new iMerc(8192,4096)));
		Assert.assertTrue(vstore.dbgGetIMercSet().contains(new iMerc(8192+4096,4096))); //"Center" vertex of right box (needed because of how we do stitching)
		play.prepareForRender();
		tris=tristore.dbgGetTriangles2D(vstore.dbgGetVertices());
		//for(DbgTriangle2D t:tris)
		//	System.out.println("Triangle: "+t);
		DbgTriangle2D lt=new DbgTriangle2D(
				8192,4096,
				8192+4096,4096,
				8192,0);
		//System.out.println("Looking for tri: "+lt);
		Assert.assertTrue(tris.contains(lt));

		Assert.assertTrue(tris.contains(new DbgTriangle2D(
				8192,0,
				8192+4096,4096,
				16384,0				
				)));
		Assert.assertTrue(tris.contains(new DbgTriangle2D(
				8192+4096,4096,
				16384,8192,				
				16384,0
				)));
		
		play.explicitSubsume(new iMerc(0,0), 6, vstore, estore, false);
		
	}
	@Test
	public void testPlayfieldSimpleIntegration2b() throws FileNotFoundException, IOException
	{
		iMerc p1=new iMerc(0,0);
		iMerc p2=new iMerc(16384,16384);
		Mockery mock=new Mockery();
		VertexStore vstore=new VertexStore(1000,0);
			
		TriangleStore tristore=new TriangleStore(1000);

		ThingFactory thingf=new ThingFactory()
		{
			@Override
			public ThingIf createThing(VertexStore vstore, TextureStore tstore,ElevationStoreIf estore,
					int i, iMerc m,Stitcher st) {
				Thing t=new Thing(m,null,i,vstore,estore,st);
				return t;
			}
			
		};
		
		final ElevationStoreIf estore=new ElevationStoreIf()
		{
			@Override
			public Elev get(iMerc pos, int hlevel) {
				if (hlevel<=-1) return new Elev((short)0,(short)1000);
				if (pos.x<4096) return new Elev((short)0,(short)500);
				return new Elev((short)500,(short)1000);
			}			
		};
		
		Playfield play=new Playfield(p1,p2,vstore,null,tristore,estore,thingf);

		Thing t=(Thing)play.dbgGetThing(new iMerc(0,0), 5);
		Assert.assertEquals((float)t.calcElevImpl(new iMerc(0,0), 1000,0,1000,0),1000.0f,5f);
		Assert.assertEquals((float)t.calcElevImpl(new iMerc(0,8192), 1000,0,1000,0),1000.0f,5f);
		Assert.assertEquals((float)t.calcElevImpl(new iMerc(8192,0), 1000,0,1000,0),500.0f,5f);
		Assert.assertEquals((float)t.calcElevImpl(new iMerc(8192,8192), 1000,0,1000,0),500.0f,5f);
		Assert.assertEquals((float)t.calcElevImpl(new iMerc(8192,8192), 1000,0,1000,0),500.0f,5f);
		Assert.assertEquals((float)t.calcElevImpl(new iMerc(16384,8192), 1000,0,1000,0),0.0f,5f);

		Assert.assertEquals((float)t.calcElevImpl(new iMerc(0,0), 1000,0,0,0),1000.0f,5f);
		Assert.assertEquals((float)t.calcElevImpl(new iMerc(8192,0), 1000,0,0,0),500.0f,5f);
		Assert.assertEquals((float)t.calcElevImpl(new iMerc(0,8192), 1000,0,0,0),500.0f,5f);
		Assert.assertEquals((float)t.calcElevImpl(new iMerc(8192,8192), 1000,0,0,0),0.0f,5f);
		Assert.assertEquals((float)t.calcElevImpl(new iMerc(16384,16384), 1000,0,0,0),0.0f,5f);
		System.out.println("all ok");

		
		for(int i=44;i<45;++i)
		{
			int lodlev=6958-100*i;
			if (lodlev<1500) break;
			System.out.println("Lodlev: #"+i+" = "+lodlev);
			LodCalc lodcalc=new LodCalc(480,lodlev);
			
			Random r=new Random();
			play.changeLods(new iMerc(0,0),(short) 500,vstore,estore,lodcalc,0);
			
			play.prepareForRender();
			HashSet<iMerc> hs=vstore.dbgGetIMercSet();
			
			
			/*
			Assert.assertTrue(hs.contains(new iMerc(0,0)));
			Assert.assertFalse(hs.contains(new iMerc(4096,4096)));
			Assert.assertTrue(hs.contains(new iMerc(16384,16384)));
			Assert.assertTrue(hs.contains(new iMerc(8192,8192)));
			*/
			HashMap<iMerc,Vertex> av=vstore.dbgGetVerticesMap();
			Vertex[] base=new Vertex[4];
			base[0]=av.get(new iMerc(0,0));
			base[1]=av.get(new iMerc(16384,0));
			base[2]=av.get(new iMerc(0,16384));
			base[3]=av.get(new iMerc(16384,16384));
			Vertex center=av.get(new iMerc(8192,8192));
			for(int j=0;j<4;++j)
				System.out.println("Vertex base: #"+j+": "+base[j]+" alt: "+base[j].calcZ());
			if (center!=null)
				System.out.println("Center: "+center+" alt: "+center.calcZ());		
		}
		play.completeDebugDump("dump_2b.json");
				
	}

	//@Test
	public void testPlayfieldSimpleIntegration3() throws FileNotFoundException, IOException
	{
		iMerc p1=new iMerc(0,0);
		iMerc p2=new iMerc(16384,16384);
		VertexStore vstore=new VertexStore(1000,0);
		ElevationStoreIf estore=TestElevMap.getSampleEstore();
		TriangleStore tristore=new TriangleStore(1000);

		ThingFactory thingf=new ThingFactory()
		{
			@Override
			public ThingIf createThing(VertexStore vstore, TextureStore tstore,ElevationStoreIf estore,
					int i, iMerc m,Stitcher st) {
				Thing t=new Thing(m,null,i,vstore,estore,st);
				return t;
			}
			
		};
		Playfield play=new Playfield(p1,p2,vstore,null,tristore,estore,thingf);
		LodCalc lodcalc=new LodCalc(480,1770);
		
		Random r=new Random();
		r.setSeed(42);
		for(int i=0;i<10;++i)
		{
			float alt=r.nextFloat()*3000;
			float xpos=r.nextFloat()*8192;
			float ypos=r.nextFloat()*8192;
			float bumpbias=r.nextFloat()*10.0f;
			play.changeLods(new iMerc((int)xpos,(int)ypos),(short) alt,vstore,estore,lodcalc,bumpbias);
			//Yes, reproduced!
			play.prepareForRender();			
			play.completeDebugDump("dump"+i+".json");
		}
		
	}
}
