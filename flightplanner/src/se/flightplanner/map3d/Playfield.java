package se.flightplanner.map3d;

import java.util.ArrayList;
import java.util.HashMap;

import se.flightplanner.Project;
import se.flightplanner.Project.iMerc;

public class Playfield implements Stitcher {
	
	final static int coarsestlevel=5;
	final static int finestlevel=13;

	VertexStore vstore;
	TriangleStore tristore;
	LodCalc lodCalc;
	ThingFactory thingf;
	private ArrayList<HashMap<iMerc,ThingIf>> levels;
	public Playfield(iMerc upperleft,iMerc lowerright,VertexStore vstore,TriangleStore tristore,LodCalc lodCalc,ElevationStore estore,
			ThingFactory thingf)
	{
		this.thingf=thingf;
		this.lodCalc=lodCalc;
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
	public void changeLods(iMerc observer,short observerElev,VertexStore vstore,ElevationStore estore)
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
				if (refine<0)
				{
					if (t.isSubsumed())
					{						
						t.unsubsume(vstore,this);
					}
					continue;
				}
				else
				{
					if (!t.isSubsumed())
					{
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
			if (zl<coarsestlevel) throw new RuntimeException("Bad level for newly created Thing");
			HashMap<iMerc,ThingIf> lh=levels.get(zl);
			lh.put(t.getPos(),t);
		}		
	}
	public void prepareForRender()
	{
		for(int i=coarsestlevel;i<=finestlevel;++i)
		{
			HashMap<iMerc,ThingIf> lh=levels.get(i);
			if (lh!=null)
				for(ThingIf t:lh.values())
					t.triangulate(tristore);
		}
		
	}

	public void stitch(Vertex v,int level,boolean unstitch) {
		level-=1;
		int zoomgap=13-level;
		int boxsize=64<<zoomgap;
		boolean unshare=unstitch;
		for(;level>=coarsestlevel;--level)
		{
			boolean goody=(v.gety()&(boxsize-1))!=0;
			boolean goodx=(v.getx()&(boxsize-1))!=0;
			if (goodx && goody)
				continue; //Corner vertex - won't need stitching. Coarser levels might, though...
			HashMap<iMerc,ThingIf> lh=levels.get(level);
			if (goody)
			{
				//Vertex may fit in horizontal edge of some box
				int x=v.getx()&(~(boxsize-1));
				ThingIf t=lh.get(new iMerc(x,v.gety())); //bottom edge
				if (t!=null)
					t.shareVertex(vstore,v,!unshare);
				t=lh.get(new iMerc(x,v.gety()+boxsize)); //top edge
				if (t!=null)
					t.shareVertex(vstore,v,!unshare);				
			}
			if (goodx)
			{
				//Vertex may fit in horizontal edge of some box
				int y=v.gety()&(~(boxsize-1));
				ThingIf t=lh.get(new iMerc(v.getx(),y)); //left edge
				if (t!=null)
					t.shareVertex(vstore,v,!unshare);
				t=lh.get(new iMerc(v.getx()+boxsize,y)); //right edge
				if (t!=null)
					t.shareVertex(vstore,v,!unshare);				
			}
			//Future optimization: If we know we've found all neighbors, we don't need to go further up in hierarchy.
			if (!goodx && !goody)
				break; //even coarser levels won't match if finer levels don't.
		}
		
	}
	
}
