package se.flightplanner2;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import se.flightplanner2.BackgroundMapLoader.UpdatableUI;
import se.flightplanner2.GetMapBitmap.BitmapRes;
import se.flightplanner2.Project.LatLon;
import se.flightplanner2.Project.iMerc;
import se.flightplanner2.vector.Vector;


import android.os.Environment;
import android.util.Log;


public class AdChartLoader implements UpdatableUI{

	static public interface AdChartOwner
	{
		public void adChartLoadFinish(boolean done);
	}
	private MapCache mapcache;
	private BackgroundMapLoader loader;
	private ArrayList<Blob> blobs;
	public double[][] A; //2x2 matrix with airport chart projection scale/rotation latlon -> image pixels
	public double[][] Ai; //2x2 matrix with airport chart projection scale/rotation latlon -> image pixels
	public double[] T; //2 vector with airport chart projection translation
	private GetMapBitmap bitmaps;
	private int level;
	private AdChartOwner owner;
	
	private int chart_width;
	private int chart_height;

	
	public Vector latlon2pixel(LatLon latlon)
	{
		return latlon2pixel(level,latlon);
	}
	
	public Vector latlon2pixel(int level,LatLon latlon)
	{
		double mlat=latlon.lat-T[0];
		double mlon=latlon.lon-T[1];
		double px=A[0][0]*mlat + A[0][1]*mlon; 
		double py=A[1][0]*mlat + A[1][1]*mlon;
		int zg=maxzoomdata-level;
		double f=Math.pow(2,zg);
		
		return new Vector(px/f,py/f);
	}
	public LatLon pixel2latlon(Vector p)
	{
		return pixel2latlon(level,p);
	}
	
	public LatLon pixel2latlon(int level,Vector p)
	{
		int zg=maxzoomdata-level;
		double f=Math.pow(2,zg);
		
		
		double mlat=Ai[0][0]*f*p.x + Ai[0][1]*f*p.y; 
		double mlon=Ai[1][0]*f*p.x + Ai[1][1]*f*p.y;

		return new LatLon(mlat+T[0],mlon+T[1]);
	}
	public int get_width(){return chart_width>>(maxzoomdata-level);}
	public int get_height(){return chart_height>>(maxzoomdata-level);}

