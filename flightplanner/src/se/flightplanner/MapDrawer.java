package se.flightplanner;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import se.flightplanner.GetMapBitmap.BitmapRes;
import se.flightplanner.GuiSituation.Clickable;
import se.flightplanner.Project.LatLon;
import se.flightplanner.Project.Merc;
import se.flightplanner.Project.iMerc;
import se.flightplanner.TripData.Waypoint;
import se.flightplanner.vector.BoundingBox;
import se.flightplanner.vector.Vector;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.Paint.Style;
import android.location.Location;
import android.os.SystemClock;
import android.util.Log;

public class MapDrawer {

	private Paint bigtextpaint;
	private Paint linepaint;
	private Paint thinlinepaint;
	private Paint trippaint;
	private Paint seltrippaint;
	private Paint arrowpaint;
	private Paint backgroundpaint;
	private Paint textpaint;
	private float x_dpmm, y_dpmm;
	private SimpleDateFormat formatter = new SimpleDateFormat("kkmmss");

	public MapDrawer(float x_dpmm, float y_dpmm) {
		this.x_dpmm = x_dpmm;
		this.y_dpmm = y_dpmm;
		int foreground = Color.WHITE;
		float textsize = y_dpmm * 1.75f;
		float bigtextsize = y_dpmm * 2.7f; // 6.5 mm text size

		textpaint = new Paint();
		textpaint.setAntiAlias(true);
		textpaint.setStrokeWidth(5);
		textpaint.setColor(foreground);
		textpaint.setStrokeCap(Paint.Cap.ROUND);
		textpaint.setTextSize(textsize);
		textpaint.setTypeface(Typeface.create(Typeface.SANS_SERIF,
				Typeface.NORMAL));

		bigtextpaint = new Paint();
		bigtextpaint.setAntiAlias(true);
		bigtextpaint.setStrokeWidth(5);
		bigtextpaint.setColor(foreground);
		bigtextpaint.setStrokeCap(Paint.Cap.ROUND);
		bigtextpaint.setTextSize(bigtextsize);
		bigtextpaint.setTypeface(Typeface.create(Typeface.SANS_SERIF,
				Typeface.NORMAL));
		linepaint = new Paint();
		linepaint.setAntiAlias(true);
		linepaint.setStrokeWidth(5);
		linepaint.setColor(Color.RED);
		linepaint.setStrokeCap(Paint.Cap.ROUND);

		thinlinepaint = new Paint();
		thinlinepaint.setAntiAlias(true);
		thinlinepaint.setStrokeWidth(2);
		thinlinepaint.setStyle(Style.STROKE);
		thinlinepaint.setColor(Color.RED);
		thinlinepaint.setStrokeCap(Paint.Cap.ROUND);

		trippaint = new Paint();
		trippaint.setAntiAlias(true);
		trippaint.setStrokeWidth(5);
		trippaint.setColor(Color.YELLOW);
		trippaint.setStrokeCap(Paint.Cap.ROUND);

		seltrippaint = new Paint();
		seltrippaint.setAntiAlias(true);
		seltrippaint.setStrokeWidth(5);
		seltrippaint.setColor(Color.WHITE);
		seltrippaint.setStrokeCap(Paint.Cap.ROUND);

		backgroundpaint = new Paint();
		backgroundpaint.setStyle(Style.FILL);
		backgroundpaint.setARGB(0xa0, 0, 0, 0);

		arrowpaint = new Paint();
		arrowpaint.setAntiAlias(true);
		arrowpaint.setStyle(Style.FILL);
		arrowpaint.setStrokeWidth(5);
		arrowpaint.setColor(Color.WHITE);
		arrowpaint.setStrokeCap(Paint.Cap.ROUND);
	}

	private void renderText(Canvas canvas, Vector p, String text) {
		Rect rect = new Rect();
		textpaint.getTextBounds(text, 0, text.length(), rect);
		int adj = (rect.bottom - rect.top) / 3;
		rect.left += p.x - 1 + 7;
		rect.right += p.x + 1 + 7;
		rect.top += p.y - 2 + adj;
		rect.bottom += p.y + 3 + adj;
		canvas.drawRect(rect, backgroundpaint);
		canvas.drawText(text, (float) (p.x + 7), (float) (p.y + adj), textpaint);
		linepaint.setStrokeWidth(10);
		int c = linepaint.getColor();
		linepaint.setColor(Color.BLACK);
		canvas.drawPoint((float) p.x, (float) p.y, linepaint);
		linepaint.setColor(c);
		linepaint.setStrokeWidth(5);
		canvas.drawPoint((float) p.x, (float) p.y, linepaint);
	}

