package se.flightplanner.map3d;

import javax.microedition.khronos.opengles.GL10;

import android.graphics.Bitmap;
import android.opengl.GLUtils;
import android.util.Log;

public class TextureHelpers {

	static public int loadTexture(Bitmap b,GL10 gl)
	{
		int[] textureID=new int[]{-1}; 
		gl.glGenTextures( 1, textureID, 0); 
		// select our current texture 
		int texid=textureID[0];
		if (texid<0)
			throw new RuntimeException("Failed to generate texture name");
		reloadTexture(b, gl, texid);
		return texid;
	}

	public static void reloadTexture(Bitmap b, GL10 gl, int texid) {
		if (b.isRecycled())
			throw new RuntimeException("Bitmap has been recycled!");
		/*try {
			Thread.sleep(25);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		
		if (texid<0)
			throw new RuntimeException("Bad texture name");
		gl.glBindTexture( GL10.GL_TEXTURE_2D, texid);
		if (b==null)
			throw new RuntimeException("Attempt to create texture from null Bitmap!");
		
	
		//do stuff that you do on texture loading 
		///Bitmap bmp = Bitmap.createBitmap(256,256, Bitmap.Config.ARGB_8888); 
	
		//Create a canvas from bitmap 
		//Draw words and shapes to the Canvas 
	
		//Prepare bitmap for next surface
		//Bitmap scaled=Bitmap.createScaledBitmap(b, 64, 64, false);
		//gl.glTexImage2D(gl.GL_TEXTURE_2D, 0, GLUtils.getInternalFormat(scaled), 64, 64, 0, 0, GLUtils.getType(scaled), scaled);
		
		Log.i("fplan","Doing texImage2D");
		GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, b, 0);
		Log.i("fplan","Finished texImage2D");
	
		gl.glTexParameterf(gl.GL_TEXTURE_2D, gl.GL_TEXTURE_MIN_FILTER, gl.GL_NEAREST);
	    gl.glTexParameterf(gl.GL_TEXTURE_2D, gl.GL_TEXTURE_MAG_FILTER, gl.GL_NEAREST);
	
	    //Different possible texture parameters, e.g	        
	    gl.glTexEnvf( gl.GL_TEXTURE_ENV, gl.GL_TEXTURE_ENV_MODE, gl.GL_MODULATE );
	    
		///gl.glBindTexture( gl.GL_TEXTURE_2D, texid);
	}

	static public void unloadTexture(GL10 gl,int texname) {
		gl.glDeleteTextures(1,new int[]{texname},0);
	}

}
