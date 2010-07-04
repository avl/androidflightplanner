package se.flightplanner;

import java.io.Serializable;

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
		LatLon(double plat,double plon)
		{
			lat=plat;
			lon=plon;
		}
	}
	static public class Merc implements Serializable
	{
		private static final long serialVersionUID = -790641290695521623L;
		public double x;
		public double y;
		Merc(double px,double py)
		{
			x=px;
			y=py;
		}
	}
	
	static public Merc latlon2merc(LatLon latlon,int zoomlevel)
	{
		double lat=latlon.lat;
		double lon=latlon.lon;
	    double factor=Math.pow(2.0,zoomlevel);
	    return new Merc(
	    		(factor*256.0*(lon+180.0)/360.0),
	    		(128*factor-128*factor*merc(lat)/merc(85.05113)));
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

	/**Return the number of mercator proj 'pixels'
    which correspond most closely to the distance given in nautical miles.
    This scale is only valid at the latitude of the given mercator coords. 
    */  
	static public double approx_scale(Merc merc_coords,int zoomlevel,double length_in_nautical_miles)
	{	    
	    double factor=(Math.pow(2.0,(zoomlevel)));
	    double lat=unmerc((128*factor-merc_coords.y)/128.0/factor*merc(85.05113));
	    double latrad=lat/(180.0/Math.PI);
	    double scale_diff=Math.cos(latrad);
	    return 256*factor*((double)(length_in_nautical_miles)/(360*60.0))/scale_diff;
	}

}