	static public class DrawResult {
		int lastcachesize;
	}

	private int left;
	private int right;
	private int top;
	private int bottom;

	public DrawResult draw_actual_map(TripData tripdata, TripState tripstate,
			AirspaceLookup lookup, Canvas canvas, Rect screen_extent,
			Location lastpos, GetMapBitmap bitmaps, final GuiSituation gui,
			long last_real_position, String download_status,
			InformationPanel panel) {
		Transform tf = gui.getTransform();
		boolean extrainfo = gui.getExtraInfo();
		int extrainfolineoffset = gui.getExtrainfolineoffset();
		boolean isDragging = (gui.getDrag_center13() != null);
		int zoomlevel = gui.getZoomlevel();
		if (zoomlevel > 13)
			throw new RuntimeException("zoomlevel must be <=13");
		gui.clearClickables();
		ArrayList<GuiSituation.Clickable> clickables = gui.getClickables();

		DrawResult res = new DrawResult();
		left = screen_extent.left;
		right = screen_extent.right;
		top = screen_extent.top;
		bottom = screen_extent.bottom;
		int sizex = screen_extent.width();
		int sizey = screen_extent.height();

		// Project.latlon2merc(new
		// LatLon(lastpos.getLatitude(),lastpos.getLongitude()),13);
		Merc mypos = tf.getPos();
		Merc mypos13 = Project.merc2merc(tf.getPos(), zoomlevel, 13);

		Vector arrow = gui.getArrow();
		Merc screen_center = tf.screen2merc(new Vector(sizex / 2, sizey / 2));
		Merc screen_center13 = Project.merc2merc(screen_center, zoomlevel, 13);

		double fivenm13 = Project.approx_scale(screen_center13.y, 13, 10);
		double diagonal13;
		{
			int zoomgap13 = 13 - zoomlevel;
			diagonal13 = ((1 << zoomgap13) * (Math.sqrt(arrow.x * arrow.x
					+ arrow.y * arrow.y) + 50)) + 1;
		}
		BoundingBox bb13 = new BoundingBox(screen_center13.x,
				screen_center13.y, screen_center13.x, screen_center13.y)
				.expand(diagonal13);

		BoundingBox smbb13 = new BoundingBox(mypos13.x, mypos13.y, mypos13.x,
				mypos13.y).expand(fivenm13);

		if (bitmaps != null) {
			iMerc centertile = new iMerc((int) screen_center.x & (~255),
					(int) screen_center.y & (~255));
			int diagonal = (int) Math.sqrt((sizex / 2) * (sizex / 2)
					+ (sizey * sizey * 3) / 5);
			int minus = (diagonal + 255) / 256;
			int tot = 2 * minus + 1;
			iMerc topleft = new iMerc(centertile.getX() - (256 * minus),
					centertile.getY() - 256 * minus);
			int cachesize = tot * tot;
			float hdg = (float) (tf.hdgrad * (180.0 / Math.PI));

			for (int j = 0; j < tot; ++j) {

				for (int i = 0; i < tot; ++i) {
					iMerc cur = new iMerc(topleft.getX() + 256 * i,
							topleft.getY() + 256 * j);
					if (cur.getX() < 0 || cur.getY() < 0)
						continue;
					Vector v = tf.merc2screen(new Merc(cur.getX(), cur.getY()));
					if (!tileOnScreen((float) v.x, (float) v.y, tf))
						continue;
					BitmapRes b = null;
					// Log.i("fplan","Bitmap for "+cur);
					b = bitmaps.getBitmap(cur, zoomlevel, cachesize);
					if (b != null && b.b != null) {
						// float px=(float)(v.x+i*256);
						// float py=(float)(v.y+j*256);

						// Log.i("fplan","Drawing bitmap at "+v.x+","+v.y);
						canvas.save();
						RectF trg = new RectF((float) v.x, (float) v.y,
								(float) v.x + 256, (float) v.y + 256);
						Rect src = b.rect;
						canvas.rotate(-hdg, (float) v.x, (float) v.y);
						canvas.drawBitmap(b.b, src, trg, null);
						canvas.restore();
					}
				}
			}
			res.lastcachesize = cachesize;
		}

		// sigPointTree.verify();
		if (zoomlevel >= 8 && lookup != null) {
			for (AirspaceArea as : lookup.areas.get_areas(bb13)) {/*
																 * boolean
																 * all_left
																 * =true;
																 * boolean
																 * all_right
																 * =true;
																 * boolean
																 * all_above
																 * =true;
																 * boolean
																 * all_below
																 * =true; int
																 * l=as.points
																 */
				ArrayList<Vector> vs = new ArrayList<Vector>();
				for (LatLon latlon : as.points) {
					Merc m = Project.latlon2merc(latlon, zoomlevel);
					Vector v = tf.merc2screen(m);
					vs.add(v);
				}
				for (int i = 0; i < vs.size(); ++i) {
					Vector a = vs.get(i);
					Vector b = vs.get((i + 1) % vs.size());
					canvas.drawLine((float) a.getx(), (float) a.gety(),
							(float) b.getx(), (float) b.gety(), linepaint);
				}
			}
		}
		if (zoomlevel >= 9 && lookup != null) {
			for (SigPoint sp : lookup.allOthers.findall(bb13)) {
				// Merc m=Project.merc2merc(sp.pos,13,zoomlevel);
				Merc m = Project.latlon2merc(sp.latlon, zoomlevel);
				// new Merc(sp.pos.x/(1<<zoomgap),
				// sp.pos.y/(1<<zoomgap));
				Vector p = tf.merc2screen(m);
				// Log.i("fplan",String.format("dxsigp: %s: %f %f",sp.name,px,py));
				// textpaint.setARGB(0, 255,255,255);
				textpaint.setARGB(0xff, 0xff, 0xa0, 0xa0);
				linepaint.setARGB(0xff, 0xff, 0xa0, 0xa0);
				renderText(canvas, p, sp.name);
			}

			for (SigPoint sp : lookup.allObst.findall(smbb13)) {
				/*
				 * double x=sp.pos.x/(1<<zoomgap); double
				 * y=sp.pos.y/(1<<zoomgap);
				 * //Log.i("fplan",String.format("sigp: %s: %f %f"
				 * ,sp.name,sp.pos.x,sp.pos.y)); double
				 * px=rot_x(x-center.x,y-center.y)+ox; double
				 * py=rot_y(x-center.x,y-center.y)+oy;
				 */
				// Merc m=Project.merc2merc(sp.pos,13,zoomlevel);
				Merc m = Project.latlon2merc(sp.latlon, zoomlevel);
				Vector p = tf.merc2screen(m);

				// Log.i("fplan",String.format("dxsigp: %s: %f %f",sp.name,px,py));
				// textpaint.setARGB(0, 255,255,255);
				textpaint.setARGB(0xff, 0xff, 0xa0, 0xff);
				linepaint.setARGB(0xff, 0xff, 0xa0, 0xff);
				renderText(canvas, p, String.format("%.0fft", sp.alt));
				// canvas.drawText(String.format("%.0fft",sp.alt), (float)(p.x),
				// (float)(p.y), textpaint);
				// canvas.drawPoint((float)p.x,(float)p.y,linepaint);
			}
		}
		if (zoomlevel >= 9 && lookup != null) {
			for (SigPoint sp : lookup.allAirfields.findall(bb13)) {
				// Merc m=Project.merc2merc(sp.pos,13,zoomlevel);
				Merc m = Project.latlon2merc(sp.latlon, zoomlevel);
				Vector p = tf.merc2screen(m);
				String text;
				if (zoomlevel >= 9)
					text = sp.name;
				else
					text = sp.name;
				// Log.i("fplan",String.format("airf: %s: %f %f",sp.name,p.x,p.y));

				textpaint.setColor(Color.GREEN);
				linepaint.setColor(Color.GREEN);
				renderText(canvas, p, text);
			}
		}

		if (tripdata != null && tripdata.waypoints.size() >= 2) {
			float[] lines = new float[4 * (tripdata.waypoints.size() - 1)];
			int curwp = -1;
			curwp = tripstate.get_target();
			for (int i = 0; i < tripdata.waypoints.size() - 1; ++i) {
				// String part=tripdata.waypoints.get(i+1).legpart;
				if (i == 0) {
					Waypoint wp1 = tripdata.waypoints.get(i);
					Merc m1 = Project.latlon2merc(wp1.latlon, zoomlevel);
					Vector p1 = tf.merc2screen(m1);
					lines[4 * i] = (float) p1.x;
					lines[4 * i + 1] = (float) p1.y;
				} else {
					lines[4 * i] = lines[4 * i - 2];
					lines[4 * i + 1] = lines[4 * i - 1];
				}
				Waypoint wp2 = tripdata.waypoints.get(i + 1);
				Merc m2 = Project.latlon2merc(wp2.latlon, zoomlevel);
				Vector p2 = tf.merc2screen(m2);
				lines[4 * i + 2] = (float) p2.x;
				lines[4 * i + 3] = (float) p2.y;

			}
			for (int i = 0; i < tripdata.waypoints.size() - 1; ++i) {
				Paint p;
				if (i == curwp - 1)
					p = trippaint;
				else
					p = trippaint;
				canvas.drawLine(lines[4 * i + 0], lines[4 * i + 1],
						lines[4 * i + 2], lines[4 * i + 3], p);
			}
			textpaint.setColor(Color.WHITE);
			for (Waypoint wp : tripdata.waypoints) {
				if (wp.lastsub == 0)
					continue; // Only draw actual waypoints, not climb- or
								// descent-events.
				Merc m = Project.latlon2merc(wp.latlon, zoomlevel);
				Vector p = tf.merc2screen(m);
				// double px=rot_x(m.x-center.x,m.y-center.y)+ox;
				// double py=rot_y(m.x-center.x,m.y-center.y)+oy;
				textpaint.setColor(Color.WHITE);
				linepaint.setColor(Color.WHITE);
				renderText(canvas, p, wp.name);
			}
		}
		boolean havefix = lastpos.getTime() > 3600 * 24 * 10 * 1000
				&& SystemClock.uptimeMillis() - last_real_position < 5000;

		if (!havefix) {
			linepaint.setStrokeWidth(15);
			linepaint.setARGB(190, 255, 200, 128);
			canvas.drawLine(left, top, right, bottom, linepaint);
			canvas.drawLine(left, bottom, right, top, linepaint);
			linepaint.setStrokeWidth(5);
			linepaint.setColor(Color.RED);
		}

		if (!isDragging) {
			arrowpaint.setColor(Color.BLACK);
			Path path = new Path();
			path.moveTo((int) arrow.x - 10, (int) arrow.y + 2);
			path.lineTo((int) arrow.x + 10, (int) arrow.y + 2);
			path.lineTo((int) arrow.x, (int) arrow.y - 15);
			path.close();
			canvas.drawPath(path, arrowpaint);
			int fivenm = (int) Project.approx_scale(mypos.y, zoomlevel, 5);
			int fivemin = 0;

			if (lastpos.hasSpeed()) {
				float fivemindist = (float) ((lastpos.getSpeed() * 60 * 5) / 1852.0f);
				fivemin = (int) Project.approx_scale(mypos.y, zoomlevel,
						fivemindist);
			}
			canvas.drawRect((int) arrow.x, (int) arrow.y - fivenm,
					(int) arrow.x + 6, (int) arrow.y - fivenm + 4, arrowpaint);
			canvas.drawRect((int) arrow.x - 10, (int) arrow.y - fivemin,
					(int) arrow.x, (int) arrow.y - fivemin + 4, arrowpaint);
			canvas.drawRect((int) arrow.x - 2, (int) 0, (int) arrow.x + 2,
					(int) arrow.y, arrowpaint);
			arrowpaint.setColor(Color.WHITE);
			path = new Path();
			path.moveTo((int) arrow.x - 7, (int) arrow.y);
			path.lineTo((int) arrow.x + 7, (int) arrow.y);
			path.lineTo((int) arrow.x, (int) arrow.y - 12);
			path.close();
			canvas.drawPath(path, arrowpaint);
			canvas.drawRect((int) arrow.x, (int) arrow.y - fivenm + 1,
					(int) arrow.x + 5, (int) arrow.y - fivenm + 3, arrowpaint);
			canvas.drawRect((int) arrow.x - 9, (int) arrow.y - fivemin + 1,
					(int) arrow.x, (int) arrow.y - fivemin + 3, arrowpaint);
			canvas.drawRect((int) arrow.x - 1, (int) 0, (int) arrow.x + 1,
					(int) arrow.y, arrowpaint);
		}

		else {
			if (lastpos != null) {
				Merc pos = Project.latlon2merc(new LatLon(
						lastpos.getLatitude(), lastpos.getLongitude()),
						zoomlevel);
				Merc dest;
				if (lastpos != null && lastpos.hasBearing()) {
					float hdg = lastpos.getBearing();
					Vector d = new Vector(0, -150);
					Vector d2 = d.rot(hdg / (180.0f / Math.PI));
					dest = new Merc(pos.x + d2.x, pos.y + d2.y);
				} else {
					dest = pos;
				}
				Vector screenpos = tf.merc2screen(pos);
				Vector screenpos2 = tf.merc2screen(dest);
				Vector forward = screenpos2.minus(screenpos).normalized();
				Vector left = forward.rot90l();
				Vector right = forward.rot90r();
				arrowpaint.setColor(Color.BLACK);
				arrowpaint.setStrokeWidth(4);
				Path path = new Path();
				path.moveTo((int) (screenpos.x + 10 * left.x - 2 * forward.x),
						(int) (screenpos.y + 10 * left.y - 2 * forward.y));
				path.lineTo((int) (screenpos.x + 10 * right.x - 2 * forward.x),
						(int) (screenpos.y + 10 * right.y - 2 * forward.y));
				path.lineTo((int) (screenpos.x + 15 * forward.x),
						(int) (screenpos.y + 15 * forward.y));
				path.close();
				canvas.drawPath(path, arrowpaint);

				canvas.drawLine((float) screenpos.x, (float) screenpos.y,
						(float) screenpos2.x, (float) screenpos2.y, arrowpaint);
				arrowpaint.setColor(Color.WHITE);
				arrowpaint.setStrokeWidth(2);
				path = new Path();
				path.moveTo((int) (screenpos.x + 7 * left.x),
						(int) (screenpos.y + 7 * left.y));
				path.lineTo((int) (screenpos.x + 7 * right.x),
						(int) (screenpos.y + 7 * right.y));
				path.lineTo((int) (screenpos.x + 12 * forward.x),
						(int) (screenpos.y + 12 * forward.y));
				path.close();
				canvas.drawPath(path, arrowpaint);

				canvas.drawLine((float) screenpos.x, (float) screenpos.y,
						(float) screenpos2.x, (float) screenpos2.y, arrowpaint);

			}

		}

		InformationPanel we = panel;
		if (we != null) {
			float tsy = bigtextpaint.getTextSize() + 2;
			final int maxlines = getNumInfoLines(bottom - top);
			Merc me = Project.merc2merc(new Merc(we.getPoint()), 13, zoomlevel);
			if (me != null) {
				// float
				// px=(float)rot_x(p.getx()-center.x,p.gety()-center.y)+ox;
				// float
				// py=(float)rot_y(p.getx()-center.x,p.gety()-center.y)+oy;
				Vector p = tf.merc2screen(me);
				thinlinepaint.setColor(Color.BLUE);
				canvas.drawCircle((float) p.x, (float) p.y, 10.0f,
						thinlinepaint);
			}

			thinlinepaint.setColor(Color.WHITE);
			float y = bottom - 3.0f * y_dpmm;//
			bigtextpaint.setColor(Color.WHITE);
			long when = we.getWhen();
			double dist=we.getDistance();
			String whenstr;
			String whentempl;
			// Log.i("fplan","When: "+when);
			if (dist>=0)
			{
				whenstr = fmttime((int)when);
				whentempl="T:22:22";
			}
			else
			{
				if (when<3600*24*10)
				{
					whenstr="Skipped";
				}
				else
				{
					Date d=new Date(when);
					SimpleDateFormat t=new SimpleDateFormat();
					whenstr="Passed "+formatter.format(d)+"Z";
				}			
				whentempl="Passed 222222Z";				
			}

			/*
			 * #error: Move position into base class of WarningEvent #Then move
			 * calculation of distance also to baseclass. #don't store time and
			 * distance, just store position. #Let moving map calculate time,
			 * based on distance (or even based on position?)
			 */

			String[] details;
			if (extrainfo)
				details = we.getExtraDetails();
			else
				details = we.getDetails();

			int actuallines = details.length;
			Log.i("fplan", "details.length 1 : " + details.length);
			if (extrainfolineoffset >= actuallines)
				extrainfolineoffset = actuallines - 1;
			if (extrainfolineoffset < 0)
				extrainfolineoffset = 0;

			actuallines = actuallines - extrainfolineoffset;
			if (actuallines > maxlines)
				actuallines = maxlines;
			if (actuallines < 0)
				actuallines = 0;
			int page = 1 + extrainfolineoffset / maxlines;
			int totpage = 1 + details.length / maxlines;
			actuallines += 1;
			int y1 = (int) (y - tsy * (actuallines - 1)) - 2;

			RectF r = new RectF(0, y - tsy * actuallines - 4, right, bottom);
			canvas.drawRect(r, backgroundpaint);
			if (dist>=0)
				addTextIfFits(canvas, "1222.2nm", r,
					String.format("%.1fnm", dist), y1, bigtextpaint);
			// canvas.drawText(String.format("%.1fnm",we.getDistance()),
			// 2,y1,bigtextpaint);
			addTextIfFits(canvas, whentempl, r, whenstr, y1, bigtextpaint);
			// canvas.drawText(String.format("T:%s",whenstr),
			// 70,y1,bigtextpaint);
			addTextIfFits(canvas, null, r, we.getTitle(), y1, bigtextpaint);
			// canvas.drawText(we.getTitle(), 140,y1,bigtextpaint);
			for (int i = 0; i < actuallines - 1; ++i) {
				Log.i("fplan", "Actuallines:" + actuallines + " offset: "
						+ extrainfolineoffset + " details length:"
						+ details.length);
				if (extrainfolineoffset + i < details.length)
					canvas.drawText(details[extrainfolineoffset + i], 2, y1
							+ tsy + i * tsy, bigtextpaint);
			}
			if (tripstate.hasLeft()) {
				canvas.drawLine((int) (0 + 2.8f * x_dpmm),
						(int) (bottom - 0.7f * y_dpmm),
						(int) (0 + 2.0f * x_dpmm),
						(int) (bottom - 1.5f * y_dpmm), thinlinepaint);
				canvas.drawLine((int) (0 + 2.0f * x_dpmm),
						(int) (bottom - 1.5f * y_dpmm),
						(int) (0 + 2.8f * x_dpmm),
						(int) (bottom - 2.3f * y_dpmm), thinlinepaint);
			} else {
				canvas.drawLine((int) (0 + 3.25f * x_dpmm),
						(int) (bottom - 0.7f * y_dpmm),
						(int) (0 + 1.75f * x_dpmm),
						(int) (bottom - 2.3f * y_dpmm), thinlinepaint);
				canvas.drawLine((int) (0 + 3.25f * x_dpmm),
						(int) (bottom - 2.3f * y_dpmm),
						(int) (0 + 1.75f * x_dpmm),
						(int) (bottom - 0.7f * y_dpmm), thinlinepaint);

			}
			canvas.drawRect((int) (0.2f * x_dpmm),
					(int) (bottom - 2.8f * y_dpmm), (int) (4.8f * x_dpmm),
					(int) (bottom - 0.2f * y_dpmm), thinlinepaint);
			float off = right - (5.0f * x_dpmm);
			if (tripstate.hasRight()) {
				canvas.drawLine((int) (0 + 2.0f * x_dpmm + off),
						(int) (bottom - 0.7f * y_dpmm),
						(int) (0 + 2.8f * x_dpmm + off),
						(int) (bottom - 1.5f * y_dpmm), thinlinepaint);
				canvas.drawLine((int) (0 + 2.8f * x_dpmm + off),
						(int) (bottom - 1.5f * y_dpmm),
						(int) (0 + 2.0f * x_dpmm + off),
						(int) (bottom - 2.3f * y_dpmm), thinlinepaint);
				canvas.drawRect((int) (0.2f * x_dpmm + off),
						(int) (bottom - 2.8f * y_dpmm),
						(int) (4.8f * x_dpmm + off),
						(int) (bottom - 0.2f * y_dpmm), thinlinepaint);
			}
			{
				canvas.drawRect((int) (5.2f * x_dpmm),
						(int) (bottom - 2.8f * y_dpmm),
						(int) (-0.2f * x_dpmm + off),
						(int) (bottom - 0.2f * y_dpmm), thinlinepaint);
				String t;
				if (extrainfo) {
					if (totpage > 1)
						t = String.format("Freq. %d of %d", page, totpage);
					else
						t = "Toggle Freq. Info (on)";
				} else {
					t = "Toggle Freq. Info (off)";
				}
				Rect tbounds = new Rect();
				textpaint.getTextBounds(t, 0, t.length(), tbounds);
				canvas.drawText(t, (right - left) * 0.5f - tbounds.width() / 2,
						(int) (bottom - 1.0 * y_dpmm), textpaint);
			}
			// canvas.drawText(String.format("%03.0f°",lastpos.getBearing()),
			// 50, y, bigtextpaint);
			// canvas.drawText(String.format("%.0fkt",lastpos.getSpeed()*3.6/1.852),150,y,textpaint);

		}

		linepaint.setColor(Color.RED);
		float y = bigtextpaint.getTextSize();
		bigtextpaint.setColor(Color.WHITE);
		RectF r = new RectF(0, 0, right, y + 2);
		canvas.drawRect(r, backgroundpaint);
		addTextIfFits(canvas, "222°.", r,
				String.format("%03.0f°", lastpos.getBearing()), y, bigtextpaint);
		addTextIfFits(canvas, "222kt", r,
				String.format("%.0fkt", lastpos.getSpeed() * 3.6 / 1.852), y,
				bigtextpaint);

		// canvas.drawText(String.format("%03.0f°",lastpos.getBearing()), 40, y,
		// bigtextpaint);
		// canvas.drawText(String.format("%.0fkt",lastpos.getSpeed()*3.6/1.852),100,y,bigtextpaint);
		int td = tripstate.get_time_to_destination();
		// canvas.drawText(fmttime(td),150,y,bigtextpaint);
		addTextIfFits(canvas, "122:22", r, MapDrawer.fmttime(td), y,
				bigtextpaint);
		if (havefix) // if significantly after 1970-0-01
		{
			Date d = new Date(lastpos.getTime());
			// canvas.drawText("FIX:"+formatter.format(d)+"Z",220,y,bigtextpaint);
			addTextIfFits(canvas, "FIX:222222Z", r,
					"FIX:" + formatter.format(d) + "Z", y, bigtextpaint);
		} else {
			// canvas.drawText("NOFIX",220,y,bigtextpaint);
			addTextIfFits(canvas, "FIX:222222Z", r, "NOFIX", y, bigtextpaint);
		}
		// /canvas.drawText(String.format("Z%d",zoomlevel), 0,y,bigtextpaint);
		addTextIfFits(canvas, "Z13", r, String.format("Z%d", zoomlevel), y,
				bigtextpaint);

		if (isDragging) {
			float h = bigtextpaint.getTextSize();
			y += h;
			final Rect tr1 = new Rect();
			thinlinepaint.setColor(Color.WHITE);

			String text = "Center";
			bigtextpaint.getTextBounds(text, 0, text.length(), tr1);
			tr1.bottom = (int) (tr1.top + h);
			tr1.offsetTo((int) (right - tr1.width() - h), (int) y);
			MapDrawer.grow(tr1, (int) (0.4f * h));
			canvas.drawRect(tr1, backgroundpaint);
			canvas.drawRect(tr1, thinlinepaint);

			canvas.drawText(text, tr1.left + 0.4f * h, tr1.bottom - 0.4f * h,
					bigtextpaint);
			clickables.add(new GuiSituation.Clickable() {
				@Override
				public Rect getRect() {
					return tr1;
				}

				@Override
				public void onClick() {
					gui.doCenterDragging();
				}
			});
			int edge = tr1.left;

			text = "Set North Up";
			final Rect tr2 = new Rect();
			bigtextpaint.getTextBounds(text, 0, text.length(), tr2);
			tr2.bottom = (int) (tr2.top + h);
			tr2.offsetTo((int) (h), (int) y);
			MapDrawer.grow(tr2, (int) (0.4f * h));
			if (tr2.right < edge) {
				canvas.drawRect(tr2, backgroundpaint);
				canvas.drawRect(tr2, thinlinepaint);

				canvas.drawText(text, tr2.left + 0.4f * h, tr2.bottom - 0.4f
						* h, bigtextpaint);
				clickables.add(new GuiSituation.Clickable() {
					@Override
					public Rect getRect() {
						return tr2;
					}

					@Override
					public void onClick() {
						gui.onNorthUp();
					}
				});
			}

		} else {
			if (download_status != null && !download_status.equals("")) {
				float y2 = (y + bigtextpaint.getTextSize() * 1.1f);
				canvas.drawRect(0, y + 2, right, y2, backgroundpaint);
				canvas.drawText("Download:" + download_status, left + 3, y2,
						bigtextpaint);
			}
		}

		return res;
	}

