package se.flightplanner;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import se.flightplanner.BackgroundMapDownloader.BackgroundMapDownloadOwner;
import se.flightplanner.MovingMap.MovingMapOwner;

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
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

public class Nav extends Activity implements LocationListener,BackgroundMapDownloadOwner,MovingMapOwner {
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
	final static int VIEW_RECORDINGS=3;
	
	final static int MENU_DOWNLOAD_TERRAIN=3;
	final static int MENU_FINISH=4;
	final static int MENU_SETTINGS=5;
	final static int MENU_VIEW_RECORDINGS=6;
	private LocationManager locman;
	BackgroundMapDownloader terraindownloader;
	private FlightPathLogger fplog;
	
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
	
	private boolean debugdrive;
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER)
		{
			if (DataDownloader.debugMode())
			{
				debugdrive=!debugdrive;
				map.enableDriving(debugdrive);
			}
		}
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
       	 	map.onSideKey(-1);
    		return true;
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
        	map.onSideKey(+1);
    		return true;
        }
         return false;
    }
	public boolean onCreateOptionsMenu(Menu menu) {
	    menu.add(0, MENU_LOGIN, 0, "Load Trip");
	    menu.add(0, MENU_DOWNLOAD_TERRAIN, 0, "Download Map");
	    menu.add(0, MENU_SETTINGS, 0, "Settings");
	    menu.add(0, MENU_VIEW_RECORDINGS, 0, "Recorded Trips");
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
			final int mapdetail=data.getIntExtra("se.flightplanner.mapdetail", 0);
			//RookieHelper.showmsg(this,"mapdetail now:"+mapdetail);
			SharedPreferences prefs=getPreferences(MODE_PRIVATE);
			SharedPreferences.Editor pedit=prefs.edit();			
			pedit.putString("user", user);
			pedit.putString("password", password);
			pedit.putInt("mapdetail", mapdetail);
			pedit.commit();
			
			String then=data.getStringExtra("se.flightplanner.thenopen");
			map.update_detail(getPreferences(MODE_PRIVATE).getInt("mapdetail", 0));
			if (then!=null && then.equals("loadterrain"))
			{
				loadTerrain();
				return;
			}
			else if (then!=null && then.equals("loadtrip"))
			{
		    	//final CharSequence[] trips= {"Red", "Green", "Blue"};
		    	loadTrip();
		    	return;
			}
			else if (then!=null && then.equals("viewrec"))
			{
				viewRecordings();
				return;
			}
		}
	}


	private void loadTrip() {
		final String user=getPreferences(MODE_PRIVATE).getString("user","user");
		final String password=getPreferences(MODE_PRIVATE).getString("password","password");
		String[] ttrips=null;
		try {
			ttrips = TripData.get_trips(
					user,password);
		} catch (Throwable e) {				
			RookieHelper.showmsg(this,"Couldn't connect to server:"+e.toString());		    	
		}
		if (ttrips!=null)
		{
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
	    case MENU_SETTINGS:
	    {
	    	Intent intent = getSettingsIntent();
	    	intent.putExtra("se.flightplanner.thenopen", "nothing");
	    	startActivityForResult(intent,SETUP_INFO);	    	
	    	break;
	    }
	    case MENU_VIEW_RECORDINGS:
	    {
	    	if (!haveUserAndPass())
	    	{
		    	Intent intent = getSettingsIntent();
		    	intent.putExtra("se.flightplanner.thenopen", "viewrec");
		    	startActivityForResult(intent,SETUP_INFO);
	    	}
	    	else
	    	{
	    		viewRecordings();
	    	}
			
	    	break;
	    }
	    case MENU_LOGIN:
	    {
	    	if (!haveUserAndPass())
	    	{
		    	Intent intent = getSettingsIntent();
		    	intent.putExtra("se.flightplanner.thenopen", "loadtrip");
		    	startActivityForResult(intent,SETUP_INFO);
	    	}
	    	else
	    	{
		    	loadTrip();
	
	    	}
	    	
	    	//showDialog(SETTINGS_DIALOG);
	    	return true;
	    }
    case MENU_DOWNLOAD_TERRAIN:
    	try {
    		if (terraindownloader!=null)
    		{
    			RookieHelper.showmsg(this,"Already in progress!");
    		}
    		else
    		{
    			if (!haveUserAndPass())
    			{
			    	Intent intent = getSettingsIntent();
	    	    	intent.putExtra("se.flightplanner.thenopen", "loadterrain");
	    	    	startActivityForResult(intent,SETUP_INFO);
    			}
    			else	    			
    				loadTerrain();
    		}    				        
		} catch (Exception e) {
			RookieHelper.showmsg(this,e.toString());
		}
    	return true;
    }
	    return false;
	}
	private void viewRecordings() {
    	try
		{
    		Intent intent = new Intent(this, ViewRecordings.class);
    		intent.putExtra("se.flightplanner.user", getPreferences(MODE_PRIVATE).getString("user","")); 
    		intent.putExtra("se.flightplanner.password", getPreferences(MODE_PRIVATE).getString("password",""));
    		fplog.saveCurrent(lookup);
    		startActivityForResult(intent,VIEW_RECORDINGS);
		}
    	catch(Exception e)
    	{
    		RookieHelper.showmsg(this,e.getMessage());
    	}
		
	}


	private Intent getSettingsIntent() {
		Intent intent = new Intent(this, SetupInfo.class);
		intent.putExtra("se.flightplanner.user", getPreferences(MODE_PRIVATE).getString("user","user")); 
		intent.putExtra("se.flightplanner.password", getPreferences(MODE_PRIVATE).getString("password","password"));
		int mapd=getPreferences(MODE_PRIVATE).getInt("mapdetail", 0);
		intent.putExtra("se.flightplanner.mapdetail", mapd);
		//RookieHelper.showmsg(this,"Got mapd"+mapd);
		return intent;
	}


	private boolean haveUserAndPass() {
		String user=getPreferences(MODE_PRIVATE).getString("user",null);
		String pass=getPreferences(MODE_PRIVATE).getString("password",null);
		if (user==null || user.length()==0)
			return false;
		if (pass==null || pass.length()==0)
			return false;
		return true;
	}


	private void loadTerrain() {
		String user=getPreferences(MODE_PRIVATE).getString("user","user");
		String pass=getPreferences(MODE_PRIVATE).getString("password","password");
		map.enableTerrainMap(false);
		int detail=getPreferences(MODE_PRIVATE).getInt("mapdetail",0);
		
		terraindownloader=new BackgroundMapDownloader(this,user,pass,detail);
		terraindownloader.execute();
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
    	fplog=new FlightPathLogger();
    	
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
	    	}	    	
	    	try
	    	{
	    		airspace=Airspace.deserialize_from_file(this,"airspace.bin");
		        lookup=new AirspaceLookup(airspace);
	    	}
	    	catch (Throwable e)
	    	{
	    		RookieHelper.showmsg(this, "You have no airspace data. Select Menu->Download Map!");
	    		//RookieHelper.showmsg(this, e.toString());
	    	}

        }
		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);

        map=new MovingMap(this,metrics,fplog,this);
        map.update_airspace(airspace,lookup,getPreferences(MODE_PRIVATE).getInt("mapdetail", 0));
        map.update_tripdata(tripdata);
		locman=(LocationManager)getSystemService(Context.LOCATION_SERVICE);
		locman.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500,0, this);
        
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

	@Override
	public void onPause()
	{
		try {
			fplog.saveCurrent(lookup);
		} catch (IOException e) {
			e.printStackTrace();
		}
		super.onPause();
	}
	@Override
	public void onDestroy()
	{
		try {
			fplog.saveCurrent(lookup);
		} catch (IOException e) {
			e.printStackTrace();
		}
		super.onDestroy();
	}

	@Override
	public void onProgress(String prog) {
		map.set_download_status(prog,false);
	}


	@Override
	public void onFinish(Airspace airspace,AirspaceLookup lookup,String error) {
		terraindownloader=null;		
		this.airspace=airspace;
		this.lookup=lookup;
		Log.i("fplan","Finish:"+error);
		if (error!=null)
		{
			map.set_download_status(error,true);
		}
		else
		{
			map.set_download_status("Complete",true);
		}
        map.update_airspace(airspace,lookup,getPreferences(MODE_PRIVATE).getInt("mapdetail", 0));
		map.enableTerrainMap(true);
	}
	@Override
	public void cancelMapDownload() {
		Log.i("fplan","cancelMapDownload:"+terraindownloader);
		if (terraindownloader!=null)
		{
			terraindownloader.cancel(true);
		}
	}
    

}