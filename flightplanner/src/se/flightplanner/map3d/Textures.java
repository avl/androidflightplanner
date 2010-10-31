package se.flightplanner.map3d;

import javax.microedition.khronos.opengles.GL10;

import android.graphics.Bitmap;
import android.opengl.GLUtils;

public class Textures {

	static public int loadTexture(Bitmap b,GL10 gl)
	{
		int[] textureID=new int[1]; 
		gl.glGenTextures( 1, textureID, 0); 
		// select our current texture 
		gl.glBindTexture( gl.GL_TEXTURE_2D, textureID[0]); 

		//do stuff that you do on texture loading 
		///Bitmap bmp = Bitmap.createBitmap(256,256, Bitmap.Config.ARGB_8888); 

		//Create a canvas from bitmap 
		//Draw words and shapes to the Canvas 

		//Prepare bitmap for next surface 
		GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, b, 0);
		// enable texturing
		return textureID[0];
	}
	
}