	public int getNumInfoLines(int ysize) {
		float tsy = bigtextpaint.getTextSize() + 2;
		final int maxlines = (int) ((0.5f * (ysize)) / tsy) - 1;
		if (maxlines <= 1)
			throw new RuntimeException(
					"The screen is too small for this program");
		Log.i("fplan", "numinfolines:" + maxlines);
		return maxlines;
	}

	private boolean tileOnScreen(float cx, float cy, Transform tf) {
		float maxdiag = 363;
		if (cx + maxdiag < left)
			return false;
		if (cx - maxdiag > right)
			return false;
		if (cy + maxdiag < top)
			return false;
		if (cy - maxdiag > bottom)
			return false;

		Vector base = new Vector(cx, cy);
		Vector v = new Vector();
		int sidex = 0;
		int sidey = 0;
		for (int j = 0; j < 2; ++j) {
			for (int i = 0; i < 2; ++i) {
				v.x = 256 * i;
				v.y = 256 * j;
				Vector r = v.rot(tf.hdgrad);
				r.x += base.x;
				r.y += base.y;
				int cursidex = 0;
				int cursidey = 0;
				if (v.x < left)
					cursidex = -1;
				if (v.x > right)
					cursidex = 1;
				if (v.y < top)
					cursidey = -1;
				if (v.y > bottom)
					cursidey = 1;
				if (cursidex == 0 && cursidey == 0)
					return true;
				if (i == 0 && j == 0) {
					sidex = cursidex;
					sidey = cursidey;
				}
				if (cursidex == 0 || cursidex != sidex)
					sidex = 0;
				if (cursidey == 0 || cursidey != sidey)
					sidey = 0;
			}
		}
		if (sidex != 0 || sidey != 0)
			return false;
		return true;

	}

	void addTextIfFits(Canvas canvas, String sizetext, RectF r,
			String realtext, float y, Paint tp) {
		if (sizetext == null) {
			canvas.drawText(realtext, r.left, y, tp);
			r.left = r.right + 1;
		} else {
			Rect rect = new Rect();
			tp.getTextBounds(sizetext, 0, sizetext.length(), rect);
			if (r.left + (rect.right - rect.left) < r.right) {
				canvas.drawText(realtext, r.left, y, tp);
				r.left += (rect.right - rect.left) + 1.0f * x_dpmm;
			} else {
				r.left = r.right + 1; // definitely out of space now!
			}
		}

	}

	static void grow(Rect r, int howmuch) {
		r.left -= howmuch;
		r.right += howmuch;
		r.top -= howmuch;
		r.bottom += howmuch;
	}

	static String fmttime(int when) {
		if (when == 0 || when > 3600 * 24 * 10)
			return "--:--";
		return String.format("%d:%02d", when / 60, when % 60);
	}

}