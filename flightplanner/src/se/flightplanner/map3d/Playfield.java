package se.flightplanner.map3d;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import android.util.Log;

import se.flightplanner.Project;
import se.flightplanner.Project.iMerc;

public class Playfield implements Stitcher {
	
	final static int coarsestlevel=5;
	final static int finestlevel=13;

	TerrainVertexStore vstore;
	TriangleStore tristore;
	TextureStore tstore;
	ThingFactory thingf;
	private ArrayList<HashMap<iMerc,ThingIf>> levels;
	
	public void completeDebugDump(String fname) throws IOException
	{
		if (fname==null)
			fname="/sdcard/fplan.dump";
		FileOutputStream rf=new FileOutputStream(fname);
		Writer f=new BufferedWriter(new OutputStreamWriter(rf));
		f.write("{\n");
		vstore.debugDump(f);
		f.write(",");
		tristore.debugDump(f);
		f.write(",");
		int tcnt=0;
		f.write("\"things\" : [");
		for(int i=coarsestlevel;i<=finestlevel;++i)
		{
			for(ThingIf t: levels.get(i).values())
			{	
				if (tcnt!=0) f.write(",\n");
				t.debugDump(f);
				++tcnt;
			}
		}
		f.write("]\n");
		f.write("}\n");
		f.flush();
		rf.close();
	}
	
	public Playfield(iMerc upperleft,iMerc lowerright,TerrainVertexStore vstore,TextureStore tstore, TriangleStore tristore,ElevationStoreIf estore,
			ThingFactory thingf)
	{
		this.thingf=thingf;
		this.vstore=vstore;
		this.tristore=tristore;
		this.tstore=tstore;
		levels=new ArrayList<HashMap<iMerc,ThingIf>>();
		for(int i=0;i<coarsestlevel;++i)
			levels.add(null);
		int cnt=0;
		for(int i=coarsestlevel;i<=finestlevel;++i)
		{
			HashMap<iMerc,ThingIf> lh=new HashMap<iMerc,ThingIf>();
			if (i==coarsestlevel)
			{
				int zoomgap=13-i;
				int boxsize=64<<zoomgap;
				int boxmask=boxsize-1;
				int y=upperleft.y;
				y&=~(boxmask);
				for(;y<lowerright.y;y+=boxsize)
				{
					int x=upperleft.x;
					x&=~(boxmask);
					for(;x<lowerright.x;x+=boxsize)
					{
						iMerc m=new iMerc(x,y);
						
						ThingIf t = thingf.createThing(vstore, tstore, estore, i, m,this);
						t.adjustRefine(1.0f);
						lh.put(m,t);
						cnt+=1;
					}
				}
			}
			levels.add(lh);
		}
	}
	
