package se.flightplanner.map3d;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import java.util.Map.Entry;

import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;
import android.util.Log;

import se.flightplanner.Project;
import se.flightplanner.Project.iMerc;
import se.flightplanner.vector.BBTree;

public class TextureStore {

	private HashMap<iMerc,TerrainTexture> store;
	private int zoomlevel;
	
	public void serialize_to_file(Context context,String filename) throws Exception
	{
		OutputStream ofstream=new BufferedOutputStream(context.openFileOutput(filename,Context.MODE_PRIVATE));
		try
		{
			DataOutputStream os=new DataOutputStream(ofstream);
			serialize(os);
			os.close();
			ofstream.close();
		}
		finally
		{
			ofstream.close();
		}		
	}
	
	public static TextureStore deserialize(DataInputStream data) throws IOException
	{
		TextureStore tstore=new TextureStore();
		tstore.store=new HashMap<iMerc,TerrainTexture>();
		int zoomlevels=data.readInt();
		if (zoomlevels!=1) throw new RuntimeException("Only one zoomlevel of textures supported so far.");
		int cnt=0;
		for(int j=0;j<zoomlevels;++j)
		{
			int izoomlevel=data.readInt();
			if (izoomlevel<0 || izoomlevel>15) throw new RuntimeException("Bad zoomlevel");
			tstore.zoomlevel=izoomlevel;
			int numtiles=data.readInt();
			Log.i("fplan","Numtiles to read: "+numtiles);
			for(int i=0;i<numtiles;++i)
			{
				
				Log.i("fplan","Reading #: "+i);
				iMerc merc=Project.imerc2imerc(iMerc.deserialize(data),izoomlevel,13);
				Log.i("fplan","Pos was "+merc);
				int imgsize=data.readInt(); //Our java-implementation doesn't fill imgsize correctly, so we ignore it when reading (the python version does however fill imgsize)
				
				BitmapFactory.Options opts = new BitmapFactory.Options();
				opts.inScaled = false;								
				Bitmap bm=BitmapFactory.decodeStream(data,null,opts);
				if (bm==null) 
					throw new RuntimeException("Couldn't decode png-image");
				tstore.store.put(merc,new TerrainTexture(merc,bm));
				cnt+=1;
				Log.i("fplan","Read #: "+i);
			}
		}
		int magic=data.readInt();
		if (magic!=0x1beef) throw new RuntimeException("Wrong magic number");
		return tstore;
	}
	public void serialize(DataOutputStream data) throws IOException
	{
		data.writeInt(1); ///only one zoomlevel supported right now
		data.writeInt(zoomlevel); //write zoomlevel
		data.writeInt(store.size());			
		for(Entry<iMerc,TerrainTexture> entry : store.entrySet())

		{
			Project.imerc2imerc(entry.getKey(),13,zoomlevel).serialize(data);
			data.writeInt(-1);
			if (entry.getValue().getBitMap().compress(CompressFormat.PNG, 80, data)!=true)
				throw new RuntimeException("Couldn't store compressed image to disk");
		}
		data.writeInt(0x1beef);
	}

	public static TextureStore deserialize_from_file(Context context,String filename) throws Exception
	{
		InputStream ofstream=new BufferedInputStream(context.openFileInput(filename));
		TextureStore data=null;
		try
		{
			
			DataInputStream os=new DataInputStream(ofstream);
			data=TextureStore.deserialize(os);
			os.close();		
		}
		finally
		{
			ofstream.close();			
		}
		return data;
	}
/*
	public Bitmap get(iMerc im) {
		return store.get(im);
	}
	*/
	public Bitmap getRandomBitmap() {
		return store.values().iterator().next().getBitMap();
	}

	public TerrainTexture getTextureAt(iMerc pos) {
		//Log.i("fplan","Tex zoomlevel: "+zoomlevel);
		int zoomgap=13-zoomlevel;
		int boxsize=256<<zoomgap;
		int boxmask=boxsize-1;
		int x=pos.x&(~boxmask);
		int y=pos.y&(~boxmask);
		TerrainTexture t=store.get(new iMerc(x,y));
		return t;
	}

	public Collection<TerrainTexture> getAll() {
		return store.values();
	}

	public void loadAllTextures(GL10 gl) {
		for(TerrainTexture tex:store.values())
		{
			tex.deleteTex(gl);
			tex.getTexName(gl);
		}
		
	}

	public int getZoomLevel() {
		return this.zoomlevel;
	}

	
}
