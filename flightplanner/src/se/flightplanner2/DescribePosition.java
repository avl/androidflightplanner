package se.flightplanner2;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import se.flightplanner2.GlobalPosition.PositionSubscriberIf;
import se.flightplanner2.Project.LatLon;
import se.flightplanner2.Project.Merc;
import se.flightplanner2.TripState.NextSigPoints;
import se.flightplanner2.descpos.LatLonRelDec;
import se.flightplanner2.descpos.NextSigPointReldec;
import se.flightplanner2.descpos.RelDec;
import se.flightplanner2.descpos.SigPointReldec;
import se.flightplanner2.vector.BoundingBox;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

public class DescribePosition extends Activity implements PositionSubscriberIf{

	protected static final int CHOOSE_POSITION = 1;
	public static String roughdir(float brg)
	{
		if (brg<11.25+22.5*0) return "north";
		if (brg<11.25+22.5*1) return "north-north-east";
		if (brg<11.25+22.5*2) return "north-east";
		if (brg<11.25+22.5*3) return "east-north-east";
		
		if (brg<11.25+22.5*4) return "east";
		if (brg<11.25+22.5*5) return "east-south-east";
		if (brg<11.25+22.5*6) return "south-east";
		if (brg<11.25+22.5*7) return "south-south-east";
		
		if (brg<11.25+22.5*8) return "south";
		if (brg<11.25+22.5*9) return "south-south-west";
		if (brg<11.25+22.5*10) return "south-west";
		if (brg<11.25+22.5*11) return "west-south-west";
		
		if (brg<11.25+22.5*12) return "west";
		if (brg<11.25+22.5*13) return "west-north-west";
		if (brg<11.25+22.5*14) return "north-west";
		if (brg<11.25+22.5*15) return "north-north-west";
		return "north";
	}
	public static String roughdirshort(float brg)
	{
		if (brg<11.25+22.5*0) return "N";
		if (brg<11.25+22.5*1) return "NNE";
		if (brg<11.25+22.5*2) return "NE";
		if (brg<11.25+22.5*3) return "ENE";
		
		if (brg<11.25+22.5*4) return "E";
		if (brg<11.25+22.5*5) return "ESE";
		if (brg<11.25+22.5*6) return "SE";
		if (brg<11.25+22.5*7) return "SSE";
		
		if (brg<11.25+22.5*8) return "S";
		if (brg<11.25+22.5*9) return "SSW";
		if (brg<11.25+22.5*10) return "SW";
		if (brg<11.25+22.5*11) return "WSW";
		
		if (brg<11.25+22.5*12) return "W";
		if (brg<11.25+22.5*13) return "WNW";
		if (brg<11.25+22.5*14) return "NW";
		if (brg<11.25+22.5*15) return "NNW";
		return "N";
	}
	public static String aviation_format_time(Date when) {
		Date now=new Date();
		long lnow=now.getTime();
		long lwhen=when.getTime();
		long delta=lwhen-lnow;
		long minute=60*1000l;
		
		if (Math.abs(delta)>20*minute)
		{
			if (Math.abs(delta)>10*60*minute)
			{
				SimpleDateFormat df=new SimpleDateFormat("MMM-dd HH:mm",Locale.US);
				df.setTimeZone(TimeZone.getTimeZone("UTC"));
				return df.format(when);				
			}
			else
			{
				SimpleDateFormat df=new SimpleDateFormat("HH:mm",Locale.US);
				df.setTimeZone(TimeZone.getTimeZone("UTC"));
				return df.format(when);
			}			
		}
		else
		{
			SimpleDateFormat df=new SimpleDateFormat("mm",Locale.US);
			df.setTimeZone(TimeZone.getTimeZone("UTC"));
			return df.format(when);
		}
	}
	TextView describer;
	ListView spin;
	int sel=AdapterView.INVALID_POSITION;
	ArrayList<RelDec> items=new ArrayList<RelDec>();
	
	void addEnsp(final NextSigPoints ensp) {
		final RelDec d=new NextSigPointReldec(ensp);
			
		items.add(d);
		
	}
		
	void addPoint(final SigPoint sp)
	{
		final RelDec d=new SigPointReldec(sp);
		items.add(d);
	}
	void addLatLon(String item,final boolean decimal)
	{
		RelDec d=new LatLonRelDec(decimal);
		items.add(d);
	}
	void update(LatLon mypos)
	{
		
		int pos=sel;
		if (pos==AdapterView.INVALID_POSITION)
			pos=0;
		if (mypos==null || pos<0 || pos>=items.size())
		{
			describer.setText("--");
			return;
		}
		RelDec rd=items.get(pos);
		describer.setText(Html.fromHtml(rd.getDescr(false,false)));		
	}
	private String filter=null;

