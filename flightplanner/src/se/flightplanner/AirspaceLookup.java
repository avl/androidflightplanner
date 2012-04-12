package se.flightplanner;

import java.util.ArrayList;
import java.util.HashMap;

import android.util.Log;

import se.flightplanner.vector.BoundingBox;
import se.flightplanner.vector.Vector;
import se.flightplanner.vector.Polygon.InsideResult;

public class AirspaceLookup {
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
			if (sp.icao!=null && (sp.notams.length>0 || sp.metar!=null || sp.taf!=null))
			{
				//details.add(sp.name);
				//extradetails.add(sp.name);
				extended.add(sp.icao);
			}
		}
		AirspaceDetails ret=new AirspaceDetails();
		ret.extended_icaos=extended.toArray(new String[]{});
		ret.hasextra=hasextra;
		return ret;
	}

	public AirspaceLookup(Airspace airspace) {
		
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
				others.add(po);					
		}
		majorAirports=new AirspaceSigPointsTree(major_airports);
		minorAirfields=new AirspaceSigPointsTree(minor_airfields);
		allObst=new AirspaceSigPointsTree(obsts);
		allOthers=new AirspaceSigPointsTree(others);
		allCities=new AirspaceSigPointsTree(cities);
		allTowns=new AirspaceSigPointsTree(towns);
		by_icao=new HashMap<String, SigPoint>();
		for(SigPoint sp:major_airports)
		{
			if (sp.icao!=null && sp.icao.length()>0)
				by_icao.put(sp.icao,sp);	
		}
		// TODO Auto-generated constructor stub
	}
	public AirspaceAreaTree areas;
	public HashMap<String,SigPoint> by_icao;
	public AirspaceSigPointsTree minorAirfields;
	public AirspaceSigPointsTree majorAirports;
	public AirspaceSigPointsTree allObst;
	public AirspaceSigPointsTree allOthers;
	public AirspaceSigPointsTree allCities;
	public AirspaceSigPointsTree allTowns;
	public SigPoint getByIcao(String icao) {
		return by_icao.get(icao);
	}
}