	public ThingIf dbgGetThing(iMerc pos,int level)
	{
		HashMap<iMerc,ThingIf> lh=levels.get(level);
		if (lh==null) throw new RuntimeException("Bad level: "+level);
		ThingIf ret=lh.get(pos);
		if (ret==null)
			return null;
		return ret;
	}
	public void scanForCracks()
	{
		for(int i=coarsestlevel;i<finestlevel;++i)
		{
			HashMap<iMerc,ThingIf> lh=levels.get(i);
			for(ThingIf t:lh.values())
			{
				HashSet<Vertex> vs=((Thing)t).getEdgeVertices();
				vs.addAll(((Thing)t).getCornersAndCenter());
				int boxsize=t.getBoxSize();
				if (!t.isSubsumed())
				{
					for(int i2=coarsestlevel;i2<finestlevel;++i2)
					{
						HashMap<iMerc,ThingIf> lh2=levels.get(i2);
						for(ThingIf t2:lh2.values())
						{
							if (t.getPos().equals(t2.getPos()) && t.getZoomlevel()==t2.getZoomlevel())
								continue; //don't compare to self
							Thing tt=(Thing)t2;
							for(Vertex v : tt.getCornersAndCenter())
							{
								if (v.getx()>=t.getPos().x &&
									v.gety()>=t.getPos().y &&
									v.getx()<=t.getPos().x+boxsize &&
									v.gety()<=t.getPos().y+boxsize)
								{
									if (!vs.contains(v))
										throw new RuntimeException("Vertex "+v+" should have been stitched into "+t+" but it wasn't!");
								}
							}
						}
					}
				}
			}
		}
	}
	public void changeLods(iMerc observer,short observerElev,TerrainVertexStore vstore,ElevationStoreIf estore,LodCalc lodCalc,float bumpinessBias)
	{
		int nearLodLimit=250;
		//System.out.println("changeLods running");
		int freetri=tristore.getFreeTriangles();
		int freevert=vstore.getFreeVertices();		
		for(int i=coarsestlevel;i<finestlevel;++i)
		{
			HashMap<iMerc,ThingIf> lh=levels.get(i);
			for(ThingIf t:lh.values())
			{
				///iMerc tpos=t.getPos();				
				float bumpiness=t.bumpiness()+bumpinessBias;
				float dist=t.getDistance(observer,observerElev);
				if (dist<=nearLodLimit)
					dist=nearLodLimit;
				float refine=lodCalc.needRefining(bumpiness, dist);
				//System.out.println("Thing: "+t.getPosStr()+": Refine calculated as "+refine);
				//if (i>=6) refine=-1.0f;
				//else refine=1;
				if (i==coarsestlevel)
					t.adjustRefine(1.0f); //Coarsest level is always fully refined".
				if (i>13)
					refine=-1;
				if (refine>=1.0f) refine=1.0f;
				if (refine>t.getRefine())
					refine=t.getRefine();
				if (refine<=0)
				{					
					if (t.isSubsumed())
					{
						//Log.i("fplan","Unsubsuming: Refine-value for "+t+" is "+refine);
						//scanForCracks();
						ArrayList<ThingIf> removedThings=new ArrayList<ThingIf>();
						//verifyVertexUsage();
						t.unsubsume(vstore,this,removedThings,tristore);
						for(ThingIf t2:removedThings)
						{
							int zl=t2.getZoomlevel();
							if (zl<=i) throw new RuntimeException("Removed things on same or higher zoomlevel!");
							if (zl<=coarsestlevel) throw new RuntimeException("Bad (low) level for removed Thing");
							if (zl>finestlevel) throw new RuntimeException("Bad (high) level for removed Thing");
							HashMap<iMerc,ThingIf> lh2=levels.get(zl);
							if (lh2.remove(t2.getPos())==null)
								throw new RuntimeException("Unexpected error - thing to be removed wasn't found in map. Pos:"+t2.getPosStr());
						}		
						//verifyVertexUsage();
						//scanForCracks();
					}
					continue;
				}
				else
				{
					if (freetri>16 &&
						freevert>9)
					{
							
						if (!t.isSubsumed())
						{
							//Log.i("fplan","Subsuming: Refine-value for "+t+" is "+refine);
							ArrayList<ThingIf> newThings=new ArrayList<ThingIf>();
							//verifyVertexUsage();
							t.subsume(newThings,vstore,tstore,this,estore);
							
							//the worst case really is pretty bad, when it comes to
							//used vertices and triangles:
							// + 5 new base vertices
							// + 4 new center vertices for stitching
							// + 8 new base triangles
							// + 8 new stitching triangles
							// ( also, 2 + 4 triangles would go away)
							freevert-=9; 
							freetri-=16;
							//Log.i("fplan","Newthings: "+newThings.size());
							for(ThingIf t2:newThings)
							{
								int zl=t2.getZoomlevel();
								if (zl<=i) throw new RuntimeException("Added things on same or higher zoomlevel!");
								if (zl<=coarsestlevel) throw new RuntimeException("Bad (low) level for newly created Thing");
								if (zl>finestlevel) throw new RuntimeException("Bad (high) level for newly created Thing");
								HashMap<iMerc,ThingIf> lh2=levels.get(zl);
								//Log.i("fplan","Physically putting thing "+t2+" into zoomlevel "+zl);
								if (lh2.put(t2.getPos(),t2)!=null)
									throw new RuntimeException("Attempt to replace existing child!");
								//Log.i("fplan","Adding new thing "+t2);
							}		
							//Log.i("fplan","Verify to start");
							//verifyVertexUsage();
							//Log.i("fplan","Verify succeeded");
						}
					}
					if (t.isSubsumed())
					{
						for(ThingIf child : t.getAllChildren())
							child.adjustRefine(refine);
					}
					continue;
				}
				//90 deg FOV
				
			}
		}
		//scanForCracks();
	}
	public void explicitSubsume(iMerc pos,int zoomlevel,TerrainVertexStore vstore,ElevationStoreIf estore,boolean subsume)
	{
		ArrayList<ThingIf> newThings=new ArrayList<ThingIf>();
		ArrayList<ThingIf> removedThings=new ArrayList<ThingIf>();
		HashMap<iMerc,ThingIf> lh=levels.get(zoomlevel);
		for(ThingIf t:lh.values())
		{
			t.adjustRefine(1.0f);
			if (t.getPos().equals(pos))
			{
				if (t.isSubsumed() && !subsume)
				{						
					t.unsubsume(vstore,this,removedThings,tristore);
					continue;
				}
				if (!t.isSubsumed() && subsume)
				{
					t.subsume(newThings,vstore,tstore,this,estore);
					continue;
				}
			}
		}		
		for(ThingIf t:newThings)
		{
			int zl=t.getZoomlevel();
			if (zl<coarsestlevel) throw new RuntimeException("Bad level for newly created Thing");
			HashMap<iMerc,ThingIf> lh2=levels.get(zl);
			t.adjustRefine(1.0f);
			lh2.put(t.getPos(),t);
		}		
		for(ThingIf t:removedThings)
		{
			int zl=t.getZoomlevel();
			if (zl<=coarsestlevel) throw new RuntimeException("Bad level for removed Thing");
			HashMap<iMerc,ThingIf> lh2=levels.get(zl);
			if (lh2.remove(t.getPos())==null)
				throw new RuntimeException("Thing to be removed didn't exist: "+t);
		}		
	}
	

	

