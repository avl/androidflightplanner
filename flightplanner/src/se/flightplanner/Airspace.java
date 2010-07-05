package se.flightplanner;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;

import org.apache.http.NameValuePair;
import org.json.JSONArray;
import org.json.JSONObject;

import se.flightplanner.Project.LatLon;
import se.flightplanner.Project.Merc;
import se.flightplanner.vector.Polygon;
import se.flightplanner.vector.Vector;

import android.app.Activity;
import android.content.Context;

public class Airspace implements Serializable{

	ArrayList<AirspaceArea> spaces;
	
	
	
	static Airspace download() throws Exception
	{		
		Airspace airspace=new Airspace();		
		airspace.spaces=new ArrayList<AirspaceArea>();
		JSONObject allobj = DataDownloader.post("/api/get_airspaces",null, null, new ArrayList<NameValuePair>());
		JSONArray spacearr=allobj.getJSONArray("airspaces");
		if (spacearr==null) throw new RuntimeException("spacearr==null");
		for(int i=0;i<spacearr.length();++i)
		{
			AirspaceArea area=new AirspaceArea();
			JSONObject areaobj=spacearr.getJSONObject(i);
			if (areaobj==null) throw new RuntimeException("areaobj==null");
			JSONArray pointsarr=areaobj.getJSONArray("points");
			if (pointsarr==null) throw new RuntimeException("pointsarr==null");
			ArrayList<Vector> pointsvec=new ArrayList<Vector>();
			area.points=new ArrayList<LatLon>();
			for(int j=0;j<pointsarr.length();++j)
			{
				JSONObject point=pointsarr.getJSONObject(j);
				if (point==null) throw new RuntimeException("point==null");
				double lat=point.getDouble("lat");
				double lon=point.getDouble("lon");
				LatLon latlon=new LatLon(lat,lon);
				area.points.add(latlon);
				Merc merc=Project.latlon2merc(latlon, 13);
				pointsvec.add(new Vector(merc.x,merc.y));				
			}
			area.poly=new Polygon(pointsvec);
			area.freqs=new ArrayList<String>();
			if (areaobj.has("freqs"))
			{
				JSONArray freqsarr=areaobj.getJSONArray("freqs");
				for(int k=0;k<freqsarr.length();++k)
				{
					JSONArray tuple=freqsarr.getJSONArray(k);
					if (tuple==null) throw new RuntimeException("Missing freq tuple");
					area.freqs.add(String.format("%s: %.3f", tuple.getString(0),tuple.getDouble(1)));
				}
			}
			if (!areaobj.has("name")) throw new RuntimeException("Missing name field");
			if (!areaobj.has("floor")) throw new RuntimeException("Missing floor field");
			if (!areaobj.has("ceiling")) throw new RuntimeException("Missing ceiling field");
			area.name=areaobj.getString("name");
			area.floor=areaobj.getString("floor");
			area.ceiling=areaobj.getString("ceiling");
			airspace.spaces.add(area);
		}
		
		return airspace;
	}
		
	private static final long serialVersionUID = 4162260268270562095L;
	public static class AirspaceArea implements Serializable 
	{
		private static final long serialVersionUID = -4964236460301544582L;
		Polygon poly;
		String name;
		ArrayList<LatLon> points;
		ArrayList<String> freqs;
		String floor;
		String ceiling;
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
	static Airspace deserialize_from_file(Context context,String filename) throws Exception
	{
		InputStream ofstream=context.openFileInput(filename);
		Airspace data=null;
		try
		{
			ObjectInputStream os=new ObjectInputStream(ofstream);
			data=(Airspace)os.readObject();
			os.close();		
		}
		finally
		{
			ofstream.close();			
		}
		return data;
	}
	public ArrayList<AirspaceArea> getSpaces() {
		return spaces;
	}
	
}
