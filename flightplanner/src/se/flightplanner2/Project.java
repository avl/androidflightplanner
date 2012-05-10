package se.flightplanner2;

import java.io.DataInputStream;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;

import android.location.Location;
import android.util.Log;

import se.flightplanner2.Project.LatLon;
import se.flightplanner2.Project.Merc;
import se.flightplanner2.vector.Vector;

public class Project {
	static double sec(double x)
	{
	    return 1.0/Math.cos(x);
	}	
	static double merc(double lat)
	{
	    lat=lat/(180.0/3.14159);
	    return Math.log(Math.tan(lat)+sec(lat));
	}
	static double unmerc(double y)
	{
	    return (180.0/3.14159)*Math.atan(Math.sinh(y));
	}
	static public class LatLon implements Serializable
	{
		private static final long serialVersionUID = 591341378114851064L;
		public double lat;
		public double lon;
		
		@Override
		public boolean equals(Object o)
		{
			if (!(o instanceof LatLon)) return false;
			LatLon oo=(LatLon)o;
			return Math.abs(oo.lat-lat)<1e-7 && Math.abs(oo.lon-lon)<1e-7;  
		}
		public String toString()
		{
			return "LatLon("+lat+","+lon+")";
		}
		private int[] getDegMinSec(double val)
		{
			int negative=1;
			if (val<0) 
			{
				val=-val;
				negative=-1;
			}
			int t=(int)(val*3600.0);
			int deg=t/3600;
			t-=3600*deg;
			int min=t/60;
			int sec=t%60;
			return new int[]{negative,deg,min,sec};										
		}
		/*!
		 * To string in deg,min,sec format
		 */
		public String toString2()
		{
			int[] tlat=getDegMinSec(lat);
			int[] tlon=getDegMinSec(lon);
			StringBuilder sb=new StringBuilder();			
			sb.append(String.format("%02d°%02d'%02d\"%s",tlat[1],tlat[2],tlat[3],tlat[0]<0 ? "S" : "N"));
			sb.append(" ");
			sb.append(String.format("%03d°%02d'%02d\"%s",tlon[1],tlon[2],tlon[3],tlon[0]<0 ? "W" : "E"));			
			return sb.toString();
		}
		public LatLon(double plat,double plon)
		{
			lat=plat;
			lon=plon;
		}
		public LatLon(Location location) {
			lat=location.getLatitude();
			lon=location.getLongitude();
		}
		public static LatLon deserialize(DataInputStream is) throws IOException {
			return new LatLon(is.readFloat(),is.readFloat());
		}
		public void serialize(DataOutputStream os) throws IOException {
			os.writeFloat((float)lat);
			os.writeFloat((float)lon);
		}
	}
	static public class Merc implements Serializable
	{
		private static final long serialVersionUID = -790641290695521623L;
		public double x;
		public double y;
		
		public Merc(Vector v)
		{
			x=v.x;
			y=v.y;
		}
		public Merc(double px,double py)
		{
			x=px;
			y=py;
		}
		public static Merc deserialize(DataInputStream is) throws IOException {
			return new Merc(is.readFloat(),is.readFloat());
		}
		public void serialize(DataOutputStream os) throws IOException {
			os.writeFloat((float)x);
			os.writeFloat((float)y);
		}
		public Vector toVector()
		{
			return new Vector(x,y);
		}
}
	static public class iMerc
	{
		private int x;
		private int y;
		public iMerc(Vector v)
		{
			x=(int)v.x;
			y=(int)v.y;
			
		}
		public String toString()
		{
			return "iMerc("+x+","+y+")";
		}
		public iMerc(iMerc p)
		{
			x=p.x;
			y=p.y;
			
		}
		public iMerc(Merc p)
		{
			x=(int)p.x;
			y=(int)p.y;
			
		}
		public Vector toVector()
		{
			return new Vector(x,y);
		}
		@Override
		public int hashCode()
		{
			return x+y*1013;
		}
		@Override
		public boolean equals(Object oo)
		{
			iMerc m=(iMerc)oo;
			return this.x==m.x && this.y==m.y;
		}
		public iMerc(double px,double py)
		{
			x=(int)px;
			y=(int)py;
		}
		public iMerc(int px,int py)
		{
			x=(int)px;
			y=(int)py;
		}
		public static iMerc deserialize(DataInputStream is) throws IOException {
			return new iMerc(is.readInt(),is.readInt());
		}
		public void serialize(DataOutputStream os) throws IOException {
			os.writeInt(x);
			os.writeInt(y);
		}
		public iMerc copy() {
			return new iMerc(x,y);
		}
		public int getX() {
			return x;
		}
		public int getY() {
			return y;
		}
	}

