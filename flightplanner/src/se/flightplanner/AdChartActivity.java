package se.flightplanner;


import se.flightplanner.SigPoint.Chart;
import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

public class AdChartActivity extends Activity implements LocationListener {
	
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
