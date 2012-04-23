package se.flightplanner;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import se.flightplanner.Project.LatLon;
import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class DetailedPlaceActivity extends Activity{
	private TableLayout mytable;
	private TextView addRow(String title)
	{
		TextView value=new TextView(this);
		value.setTextSize(20);
		value.setGravity(Gravity.CENTER);
		return (TextView)addRow(title,value);
	}
	private<VT> VT addRow(String title,VT value)
	{
		TableRow row=new TableRow(this);		
		TextView label=new TextView(this);
		label.setTextSize(15);
		label.setText(title);
		row.addView(label);
		row.addView((View)value);		
		mytable.addView(row);
		return value;		
	}
	
	//private SigPoint sp;
	private TextView ETA2;
	private TextView d;
	private TextView gs;
	private TextView hdg;
	private TextView delay;
	private TextView planned_field;
	private TextView waypoint_hdg;
	private CompassRoseView compass;
	private LocationManager locman;
	
	private void fail()
	{
		ETA2.setText("--");
		d.setText("--");
		gs.setText("--");
		hdg.setText("--");		
		if (delay!=null) delay.setText("--");
		if (planned_field!=null) planned_field.setText("--");
		if (waypoint_hdg!=null) waypoint_hdg.setText("--");
			
		
	}
	/*
	private static String fmttime(long when) {
		if (when == 0 || when > 3600 * 24 * 10)
			return "--:--";
		return String.format("%d:%02d", when / 60, when % 60);
	}*/
	
	BearingSpeedCalc bsca=new BearingSpeedCalc();
	private void update_location(Location location)
	{
		if (location==null || place==null)
		{
			fail();
			return;
		}
		
		gs.setText(""+(int)(location.getSpeed()*3.6f/1.852f)+"kt");

		
		//Log.i("fplan.tmp","Update location:"+location.getLatitude()+","+location.getLongitude());
		location=bsca.calcBearingSpeed(location);
		LatLon ownlatlon=new LatLon(location);
		place.update_pos(location);
		
		double dv=place.getDistance();
		String dtxt=(dv>=0) ? String.format("%.2fNM", dv) : "--";
		d.setText(dtxt);
		//Log.i("fplan.tmp","Settign distance to:"+dtxt);		
		//Date when=tripstate.getETA2();
		
		//GregorianCalendar greg=new GregorianCalendar(TimeZone.getTimeZone("UTC"),Locale.ROOT);
		SimpleDateFormat df=new SimpleDateFormat("HH:mm",Locale.US);
		df.setTimeZone(TimeZone.getTimeZone("UTC"));
		Date eta2time=place.getETA2();
		if (eta2time!=null)
			ETA2.setText(df.format(eta2time)+"Z");
		
		if (delay!=null && eta2time!=null)
		{			
			Date planned=place.getPlanned();
			planned_field.setText(df.format(planned)+"Z");
			int diff=(int)((planned.getTime()-eta2time.getTime())/1000l);
			if (diff>0)
			{ //early
				if (diff<60)
					delay.setText(""+diff+"s early");
				else
					delay.setText(""+diff/60+"min early");
			}
			else
			{
				diff=-diff;
				if (diff<60)
					delay.setText(""+diff+"s late");
				else
					delay.setText(""+diff/60+"min late");				
			}
				
			
		}
		
		float hdg=location.getBearing();
		LatLon placepos=place.getPos();
		if (placepos!=null)
		{
			this.hdg.setText(String.format("%03.0f°", hdg));
			float brg=Project.bearing(ownlatlon,placepos);
			waypoint_hdg.setText(String.format("%03.0f°", brg));
			compass.set(hdg, brg);
		}
		else
		{
			waypoint_hdg.setText("--");
			compass.set(hdg, -1000);
		}
		
		
		d.invalidate();
	}
	private DetailedPlace place;
	@Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		place=GlobalDetailedPlace.detailedplace;
		if (place==null)
		{
			Log.i("fplan","Quitting early since place==null: "+GlobalDetailedPlace.detailedplace);
			finish();
			return;
		}
        Log.i("fplan","DetailedPlaceActivity create starting:"+place);
		//sp=(SigPoint)getIntent().getExtras().getSerializable("se.flightplanner.sigpoint");
		setContentView(R.layout.details);
		
		TextView main=(TextView)findViewById(R.id.main_text);
		main.setText("Info for "+place.getName());
		mytable=(TableLayout)findViewById(R.id.dettable);
		gs=addRow("Our GS");
		hdg=addRow("Our Heading");
		d=addRow("Distance");
		ETA2=addRow("ETA");
		waypoint_hdg=addRow("Wpt Hdg");
		
		if (place.hasPlannedTime())
		{
			planned_field=addRow("Planned");
			delay=addRow("Delay");
		}
		
		compass=addRow("Direction",new CompassRoseView(this));
		
		
		update_location(null);
        locman=(LocationManager)getSystemService(Context.LOCATION_SERVICE);
        if (locman!=null)
        	locman.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000,0, new LocationListener() {
				@Override
				public void onStatusChanged(String provider, int status, Bundle extras) {
				}
				@Override
				public void onProviderEnabled(String provider) {					
				}
				@Override
				public void onProviderDisabled(String provider) {					
				}
				@Override
				public void onLocationChanged(Location location) {
					update_location(location);					
				}
			});
        //Log.i("fplan.tmp","DetailedPlaceActivity create stopped");

	}
	
	
}
