/**
 * 
 */
package se.flightplanner2;

import android.os.SystemClock;
import se.flightplanner2.Project.iMerc;
import se.flightplanner2.vector.Vector;
import se.flightplanner2.vector.Polygon.InsideResult;

public class SpaceStats
{
	public String diststr;
	public float dist;
	public int bearing;
	public boolean inside;
	public long updated;
	/**
	 * Only used by discontinued 3D-version.
	 * @param ipos
	 * @param area
	 * @return
	 */
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
			stats.inside=false;
		}
		else
		{
			stats.inside=true;
			stats.diststr="inside";
			stats.dist=0;
		}
		Vector diff=res.closest.minus(pos);
		stats.bearing=(int)Project.vector2heading(diff);
		return stats;
	}
}