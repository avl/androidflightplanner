package se.flightplanner.map3d;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.Map.Entry;

import android.content.Context;
import android.util.Log;

import se.flightplanner.Airspace;
import se.flightplanner.Project;
import se.flightplanner.Project.Merc;
import se.flightplanner.Project.iMerc;
import se.flightplanner.vector.BBTree;
import se.flightplanner.vector.BoundingBox;
import se.flightplanner.vector.BBTree.Item;

public class ElevationStore {

	private static final int dim=64;
	static private class Level
	{
		BBTree tree;
		ArrayList<ElevTile> alltiles;
	}
	HashMap<Integer,Level> levels;
	public ElevationStore(int zero)
	{
		if (zero!=0) throw new RuntimeException("0-init ElevationStore is the only one that can be created without data");
		levels=new HashMap<Integer,Level>();		
	}
	private ElevationStore()
	{
	}
	static public class Elev
	{
		public Elev(short lo,short hi)
		{
			loElev=lo;
			hiElev=hi;			
		}
		public short hiElev;
		public short loElev;
	}
	static public class ElevTile implements BBTree.Item
	{
		BoundingBox box;
		iMerc m1;
		iMerc m2;
		byte zoomlevel;
		short[] data;
		public Elev get(iMerc m)
		{
			int ix=(int)(dim*(long)(m.x-m1.x)/(m2.x-m1.x));
			int iy=(int)(dim*(long)(m.y-m1.y)/(m2.y-m1.y));
			if (ix>=dim || iy>=dim || ix<0 || iy<0)
				return null;
			return new Elev(data[2*(iy*dim+ix)+0],data[2*(iy*dim+ix)+1]);			
		}
		public BoundingBox bb() {
			return box;
		}
		public Object payload() {
			return this;
		}
		public static ElevTile deserialize(DataInputStream data,byte pzoomlevel) throws IOException {
			ElevTile e=new ElevTile();
			e.zoomlevel=pzoomlevel;
			e.m1=iMerc.deserialize(data);
			e.m2=new iMerc(e.m1.x+dim,e.m1.y+dim);
			e.m1=Project.imerc2imerc(e.m1, e.zoomlevel, 13);
			e.m2=Project.imerc2imerc(e.m2, e.zoomlevel, 13);
			e.data=new short[2*dim*dim];
			int len=data.readInt();
			if (len!=dim*dim*4)
				throw new RuntimeException("Bad binary format of heightmap");
			
			ByteBuffer bbuf=ByteBuffer.allocate(2*2*dim*dim);			
			if (bbuf.array().length!=dim*dim*4)
				throw new RuntimeException("Failed when allocating buf for elev tile");
			int tot=0;
			for(;tot<len;)
			{
				int l=data.read(bbuf.array(),tot,len-tot);
				if (l==-1) throw new RuntimeException("Unexpected end of file in Elev tile reader");
				tot+=l;
			}
			if (tot!=dim*dim*4)
				throw new RuntimeException("Failed when reading elevation tile:"+tot+" should:"+len);
			ShortBuffer shbuf=bbuf.asShortBuffer();
			shbuf.position(0);
			shbuf.get(e.data);
			//for(int i=0;i<2*dim*dim;++i)
			//	e.data[i]=data.readShort();
			
			e.box=new BoundingBox(e.m1.x,e.m1.y,e.m2.x,e.m2.y);
			return e;
		}		
		public void serialize(DataOutputStream strm,int zoomlevel) throws IOException {
			iMerc temp=Project.imerc2imerc(m1, 13, zoomlevel);
			temp.serialize(strm);
			strm.writeInt(dim*dim*4);
			ByteBuffer bbuf=ByteBuffer.allocate(2*2*dim*dim);			
			ShortBuffer shbuf=bbuf.asShortBuffer();
			shbuf.put(data);
			bbuf.position(0);
			strm.write(bbuf.array());
			//for(int i=0;i<2*dim*dim;++i)
			//	strm.writeShort(data[i]);			
		}
	}
	public static ElevationStore deserialize(DataInputStream data) throws IOException
	{
		ElevationStore estore=new ElevationStore();
		estore.levels=new HashMap<Integer,Level>();
		int zoomlevels=data.readInt();
		if (zoomlevels<=0 || zoomlevels>15) throw new RuntimeException("Bad zoomlevel count");
		int cnt=0;
		for(int j=0;j<zoomlevels;++j)
		{
			ArrayList<BBTree.Item> elevlist=new ArrayList<BBTree.Item>();
			int izoomlevel=data.readInt();
			if (izoomlevel<0 || izoomlevel>15) throw new RuntimeException("Bad zoomlevel");
			byte zoomlevel=(byte)izoomlevel;
			int numtiles=data.readInt();
			Level level=new Level();
			level.alltiles=new ArrayList<ElevTile>();
			for(int i=0;i<numtiles;++i)
			{
				ElevTile e=ElevTile.deserialize(data,zoomlevel);
				elevlist.add(e);
				level.alltiles.add(e);
				cnt+=1;
			}
			level.tree=new BBTree(elevlist,0.5);
			estore.levels.put(izoomlevel, level);
		}
		int magic=data.readInt();
		if (magic!=0x1beef) throw new RuntimeException("Wrong magic number");
		return estore;
	}
	public void serialize(DataOutputStream data) throws IOException
	{
		Set<Entry<Integer,Level>> entries=levels.entrySet();
		data.writeInt(entries.size());
		for(Entry<Integer,Level> entry : entries)
		{
			int zoomlevel=entry.getKey();
			Level level=entry.getValue();
			data.writeInt(zoomlevel); //write zoomlevel
			data.writeInt(level.alltiles.size());			
			for(ElevTile t : level.alltiles)
				t.serialize(data,zoomlevel);
		}
		data.writeInt(0x1beef);
	}
	public Elev get(iMerc pos,int level)
	{
		ElevTile et=getTile(pos,level);
		if (et==null)
			return null;
		return et.get(pos);
	}
	public ElevTile getTile(iMerc pos,int curlevel)
	{
		BoundingBox box=new BoundingBox(pos.x,pos.y,pos.x+1,pos.y+1);
		for(;curlevel>=0;--curlevel)
		{
			Level l=levels.get(curlevel);
			if (l==null) continue;
			for(Item i:l.tree.overlapping(box))
			{
				ElevTile tile=(ElevTile)i.payload();
				if (tile.m1.x>pos.x || tile.m1.y>pos.y ||
				    tile.m2.x<=pos.x || tile.m2.y<=pos.y)
					continue; //doesn't contain the asked for merc.
				return tile;
			}
		}
		return null;
	}
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
	
	public static ElevationStore deserialize_from_file(Context context,String filename) throws Exception
	{
		InputStream ofstream=new BufferedInputStream(context.openFileInput(filename));
		ElevationStore data=null;
		try
		{
			
			DataInputStream os=new DataInputStream(ofstream);
			data=ElevationStore.deserialize(os);//(Airspace)os.readObject();
			os.close();		
		}
		finally
		{
			ofstream.close();			
		}
		return data;
	}
	
}
