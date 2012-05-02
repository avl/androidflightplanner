package se.flightplanner2;

import android.app.Activity;
import android.location.Location;
import android.view.KeyEvent;

public interface MainMapIf {

	public abstract void enableDriving(boolean debugdrive);

	public abstract void zoom(int i);

	public abstract void onSideKey(int i);

	public abstract void update_detail(int int1, boolean northup);

	public abstract void enableTerrainMap(boolean b);

	public abstract void update_airspace(Airspace airspace,
			AirspaceLookup lookup, int int1, boolean northup);

	public abstract void update_tripdata(TripData tripdata,TripState tripstate);

	public abstract void gps_update(Location object);

	public abstract void invalidate();

	public abstract void thisSetContentView(Activity nav);

	public abstract void gps_disabled();

	public abstract void set_download_status(String prog, boolean b);

	public abstract void releaseMemory();

}