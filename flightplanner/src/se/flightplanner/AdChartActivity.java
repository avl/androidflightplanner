package se.flightplanner;


import se.flightplanner.SigPoint.Chart;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

public class AdChartActivity extends Activity implements LocationListener {
	final static int MENU_BACK=1;

	public boolean onCreateOptionsMenu(Menu menu) {
	    menu.add(0, MENU_BACK, 0, "Back");
		return true;
	}
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	    case MENU_BACK:
	    	this.finish();
	    	return true;
	    }
		return false;
	}
	LocationManager locman;
	AdChartView view;
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    	Log.i("fplan.chart","Before calling getSerializable");
    	
    	
    	
    	Chart chart=(Chart)getIntent().getExtras().getSerializable("se.flightplanner.chart");
    	/*this.chart=new Chart();
    	this.chart.width=1000;
    	this.chart.height=1000;
    	this.chart.name="ESSA";*/
    	if (chart.width>chart.height)
    		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    	else
    		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    	Log.i("fplan.chart","After calling getSerializable");
    	try
    	{
    		view=new AdChartView(this,chart);
		}
		catch(Exception e)
		{
			e.printStackTrace();
			finish();
			return;
		}
        
        setContentView(view);

        locman=(LocationManager)getSystemService(Context.LOCATION_SERVICE);
		locman.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500,0, this);

        
	}
	@Override
	public void onLocationChanged(Location location) {
		view.update_location(location);
	}
	@Override
	public void onProviderDisabled(String provider) {
		view.no_location_fix();		
	}
	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub
		view.no_location_fix();
	}
    
}
