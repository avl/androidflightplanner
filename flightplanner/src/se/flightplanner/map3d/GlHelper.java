package se.flightplanner.map3d;

import javax.microedition.khronos.opengles.GL10;

public class GlHelper {

	public static void checkGlError(GL10 gl)
	{
		int err=gl.glGetError();
		if (err!=gl.GL_NO_ERROR)
			throw new RuntimeException("GL error:"+err);		
	}

}
