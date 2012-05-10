package se.flightplanner2;

public class Config {

	public static final String path="/Android/data/se.flightplanner2/files/";
	
	//Assume a given clearance is valid for 1 hour.
	public static long clearance_valid_time=1*3600*1000l;

	public static int max_zoomlevel=13; //not likely to change

	public static int max_elev_zoomlevel=8;
	
	public static boolean skip_download=true;
}
