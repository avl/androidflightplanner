package se.flightplanner.map3d;

import java.util.ArrayList;
import java.util.HashMap;

import se.flightplanner.Project;
import se.flightplanner.Project.iMerc;

public class Playfield {
	/*
	final static int coarsestlevel=5;
	final static int finestlevel=13;

	VertexStore vstore;
	TriangleStore tristore;
	private ArrayList<HashMap<iMerc,Thing>> levels;
	public Playfield(iMerc upperleft,iMerc lowerright,VertexStore vstore,TriangleStore tristore)
	{
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
						Thing t=new Thing(m,boxsize,null,i,vstore);
						lh.put(m,t);
					}
				}
			}
			levels.add(lh);
		}
	}
	public void calculate_lods(iMerc observer,short observerElev)
	{
		for(int i=coarsestlevel;i<=finestlevel;++i)
		{
			HashMap<iMerc,Thing> lh=levels.get(i);
			for(Thing t:lh.values())
			{
				iMerc tpos=t.getPos();				
				int ypixels=Project.approx_ft_pixels(tpos,13)*t.bumpiness_ft();
				//90 deg FOV
				int pixels=
				
			}
		}
	}
*/	
}
