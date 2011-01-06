package se.flightplanner;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import se.flightplanner.Project.iMerc;

public class Blob {
	private int tilesize=256;
	private int x1;
	private int y1;
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
		if (!(m.x>=0 && m.y>=0)) throw new RuntimeException("Bad merc");		
		if (m.x%tilesize!=0 || m.y%tilesize!=0)
			throw new RuntimeException("Invalid tilesize");
		TileNumber t=new TileNumber();
		t.x=(m.x-x1)/tilesize;
		t.y=(m.y-y1)/tilesize;
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
        int x2=raf.readInt();
        int y2=raf.readInt();
        zoomlevel=raf.readInt();

        TileNumber t1=get_tile_number(new iMerc(x1,y1));
        TileNumber t2=get_tile_number(new iMerc(x2,y2));
        sx=t2.x-t1.x;
        sy=t2.y-t1.y;
        if (!(sx>0 && sy>0))
        	throw new RuntimeException("Bad sx- and sy-variables");
	}
	public long getSize()
	{
		return size;
	}
	private int seekright(iMerc coords) throws IOException
	{
	    TileNumber t=get_tile_number(coords);
	    if (t.x<0 || t.x>=sx || t.y<0 || t.y>=sy)
	        return -1;
	    long pos=20+4*((long)t.x+(long)t.y*(long)sx);
	    raf.seek(pos);
	    long datapos=raf.readInt();
	    if (datapos<0)
	    	datapos+=(1L<<32L);
	    if (datapos>size-4)
	    	throw new RuntimeException("Bad size"); 
	    raf.seek(datapos);
	    if (datapos<=0)
	    	return -1;
	    int imagesize=raf.readInt();
	    if (imagesize<0)
	    	throw new RuntimeException("Unexpected imagesize");
	    return imagesize;
	}
    byte[] get_tile(iMerc coords) throws IOException
    {    	
    	int imagesize=seekright(coords);
    	if (imagesize==-1)
    		return null;
        
        byte[] ret=new byte[imagesize];
        if (raf.read(ret)!=imagesize)
        	throw new RuntimeException("Couldn't read image");
        return ret;
    }
	Bitmap get_bitmap(iMerc coords) throws IOException
	{
    	int imagesize=seekright(coords);
    	if (imagesize==-1)
    		return null;
		
		BitmapFactory.Options opts = new BitmapFactory.Options();
		opts.inScaled = false;								
		Bitmap bm=BitmapFactory.decodeFileDescriptor(raf.getFD(),null,opts);

		return null;
	}
    void close() throws IOException
    {
    	raf.close();
    }
}
