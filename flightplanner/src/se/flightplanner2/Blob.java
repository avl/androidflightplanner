package se.flightplanner2;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import se.flightplanner2.Project.iMerc;

public class Blob {
	private int tilesize=256;
	public int getX1() {
		return x1;
	}
	public int getY1() {
		return y1;
	}
	public int getX2() {
		return x2;
	}
	public int getY2() {
		return y2;
	}
	private int x1;
	private int y1;
	private int x2;
	private int y2;
	private int sx;
	private int sy;
	private int zoomlevel;
	private long size;
	
	
	
	public static class TileNumber
	{
		public int x;
		public int y;
	};
	TileNumber get_tile_number(iMerc m)
	{
		if (m.x>x2) return null;
		if (m.y>y2) return null;
		if (m.x<x1) return null;
		if (m.y<y1) return null;
		
		if (!(m.getX()>=0 && m.getY()>=0)) return null;		
		if (m.getX()%tilesize!=0 || m.getY()%tilesize!=0)
			throw new RuntimeException("Invalid tilesize");
		TileNumber t=new TileNumber();
		t.x=(m.getX()-x1)/tilesize;
		t.y=(m.getY()-y1)/tilesize;
		return t;
	}
	private RandomAccessFile raf;
	public Blob(String filename,int tilesize_) throws IOException
	{
		this.tilesize=tilesize_;	
		raf=new RandomAccessFile(filename,"r");		
		size=raf.length();
        x1=raf.readInt();
        y1=raf.readInt();
        x2=raf.readInt();
        y2=raf.readInt();
        zoomlevel=raf.readInt();

        TileNumber t1=get_tile_number(new iMerc(x1,y1));
        TileNumber t2=get_tile_number(new iMerc(x2,y2));
        if (t1==null || t2==null)
        	throw new RuntimeException("Corrupt inconsistent map data");
        sx=t2.x-t1.x+1;
        sy=t2.y-t1.y+1;
        if (!(sx>0 && sy>0))
        	throw new RuntimeException("Bad sx- and sy-variables");
	}
	public long getSize()
	{
		return size;
	}
	private int seekright(iMerc coords) throws IOException
	{
		return seekright(coords,0);
	}
	private int seekright(iMerc coords,int offset) throws IOException
	{
	    TileNumber t=get_tile_number(coords);
	    if (t==null)
	    	return -1;
	    if (t.x<0 || t.x>=sx || t.y<0 || t.y>=sy)
	    {
	    	Log.i("fplan","Sought tile is outside of map");
	        return -1;
	    }
	    long pos=20+4*((long)t.x+(long)t.y*(long)(sx));
	    raf.seek(pos);
	    long datapos=raf.readInt();
	    if (datapos<0)
	    	datapos+=(1L<<32L);
	    if (datapos>size-4)
	    	throw new IOException("Bad size"); 
	    raf.seek(datapos);
	    if (datapos<=0)
	    {
	    	Log.i("fplan","Corrupt blob");
	    	return -1;
	    }
	    int imagesize=raf.readInt();
	    if (imagesize<0)
	    	throw new IOException("Unexpected imagesize");
	    if (offset!=0)
	    {
		    if (offset>imagesize)
		    	throw new IOException("Bad offset: "+offset+" imagesize: "+imagesize);	    
		    raf.seek(datapos+4+offset);
		    return imagesize-offset;
	    }
	    return imagesize;
	}
    byte[] get_tile(iMerc coords,int expected_maxsize) throws IOException
    {    	
    	int imagesize=seekright(coords);
    	if (imagesize<0)
    		return null;
    	if (imagesize>expected_maxsize)
    		throw new IOException("Tile was bigger than expected max: "+expected_maxsize+", it was: "+imagesize);
        
        byte[] ret=new byte[imagesize];
        raf.readFully(ret);
        return ret;
    }
	Bitmap get_bitmap(iMerc coords) throws IOException
	{
		/*
    	int imagesize=seekright(coords);
    	if (imagesize==-1)
    		return null;
		Log.i("fplan","Ready to read out bitmap, size "+imagesize);
		*/
		//Log.i("fplan.bm","Loading bitmap: "+coords+" zoom: "+zoomlevel);
		BitmapFactory.Options opts = new BitmapFactory.Options();
		opts.inPreferredConfig=Bitmap.Config.RGB_565;
		try {
			Field f = opts.getClass().getField("inScaled");
			f.setBoolean(opts,false);
		} catch (Throwable e) {
			e.printStackTrace();
		} 		
		byte[] data=get_tile(coords,200000);
		if (data==null)
			return null;
		Bitmap bm=BitmapFactory.decodeByteArray(data,0,data.length);		
		//Log.i("fplan","Bitmap:"+bm);
		return bm;
	}
    void close() throws IOException
    {
    	raf.close();
    }
	public byte[] get_data_at(iMerc coords, int offset, int size) throws IOException{
		
    	int imagesize=seekright(coords,offset);
    	if (imagesize<size)
    		return null;
        
        byte[] ret=new byte[size];
        raf.readFully(ret);
        return ret;
	}
}
