package se.flightplanner;

import se.flightplanner.Project.LatLon;
import se.flightplanner.simpler.SimplerView;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;

public class SimplerActivity extends Activity implements LocationListener {
	private LatLon pos;
	private double hdg;
	private double gs;
	private long inhibit_relayout; 
	private Handler handler;
	private SimplerView simplerView;
	private LocationManager locman;
	@Override
    public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		handler=new Handler();
		pos=(LatLon)getIntent().getSerializableExtra("se.flightplanner.pos");
		hdg=getIntent().getFloatExtra("se.flightplanner.hdg",0);
		gs=getIntent().getFloatExtra("se.flightplanner.gs",0);
		
		simplerView=new SimplerView(this,GlobalLookup.lookup,pos,(float)hdg,(float)gs,new SimplerView.ViewOwner()
		{
			public void touched()
			{
				postpone_layout();				
			}
		});
		setContentView(simplerView);
		
        locman=(LocationManager)getSystemService(Context.LOCATION_SERVICE);
        if (locman!=null)
        	locman.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000,500, this);
        
        simplerView.setKeepScreenOn(true);
		
	}
	@Override
	public void onDestroy()
	{
		if (update_cb!=null)
			handler.removeCallbacks(update_cb);
		simplerView.stop();
		super.onDestroy();
		if (locman!=null)
			locman.removeUpdates(this);
	}
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
    	super.onConfigurationChanged(newConfig);
    	simplerView.invalidate();
    }

    static final int inhibit_timeout=5000;
	private BearingSpeedCalc bsp=new BearingSpeedCalc();
	
	private void postpone_layout()
	{
		inhibit_relayout=SystemClock.elapsedRealtime();
		Log.i("fplan","order postpone relayout");
		
		if (scheduled)
		{
			handler.removeCallbacks(update_cb);
			handler.postDelayed(update_cb, inhibit_timeout);
		}
	}
	
	
	@Override
	public void onLocationChanged(Location location) {
		Log.i("fplan","Updating position");
		location=bsp.calcBearingSpeed(location);
		hdg=location.getBearing();
		gs=location.getSpeed()*3.6/1.852;
		pos=new LatLon(location);
		long now=SystemClock.elapsedRealtime();
		if (now-inhibit_relayout>inhibit_timeout)
		{
			update_cb.run();
		}
		else
		{
			handler.removeCallbacks(update_cb);
			Log.i("fplan","Not relayouting because of ongoing interaction");
			simplerView.update(pos,hdg,gs,true); //just a quick update
			handler.postDelayed(update_cb, inhibit_timeout-(now-inhibit_relayout));
			scheduled=true;
		}
	}
	
	private boolean scheduled=false;
	private Runnable update_cb=new Runnable()
	{
		@Override
		public void run() {
			scheduled=false;
			simplerView.update(pos,hdg,gs,false);					
		}
	};
	
	@Override
	public void onProviderDisabled(String provider) {
		
	}
	@Override
	public void onProviderEnabled(String provider) {
		
	}
	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		
	}
	
}
