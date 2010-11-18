package se.flightplanner;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import se.flightplanner.Project.LatLon;
import se.flightplanner.Project.iMerc;
import se.flightplanner.map3d.AirspaceDrawer;
import se.flightplanner.map3d.AltParser;
import se.flightplanner.map3d.ElevationStoreIf;
import se.flightplanner.map3d.GlHelper;
import se.flightplanner.map3d.LodCalc;
import se.flightplanner.map3d.Playfield;
import se.flightplanner.map3d.PlayfieldDrawer;
import se.flightplanner.map3d.Stitcher;
import se.flightplanner.map3d.TextureStore;
import se.flightplanner.map3d.Thing;
import se.flightplanner.map3d.ThingFactory;
import se.flightplanner.map3d.ThingIf;
import se.flightplanner.map3d.TriangleStore;
import se.flightplanner.map3d.TerrainVertexStore;

import android.graphics.Bitmap;
import android.location.Location;
import android.opengl.GLSurfaceView.Renderer;
import android.util.Log;

public class MovingMap3DRenderer implements Renderer {

	Location pos;
    private FloatBuffer   mVertexBuffer;
    //private IntBuffer   mColorBuffer;
    private ByteBuffer  mIndexBuffer;
    private float a;
    int width,height;
    float altitude_feet;
    private float headturn;
    private PlayfieldDrawer playfield;
    private Bitmap fontbitmap;
    public MovingMap3DRenderer(Bitmap fontbitmap) {
		// vstore=new Vertex
    	this.fontbitmap=fontbitmap;
		a=0.0f;
		headturn=0.0f;
		pos=new Location("gps");
		pos.setLatitude(59.0);
		pos.setLongitude(17.0);
		pos.setBearing((float) 360.0);

	
	}

	public void onDrawFrame(GL10 gl) {
		//TODO: Move the below to OnSurfaceChanged
		
		gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
		gl.glMatrixMode(GL10.GL_MODELVIEW);
		gl.glLoadIdentity();
		try {
			draw_vertices(gl);
		} catch (IOException e1) {
			e1.printStackTrace();
			throw new RuntimeException(e1);
		}
		
		try {
			Thread.sleep(50);
		} catch (InterruptedException e) {
		}
	}
	int b=0;
	private void draw_vertices(GL10 gl) throws IOException {
		GlHelper.checkGlError(gl);

		
		//LatLon cameraLatLon=new LatLon(63.397,13.056);
		//LatLon cameraLatLon=new LatLon(59.344,18.111);//pos.getLatitude(),pos.getLongitude());
		LatLon cameraLatLon=new LatLon(59.45,17.706);//pos.getLatitude(),pos.getLongitude());
		iMerc cameramerc=Project.latlon2imerc(cameraLatLon, 13);
		cameramerc.y+=b;
		b+=10;

		LatLon obstLatLon=new LatLon(59,17);
		iMerc obstmerc=Project.latlon2imerc(obstLatLon, 13);
		byte one = (byte)-1;
		
		/*
I/fplan   (18568): Drawed 2 triangles
I/fplan   (18568): Rendered vertex #0: -65.53,90.29,-10.0
I/fplan   (18568): Rendered vertex #1: 98.31,90.29,-10.0
I/fplan   (18568): Rendered vertex #2: -65.53,254.13,-10.0
I/fplan   (18568): Rendered vertex #3: 98.31,254.13,-10.0
I/fplan   (18568): Index nr #0: 0,2,1
I/fplan   (18568): Index nr #1: 2,3,1
I/fplan   (18568): Drawed 2 triangles


		 */
		
        int altitude=(int)(altitude_feet);
        /*
		float vertices[] = 
		{ -65,90,10.0f-altitude/100.0f,
		   98,90,10.0f-altitude/100.0f,
		   -65,254,10.0f-altitude/100.0f,
		   98,254,10.0f-altitude/100.0f
		};
		
		byte colors[] = { (byte)-1,0,0,(byte)-1,
						0,(byte)-1,0,(byte)-1,
						0,0,(byte)-1,(byte)-1,
						(byte)-1,(byte)-1,(byte)-1,(byte)-1
						};

		short indices[] = {0,2,1,
					2,3,1};
		*/
		/*
		float onec=1.0f; 
		float yd=0;//-10.0f+a;
		float vertices[] = 
			{ -onec, -onec+yd, -onec, 
				onec, -onec+yd, -onec, 
				onec, onec+yd, -onec,
				-onec, onec+yd, -onec, 
				-onec, -onec+yd, onec, 
				onec, -onec+yd, onec, 
				onec, onec+yd, onec, 
				-onec, onec+yd, onec, };

		byte colors[] = { 0, 0, 0, one, one, 0, 0, one, one, one, 0, one, 0,
				one, 0, one, 0, 0, one, one, one, 0, one, one, one, one, one,
				one, 0, one, one, one, };

		short indices[] = { 0, 4, 5, 0, 5, 1, 1, 5, 6, 1, 6, 2, 2, 6, 7, 2, 7,
				3, 3, 7, 4, 3, 4, 0, 4, 7, 6, 4, 6, 5, 3, 0, 1, 3, 1, 2 };
		*/
		
		/*
		ByteBuffer vbb = ByteBuffer.allocateDirect(vertices.length * 4);
		vbb.order(ByteOrder.nativeOrder());
		mVertexBuffer = vbb.asFloatBuffer();
		mVertexBuffer.put(vertices);
		mVertexBuffer.position(0);

		ByteBuffer cbb = ByteBuffer.allocateDirect(colors.length);
		cbb.order(ByteOrder.nativeOrder());
		cbb.put(colors);
		cbb.position(0); 

		mIndexBuffer = ByteBuffer.allocateDirect(indices.length*2);
		mIndexBuffer.order(ByteOrder.nativeOrder());
		ShortBuffer si=mIndexBuffer.asShortBuffer();
		si.put(indices);
		si.position(0);
		 */
/*        
Probably calc mid-vertices in a separate pass, last.
For transitioning Things, the mid would initially be a
straight mix between the "two corners", so that result would
be same as before subsumtion. Then, as the subsumption deepends,
the mid-vertex would be more and more its own (calculated
from the Thing).
*/        
		
		/*
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
		*/
        /*
        gl.glFrontFace(gl.GL_CCW);
		gl.glVertexPointer(3, gl.GL_FLOAT, 0, mVertexBuffer);
		gl.glColorPointer(4, gl.GL_UNSIGNED_BYTE, 0, cbb);
		gl.glDrawElements(gl.GL_TRIANGLES, 6, gl.GL_UNSIGNED_SHORT,
				si);
		*/
        float hdg=/*pos.getBearing()+*/headturn;
		GlHelper.checkGlError(gl);
        //gl.glPopMatrix();
		if (playfield!=null)
			playfield.draw(gl,cameramerc,altitude,hdg,width,height);
		
		GlHelper.checkGlError(gl);
		

	}

