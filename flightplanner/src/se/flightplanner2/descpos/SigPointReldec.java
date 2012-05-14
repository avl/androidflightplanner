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

	public SigPointReldec(SigPoint sp) {
		this.sp = sp;
	}

	@Override
	public String getDescr(boolean shorted) {
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
		String shortdesc=String.format("%.0f miles %s of %s",distance,DescribePosition.roughdir(bearing),name);
		if (shorted)
			return shortdesc;
		
		//say "Long final rwy" if possible..
		sb.append("<p>");
		sb.append(shortdesc);
		sb.append("</p>");
		sb.append("(or)<br/>");
		sb.append("<p>");
		sb.append(String.format("%03.0f° %.1f miles from %s",bearing,distance,name));
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