	static public Merc latlon2merc(double lat,double lon,int zoomlevel)
	{
	    double factor=Math.pow(2.0,zoomlevel);
	    return new Merc(
	    		(factor*256.0*(lon+180.0)/360.0),
	    		(128*factor-128*factor*merc(lat)/merc(85.05113)));		
	}
	static public iMerc latlon2imerc(double lat,double lon,int zoomlevel)
	{
	    double factor=Math.pow(2.0,zoomlevel);
	    return new iMerc(
	    		(int)((factor*256.0*(lon+180.0)/360.0)),
	    		(int)((128*factor-128*factor*merc(lat)/merc(85.05113))));		
	}
	static public Vector latlon2mercvec(double lat,double lon,int zoomlevel)
	{
	    double factor=Math.pow(2.0,zoomlevel);
	    return new Vector(
	    		(factor*256.0*(lon+180.0)/360.0),
	    		(128*factor-128*factor*merc(lat)/merc(85.05113)));		
	}
	
	static public Merc latlon2merc(LatLon latlon,int zoomlevel)
	{
		double lat=latlon.lat;
		double lon=latlon.lon;
		return latlon2merc(lat,lon,zoomlevel);
	}
	static public iMerc latlon2imerc(LatLon latlon,int zoomlevel)
	{
		double lat=latlon.lat;
		double lon=latlon.lon;
		return latlon2imerc(lat,lon,zoomlevel);
	}
	
	static public Vector latlon2mercvec(LatLon latlon,int zoomlevel)
	{
		double lat=latlon.lat;
		double lon=latlon.lon;
		return latlon2mercvec(lat,lon,zoomlevel);
	}
	static public LatLon merc2latlon(Merc merc,int zoomlevel)
	{
		double x=merc.x;
		double y=merc.y;
		double factor=Math.pow(2.0,zoomlevel);
	    return new LatLon(
	    		unmerc((128*factor-y)/128.0/factor*merc(85.05113)),
	    		x*360.0/(256.0*factor)-180.0);
	}
	static public LatLon mercvec2latlon(Vector merc,int zoomlevel)
	{
		double x=merc.x;
		double y=merc.y;
		double factor=Math.pow(2.0,zoomlevel);
	    return new LatLon(
	    		unmerc((128*factor-y)/128.0/factor*merc(85.05113)),
	    		x*360.0/(256.0*factor)-180.0);
	}
	static public LatLon imerc2latlon(iMerc merc,int zoomlevel)
	{
		double x=merc.getX();
		double y=merc.getY();
		double factor=Math.pow(2.0,zoomlevel);
	    return new LatLon(
	    		unmerc((128*factor-y)/128.0/factor*merc(85.05113)),
	    		x*360.0/(256.0*factor)-180.0);
	}

	/**
	 * Return the number of mercator proj 'pixels'
     * which correspond most closely to the distance given in nautical miles.
     * This scale is only valid at the latitude of the given mercator coords. 
    */  
	static public double approx_scale(Merc merc_coords,int zoomlevel,double length_in_nautical_miles)
	{
		return approx_scale(merc_coords.y,zoomlevel,length_in_nautical_miles);
	}
	
	/**
	 * Returns the number of pixels for each feet of elevation.
	 */
	static public float approx_ft_pixels(iMerc coords,int zoomlevel)
	{
	    double factor=Math.pow(2.0,zoomlevel);
	    double lat=unmerc((128*factor-coords.getY())/128.0/factor*merc(85.05113));
	    double latrad=lat/(180.0/Math.PI);
	    double scale_diff=Math.cos(latrad);
	    
	    double one_foot=((256*factor/(360*60.0))/6076.11549)/scale_diff;
	    return (float)one_foot;
	}
	
