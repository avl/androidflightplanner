package se.flightplanner;

import java.io.InputStream;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.StringBufferInputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONObject;

import se.flightplanner.Project.LatLon;
import se.flightplanner.Project.Merc;
import se.flightplanner.SigPoint.Chart;
import se.flightplanner.vector.BoundingBox;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.os.Environment;
import android.util.Log;

public class Airspace implements Serializable{

	public ArrayList<AirspaceArea> spaces;
	public ArrayList<SigPoint> points;

	public static Airspace deserialize(DataInputStream is) throws IOException {
		Airspace a=new Airspace();
		
		int magic=is.readInt();
		int version=is.readInt();
		if (magic!=0x8A31CDA)
			throw new RuntimeException("Couldn't load airspace data, bad header magic. Was: "+magic+" should be: "+0x8A31CDA);
		if (version!=1 && version!=2 && version!=3 && version!=4)
			throw new RuntimeException("Couldn't load airspace data, bad version");
		
		int numspaces=is.readInt();
		if (numspaces>1000)
			throw new RuntimeException("Too many airspace definitions: "+numspaces);
		a.spaces=new ArrayList<AirspaceArea>();
		for(int i=0;i<numspaces;++i)
			a.spaces.add(AirspaceArea.deserialize(is,version));

		int numpoints=is.readInt();
		if (numpoints>10000)
			throw new RuntimeException("Too many points: "+numpoints);
		a.points=new ArrayList<SigPoint>();
		for(int i=0;i<numpoints;++i)
			a.points.add(SigPoint.deserialize(is,version));
		
		return a;
	}
	public void serialize(DataOutputStream os) throws IOException {
		
		int numspaces=spaces.size();
		os.writeInt(0x8A31CDA);
		os.writeInt(4); //version 4
		os.writeInt(numspaces);
		for(int i=0;i<numspaces;++i)
			spaces.get(i).serialize(os);
		int numpoints=points.size();
		os.writeInt(numpoints);
		for(int i=0;i<numpoints;++i)
			points.get(i).serialize(os);
	}
	/*
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
	*/
	
	public Airspace()
	{
		spaces=new ArrayList<AirspaceArea>();
	}
	public Airspace(ArrayList<AirspaceArea> pspaces)
	{
		spaces=pspaces;
	}
	
	public static Airspace download() throws Exception
	{
		return download(null);
	}
	public static Airspace download(InputStream fakeDataForTest) throws Exception
	{		
		System.out.println("Start download operation");
		InputStream inp;
		if (fakeDataForTest!=null)
		{			
			inp=fakeDataForTest;
		}
		else
		{
			ArrayList<NameValuePair> nvps=new ArrayList<NameValuePair>();
			nvps.add(new BasicNameValuePair("version","4"));			
			inp=DataDownloader.postRaw("/api/get_airspaces",null, null, nvps,false);
		}
		Airspace airspace=deserialize(new DataInputStream(inp));
		inp.close();
		System.out.println("Finish download operation");
		return airspace;
		
		/* Old style:
		JSONObject allobj;
			allobj = DataDownloader.post("/api/get_airspaces",null, null, new ArrayList<NameValuePair>(),false);
		JSONArray pointsarr2=allobj.getJSONArray("points");
		System.out.println("Finished post operation, start parse");
		airspace.points=new ArrayList<SigPoint>(pointsarr2.length());
		for(int i=0;i<pointsarr2.length();++i)
		{
			JSONObject jsonpoint=pointsarr2.getJSONObject(i);
			SigPoint point=new SigPoint();
			double lat=jsonpoint.getDouble("lat");
			double lon=jsonpoint.getDouble("lon");
			LatLon latlon=new LatLon(lat,lon);
			point.latlon=new LatLon(lat,lon);
			point.calcMerc();
			point.name=jsonpoint.getString("name");
			point.alt=jsonpoint.getDouble("alt");
			point.kind=jsonpoint.getString("kind").intern();
			airspace.points.add(point);
		}
		
		JSONArray spacearr=allobj.getJSONArray("airspaces");
		airspace.spaces=new ArrayList<AirspaceArea>(spacearr.length());
		if (spacearr==null) throw new RuntimeException("spacearr==null");
		for(int i=0;i<spacearr.length();++i)
		{
			JSONObject areaobj=spacearr.getJSONObject(i);
			if (areaobj==null) throw new RuntimeException("areaobj==null");
			AirspaceArea area=new AirspaceArea();
			
			area.name=areaobj.getString("name");
			area.floor=areaobj.getString("floor");
			area.ceiling=areaobj.getString("ceiling");
			
			JSONArray areapointsarr=areaobj.getJSONArray("points");
			if (areapointsarr==null) throw new RuntimeException("areapointsarr==null");
			area.points=new ArrayList<LatLon>();
			for(int j=0;j<areapointsarr.length();++j)
			{
				JSONObject point=areapointsarr.getJSONObject(j);
				if (point==null) throw new RuntimeException("point==null");
				double lat=point.getDouble("lat");
				double lon=point.getDouble("lon");
				LatLon latlon=new LatLon(lat,lon);
				area.points.add(latlon);
			}
			area.initPoly(area.points);
			area.freqs=new ArrayList<String>();
			if (area.name.contains("BROMMA"))
				Log.i("fplan","Parsing freqs");
			if (areaobj.has("freqs"))
			{
				JSONArray freqsarr=areaobj.getJSONArray("freqs");
				for(int k=0;k<freqsarr.length();++k)
				{
					JSONArray tuple=freqsarr.getJSONArray(k);
					if (tuple==null) throw new RuntimeException("Missing freq tuple");
					area.freqs.add(String.format("%s: %.3f", tuple.getString(0),tuple.getDouble(1)));
					if (area.name.contains("BROMMA"))
					{
						Log.i("fplan","Adding "+String.format("%s: %.3f", tuple.getString(0),tuple.getDouble(1)));
					}
				}
			}	
			if (area.name.contains("BROMMA"))
				Log.i("fplan","Parsing freq - result"+area.freqs);
			if (!areaobj.has("name")) throw new RuntimeException("Missing name field");
			if (!areaobj.has("floor")) throw new RuntimeException("Missing floor field");
			if (!areaobj.has("ceiling")) throw new RuntimeException("Missing ceiling field");
			airspace.spaces.add(area);
		}
		*/
		
		/*
		airspace.pointTree=new AirspaceSigPointsTree(airspace.points);
		airspace.areaTree=new AirspaceAreaTree(airspace.spaces);
		
		
		System.out.println("Finish download operation");
		return airspace;
		 */
	}

