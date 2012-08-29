package se.flightplanner2;

import android.app.Activity;
import android.location.Location;
import android.view.KeyEvent;

public interface MainMapIf {

	public abstract void zoom(int i);

	public abstract void onSideKey(int i);

	public abstract void update_detail(int int1, boolean northup,boolean sideview);

	public abstract void enableTerrainMap(boolean b);

	public abstract void update_airspace(Airspace airspace,
			AirspaceLookup lookup, int int1, boolean northup,boolean sideview);

	public abstract void update_tripdata(TripData tripdata,TripState tripstate);

	public abstract void gps_update(Location object, boolean show_terrain_warning, int cvr_amp);

	public abstract void invalidate();

	public abstract void thisSetContentView(Activity nav);

	public abstract void gps_disabled();

	public abstract void set_download_status(String prog, boolean b);

	public abstract void releaseMemory();

	public abstract void proxwarner_update(String[] warning);

	public abstract void set_gps_sat_cnt(int satcnt, int satfixcnt);

	public abstract void set_battery_level(int level,boolean plugged);

	public abstract void selectChart(String chart);

	public abstract void stop();


}