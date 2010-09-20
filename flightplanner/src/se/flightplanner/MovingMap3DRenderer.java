package se.flightplanner;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import se.flightplanner.map3d.TriangleStore;
import se.flightplanner.map3d.VertexStore;

import android.opengl.GLSurfaceView.Renderer;

public class MovingMap3DRenderer implements Renderer {
	VertexStore vstore;
	TriangleStore tristore;
    public MovingMap3DRenderer() {
    	vstore=new Vertex
    }

	public void onDrawFrame(GL10 gl) {
		gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
	    gl.glMatrixMode(GL10.GL_MODELVIEW);
	    gl.glLoadIdentity();
	    gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
	    draw_vertices();
	}

	private void draw_vertices() {
		
	}

	public void onSurfaceChanged(GL10 gl, int width, int height) {
        gl.glViewport(0, 0, width, height);

        float ratio = (float) width / height;
        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glFrustumf(-ratio, ratio, -1, 1, 1, 10);
	}

	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		  gl.glDisable(GL10.GL_DITHER);
	}

}
