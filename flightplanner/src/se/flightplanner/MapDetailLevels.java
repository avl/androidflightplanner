package se.flightplanner;

public class MapDetailLevels {

	public static int getMaxLevelFromDetail(int detail)
	{
		if (detail==0) return 6;
		return 10;
	}
}
