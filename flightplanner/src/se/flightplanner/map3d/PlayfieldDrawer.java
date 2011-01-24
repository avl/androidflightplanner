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
import android.graphics.Canvas;
import android.opengl.GLUtils;
import android.util.Log;

import se.flightplanner.AirspaceLookup;
import se.flightplanner.Project;
import se.flightplanner.Project.LatLon;
import se.flightplanner.Project.iMerc;
import se.flightplanner.map3d.GuiState.DrawOrder;
import se.flightplanner.map3d.ObserverContext.ObserverState;
import se.flightplanner.map3d.TriangleStore.Indices;
import se.flightplanner.map3d.TriangleStore.RenderTexCb;
import se.flightplanner.map3d.TerrainVertexStore.VertAndColor;

public class PlayfieldDrawer {

	VertexStore3D vs3d;
	TerrainVertexStore vstore;
	TriangleStore tristore;
	ElevationStoreIf elevstore;
	TextureStore tstore;
	ObserverContext observercontext;
	//FontHandler fonthandler;
	GuiState guiState;
	GuiDrawer guiDrawer;
	LodCalc lodc;
	boolean dodump;
	Playfield playfield;
    private AirspaceDrawer airspacedrawer;
    private PointDrawer pointdrawer;
	int deftex;
	int gtex;
	Random rand;
	public PlayfieldDrawer(ElevationStoreIf estore, TextureStore tstore,AirspaceLookup lookup, Bitmap fontbitmap,GuiState gui)
	{
		guiState=gui;
		gtex=0;
		observercontext=new ObserverContext(lookup);
		airspacedrawer=new AirspaceDrawer(observercontext,new AltParser());
		pointdrawer=new PointDrawer(lookup.allObst,lookup.majorAirports);
		
		//fonthandler=new FontHandler(fontbitmap);
		guiDrawer=new GuiDrawer();
		
		
		rand=new Random();
		iMerc p1=Project.latlon2imerc(new LatLon(70,10),13);
		iMerc p2=Project.latlon2imerc(new LatLon(50,20),13);
		this.vs3d=new VertexStore3D(2500);
		vstore=new TerrainVertexStore(2500,tstore.getZoomLevel(),vs3d);
		tristore=new TriangleStore(3500);
		deftex=-1;
		lodc=new LodCalc(480,400); //TODO: Screenheight isn't always 480. Also, tolerance 1000 is too big!
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
	public void draw(final GL10 gl,iMerc observer,int observerElev,float hdg, int width, int height) throws IOException
	{
		try
		{
			guiState.setScreenDim(width,height);
			ObserverState obsstate=this.observercontext.getState();
			GlHelper.checkGlError(gl);
			
			DrawOrder draw=guiState.getDrawOrder();

			float ratio = (float) width / height;
			gl.glViewport(0, 0, width, height);
			gl.glMatrixMode(GL10.GL_PROJECTION);
			gl.glLoadIdentity();
			gl.glFrustumf(-ratio, ratio, -1, 1, 1.0f, 1e4f);
	        
			gl.glMatrixMode(GL10.GL_MODELVIEW);
	        gl.glLoadIdentity();
	        gl.glRotatef(90, 1.0f, 0, 0); //tilt to look to horizon (don't change)
	        int glance=0;
	        if (draw!=null)
	        	glance=draw.glance;
	        gl.glRotatef(-hdg-glance, 0, 0, 1); //compass heading
	        
			GlHelper.checkGlError(gl);
			
			GlHelper.checkGlError(gl);

	        gl.glDisable(GL10.GL_LIGHTING);
	        gl.glDisable(gl.GL_BLEND);
	        gl.glEnable(gl.GL_DEPTH_TEST);
			GlHelper.checkGlError(gl);

	        //gl.glEnable(GL10.GL_TEXTURE_2D);
			//final Texture temptex=tstore.getTextureAt(observer);
			playfield.changeLods(observer, observerElev, vstore, elevstore,lodc,0);
			observercontext.update(observer,(int)hdg);
			airspacedrawer.updateAirspaces(vs3d, tristore);
			pointdrawer.update(observer, vs3d, tristore);
			guiState.maybeDoUpdate(obsstate);
			playfield.prepareForRender();
			airspacedrawer.prepareForRender();
			pointdrawer.prepareForRender();
			//Log.i("fplan","Triangles: "+tristore.getUsedTriangles()+" Vertices: "+vs3d.getUsedVertices());
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

				public void renderTex(TerrainTexture tex,Indices ind) {
					if (tex!=null)
					{					
						//Log.i("fplan","Loading bmap: "+bmap.getWidth()+"x"+bmap.getHeight()+" with middle pixel: "+
						//			bmap.getPixel(128,128));
						GlHelper.checkGlError(gl);
						gl.glEnable(gl.GL_TEXTURE_2D);
						int texname=tex.getTexName(gl);//+gtex;
									
						
						GlHelper.checkGlError(gl);
						gl.glBindTexture(GL10.GL_TEXTURE_2D, texname);
						GlHelper.checkGlError(gl);	
						//gl.glDisable(gl.GL_TEXTURE_2D);
					}
					else
					{
						gl.glDisable(gl.GL_TEXTURE_2D);
						
					}
					GlHelper.checkGlError(gl);
					gl.glDrawElements(gl.GL_TRIANGLES, 3*ind.tricount, gl.GL_UNSIGNED_SHORT,ind.buf);
					GlHelper.checkGlError(gl);
					
				}
				
			});
			
			
			gl.glDisable(gl.GL_CULL_FACE);
			GlHelper.checkGlError(gl);
			guiDrawer.draw(draw,gl,obsstate,width,height);
			GlHelper.checkGlError(gl);
			
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
		/*
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
			
		}*/
		dodump=true;
	}
	public void loadAllTextures(GL10 gl) {
		tstore.loadAllTextures(gl);
	}
	public void onTouch(float x, float y) {
		guiState.onTouchUpdate((int)x,(int)y);
	}
	public void onTouchUp(float x, float y) {
		guiState.onTouchFingerUp((int)x, (int)y);
	}
	public void arrowkeys(int i) {
		guiState.arrowkeys(i);
	}
}
