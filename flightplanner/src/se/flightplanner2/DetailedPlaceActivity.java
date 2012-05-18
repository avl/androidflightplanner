package se.flightplanner2;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;


import se.flightplanner2.GlobalGetElev.GetElevation;
import se.flightplanner2.GlobalPosition.PositionSubscriberIf;
import se.flightplanner2.Project.LatLon;
import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
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
	@Override
	public void onBackPressed() {
	  super.onBackPressed();
	  overridePendingTransition(0, 0);
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
	private void addPrevNext(final DetailedPlace intf)
	{
		TableRow row=new TableRow(this);		
		Button b1=new Button(this);
	
		b1.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v) {
				intf.prev();
				initialize();
			}
		});
		b1.setText("Prev");
		Button b2=new Button(this);
		b2.setText("Next");
		b2.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v) {
				intf.next();
				initialize();
			}
		});
		row.addView(b1);
		row.addView(b2);		
		mytable.addView(row);
	}
	
	//private SigPoint sp;
	private TextView ETA2;
	private TextView ETA2time;
	private TextView passed;
	private TextView d;
	private TextView gs;
	private TextView hdg;
	private TextView tod;
	private TextView delay;
	private TextView planned_fuel;
	private TextView planned_gs;
	private Location last_location;
	private TextView terr_elev;
	private TextView planned_field;
	private TextView waypoint_hdg;
	private CompassRoseView compass;
	
	private void fail()
	{
		if (ETA2!=null) ETA2.setText("--");
		if (passed!=null) passed.setText("--");
		if (ETA2time!=null) ETA2time.setText("--");
		if (d!=null) d.setText("--");
		gs.setText("--");
		hdg.setText("--");
		if (terr_elev!=null) terr_elev.setText("--");
		
		SimpleDateFormat df=new SimpleDateFormat("HH:mm",Locale.US);
		df.setTimeZone(TimeZone.getTimeZone("UTC"));
		tod.setText(df.format(new Date())+"Z");
		if (delay!=null) delay.setText("--");
		if (planned_fuel!=null) planned_fuel.setText("--");
		if (planned_gs!=null) planned_gs.setText("--");
		if (planned_field!=null) planned_field.setText("--");
		if (waypoint_hdg!=null) waypoint_hdg.setText("--");
			
		
	}
	/*
	private static String fmttime(long when) {
		if (when == 0 || when > 3600 * 24 * 10)
			return "--:--";
		return String.format("%d:%02d", when / 60, when % 60);
	}*/
	
	private BearingSpeedCalc bsca=new BearingSpeedCalc();
	private LatLon last_elev_placepos;
	private short last_elev;
	private void update_location(Location location)
	{
		if (location==null || place==null)
		{
			fail();
			return;
		}
		location=bsca.calcBearingSpeed(location);
		
		gs.setText(""+(int)(location.getSpeed()*3.6f/1.852f)+"kt");

		
		//Log.i("fplan.tmp","Update location:"+location.getLatitude()+","+location.getLongitude());
		LatLon ownlatlon=new LatLon(location);
		place.update_pos(location);
		
		double dv=place.getDistance();
		String dtxt=(dv>=0) ? String.format("%.2fNM", dv) : "--";
		if (d!=null) d.setText(dtxt);
		//Log.i("fplan.tmp","Settign distance to:"+dtxt);		
		//Date when=tripstate.getETA2();
		
		//GregorianCalendar greg=new GregorianCalendar(TimeZone.getTimeZone("UTC"),Locale.ROOT);
		SimpleDateFormat df=new SimpleDateFormat("HH:mm:ss",Locale.US);
		df.setTimeZone(TimeZone.getTimeZone("UTC"));
		
		tod.setText(df.format(new Date())+"Z");
		
		Date waspassed=place.getPassed();
		if (passed!=null)
		{
			passed.setText("--");
			if (waspassed!=null)
			{
				passed.setText(df.format(waspassed)+"Z");
			}
		}
		
		Date eta2time=place.getETA2();
		if (eta2time!=null)
		{
			if (ETA2!=null)
				ETA2.setText(df.format(eta2time)+"Z");
			int totseconds=(int)((eta2time.getTime()-new Date().getTime())/1000);
			int hours=totseconds/3600;
			totseconds-=hours*3600;
			int minutes=totseconds/60;
			int seconds=totseconds%60;
			String s;
			if (hours>=1)
				s=String.format("%dh%02dm%02ds",hours,minutes,seconds);
			else if (minutes>=1)
				s=String.format("%dm%02ds",minutes,seconds);
			else
				s=String.format("%ds",seconds);
				
			if (ETA2time!=null)
				ETA2time.setText(s);
		}
		else
		{
			if (ETA2!=null) ETA2.setText("--");
			if (ETA2time!=null) ETA2time.setText("--");
		}
		
		Date planned=place.getPlanned();
		if (planned_field!=null)
		{			
			if (planned!=null)
				planned_field.setText(df.format(planned)+"Z");
			else
				planned_field.setText("--");
		}
		
		if (delay!=null && (eta2time!=null || waspassed!=null))
		{			
			Date comptime;
			if (eta2time!=null)
				comptime=eta2time;
			else
				comptime=waspassed;
			int diff=(int)((planned.getTime()-comptime.getTime())/1000l);
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
		else
		{
			if (delay!=null) delay.setText("--");
		}
		if (planned_fuel!=null)
		{
			Float fuel=place.getPlannedFuel();
			if (fuel!=null)
				planned_fuel.setText(String.format("%.3fL",(float)fuel));
		}
		if (planned_gs!=null)
		{
			Float gs=place.getPlannedGs();
			if (gs!=null)
				planned_gs.setText(String.format("%.0fkt",(float)gs));
		}
		
		float hdg=location.getBearing();
		LatLon placepos=place.getPos();
		if (placepos!=null)
		{
			this.hdg.setText(String.format("%03.0f°", hdg));
			float brg=Project.bearing(ownlatlon,placepos);
			if (waypoint_hdg!=null) waypoint_hdg.setText(String.format("%03.0f°", brg));
			if (compass!=null)
			{
				if (dv<0.01)
					compass.set(hdg, -1000); //so close we can't really tell the direction
				else
					compass.set(hdg, brg);
			}
			
			GetElevation gelv=GlobalGetElev.get_elev;
			if (gelv!=null)
			{
				
				int elev;
				if (last_elev_placepos!=null && last_elev_placepos.equals(placepos))
					elev=last_elev;
				else
					elev=gelv.get_elev_ft(placepos,13,1);
				terr_elev.setText(""+elev+"ft");
			}
			else
			{
				terr_elev.setText("--");				
			}
			
		}
		else
		{
			if (waypoint_hdg!=null) waypoint_hdg.setText("--");
			if (compass!=null)
				compass.set(hdg, -1000);
		}		
				
	}
	//private Button prev;
	//private Button next;
	private DetailedPlace place;
	private PositionSubscriberIf subs;
	@Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		place=GlobalDetailedPlace.detailedplace;
		if (place==null)
		{
			Log.i("fplan","Quitting early since place==null: "+GlobalDetailedPlace.detailedplace);
			finish();
			overridePendingTransition(0, 0);
			return;
		}
        Log.i("fplan","DetailedPlaceActivity create starting:"+place);
		//sp=(SigPoint)getIntent().getExtras().getSerializable("se.flightplanner2.sigpoint");
		initialize();
		
		
		
		subs=new PositionSubscriberIf() {				
			@Override
			public void gps_update(Location location) {
				last_location=location;
				update_location(location);					
			}
			@Override
			public void gps_disabled() {
				fail();
			}
		};
		GlobalPosition.registerSubscriber(
				subs
			);
        //Log.i("fplan.tmp","DetailedPlaceActivity create stopped");

	}
	@Override
	protected void onDestroy()
	{
		GlobalPosition.unRegisterSubscriber(subs);
		super.onDestroy();
	}
	
	private void initialize() {
		setContentView(R.layout.details);
		
		TextView main=(TextView)findViewById(R.id.main_text);
				
		main.setText("Info for "+place.getName());		
		
		mytable=(TableLayout)findViewById(R.id.dettable);
		if (place.hasPrevNext())
		{
			addPrevNext(place);			
		}
		
		hdg=addRow("Our Heading");
		tod=addRow("UTC Time");
		gs=addRow("Our GS");
		
		if (!place.is_own_position())
		{
			waypoint_hdg=addRow("Bearing");
			d=addRow("Distance");
			ETA2=addRow("ETA");
			ETA2time=addRow("Time Remain.");
			passed=addRow("Passed");
		}
		compass=addRow("Direction",new CompassRoseView(this));
		
		
		
		if (place.hasPlanned())
		{
			planned_field=addRow("Planned");
			delay=addRow("Delay");
			planned_fuel=addRow("Planned Fuel");
			planned_gs=addRow("Planned GS");
		}
		
		terr_elev=addRow("Terrain Elev.");
		
		update_location(last_location);
	}
	
	
}