	private boolean selectpos;
	@Override
    public void onCreate(Bundle savedInstanceState) 
	{		
		super.onCreate(savedInstanceState);
		selectpos=getIntent().getBooleanExtra("se.flightplanner2.selectpos", false);
		setContentView(R.layout.descpos);
		Button selbutton=(Button)findViewById(R.id.selectbuttonend);
		if (!selectpos)
			selbutton.setVisibility(View.GONE);
		else
			selbutton.setOnClickListener(new OnClickListener()
			{
				@Override
				public void onClick(View v) {
					Intent intent = DescribePosition.this.getIntent();

					int pos=sel;
					if (pos==AdapterView.INVALID_POSITION)
						pos=0;
					if (pos>=0 || pos<items.size())
					{
						RelDec rd=items.get(pos);
						intent.putExtra("se.flightplanner2.reldec",rd);
					}
					
					DescribePosition.this.setResult(RESULT_OK, intent);
					DescribePosition.this.finish();
					DescribePosition.this.overridePendingTransition(0, 0);	    	
					
				}			
			});
	
		describer=(TextView)findViewById(R.id.posdescr);
		spin=(ListView)findViewById(R.id.relspinner);
		
		addLatLon("WGS84 Deg:MM:SS",false);
		
		Button sigp=(Button)findViewById(R.id.dp_sigp);
		Button airp=(Button)findViewById(R.id.dp_airport);
		Button town=(Button)findViewById(R.id.dp_town);
		Button route=(Button)findViewById(R.id.dp_route);
		Button all=(Button)findViewById(R.id.dp_all);
				
		final Button[] bts=new Button[]{
				all,sigp,airp,town,route
		};
		for(Button b:bts)
			b.setBackgroundColor(Color.GRAY);
		
		bts[0].setBackgroundColor(Color.GREEN);
		filter="Points";
		for(final Button but:bts)
		{
			but.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View v) {
					for(Button b:bts)
						b.setBackgroundColor(Color.GRAY);
					but.setBackgroundColor(Color.GREEN);
					filter=but.getText().toString();
					Log.i("fplan.dp","Filtering on "+filter);
					complete_update();
				}
			});
		}
		
		
		complete_update();
		GlobalPosition.registerSubscriber(this);
		
		update(null);
		
	}
	@Override
	protected void onDestroy()
	{
		GlobalPosition.unRegisterSubscriber(this);
		super.onDestroy();
	}
	private void complete_update() {
		LatLon mypos=GlobalPosition.getLastLatLonPosition();
		if (mypos==null)
		{
			finish();
			overridePendingTransition(0, 0);			
			return;
		}
		
		items.clear();
		ArrayList<SigPoint> all=new ArrayList<SigPoint>();
		AirspaceLookup lookup=GlobalLookup.lookup;
		TripState tst=GlobalTripState.tripstate;
		if (filter.equals("Route"))
		{
			if (tst!=null)
			{
				for(NextSigPoints ensp:tst.get_remaining_ensps())
				{
					addEnsp(ensp);
				}
			}
		}
		else
		{
			if (lookup!=null)
			{
				Merc m=Project.latlon2merc(mypos, 13);
				if (filter.equals("All") || filter.equals("Points"))
					all.addAll(getAirports(m,lookup.allSigPoints));
				if (filter.equals("All") || filter.equals("Airports"))
					all.addAll(getAirports(m,lookup.majorAirports));
				if (filter.equals("All") || filter.equals("Towns"))
					all.addAll(getAirports(m,lookup.allCities));
							
				SigPoint.sort_nearest(all,mypos);
						
			}
		}
		//addLatLon("WGS84 Decimal",true);
		for(SigPoint sp:all)
		{
			addPoint(sp);
		}
		ArrayList<String> stritems=new ArrayList<String>();
		for(RelDec r:items)
			stritems.add(r.getName());
		
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_selectable_list_item,stritems);
	
		//adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spin.setAdapter(adapter);		
		//spin.setSelection(0);
		spin.setOnItemClickListener(new OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1,
					int index, long arg3) {
				Log.i("fplan","OnClick does run");
				sel=index;
				LatLon mypos=GlobalPosition.getLastLatLonPosition();
				update(mypos);
				
			}
		});
		update(mypos);
	}
	private ArrayList<SigPoint> getAirports(Merc m,AirspaceSigPointsTree airports) {
		ArrayList<SigPoint> founditems=new ArrayList<SigPoint>();
		for(int wider_net=20;wider_net<300;wider_net=(wider_net*3)/2+1)
		{

			founditems=airports.findall(new BoundingBox(m.toVector(),Project.approx_scale(m, 13,wider_net)));
			//Log.i("fplan","Looking around "+m.x+","+m.y+" size: "+Project.approx_scale(m, 13,wider_net)+" found: "+bigairfs.size());
			if (founditems.size()>=30) break;
		}
		return founditems;
	}
	@Override
	public void gps_update(Location location) {
		LatLon mypos=new LatLon(location);
		Log.i("fplan","DescribePosition update");
		update(mypos);
	}
	@Override
	public void gps_disabled() {
		update(null);	
	}
}

