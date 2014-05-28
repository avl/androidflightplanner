package se.flightplanner2;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import se.flightplanner2.AirspaceLookup.ClosestAirportResult;
import se.flightplanner2.AirspaceLookup.QnhGuessResult;
import se.flightplanner2.AppState.GuiIf;
import se.flightplanner2.BackgroundMapDownloader.BackgroundMapDownloadOwner;
import se.flightplanner2.GlobalPosition.PositionSubscriberIf;
import se.flightplanner2.MovingMap.MovingMapOwner;
import se.flightplanner2.Project.LatLon;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.GpsStatus.Listener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.Vibrator;
import android.text.Html;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class Nav extends Activity implements PositionSubscriberIf,
		BackgroundMapDownloadOwner, MovingMapOwner, GuiIf {
	/** Called when the activity is first created. */
	MainMapIf map;
	TripData tripdata;
	Airspace airspace;
	AirspaceLookup lookup;
	TripState tripstate;
	// ElevationStore estore;
	// TextureStore tstore;
	// AirspaceAreaTree areaTree;
	// AirspaceSigPointsTree sigPointTree;
	final static int MENU_LOGIN = 0;
	final static int SETUP_INFO = 1;
	final static int SETTINGS_DIALOG = 2;
	final static int VIEW_RECORDINGS = 3;
	final static int VIEW = 4;
	final static int ADINFO = 5;

	final static int MENU_SYNC = 3;
	final static int MENU_FINISH = 4;
	final static int MENU_SETTINGS = 5;
	final static int MENU_VIEW_RECORDINGS = 6;
	final static int MENU_VIEW_CHARTS = 7;
	final static int MENU_HELP = 8;
	final static int MENU_DESCPOS = 9;
	final static int MENU_SIMPLER = 10;
	final static int MENU_PHRASES = 11;
	final static int MENU_MORE = 12;
	final static int MENU_BACK = 13;
	private LocationManager locman;
	
	private FlightPathLogger fplog;
	private GlobalPositionImpl globalposition;

	static class NavData {
		TripData tripdata;
		Airspace airspace;
		// ElevationStore estore;
		// TextureStore tstore;
		AirspaceLookup lookup;
		TripState state;
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		final NavData data = new NavData();
		data.tripdata = tripdata;
		data.airspace = airspace;
		data.lookup = lookup;
		GlobalLookup.lookup = lookup;
		data.state = tripstate;
		// data.estore=estore;
		// data.tstore=tstore;
		return data;
	}


	@Override
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
			if (Config.debugMode())
				GlobalPosition.pos.debugElev(250*0.3048f);
			else
				map.zoom(1);
			return true;
		}
		if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
			if (Config.debugMode())
				GlobalPosition.pos.debugElev(-250*0.3048f);
			else
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
		menu.add(0, MENU_DESCPOS, 0, "Descr. Pos");
		menu.add(0, MENU_SIMPLER, 0, "Airspaces");
		menu.add(0, MENU_PHRASES, 0, "Phrases");
		menu.add(0, MENU_VIEW_CHARTS, 0, "Airports");
		menu.add(0, MENU_SYNC, 0, "Sync");

		menu.add(0, MENU_MORE, 0, "More");
		
		/*
		 * asdf
		menu.add(0, MENU_LOGIN, 0, "Select Trip");
		menu.add(0, MENU_SETTINGS, 0, "Settings");
		menu.add(0, MENU_VIEW_RECORDINGS, 0, "Recorded Trips");
		menu.add(0, MENU_FINISH, 0, "Exit");
		*/
		return true;
	}

	@Override
	protected void onActivityResult(int req, int res, Intent data) {
		/*
		 * AlertDialog.Builder builder = new AlertDialog.Builder(this);
		 * builder.setMessage( data.getStringExtra("se.flightplanner2.login")+
		 * " data: "+data.getStringExtra("se.flightplanner2.login")+ " req:"+new
		 * Integer(req).toString()+ " res:"+new Integer(res).toString())
		 * .setCancelable(true); AlertDialog diag=builder.create(); diag.show();
		 */
		if (req==ADINFO && data!=null)
		{
	    	//Log.i("fplan.adchart","ADINFO request,selecting chart");

			String chart=data.getStringExtra("se.flightplanner2.adchart");
			if (chart!=null && !chart.equals(""))		
				map.selectChart(chart);
		}
		if (req == SETUP_INFO && data != null) {
			final String user = data.getStringExtra("se.flightplanner2.login");
			final String password = data
					.getStringExtra("se.flightplanner2.password");
			String storage = data
					.getStringExtra("se.flightplanner2.storage");
			String prevstorage=getSharedPreferences("se.flightplanner2.prefs",MODE_PRIVATE).getString("storage", "");
			final int mapdetail = data.getIntExtra(
					"se.flightplanner2.mapdetail", 2);
			final boolean northup = data.getBooleanExtra(
					"se.flightplanner2.northup", false);
			final boolean vibrate = data.getBooleanExtra(
					"se.flightplanner2.vibrate", false);
			final boolean terrwarn = data.getBooleanExtra(
					"se.flightplanner2.terrwarn", false);
			final boolean autosync= data.getBooleanExtra(
					"se.flightplanner2.autosync", false);
			final boolean cvr= data.getBooleanExtra(
					"se.flightplanner2.cvr", false);
			final boolean sideview= data.getBooleanExtra(
					"se.flightplanner2.sideview", false);
			final boolean nmea_udp= data.getBooleanExtra(
					"se.flightplanner2.nmea_udp", false);
			// RookieHelper.showmsg(this,"mapdetail now:"+mapdetail);
			SharedPreferences prefs = getSharedPreferences("se.flightplanner2.prefs",MODE_PRIVATE);
			SharedPreferences.Editor pedit = prefs.edit();
			pedit.putString("user", user);
			pedit.putString("password", password);
			pedit.putString("storage", storage);
			pedit.putInt("mapdetail", mapdetail);
			pedit.putBoolean("northup", northup);
			pedit.putBoolean("vibrate", vibrate);
			pedit.putBoolean("terrwarn", terrwarn);
			pedit.putBoolean("autosync", autosync);
			pedit.putBoolean("cvr", cvr);
			pedit.putBoolean("sideview", sideview);
			pedit.putBoolean("nmea_udp", nmea_udp);
			
			pedit.commit();
			if (airspace!=null && autosync)
			{
				AutoSyncService.schedule(this.getApplicationContext(), airspace,true);
			}
			else
			{
				AutoSyncService.cancel(this.getApplicationContext());
			}
			if (cvr)
				this.cvr.start();
			else
				this.cvr.stop();

			String then = data.getStringExtra("se.flightplanner2.thenopen");
			map.update_detail(
					getSharedPreferences("se.flightplanner2.prefs",MODE_PRIVATE).getInt("mapdetail", 2),
					getSharedPreferences("se.flightplanner2.prefs",MODE_PRIVATE).getBoolean("northup", false),
					getSharedPreferences("se.flightplanner2.prefs",MODE_PRIVATE).getBoolean("sideview", false));
			globalposition.enableUdpNmea(
					getSharedPreferences("se.flightplanner2.prefs",MODE_PRIVATE).getBoolean("nmea_udp", false));
			if (prevstorage==null)
				prevstorage="";
			if (storage==null)
				storage="";
			if (!storage.equals(prevstorage))
			{
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setMessage("You have modified the map storage path. The applications must be restarted. The application will now close, please start it again after it closes!")
				.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
				    public void onClick(DialogInterface dialog, int id) {
				    	Nav.this.finish();
						Nav.this.cvr.stop();
						Nav.this.overridePendingTransition(0, 0);
				    }
				});
				AlertDialog diag=builder.create();
				diag.show();
				return;
			}
			if (then != null && then.equals("loadterrain")) {
				sync();
				return;
			} else if (then != null && then.equals("viewrec")) {
				viewRecordings();
				return;
			}
		}
	}

	private Location last_location;

	private boolean isNetworkAvailable() {
		ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetworkInfo = connectivityManager
				.getActiveNetworkInfo();
		return activeNetworkInfo != null;
	}

	private boolean do_load_trips = false;

	private void loadTrip() {
		if (true) {
			
			Date last_load_terrain=BackgroundMapDownloader.get_last_sync(getStoragePath());
			if (isNetworkAvailable()
					&& (new Date().getTime() - last_load_terrain.getTime() > 60 * 15 * 1000 || last_load_terrain.getTime() == 0)) {
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setMessage(
						Html.fromHtml(
								"It has been some time since you ran sync. Sync data before displaying trip list? If you choose not to, you won't see any changes to trips since last sync.",
								null, null))
						.setCancelable(true)
						.setPositiveButton("Sync first",
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int id) {
										dialog.dismiss();
										do_load_trips = true;
										sync();
									}
								})
						.setNegativeButton("Select trip",
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int id) {
										dialog.dismiss();
										selectTrip();
									}
								});
				AlertDialog diag = builder.create();
				diag.show();
			} else {
				long ago = new Date().getTime() - last_load_terrain.getTime();
				Toast toast = Toast.makeText(this, String.format(
						"Last sync: %d minutes ago", (int) (ago / 60000)),
						Toast.LENGTH_SHORT);
				toast.show();
				do_load_trips = false;
				selectTrip();
			}
		}
		/*
		 * else { do_load_trips=true; loadTerrain(); }
		 */
	}

	private String getStoragePath() {
		return getSharedPreferences("se.flightplanner2.prefs",MODE_PRIVATE).getString("storage", "");		
	}


	private void selectTrip() {
		do_load_trips = false;
		if (airspace == null) {
			RookieHelper
					.showmsg(
							this,
							"You have no airspace or trip data. Select Menu->Sync to download the latest data!");
			return;
		}

		final String[] trips = airspace.getTripList();

		if (trips.length == 0) {
			RookieHelper
					.showmsg(this,
							"You have no trips! Go to www.flightplanner.se and create some!");
		} else {
			final Nav nav = this;
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Choose Trip");
			builder.setItems(trips, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int item) {
					nav.loadSelectedTrip(trips[item]);
				}
			});
			AlertDialog diag = builder.create();
			diag.show();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	Listener gpsstatuslistener = new Listener() {
		@Override
		public void onGpsStatusChanged(int event) {
			if (locman == null)
				return;
			if (event == GpsStatus.GPS_EVENT_SATELLITE_STATUS) {
				GpsStatus st = locman.getGpsStatus(null);
				int satcnt = 0;
				int satfixcnt = 0;
				for (GpsSatellite sat : st.getSatellites()) {
					/*Log.i("fplan.gps",
							"Sat: " + sat.getSnr() + " fix: " + sat.usedInFix()
									+ " ");*/
					if (sat.usedInFix())
						satfixcnt += sat.getSnr();
					satcnt += 1;
				}
				map.set_gps_sat_cnt(satcnt, satfixcnt);
			}

		}
	};

	public void handleMoreMEnu(int item)
	{
		switch(item)
		{
		case MENU_FINISH:
			finish();
			cvr.stop();
			overridePendingTransition(0, 0);
			break;
		case MENU_SETTINGS: {
			Intent intent = getSettingsIntent();
			intent.putExtra("se.flightplanner2.thenopen", "nothing");
			startActivityForResult(intent, SETUP_INFO);
			break;
		}
		case MENU_VIEW_RECORDINGS: {
			if (!haveUserAndPass()) {
				Intent intent = getSettingsIntent();
				intent.putExtra("se.flightplanner2.thenopen", "viewrec");
				startActivityForResult(intent, SETUP_INFO);
			} else {
				viewRecordings();
			}

			break;
		}
		case MENU_HELP: {
			Intent intent = new Intent(this, HelpActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
			startActivity(intent);
			break;
		}
		case MENU_LOGIN: {
			if (!haveUserAndPass()) {
				Intent intent = getSettingsIntent();
				intent.putExtra("se.flightplanner2.thenopen", "loadtrip");
				startActivityForResult(intent, SETUP_INFO);
			} else {
				loadTrip();

			}

			break;
		}
		
		}
	}
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_VIEW_CHARTS: {
			viewAdChart();
			break;
		}
		case MENU_MORE: {
			final Nav nav = this;
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Choose Option");
			final int[] choices=new int[]{
					MENU_HELP,
					MENU_LOGIN,
					MENU_SETTINGS,
					MENU_VIEW_RECORDINGS,
					MENU_FINISH,
					MENU_BACK
			};
			final String[] items=new String[]{
					"Help",
					"Select Trip",
					"Settings",
					"Recorded Trips",
					"Exit",
					"Back"
			};
			builder.setItems(items, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int item) {
					if (item>=0 && item<choices.length)
						nav.handleMoreMEnu(choices[item]);
				}
			});
			AlertDialog diag = builder.create();
			diag.show();
			break;
			
		}
		case MENU_SYNC:
			try {
				do_sync();
			} catch (Exception e) {
				e.printStackTrace();
				RookieHelper.showmsg(this, e.toString());
			}
			return true;

		case MENU_DESCPOS: {
			Intent intent = new Intent(this, DescribePosition.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
			if (last_location != null) {
				startActivity(intent);
			} else {
				RookieHelper.showmsg(this, "Position Unknown");
			}
			break;
		}
		case MENU_SIMPLER: {
			doShowAirspaces();
			break;
		}
		case MENU_PHRASES: {
			Intent intent = new Intent(this, PhrasesActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
			startActivity(intent);
			break;
		}

		}
		return false;
	}

	public void doShowAirspaces() {
		Intent intent = new Intent(this, SimplerActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
		if (last_location != null) {
			startActivity(intent);
		} else {
			RookieHelper.showmsg(this, "Position Unknown");
		}

	}

	@Override
	public void showAirspaces() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		StringBuilder spacestr = new StringBuilder();
		final ArrayList<AirspaceArea> areas = proxdet.getAreas();
		if (areas == null)
			return;

		for (AirspaceArea a : areas) {
			spacestr.append("<b>" + a.name + "</b><br />");
		}
		builder.setMessage(
				Html.fromHtml("<h1>Airspace Ahead</h1>"
						+ "<p>You are approaching the following airspaces:</p>"
						+ spacestr.toString(), null, null))
				.setCancelable(true)
				.setPositiveButton("Mark cleared",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dialog.dismiss();
								Date now = new Date();
								for (AirspaceArea a : areas)
									a.cleared = now.getTime();
								clearper.save(lookup);

							}
						})
				.setNeutralButton("More Info",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dialog.dismiss();
								doShowAirspaces();
							}
						})
				.setNegativeButton("Cancel",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dialog.dismiss();
							}
						});
		AlertDialog diag = builder.create();
		diag.show();
	}

	private void do_sync() {
		if (AppState.terraindownloader != null) {
			RookieHelper.showmsg(this, "Sync already in progress!");
		} else {
			if (!haveUserAndPass()) {
				Intent intent = getSettingsIntent();
				intent.putExtra("se.flightplanner2.thenopen", "loadterrain");
				startActivityForResult(intent, SETUP_INFO);
			} else
				sync();
		}
	}

	private void viewRecordings() {
		try {
			Intent intent = new Intent(this, ViewRecordings.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
			intent.putExtra("se.flightplanner2.user",
					getSharedPreferences("se.flightplanner2.prefs",MODE_PRIVATE).getString("user", ""));
			intent.putExtra("se.flightplanner2.password",
					getSharedPreferences("se.flightplanner2.prefs",MODE_PRIVATE).getString("password", ""));
			fplog.saveCurrent(lookup);
			startActivityForResult(intent, VIEW_RECORDINGS);
		} catch (Exception e) {
			RookieHelper.showmsg(this, e.getMessage());
		}
	}

	private void viewAdChart() {
		final ArrayList<String> icaos = new ArrayList<String>();
		final ArrayList<String> humanReadableNames = new ArrayList<String>();
		if (airspace != null) {
			LatLon latlon = null;
			if (last_location != null)
				latlon = new LatLon(last_location.getLatitude(),
						last_location.getLongitude());
			lookup.getAdChartNames(icaos, humanReadableNames, latlon);
		}

		if (icaos.size() == 0) {
			RookieHelper
					.showmsg(this, "No aerodromes found. Run 'Sync' again.");
		} else {
			final Nav nav = this;
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Choose Airport");
			builder.setItems(humanReadableNames.toArray(new String[] {}),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int item) {
							if (icaos.get(item) == null) { // clicked on the
															// divider between
															// close airports
															// and
															// alphabetically
															// sorted airports.
								return;
							}
							nav.loadSelectedAd(icaos.get(item));
						}
					});
			AlertDialog diag = builder.create();
			diag.show();
		}
	}

	protected void loadSelectedAd(String icao) {
		Intent intent = new Intent(this, ViewAdInfo.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
		intent.putExtra("se.flightplanner2.icao", icao);
		Log.i("fplan.chart", "After calling put Serializable");
		map.releaseMemory();
		startActivityForResult(intent,ADINFO);
	}

	private Intent getSettingsIntent() {
		Intent intent = new Intent(this, SetupInfo.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
		intent.putExtra("se.flightplanner2.user", getSharedPreferences("se.flightplanner2.prefs",MODE_PRIVATE)
				.getString("user", "user"));
		intent.putExtra("se.flightplanner2.password",
				getSharedPreferences("se.flightplanner2.prefs",MODE_PRIVATE).getString("password", "password"));
		intent.putExtra("se.flightplanner2.storage",
				getSharedPreferences("se.flightplanner2.prefs",MODE_PRIVATE).getString("storage", ""));
		int mapd = getSharedPreferences("se.flightplanner2.prefs",MODE_PRIVATE).getInt("mapdetail", 2);
		intent.putExtra("se.flightplanner2.mapdetail", mapd);
		intent.putExtra("se.flightplanner2.northup",
				getSharedPreferences("se.flightplanner2.prefs",MODE_PRIVATE).getBoolean("northup", false));
		intent.putExtra("se.flightplanner2.vibrate",
				getSharedPreferences("se.flightplanner2.prefs",MODE_PRIVATE).getBoolean("vibrate", false));
		intent.putExtra("se.flightplanner2.terrwarn",
				getSharedPreferences("se.flightplanner2.prefs",MODE_PRIVATE).getBoolean("terrwarn", false));
		intent.putExtra("se.flightplanner2.autosync",
				getSharedPreferences("se.flightplanner2.prefs",MODE_PRIVATE).getBoolean("autosync", false));
		intent.putExtra("se.flightplanner2.cvr",
				getSharedPreferences("se.flightplanner2.prefs",MODE_PRIVATE).getBoolean("cvr", false));
		intent.putExtra("se.flightplanner2.sideview",
				getSharedPreferences("se.flightplanner2.prefs",MODE_PRIVATE).getBoolean("sideview", false));
		intent.putExtra("se.flightplanner2.nmea_udp",
				getSharedPreferences("se.flightplanner2.prefs",MODE_PRIVATE).getBoolean("nmea_udp", false));
		// RookieHelper.showmsg(this,"Got mapd"+mapd);
		intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
		return intent;
	}

	private boolean haveUserAndPass() {
		String user = getSharedPreferences("se.flightplanner2.prefs",MODE_PRIVATE).getString("user", null);
		String pass = getSharedPreferences("se.flightplanner2.prefs",MODE_PRIVATE).getString("password", null);
		if (user == null || user.length() == 0)
			return false;
		if (pass == null || pass.length() == 0)
			return false;
		return true;
	}

	private void sync() {

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		Date last = BackgroundMapDownloader.get_last_sync(getStoragePath());
		long now = new Date().getTime();
		String minago;
		if (last == null)
			minago = "This is the first time a sync is made. This will download large ammounts of data. Go ahead?";
		else {
			long delta = now - last.getTime();
			if (delta > 3600000l * 24 * 60)
				minago = "This is the first time a sync is made in a very long time. This will possibly download large ammounts of data. Go ahead?";
			else {
				long minutes = delta / 60000l;
				if (minutes < 90)
					minago = "Last sync was " + minutes
							+ " minutes ago. Sync now?";
				else {
					long hours = delta / (3600000l);
					if (hours < 36)
						minago = "Last sync was " + hours
								+ " hours ago. Sync now?";
					else {
						long days = delta / (3600000l * 24l);
						minago = "Last sync was " + days
								+ " days ago. Sync now?";
					}
				}
			}
		}

		builder.setMessage(minago)
				.setCancelable(true)
				.setPositiveButton("Yes, Sync",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dialog.dismiss();
								real_do_sync();
							}
						})
				.setNegativeButton("No, Don't",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dialog.dismiss();
							}
						});
		AlertDialog diag = builder.create();
		diag.show();
	}

	/*
	 * protected Dialog onCreateDialog(int id) { if (id==SETTINGS_DIALOG) {
	 * Context mContext = getApplicationContext(); assert mContext!=null; Dialog
	 * dialog = new Dialog(mContext);
	 * 
	 * dialog.setContentView(R.layout.settingsinfo);
	 * dialog.setTitle("Settings");
	 * 
	 * //TextView text = (TextView) dialog.findViewById(R.id.edittext);
	 * //text.setText("Default!"); return dialog; } else { assert false; return
	 * null; } }
	 */
	private BroadcastReceiver batteryReceiver=new BroadcastReceiver(){  
	    @Override  
	    public void onReceive(Context arg0, Intent intent) {  
	      int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
	      int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
	      map.set_battery_level(level,plugged!=0);
	        
	    }  
	  };
	private CVR cvr;
	@Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);

		final String storage = getSharedPreferences("se.flightplanner2.prefs",MODE_PRIVATE).getString("storage","");
    	File extpath = Storage.getStorage(storage);
		File aiptextpath = new File(extpath,
				Config.path);
		aiptextpath.mkdirs();

    	UpgradeFromv1.upgradeIfNeeded();
    	getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
    	
    	final NavData data = (NavData) getLastNonConfigurationInstance();
    	
    	AppState.gui=this;
		
		locman=(LocationManager)getSystemService(Context.LOCATION_SERVICE);
		locman.addGpsStatusListener(gpsstatuslistener);		
		globalposition=new GlobalPositionImpl(locman);
		GlobalPosition.pos=globalposition;
		globalposition.registerSubscriber(this);
    	
    	tripstate=new TripState(null);
		GlobalTripState.tripstate=tripstate;
    	//setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    	fplog=new FlightPathLogger(Storage.getStoragePath(this));
    	
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
        
        if (airspace!=null)
        {
    		if (getSharedPreferences("se.flightplanner2.prefs",MODE_PRIVATE).getBoolean("autosync", false))
    		{        	
    			AutoSyncService.schedule(this.getApplicationContext(), airspace,false);
    		}
        }
        
		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		
		

        tripstate=new TripState(tripdata);
		GlobalTripState.tripstate=tripstate;
        
        map=new MovingMap(this,metrics,fplog,this,tripstate,Storage.getStoragePath(this));
        map.update_airspace(airspace,lookup,getSharedPreferences("se.flightplanner2.prefs",MODE_PRIVATE).getInt("mapdetail", 2),
        		getSharedPreferences("se.flightplanner2.prefs",MODE_PRIVATE).getBoolean("northup", false),
        		getSharedPreferences("se.flightplanner2.prefs",MODE_PRIVATE).getBoolean("sideview", false)
        		);
        map.update_tripdata(tripdata,tripstate);
		map.set_altimeter(getAltimeterSetting(),qnh);
		map.thisSetContentView(this);
		map.gps_update(null,false,0);

		
    	this.registerReceiver(batteryReceiver,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));  
		
		
		proxdet=new AirspaceProximityDetector(lookup,5);
		clearper=new ClearancePersistence(Storage.getStoragePath(this));
		GlobalClearancePersistence.clearper=clearper;
		vibrator= (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		warner=new AirspaceWarner(proxdet);
		if (airspace!=null && airspace.spaces!=null)
			clearper.load(airspace.spaces,Storage.getStoragePath(this));
		
		if (Config.debugMode() && Config.gpsdrive)
		{
			SensorManager sensorManager=(SensorManager) getSystemService(SENSOR_SERVICE);
	        sensorManager.registerListener(
	        		new SensorEventListener() {						
						@Override
						public void onSensorChanged(SensorEvent event) {
							
							int SPD=1;
							float speed_sign=-1;
							int TRN=0;
							int DOWN=2;
							///Log.i("fplan.sensor","Sensors: X: "+event.values[1]+" Y: "+event.values[0]+" Z: "+event.values[2]);
							float speed=-speed_sign*event.values[SPD]*40;
							if (speed<0) speed=0;
							float turn_rate=0;
							float bank_angle=(float)Math.atan2(event.values[TRN],event.values[DOWN]);
							if (speed>1 && Math.abs(bank_angle)>0.01)
							{
								turn_rate = Project.getTurnRate(speed, bank_angle);
							}
							GlobalPosition.pos.setDebugTurn(turn_rate);	
							GlobalPosition.pos.setDebugSpeed(speed);
						}

						
						
						@Override
						public void onAccuracyChanged(Sensor sensor, int accuracy) {
						}
					},
	        		 sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
		}
		if (true /* check altimeter setting*/)
		{
			final SensorManager sensorManager=(SensorManager) getSystemService(SENSOR_SERVICE);
			Sensor sensor=sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);		
			if (sensor!=null)
			{
		        sensorManager.registerListener(
		        		new SensorEventListener() {			
		        			//float test;
							@Override
							public void onSensorChanged(SensorEvent event) {
								double pressure_value = event.values[0];
								
								/*test-=20;
								if (test<-750)
									test=0;
								*/
								//pressure_value+=test;
								//Log.i("fplan.pressure","Current pressure: "+pressure_value);
								//double h_self=AltitudeCalculator.getAltitude(1013.0f,pressure_value);
								//float h_android=sensorManager.getAltitude(1013.0f, (float)pressure_value);
								//Log.i("fplan.pressure","Android alt: "+h_android/0.3048+" ft, our alt: "+(float)h_self/0.3048+" ft");
								map.pressure_update(pressure_value);
							}						
							@Override
							public void onAccuracyChanged(Sensor sensor, int accuracy) {
							}
						},
		        		 sensor, SensorManager.SENSOR_DELAY_NORMAL);
			}
		}
		cvr=new CVR(Storage.getStoragePath(this));
		if (getSharedPreferences("se.flightplanner2.prefs",MODE_PRIVATE).getBoolean("cvr", false))			
			cvr.start();
		
		globalposition.enableUdpNmea(
				getSharedPreferences("se.flightplanner2.prefs",MODE_PRIVATE).getBoolean("nmea_udp", false));
    }

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		map.invalidate();
	}

	private double offlat = 0;
	private double offlon = 0;
	private ClearancePersistence clearper;
	private AirspaceProximityDetector proxdet;
	private Vibrator vibrator;
	private AirspaceWarner warner;
	private BearingSpeedCalc bearingspeed = new BearingSpeedCalc();

	
	
	@Override
	public void gps_update(Location location) {
		// Log.i("fplan","Location changed");

		warner.run(location, (getSharedPreferences("se.flightplanner2.prefs",MODE_PRIVATE).getBoolean(
				"vibrate", false)) ? vibrator : null, this);
		clearper.update(location, lookup);
		map.proxwarner_update(warner.getWarning());
		int amp=0;
		if (cvr.recording)
		{
			amp=cvr.getMaxAmp();
		}
		map.gps_update(location,
				getSharedPreferences("se.flightplanner2.prefs",MODE_PRIVATE).getBoolean("terrwarn", false),amp);
		last_location = location;
		// RookieHelper.showmsg(this,
		// ""+location.getLatitude()+","+location.getLongitude());
		// locman.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500,5,
		// this);
	}

	@Override
	public void gps_disabled() {
		map.gps_disabled();
	}

	@Override
	public void onPause() {
		try {
			fplog.saveCurrent(lookup);
		} catch (IOException e) {
			e.printStackTrace();
		}
		super.onPause();
	}

	@Override
	public void onDestroy() {
		map.stop();
		cvr.stop();

    	AppState.gui=null;

		try {
			fplog.saveCurrent(lookup);			
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (globalposition!=null) globalposition.stop();
		if (locman!=null && gpsstatuslistener != null)
			locman.removeGpsStatusListener(gpsstatuslistener);
		
		if (batteryReceiver!=null)
			this.unregisterReceiver(batteryReceiver);
		GlobalLookup.lookup = null;
		super.onDestroy();
	}

	@Override
	public void onProgress(String prog) {
		map.set_download_status(prog, false);
	}

	
	
	@Override
	public void onFinish(Airspace airspace, AirspaceLookup lookup, String error) {
		AppState.terraindownloader = null;
		if (airspace != null) {
			this.airspace = airspace;
			this.lookup = lookup;
			GlobalLookup.lookup = lookup;
		}
		Log.i("fplan", "Finish:" + error);
		if (error != null) {
			map.set_download_status(error, true);
		} else {
			map.set_download_status("Complete", true);
		}

		if (airspace != null) {
			proxdet.update_lookup(lookup);
			map.update_airspace(airspace, lookup, getSharedPreferences("se.flightplanner2.prefs",MODE_PRIVATE).getInt("mapdetail", 2), 
					getSharedPreferences("se.flightplanner2.prefs",MODE_PRIVATE).getBoolean("northup", false),
					getSharedPreferences("se.flightplanner2.prefs",MODE_PRIVATE).getBoolean("sideview", false)					
					);
		}
		map.enableTerrainMap(true);
		
		if (getSharedPreferences("se.flightplanner2.prefs",MODE_PRIVATE).getBoolean("autosync", false))
		{
			if (airspace!=null)
				AutoSyncService.schedule(this.getApplicationContext(), airspace,true);
			else
			if (this.airspace!=null)
				AutoSyncService.schedule(this.getApplicationContext(), this.airspace,true);
		}

		if (do_load_trips) {
			selectTrip();
		}
	}

	@Override
	public void cancelMapDownload() {
		Log.i("fplan", "cancelMapDownload:" + AppState.terraindownloader);
		if (AppState.terraindownloader != null) {
			AppState.terraindownloader.cancel(true);
		}
	}

	// AsyncTask<Void,Void,TripData> load_trip_task;
	private void loadSelectedTrip(final String trip) {
		final Nav nav = this;
		if (airspace != null)
			try {
				TripData td = airspace.getTrip(trip);
				if (td == null)
					throw new RuntimeException("No trip with that name.");
				if (td.waypoints.size() == 0) {
					RookieHelper
							.showmsg(this,
									"This trip has no waypoints. Go to www.swflightplanner.se and create some!");
				}
				nav.tripdata = td;
				tripstate = new TripState(nav.tripdata);
				nav.tripstate=tripstate;
				GlobalTripState.tripstate = tripstate;
				map.update_tripdata(nav.tripdata, tripstate);
				tripstate.reupdate();
				nav.tripdata.serialize_to_file(nav, "tripdata.bin");
			} catch (Throwable e) {
				RookieHelper.showmsg(nav, e.toString());
			}
	}

	private void showADInfo(final Place place) {

		if (place.getAerodrome() != null) {
			map.releaseMemory();
			SigPoint sp = place.getAerodrome();
			if (sp.extra != null && sp.extra.icao != null
					&& !sp.extra.icao.equals(""))
				loadSelectedAd(sp.extra.icao);
		} else if (place.getDetailedPlace() != null) {
			map.releaseMemory();
			Intent intent = new Intent(this, DetailedPlaceActivity.class);
			GlobalDetailedPlace.detailedplace = place.getDetailedPlace();
			intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
			//Log.i("fplan", "tripstate = " + GlobalDetailedPlace.detailedplace);
			startActivity(intent);			
			
		} else if (place.getLatLon() != null) {
			map.releaseMemory();
			Intent intent = new Intent(this, ViewAdInfo.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
			intent.putExtra("se.flightplanner2.latlon", place.getLatLon());			
			//GlobalDetailedPlace.detailedplace = place.getDetailedPlace();
			//Log.i("fplan", "tripstate = " + GlobalDetailedPlace.detailedplace);
			startActivityForResult(intent,ADINFO);
		}

		/*
		 * } StringBuilder sb=new StringBuilder();
		 * 
		 * sb.append("<h1>"+sp.name+"</h1>"); if (sp.extra!=null) { if
		 * (sp.icao!=null) sb.append("<p>("+sp.icao+")</p>"); if
		 * (sp.metar!=null) { sb.append("<h2>METAR:</h2><p> "+sp.metar+"</p>");
		 * } if (sp.taf!=null) { sb.append("<h2>TAF:</h2><p> "+sp.taf+"</p>"); }
		 * if (sp.notams.length>0) { sb.append("<h2>NOTAMs:</h2>"); for(String
		 * notam:sp.notams) { sb.append("<p><pre>"+notam+"</pre></p>"); }
		 * 
		 * } RookieHelper.showmsg(Nav.this, sb.toString());
		 */
	}

	@Override
	public void doShowExtended(Place[] places) {
		ArrayList<String> airports = new ArrayList<String>();
		final ArrayList<Place> sigs = new ArrayList<Place>();
		for (Place place : places) {
			// SigPoint airport=lookup.getByIcao(icao);
			// if (airport==null) continue;
			sigs.add(place);
			airports.add(place.getHumanName());
		}
		if (airports.size() > 0) {
			if (airports.size() == 1) {
				showADInfo(sigs.get(0));
			} else {
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle("Choose Airport");
				builder.setItems(airports.toArray(new String[] {}),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int item) {
								showADInfo(sigs.get(item));
							}
						});
				AlertDialog diag = builder.create();
				diag.show();
			}
		}
	}


	@Override
	public void do_sync_now() {
		if (!isNetworkAvailable()) return;
		real_do_sync();
	}


	private void real_do_sync() {
		if (AppState.terraindownloader!=null)
		{
			Log.i("fplan.autosync","Sync already in progress");
			return;
		}
		final String user = getSharedPreferences("se.flightplanner2.prefs",MODE_PRIVATE).getString("user",
				"user");
		final String pass = getSharedPreferences("se.flightplanner2.prefs",MODE_PRIVATE).getString("password",
				"password");
		final String storage = getSharedPreferences("se.flightplanner2.prefs",MODE_PRIVATE).getString("storage",
				"");
		final int detail = getSharedPreferences("se.flightplanner2.prefs",MODE_PRIVATE).getInt("mapdetail", 2);
		AppState.terraindownloader = new BackgroundMapDownloader(
				Nav.this, user, pass, detail, storage);
		Log.i("fplan", "Previous airspace: " + airspace);
		map.enableTerrainMap(false);
		AppState.terraindownloader.execute(airspace);
	}



	int qnh=0;
	public String getAltimeterSetting()
	{
		final String alti = getSharedPreferences("se.flightplanner2.prefs",MODE_PRIVATE).getString("altimeter","GPS");
		if (qnh==0 && alti.equals("Baro Alt")) return "GPS";
		return alti;
		
	}

	@Override
	public void toggleAltimeter() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Altimeter Setting");
		String curr=getAltimeterSetting();
		String[] choices=new String[]{
				"GPS",
				"Baro FL",
				"Baro Alt",
		};
		if (curr.equals("GPS"))
			choices[0]="*"+choices[0];
		if (curr.equals("Baro FL"))
			choices[1]="*"+choices[1];
		if (curr.equals("Baro Alt"))
			choices[2]="*"+choices[2];
		builder.setItems(choices, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int item) {
				String setting="GPS";
				switch(item)
				{
				case 0:
					setting="GPS";
					qnh=0;
					break;
				case 1:
					setting="Baro FL";					
					break;
				case 2:
					setting="Baro Alt";
					break;
				}
				SharedPreferences prefs = getSharedPreferences("se.flightplanner2.prefs",MODE_PRIVATE);
				SharedPreferences.Editor pedit = prefs.edit();
				pedit.putString("altimeter", setting);				
				pedit.commit();
				if (setting.equals("Baro Alt"))
					Nav.this.setQnh();
				dialog.dismiss();
				map.set_altimeter(getAltimeterSetting(),qnh);
				
			}
		});
		AlertDialog diag = builder.create();
		diag.show();
	}


	protected void setQnh() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Set QNH");

		final ListView listView=new ListView(this);
		String[] theList=new String[250];
		final int lowestQnh=850;
		Log.i("fplan.regexp","Time to guess qnh, based on location: "+last_location);
		QnhGuessResult guess=null;
		if (last_location != null)
		{
			LatLon latlon = new LatLon(last_location.getLatitude(),
					last_location.getLongitude());

			ClosestAirportResult qg=lookup.getClosestAirportWithMetar(latlon);
			if (qg!=null)
			{
				Log.i("fplan.regexp","Closest airpo: "+qg.icao+" at "+qg.distance);
				guess=lookup.GuessQnhFromMetar(qg.metar, qg.icao);
				if (guess!=null)
					Log.i("fplan.regexp","QNH guess: "+guess.qnh+" from: "+guess.descr);
			}
		}		
		for(int i=0;i<250;++i)
		{
			int curqnh=lowestQnh+i;
			String s=""+(curqnh);
			if (curqnh==qnh)
				s="*"+s;
			if (guess!=null && guess.qnh==curqnh)
				s=s+guess.descr;
			theList[i]=s;
		}
		listView.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,theList));
		builder.setView(listView);
		final AlertDialog diag = builder.create();
		listView.setOnItemClickListener(new OnItemClickListener(){
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int item,
					long arg3) {				
				Nav.this.qnh=lowestQnh+item;
				map.set_altimeter(getAltimeterSetting(),Nav.this.qnh);
				diag.dismiss();
			}			
		});
		/*
		builder.setItems(choices, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int item) {
				Nav.this.qnh=lowest+item;
				dialog.dismiss();
				
			}
		})
		*/;
		diag.show();
		int presel_qnh=qnh>0 ? qnh : 1013;
		int presel_idx=presel_qnh-lowestQnh-2;
		if (presel_idx<0) presel_idx=0;
		if (presel_idx>=theList.length) presel_idx=theList.length-2;
		final int final_presel_idx=presel_idx;
		listView.post(new Runnable(){
			@Override
			public void run() {
				listView.setSelection(final_presel_idx);
			}
		});
		
	}

}