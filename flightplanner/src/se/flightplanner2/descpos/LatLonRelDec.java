package se.flightplanner2.descpos;

import se.flightplanner2.GlobalPosition;
import se.flightplanner2.Project;
import se.flightplanner2.Project.LatLon;
import android.util.Log;

public final class LatLonRelDec extends RelDec {
	private final boolean decimal;

	public LatLonRelDec(boolean decimal) {
		this.decimal = decimal;
	}
	public String getName()
	{
		return "WGS84 Position";
	}

	@Override
	public String getDescr(boolean shortdesc,boolean exacter) {
		LatLon mypos=GlobalPosition.getLastLatLonPosition();
		Log.i("fplan","DescribePosition update");
		if (shortdesc)
			return mypos.toString2();
		if (decimal)
		{
			return String.format("<p>WGS84 Decimal:</p>Latitude: %02.4f<br/>Longitude: %03.4f<br/>",mypos.lat,mypos.lon);
		}
		else
		{
			
			return String.format("<p>WGS84:</p><p>"+mypos.toString2().replace(" ", "<br/>")+"</p>");					
		}
	}
}