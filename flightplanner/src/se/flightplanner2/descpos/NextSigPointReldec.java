package se.flightplanner2.descpos;

import java.util.Date;

import se.flightplanner2.DescribePosition;
import se.flightplanner2.GlobalPosition;
import se.flightplanner2.GlobalTripState;
import se.flightplanner2.Project;
import se.flightplanner2.TripState;
import se.flightplanner2.Project.LatLon;
import se.flightplanner2.TripState.NextSigPoints;
import android.util.Log;

public final class NextSigPointReldec extends RelDec {
	private final NextSigPoints ensp;
	String name;
	public NextSigPointReldec(NextSigPoints ensp) {
		this.ensp = ensp;
		name=ensp.name;
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
		float bearing=Project.bearing(ensp.latlon,mypos);
		double distance=Project.exacter_distance(mypos, ensp.latlon);
		StringBuilder sb=new StringBuilder();
		
		String shortdesc=String.format("%.0f miles %s %s",distance,DescribePosition.roughdir(bearing),name);
		String longdesc=String.format("%03.0f° %.1f miles from %s",bearing,distance,name);
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
		String icao_sigp_format=NextSigPointReldec.getSigPointPosDescrFromEnsp(ensp);
		if (icao_sigp_format!=null)
		{
			sb.append("(or)<br/>");
			sb.append("<p>"+icao_sigp_format+"</p");
		}
		return sb.toString();
	}

	static public String getSigPointPosDescrFromEnsp(NextSigPoints nesp) {
		
		if (nesp==null) return null;
		TripState tst=GlobalTripState.tripstate;
		Log.i("fplan.dp","Nesp:"+nesp+" tst: "+tst);
		if (tst!=null)
		{
			tst.update_ensp(nesp);
		}
		Date either=nesp.passed!=null ? nesp.passed : nesp.eta;
		Log.i("fplan.dp","Either date:"+either);
		if (either!=null)
		{
			Date now=new Date();
			if (Math.abs(now.getTime()-either.getTime())<30000) //within one minute
				return nesp.name;
		}
		if (nesp.passed!=null)
			return nesp.name+" "+DescribePosition.aviation_format_time(nesp.passed);
		if (nesp.eta!=null)		
			return "ESTIMATING "+nesp.name+" "+DescribePosition.aviation_format_time(nesp.eta);
		return null;
	}
}