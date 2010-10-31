package se.flightplanner.map3d;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.Map.Entry;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;

import se.flightplanner.Project.iMerc;
import se.flightplanner.vector.BBTree;

public class TextureStore {

	private HashMap<iMerc,Bitmap> store;
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
		tstore.store=new HashMap<iMerc,Bitmap>();
		int zoomlevels=data.readInt();
		if (zoomlevels!=1) throw new RuntimeException("Only one zoomlevel of textures supported so far.");
		int cnt=0;
		for(int j=0;j<zoomlevels;++j)
		{
			int izoomlevel=data.readInt();
			if (izoomlevel<0 || izoomlevel>15) throw new RuntimeException("Bad zoomlevel");
			tstore.zoomlevel=izoomlevel;
			int numtiles=data.readInt();
			for(int i=0;i<numtiles;++i)
			{
				iMerc merc=iMerc.deserialize(data);
				int imgsize=data.readInt();
				Bitmap bm=BitmapFactory.decodeStream(data);
				tstore.store.put(merc,bm);
				cnt+=1;
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
		for(Entry<iMerc,Bitmap> entry : store.entrySet())
		{
			entry.getKey().serialize(data);
			entry.getValue().compress(CompressFormat.PNG, 80, data);			
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

	public Bitmap get(iMerc im) {
		return store.get(im);
	}
	public Bitmap getRandomBitmap() {
		return store.values().iterator().next();
	}

	
}
