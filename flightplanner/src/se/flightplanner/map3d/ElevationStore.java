package se.flightplanner.map3d;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import se.flightplanner.Project.Merc;
import se.flightplanner.Project.iMerc;
import se.flightplanner.vector.BBTree;
import se.flightplanner.vector.BoundingBox;
import se.flightplanner.vector.BBTree.Item;

public class ElevationStore {

	private static final int dim=64;
	private BBTree elevs;
	private ArrayList<ElevTile> alltiles;
	
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
		short zoomlevel;
		short[] data;
		public Elev get(iMerc m)
		{
			int dx=(m.x-m1.x)/(m2.x-m1.x);
			int dy=(m.y-m1.y)/(m2.y-m1.y);
			int ix=(dim*dx);
			int iy=(dim*dy);
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
		public static ElevTile deserialize(DataInputStream data) throws IOException {
			ElevTile e=new ElevTile();
			e.m1=iMerc.deserialize(data);
			e.m2=iMerc.deserialize(data);
			e.zoomlevel=data.readShort();
			e.data=new short[2*dim*dim];
			for(int i=0;i<2*dim*dim;++i)
				e.data[i]=data.readShort();
			e.box=new BoundingBox(e.m1.x,e.m1.y,e.m2.x,e.m2.y);
			return e;
		}		
		public void serialize(DataOutputStream strm) throws IOException {
			m1.serialize(strm);
			m2.serialize(strm);
			strm.writeShort(zoomlevel);
			for(int i=0;i<dim*dim;++i)
				strm.writeShort(data[i]);			
		}
	}
	public static ElevationStore deserialize(DataInputStream data) throws IOException
	{
		int numtiles=data.readInt();
		ElevationStore estore=new ElevationStore();
		estore.alltiles=new ArrayList<ElevTile>();
		ArrayList<BBTree.Item> elevlist=new ArrayList<BBTree.Item>();
		for(int i=0;i<numtiles;++i)
		{
			ElevTile e=ElevTile.deserialize(data);
			elevlist.add(e);
			estore.alltiles.add(e);
		}
		estore.elevs=new BBTree(elevlist,0.5);
		return estore;
	}
	public void serialize(DataOutputStream data) throws IOException
	{		
		data.writeInt(alltiles.size());
		for(ElevTile t : alltiles)
			t.serialize(data);
	}
	public Elev get(iMerc pos,int level)
	{
		ElevTile et=getTile(pos,level);
		return et.get(pos);
	}
	public ElevTile getTile(iMerc pos,int level)
	{
		BoundingBox box=new BoundingBox(pos.x,pos.y,pos.x+1,pos.y+1);
		ElevTile besttile=null;
		int bestzoom=0;
		for(Item i:elevs.overlapping(box))
		{
			ElevTile tile=(ElevTile)i.payload();
			if (tile.m1.x>pos.x || tile.m1.y>pos.y ||
			    tile.m2.x<=pos.x || tile.m2.y<=pos.y)
				continue; //doesn't contain the asked for merc.
			if (Math.abs(tile.zoomlevel-bestzoom)<bestzoom || besttile==null)
			{
				besttile=tile;
				bestzoom=tile.zoomlevel;
			}
		}
		return besttile;
	}
	
}
