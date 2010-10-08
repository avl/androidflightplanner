package se.flightplanner;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import se.flightplanner.Project.LatLon;
import se.flightplanner.Project.iMerc;
import se.flightplanner.map3d.ElevationStore;
import se.flightplanner.map3d.LodCalc;
import se.flightplanner.map3d.Playfield;
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

	VertexStore vstore;
	TriangleStore tristore;
	LodCalc lodc;
	Location pos;
    private IntBuffer   mVertexBuffer;
    private IntBuffer   mColorBuffer;
    private ByteBuffer  mIndexBuffer;
    private float a;
    float altitude_feet;
    private float headturn;
    private Playfield playfield;
	private ElevationStore elevstore;
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
		gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
		gl.glMatrixMode(GL10.GL_MODELVIEW);
		gl.glLoadIdentity();
		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		draw_vertices(gl);
	}

	private void draw_vertices(GL10 gl) {
		LatLon cameraLatLon=new LatLon(59+0.001,18);//pos.getLatitude(),pos.getLongitude());
		iMerc cameramerc=Project.latlon2imerc(cameraLatLon, 13);

		LatLon obstLatLon=new LatLon(59,18);
		iMerc obstmerc=Project.latlon2imerc(obstLatLon, 13);

		int one = 0x10000;
		int vertices[] = { -one, -one, -one, one, -one, -one, one, one, -one,
				-one, one, -one, -one, -one, one, one, -one, one, one, one,
				one, -one, one, one, };

		int colors[] = { 0, 0, 0, one, one, 0, 0, one, one, one, 0, one, 0,
				one, 0, one, 0, 0, one, one, one, 0, one, one, one, one, one,
				one, 0, one, one, one, };

		byte indices[] = { 0, 4, 5, 0, 5, 1, 1, 5, 6, 1, 6, 2, 2, 6, 7, 2, 7,
				3, 3, 7, 4, 3, 4, 0, 4, 7, 6, 4, 6, 5, 3, 0, 1, 3, 1, 2 };
		ByteBuffer vbb = ByteBuffer.allocateDirect(vertices.length * 4);
		vbb.order(ByteOrder.nativeOrder());
		mVertexBuffer = vbb.asIntBuffer();
		mVertexBuffer.put(vertices);
		mVertexBuffer.position(0);

		ByteBuffer cbb = ByteBuffer.allocateDirect(colors.length * 4);
		cbb.order(ByteOrder.nativeOrder());
		mColorBuffer = cbb.asIntBuffer();
		mColorBuffer.put(colors);
		mColorBuffer.position(0);

		mIndexBuffer = ByteBuffer.allocateDirect(indices.length);
		mIndexBuffer.put(indices);
		mIndexBuffer.position(0);

        gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
        gl.glMatrixMode(GL10.GL_MODELVIEW);
        gl.glLoadIdentity();
        a+=0.1;
        ///Log.i("fplan","A:"+a);
        gl.glRotatef(-90, 1.0f, 0, 0); //tilt to look to horizon (don't change)
        float hdg=pos.getBearing()+headturn;//a*10.0f;
        float altitude=3.0f;
        gl.glRotatef(hdg, 0, 0, 1); //compass heading
        //gl.glTranslatef(0,10.0f,0);
        //gl.glTranslatef(10.0f*(float)Math.cos(a),10.0f*(float)Math.sin(a),0);
        //gl.glRotatef((float)(-3.14159/2.0), 1.0f, 0, 0);
        gl.glTranslatef(obstmerc.x, obstmerc.y, 0.0f);
        //gl.glRotatef((float)(0.5)+a,        0, 1, 0);
        //gl.glRotatef((float)(0.5*0.25)+a,  1, 0, 0);
        gl.glTranslatef(-cameramerc.x, -cameramerc.y, -altitude);
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
		
		
		gl.glFrontFace(gl.GL_CCW);
		gl.glVertexPointer(3, gl.GL_FIXED, 0, mVertexBuffer);
		gl.glColorPointer(4, gl.GL_FIXED, 0, mColorBuffer);
		gl.glDrawElements(gl.GL_TRIANGLES, 36, gl.GL_UNSIGNED_BYTE,
				mIndexBuffer);

	}

	public void onSurfaceChanged(GL10 gl, int width, int height) {
		gl.glViewport(0, 0, width, height);

		float ratio = (float) width / height;
		gl.glMatrixMode(GL10.GL_PROJECTION);
		gl.glLoadIdentity();
		gl.glFrustumf(-ratio, ratio, -1, 1, 1, 100000);
	}

	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		gl.glDisable(GL10.GL_DITHER);
        gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT,
                GL10.GL_FASTEST);
        gl.glEnable(GL10.GL_CULL_FACE);
        gl.glClearColor(1,1,1,1);
        gl.glShadeModel(GL10.GL_SMOOTH);
        gl.glEnable(GL10.GL_DEPTH_TEST);

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
		if (altitude_feet<1000.0f)
			altitude_feet=1000.0f;
	}

	public void sideways(int i) {
		// TODO Auto-generated method stub
		if (i<0) headturn-=10f;
		if (i>0) headturn+=10f;
		if (headturn<-180) headturn=(float) 180;
		if (headturn>180) headturn=(float) -180;
	}

	public void update(Airspace airspace, AirspaceLookup lookup,
			TripData tripdata) {
		// TODO Auto-generated method stub
		iMerc p1=Project.latlon2imerc(new LatLon(60,16),13);
		iMerc p2=Project.latlon2imerc(new LatLon(58,20),13);
		vstore=new VertexStore(2000);
		tristore=new TriangleStore(1000);
		lodc=new LodCalc(480,1000); //TODO: Screenheight isn't always 480. Also, tolerance 1000 is too big!
		this.elevstore=tripdata.getElevStore();
		if (this.elevstore==null)
			throw new RuntimeException("Elevstore is null.");
		playfield=new Playfield(p1,p2,vstore,tristore,lodc,elevstore,
				new ThingFactory()
			{
				public ThingIf createThing(VertexStore vstore,
						ElevationStore estore, int zoomlevel, iMerc m, Stitcher st) {
					return new Thing(m,null,zoomlevel,vstore,estore,st);
				}
			}
		);
		
	}

}
