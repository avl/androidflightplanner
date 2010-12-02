/**
 * 
 */
package se.flightplanner;

import android.os.SystemClock;
import se.flightplanner.Project.iMerc;
import se.flightplanner.map3d.ObserverContext;
import se.flightplanner.map3d.ObserverContext.ObserverState;
import se.flightplanner.vector.Vector;
import se.flightplanner.vector.Polygon.InsideResult;

public class SpaceStats
{
	public String diststr;
	public float dist;
	public int bearing;
	public long updated;
	public static SpaceStats getStats(iMerc ipos, AirspaceArea area) {
		SpaceStats stats=new SpaceStats();
		Vector pos=ipos.toVector();
		InsideResult res=area.poly.inside(pos);
		stats.updated=SystemClock.uptimeMillis();
		//String diststr;
		if (!res.isinside)
		{
			Vector close=area.poly.closest(pos);
			double nmdist=close.minus(pos).length()/Project.approx_scale(pos.y, 13, 1.0);
			stats.diststr=String.format("%.0fnm", nmdist);
			stats.dist=(float)nmdist;
		}
		else
		{
			stats.diststr="inside";
			stats.dist=0;
		}
		Vector diff=res.closest.minus(pos);
		stats.bearing=(int)Project.vector2heading(diff);
		return stats;
	}
}