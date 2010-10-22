package se.flightplanner.map3d;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

import javax.microedition.khronos.opengles.GL10;

import android.util.Log;

import se.flightplanner.Project;
import se.flightplanner.Project.LatLon;
import se.flightplanner.Project.iMerc;
import se.flightplanner.map3d.VertexStore.VertAndColor;

public class PlayfieldDrawer {

	VertexStore vstore;
	TriangleStore tristore;
	ElevationStore elevstore;
	LodCalc lodc;
	boolean dodump;
	Playfield playfield;
	public PlayfieldDrawer(ElevationStore estore)
	{
		iMerc p1=Project.latlon2imerc(new LatLon(70,10),13);
		iMerc p2=Project.latlon2imerc(new LatLon(50,20),13);
		vstore=new VertexStore(2000);
		tristore=new TriangleStore(2000);
		lodc=new LodCalc(480,200); //TODO: Screenheight isn't always 480. Also, tolerance 1000 is too big!
		elevstore=estore;
		if (elevstore==null)
			elevstore=new ElevationStore(0);
		playfield=new Playfield(p1,p2,vstore,tristore,elevstore,
				new ThingFactory()
			{
				public ThingIf createThing(VertexStore vstore,
						ElevationStore estore, int zoomlevel, iMerc m, Stitcher st) {
					return new Thing(m,null,zoomlevel,vstore,estore,st);
				}
			}
		);
		
	}
	
	@SuppressWarnings("static-access")
	public void draw(GL10 gl,iMerc observer,short observerElev) throws IOException
	{
		try
		{
	        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
	        gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
			playfield.changeLods(observer, observerElev, vstore, elevstore,lodc,0);
			playfield.prepareForRender();
			if (dodump)
			{
				playfield.completeDebugDump("/sdcard/dump.json");
				dodump=false;
			}
			VertAndColor va=vstore.getVerticesReadyForRender(observer,observerElev);
					
			gl.glFrontFace(gl.GL_CCW);
			gl.glVertexPointer(3, gl.GL_FLOAT, 0, va.vertices);
			gl.glColorPointer(4, gl.GL_UNSIGNED_BYTE, 0, va.colors);
			TriangleStore.Indices ind=tristore.getIndexForRender(vstore);
			gl.glDrawElements(gl.GL_TRIANGLES, 3*ind.tricount, gl.GL_UNSIGNED_SHORT,
					ind.buf);
			Log.i("fplan","Drawed "+ind.tricount+" triangles");
		}
		catch(RuntimeException e)
		{
			if (playfield!=null)
				playfield.completeDebugDump("/sdcard/dump.json");
			throw e;
		}
	}

	public void debugdump() throws IOException {
		dodump=true;
	}
}