	int maxzoomdata;
	void start()
	{
		mapcache.forgetqueries();		
	}
	void end()
	{
		mapcache.garbageCollect();
		if (mapcache.haveUnsatisfiedQueries())
		{
			startLoad();			
		}
	}
	public static boolean haveproj(String chartname,String storage)
	{
		File extpath = Storage.getStorage(storage);
		
		File chartprojpath = new File(extpath,
				Config.path+chartname+".proj");
		if (!chartprojpath.exists())
			return false;
		DataInputStream ds;
		try {
			ds = new DataInputStream(
					new FileInputStream(chartprojpath));
			
			boolean isfloat=(chartprojpath.length()==6*4);
			if (isfloat)
			{
				for(int i=0;i<4;++i)
					if (ds.readFloat()!=0)
						return true;
			}
			else
			{
				for(int i=0;i<4;++i)
					if (ds.readDouble()!=0)
						return true;
				
			}
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
				
		return false;
	}
	public AdChartLoader(String chartname,AdChartOwner owner,String storage) {
		this.owner=owner;
		loader=null;
		mapcache=new MapCache();
		level=2;
		blobs=new ArrayList<Blob>();
		maxzoomdata=4;
		bitmaps=new GetMapBitmap(mapcache);
		File extpath = Storage.getStorage(storage);
		
		File chartprojpath = new File(extpath,
				Config.path+chartname+".proj");
		A=new double[2][2];
		Ai=new double[2][2];
		T=new double[2];
		try
		{
			DataInputStream ds=new DataInputStream(
					new FileInputStream(chartprojpath));
			boolean isfloat=(chartprojpath.length()==6*4);
			if (isfloat)
			{
				A[0][0]=ds.readFloat();
				A[1][0]=ds.readFloat();
				A[0][1]=ds.readFloat();
				A[1][1]=ds.readFloat();
				T[0]=ds.readFloat();
				T[1]=ds.readFloat();
				chart_width=3000; //this won't happen
				chart_height=3000;
			}
			else
			{
				A[0][0]=ds.readDouble();
				A[1][0]=ds.readDouble();
				A[0][1]=ds.readDouble();
				A[1][1]=ds.readDouble();
				T[0]=ds.readDouble();
				T[1]=ds.readDouble();		
				chart_width=3000;
				chart_height=3000;
				try{
					chart_width=ds.readInt();
					chart_height=ds.readInt();
				}catch(Throwable e)
				{
					//Log.i("fplan.adchart","Failed to get real width/height");
					e.printStackTrace();
				}
				
			}
			ds.close();
			//Log.i("fplan.adchart","loaded matrix:"+A[0][0]+","+A[1][0]+","+A[0][1]+","+A[1][1]);
			//Log.i("fplan.adchart","loaded vector:"+T[0]+", "+T[1]);
			
			{
				double a=A[0][0];
				double b=A[0][1];
				double c=A[1][0];
				double d=A[1][1];
				double f=(a*d-b*c);
				if (Math.abs(f)>=1e-12)
				{
					Ai[0][0]= d/f;
					Ai[0][1]= -b/f;
					Ai[1][0]= -c/f;
					Ai[1][1]= a/f;
				}
			}
			
			for(int i=0;i<5;++i)
			{
				Integer is=new Integer(5-i-1);
	
				File chartpath = new File(extpath,
						Config.path+chartname+"-"+is.toString()+".bin");
				Blob blob=new Blob(chartpath.getAbsolutePath(),256);
				blobs.add(blob);
				/*Log.i("fplan.adchart","Dimensions of level "+is+" "+
						"x1:"+blob.getX1()+
						"y1:"+blob.getY1()+
						"x2:"+blob.getX2()+
						"y2:"+blob.getY2()
						);*/
			}
		}
		catch(Throwable e)
		{
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		
	}
	
	public boolean haveGeoLocation() {
		if (A[0][0]==0 &&
			A[1][0]==0 &&
			A[0][1]==0 &&
			A[1][1]==0)
			return false;
			
		return true;
	}
	public BitmapRes getBitmap(iMerc pos) {
		return bitmaps.getBitmap(pos, level);
		
	}
	@Override
	public void updateUI(boolean done) {
		if (done)
		{
			loader=null;
			startLoad();			
			owner.adChartLoadFinish(done);		
		}
	}

	private void startLoad() {
		if (loader==null)
		{
			if (mapcache!=null && mapcache.haveUnsatisfiedQueries())
			{
				loader=new BackgroundMapLoader(blobs,mapcache,this);
				loader.run();
			}
		}
	}
	public int guess_zoomlevel(double onenmpixels) {
		Vector zero=new Vector();
		float best_delta=1e10f;
		int best_level=2;
		for(int i=0;i<=maxzoomdata;++i)
		{
			LatLon ll=pixel2latlon(i,zero);
			ll.lat+=1.0/60.0;
			Vector px=latlon2pixel(i,ll);
			float cand_onenmpixels=(float)px.length();
			//Log.i("fplan.bad","onenm-pixels: "+onenmpixels+" canddiate: "+cand_onenmpixels);
			float delta=(float)Math.abs(cand_onenmpixels-onenmpixels);
			if (delta<best_delta)
			{
				best_level=i;
				best_delta=delta;
				
			}
			
		}
		return best_level;
	}
	void set_level(int level){
		this.level=level;
	}

	public float getChartUpBearing() {
		
		LatLon l1=pixel2latlon(maxzoomdata,new Vector(0,chart_height));
		LatLon l2=pixel2latlon(maxzoomdata,new Vector(0,0));
		
		return Project.bearing(l1, l2);
	}

	public LatLon getChartCenter() {
		return pixel2latlon(maxzoomdata,new Vector(chart_width/2,chart_height/2));
	}

	public int best_zoomlevel(int width) {
		LatLon left=pixel2latlon(maxzoomdata,new Vector(0,0));
		LatLon right=pixel2latlon(maxzoomdata,new Vector(chart_width,0));
		Vector m17left=Project.latlon2mercvec(left, 17);
		Vector m17right=Project.latlon2mercvec(right, 17);
		int m17size=(int)(m17right.minus(m17left).length());
		int prev=17;
		Log.i("fplan.m17","m17 size: "+m17size+" chart width: "+chart_width+" left, right: "+left+","+right);
		for(int i=17;i>=0;--i)
		{
			if ((m17size>>(17-i))<width)
				return prev;
			prev=i;			
		}
		return 0;
	}

	public void releaseMemory() {
		mapcache.releaseMemory();
	}
		


}
