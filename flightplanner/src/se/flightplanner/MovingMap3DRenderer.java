package se.flightplanner;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import se.flightplanner.Project.LatLon;
import se.flightplanner.Project.iMerc;
import se.flightplanner.map3d.ElevationStore;
import se.flightplanner.map3d.LodCalc;
import se.flightplanner.map3d.Playfield;
import se.flightplanner.map3d.PlayfieldDrawer;
import se.flightplanner.map3d.Stitcher;
import se.flightplanner.map3d.Thing;
import se.flightplanner.map3d.ThingFactory;
import se.flightplanner.map3d.ThingIf;
import se.flightplanner.map3d.TriangleStore;
import se.flightplanner.map3d.VertexStore;

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
    public MovingMap3DRenderer() {
		// vstore=new Vertex
		a=0.0f;
		headturn=0.0f;
		pos=new Location("gps");
		pos.setLatitude(59.0);
		pos.setLongitude(18.0);
		pos.setBearing((float) 360.0);

	
	}

	public void onDrawFrame(GL10 gl) {
		//TODO: Move the below to OnSurfaceChanged
		float ratio = (float) width / height;
		gl.glViewport(0, 0, width, height);
		gl.glMatrixMode(GL10.GL_PROJECTION);
		gl.glLoadIdentity();
		gl.glFrustumf(-ratio, ratio, -1, 1, 1.0f, 1e4f);
		
		gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
		gl.glMatrixMode(GL10.GL_MODELVIEW);
		gl.glLoadIdentity();
		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		draw_vertices(gl);
		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
		}
	}

	private void draw_vertices(GL10 gl) {
		
		
		
		LatLon cameraLatLon=new LatLon(60,18);//pos.getLatitude(),pos.getLongitude());
		iMerc cameramerc=Project.latlon2imerc(cameraLatLon, 13);

		LatLon obstLatLon=new LatLon(59,18);
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
		
		/*
		float vertices[] = 
		{ -65,90,10,
		   98,90,10,
		   -65,254,10,
		   98,254,10
		};
		
		byte colors[] = { (byte)-1,0,0,
						0,(byte)-1,0,
						0,0,(byte)-1,
						(byte)-1,(byte)-1,(byte)-1};

		short indices[] = {0,2,1,
					2,3,1};
		*/
		
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
		
		
		
		ByteBuffer vbb = ByteBuffer.allocateDirect(vertices.length * 4);
		vbb.order(ByteOrder.nativeOrder());
		mVertexBuffer = vbb.asFloatBuffer();
		mVertexBuffer.put(vertices);
		mVertexBuffer.position(0);

		ByteBuffer cbb = ByteBuffer.allocateDirect(colors.length * 4);
		cbb.order(ByteOrder.nativeOrder());
		cbb.put(colors);
		cbb.position(0);

		mIndexBuffer = ByteBuffer.allocateDirect(indices.length*2);
		mIndexBuffer.order(ByteOrder.nativeOrder());
		ShortBuffer si=mIndexBuffer.asShortBuffer();
		si.put(indices);
		si.position(0);

        gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
        gl.glMatrixMode(GL10.GL_MODELVIEW);
        gl.glLoadIdentity();
        a+=0.1;
        ///Log.i("fplan","A:"+a);
        gl.glRotatef(-90, 1.0f, 0, 0); //tilt to look to horizon (don't change)
        float hdg=pos.getBearing()+headturn;//a*10.0f;
        float altitude=altitude_feet;//3.0f;
        //float f=1.0f/65536.0f;
        
        //gl.glRotatef(hdg, 0, 0, 1); //compass heading
        
        //gl.glTranslatef(0,20.0f,0);
        //gl.glTranslatef(10.0f*(float)Math.cos(a),10.0f*(float)Math.sin(a),0);
        //gl.glRotatef((float)(-3.14159/2.0), 1.0f, 0, 0);
        //gl.glTranslatef(-cameramerc.x, -cameramerc.y, -altitude);
        //Log.i("fplan","Camerapos: "+cameramerc.x+","+cameramerc.y+","+altitude);
        gl.glPushMatrix();
        //gl.glTranslatef(obstmerc.x-cameramerc.x, obstmerc.y-cameramerc.y, 0.0f-altitude);
        Log.i("fplan","Obstmerc: "+obstmerc.x+","+obstmerc.y+", obstsize:"+onec);
        //gl.glRotatef((float)(0.5)+a,        0, 1, 0);
        //gl.glRotatef((float)(0.5*0.25)+a,  1, 0, 0);
        
        
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
		
        gl.glFrontFace(gl.GL_CCW);
		gl.glVertexPointer(3, gl.GL_FLOAT, 0, mVertexBuffer);
		gl.glColorPointer(3, gl.GL_UNSIGNED_BYTE, 0, cbb);
		gl.glDrawElements(gl.GL_TRIANGLES, 36, gl.GL_UNSIGNED_SHORT,
				si);
		
        gl.glPopMatrix();
		/*if (playfield!=null)
			playfield.draw(gl,cameramerc,(short)altitude);
		*/

	}

	public void onSurfaceChanged(GL10 gl, int width, int height) {

		
		this.width=width;
		this.height=height;
	}

	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		gl.glDisable(GL10.GL_DITHER);
        gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT,
                GL10.GL_FASTEST);
        gl.glDisable(GL10.GL_CULL_FACE);
        gl.glClearColor(1,1,1,1);
        gl.glShadeModel(GL10.GL_SMOOTH);
        ///gl.glEnable(GL10.GL_DEPTH_TEST);
        gl.glDisable(GL10.GL_DEPTH_TEST);

	}

	public void setpos(Location lastpos) {
		//pos=lastpos;
	}

	public void zoom(int i) {
		// TODO Auto-generated method stub
		
		if (i>0)
			altitude_feet-=1000.0f;
		if (i<0)
			altitude_feet+=1000.0f;
		if (altitude_feet>10000.0f)
			altitude_feet=10000.0f;
		if (altitude_feet<-10000.0f)
			altitude_feet=-10000.0f;
			
	}

	public void sideways(int i) {
		// TODO Auto-generated method stub
		if (i<0) headturn-=10f;
		if (i>0) headturn+=10f;
		if (headturn<-180) headturn=(float) 180;
		if (headturn>180) headturn=(float) -180;
	}

	public void update(Airspace airspace, AirspaceLookup lookup,
			ElevationStore estore) {
		// TODO Auto-generated method stub
		Log.i("fplan","renderer update called!");
		playfield=new PlayfieldDrawer(estore);		
		
	}

	public void update_tripdata(TripData tripdata) {
		// TODO Auto-generated method stub
		
	}

}
