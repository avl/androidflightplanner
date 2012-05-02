package se.flightplanner2;

import java.util.ArrayList;

import se.flightplanner2.Project.LatLon;
import se.flightplanner2.Project.Merc;
import se.flightplanner2.vector.BoundingBox;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class DescribePosition extends Activity implements LocationListener{

	static private abstract class RelDec
	{
		String name;
		public abstract String getDescr();				
	}
	private String roughdir(float brg)
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
	TextView describer;
	ListView spin;
	int sel=AdapterView.INVALID_POSITION;
	ArrayList<RelDec> items=new ArrayList<DescribePosition.RelDec>();
	void addPoint(final SigPoint sp)
	{
		final RelDec d=new RelDec()
		{
			@Override
			public String getDescr() {
				float bearing=Project.bearing(sp.latlon,mypos);
				double distance=Project.exacter_distance(mypos, sp.latlon);
				StringBuilder sb=new StringBuilder();
				sb.append("<p>");
				sb.append(String.format("%.1f miles %s of %s",distance,roughdir(bearing),name));
				sb.append("</p>");
				sb.append("(or)<br/>");
				sb.append("<p>");
				sb.append(String.format("%03.0f° %.1f miles from %s",bearing,distance,name));
				sb.append("</p>");
				return sb.toString();
			}
		};
		d.name=sp.name;		
		items.add(d);
	}
	void addLatLon(String item,final boolean decimal)
	{
		RelDec d=new RelDec()
		{
			@Override
			public String getDescr() {
				if (mypos==null)
					return "Unknown position";
				if (decimal)
				{
					return String.format("<p>WGS84 Decimal:</p>Latitude: %02.4f<br/>Longitude: %03.4f<br/>",mypos.lat,mypos.lon);
				}
				else
				{
					
					return String.format("<p>WGS84:</p><p>"+mypos.toString2().replace(" ", "<br/>")+"</p>");					
				}
			}
		};
		d.name=item;
		items.add(d);
	}
	void update(LatLon mypos)
	{
		int pos=sel;
		if (pos==AdapterView.INVALID_POSITION)
			pos=0;
		if (mypos==null)
		{
			describer.setText("Position Unknown");
			return;
		}
		RelDec rd=items.get(pos);
		describer.setText(Html.fromHtml(rd.getDescr()));		
	}
	LocationManager locman;

	LatLon mypos;
	@Override
    public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.descpos);
		mypos=(LatLon)getIntent().getSerializableExtra("se.flightplanner2.pos");
		if (mypos==null)
		{
			RookieHelper.showmsg(this.getApplicationContext(),"Current position unknown");
			finish();
			return;
		}
		describer=(TextView)findViewById(R.id.posdescr);
		spin=(ListView)findViewById(R.id.relspinner);
		addLatLon("WGS84 Deg:MM:SS",false);
		ArrayList<SigPoint> bigairfs=null;		
		ArrayList<SigPoint> smallairfs=null;		
		ArrayList<SigPoint> sigps=new ArrayList<SigPoint>();		
		ArrayList<SigPoint> bigcity=new ArrayList<SigPoint>();		
		if (GlobalLookup.lookup!=null)
		{
			Merc m=Project.latlon2merc(mypos, 13);
			bigairfs=getAirports(m,GlobalLookup.lookup.majorAirports);
			smallairfs=getAirports(m,GlobalLookup.lookup.minorAirfields);
			bigcity=getAirports(m,GlobalLookup.lookup.allCities);
			
			ArrayList<SigPoint> tmp=getAirports(m,GlobalLookup.lookup.allSigPoints);
			for(SigPoint p:tmp)
			{
				//if (p.alt==0)
					sigps.add(p);					
			}
			if (sigps.size()>0)
				addPoint(sigps.get(0));
			if (bigairfs.size()>0)
				addPoint(bigairfs.get(0));			
		}
		addLatLon("WGS84 Decimal",true);
		for(int i=0;i<10;++i)
		{
			if (i>0 && i<sigps.size())
				addPoint(sigps.get(i));
			if (i>0 && i<bigairfs.size())
				addPoint(bigairfs.get(i));
			if (i<bigcity.size())
				addPoint(bigcity.get(i));
		}
		for(int i=0;i<smallairfs.size();++i)
		{
			addPoint(smallairfs.get(i));
		}
		ArrayList<String> stritems=new ArrayList<String>();
		for(RelDec r:items)
			stritems.add(r.name);
		
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
				update(mypos);
				
			}
		});
		mypos=null;
        locman=(LocationManager)getSystemService(Context.LOCATION_SERVICE);
		locman.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000,0, this);
		
		update(null);
	}
	private ArrayList<SigPoint> getAirports(Merc m,AirspaceSigPointsTree airports) {
		ArrayList<SigPoint> bigairfs=new ArrayList<SigPoint>();
		for(int wider_net=20;wider_net<300;wider_net=(wider_net*3)/2+1)
		{

			bigairfs=airports.findall(new BoundingBox(m.toVector(),Project.approx_scale(m, 13,wider_net)));
			//Log.i("fplan","Looking around "+m.x+","+m.y+" size: "+Project.approx_scale(m, 13,wider_net)+" found: "+bigairfs.size());
			if (bigairfs.size()>=10) break;
		}
		SigPoint.sort_nearest(bigairfs,mypos);
		return bigairfs;
	}
	@Override
	public void onLocationChanged(Location location) {
		mypos=new LatLon(location);
		Log.i("fplan","DescribePosition update");
		update(mypos);
	}
	@Override
	public void onProviderDisabled(String provider) {
	}
	@Override
	public void onProviderEnabled(String provider) {
	}
	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
	}
}
