package se.flightplanner.map3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

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
				col.updn[0].contribElev((short)(floor),(short) 1000);
				col.updn[1].contribElev((short)(ceiling),(short) 1000);
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
			Column c = obtaincol(pos, vstore);
			return c.updn[upper];			
		}
		private Column obtaincol(iMerc pos, VertexStore3D vstore) {
			Column c=vertices.get(pos);
			///float[] brights=new float[]{0.5f+0.5f*lobright,0.5f+0.5f*hibright};
			if (c==null)
			{				
				c=new Column();
				for(int i=0;i<2;++i)
				{
					Vertex v=vstore.alloc();					
					v.deploy(pos.x,pos.y,(byte)0,"Airspace vertex23 "+source.name+" "+pos+" up/dn:"+i,0,0);
					c.updn[i]=v;
				}
				vertices.put(pos,c);
			}
			return c;
		}
		DrawnAirspace(AirspaceArea area,AltParser altp,VertexStore3D vstore,TriangleStore tristore)
		{
			source=area;
			floor=(float)altp.parseAlt(area.floor);
			ceiling=(float)altp.parseAlt(area.ceiling);
			ArrayList<SimpleTriangle> tris=new ArrayList<SimpleTriangle>();
			triangles=new ArrayList<Triangle>();
			vertices=new HashMap<iMerc, Column>();
			//Log.i("fplan","Triangulating "+source.name+" Source.poly:"+source.poly);
			PolygonTriangulator.triangulate(source.poly,tris);
			ArrayList<Vector> ps=source.poly.get_points();
			int num=ps.size();
			HashSet<Vertex> outerEdges=new HashSet<Vertex>();
			
			for(int i=0;i<num;++i)
			{
				Vector curv=ps.get(i);
				Vector nextv=ps.get((i+1)%num);
				Vector d=nextv.minus(curv).normalized().rot90r();
				float bright=0.5f*(1.0f+(float)d.gety());
				Log.i("fplan","Bright:"+curv+" / "+nextv+" d: "+d+" bright: "+bright);
				iMerc cur=new iMerc(curv.getx(),curv.gety());
				iMerc next=new iMerc(nextv.getx(),nextv.gety());

				
				if (i%2==0)
				{
					Triangle at=tristore.alloc();
					Triangle bt=tristore.alloc();
					Column col1=obtaincol(cur,vstore);
					Column col2=obtaincol(next,vstore);
					///TODO: We should introduce extra vertices so that all airspaces
					///have an even number of vertices. 
					outerEdges.add(col1.updn[0]);
					outerEdges.add(col1.updn[1]);
					assigncol(col1.updn[0],bright);
					at.assign(col2.updn[1], col2.updn[0], col1.updn[0], null);
					bt.assign(col1.updn[1], col2.updn[1], col1.updn[0], null);
					triangles.add(at);
					triangles.add(bt);
				}
				else
				{
					
					Triangle at=tristore.alloc();
					Triangle bt=tristore.alloc();
					Column col1=obtaincol(cur,vstore);
					Column col2=obtaincol(next,vstore);
					assigncol(col2.updn[1],bright);
					at.assign(col2.updn[0], col1.updn[0], col2.updn[1], null);
					bt.assign(col1.updn[0], col1.updn[1], col2.updn[1], null);					
					triangles.add(at);
					triangles.add(bt);
					
				}
			}
			
			
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
					int start=0;
					while (start<2 && outerEdges.contains(triverts[(start+2)%3]))
						start+=1;
					Vertex last=triverts[(start+2)%3];
					if (updn==1)
					{
						assigncol(last,0.75f);
						outtri.assign(triverts[(start+1)%3],triverts[(start+0)%3],triverts[(start+2)%3],null);
					}
					else
					{
						assigncol(last,0.25f);
						outtri.assign(triverts[(start+0)%3],triverts[(start+1)%3],triverts[(start+2)%3],null);
					}
					triangles.add(outtri);
				}
			}	
					
		}
		private void assigncol(Vertex vertex, float bright) {
			/*float r=source.r;
			float g=source.g;
			float b=source.b;
			r*=0.5f+0.5f*bright;
			g*=0.5f+0.5f*bright;
			b*=0.5f+0.5f*bright;
			if (r>=255) r=255;
			if (g>=255) g=255;
			if (b>=255) b=255;
			vertex.r=(byte)(int)r;
			vertex.g=(byte)(int)g;
			vertex.b=(byte)(int)b;*/
			float x=bright*0.5f+0.5f;
			if (x<0.5f) x=0.5f;
			if (x>1.0f) x=1.0f;

			float r=(((int)source.r)&0xff)*x;
			float g=(((int)source.g)&0xff)*x;
			float b=(((int)source.b)&0xff)*x;
			int ir=(int)r;
			int ig=(int)g;
			int ib=(int)b;
			vertex.a=(byte)127;
			vertex.r=(byte)ir;
			vertex.g=(byte)ig;
			vertex.b=(byte)ib;
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
			//Log.i("fplan","BB:"+bb+"Drawing "+area.name);
			/*if (!area.name.contains("CTR"))
				continue;*/
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