	private static final long serialVersionUID = 4162260268270562095L;
	void serialize_to_file(String filename) throws Exception
	{
		File extpath = Environment.getExternalStorageDirectory();
		File dirpath = new File(extpath,
				"/Android/data/se.flightplanner/files/");
		if (!dirpath.exists())
			dirpath.mkdirs();
		File path = new File(dirpath,filename);
		if (path.exists())
			path.delete();
		OutputStream ofstream=new BufferedOutputStream(
				new FileOutputStream(path)
				);
		
		try
		{
			DataOutputStream os=new DataOutputStream(ofstream);
			serialize(os);
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
		File extpath = Environment.getExternalStorageDirectory();
		File path = new File(extpath,
				"/Android/data/se.flightplanner/files/"+filename);
		InputStream ofstream=new BufferedInputStream(
				new FileInputStream(path)
				);

		Airspace data=null;
		try
		{
			
			DataInputStream os=new DataInputStream(ofstream);
			data=Airspace.deserialize(os);//(Airspace)os.readObject();
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
	static private class Pair
	{
		String airport;
		String chart;
		float dist;
	}
	public void getAdChartNames(ArrayList<String> chartName,ArrayList<String> airportName, LatLon location) {
		ArrayList<Pair> tmp=new ArrayList<Airspace.Pair>();
		ArrayList<Pair> closest=new ArrayList<Pair>();
		Comparator<Pair> comp=new Comparator<Pair>(){
			@Override
			public int compare(Pair o1, Pair o2) {
				if (o1.dist<o2.dist) return -1;
				if (o1.dist>o2.dist) return +1;
				return 0;
			}					
		};
		for(SigPoint p:points)
		{
			if (p.chart!=null)
			{
				Pair pair=new Pair();
				pair.airport=p.name;
				pair.chart=p.chart.name;
				if (location!=null)
				{
					pair.dist=(float) Project.exacter_distance(location, 
							Project.merc2latlon(p.pos, 13));
					if (closest.size()<2 || pair.dist<closest.get(1).dist)
						closest.add(pair);
					Collections.sort(closest, comp);
					for(int i=closest.size()-1;i>=2;--i)
						closest.remove(i);
				}
				tmp.add(pair);
				
			}
		}
		Collections.sort(tmp, new Comparator<Pair>() {
			@Override
			public int compare(Pair object1, Pair object2) {
				return object1.airport.compareTo(object2.airport);
			}
		});
		if (location!=null && closest.size()>0)
		{
			for(Pair pair:closest)
			{
				chartName.add(pair.chart);
				airportName.add(pair.airport);
			}
			chartName.add(null);
			airportName.add("----");
		}
		for(Pair pair:tmp)
		{
			chartName.add(pair.chart);
			airportName.add(pair.airport);
		}

	}
	public Chart getChart(String name) {
		for(SigPoint p:points)
		{
			if (p.chart!=null && p.chart.name==name)
				return p.chart;
		}
		return null;
	}
	
}