	public void onSurfaceChanged(GL10 gl, int width, int height) {

		
		this.width=width;
		this.height=height;
		
		float ratio = (float) width / height;
		gl.glViewport(0, 0, width, height);
		gl.glMatrixMode(GL10.GL_PROJECTION);
		gl.glLoadIdentity();
		gl.glFrustumf(-ratio, ratio, -1, 1, 1.0f, 1e4f);
		
		if (playfield!=null)
			playfield.loadAllTextures(gl);
	}

	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		GlHelper.checkGlError(gl);
		gl.glDisable(GL10.GL_DITHER);
        gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT,GL10.GL_FASTEST);
        gl.glEnable(GL10.GL_CULL_FACE);
        gl.glClearColor(0.75f,0.75f,1,1);
        gl.glShadeModel(GL10.GL_FLAT);
        gl.glEnable(GL10.GL_DEPTH_TEST);
        gl.glBlendFunc(gl.GL_SRC_ALPHA,gl.GL_ONE_MINUS_SRC_ALPHA);
		GlHelper.checkGlError(gl);
		
		if (playfield!=null)
			playfield.loadAllTextures(gl);


	}

	public void setpos(Location lastpos) {
		//pos=lastpos;
	}

	public void zoom(int i) {
		// TODO Auto-generated method stub
		
		if (i>0)
			altitude_feet-=100.0f;
		if (i<0)
			altitude_feet+=100.0f;
		if (altitude_feet>100000.0f)
			altitude_feet=100000.0f;
		if (altitude_feet<-100000.0f)
			altitude_feet=-100000.0f;
			
	}

	public void sideways(int i) {
		// TODO Auto-generated method stub
		if (i<0) headturn-=10f;
		if (i>0) headturn+=10f;
		if (headturn<-180) headturn=(float) 180;
		if (headturn>180) headturn=(float) -180;
	}

	public void update(Airspace airspace, AirspaceLookup lookup,
			ElevationStoreIf estore, TextureStore tstore) {
		// TODO Auto-generated method stub
		Log.i("fplan","renderer update called!");
		playfield=new PlayfieldDrawer(estore,tstore,lookup,fontbitmap);		
		
	}

	public void update_tripdata(TripData tripdata) {
		// TODO Finish this when we actually start using trip data.			
	}

	public void debugdump() throws IOException {
		if (playfield!=null)
			playfield.debugdump();
		
	}

}
