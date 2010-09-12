package se.flightplanner;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;

import se.flightplanner.vector.Vector;

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
		public LatLon(double plat,double plon)
		{
			lat=plat;
			lon=plon;
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
	}
	static public class iMerc
	{
		public int x;
		public int y;
		public iMerc(Vector v)
		{
			x=(int)v.x;
			y=(int)v.y;
			
		}
		public iMerc(iMerc p)
		{
			x=p.x;
			y=p.y;
			
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
	static public LatLon imerc2latlon(iMerc merc,int zoomlevel)
	{
		double x=merc.x;
		double y=merc.y;
		double factor=Math.pow(2.0,zoomlevel);
	    return new LatLon(
	    		unmerc((128*factor-y)/128.0/factor*merc(85.05113)),
	    		x*360.0/(256.0*factor)-180.0);
	}

	/**Return the number of mercator proj 'pixels'
    which correspond most closely to the distance given in nautical miles.
    This scale is only valid at the latitude of the given mercator coords. 
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
	    double lat=unmerc((128*factor-coords.y)/128.0/factor*merc(85.05113));
	    double latrad=lat/(180.0/Math.PI);
	    double scale_diff=Math.cos(latrad);
	    
	    double one_foot=256*factor/(360*60.0/6076.11549)/scale_diff;
	    return (float)one_foot;
	}
	
	static public double approx_scale(double merc_coords_y,int zoomlevel,double length_in_nautical_miles)
	{	    
	    double factor=(Math.pow(2.0,(zoomlevel)));
	    double lat=unmerc((128*factor-merc_coords_y)/128.0/factor*merc(85.05113));
	    double latrad=lat/(180.0/Math.PI);
	    double scale_diff=Math.cos(latrad);
	    return 256*factor*((double)(length_in_nautical_miles)/(360*60.0))/scale_diff;
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

}
