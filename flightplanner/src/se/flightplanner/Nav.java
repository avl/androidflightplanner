package se.flightplanner;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

//import se.flightplanner.map3d.ElevationStore;
//import se.flightplanner.map3d.TextureStore;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

public class Nav extends Activity implements LocationListener {
    /** Called when the activity is first created. */
	MovingMap map;
	TripData tripdata;
	Airspace airspace;
	AirspaceLookup lookup;
	//ElevationStore estore;
	//TextureStore tstore;
	//AirspaceAreaTree areaTree;
	//AirspaceSigPointsTree sigPointTree;
	final static int MENU_LOGIN=0;
	final static int SETUP_INFO=1;
	final static int SETTINGS_DIALOG=2;
	final static int MENU_DOWNLOAD_AIRSPACE=3;
	final static int MENU_FINISH=4;
	private LocationManager locman;
	
	static class NavData
	{
		TripData tripdata;
		Airspace airspace;
		//ElevationStore estore;
		//TextureStore tstore;
		AirspaceLookup lookup;
	}
	
	@Override
	public Object onRetainNonConfigurationInstance() {
	    final NavData data = new NavData();
	    data.tripdata=tripdata;
	    data.airspace=airspace;
	    data.lookup=lookup;
	    //data.estore=estore;
	    //data.tstore=tstore;
	    return data;
	}
	
	
	public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
       	 	map.zoom(1);
    		return true;
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
        	map.zoom(-1);
    		return true;
        }
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
        	map.zoom(1);
    		return true;
        }
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
       	 	map.zoom(-1);
    		return true;
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
       	 	map.sideways(-1);
    		return true;
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
        	map.sideways(+1);
    		return true;
        }
         return false;
    }
	public boolean onCreateOptionsMenu(Menu menu) {
	    menu.add(0, MENU_LOGIN, 0, "Load Trip");
	    menu.add(0, MENU_DOWNLOAD_AIRSPACE, 0, "Download Map");
	    menu.add(0, MENU_FINISH, 0, "Exit");
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
		
		if (req==SETUP_INFO && data!=null)
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
		    	builder.setTitle("Choose Trip");
		    	builder.setItems(trips, new DialogInterface.OnClickListener() {
		    	    public void onClick(DialogInterface dialog, int item) {	    	        
				    	try {
							nav.tripdata=TripData.get_trip(user,password,trips[item]);
							
							/*
							
							
							{
								ArrayList<NameValuePair> nvps=new ArrayList<NameValuePair>();
								nvps.add(new BasicNameValuePair("trip",trips[item]));
								InputStream strm=DataDownloader.postRaw("/api/get_elev_near_trip",user, password, nvps,false);
								nav.estore=ElevationStore.deserialize(new DataInputStream(strm));
								strm.close();
							}
							{
								ArrayList<NameValuePair> nvps=new ArrayList<NameValuePair>();
								nvps.add(new BasicNameValuePair("trip",trips[item]));
								InputStream strm=DataDownloader.postRaw("/api/get_map_near_trip",user, password, nvps,false);
								nav.tstore=TextureStore.deserialize(new DataInputStream(strm));
								strm.close();
							}
							*/

					    	try
					    	{
					    		nav.tripdata.serialize_to_file(nav,"tripdata.bin");
					    		//nav.estore.serialize_to_file(nav,"elev.bin");
					    		//nav.tstore.serialize_to_file(nav,"tex.bin");
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
	@Override 
	protected void onResume()
	{
		super.onResume();
		if (locman!=null)
			locman.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500,5, this);
	}
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	    case MENU_FINISH:
	    	locman.removeUpdates(this);
	    	finish();
	    	break;
	    case MENU_LOGIN:

	    	Intent intent = new Intent(this, SetupInfo.class);
	    	intent.putExtra("se.flightplanner.user", getPreferences(MODE_PRIVATE).getString("user","user")); 
	    	intent.putExtra("se.flightplanner.password", getPreferences(MODE_PRIVATE).getString("password","password")); 	    	
	    	startActivityForResult(intent,SETUP_INFO);
	    	//showDialog(SETTINGS_DIALOG);
	    	return true;
	    case MENU_DOWNLOAD_AIRSPACE:
	    	try {
	    		///RookieHelper.showmsg(this,"Airspace data for Sweden will now be downloaded. This can take several minutes, and your phone may become unresponsive. Turn on internet access, click ok, and have patience!");
				airspace=Airspace.download();
				airspace.serialize_to_file(this,"airspace.bin");
				
				Log.i("fplan","Building BSP-trees");
				lookup=new AirspaceLookup(airspace);
		    	//areaTree=new AirspaceAreaTree(airspace.getSpaces());
		    	//sigPointTree=new AirspaceSigPointsTree(airspace.getPoints());
				Log.i("fplan","BSP-trees finished");
		        map.update_airspace(airspace,lookup);
		        
			} catch (Exception e) {
				RookieHelper.showmsg(this,e.toString());
			}
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
    	
    	final NavData data = (NavData) getLastNonConfigurationInstance();
    	
    	setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    	
        if (data != null) {
        	tripdata=data.tripdata;
        	airspace=data.airspace;
        	lookup=data.lookup;
        	//estore=data.estore;
        	//tstore=data.tstore;
        }
        else
        {	
	    	try
	    	{
	    		tripdata=TripData.deserialize_from_file(this,"tripdata.bin");
	    		//estore=ElevationStore.deserialize_from_file(this,"elev.bin");
	    		//tstore=TextureStore.deserialize_from_file(this,"tex.bin");
	    	}
	    	catch (Throwable e)
	    	{
	    		e.printStackTrace();
	    		RookieHelper.showmsg(this, e.toString());
	    	}	    	
	    	try
	    	{
	    		airspace=Airspace.deserialize_from_file(this,"airspace.bin");
	    	}
	    	catch (Throwable e)
	    	{
	    		RookieHelper.showmsg(this, "You have no airspace data. Select Menu->Download Map!");
	    		//RookieHelper.showmsg(this, e.toString());
	    	}
	        lookup=new AirspaceLookup(airspace);

        }
    	
        map=new MovingMap(this);
        map.update_airspace(airspace,lookup);
        map.update_tripdata(tripdata);
		locman=(LocationManager)getSystemService(Context.LOCATION_SERVICE);
		locman.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500,5, this);
        
		setContentView(map);
		map.gps_update(null);
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
    	super.onConfigurationChanged(newConfig);
    	map.invalidate();
    }
    
    
	public void onLocationChanged(Location location) {
		map.gps_update(location);
		//RookieHelper.showmsg(this, ""+location.getLatitude()+","+location.getLongitude());
		locman.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500,5, this);
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