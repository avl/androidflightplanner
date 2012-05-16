package se.flightplanner2.descpos;

import se.flightplanner2.DescribePosition;
import se.flightplanner2.GlobalPosition;
import se.flightplanner2.GlobalTripState;
import se.flightplanner2.Project;
import se.flightplanner2.SigPoint;
import se.flightplanner2.TripState;
import se.flightplanner2.Project.LatLon;
import se.flightplanner2.TripState.NextSigPoints;

public final class SigPointReldec extends RelDec {
	private final SigPoint sp;
	private String name;
	public SigPointReldec(SigPoint sp) {
		this.sp = sp;		
		name=sp.name;
		if (name.startsWith("E") && name.length()>5)
		{
			String icaocand=name.substring(0,4);
			if (icaocand.toUpperCase().equals(icaocand) && name.charAt(4)==' ')
			{
				name=name.substring(5);
			}
		}
	}
	public String getName()
	{
		return name;
	}

	@Override
	public String getDescr(boolean shorted,boolean exacter) {
		LatLon mypos=GlobalPosition.getLastLatLonPosition();
		float bearing=Project.bearing(sp.latlon,mypos);
		double distance=Project.exacter_distance(mypos, sp.latlon);
		StringBuilder sb=new StringBuilder();
		String icao_sigp_format=SigPointReldec.getSigPointPosDescr(sp);
		if (icao_sigp_format!=null)
		{
			if (shorted)
				return icao_sigp_format;
			sb.append("<p>"+icao_sigp_format+"</p");
		}
		String shortdesc;
		String longdesc;
		if (distance<0.25)
		{
			shortdesc=longdesc=name;
		} 
		else if (distance<.75)
		{
			shortdesc=name;
			longdesc=String.format("%03.0f° %.1f miles from %s",bearing,distance,name);
			
		}
		else
		{
			shortdesc=String.format("%.0f miles %s %s",distance,DescribePosition.roughdir(bearing),name);
			longdesc=String.format("%03.0f° %.1f miles from %s",bearing,distance,name);
		}
		
		if (shorted)
		{
			if (exacter)
				return longdesc;
			else
				return shortdesc;
		}
		
		//say "Long final rwy" if possible..
		sb.append("<p>");
		sb.append(shortdesc);
		sb.append("</p>");
		sb.append("(or)<br/>");
		sb.append("<p>");
		sb.append(longdesc);
		sb.append("</p>");
		return sb.toString();
	}

	static private String getSigPointPosDescr(SigPoint sp)
	{
		
		TripState st=GlobalTripState.tripstate;
		if (st==null) return null; 
		
		NextSigPoints nesp=st.getSPInfo(sp);
		return NextSigPointReldec.getSigPointPosDescrFromEnsp(nesp);
	}
}