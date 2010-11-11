package se.flightplanner.map3d;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.HashMap;
import java.util.Random;
import java.util.Map.Entry;

import javax.microedition.khronos.opengles.GL10;

import android.graphics.Bitmap;
import android.opengl.GLUtils;
import android.util.Log;

import se.flightplanner.Project;
import se.flightplanner.Project.LatLon;
import se.flightplanner.Project.iMerc;
import se.flightplanner.map3d.TriangleStore.Indices;
import se.flightplanner.map3d.TriangleStore.RenderTexCb;
import se.flightplanner.map3d.TerrainVertexStore.VertAndColor;

public class PlayfieldDrawer {

	VertexStore3D vs3d;
	TerrainVertexStore vstore;
	TriangleStore tristore;
	ElevationStoreIf elevstore;
	TextureStore tstore;
	LodCalc lodc;
	boolean dodump;
	Playfield playfield;
	int deftex;
	int gtex;
	Random rand;
	public PlayfieldDrawer(ElevationStoreIf estore, TextureStore tstore)
	{
		gtex=0;
		rand=new Random();
		iMerc p1=Project.latlon2imerc(new LatLon(70,10),13);
		iMerc p2=Project.latlon2imerc(new LatLon(50,20),13);
		this.vs3d=new VertexStore3D(1000);
		vstore=new TerrainVertexStore(2000,tstore.getZoomLevel(),vs3d);
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
				public ThingIf createThing(TerrainVertexStore vstore,
						TextureStore tstore,ElevationStoreIf estore, int zoomlevel, iMerc m, Stitcher st) {
					return new Thing(m,null,zoomlevel,vstore,tstore,estore,st);
				}
			}
		);
		
	}
	@SuppressWarnings("static-access")
	public void draw(final GL10 gl,iMerc observer,short observerElev) throws IOException
	{
		try
		{
			GlHelper.checkGlError(gl);

	        gl.glDisable(GL10.GL_LIGHTING);
	        gl.glEnable(gl.GL_BLEND);
			GlHelper.checkGlError(gl);

	        //gl.glEnable(GL10.GL_TEXTURE_2D);
			final Texture temptex=tstore.getTextureAt(observer);
			playfield.changeLods(observer, observerElev, vstore, elevstore,lodc,0);
			playfield.prepareForRender();
			if (dodump)
			{
				playfield.completeDebugDump("/sdcard/dump.json");
				dodump=false;
			}
			final VertAndColor va=vstore.vstore3d.getVerticesReadyForRender(vstore, observer,observerElev);
			GlHelper.checkGlError(gl);
	        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
	        gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
	        gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
			gl.glVertexPointer(3, gl.GL_FLOAT, 0, va.vertices);
			gl.glColorPointer(4, gl.GL_UNSIGNED_BYTE, 0, va.colors);
			gl.glTexCoordPointer(2,gl.GL_FLOAT,0,va.texcoords);
	        //Different possible texture parameters, e.g	        
					
			gl.glFrontFace(gl.GL_CCW);
			

			tristore.getIndexForRender(vs3d, new RenderTexCb()
			{

				public void renderTex(Texture tex,Indices ind) {
					// TODO Auto-generated method stub
					//if (tex!=temptex)
					//	tex=null;
					if (tex!=null)
					{					
						//Log.i("fplan","Loading bmap: "+bmap.getWidth()+"x"+bmap.getHeight()+" with middle pixel: "+
						//			bmap.getPixel(128,128));
						GlHelper.checkGlError(gl);
						//GLUtils.
						gl.glEnable(gl.GL_TEXTURE_2D);
						int texname=tex.getTexName(gl);//+gtex;
						//gl.glFinish();
						//if (rand.nextInt(5)==0)
						//	texname+=gtex;
						//texname=gtex;
						/*+itr[0];
						itr[0]+=1-0;
						if (itr[0]>9)
							itr[0]=0;
						*/

						//Log.i("fplan","Drawing tex "+texname);
						/*
						if (deftex == -1) {
							Bitmap bmap = tstore.getRandomBitmap();
							GlHelper.checkGlError(gl);
							deftex = Texture.loadTexture(bmap, gl);
							GlHelper.checkGlError(gl);
						}
						*/
						
						
						GlHelper.checkGlError(gl);
						//texname=gtex;//rand.nextInt(9)+1;
						//Log.i("fplan","Drawing tex: "+texname);
						gl.glBindTexture(GL10.GL_TEXTURE_2D, texname);
						GlHelper.checkGlError(gl);	
						//gl.glDisable(gl.GL_TEXTURE_2D);
					}
					else
					{
						gl.glDisable(gl.GL_TEXTURE_2D);
					}
					GlHelper.checkGlError(gl);
					//gl.gl
					gl.glDrawElements(gl.GL_TRIANGLES, 3*ind.tricount, gl.GL_UNSIGNED_SHORT,ind.buf);
					GlHelper.checkGlError(gl);
					
				}
				
			});
			/*		
			int tottris=0;
			for(Entry<Texture, Indices> entry : inds.entrySet())
			{
				Texture tex=entry.getKey();
				if (tex!=null)
				{					
					//Log.i("fplan","Loading bmap: "+bmap.getWidth()+"x"+bmap.getHeight()+" with middle pixel: "+
					//			bmap.getPixel(128,128));
					GlHelper.checkGlError(gl);
					int texname=tex.getTexName(gl);
					gl.glBindTexture(GL10.GL_TEXTURE_2D, texname);
					GlHelper.checkGlError(gl);
					gl.glEnable(gl.GL_TEXTURE_2D);
					//gl.glDisable(gl.GL_TEXTURE_2D);
				}
				else
				{
					gl.glDisable(gl.GL_TEXTURE_2D);
				}
				GlHelper.checkGlError(gl);
				Indices ind=entry.getValue();
				gl.glDrawElements(gl.GL_TRIANGLES, 3*ind.tricount, gl.GL_UNSIGNED_SHORT,ind.buf);
				GlHelper.checkGlError(gl);
				tottris+=entry.getValue().tricount;
			Log.i("fplan","Drawed "+tottris+" triangles");
			}
			*/
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
		gtex+=1;
		if (gtex>9) gtex=0;
		Log.i("fplan","Gtex:"+gtex);
		for(Texture tex:tstore.getAll())
		{
			int g=tex.getTexNameIfExists();
			if (g==gtex)
			{
				Log.i("fplan"," This is at : "+Project.imerc2latlon(tex.getPos(), 13));
			}
			
		}
		//dodump=true;
	}
	public void loadAllTextures(GL10 gl) {
		tstore.loadAllTextures(gl);
	}
}
