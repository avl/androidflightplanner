package se.flightplanner.map3d;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

import javax.microedition.khronos.opengles.GL10;

import android.graphics.Bitmap;
import android.util.Log;

import se.flightplanner.Project;
import se.flightplanner.Project.LatLon;
import se.flightplanner.Project.iMerc;
import se.flightplanner.map3d.VertexStore.VertAndColor;

public class PlayfieldDrawer {

	VertexStore vstore;
	TriangleStore tristore;
	ElevationStoreIf elevstore;
	TextureStore tstore;
	LodCalc lodc;
	boolean dodump;
	Playfield playfield;
	int deftex;
	public PlayfieldDrawer(ElevationStoreIf estore, TextureStore tstore)
	{
		iMerc p1=Project.latlon2imerc(new LatLon(70,10),13);
		iMerc p2=Project.latlon2imerc(new LatLon(50,20),13);
		vstore=new VertexStore(2000);
		tristore=new TriangleStore(2000);
		deftex=-1;
		lodc=new LodCalc(480,100); //TODO: Screenheight isn't always 480. Also, tolerance 1000 is too big!
		elevstore=estore;
		this.tstore=tstore;
		if (elevstore==null)
			elevstore=new ElevationStore(0);
		playfield=new Playfield(p1,p2,vstore,tstore,tristore,elevstore,
				new ThingFactory()
			{
				public ThingIf createThing(VertexStore vstore,
						TextureStore tstore,ElevationStoreIf estore, int zoomlevel, iMerc m, Stitcher st) {
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
			GlHelper.checkGlError(gl);
			if (deftex==-1)
			{
				Bitmap bmap=tstore.getRandomBitmap();
				Log.i("fplan","Loading bmap: "+bmap.getWidth()+"x"+bmap.getHeight()+" with middle pixel: "+
							bmap.getPixel(128,128));
				GlHelper.checkGlError(gl);
				deftex=Textures.loadTexture(bmap,gl);
				GlHelper.checkGlError(gl);
			}
			
			gl.glBindTexture(GL10.GL_TEXTURE_2D, deftex);
			GlHelper.checkGlError(gl);

	        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
	        gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
	        gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
	        gl.glDisable(GL10.GL_LIGHTING);
			GlHelper.checkGlError(gl);

	        gl.glEnable(GL10.GL_TEXTURE_2D);
	        gl.glTexParameterf(gl.GL_TEXTURE_2D, gl.GL_TEXTURE_MIN_FILTER, gl.GL_LINEAR);

			playfield.changeLods(observer, observerElev, vstore, elevstore,lodc,0);
			playfield.prepareForRender();
			if (dodump)
			{
				playfield.completeDebugDump("/sdcard/dump.json");
				dodump=false;
			}
			VertAndColor va=vstore.getVerticesReadyForRender(observer,observerElev);
			GlHelper.checkGlError(gl);
					
			gl.glFrontFace(gl.GL_CCW);
			gl.glVertexPointer(3, gl.GL_FLOAT, 0, va.vertices);
			gl.glColorPointer(4, gl.GL_UNSIGNED_BYTE, 0, va.colors);
			gl.glTexCoordPointer(2,gl.GL_FLOAT,0,va.texcoords);
			TriangleStore.Indices ind=tristore.getIndexForRender(vstore);
			GlHelper.checkGlError(gl);
			gl.glDrawElements(gl.GL_TRIANGLES, 3*ind.tricount, gl.GL_UNSIGNED_SHORT,
					ind.buf);
			GlHelper.checkGlError(gl);
			Log.i("fplan","Drawed "+ind.tricount+" triangles");
		}
		catch(RuntimeException e)
		{
			if (playfield!=null)
				playfield.completeDebugDump("/sdcard/dump.json");
			throw e;
		}
		GlHelper.checkGlError(gl);
	}

	public void debugdump() throws IOException {
		dodump=true;
	}
}
