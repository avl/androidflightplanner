package se.flightplanner.map3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import android.util.Log;

import se.flightplanner.AirspaceSigPointsTree;
import se.flightplanner.Project;
import se.flightplanner.SigPoint;
import se.flightplanner.Project.iMerc;
import se.flightplanner.map3d.AirspaceDrawer.Column;
import se.flightplanner.map3d.AirspaceDrawer.DrawnAirspace;
import se.flightplanner.vector.BoundingBox;

public class PointDrawer {

	static class DrawnPoint
	{
		SigPoint sigpoint;
		ArrayList<Triangle> triangles;
		ArrayList<Vertex> vertices;
		boolean used;
		int altpixels;
		public DrawnPoint(SigPoint sig,VertexStore3D vstore,TriangleStore tristore)
		{
			this.sigpoint=sig;
			vertices=new ArrayList<Vertex>();
			triangles=new ArrayList<Triangle>();
			if (sig.kind=="obstacle")
			{
				iMerc ipos=new iMerc(sig.pos);
				altpixels=5*(int)(sig.alt*Project.approx_ft_pixels(ipos, 13));
				int base=(int)(altpixels*0.25f);
				Log.i("fplan",sig.name+"altpixels:"+altpixels+" base: "+base);
				for(int i=0;i<5;++i)
					vertices.add(vstore.alloc());
				vertices.get(0).deploy(ipos.x-base, ipos.y-base, (byte) 0, "obstacle "+sig.name, 0, 0);
				vertices.get(1).deploy(ipos.x+base, ipos.y-base, (byte) 0, "obstacle "+sig.name, 0, 0);
				vertices.get(2).deploy(ipos.x+base, ipos.y+base, (byte) 0, "obstacle "+sig.name, 0, 0);
				vertices.get(3).deploy(ipos.x-base, ipos.y+base, (byte) 0, "obstacle "+sig.name, 0, 0);
				vertices.get(4).deploy(ipos.x, ipos.y, (byte) 0, "obstacle "+sig.name, 0, 0);
				for(int i=0;i<4;++i)
					addTri(4,(i+1)%4,i,tristore);
				int idx=0;
				byte[] intense=new byte[]{(byte)128,(byte)192,(byte)255,(byte)192,(byte)255};
				for(Vertex v : vertices)
				{
					v.r=intense[idx];
					v.b=intense[idx];
					v.g=0;
					v.a=127;
					++idx;
				}
			}
			
		}
		private void addTri(int i, int j, int k,TriangleStore tristore) {
			Triangle t=tristore.alloc();
			t.assign(vertices.get(i),vertices.get(j),vertices.get(k),null);
			triangles.add(t);
		}
		public void release(TriangleStore tristore)
		{
			for(Triangle t : triangles)
				tristore.release(t);
			for(Vertex v: vertices)
				v.decrementUsage();
		}
		public void calcElev() {
			for(int i=0;i<4;++i)
			{
				Vertex v=vertices.get(i);
				v.contribElev((short)0,(short)1000);
			}
			Vertex top=vertices.get(4);
			top.contribElev((short)sigpoint.alt,(short)1000);
			
			
		}
	}
	HashMap<SigPoint,DrawnPoint> points;
	AirspaceSigPointsTree allObst;
	public PointDrawer(AirspaceSigPointsTree allObst) {
		this.allObst=allObst;
		this.points=new HashMap<SigPoint,DrawnPoint>();
	}
	void update(iMerc pos,VertexStore3D vstore,TriangleStore tristore)
	{
		BoundingBox bb=new BoundingBox(pos.x,pos.y,pos.x,pos.y);
		bb=bb.expand(Project.approx_scale(pos.y, 13, 30));
		for(DrawnPoint existing : points.values())
			existing.used=false;
		
		ArrayList<SigPoint> nowsigs=allObst.findall(bb);
		for(SigPoint sig : nowsigs)
		{
			//Log.i("fplan","Sigpoint:"+sig.name);
			DrawnPoint p=points.get(sig);
			if (p==null)
			{
				p=new DrawnPoint(sig,vstore,tristore);
				points.put(sig,p);
			}
			p.used=true;
		}
		Iterator<Entry<SigPoint,DrawnPoint>> it=points.entrySet().iterator();
		while(it.hasNext())
		{
			Entry<SigPoint,DrawnPoint> e=it.next();
			if (e.getValue().used==false)
			{
				e.getValue().release(tristore);
				it.remove();
			}
		}		
	}
	public void prepareForRender() {		
		for(DrawnPoint existing : points.values())
			existing.calcElev();
	}
}
