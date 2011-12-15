package se.flightplanner;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import se.flightplanner.BackgroundMapDownloader.BackgroundMapDownloadOwner;
import se.flightplanner.MovingMap.MovingMapOwner;
import se.flightplanner.Project.LatLon;

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
import android.os.AsyncTask;
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
	MainMapIf map;
	TripData tripdata;
	Airspace airspace;
	AirspaceLookup lookup;
	TripState tripstate;
	//ElevationStore estore;
	//TextureStore tstore;
	//AirspaceAreaTree areaTree;
	//AirspaceSigPointsTree sigPointTree;
	final static int MENU_LOGIN=0;
	final static int SETUP_INFO=1;
	final static int SETTINGS_DIALOG=2;
	final static int VIEW_RECORDINGS=3;
	final static int VIEW=4;
	
	final static int MENU_DOWNLOAD_TERRAIN=3;
	final static int MENU_FINISH=4;
	final static int MENU_SETTINGS=5;
	final static int MENU_VIEW_RECORDINGS=6;
	final static int MENU_VIEW_CHARTS=7;
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
		TripState state;
	}
	
	@Override
	public Object onRetainNonConfigurationInstance() {
	    final NavData data = new NavData();
	    data.tripdata=tripdata;
	    data.airspace=airspace;
	    data.lookup=lookup;
	    data.state=tripstate;
	    //data.estore=estore;
	    //data.tstore=tstore;
	    return data;
	}
	
	private boolean debugdrive;
	@Override
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
	    menu.add(0, MENU_VIEW_CHARTS, 0, "Charts");
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
			final boolean northup=data.getBooleanExtra("se.flightplanner.northup", false);
			//RookieHelper.showmsg(this,"mapdetail now:"+mapdetail);
			SharedPreferences prefs=getPreferences(MODE_PRIVATE);
			SharedPreferences.Editor pedit=prefs.edit();			
			pedit.putString("user", user);
			pedit.putString("password", password);
			pedit.putInt("mapdetail", mapdetail);
			pedit.putBoolean("northup", northup);
			pedit.commit();
			
			
			String then=data.getStringExtra("se.flightplanner.thenopen");
			map.update_detail(getPreferences(MODE_PRIVATE).getInt("mapdetail", 0),
					getPreferences(MODE_PRIVATE).getBoolean("northup", false));
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


	private AsyncTask<Void, Void, String[]> loadtrips;
	private Location last_location;
	
	private void loadTrip() {
		final String user=getPreferences(MODE_PRIVATE).getString("user","user");
		final String password=getPreferences(MODE_PRIVATE).getString("password","password");
		final Nav nav=this;
		if (loadtrips!=null || load_trip_task!=null)
		{
			
			RookieHelper.showmsg(this,"Trip list is loading! Have patience!");
			return;
		}
		loadtrips=new AsyncTask<Void, Void, String[]>()
		{
			@Override
			protected String[] doInBackground(
					Void... params) {
				try {
					String[] ttrips = TripData.get_trips(
							user,password);
					return ttrips;
				} catch (Throwable e) {				
					return null;		    	
				}								
			}
			@Override
			protected void onPostExecute(String[] result) {
				super.onPostExecute(result);
				if (result==null)
				{
					RookieHelper.showmsg(nav, "Load failed. Check internet connection.");
					return;
				}
				loadtrips=null;
				selectTrip(user, password, result);
			}
			@Override
			protected void onCancelled() {
				super.onCancelled();
				loadtrips=null;
			}
		};
		loadtrips.execute((Void)null);
		
		
	}
	private void selectTrip(final String user, final String password,
			String[] ttrips) {
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
				    	nav.loadSelectedTrip(user, password, trips[item]);
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
	    case MENU_VIEW_CHARTS:
	    {
	    	viewAdChart();
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

	private void viewAdChart()
	{
		final ArrayList<String> ads=new ArrayList<String>();
		final ArrayList<String> humanReadableNames=new ArrayList<String>();
		if (airspace!=null)
		{
			LatLon latlon=null;
			if (last_location!=null)
				latlon=new LatLon(last_location.getLatitude(),last_location.getLongitude());
			airspace.getAdChartNames(ads,humanReadableNames,latlon);
		}
		
		if (ads.size()==0)
		{	    		
			RookieHelper.showmsg(this,"No aerodrome charts downloaded. Go to Settings, select High Detail maps, then go back and Download Map again.");
		}
		else
		{
	        final Nav nav=this;		        
	    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
	    	builder.setTitle("Choose Aerodrome");
	    	builder.setItems(humanReadableNames.toArray(new String[]{}), new DialogInterface.OnClickListener() {
	    	    public void onClick(DialogInterface dialog, int item) {
	    	    	if (ads.get(item)==null)
	    	    	{ //clicked on the divider between close airports and alphabetically sorted airports.
	    	    		return;
	    	    	}
			    	nav.loadSelectedAd(ads.get(item));
	    	    }
	    	});
	    	AlertDialog diag=builder.create();
	    	diag.show();
		}
	}
	protected void loadSelectedAd(String name) {
		Intent intent = new Intent(this, AdChartActivity.class);
		intent.putExtra("se.flightplanner.user", getPreferences(MODE_PRIVATE).getString("user","")); 
		intent.putExtra("se.flightplanner.password", getPreferences(MODE_PRIVATE).getString("password",""));
    	Log.i("fplan.chart","Before calling put Serializable");
		intent.putExtra("se.flightplanner.chart", airspace.getChart(name));
    	Log.i("fplan.chart","After calling put Serializable");	
		map.releaseMemory();
		startActivity(intent);
	}
	private Intent getSettingsIntent() {
		Intent intent = new Intent(this, SetupInfo.class);
		intent.putExtra("se.flightplanner.user", getPreferences(MODE_PRIVATE).getString("user","user")); 
		intent.putExtra("se.flightplanner.password", getPreferences(MODE_PRIVATE).getString("password","password"));
		int mapd=getPreferences(MODE_PRIVATE).getInt("mapdetail", 0);
		intent.putExtra("se.flightplanner.mapdetail", mapd);
		intent.putExtra("se.flightplanner.northup", getPreferences(MODE_PRIVATE).getBoolean("northup", false));
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
    	
    	tripstate=new TripState(null);
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
	    		Log.i("fplan.nav","Deserialized data okay, now generating lookup");
		        lookup=new AirspaceLookup(airspace);
	    	}
	    	catch (Throwable e)
	    	{
	    		Log.i("fplan.nav","Failed loading airspace data:"+e);
	    		RookieHelper.showmsg(this, "You have no airspace data. Select Menu->Download Map!");
	    		//RookieHelper.showmsg(this, e.toString());
	    	}

        }
		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);

        tripstate=new TripState(tripdata);
        map=new MovingMap(this,metrics,fplog,this,tripstate);
        map.update_airspace(airspace,lookup,getPreferences(MODE_PRIVATE).getInt("mapdetail", 0),
        		getPreferences(MODE_PRIVATE).getBoolean("northup", false));
        map.update_tripdata(tripdata,tripstate);
		locman=(LocationManager)getSystemService(Context.LOCATION_SERVICE);
		locman.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500,0, this);
        
		map.thisSetContentView(this);
		map.gps_update(null);
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
    	super.onConfigurationChanged(newConfig);
    	map.invalidate();
    }
    
    
	@Override
	public void onLocationChanged(Location location) {
		map.gps_update(location);
		last_location=location;
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
		if (airspace!=null)
		{
			this.airspace=airspace;
			this.lookup=lookup;
		}
		Log.i("fplan","Finish:"+error);
		if (error!=null)
		{
			map.set_download_status(error,true);
		}
		else
		{
			map.set_download_status("Complete",true);
		}
        map.update_airspace(airspace,lookup,getPreferences(MODE_PRIVATE).getInt("mapdetail", 0),
        		getPreferences(MODE_PRIVATE).getBoolean("northup", false));
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
	AsyncTask<Void,Void,TripData> load_trip_task;
	private void loadSelectedTrip(final String user, final String password,
			final String trip) {
		final Nav nav=this;
		if (load_trip_task!=null)
			return;
		load_trip_task=new AsyncTask<Void,Void,TripData>()
		{
			protected TripData doInBackground(Void... params) {
				try {			
					return TripData.get_trip(user,password,trip);			
				} catch (Throwable e) {
					e.printStackTrace();
					return null;
				}
			}
			protected void onCancelled() {
				load_trip_task=null;
			};
			@Override
			protected void onPostExecute(TripData result) {
				// TODO Auto-generated method stub
				super.onPostExecute(result);
				load_trip_task=null;
				if (result==null)
				{
					RookieHelper.showmsg(nav,"Failed to load trip. Check internet connection.");
					return;
				}
				try
				{
					nav.tripdata=result;
					nav.tripdata.serialize_to_file(nav,"tripdata.bin");
					tripstate=new TripState(nav.tripdata);
					map.update_tripdata(nav.tripdata,tripstate);				
				} 
				catch(Throwable e) 
				{
					RookieHelper.showmsg(nav, e.toString());
				}					
			}
			
		};
		load_trip_task.execute((Void)null);
	}
    

}