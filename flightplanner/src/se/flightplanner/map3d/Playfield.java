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
	private ArrayList<HashMap<iMerc,Thing>> levels;
	public Playfield(iMerc upperleft,iMerc lowerright,VertexStore vstore,TriangleStore tristore,LodCalc lodCalc,ElevationStore estore)
	{
		this.lodCalc=lodCalc;
		this.vstore=vstore;
		this.tristore=tristore;
		levels=new ArrayList<HashMap<iMerc,Thing>>();
		for(int i=0;i<coarsestlevel;++i)
			levels.add(null);
		for(int i=coarsestlevel;i<=finestlevel;++i)
		{
			HashMap<iMerc,Thing> lh=new HashMap<iMerc,Thing>();
			if (i==coarsestlevel)
			{
				int zoomgap=13-i;
				int boxsize=256<<zoomgap;
				
				int y=upperleft.y;
				for(;y<lowerright.y;y+=boxsize)
				{
					int x=upperleft.x;
					for(;x<lowerright.x;x+=boxsize)
					{
						iMerc m=new iMerc(x,y);
						Thing t=new Thing(m,null,i,vstore,estore);
						lh.put(m,t);
					}
				}
			}
			levels.add(lh);
		}
	}
	public void calculate_lods(iMerc observer,short observerElev,VertexStore vstore,ElevationStore estore)
	{
		ArrayList<Thing> newThings=new ArrayList<Thing>();
		for(int i=coarsestlevel;i<=finestlevel;++i)
		{
			HashMap<iMerc,Thing> lh=levels.get(i);
			for(Thing t:lh.values())
			{
				iMerc tpos=t.getPos();				
				float bumpiness=t.bumpiness();
				float refine=lodCalc.needRefining(bumpiness, t.getDistance(observer,observerElev));
				if (refine<0)
				{
					if (t.isSubsumed())
						t.unsubsume();
					continue;
				}
				else
				{
					if (!t.isSubsumed())
						t.subsume(newThings,vstore,this,estore);
					continue;
				}
				//90 deg FOV
				
			}
		}
	}

	public void stitch(Vertex v,int level) {
		level-=1;
		int zoomgap=13-level;
		int boxsize=256<<zoomgap;
		while(level>=coarsestlevel)
		{
			HashMap<iMerc,Thing> lh=levels.get(level);
			boolean goody=(v.gety()&(boxsize-1))!=0;
			if (goody)
			{
				//Vertex may fit in horizontal edge of some box
				int x=v.getx()&(~(boxsize-1));
				Thing t=lh.get(new iMerc(x,v.gety())); //bottom edge
				if (t!=null)
					t.shareVertex(v);
				t=lh.get(new iMerc(x,v.gety()+boxsize)); //top edge
				if (t!=null)
					t.shareVertex(v);				
			}
			boolean goodx=(v.getx()&(boxsize-1))!=0;
			if (goodx)
			{
				//Vertex may fit in horizontal edge of some box
				int y=v.gety()&(~(boxsize-1));
				Thing t=lh.get(new iMerc(v.getx(),y)); //left edge
				if (t!=null)
					t.shareVertex(v);
				t=lh.get(new iMerc(v.getx()+boxsize,y)); //right edge
				if (t!=null)
					t.shareVertex(v);				
			}
			
			level-=1;
		}
		
	}
	
}
