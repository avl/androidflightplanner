package se.flightplanner.map3d;

import java.util.ArrayList;
import java.util.HashMap;

import android.util.Log;

import se.flightplanner.AirspaceArea;
import se.flightplanner.AirspaceLookup;
import se.flightplanner.Project;
import se.flightplanner.Project.iMerc;
import se.flightplanner.vector.BoundingBox;
import se.flightplanner.vector.PolygonTriangulator;
import se.flightplanner.vector.SimpleTriangle;
import se.flightplanner.vector.Vector;

public class AirspaceDrawer {
	static class Column
	{
		Vertex[] updn=new Vertex[2];		
	}
	static class DrawnAirspace
	{
		AirspaceArea source;
		float floor;
		float ceiling;
		ArrayList<Triangle> triangles;		
		HashMap<iMerc,Column> vertices;
		boolean used;
		public void calcElevs()
		{
			for(Column col : vertices.values())
			{
				col.updn[0].contribElev((short)(floor/100.0),(short) 100);
				col.updn[1].contribElev((short)(ceiling/100.0),(short) 100);
			}
		}
		public void free(TriangleStore tristore) {
			for(Column c : vertices.values())
			{
				for(int i=0;i<2;++i)
				{
					///Log.i("fplan",source.name+" Freeing "+c.updn[i]+" up/dn:"+i);
					c.updn[i].decrementUsage();
				}
			}
			for(Triangle tri:triangles)
				tristore.release(tri);
			vertices=null;
			triangles=null;
		}
		Vertex obtain(iMerc pos,int upper,VertexStore3D vstore)
		{
			Column c=vertices.get(pos);
			if (c==null)
			{				
				c=new Column();
				for(int i=0;i<2;++i)
				{
					Vertex v=vstore.alloc();					
					v.deploy(pos.x,pos.y,(byte)0,"Airspace vertex23 "+source.name+" "+pos+" up/dn:"+i,0,0);
					v.r=(byte)-1;
					v.g=(byte)(floor/400);
					v.b=(byte)((vertices.size()*20)%200+50);
					c.updn[i]=v;
				}
				vertices.put(pos,c);
			}
			return c.updn[upper];			
		}
		DrawnAirspace(AirspaceArea area,AltParser altp,VertexStore3D vstore,TriangleStore tristore)
		{
			source=area;
			floor=(float)altp.parseAlt(area.floor);
			ceiling=(float)altp.parseAlt(area.ceiling);
			ArrayList<SimpleTriangle> tris=new ArrayList<SimpleTriangle>();
			triangles=new ArrayList<Triangle>();
			vertices=new HashMap<iMerc, Column>();
			Log.i("fplan","Triangulating "+source.name+" Source.poly:"+source.poly);
			PolygonTriangulator.triangulate(source.poly,tris);
			for(int updn=0;updn<2;++updn)
			{
				for(SimpleTriangle t : tris)
				{
					Vertex[] triverts=new Vertex[3];
					Triangle outtri=tristore.alloc();
					for(int i=0;i<3;++i)
					{
						Vector vec=t.get(i);
						iMerc pos=new iMerc(vec.getx(),vec.gety());						
						triverts[i]=obtain(pos,updn,vstore);						
					}
					Log.i("fplan","Outtri: "+t);
					outtri.assign(triverts[0],triverts[1],triverts[2],null);
					triangles.add(outtri);
				}
			}			
		}
	}
	public AirspaceDrawer(AirspaceLookup airspace,AltParser altp)
	{
		this.airspace=airspace;
		this.altp=altp;
		spaces=new HashMap<AirspaceArea, DrawnAirspace>();
	}
	AirspaceLookup airspace;
	AltParser altp;
	HashMap<AirspaceArea,DrawnAirspace> spaces;
	public void updateAirspaces(iMerc pos,VertexStore3D vstore,TriangleStore tristore)
	{
		BoundingBox bb=new BoundingBox(pos.x,pos.y,pos.x,pos.y);
		bb=bb.expand(Project.approx_scale(pos.y, 13, 20));
		for(DrawnAirspace existing : spaces.values())
			existing.used=false;
		for(AirspaceArea area : airspace.areas.get_areas(bb))
		{
			Log.i("fplan","BB:"+bb+"Drawing "+area.name);
			//Log.i("fplan","Got an area with a real poly!");
			DrawnAirspace drawn=spaces.get(area);
			if (drawn==null)
			{
				drawn=new DrawnAirspace(area, altp, vstore, tristore);
				spaces.put(area,drawn);
			}
			drawn.used=true;
		}
		ArrayList<DrawnAirspace> toRemove=new ArrayList<DrawnAirspace>();
		for(DrawnAirspace existing : spaces.values())
		{
			if (!existing.used)
			{
				existing.free(tristore);
				toRemove.add(existing);
			}
		}
		for(DrawnAirspace remove:toRemove)
			spaces.remove(remove.source);
	
	}
	public void prepareForRender() {
		for(DrawnAirspace da : spaces.values())
			da.calcElevs();
		
	}
	
}
