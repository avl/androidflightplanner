package se.flightplanner2;

import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;

import se.flightplanner2.AppState.GuiIf;
import se.flightplanner2.BackgroundMapDownloader.BackgroundMapDownloadOwner;
import se.flightplanner2.TripData.Waypoint;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

public class AutoSyncService extends Service implements BackgroundMapDownloadOwner {

	static class MyBinder extends Binder {
		public WeakReference<AutoSyncService> mService;

		AutoSyncService getService() {
			return mService.get();
		}

	}

	private MyBinder binder = new MyBinder();
	{
		binder.mService = new WeakReference<AutoSyncService>(this);
	}

	static public void schedule(Context appctx, Airspace space,boolean toast) {
		ArrayList<Long> wakeups = new ArrayList<Long>();
		if (space==null)
		{
			Log.i("fplan.autosync","in schedule space==null");
			return;
		}
		long nowsec = new Date().getTime() / 1000l;
		
		long lastsyncsec=BackgroundMapDownloader.get_last_sync(getStorage(appctx)).getTime()/1000;
		
		for (TripData trip : space.trips) {
			for (int i = 0; i < trip.waypoints.size(); ++i) {
				Waypoint wp = trip.waypoints.get(i);
				if (i == 0 || wp.land_at_end) {
					long fivemin_bef = wp.depart_dt - 60 * 5;
					long thirtymin_bef = wp.depart_dt - 60 * 30;
					if (fivemin_bef > nowsec)
						wakeups.add(fivemin_bef);
					if (thirtymin_bef > nowsec)
						wakeups.add(thirtymin_bef);
				}
			}
		}
		if (wakeups.size()==0)
			return;
		long next = Collections.min(wakeups);
		long now=new Date().getTime()/1000l;
		Intent intent = new Intent(appctx, AutoSyncService.class);
		PendingIntent pendingIntent = PendingIntent.getService(appctx, 0,
				intent, 0);

		AlarmManager alarmManager = (AlarmManager) appctx
				.getSystemService(Context.ALARM_SERVICE);
		
		if (next-lastsyncsec<60*5)
			next=lastsyncsec+60*5;
		Log.i("fplan.autosync","Scheduling wakeup in "+(next-now)/(60l)+" minutes = "+new Date(next*1000l));
		if (toast)
		{
			
			
			
			long mins=(next-now)/(60l);
			String timestr;
			if (mins==0)
				timestr="1 minute";
			else if (mins<=90)
				timestr=""+mins+" minutes";
			else
			{
				long hours=mins/60;
				if (hours==1)
					timestr="1 hour";
				else if (hours<48)
					timestr=""+hours+" hours";
				else
				{
					long days=hours/24;
					if (days==1)
						timestr="1 day";
					else
						timestr=""+days+" days";
				}
				
			}
				
			Toast.makeText(appctx, "Autosync active, running in: "+timestr, Toast.LENGTH_LONG).show();
		}

		alarmManager.set(AlarmManager.RTC, next * 1000l, pendingIntent);
	}



	private static String getStorage(Context appctx) {
		return appctx.getSharedPreferences("se.flightplanner2.prefs",MODE_PRIVATE).getString("storage", "");
	}
	
	

	@Override
	public void onCreate() {
		super.onCreate();
		Log.i("fplan.autosync", "UpdateService oncreate");
		
		if (AppState.gui==null)
		{
			SharedPreferences shp = getSharedPreferences("se.flightplanner2.prefs",MODE_PRIVATE);
			String user=shp.getString("user", "");
			String pass=shp.getString("password", "");
			int mapdetail = shp.getInt("mapdetail", 0);
			AppState.terraindownloader=new BackgroundMapDownloader(this, user, pass, mapdetail, Storage.getStoragePath(this));
			Airspace airspace=null;
			try{
				airspace=Airspace.deserialize_from_file(this,"airspace.bin");
				GlobalLookup.lookup=new AirspaceLookup(airspace);
			}
			catch(Exception e)
			{
				e.printStackTrace();
				Log.i("fplan.autosync","Failed to load airspace from file");
			}
			
			AppState.terraindownloader.execute(airspace);
		}
		else
		{
			Log.i("fplan.autosync", "GUI Present, handing off sync task to GUI instead.");
			AppState.gui.do_sync_now();
			stopSelf();
		}				
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.i("fplan.autosync", "onDestroy");
	}

	@Override
	public IBinder onBind(Intent intent) {

		Log.i("fplan.autosync", "AutosyncService onBind");
		super.onUnbind(intent);
		return binder;
	}

	@Override
	public boolean onUnbind(Intent intent) {

		return false;
	}

	@Override
	public void onProgress(String prog) {
		Log.i("fplan.autosync", "Progress: "+prog);
		
	}

	@Override
	public void onFinish(Airspace airspace, AirspaceLookup lookup, String error) {
		AppState.terraindownloader=null;
		Log.i("fplan.autosync", "onFinish: error: "+error);
		GuiIf gui=AppState.gui;
		if (gui!=null)
		{
			Log.i("fplan.autosync","GUI has come up, handing off late to GUI");
			gui.onFinish(airspace, lookup, error);
		}
		if (lookup!=null && lookup.airspace!=null)
			schedule(this, airspace,false);
		stopSelf();
		
	}



	public static void cancel(Context appctx) {
		AlarmManager alarmManager = (AlarmManager) appctx
				.getSystemService(Context.ALARM_SERVICE);
		Intent intent = new Intent(appctx, AutoSyncService.class);
		PendingIntent pendingIntent = PendingIntent.getService(appctx, 0,
				intent, 0);
		alarmManager.cancel(pendingIntent);	
	}

}
