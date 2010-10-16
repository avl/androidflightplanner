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

import android.util.Log;

import se.flightplanner.Project;
import se.flightplanner.Project.iMerc;

public class Playfield implements Stitcher {
	
	final static int coarsestlevel=5;
	final static int finestlevel=13;

	VertexStore vstore;
	TriangleStore tristore;
	ThingFactory thingf;
	private ArrayList<HashMap<iMerc,ThingIf>> levels;
	
	public void completeDebugDump(String fname) throws IOException
	{
		if (fname==null)
			fname="/sdcard/fplan.dump";
		FileOutputStream rf=new FileOutputStream(fname);
		Writer f=new BufferedWriter(new OutputStreamWriter(rf));
		vstore.debugDump(f);
		tristore.debugDump(f);
		int tcnt=0;
		for(int i=coarsestlevel;i<finestlevel;++i)
		{
			for(ThingIf t: levels.get(i).values())
			{
				if (tcnt!=0) f.write(" , ");
				t.debugDump(f);
				++tcnt;
			}
		}
		f.flush();
		rf.close();
	}
	
	public Playfield(iMerc upperleft,iMerc lowerright,VertexStore vstore,TriangleStore tristore,ElevationStore estore,
			ThingFactory thingf)
	{
		this.thingf=thingf;
		this.vstore=vstore;
		this.tristore=tristore;
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
						
						ThingIf t = thingf.createThing(vstore, estore, i, m,this);
						
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
	
	public void changeLods(iMerc observer,short observerElev,VertexStore vstore,ElevationStore estore,LodCalc lodCalc)
	{
		ArrayList<ThingIf> newThings=new ArrayList<ThingIf>();
		for(int i=coarsestlevel;i<=finestlevel;++i)
		{
			HashMap<iMerc,ThingIf> lh=levels.get(i);
			for(ThingIf t:lh.values())
			{
				///iMerc tpos=t.getPos();				
				float bumpiness=t.bumpiness();
				float dist=t.getDistance(observer,observerElev);
				float refine=lodCalc.needRefining(bumpiness, dist);
				//if (i<=8) refine=1.0f;
				if (refine<0)
				{
					if (t.isSubsumed())
					{
						//Log.i("fplan","Un-subsuming "+t);
						t.unsubsume(vstore,this);
					}
					continue;
				}
				else
				{
					if (!t.isSubsumed())
					{
						//Log.i("fplan","Subsuming "+t);
						t.subsume(newThings,vstore,this,estore);
					}
					continue;
				}
				//90 deg FOV
				
			}
		}
		for(ThingIf t:newThings)
		{
			int zl=t.getZoomlevel();
			if (zl<=coarsestlevel) throw new RuntimeException("Bad level for newly created Thing");
			HashMap<iMerc,ThingIf> lh=levels.get(zl);
			lh.put(t.getPos(),t);
		}		
	}
	public void explicitSubsume(iMerc pos,int zoomlevel,VertexStore vstore,ElevationStore estore,boolean subsume)
	{
		ArrayList<ThingIf> newThings=new ArrayList<ThingIf>();
		HashMap<iMerc,ThingIf> lh=levels.get(zoomlevel);
		for(ThingIf t:lh.values())
		{
			if (t.getPos().equals(pos))
			{
				if (t.isSubsumed() && !subsume)
				{						
					t.unsubsume(vstore,this);
					continue;
				}
				if (!t.isSubsumed() && subsume)
				{
					t.subsume(newThings,vstore,this,estore);
					continue;
				}
			}
		}		
		for(ThingIf t:newThings)
		{
			int zl=t.getZoomlevel();
			if (zl<coarsestlevel) throw new RuntimeException("Bad level for newly created Thing");
			HashMap<iMerc,ThingIf> lh2=levels.get(zl);
			lh2.put(t.getPos(),t);
		}		
	}
	
	
	public void prepareForRender()
	{
		for(int i=coarsestlevel;i<=finestlevel;++i)
		{
			HashMap<iMerc,ThingIf> lh=levels.get(i);
			if (lh!=null)
			{
				for(ThingIf t:lh.values())
				{
					t.triangulate(tristore);
				}
			}
		}
	}
	public void stitch(Vertex v,int level,ThingIf parent,boolean unstitch) {
		level-=1;
		int zoomgap=13-level;
		int boxsize=64<<zoomgap;
		boolean unshare=unstitch;
		for(;level>=coarsestlevel;--level)
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
					ThingIf t=lh.get(new iMerc(x,v.gety())); //bottom edge
					if (t!=null && t!=parent)
						t.shareVertex(vstore,v,!unshare);
					t=lh.get(new iMerc(x,v.gety()+boxsize)); //top edge
					if (t!=null && t!=parent)
						t.shareVertex(vstore,v,!unshare);				
				}
				if (goodx)
				{
					//Vertex may fit in horizontal edge of some box
					int y=v.gety()&(~(boxsize-1));
					ThingIf t=lh.get(new iMerc(v.getx(),y)); //left edge
					if (t!=null && t!=parent)
						t.shareVertex(vstore,v,!unshare);
					t=lh.get(new iMerc(v.getx()+boxsize,y)); //right edge
					if (t!=null && t!=parent)
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
