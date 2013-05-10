package se.flightplanner2;




import se.flightplanner2.GlobalPosition.PositionIf;
import se.flightplanner2.GlobalPosition.PositionSubscriberIf;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

public class AdChartActivity extends Activity implements PositionSubscriberIf {
	final static int MENU_BACK=1;

	@Override
	public void onBackPressed() {
	  super.onBackPressed();
	  overridePendingTransition(0, 0);
	}
	public boolean onCreateOptionsMenu(Menu menu) {
	    menu.add(0, MENU_BACK, 0, "Back");
		return true;
	}
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	    case MENU_BACK:
	    	finish();
    	    overridePendingTransition(0, 0);	    	
	    	return true;
	    }
		return false;
	}
	private AdChartView view;
	
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
    	super.onConfigurationChanged(newConfig);
    	view.invalidate();
    }
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    	Log.i("fplan.chart","Before calling getSerializable");
    	
    
    	String chartname=getIntent().getExtras().getString("se.flightplanner2.chartname");
    	/*this.chart=new Chart();
    	this.chart.width=1000;
    	this.chart.height=1000;
    	this.chart.name="ESSA";*/
    	Log.i("fplan.chart","After calling getSerializable");
    	try
    	{
    		view=new AdChartView(this,chartname,Storage.getStoragePath(this));
    		if (view.failed_to_get_width())
    		{
    			Log.i("fplan.chart","Failed get chart width");
    			finish();
        	    overridePendingTransition(0, 0);	    	
    			return;
    		}
		}
		catch(Exception e)
		{
			e.printStackTrace();
			finish();
    	    overridePendingTransition(0, 0);	    	
			return;
		}
    	/*
    	if (view.get_chart_width()>view.get_chart_height())
    		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    	else
    		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        */
        setContentView(view);

        if (view.haveGeoLocation())
        {
        		GlobalPosition.registerSubscriber(this);
        }

        
	}
	@Override
	public void onDestroy()
	{
		GlobalPosition.unRegisterSubscriber(this);
		super.onDestroy();
	}	
	@Override
	public void gps_update(Location loc) {
		view.update_location(loc);
	}
	@Override
	public void gps_disabled() {
		view.no_location_fix();		
	}
    
}
