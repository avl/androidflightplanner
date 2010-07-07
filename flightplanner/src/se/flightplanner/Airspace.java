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
import se.flightplanner.vector.BoundingBox;
import se.flightplanner.vector.Polygon;
import se.flightplanner.vector.Vector;

import android.app.Activity;
import android.content.Context;

public class Airspace implements Serializable{

	ArrayList<AirspaceArea> spaces;
	ArrayList<SigPoint> points;
	
	AirspaceAreaTree areaTree;
	AirspaceSigPointsTree pointTree;
	
	ArrayList<AirspaceArea> getAirSpaces(BoundingBox box)
	{
		return areaTree.get_areas(box);
	}
	ArrayList<SigPoint> getPoints(BoundingBox box)
	{
		return pointTree.findall(box);
	}
	
	public Airspace()
	{
		spaces=new ArrayList<AirspaceArea>();
	}
	public Airspace(ArrayList<AirspaceArea> pspaces)
	{
		spaces=pspaces;
	}
	
	static Airspace download() throws Exception
	{		
		Airspace airspace=new Airspace();		
		airspace.spaces=new ArrayList<AirspaceArea>();
		JSONObject allobj = DataDownloader.post("/api/get_airspaces",null, null, new ArrayList<NameValuePair>());
		JSONArray pointsarr2=allobj.getJSONArray("points");
		for(int i=0;i<pointsarr2.length();++i)
		{
			JSONObject jsonpoint=pointsarr2.getJSONObject(i);
			SigPoint point=new SigPoint();
			double lat=jsonpoint.getDouble("lat");
			double lon=jsonpoint.getDouble("lon");
			LatLon latlon=new LatLon(lat,lon);
			Merc merc=Project.latlon2merc(latlon, 13);
			point.pos=merc;
			point.name=jsonpoint.getString("name");
			point.alt=jsonpoint.getDouble("alt");
			point.kind=jsonpoint.getString("kind").intern();
		}
		
		JSONArray spacearr=allobj.getJSONArray("airspaces");
		if (spacearr==null) throw new RuntimeException("spacearr==null");
		for(int i=0;i<spacearr.length();++i)
		{
			AirspaceArea area=new AirspaceArea();
			JSONObject areaobj=spacearr.getJSONObject(i);
			if (areaobj==null) throw new RuntimeException("areaobj==null");
			JSONArray areapointsarr=areaobj.getJSONArray("points");
			if (areapointsarr==null) throw new RuntimeException("areapointsarr==null");
			ArrayList<Vector> pointsvec=new ArrayList<Vector>();
			area.points=new ArrayList<LatLon>();
			for(int j=0;j<areapointsarr.length();++j)
			{
				JSONObject point=areapointsarr.getJSONObject(j);
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
		
		airspace.pointTree=new AirspaceSigPointsTree(airspace.points);
		airspace.areaTree=new AirspaceAreaTree(airspace.spaces);
		
		return airspace;
	}
		
	private static final long serialVersionUID = 4162260268270562095L;
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
	public ArrayList<SigPoint> getPoints() {
		return points;
	}
	
}