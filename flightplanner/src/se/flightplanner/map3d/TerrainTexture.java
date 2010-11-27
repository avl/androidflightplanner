package se.flightplanner.map3d;

import javax.microedition.khronos.opengles.GL10;

import se.flightplanner.Project.iMerc;

import android.graphics.Bitmap;
import android.util.Log;

public class TerrainTexture {
	private iMerc pos;
	private Bitmap bitmap;
	private int texname;
	public TerrainTexture(iMerc pos, Bitmap bm)
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
		TerrainTexture t=(TerrainTexture)oo;
		return pos.equals(t.pos);
	}
	
	int getTexName(GL10 gl)
	{
		if (texname==-1)
		{
			texname=TextureHelpers.loadTexture(bitmap,gl);
			Log.i("fplan","Loading texture at : "+this.pos+" as texid: "+texname);
		}
		return texname;
	}
	
	public int getTexNameIfExists() {
		return texname;
	}
	public void deleteTex(GL10 gl) {
		if (texname!=-1)
		{
			TextureHelpers.unloadTexture(gl,texname);
		}
		texname=-1;
	}
	
}
