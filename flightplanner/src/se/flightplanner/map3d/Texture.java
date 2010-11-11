package se.flightplanner.map3d;

import javax.microedition.khronos.opengles.GL10;

import se.flightplanner.Project.iMerc;

import android.graphics.Bitmap;
import android.opengl.GLUtils;
import android.util.Log;

public class Texture {
	private iMerc pos;
	private Bitmap bitmap;
	private int texname;
	public Texture(iMerc pos, Bitmap bm)
	{
		this.pos=pos;
		this.bitmap=bm;
		this.texname=-1;
	}
	public iMerc getPos()
	{
		return pos;
	}
	public Bitmap getBitMap()
	{
		return bitmap;
	}
	@Override
	public int hashCode()
	{
		return pos.hashCode();
	}
	@Override
	public boolean equals(Object oo)
	{
		Texture t=(Texture)oo;
		return pos.equals(t.pos);
	}
	
	int getTexName(GL10 gl)
	{
		if (texname==-1)
		{
			texname=loadTexture(bitmap,gl);
			Log.i("fplan","Loading texture at : "+this.pos+" as texid: "+texname);
		}
		return texname;
	}
	
	static public int loadTexture(Bitmap b,GL10 gl)
	{
		int[] textureID=new int[]{-1}; 
		gl.glGenTextures( 1, textureID, 0); 
		// select our current texture 
		gl.glBindTexture( GL10.GL_TEXTURE_2D, textureID[0]);
		if (textureID[0]==-1)
			throw new RuntimeException("Failed to generate texture name");
		if (b==null)
			throw new RuntimeException("Attempt to create texture from null Bitmap!");
		

		//do stuff that you do on texture loading 
		///Bitmap bmp = Bitmap.createBitmap(256,256, Bitmap.Config.ARGB_8888); 

		//Create a canvas from bitmap 
		//Draw words and shapes to the Canvas 

		//Prepare bitmap for next surface
		//Bitmap scaled=Bitmap.createScaledBitmap(b, 64, 64, false);
		//gl.glTexImage2D(gl.GL_TEXTURE_2D, 0, GLUtils.getInternalFormat(scaled), 64, 64, 0, 0, GLUtils.getType(scaled), scaled);
		
		GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, b, 0);

		gl.glTexParameterf(gl.GL_TEXTURE_2D, gl.GL_TEXTURE_MIN_FILTER, gl.GL_NEAREST);
        gl.glTexParameterf(gl.GL_TEXTURE_2D, gl.GL_TEXTURE_MAG_FILTER, gl.GL_NEAREST);

        //Different possible texture parameters, e.g	        
        gl.glTexEnvf( gl.GL_TEXTURE_ENV, gl.GL_TEXTURE_ENV_MODE, gl.GL_MODULATE );
        
		gl.glBindTexture( gl.GL_TEXTURE_2D, textureID[0]);
		// enable texturing
		return textureID[0];
	}
	public int getTexNameIfExists() {
		return texname;
	}
	public void deleteTex(GL10 gl) {
		if (texname!=-1)
			gl.glDeleteTextures(1,new int[]{texname},0);
		texname=-1;
	}
	
}
