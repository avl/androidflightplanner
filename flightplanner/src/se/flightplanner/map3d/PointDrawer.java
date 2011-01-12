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

	static class DrawnPoint {
		SigPoint sigpoint;
		ArrayList<Triangle> triangles;
		ArrayList<Vertex> vertices;
		boolean used;
		float alt;
		
		public DrawnPoint(SigPoint sig, VertexStore3D vstore,
				TriangleStore tristore) {
			this.sigpoint = sig;
			vertices = new ArrayList<Vertex>();
			triangles = new ArrayList<Triangle>();
			iMerc ipos = new iMerc(sig.pos);
			if (sig.kind == "obstacle") {
				alt = (float) sigpoint.alt;
				int altpixels = 5 * (int) (sig.alt * Project.approx_ft_pixels(ipos,
						13));
				int base1 = (int) (altpixels * 0.25f);
				Log.i("fplan", sig.name + "altpixels:" + altpixels + " base: "
						+ base1);
				for (int i = 0; i < 5; ++i)
					vertices.add(vstore.alloc());
				vertices.get(0).deploy(ipos.getX() - base1, ipos.getY() - base1,
						(byte) 0, "obstacle " + sig.name, 0, 0);
				vertices.get(1).deploy(ipos.getX() + base1, ipos.getY() - base1,
						(byte) 0, "obstacle " + sig.name, 0, 0);
				vertices.get(2).deploy(ipos.getX() + base1, ipos.getY() + base1,
						(byte) 0, "obstacle " + sig.name, 0, 0);
				vertices.get(3).deploy(ipos.getX() - base1, ipos.getY() + base1,
						(byte) 0, "obstacle " + sig.name, 0, 0);
				vertices.get(4).deploy(ipos.getX(), ipos.getY(), (byte) 0,
						"obstacle " + sig.name, 0, 0);
				for (int i = 0; i < 4; ++i)
					addTri(4, (i + 1) % 4, i, tristore);
				int idx = 0;
				byte[] intense = new byte[] { (byte) 128, (byte) 192,
						(byte) 255, (byte) 192, (byte) 255 };
				for (Vertex v : vertices) {
					v.r = intense[idx];
					v.b = intense[idx];
					v.g = 0;
					v.a = 127;
					++idx;
				}
			}
			if (sig.kind == "airport") {
				alt = (float) sigpoint.alt + 1000;
				int base2 = (int) Project.approx_scale(ipos.getY(), 13, 0.25);
				for (int i = 0; i < 8; ++i)
					vertices.add(vstore.alloc());
				vertices.get(0).deploy(ipos.getX() - base2, ipos.getY() - base2,
						(byte) 0, "obstacle " + sig.name, 0, 0);
				vertices.get(1).deploy(ipos.getX() + base2, ipos.getY() - base2,
						(byte) 0, "obstacle " + sig.name, 0, 0);
				vertices.get(2).deploy(ipos.getX() + base2, ipos.getY() + base2,
						(byte) 0, "obstacle " + sig.name, 0, 0);
				vertices.get(3).deploy(ipos.getX() - base2, ipos.getY() + base2,
						(byte) 0, "obstacle " + sig.name, 0, 0);
				vertices.get(4).deploy(ipos.getX() - base2, ipos.getY() - base2,
						(byte) 0, "obstacle " + sig.name, 0, 0);
				vertices.get(5).deploy(ipos.getX() + base2, ipos.getY() - base2,
						(byte) 0, "obstacle " + sig.name, 0, 0);
				vertices.get(6).deploy(ipos.getX() + base2, ipos.getY() + base2,
						(byte) 0, "obstacle " + sig.name, 0, 0);
				vertices.get(7).deploy(ipos.getX() - base2, ipos.getY() + base2,
						(byte) 0, "obstacle " + sig.name, 0, 0);
				byte[] intense2 = new byte[] { (byte) 128, (byte) 192,
						(byte) 255, (byte) 192, (byte) 255 };
				for (int i = 0; i < 4; ++i) {
					int lownext = (i + 1) % 4;
					addTri(i, i + 4,  lownext, tristore);
					addTri(i + 4, (i + 1) % 4 + 4, lownext, tristore);
					Vertex v = vertices.get(lownext);
					v.r = (byte) (intense2[i] - (byte) 64);
					v.b = (byte) (intense2[i]);
					v.g = (byte) (intense2[i] - (byte) 64);
					v.a = 127;
				}
				for (int i = 4; i < 8; ++i) {
					Vertex v = vertices.get(i);
					v.r = (byte) 128;
					v.b = (byte) 192;
					v.g = (byte) 128;
					v.a = 127;
				}
				addTri(5, 4, 6, tristore);
				addTri(6, 4, 7, tristore);
			}

		}

		private void addTri(int i, int j, int k, TriangleStore tristore) {
			Triangle t = tristore.alloc();
			t.assign(vertices.get(i), vertices.get(j), vertices.get(k), null);
			triangles.add(t);
		}

		public void release(TriangleStore tristore,VertexStore3D vstore) {
			for (Triangle t : triangles)
				tristore.release(t);
			Log.i("fplan","Releasing point:"+sigpoint.name);
			for (Vertex v : vertices)
			{
				boolean freed=vstore.decrement(v);
				if (!freed)
					throw new RuntimeException("Unexpected resource leak in PointDrawer");

			}
		}

		public void calcElev() {
			for (int i = 0; i < 4; ++i) {
				Vertex v = vertices.get(i);
				v.contribElev( 0, 1000);
			}
			for (int i = 4; i < vertices.size(); ++i) {
				Vertex top = vertices.get(i);
				top.contribElev( (int)alt,  1000);
			}

		}
	}

	HashMap<SigPoint, DrawnPoint> points;
	AirspaceSigPointsTree allObst;
	AirspaceSigPointsTree allAirfields;

	public PointDrawer(AirspaceSigPointsTree allObst,AirspaceSigPointsTree allAirfields) {
		this.allObst = allObst;
		this.allAirfields=allAirfields;
		this.points = new HashMap<SigPoint, DrawnPoint>();
	}

	void update(iMerc pos,VertexStore3D vstore,TriangleStore tristore)
	{
		BoundingBox bb=new BoundingBox(pos.getX(),pos.getY(),pos.getX(),pos.getY());
		bb=bb.expand(Project.approx_scale(pos.getY(), 13, 30));
		for(DrawnPoint existing : points.values())
			existing.used=false;
		
		ArrayList<SigPoint> nowsigs=allObst.findall(bb);
		bb=new BoundingBox(pos.getX(),pos.getY(),pos.getX(),pos.getY());
		bb=bb.expand(Project.approx_scale(pos.getY(), 13, 60));
		nowsigs.addAll(allAirfields.findall(bb));
		int freevert=vstore.getFreeVertices();
		int freetri=tristore.getFreeTriangles();
		for(SigPoint sig : nowsigs)
		{
			if (freevert<20 || freetri<40)
				break;
			//Log.i("fplan","Sigpoint:"+sig.name);
			DrawnPoint p=points.get(sig);
			if (p==null)
			{
				p=new DrawnPoint(sig,vstore,tristore);
				freevert-=20;
				freetri-=40;
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
				e.getValue().release(tristore,vstore);
				it.remove();
			}
		}		
	}

	public void prepareForRender() {
		for (DrawnPoint existing : points.values())
			existing.calcElev();
	}
}
