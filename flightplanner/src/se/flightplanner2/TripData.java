package se.flightplanner2;

import java.io.DataInputStream;
import java.io.DataOutputStream;
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
import java.util.Date;

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

import se.flightplanner2.Project.LatLon;

import android.content.Context;
import android.location.Location;
import android.util.Log;

public class TripData implements Serializable {
	private static final long serialVersionUID = -6626110951719722186L;
	ArrayList<Waypoint> waypoints;
	String trip;
	String aircraft; //registration
	String atsradiotype; //type name used on radio
	
	
	/*
	static String[] get_trips(String user,String pass) throws Exception
	{		
		
		//rh.
		
		JSONObject obj = DataDownloader.post("/api/get_trips",user, pass, new ArrayList<NameValuePair>(),false);	
		Log.i("fplan","downloaded trips, about to parse:"+obj);
		
		ArrayList<String> out=new ArrayList<String>();
		JSONArray jsontrips=obj.getJSONArray("trips");
		for(int i=0;i<jsontrips.length();++i)
		{
			out.add(jsontrips.getString(i));
		}
		Log.i("fplan","parsed trips"+out);
		return out.toArray(new String[]{});
	}
	*/
	
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
	
	static public class Waypoint implements Serializable
	{
		private static final long serialVersionUID = 4282439811657511726L;
		public LatLon latlon;
		public double startalt;
		public double endalt;
		public float winddir;
		public float windvel;
		public String name;
		public String legpart;
		public String what;
		public int lastsub;
		public double gs;
		public double d;
		public double tas;
		public boolean land_at_end=false;
		public String altitude;
		public float endfuel;  //fuel at end of leg leading up to this waypoint.
		public float fuelburn; //burn on leg leading up to this waypoint.
		public long depart_dt;
		public long arrive_dt;
		public static Waypoint deserialize(DataInputStream is,int version) throws IOException
		{
			Waypoint w=new Waypoint();
			w.name=is.readUTF();
			float lat=is.readFloat();
			float lon=is.readFloat();
			w.latlon=new LatLon(lat,lon);
			w.altitude=is.readUTF();
			w.startalt=is.readFloat();
			w.endalt=is.readFloat();
			w.winddir=is.readFloat();
			w.windvel=is.readFloat();
			w.gs=is.readFloat();
			w.what=is.readUTF();
			w.legpart=is.readUTF();
			w.d=is.readFloat();
			w.tas=is.readFloat();
			w.land_at_end=is.readByte()!=0;
			w.endfuel=is.readFloat();
			w.fuelburn=is.readFloat();
			w.depart_dt=is.readLong();
			//Log.i("fplan.depart_dt","Read depart_dt: "+new Date(w.depart_dt*1000));
			w.arrive_dt=is.readLong();
			w.lastsub=is.readByte();
			return w;
		}
		public void serialize(DataOutputStream os)throws IOException {
			os.writeUTF(name);
			os.writeFloat((float)latlon.lat);
			os.writeFloat((float)latlon.lon);
			os.writeUTF(altitude);
			os.writeFloat((float)startalt);
			os.writeFloat((float)endalt);
			os.writeFloat(winddir);
			os.writeFloat(windvel);
			os.writeFloat((float)gs);
			os.writeUTF(what);
			os.writeUTF(legpart);
			os.writeFloat((float)d);
			os.writeFloat((float)tas);
			os.writeByte(land_at_end ? 1 : 0);
			os.writeFloat(endfuel);
			os.writeFloat(fuelburn);
			os.writeLong(depart_dt);
			os.writeLong(arrive_dt);
			os.writeByte(lastsub);
		}
	}
	public static TripData deserialize(DataInputStream is,int version) throws IOException
	{
		TripData d=new TripData();
		d.trip=is.readUTF();
		if (version>=8)
		{
			d.aircraft=is.readUTF();
			d.atsradiotype=is.readUTF();
		}
		else
		{
			d.aircraft="?";
			d.atsradiotype="?";
		}
		int numw=is.readInt();
		d.waypoints=new ArrayList<TripData.Waypoint>();
		for(int i=0;i<numw;++i)
		{
			if (is.readInt()!=0xbeef)
				throw new RuntimeException("Bad magic in TripData");
			d.waypoints.add(TripData.Waypoint.deserialize(is, version));
		}
		return d;
	}
	public void serialize(DataOutputStream os) throws IOException{
		os.writeUTF(trip);
		os.writeUTF(aircraft);
		os.writeUTF(atsradiotype);
		
		os.writeInt(waypoints.size());
		for(Waypoint w:waypoints)
		{
			os.writeInt(0xbeef);
			w.serialize(os);
		}		
	}
	/*
	static TripData get_trip(String user,String pass,String trip) throws Exception
	{
		TripData td=new TripData();
		{
			ArrayList<NameValuePair> nvps=new ArrayList<NameValuePair>();
			nvps.add(new BasicNameValuePair("trip",trip));
			JSONObject obj = DataDownloader.post("/api/get_trip",user, pass, nvps,false);	
			if (!obj.has("waypoints"))
				throw new RuntimeException("Fetched obj has no waypoints. Obj: "+obj.toString());
			JSONArray jsonwps=obj.getJSONArray("waypoints");
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
				wp.startalt=wpobj.getDouble("startalt");
				wp.endalt=wpobj.getDouble("endalt");
				wp.name=wpobj.getString("name");
				wp.legpart=wpobj.getString("legpart");
				wp.what=wpobj.getString("what");
				wp.lastsub=wpobj.getInt("lastsub");
				wp.gs=wpobj.getDouble("gs");
				wp.tas=wpobj.getDouble("tas");
				wp.d=wpobj.getDouble("d");
				wp.land_at_end=wpobj.optBoolean("land_at_end",false);
				td.waypoints.add(wp);
			}
		}
		{
			
		}
		return td;
	}
	*/

	
}
