package se.flightplanner2;

public class MapDetailLevels {

	public static int getMaxLevelFromDetail(int detail)
	{
		if (detail==-1) return -1;
		if (detail==0) return 6;
		if (detail==1) return 10;
		if (detail==2) return 11;
		if (detail==3) return 13;
		return 10;
	}

	public static boolean getHaveElevFromDetail(int detail) {
		if (detail>=2 || Config.debugMode())
			return true;
		return false;
	}

	public static int getMaxElevLevelFromDetail(int mapdetail) {
		return 8;
	}
}
