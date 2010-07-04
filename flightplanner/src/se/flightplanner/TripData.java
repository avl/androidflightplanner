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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
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
		
		JSONObject obj = post("/api/get_trips",user, pass, new ArrayList<NameValuePair>());	
		
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
	
	private static JSONObject post(String path,String user, String pass,
			ArrayList<NameValuePair> nvps) throws Exception {
		
		byte[] md5pass=MessageDigest.getInstance("MD5").
			digest(pass.getBytes());
		StringBuilder hd=new StringBuilder();
		for(int i=0;i<md5pass.length;++i)
		{
			byte b=md5pass[i];
			hd.append(Character.forDigit((b>>4)&15,16));
			hd.append(Character.forDigit(b&15,16));
		}
		nvps.add(new BasicNameValuePair("user",user));
		nvps.add(new BasicNameValuePair("password",hd.toString()));
		HttpPost req=new HttpPost("http://10.0.2.2:5000"+path);
		UrlEncodedFormEntity postparams=new UrlEncodedFormEntity(nvps,"UTF8");
		req.setEntity(postparams);

		DefaultHttpClient cli=new DefaultHttpClient();
		BasicResponseHandler rh=new BasicResponseHandler();
		String str=cli.execute(req,rh);
		JSONObject obj=new JSONObject(str);
		if (obj.has("error"))
		{
			throw new RuntimeException("Error:"+obj.getString("error"));
		}
		return obj;
		/*
		JSONObject obj;
		UrlEncodedFormEntity postparams=new UrlEncodedFormEntity(nvps,"UTF8");
		req.setEntity(postparams);
		
		HttpResponse resp = cli.execute(req);
		Log.i("get",resp.getStatusLine().toString());
		HttpEntity ent=resp.getEntity();
		if (ent==null)
		{
			throw new RuntimeException("No request body");
		}
		InputStream str=ent.getContent();
		InputStreamReader rd=new InputStreamReader(str,"UTF-8");
		char[] buf=new char[1024];
		for(;;)
		{
			int r=rd.read(buf,0,1024);
			if (r>0)
				bd.append(buf,0,r);
			if (r<=0)
				break;
		}
		Log.i("ent",bd.toString());
		
		obj=new JSONObject(bd.toString());
		if (obj.has("error"))
		{
			throw new RuntimeException("Error:"+obj.getString("error"));
		}
		return obj;
		*/
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
		JSONObject obj = post("/api/get_trip",user, pass, nvps);	
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
