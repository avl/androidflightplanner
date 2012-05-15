package se.flightplanner2;

import java.io.InputStream;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.StringBufferInputStream;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONObject;

import se.flightplanner2.Project.LatLon;
import se.flightplanner2.Project.Merc;
import se.flightplanner2.vector.BoundingBox;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.os.Environment;
import android.util.Log;

public class Airspace implements Serializable{
	static public interface AirspaceProgress
	{
		public void report(int percent); 
	}
	public String aipgen;
	public ArrayList<AirspaceArea> spaces;	
	public ArrayList<SigPoint> points;
	public String namedigest;
	public ArrayList<AipText> aiptexts;
	public ArrayList<TripData> trips;
	
	public static String toHex(byte[] bytes) {
	    BigInteger bi = new BigInteger(1, bytes);
	    return String.format("%0" + (bytes.length << 1) + "X", bi);
	}
	static public int compare(byte[] left, byte[] right) {
        for (int i = 0, j = 0; i < left.length && j < right.length; i++, j++) {
            int a = (left[i] & 0xff);
            int b = (right[j] & 0xff);
            if (a != b) {
                return a - b;
            }
        }
        return left.length - right.length;
    }
	
	static int comp(String a,String b)
	{
		
		byte[] a1=a.getBytes();
		byte[] b1=b.getBytes();
		return compare(a1,b1);
	}

