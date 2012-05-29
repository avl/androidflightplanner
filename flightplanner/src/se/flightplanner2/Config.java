package se.flightplanner2;

public class Config {

	public static final String path="/Android/data/se.flightplanner2/files/";
	
	//Assume a given clearance is valid for 1 hour.
	public static long clearance_valid_time=1*3600*1000l;

	public static int max_zoomlevel=20;

	public static int max_elev_zoomlevel=8;
	
	public static boolean skip_download=false;

	
	/**
	 * Use accelerometers to drive fake gps signals.
	 */
	public static boolean gpsdrive=false;

	public static boolean debugMode() {
		return false;
	}

	static String get_addr()
	{
		String addr;
		if (debugMode())
		{
			//addr="http://10.0.2.2:5000";
			//String addr="192.168.42.222:5000";
			//String addr="http://192.168.1.150:5000";
			
			//String addr="http://192.168.1.102:5000";
			//addr="http://192.168.1.160:5000";
			//addr="http://192.168.1.101:5000";
			//String addr="http://79.99.0.86:5000";
			//addr="http://192.168.1.160:5000";
			//addr="http://192.168.43.251:5000";
			addr="http://www.swflightplanner.se";
			
		}
		else
		{
			addr="http://www.swflightplanner.se";
		}
		
		
		return addr;
	}
}
