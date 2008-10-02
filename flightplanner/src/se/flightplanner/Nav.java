package se.flightplanner;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

public class Nav extends Activity implements LocationListener {
    /** Called when the activity is first created. */
	MovingMap map;
	TripData tripdata;
	final static int MENU_LOGIN=0;
	final static int SETUP_INFO=1;
	final static int SETTINGS_DIALOG=2;
	private LocationManager locman;
	
	

	public boolean onCreateOptionsMenu(Menu menu) {
	    menu.add(0, MENU_LOGIN, 0, "Load Trip");
	    return true;
	}
	@Override
	protected void onActivityResult(int req,int res,Intent data)
	{
		/*
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	builder.setMessage(
    		data.getStringExtra("se.flightplanner.login")+
    			" data: "+data.getStringExtra("se.flightplanner.login")+
    			" req:"+new Integer(req).toString()+
    			" res:"+new Integer(res).toString())
    		.setCancelable(true);
    	AlertDialog diag=builder.create();
    	diag.show();
    	*/
		
		if (req==SETUP_INFO)
		{
			final String user=data.getStringExtra("se.flightplanner.login");
			final String password=data.getStringExtra("se.flightplanner.password");

			SharedPreferences prefs=getPreferences(MODE_PRIVATE);
			SharedPreferences.Editor pedit=prefs.edit();			
			pedit.putString("user", user);
			pedit.putString("password", password);
			pedit.commit();
					
			
	    	//final CharSequence[] trips= {"Red", "Green", "Blue"};
	    	if (user==null || password==null || user.equals("") || password.equals(""))
	    	{
	    		RookieHelper.showmsg(this,"Choose Login, and enter user/password first!");
		    	return;
	    	}
	    	String[] ttrips;
			try {
				ttrips = TripData.get_trips(
						user,password);
			} catch (Throwable e) {				
				RookieHelper.showmsg(this,"Couldn't connect to server:"+e.toString());
				return;
		    	
			}
	    	final String[] trips=ttrips;
	    		
	    	if (trips.length==0)
	    	{	    		
	    		RookieHelper.showmsg(this,"You have no trips! Go to www.flightplanner.se and create some!");
	    	}
	    	else
	    	{
		        final Nav nav=this;
		    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
		    	builder.setTitle("Choose Trip/"+user+"/"+password);
		    	builder.setItems(trips, new DialogInterface.OnClickListener() {
		    	    public void onClick(DialogInterface dialog, int item) {	    	        
				    	try {
							nav.tripdata=TripData.get_trip(user,password,trips[item]);
					    	try
					    	{
					    		nav.tripdata.serialize_to_file(nav,"tripdata.bin");
					    	} 
					    	catch(Throwable e) 
					    	{
					    		RookieHelper.showmsg(nav, e.toString());
					    	}
							
							map.update_tripdata(nav.tripdata);
				    	} catch (Throwable e) {
							RookieHelper.showmsg(nav,"Couldn't fetch trip from server:"+e.toString());
						}
		    	    }
		    	});
		    	AlertDialog diag=builder.create();
		    	diag.show();
	    	}
		}
	}
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	    case MENU_LOGIN:

	    	Intent intent = new Intent(this, SetupInfo.class);
	    	intent.putExtra("se.flightplanner.user", getPreferences(MODE_PRIVATE).getString("user","user")); 
	    	intent.putExtra("se.flightplanner.password", getPreferences(MODE_PRIVATE).getString("password","password")); 	    	
	    	startActivityForResult(intent,SETUP_INFO);
	    	//showDialog(SETTINGS_DIALOG);
	    	return true;
	    }
	    return false;
	}
	/*
	protected Dialog onCreateDialog(int id)
	{
		if (id==SETTINGS_DIALOG)
		{
			Context mContext = getApplicationContext();
			assert mContext!=null;
			Dialog dialog = new Dialog(mContext);
	
			dialog.setContentView(R.layout.settingsinfo);
			dialog.setTitle("Settings");
	
			//TextView text = (TextView) dialog.findViewById(R.id.edittext);
			//text.setText("Default!");
			return dialog;
		}
		else
		{
			assert false;
			return null;
		}		
	}
	*/
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	try
    	{
    		tripdata=TripData.deserialize_from_file(this,"tripdata.bin");
    	}
    	catch (Throwable e)
    	{
    		RookieHelper.showmsg(this, e.toString());
    	}
    	
        map=new MovingMap(this);        
		locman=(LocationManager)getSystemService(Context.LOCATION_SERVICE);
		locman.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0,0, this);
        setContentView(map);
    }
    
	public void onLocationChanged(Location location) {
		map.gps_update(location);
	}
	public void onProviderDisabled(String provider) {
		map.gps_disabled();
	}
	public void onProviderEnabled(String provider) {
	}
	public void onStatusChanged(String provider, int status, Bundle extras) {
		if (status!=LocationProvider.AVAILABLE)
			map.gps_disabled();
	}
    

}