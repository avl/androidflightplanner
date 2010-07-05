package se.flightplanner;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import se.flightplanner.Project.LatLon;

import android.content.Context;
import android.location.Location;
import android.util.Log;

public class TripData implements Serializable {
	private static final long serialVersionUID = -6626110951719722186L;
	static String[] get_trips(String user,String pass) throws Exception
	{		
		
		//rh.
		
		JSONObject obj = DataDownloader.post("/api/get_trips",user, pass, new ArrayList<NameValuePair>());	
		
		ArrayList<String> out=new ArrayList<String>();
		JSONArray jsontrips=obj.getJSONArray("trips");
		for(int i=0;i<jsontrips.length();++i)
		{
			out.add(jsontrips.getString(i));
		}		
		return out.toArray(new String[]{});
	}
	
	void serialize_to_file(Context context,String filename) throws Exception
	{
		OutputStream ofstream=context.openFileOutput(filename,Context.MODE_PRIVATE);
		try
		{
			ObjectOutputStream os=new ObjectOutputStream(ofstream);
			os.writeObject(this);
			os.close();
			ofstream.close();
		}
		finally
		{
			ofstream.close();
		}		
	}
	static TripData deserialize_from_file(Context context,String filename) throws Exception
	{
		InputStream ofstream=context.openFileInput(filename);
		TripData data=null;
		try
		{
			ObjectInputStream os=new ObjectInputStream(ofstream);
			data=(TripData)os.readObject();
			os.close();		
		}
		finally
		{
			ofstream.close();			
		}
		return data;
	}
	
	String trip;
	static public class Waypoint implements Serializable
	{
		private static final long serialVersionUID = 4282439811657511726L;
		public LatLon latlon;
		public double altitude;
		public String name;
		public String legpart;
		public String what;
		public int lastsub;
	}
	ArrayList<Waypoint> waypoints;
	static TripData get_trip(String user,String pass,String trip) throws Exception
	{		
		ArrayList<NameValuePair> nvps=new ArrayList<NameValuePair>();
		nvps.add(new BasicNameValuePair("trip",trip));
		JSONObject obj = DataDownloader.post("/api/get_trip",user, pass, nvps);	
		if (!obj.has("waypoints"))
			throw new RuntimeException("Fetched obj has no waypoints. Obj: "+obj.toString());
		JSONArray jsonwps=obj.getJSONArray("waypoints");
		TripData td=new TripData();
		td.trip=trip;
		td.waypoints=new ArrayList<Waypoint>();
		for(int i=0;i<jsonwps.length();++i)
		{
			if (jsonwps.isNull(i))
				throw new RuntimeException("There is no waypoint with number "+i);
			JSONObject wpobj=jsonwps.getJSONObject(i);
			
			Waypoint wp=new Waypoint();
			wp.latlon=new LatLon(wpobj.getDouble("lat"),
					wpobj.getDouble("lon"));
			wp.altitude=wpobj.getDouble("altitude");
			wp.name=wpobj.getString("name");
			wp.legpart=wpobj.getString("legpart");
			wp.what=wpobj.getString("what");
			wp.lastsub=wpobj.getInt("lastsub");
			td.waypoints.add(wp);
		}		
		return td;
	}
	
}
