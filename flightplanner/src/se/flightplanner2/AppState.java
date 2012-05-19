package se.flightplanner2;

public class AppState {
	static public BackgroundMapDownloader terraindownloader;
	static public interface GuiIf
	{
		public void do_sync_now();
		public void onFinish(Airspace airspace, AirspaceLookup lookup, String error);
	}
	static public GuiIf gui=null;
}
