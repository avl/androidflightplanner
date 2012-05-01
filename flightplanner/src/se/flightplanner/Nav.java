package se.flightplanner;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.Html;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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
	final static int MENU_HELP=8;
	final static int MENU_DESCPOS=9;
	final static int MENU_SIMPLER=10;
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
	    GlobalLookup.lookup=lookup;
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
	    menu.add(0, MENU_LOGIN, 0, "Select Trip");
	    menu.add(0, MENU_DOWNLOAD_TERRAIN, 0, "Sync");
	    menu.add(0, MENU_VIEW_CHARTS, 0, "Airports");
	    menu.add(0, MENU_FINISH, 0, "Exit");
	    menu.add(0, MENU_SETTINGS, 0, "Settings");
	    menu.add(0, MENU_HELP, 0, "Help");
	    
	    menu.add(0, MENU_VIEW_RECORDINGS, 0, "Recorded Trips");
	    menu.add(0, MENU_DESCPOS, 0, "Describe My Position");
	    menu.add(0, MENU_SIMPLER, 0, "Nearby Airspaces");
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
			else if (then!=null && then.equals("viewrec"))
			{
				viewRecordings();
				return;
			}
		}
	}


	private Location last_location;
	
	private boolean isNetworkAvailable() {
	    ConnectivityManager connectivityManager 
	          = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
	    NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
	    return activeNetworkInfo != null;
	}
	private boolean do_load_trips=false;
	
	private void loadTrip() {		
		if (true)
		{
			if (isNetworkAvailable() && (SystemClock.elapsedRealtime()-last_load_terrain>60*15*1000 || last_load_terrain==0))
			{
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setMessage(Html.fromHtml("It has been some time since you ran sync. Sync data before displaying trip list? If you choose not to, you won't see any changes to trips since last sync.",null,null))
				.setCancelable(true)
				.setPositiveButton("Sync first", new DialogInterface.OnClickListener() {
				    public void onClick(DialogInterface dialog, int id) {
				         dialog.dismiss();
						do_load_trips=true;
						loadTerrain();			
				    }
				})
				.setNegativeButton("Select trip", new DialogInterface.OnClickListener() {
				    public void onClick(DialogInterface dialog, int id) {
			         dialog.dismiss();
						selectTrip();				
				    }
				});
				AlertDialog diag=builder.create();
				diag.show();
			}
			else
			{
				long ago=SystemClock.elapsedRealtime()-last_load_terrain;
				Toast toast = Toast.makeText(this, String.format("Last sync: %d minutes ago",(int)(ago/60000)), Toast.LENGTH_SHORT);
				toast.show();
				do_load_trips=false;
				selectTrip();				
			}
		}
		/*else
		{
			do_load_trips=true;
			loadTerrain();			
		}*/		
	}
	private void selectTrip() {
		do_load_trips=false;
		if (airspace==null)
		{
			RookieHelper.showmsg(this, "You have no airspace or trip data. Select Menu->Sync to download the latest data!");
			return;			
		}
							
		final String[] trips=airspace.getTripList();
			
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
			    	nav.loadSelectedTrip(trips[item]);
	    	    }
	    	});
	    	AlertDialog diag=builder.create();
	    	diag.show();
		}
	}
	@Override 
	protected void onResume()
	{
		super.onResume();
		if (locman!=null)
			locman.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000,5, this);
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
    		do_load_terrain();    				        
		} catch (Exception e) {
			RookieHelper.showmsg(this,e.toString());
		}
    	return true;
    	
    case MENU_DESCPOS:
    {
    	Intent intent = new Intent(this, DescribePosition.class);
    	if (last_location!=null)
    	{
    		intent.putExtra("se.flightplanner.pos", new LatLon(last_location));
    		startActivity(intent);
    	}
    	else
    	{
    		RookieHelper.showmsg(this, "Position Unknown");
    	}
    	break;
    }
    case MENU_SIMPLER:
    {
    	Intent intent = new Intent(this, SimplerActivity.class);
    	if (last_location!=null)
    	{
    		intent.putExtra("se.flightplanner.pos", new LatLon(last_location));
    		intent.putExtra("se.flightplanner.hdg", (float)last_location.getBearing());
    		intent.putExtra("se.flightplanner.gs", (float)(last_location.getSpeed()*3.6/1.852));
    		startActivity(intent);
    	}
    	else
    	{
    		RookieHelper.showmsg(this, "Position Unknown");
    	}
    	break;
    }
    	
    }
	    return false;
	}
	private void do_load_terrain() {
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
		final ArrayList<String> icaos=new ArrayList<String>();
		final ArrayList<String> humanReadableNames=new ArrayList<String>();
		if (airspace!=null)
		{
			LatLon latlon=null;
			if (last_location!=null)
				latlon=new LatLon(last_location.getLatitude(),last_location.getLongitude());
			lookup.getAdChartNames(icaos,humanReadableNames,latlon);
		}
		
		if (icaos.size()==0)
		{	    		
			RookieHelper.showmsg(this,"No aerodromes found. Run 'Sync' again.");
		}
		else
		{
	        final Nav nav=this;		        
	    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
	    	builder.setTitle("Choose Airport");
	    	builder.setItems(humanReadableNames.toArray(new String[]{}), new DialogInterface.OnClickListener() {
	    	    public void onClick(DialogInterface dialog, int item) {
	    	    	if (icaos.get(item)==null)
	    	    	{ //clicked on the divider between close airports and alphabetically sorted airports.
	    	    		return;
	    	    	}
			    	nav.loadSelectedAd(icaos.get(item));
	    	    }
	    	});
	    	AlertDialog diag=builder.create();
	    	diag.show();
		}
	}
	protected void loadSelectedAd(String icao) {
		Intent intent = new Intent(this, ViewAdInfo.class);		
		intent.putExtra("se.flightplanner.icao", icao);
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
		Log.i("fplan","Previous airspace: "+airspace);
		terraindownloader.execute(airspace);
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
    	//setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    	fplog=new FlightPathLogger();
    	
    	last_location=bearingspeed.calcBearingSpeed(null);

        if (data != null) {
        	tripdata=data.tripdata;
        	airspace=data.airspace;
        	lookup=data.lookup;
    	    GlobalLookup.lookup=lookup;        	
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
			    GlobalLookup.lookup=lookup;		        
	    	}
	    	catch (Throwable e)
	    	{
	    		e.printStackTrace();
	    		Log.i("fplan.nav","Failed loading airspace data:"+e);
	    		RookieHelper.showmsg(this, "You have no airspace data. Select Menu->Sync!");
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
		locman.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000,5, this);
        
		map.thisSetContentView(this);
		map.gps_update(null);
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
    	super.onConfigurationChanged(newConfig);
    	map.invalidate();
    }
    
	private double offlat=0;
	private double offlon=0;
	private BearingSpeedCalc bearingspeed=new BearingSpeedCalc();
	@Override
	public void onLocationChanged(Location loc) {
		//Log.i("fplan","Location changed");
		if (loc!=null && DataDownloader.chartGpsDebugMode())
		{
			double lat=loc.getLatitude();
			double lon=loc.getLongitude();
		
			//Forcefully move us to middle of Arlanda airport,
			//So that we can easily test moving around there without
			//actually being there :-).
			if (offlat==0)
			{
				offlat=lat;
				offlon=lon;
			}
			lat=lat-offlat+59.652011;
			lon=lon-offlon+17.918701;
			loc.removeBearing();
			loc.setLatitude(lat);
			loc.setLongitude(lon);
		}
		
		Location location=bearingspeed.calcBearingSpeed(loc);		
		map.gps_update(location);
		last_location=location;
		//RookieHelper.showmsg(this, ""+location.getLatitude()+","+location.getLongitude());
		//locman.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500,5, this);
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
		    GlobalLookup.lookup=null;
		} catch (IOException e) {
			e.printStackTrace();
		}
		super.onDestroy();
	}

	@Override
	public void onProgress(String prog) {
		map.set_download_status(prog,false);
	}


	private long last_load_terrain=0;
	@Override
	public void onFinish(Airspace airspace,AirspaceLookup lookup,String error) {
		terraindownloader=null;
		if (airspace!=null)
		{
			this.airspace=airspace;
			this.lookup=lookup;
		    GlobalLookup.lookup=lookup;			
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
		if (airspace!=null)
			map.update_airspace(airspace,lookup,getPreferences(MODE_PRIVATE).getInt("mapdetail", 0),
        		getPreferences(MODE_PRIVATE).getBoolean("northup", false));
		map.enableTerrainMap(true);
		last_load_terrain=SystemClock.elapsedRealtime();
		
		
		if (do_load_trips)
		{
			selectTrip();
		}
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
	private void loadSelectedTrip(final String trip) {
		final Nav nav=this;
		if (airspace!=null)
			try
			{
				TripData td=airspace.getTrip(trip);
				if (td==null) throw new RuntimeException("No trip with that name.");
				if (td.waypoints.size()==0)
				{
					RookieHelper.showmsg(this, "This trip has no waypoints. Go to www.swflightplanner.se and create some!");
				}
				nav.tripdata=td;
				nav.tripdata.serialize_to_file(nav,"tripdata.bin");
				tripstate=new TripState(nav.tripdata);
				map.update_tripdata(nav.tripdata,tripstate);
				tripstate.reupdate();
			} 
			catch(Throwable e) 
			{
				RookieHelper.showmsg(nav, e.toString());
			}
	}

	
	private void showADInfo(final Place place) {
		
		if (place.getAerodrome()!=null)
		{
			map.releaseMemory();
			SigPoint sp=place.getAerodrome();
			if (sp.extra!=null && sp.extra.icao!=null && !sp.extra.icao.equals(""))
				loadSelectedAd(sp.extra.icao);			
		}
		else if (place.getDetailedPlace()!=null)
		{
			map.releaseMemory();
			Intent intent = new Intent(this, DetailedPlaceActivity.class);
			GlobalDetailedPlace.detailedplace=place.getDetailedPlace();
			Log.i("fplan","tripstate = "+GlobalDetailedPlace.detailedplace);
			startActivity(intent);
		}
		
		/*
	}
    	StringBuilder sb=new StringBuilder();
		
		sb.append("<h1>"+sp.name+"</h1>");
		if (sp.extra!=null)
		{
    	if (sp.icao!=null)
    		sb.append("<p>("+sp.icao+")</p>");
    	if (sp.metar!=null)
    	{
    		sb.append("<h2>METAR:</h2><p> "+sp.metar+"</p>");
    	}
    	if (sp.taf!=null)
    	{
    		sb.append("<h2>TAF:</h2><p> "+sp.taf+"</p>");
    	}
    	if (sp.notams.length>0)
    	{
	    	sb.append("<h2>NOTAMs:</h2>");
    		for(String notam:sp.notams)
    		{
    	    	sb.append("<p><pre>"+notam+"</pre></p>");
    		}

    	}
    	RookieHelper.showmsg(Nav.this, sb.toString());
    	*/
	}
	
	
	@Override
	public void doShowExtended(Place[] places) {
		ArrayList<String> airports=new ArrayList<String>();
		final ArrayList<Place> sigs=new ArrayList<Place>();
		for(Place place:places)
		{
			//SigPoint airport=lookup.getByIcao(icao);
			//if (airport==null) continue;
			sigs.add(place);
			airports.add(place.getHumanName());
		}
		if (airports.size()>0)
		{
	        if (airports.size()==1)
	        {	        	
	        	showADInfo(sigs.get(0));
	        }
	        else
	        {
		    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
		    	builder.setTitle("Choose Airport");
		    	builder.setItems(airports.toArray(new String[]{}), new DialogInterface.OnClickListener() {
		    	    public void onClick(DialogInterface dialog, int item) {		    	    	
		    	    	showADInfo(sigs.get(item));
		    	    }	
		    	});
		    	AlertDialog diag=builder.create();
		    	diag.show();			
	        }
		}
	}
    

}