	@SuppressWarnings("unchecked")
	public static Airspace deserialize(DataInputStream is,Airspace previous,AirspaceProgress prog) throws IOException {
		Airspace a=new Airspace();
		
		
		if (previous!=null)
			Log.i("fplan","Previous: "+previous.spaces.size());
		int magic=is.readInt();
		if (magic!=0x8A31CDA)
			throw new RuntimeException("Couldn't load airspace data, bad header magic. Was: "+magic+" should be: "+0x8A31CDA);
		int version=is.readInt();
		int corrpass=is.readByte();
		Log.i("fplan","Reading corrpass: "+corrpass);
		if (corrpass!=1)
			throw new RuntimeException("Wrong password");
		if (version<1 || version>8)
			throw new RuntimeException("Couldn't load airspace data, bad version");
		if (version>=5)
		{
			a.aipgen=is.readUTF();
			Log.i("fplan","Read aipgen: "+a.aipgen);
			if (is.readByte()==1 || previous==null)
			{
				Log.i("fplan","Loading from scratch");
				previous=null;
			}
			else
			{
				Log.i("fplan","Not loading from scratch");
			}
		}
		else
		{
			previous=null;
			a.aipgen="";
		}
		//ArrayList<Integer> spacekills=new ArrayList<Integer>();		
		a.spaces=read_sub(AirspaceArea.class,is, prog, 0,15,version, "spaces",
				previous!=null ? (ArrayList<AirspaceArea>)previous.spaces.clone() : null);

		//a.points=new ArrayList<SigPoint>();
		//ArrayList<Integer> pointkills=new ArrayList<Integer>();
		a.points=read_sub(SigPoint.class,is,prog,15,30,version,"points",
				previous!=null ? (ArrayList<SigPoint>)previous.points.clone() : null);

		a.aiptexts=read_sub(AipText.class,is,prog,30,80,version,"aiptext",
				previous!=null ? (ArrayList<AipText>)previous.aiptexts.clone() : null);
		
		a.trips=read_sub(TripData.class,is,prog,80,95,version,"aiptext",
				previous!=null ? (ArrayList<TripData>)previous.trips.clone() : null);
		
		//a.aiptexts=read_sub(AipText.class,is,prog,66,99,version,numpoints,pointkills);
		
		/*
		for(int i=0;i<numpoints;++i)
		{
			if (prog!=null && (i%10==0 || i==numpoints-1))
				prog.report(50+(50*i)/numpoints);
			if (version>=5)
			{
				if (is.readByte()==0)
				{ //"kill"
					int idx=is.readInt();
					Log.i("fplan","Kill point with idx: "+idx);
					pointkills.add(idx);
					continue;
				}
			}
			a.points.add(SigPoint.deserialize(is,version));
		}
		*/
		
		
		/*if (previous!=null)
		{
			//Log.i("fplan","Previous A: "+previous.spaces.size());
			Collections.sort(pointkills);
			Collections.sort(spacekills);
			ArrayList<SigPoint> prevpoints=(ArrayList<SigPoint>)(previous.points.clone());
			ArrayList<AirspaceArea> prevspaces=(ArrayList<AirspaceArea>)(previous.spaces.clone());
			{
				int src=0;
				int trg=0;
				for(int i=0;src<prevpoints.size();)
				{
					int pk;
					if (i<pointkills.size()) pk=pointkills.get(i);
					else pk=-1;
					if (pk!=src)
					{
						//Log.i("fplan","Copying element "+src+" -> "+trg);
						if (src!=trg)
							prevpoints.set(trg,prevpoints.get(src));
						++src;
						++trg;
					}
					else
					{//pk==src
						//Log.i("fplan","Implementing points kill of idx: "+src);						
						//Log.i("fplan","Not copying element "+src+" -> anywhere");
						++i;
						++src;
					}						
				}
				int newsize=prevpoints.size()-pointkills.size();
				for(int i=prevpoints.size()-1;i>=newsize;--i)
				{
					//Log.i("fplan","Removing element "+i);
					prevpoints.remove(i);
				}
			}
			{
				int src=0;
				int trg=0;
				for(int i=0;src<prevspaces.size();)
				{
					int pk;
					if (i<spacekills.size()) pk=spacekills.get(i);
					else pk=-1;
					if (pk!=src)
					{
						//Log.i("fplan","Copying element "+src+" -> "+trg);
						if (src!=trg)
							prevspaces.set(trg,prevspaces.get(src));
						++src;
						++trg;
					}
					else
					{//pk==src
						//Log.i("fplan","Implementing spaces kill of idx: "+src);						
						//Log.i("fplan","Not copying element "+src+" -> anywhere");
						++i;
						++src;
					}						
				}
				int newsize=prevspaces.size()-spacekills.size();
				for(int i=prevspaces.size()-1;i>=newsize;--i)
				{
					//Log.i("fplan","Removing element "+i);
					prevspaces.remove(i);
				}
			}
			
			prevspaces.addAll(a.spaces);
			prevpoints.addAll(a.points);
			a.spaces=prevspaces;
			a.points=prevpoints;
			//previous.spaces=null;
			//previous.points=null;
		
		}
		*/
		a.namedigest=null;
		if (version>=5)
		{
			//Log.i("fplan","Checksumming all points and spaces");
			int magic2=is.readInt();
			
			if (magic2!=0x1eedbaa5)
			{
				Log.i("fplan","Magic: "+magic2+" correct:"+(magic2==0x1eedbaa5));
				throw new RuntimeException("Bad magic in downloaded data");
			}
			
			
			String checksum=is.readUTF();
			MessageDigest md=null;
			try {
				md = MessageDigest.getInstance("MD5");
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			}
			if (md!=null)
			{
				Collections.sort(a.spaces,new Comparator<AirspaceArea>(){
					@Override
					public int compare(AirspaceArea object1,
							AirspaceArea object2) {
						
						return comp(object1.name,object2.name);
					}
				});
				Collections.sort(a.points,new Comparator<SigPoint>(){
					@Override
					public int compare(SigPoint object1,
							SigPoint object2) {
						return comp(object1.name,object2.name);
					}
				});
				Collections.sort(a.aiptexts,new Comparator<AipText>(){
					@Override
					public int compare(AipText object1,
							AipText object2) {
						return comp(object1.name,object2.name);
					}
				});
				Collections.sort(a.trips,new Comparator<TripData>(){
					@Override
					public int compare(TripData object1,
							TripData object2) {
						return comp(object1.trip,object2.trip);
					}
				});
				
				for(AipText apt:a.aiptexts)
					md.update(apt.name.getBytes());
				for(AirspaceArea aa:a.spaces)
					md.update(aa.name.getBytes());
				for(SigPoint sp:a.points)
					md.update(sp.name.getBytes());
				for(TripData apt:a.trips)
					md.update(apt.trip.getBytes());
			}								
			a.namedigest=toHex(md.digest());
			Log.i("fplan","namedigest:"+a.namedigest);
			if (!checksum.toLowerCase().equals(a.namedigest.toLowerCase()))
			{
				//Log.i("fplan","Bad checksum hex digest. Was: "+a.namedigest+" Expected: "+checksum);
				throw new RuntimeException("Bad checksum on downloaded data");
			}
			
		}

			
		return a;
	}
	private static<T> ArrayList<T> read_sub(Class Tclass,DataInputStream is, AirspaceProgress prog,int start,int end,
			int version, String what,ArrayList<T> prevspaces) throws IOException {
		//ArrayList<AirspaceArea> prevspaces=(ArrayList<AirspaceArea>)(previous.spaces.clone());
		int numspaces=is.readInt();
		if (numspaces>15000)
			throw new RuntimeException("Too many "+what+": "+numspaces);		
		ArrayList<Integer> spacekills=new ArrayList<Integer>();
		ArrayList<T> nspaces=new ArrayList<T>(); 
		for(int i=0;i<numspaces;++i)
		{
			if (prog!=null && (i%10==0 || i==numspaces-1))
				prog.report((int)(start+((end-start)*i)/numspaces));
			if (version>=5)
			{
				if (is.readByte()==0)
				{ //"kill"
					int idx=is.readInt();
					Log.i("fplan","Kill space with idx: "+idx);
					spacekills.add(idx);
					continue;
				}
			}
			
		    
		    T t;
			try {
				Class params[] = new Class[2];
				params[0]=DataInputStream.class;
				params[1]=Integer.TYPE;
				Method deser = Tclass.getDeclaredMethod("deserialize",params);
		  	    //Object[] mainArgs = new Object[]{is,version};
			    t=(T)deser.invoke(null, is,version);
			}catch(Exception e)
			{
				throw new RuntimeException(e);
			}		    		    
			//Log.i("fplan","Deserialize space: ");
			nspaces.add(t);
			//Log.i("fplan","Deser "+what);			
		}
		
		
		if (prevspaces!=null)
		{
			Collections.sort(spacekills);
			
			{
				int src=0;
				int trg=0;
				for(int i=0;src<prevspaces.size();)
				{
					int pk;
					if (i<spacekills.size()) pk=spacekills.get(i);
					else pk=-1;
					if (pk!=src)
					{
						//Log.i("fplan","Copying element "+src+" -> "+trg);
						if (src!=trg)
							prevspaces.set(trg,prevspaces.get(src));
						++src;
						++trg;
					}
					else
					{//pk==src
						//Log.i("fplan","Implementing spaces kill of idx: "+src);						
						//Log.i("fplan","Not copying element "+src+" -> anywhere");
						++i;
						++src;
					}						
				}
				int newsize=prevspaces.size()-spacekills.size();
				for(int i=prevspaces.size()-1;i>=newsize;--i)
				{
					//Log.i("fplan","Removing element "+i);
					prevspaces.remove(i);
				}
			}
			prevspaces.addAll(nspaces);
			return prevspaces;
		}
		else
		{
			return nspaces;
		}		
		
	}
	public void serialize(DataOutputStream os) throws IOException {
		
		Log.i("fplan","Serializing airspace");
		int numspaces=spaces.size();
		os.writeInt(0x8A31CDA);
		
		int version=8;
		os.writeInt(version); //version 8
		os.writeByte(1); //correct password
		os.writeUTF(aipgen);
		os.writeByte(1); //from scratch
		os.writeInt(numspaces);
		for(int i=0;i<numspaces;++i)
		{
			os.writeByte(1);//don't kill
			spaces.get(i).serialize(os);
		}
		int numpoints=points.size();
		os.writeInt(numpoints);
		for(int i=0;i<numpoints;++i)
		{
			os.writeByte(1);//don't kill
			points.get(i).serialize(os);
		}
		os.writeInt(aiptexts.size());
		for(int i=0;i<aiptexts.size();++i)
		{
			os.writeByte(1);//don't kill
			aiptexts.get(i).serialize(os);
		}
		
		os.writeInt(trips.size());
		for(int i=0;i<trips.size();++i)
		{
			os.writeByte(1);//don't kill
			trips.get(i).serialize(os);
		}
		
		os.writeInt(0x1eedbaa5); //magic
		os.writeUTF(namedigest);
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
	
	public static Airspace download(Airspace previous,AirspaceProgress prog,String user,String pass) throws Exception
	{
		return download(null,previous,prog,user,pass);
	}
	public static Airspace download(InputStream fakeDataForTest,Airspace previous,AirspaceProgress prog,String user,String pass) throws Exception
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
			nvps.add(new BasicNameValuePair("version","8"));
			nvps.add(new BasicNameValuePair("user",user));
			nvps.add(new BasicNameValuePair("password",pass));
			if (previous==null)
				nvps.add(new BasicNameValuePair("aipgen",""));
			else
				nvps.add(new BasicNameValuePair("aipgen",previous.aipgen));
			nvps.add(new BasicNameValuePair("zip","1"));
			InputStream rawinp=DataDownloader.postRaw("/api/get_airspaces",null, null, nvps,false);
			InflaterInputStream dinp=new InflaterInputStream(rawinp);
			inp=dinp;
		}
		Airspace airspace=deserialize(new DataInputStream(inp),previous,prog);
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
				Config.path);
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
				Config.path+filename);
		InputStream ofstream=new BufferedInputStream(
				new FileInputStream(path)
				);

		Airspace data=null;
		try
		{
			
			DataInputStream os=new DataInputStream(ofstream);
			data=Airspace.deserialize(os,null,null);//(Airspace)os.readObject();
			os.close();		
		}
		finally
		{
			ofstream.close();			
		}
		
		try
		{
			File chartlistpath = new File(extpath,
					Config.path+"chartlist.dat");			
			data.load_chart_list(chartlistpath);
		} catch(Throwable e)
		{
			e.printStackTrace();
			Log.i("fplan","Failed loading chart list");
		}
		return data;
	}
	public ArrayList<AirspaceArea> getSpaces() {
		return spaces;
	}
	public ArrayList<SigPoint> getPoints() {
		return points;
	}
	static public class VariantInfo
	{
		String chartname;
		String variant;
		public VariantInfo(String chartname,String variant)
		{
			this.chartname=chartname;
			this.variant=variant;
		}
	}
	static public class ChartInfo
	{
		private String icao;
		private HashMap<String,VariantInfo> variants=new HashMap<String,VariantInfo>();
		public ChartInfo(String icao,String chartname,String variant)
		{
			this.icao=icao;			
			this.variants.put(variant,new VariantInfo(chartname,variant));
		}
		public Collection<VariantInfo> getVariants()
		{
			return variants.values();
		}
		public void put(VariantInfo variantInfo) {
			variants.put(variantInfo.variant,variantInfo);			
		}
	}
	HashMap<String,ChartInfo> charts=new HashMap<String, ChartInfo>();
	
	ChartInfo getChart(String icao)
	{
		return charts.get(icao);
	}
	public void load_chart_list(File chartlistpath) throws IOException {
		DataInputStream ds=new DataInputStream(
				new FileInputStream(chartlistpath));
		charts.clear();
		int len=ds.readInt();
		int version=1;
		if (len<10000000)
			version=1;
		else
		{
			version=ds.readInt();
			len=ds.readInt();
		}
		for(int i=0;i<len;++i)
		{
			String icao=ds.readUTF(); //icao
			String chartname=ds.readUTF();
			String variant="";
			if (version>=2)
				variant=ds.readUTF();
			else
				/*human=*/ds.readUTF();
				
			ChartInfo prev=charts.get(icao);
			if (prev==null)
			{
				ChartInfo ci=new ChartInfo(icao,chartname,variant);
				charts.put(icao,ci);
			}
			else
			{
				prev.put(new VariantInfo(chartname,variant));
			}
		}
		ds.close();					
	}
	public void report_new_chart(String humanreadable, String chartname,String icao,String variant) {
		ChartInfo prev=charts.get(icao);
		if (prev==null)
		{
			ChartInfo ci=new ChartInfo(icao,chartname,variant);
			charts.put(icao,ci);
		}
		else
		{
			prev.put(new VariantInfo(chartname,variant));
		}
		
	}
	public void save_chart_list(File chartlistpath) throws IOException {
		DataOutputStream ds=new DataOutputStream(
				new FileOutputStream(chartlistpath));
		ds.writeInt(0x7fffffff);
		ds.writeInt(2); //version
		int len=0;
		for(Entry<String,ChartInfo> e:charts.entrySet())
		{
			for(VariantInfo variant:e.getValue().getVariants())
			{
				++len;
			}
		}
		
		ds.writeInt(len);
		for(Entry<String,ChartInfo> e:charts.entrySet())
		{
			for(VariantInfo variant:e.getValue().getVariants())
			{
				ds.writeUTF(e.getKey()); //icao
				ds.writeUTF(variant.chartname);
				ds.writeUTF(variant.variant);
				
			}
		}
		ds.close();			
		
	}
	public String[] getTripList() {
		ArrayList<String> ts=new ArrayList<String>();
		for(TripData t:trips)
		{
			ts.add(t.trip);
		}
		
		final Collator myCollator = Collator.getInstance();
		Collections.sort(ts, new Comparator<String>() {
			@Override
			public int compare(String object1, String object2) {
				return myCollator.compare(object1, object2);
			}
			
		});

		return ts.toArray(new String[]{});
	}
	public TripData getTrip(String trip) {
		for(TripData t:trips)
		{
			if (t.trip.equals(trip))
				return t;
		}
		return null;
	}
	
}