	/**
	 * Return the number of mercator proj 'pixels'
     * which correspond most closely to the distance given in nautical miles.
     * This scale is only valid at the latitude of the given mercator coords. 
    */  
	static public double approx_scale(double merc_coords_y,int zoomlevel,double length_in_nautical_miles)
	{	    
	    double factor=(Math.pow(2.0,(zoomlevel)));
	    double lat=unmerc((128*factor-merc_coords_y)/128.0/factor*merc(85.05113));
	    double latrad=lat/(180.0/Math.PI);
	    double scale_diff=Math.cos(latrad);
	    return 256*factor*((double)(length_in_nautical_miles)/(360*60.0))/scale_diff;
	}
	static public double approx_nm(float merc_coords_y,int zoomlevel,int merc_length)
	{	    
	    float factor=(float)(Math.pow(2.0,(zoomlevel)));
	    float lat=(float)unmerc((128*factor-merc_coords_y)/128.0/factor*merc(85.05113));
	    float latrad=(float)(lat/(180.0/Math.PI));
	    float scale_diff=(float)Math.cos(latrad);
	    //merc_length=256*factor*((double)(length_in_nautical_miles)/(360*60.0))/scale_diff;
	    float length_in_nautical_miles=merc_length*scale_diff*360.0f*60.0f/(256.0f*factor);
	    return length_in_nautical_miles;
	}
	public static float bearing(LatLon p1,LatLon p2)
	{
		Merc m1=latlon2merc(p1, 17);
		Merc m2=latlon2merc(p2, 17);
		double dx=(double)(m2.x-m1.x);
		double dy=(double)(m2.y-m1.y);
		if (dx>(256<<17)/2)
			dx-=(256<<17);
		float tt=90f-(float)(Math.atan2(-dy,dx)*180.0/Math.PI);
		if (tt<0) tt+=360.0;
		return tt;		
	}
	public static double vector2heading(double dx, double dy) {
		double tt=90-(Math.atan2(-dy,dx)*180.0/Math.PI);
		if (tt<0) tt+=360.0;
		return tt;
	}
	public static double vector2heading(Vector v) {
		return vector2heading(v.getx(),v.gety());
	}
	public static Vector heading2vector(double ang) {
		double rad=Math.PI*(90-ang)/180.0;
		double dx=Math.cos(rad);
		double dy=-Math.sin(rad);
		return new Vector(dx,dy);
	}
	/*
	public static Vector merc2merc(Vector p, int srczoom, int trgzoom) {
		int delta=trgzoom-srczoom;
		if (delta==0) return p;
		float f=(float) Math.pow(2.0, delta);
		return new Vector(p.getx()*f,p.gety()*f);
	}*/
	public static Merc merc2merc(Merc p, int srczoom, int trgzoom) {
		int delta=trgzoom-srczoom;
		if (delta==0) return p;
		float f=(float) Math.pow(2.0, delta);
		return new Merc(p.x*f,p.y*f);
	}
	public static iMerc imerc2imerc(iMerc p, int srczoom, int trgzoom) {
		int delta=trgzoom-srczoom;
		if (delta==0) return p;
		if (delta>0)
			return new iMerc(p.getX()<<delta,p.getY()<<delta);
		else
			return new iMerc(p.getX()>>(-delta),p.getY()>>(-delta));
	}
	

	

	static double scalar_prod(double[] v1,double[] v2)
	{
		double sum=0;
		for(int i=0;i<3;++i)
			sum+=v1[i]*v2[i];
		return sum;
	}	
	static double[] to_vector(LatLon latlon)
	{	
		
		double lat=latlon.lat/(180.0/Math.PI);
		double lon=latlon.lon/(180.0/Math.PI);
		double z=Math.sin(lat)*(1.0-1.0/298.257223563 );
		double t=Math.cos(lat);
		double x=t*Math.cos(lon);
		double y=t*Math.sin(lon);	
		return new double[]{x,y,z};
	}
	static double[] vector_difference(double[] v1,double[] v2)
	{
		return new double[]{v1[0]-v2[0],v1[1]-v2[1],v1[2]-v2[2]};
	}
	
	static double vector_length(double[] v1)
	{
		return Math.sqrt(v1[0]*v1[0]+v1[1]*v1[1]+v1[2]*v1[2]);
	}
	static double[] vector_normalized(double[] v1)
	{
		double l=vector_length(v1);
		if (Math.abs(l)<1e-10)
			return new double[]{1,0,0};
		return new double[]{v1[0]/l,v1[1]/l,v1[2]/l};
	}
	/**
	 * Returns an approximation of the distance on the earth's surface
	 * between the two given coordinates, as the eagle flies.
	 * This is more exact than trying to use the approx_scale functions above,
	 * but still just a pretty bad approximation (spherical earth!).
	 */
	public static double exacter_distance(LatLon latLon1, LatLon latLon2) {

		double[] v1=to_vector(latLon1);
		double[] v2=to_vector(latLon2);
		v1=vector_normalized(v1);
		v2=vector_normalized(v2);
		double sc=scalar_prod(v1,v2);

		double ang=0;
		if (sc>=0.9999 || sc<-0.9999)
		{
			ang=vector_length(vector_difference(v1,v2));
		}
		else
		{
			ang=Math.acos(sc);
		}
		
		double ret=((6378137.0/1852.0)*ang); //nautical miles
		return ret;
	}

}