	public void prepareForRender()
	{
		//verifyVertexUsage();
		for(int i=coarsestlevel;i<=finestlevel;++i)
		{
			HashMap<iMerc,ThingIf> lh=levels.get(i);
			if (lh!=null)
			{
				for(ThingIf t:lh.values())
				{
					t.triangulate(tristore,vstore);
					t.calcElevs1(tristore,vstore);
				}
			}
		}
		//verifyVertexUsage();
		for(int i=coarsestlevel;i<=finestlevel;++i)
		{
			HashMap<iMerc,ThingIf> lh=levels.get(i);
			if (lh!=null)
			{
				for(ThingIf t:lh.values())
				{
					t.calcElevs2(tristore,vstore);
					
					for(Vertex v:((Thing)t).getCornersAndCenter())
					{
						if (!v.dbgHasElev())
							throw new RuntimeException("No elev, vertex: "+v+" of thing: "+t);
					}
				}
			}
		}
		//verifyVertexUsage();
			
		
	}
	/*
	 * Temporarily decomissioned since we now have other vertices than terrain vertices...
	private void verifyVertexUsage() {
		HashSet<Vertex> allInStore=vstore.dbgGetAllUsed();
		int good=0;
		for(int i=coarsestlevel;i<=finestlevel;++i)
		{
			HashMap<iMerc,ThingIf> lh=levels.get(i);
			if (lh!=null)
			{
				//Log.i("fplan","Number of things on level "+i+" is "+lh.size());
				for(ThingIf t:lh.values())
				{
					for(Vertex v:((Thing)t).getCornersAndCenter())
					{
						if (allInStore.contains(v))
						{
							good+=1;
							if (allInStore.remove(v)==false)
								throw new RuntimeException("Failed to remove v");
						}
					}					
				}
			}
		}
		if (allInStore.size()!=0)
		{
			Vertex vf=allInStore.iterator().next();
			throw new RuntimeException("Bad cnt="+allInStore.size()+", good cnt="+good+", first is "+vf+" vf.isUsed="+vf.isUsed());
		}
	}
	*/
	public void stitch(Vertex v,int level,ThingIf parent,boolean dostitch) {
		level-=1;
		int zoomgap=13-level;
		int boxsize=64<<zoomgap;
		boolean unshare=!dostitch;
		for(;level>=coarsestlevel;--level,boxsize<<=1)
		{
			
			boolean goody=(v.gety()&(boxsize-1))==0;
			boolean goodx=(v.getx()&(boxsize-1))==0;
			if (goodx && goody)
			{
				 //Corner vertex - won't need stitching. Coarser levels might, though...
			}
			else
			{
				HashMap<iMerc,ThingIf> lh=levels.get(level);
				if (goody)
				{
					//Vertex may fit in horizontal edge of some box
					int x=v.getx()&(~(boxsize-1));
					ThingIf t=lh.get(new iMerc(x,v.gety()-boxsize)); //bottom edge
					if (t!=null && t!=parent && !t.isReleased())
						t.shareVertex(vstore,v,!unshare);
					t=lh.get(new iMerc(x,v.gety())); //top edge
					if (t!=null && t!=parent && !t.isReleased())
						t.shareVertex(vstore,v,!unshare);				
				}
				if (goodx)
				{
					//Vertex may fit in horizontal edge of some box
					int y=v.gety()&(~(boxsize-1));
					ThingIf t=lh.get(new iMerc(v.getx()-boxsize,y)); //left edge
					if (t!=null && t!=parent && !t.isReleased())
						t.shareVertex(vstore,v,!unshare);
					t=lh.get(new iMerc(v.getx(),y)); //right edge
					if (t!=null && t!=parent && !t.isReleased())
						t.shareVertex(vstore,v,!unshare);				
				}
				//Future optimization: If we know we've found all neighbors, we don't need to go further up in hierarchy.
				if (!goodx && !goody)
					break; //even coarser levels won't match if finer levels don't.
			}
			if (parent!=null)
				parent=parent.getParent();
		}
		
	}
	
}
