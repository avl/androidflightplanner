package se.flightplanner2;

import java.util.Date;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.util.Log;

import se.flightplanner2.Airspace.ChartInfo;
import se.flightplanner2.Project.LatLon;
import se.flightplanner2.vector.BoundingBox;
import se.flightplanner2.vector.Vector;
import se.flightplanner2.vector.Polygon.InsideResult;

public class AirspaceLookup implements AirspaceLookupIf {
	static public class AirspaceDetails
	{
		boolean hasextra;
		String[] extended_icaos;
	}
	public AirspaceDetails get_airspace_details(double abit,long marker_size,
		Vector just_a_bit_in,ArrayList<String> details,ArrayList<String> extradetails) {
		boolean hasextra=false;
		ArrayList<String> extended=new ArrayList<String>();
		BoundingBox bb=BoundingBox.aroundpoint(just_a_bit_in, abit);
		for(AirspaceArea inarea:areas.get_areas(bb))
		{
			
			InsideResult r=inarea.poly.inside(just_a_bit_in);
			//double cd=r.closest.minus(point).length();
			if (r.isinside) //our polygons are clockwise, because the Y-axis points down - this inverts the meaning of inside and outside
			{ //If _INSIDE_ polygon
				String det=inarea.floor+"-"+inarea.ceiling+": "+inarea.name;
				details.add(det);
				extradetails.add(det);
				for(String fre : inarea.freqs)
				{
					if (fre.length()>0)
					{
						//Log.i("fplan","Adding airspace detail "+fre);
						extradetails.add(fre);
						hasextra=true;
					}					
				}
			}
		}					
		if (details.size()==0)
		{
			details.add("0 ft-FL 095: Uncontrolled Airspace");
			extradetails.add("0 ft-FL 095: Uncontrolled Airspace");
		}
	
		//bb.
		BoundingBox bb2=BoundingBox.aroundpoint(just_a_bit_in, marker_size);
		for(SigPoint sp:majorAirports.findall(bb2))
		{
			if (sp.extra!=null)
			{
				if (sp.extra.icao!=null && (sp.extra.notams.length>0 || sp.extra.metar!=null || sp.extra.taf!=null))
				{
					//details.add(sp.name);
					//extradetails.add(sp.name);
					extended.add(sp.extra.icao);
				}
			}
		}
		AirspaceDetails ret=new AirspaceDetails();
		ret.extended_icaos=extended.toArray(new String[]{});
		ret.hasextra=hasextra;
		return ret;
	}
	public ArrayList<AipText> getAipText(String icao)
	{
		return icao2aiptext.get(icao);
	}
	public AirspaceLookup(Airspace airspace) {
		
		this.airspace=airspace;
		
		for(AipText apt:airspace.aiptexts)
		{
			ArrayList<AipText> item=icao2aiptext.get(apt.icao);
			if (item==null)
			{
				ArrayList<AipText> t =new ArrayList<AipText>();
				t.add(apt);
				icao2aiptext.put(apt.icao, t);			
			}
			else
			{
				item.add(apt);
			}
		}
		
		
		ArrayList<AirspaceArea> areaarr;
		if (airspace==null)
			areaarr=new ArrayList<AirspaceArea>();
		else
			areaarr=airspace.getSpaces();
		areas=new AirspaceAreaTree(areaarr);
		ArrayList<SigPoint> pointarr;
		if (airspace==null)
			pointarr=new ArrayList<SigPoint>();
		else
			pointarr=airspace.getPoints();
		ArrayList<SigPoint> sigpoints=new ArrayList<SigPoint>();
		ArrayList<SigPoint> major_airports=new ArrayList<SigPoint>();
		ArrayList<SigPoint> minor_airfields=new ArrayList<SigPoint>();
		ArrayList<SigPoint> others=new ArrayList<SigPoint>();
		ArrayList<SigPoint> obsts=new ArrayList<SigPoint>();
		ArrayList<SigPoint> cities=new ArrayList<SigPoint>();
		ArrayList<SigPoint> towns=new ArrayList<SigPoint>();
		for(SigPoint po: pointarr)
		{
			//Log.i("fplan","Type:"+po.kind);
			if (po.kind=="port")
				major_airports.add(po);
			else
			if (po.kind=="field")
				minor_airfields.add(po);
			else
			if (po.kind=="obstacle")
				obsts.add(po);
			else
			if (po.kind=="city")
				cities.add(po);
			else
			if (po.kind=="town")
				towns.add(po);
			else
			{
				if (po.kind=="sigpoint")
					sigpoints.add(po);
				others.add(po);
			}
		}
		majorAirports=new AirspaceSigPointsTree(major_airports);
		minorAirfields=new AirspaceSigPointsTree(minor_airfields);
		allObst=new AirspaceSigPointsTree(obsts);
		allOthers=new AirspaceSigPointsTree(others);
		allSigPoints=new AirspaceSigPointsTree(sigpoints);
		allCities=new AirspaceSigPointsTree(cities);
		allTowns=new AirspaceSigPointsTree(towns);
		by_icao=new HashMap<String, SigPoint>();
		for(SigPoint sp:major_airports)
		{
			if (sp.extra!=null && sp.extra.icao!=null && sp.extra.icao.length()>0)
				by_icao.put(sp.extra.icao,sp);	
		}
		// TODO Auto-generated constructor stub
	}
	public AirspaceAreaTree areas;
	@Override
	public AirspaceAreaTree getAreas(){return areas;}
	public HashMap<String,SigPoint> by_icao;
	public AirspaceSigPointsTree minorAirfields;
	public AirspaceSigPointsTree majorAirports;
	public AirspaceSigPointsTree allObst;
	public AirspaceSigPointsTree allOthers;
	public AirspaceSigPointsTree allSigPoints;
	public AirspaceSigPointsTree allCities;
	public AirspaceSigPointsTree allTowns;
	public Airspace airspace;
	public HashMap<String,ArrayList<AipText> > icao2aiptext=new HashMap<String, ArrayList<AipText>>();
	
	
	public SigPoint getByIcao(String icao) {
		return by_icao.get(icao);
	}
	static private class Pair
	{
		String human;
		String icao;
		float dist;
	}
	public ChartInfo getChartInfo(String icao)
	{
		return airspace.getChart(icao);		
	}
	Comparator<Pair> comp=new Comparator<Pair>(){
		@Override
		public int compare(Pair o1, Pair o2) {
			if (o1.dist<o2.dist) return -1;
			if (o1.dist>o2.dist) return +1;
			return 0;
		}					
	};
	static public class ClosestAirportResult
	{
		public float distance;
		public String icao;
		public String metar;
	}
	public ClosestAirportResult getClosestAirportWithMetar(LatLon ownpos) {
		
		ClosestAirportResult res=new ClosestAirportResult();
		res.distance=1e30f;
		for(SigPoint p:majorAirports.getall())
		{			
			if (p.extra!=null && p.extra.icao!=null && !p.extra.icao.equals("")
					&& p.extra.metar!=null && !p.extra.metar.equals(""))
			{
				float dist=(float) Project.exacter_distance(ownpos, 
						Project.merc2latlon(p.pos, 13));
				if (dist<res.distance)
				{
					res.distance=dist;
					res.icao=p.extra.icao;
					res.metar=p.extra.metar;
				}
			}
		}
		if (res.icao!=null)
			return res;
		else
			return null;
	}
	static public class QnhGuessResult
	{
		String descr;
		int qnh;
	}
	public QnhGuessResult GuessQnhFromMetar(String metar,String icao)
	{
		Log.i("fplan.regexp","Matching: <"+metar+">");
		String pattern = "\\s*(\\d{6}).*\\s+Q(\\d{4})\\b.*";
		Pattern p = Pattern.compile(pattern);
		Matcher m = p.matcher(metar);
		
		if (!m.matches() || m.groupCount()!=2)
		{
			Log.i("fplan.regexp","Failed to match, wrong groupcount");
			return null;
		}
		String time=m.group(1);
		Date now=new Date();
		int dayofmonth=Integer.parseInt(time.substring(0, 2));
		int hour=Integer.parseInt(time.substring(2, 4));
		int minute=Integer.parseInt(time.substring(4, 6));
		Log.i("fplan.regexp","Parsed "+dayofmonth+" "+hour+" "+minute);
		//Calendar nowcal=GregorianCalendar.getInstance(TimeZone.getTimeZone("UTC"));
		Calendar cal = GregorianCalendar.getInstance(TimeZone.getTimeZone("UTC"));
	    Log.i("fplan.regexp","Now: "+cal);
		
	    cal.set(Calendar.DAY_OF_MONTH, dayofmonth);
	    cal.set(Calendar.HOUR_OF_DAY, hour);
	    cal.set(Calendar.MINUTE, minute);
	    Log.i("fplan.regexp","Time of METAR: "+cal);
	    Date metartime=cal.getTime();
	    long datediff=((now.getTime()-metartime.getTime())/1000)/(60*60*24);
	    //If the METAR appears more than 20 days into the future, it is certainly just in the past.
	    if (datediff<-20)
	    {
	    	cal.add(Calendar.MONTH, -1);
	    }
	    double timediff=((now.getTime()-metartime.getTime())/1000.0)/(60.0*60.0);
	    Log.i("fplan.regexp","Metar information is "+timediff+" hours old");
	    if (timediff>6.0)
	    	return null;
	    QnhGuessResult res=new QnhGuessResult();

		int qnh=Integer.parseInt(m.group(2));
	    res.qnh=qnh;
	    if (timediff<-5)
	    	return null;
	    if (timediff<0)
	    	timediff=0;
	    res.descr=" at "+icao+" +"+(int)timediff+"H";
		return res;

	}
	public void getAdChartNames(ArrayList<String> icaos,ArrayList<String> humanNames, LatLon location) {
		
		ArrayList<Pair> tmp=new ArrayList<Pair>();
		ArrayList<Pair> closest=new ArrayList<Pair>();
		for(SigPoint p:majorAirports.getall())
		{
			if (p.extra!=null && p.extra.icao!=null && !p.extra.icao.equals(""))
			{
				ChartInfo ci=airspace.getChart(p.extra.icao);
				if (ci==null &&
					(p.extra.metar==null || p.extra.metar.equals("")) &&
					(p.extra.taf==null || p.extra.taf.equals("")) &&
					(p.extra.notams==null || p.extra.notams.length==0)
						) continue;
				Pair pair=new Pair();
				pair.human=p.name;
				pair.icao=p.extra.icao;
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
		final Collator myCollator = Collator.getInstance();
		Collections.sort(tmp, new Comparator<Pair>() {
			@Override
			public int compare(Pair object1, Pair object2) {
				return myCollator.compare(object1.human,object2.human);
			}
		});
		if (location!=null && closest.size()>0)
		{
			for(Pair pair:closest)
			{
				icaos.add(pair.icao);
				humanNames.add(pair.human);
			}
			icaos.add(null);
			humanNames.add("----");
		}
		for(Pair pair:tmp)
		{
			icaos.add(pair.icao);
			humanNames.add(pair.human);
		}

	}
	@Override
	public ArrayList<AirspaceArea> getAllAirspace() {
		if (airspace==null || airspace.spaces==null)
			return new ArrayList<AirspaceArea>(); 
		return airspace.spaces;
	}
	HashMap<String,Boolean> haveprojcache=new HashMap<String, Boolean>();
	public boolean haveproj(String chartname) {
		Boolean b=haveprojcache.get(chartname);
		if (b!=null) return b;
		b=AdChartLoader.haveproj(chartname,airspace!=null ? airspace.storage : "");
		haveprojcache.put(chartname, b);
		return b;
	}

	
	
}
