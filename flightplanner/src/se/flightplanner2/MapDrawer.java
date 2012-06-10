package se.flightplanner2;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.TimeZone;

import se.flightplanner2.ElevBitmapCache.BMResult;
import se.flightplanner2.GetMapBitmap.BitmapRes;
import se.flightplanner2.GlobalGetElev.GetElevation;
import se.flightplanner2.GuiSituation.Clickable;
import se.flightplanner2.Project.LatLon;
import se.flightplanner2.Project.Merc;
import se.flightplanner2.Project.iMerc;
import se.flightplanner2.SigPoint.Runway;
import se.flightplanner2.TripData.Waypoint;
import se.flightplanner2.TripState.BugInfo;
import se.flightplanner2.TripState.NextLanding;
import se.flightplanner2.vector.BoundingBox;
import se.flightplanner2.vector.ConvexPolygon;
import se.flightplanner2.vector.Vector;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Canvas.EdgeType;
import android.graphics.Canvas.VertexMode;
import android.graphics.BitmapShader;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Shader.TileMode;
import android.graphics.Typeface;
import android.graphics.Paint.Style;
import android.location.Location;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;

public class MapDrawer {

	private Paint neutralpaint;
	private Paint bigtextpaint;
	private Paint mediumtextpaint;
	private Paint smalltextpaint;
	private Paint hugetextpaint;
	private Paint ahtextpaint;
	private Paint linepaint;
	private Paint thinlinepaint;
	private Paint trippaint;
	private Paint widetrippaint;
	private Paint arrowpaint;
	private Paint backgroundpaint;
	private Paint blackgroundpaint;
	private Paint textpaint;
	private float x_dpmm, y_dpmm;
	private SimpleDateFormat formatter = new SimpleDateFormat("HHmmss");
	private SimpleDateFormat formatter2 = new SimpleDateFormat("HH:mm");
	private String zoom_in_text = null;
	private String zoom_out_text = null;
	private boolean zoom_buttons;

	private class CacheKey {
		String text;
		int color;

		public CacheKey(String t, int col) {
			text = t;
			color = col;
		}

		@Override
		public boolean equals(Object o) {
			CacheKey ko = (CacheKey) o;
			return ko.text.equals(text) && color == ko.color;
		}

		@Override
		public int hashCode() {
			return text.hashCode() + color;
		}
	}

	HashMap<CacheKey, Bitmap> cached = new HashMap<MapDrawer.CacheKey, Bitmap>();
	HashMap<CacheKey, Bitmap> used = new HashMap<MapDrawer.CacheKey, Bitmap>();
	private float bigtextsize;
	private float mediumtextsize;

	public MapDrawer(float px_dpmm, float py_dpmm, float screen_size_x,
			float screen_size_y) {
		formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
		formatter2.setTimeZone(TimeZone.getTimeZone("UTC"));
		
		this.x_dpmm = px_dpmm;
		this.y_dpmm = py_dpmm;
		float factor = (float) Math
				.sqrt(Math.max(screen_size_x, screen_size_y) / 92.0f);
		if (factor < 1.0f)
			factor = 1.0f;
		Log.i("fplan.factor", "Factor: " + factor + " screen size x; "
				+ screen_size_x + " screen size Y: " + screen_size_y);
		x_dpmm *= factor;
		y_dpmm *= factor;
		int foreground = Color.WHITE;
		float textsize = y_dpmm * 2.4f;
		float smalltextsize = y_dpmm * 2.2f;
		bigtextsize = y_dpmm * 3.4f; // 6.5 mm text size
		mediumtextsize = y_dpmm * 2.9f; // 6.5 mm text size
		float hugetextsize = y_dpmm * 3.4f; // 6.5 mm text size

		textpaint = new Paint();
		textpaint.setAntiAlias(true);
		textpaint.setColor(foreground);
		textpaint.setTextSize(textsize);
		textpaint.setTypeface(Typeface.create(Typeface.SANS_SERIF,
				Typeface.NORMAL));

		bigtextpaint = new Paint();
		bigtextpaint.setAntiAlias(true);
		bigtextpaint.setColor(foreground);
		bigtextpaint.setTextSize(bigtextsize);
		bigtextpaint.setTypeface(Typeface.create(Typeface.SANS_SERIF,
				Typeface.NORMAL));

		mediumtextpaint = new Paint();
		mediumtextpaint.setAntiAlias(true);
		mediumtextpaint.setColor(Color.WHITE);
		mediumtextpaint.setTextSize(mediumtextsize);
		mediumtextpaint.setTypeface(Typeface.create(Typeface.SANS_SERIF,
				Typeface.NORMAL));

		smalltextpaint = new Paint();
		smalltextpaint.setAntiAlias(true);
		smalltextpaint.setColor(foreground);
		smalltextpaint.setTextSize(smalltextsize);
		smalltextpaint.setTypeface(Typeface.create(Typeface.SANS_SERIF,
				Typeface.NORMAL));

		hugetextpaint = new Paint();
		hugetextpaint.setAntiAlias(true);
		hugetextpaint.setColor(foreground);
		hugetextpaint.setTextSize(hugetextsize);
		hugetextpaint.setTypeface(Typeface.create(Typeface.SANS_SERIF,
				Typeface.NORMAL));

		ahtextpaint = new Paint();
		ahtextpaint.setAntiAlias(true);
		ahtextpaint.setColor(Color.rgb(255, 190, 190));
		ahtextpaint.setTextSize(15);
		ahtextpaint.setTypeface(Typeface.create(Typeface.SANS_SERIF,
				Typeface.NORMAL));

		linepaint = new Paint();
		linepaint.setAntiAlias(false);
		linepaint.setStrokeWidth(1f * x_dpmm);
		linepaint.setColor(Color.RED);
		linepaint.setStrokeCap(Paint.Cap.ROUND);

		neutralpaint = new Paint();
		thinlinepaint = new Paint();
		thinlinepaint.setAntiAlias(false);
		thinlinepaint.setStrokeWidth(0.5f * x_dpmm);
		thinlinepaint.setStyle(Style.STROKE);
		thinlinepaint.setColor(Color.RED);
		thinlinepaint.setStrokeCap(Paint.Cap.ROUND);

		trippaint = new Paint();
		trippaint.setAntiAlias(false);
		trippaint.setStrokeWidth(0.5f * x_dpmm);
		trippaint.setARGB(0xff, 0xff, 0xff, 0xff);
		trippaint.setColor(Color.rgb(0xff, 0xff, 0xff));
		trippaint.setStrokeCap(Paint.Cap.ROUND);

		widetrippaint = new Paint();
		widetrippaint.setAntiAlias(false);
		widetrippaint.setStrokeWidth(0.75f * x_dpmm);
		widetrippaint.setARGB(0x70, 0x80, 0x80, 0xff);
		widetrippaint.setStrokeCap(Paint.Cap.ROUND);

		backgroundpaint = new Paint();
		backgroundpaint.setStyle(Style.FILL);
		backgroundpaint.setARGB(0xa0, 0, 0, 0);
		blackgroundpaint = new Paint();
		blackgroundpaint.setStyle(Style.FILL);
		blackgroundpaint.setColor(Color.BLACK);

		arrowpaint = new Paint();
		arrowpaint.setAntiAlias(false);
		arrowpaint.setStyle(Style.FILL);
		arrowpaint.setStrokeWidth(1.5f * x_dpmm);
		arrowpaint.setColor(Color.WHITE);
		arrowpaint.setStrokeCap(Paint.Cap.ROUND);
	}

	private void renderText(Canvas mcanvas, Vector p, String text,
			DeclutterTree declutter, int color) {

		// if (p!=null) return;
		int bm_off_x = (int) (3.0f * x_dpmm);
		int bm_off_y = (int) (3.0f * x_dpmm);
		CacheKey ck = new CacheKey(text, color);
		{
			Bitmap bm = cached.get(ck);
			if (bm != null) {
				used.put(ck, bm);
				Rect rect = new Rect();
				rect.right = bm.getWidth();
				rect.bottom = bm.getHeight();
				rect.offset((int) p.x, (int) p.y);
				if (declutter == null || declutter.checkAndAdd(rect)) {
					// Log.i("fplan.fps","Drawing cached bm in: "+rect);
					mcanvas.drawBitmap(bm, (float) p.x - bm_off_x, (float) p.y
							- bm_off_y, neutralpaint);
				}
				return;
			}
		}

		Rect rect = new Rect();
		textpaint.getTextBounds(text, 0, text.length(), rect);
		int xadj = -rect.left;
		int yadj = -rect.top;
		rect.left -= 2;
		rect.right += 2;
		rect.bottom += 2;
		rect.top -= 2;

		Bitmap bm = Bitmap.createBitmap(rect.width() + bm_off_x, rect.height()
				+ bm_off_y, Bitmap.Config.ARGB_4444);
		Canvas canvas = new Canvas(bm);
		rect.offset(xadj + bm_off_x, yadj + bm_off_y);

		Rect globrect = new Rect(rect);
		globrect.offset((int) p.x, (int) p.y);
		RectF globrectf = new RectF(globrect);
		if (!mcanvas.quickReject(globrectf, Canvas.EdgeType.BW)
				&& (declutter == null || declutter.checkAndAdd(globrect))) {
			canvas.drawRect(rect, backgroundpaint);
			textpaint.setColor(color);
			canvas.drawText(text, rect.left + xadj + 2, rect.top + yadj + 2,
					textpaint);
			linepaint.setStrokeWidth(1.2f * x_dpmm);
			linepaint.setColor(Color.BLACK);
			canvas.drawPoint(bm_off_x, bm_off_y, linepaint);

			linepaint.setColor(color);
			linepaint.setStrokeWidth(0.6f * x_dpmm);
			canvas.drawPoint(bm_off_x, bm_off_y, linepaint);

			cached.put(ck, bm);
			used.put(ck, bm);
			// Log.i("fplan.fps","Caching bm in: "+rect);
			// /canvas.
			mcanvas.drawBitmap(bm, (float) p.x - bm_off_x, (float) p.y
					- bm_off_y, neutralpaint);

		} else {
			linepaint.setColor(color);
			linepaint.setStrokeWidth(0.6f * x_dpmm);
			mcanvas.drawPoint((float) p.x, (float) p.y, linepaint);

		}

	}


	long redraw_start;
	View redraw_view;
	Handler handler = new Handler();
	Runnable schedule_redraw = new Runnable() {
		@Override
		public void run() {
			redraw_view.invalidate();
		}
	};
	
	static public class Circle
	{
		float x,y,radius;
		float dx,dy,dradius;
		int steps_left;
		int steps_done;
	}
	Circle circle;
	Runnable animate_circle = new Runnable() {
		@Override
		public void run() {
			
			if (circle==null)
				return;
			circle.x+=circle.dx;
			circle.y+=circle.dy;
			circle.radius-=circle.dradius;
			circle.steps_left-=1;
			redraw_view.invalidate();
			//Log.i("fplan.circle","Pos: "+circle.x+","+circle.y+"  radius: "+circle.radius);
			int eff_steps_left=Math.min(circle.steps_left,40-circle.steps_done);
			if (eff_steps_left>=0)
			{
				if (eff_steps_left==0)
					handler.postDelayed(this,1000);
				else
					handler.postDelayed(this,100);
			}
			else
				circle=null;
		}
	};

	boolean onlyWithin(long ms, boolean dragging) {
		if (!dragging)
			return true;
		long en = SystemClock.elapsedRealtime() - redraw_start;
		if (en < ms)
			return true;
		handler.removeCallbacks(schedule_redraw);
		handler.postDelayed(schedule_redraw, 300);
		return false;
	}
	public void stop()
	{
		if (schedule_redraw!=null)
			handler.removeCallbacks(schedule_redraw);
		if (animate_circle!=null)
			handler.removeCallbacks(animate_circle);
		circle=null;
		
	}

	private int left;
	private int right;
	private int top;
	private int bottom;
	private int last_zoomlevel;
	private double last_mypos_y;
	private double last_mypos_x;
	
	
	public void draw_actual_map(TripData tripdata, TripState tripstate,
			AirspaceLookup lookup, Canvas canvas, Rect screen_extent,
			Location lastpos, GetMapBitmap bitmaps, final GuiSituation gui,
			long last_real_position, String download_status,
			InformationPanel panel, View view, String[] prox_warning,
			int gps_sat_cnt, int gps_sat_fix_cnt, ElevBitmapCache elevbmc,
			boolean terrwarn,int battery,boolean charging,final AdChartLoader adloader, 
			String[] chosen_ad_maps, int chosen_ad_map_i,long chosen_ad_map_when, int last_cvr_amp) {

		
		
		long bef = SystemClock.elapsedRealtime();
		long now=bef;

		redraw_start = bef;
		redraw_view = view;
		int elev_ft = (int) (lastpos.getAltitude() / 0.3048f);
		//if (Config.debugMode())
		//	;//elev_ft = 7000 - (int) ((long) ((SystemClock.elapsedRealtime() * 0.001f * 75f)) % 7000);
		
		int amsl=Integer.MAX_VALUE;								
		GetElevation gelev=GlobalGetElev.get_elev;
		if (gelev!=null)
		{
			amsl=elev_ft/*-gelev.get_elev_ft(new LatLon(lastpos))*/;
		}

		double gs_kt = lastpos.getSpeed() * 3.6 / 1.852;
		if (gs_kt < 10)
			terrwarn = false;
		boolean havefix = lastpos.getTime() > 3600 * 24 * 10 * 1000
				&& SystemClock.uptimeMillis() - last_real_position < 15000;

		elevbmc.start_frame(gui.getZoomlevel(), elev_ft);
		TransformIf tf = gui.getTransform();
		boolean extrainfo = gui.getExtraInfo();

		int extrainfolineoffset = gui.getExtrainfolineoffset();
		boolean isDragging = (gui.getDrag_center13() != null);
		boolean chartmode=gui.getChartMode();
		boolean elevonly=gui.getElevOnly();
		if (elevonly)
			elevbmc.setMode(ElevBitmapCache.Mode.EXPLORE);
		else
			elevbmc.setMode(ElevBitmapCache.Mode.WARNING);
		int zoomlevel = gui.getZoomlevel();
		if (zoomlevel > 22)
			throw new RuntimeException("zoomlevel must be <=22");
		gui.clearClickables();
		ArrayList<GuiSituation.Clickable> clickables = gui.getClickables();

		left = screen_extent.left;
		right = screen_extent.right;
		top = screen_extent.top;
		
		int lowerinset=100;
		
		bottom = screen_extent.bottom-lowerinset;
		int sizex = screen_extent.width();
		int sizey = bottom-top;

		// Project.latlon2merc(new
		// LatLon(lastpos.getLatitude(),lastpos.getLongitude()),13);
		Merc mypos = tf.getPos();
		float delta = (float) (Math.abs(last_mypos_x - mypos.x) + Math
				.abs(last_mypos_y - mypos.y));
		last_mypos_x = mypos.x;
		last_mypos_y = mypos.y;
		boolean isUserPresentlyMovingMap = gui.getFingerDown();
		if (delta < 20)
			isUserPresentlyMovingMap = false;
	
		Merc mypos13 = Project.merc2merc(tf.getPos(), zoomlevel, 13);

		Vector arrow = gui.getArrow();
		Merc screen_center = tf.screen2merc(new Vector(left+sizex / 2, top+sizey / 2));
		Merc screen_center13 = Project.merc2merc(screen_center, zoomlevel, 13);
		
		float yheader = Math.max(hugetextpaint.getTextSize() * 0.85f,bigtextsize*1.85f);

		float fivenm = (float) Project.approx_scale(mypos.y, zoomlevel, 5);
		double tennm13 = Project.approx_scale(screen_center13.y, 13, 10);
		

		double diagonal13;
		{
			int zoomgap13 = 13 - zoomlevel;
			diagonal13 = ((Math.pow(2, zoomgap13)) * (Math.sqrt((arrow.x-left) * (arrow.x-left)
					+ (arrow.y-top) * (arrow.y-top)) + 50)) + 1;
		}
		BoundingBox bb13 = new BoundingBox(screen_center13.x,
				screen_center13.y, screen_center13.x, screen_center13.y)
				.expand(diagonal13);

		BoundingBox smbb13 = new BoundingBox(mypos13.x, mypos13.y, mypos13.x,
				mypos13.y).expand(tennm13);
		canvas.save();
		canvas.clipRect(left, top, right, bottom);
		if (adloader!=null)
		{
			drawAdMap(canvas, adloader, tf, zoomlevel, screen_center);
		}
		if (bitmaps != null) {
			Rect cliprect=new Rect(left,(int) yheader,right,bottom);
			drawBitmapMap(canvas, bitmaps, elevbmc, terrwarn, adloader, tf,
					zoomlevel, sizex, sizey, isUserPresentlyMovingMap,
					screen_center,elevonly,cliprect);
		}

		elevbmc.delete_all_unused();
		elevbmc.schedule_background_tasks();
		
		

		

		ArrayList<SigPoint> major_airfields = null;
		if (onlyWithin(60, isUserPresentlyMovingMap))
			if (zoomlevel >= 9 && lookup != null) {
				linepaint.setStrokeWidth(1.0f * x_dpmm);
				major_airfields = lookup.majorAirports.findall(bb13);
				for (SigPoint sp : major_airfields) {
					if (sp.extra != null && sp.extra.runways != null) {
						for (Runway runway : sp.extra.runways) {
							Merc m1 = Project.latlon2merc(runway.ends[0].pos,
									zoomlevel);
							Merc m2 = Project.latlon2merc(runway.ends[1].pos,
									zoomlevel);
							Vector p1 = tf.merc2screen(m1);
							Vector p2 = tf.merc2screen(m2);
							// linepaint.setStrokeWidth(10);
							linepaint.setColor(Color.BLACK);
							canvas.drawLine((float) p1.x, (float) p1.y,
									(float) p2.x, (float) p2.y, linepaint);
							
						}
					}
				}
			}

		drawBaseVectorMap(tripdata, lookup, canvas, havefix, tf, isDragging,
				chartmode, zoomlevel, isUserPresentlyMovingMap, bb13, smbb13,
				major_airfields);
		
		boolean northup = false;
		if (gui != null)
			northup = gui.getnorthup();
		if (!isDragging && !northup) {
			arrowpaint.setColor(Color.argb(0x80,0,0,0));
			Path path = new Path();
			path.moveTo((int) arrow.x - 2.5f*x_dpmm, (int) arrow.y + 0.3f*x_dpmm);
			path.lineTo((int) arrow.x + 2.5f*x_dpmm, (int) arrow.y + 0.3f*x_dpmm);
			path.lineTo((int) arrow.x, (int) arrow.y - 2.25f*x_dpmm);
			path.close();
			canvas.drawPath(path, arrowpaint);
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
			canvas.drawRect((int) arrow.x - 0.5f*x_dpmm, (int) 0, (int) arrow.x + 0.5f*x_dpmm,
					(int) arrow.y, arrowpaint);
			
			
			float gradientpixels = getGradientPixels(fivenm);
			arrowpaint.setColor(Color.WHITE);
			arrowpaint.setShader(new LinearGradient(
					(float)arrow.x, (float)arrow.y, 
					(float)(arrow.x+1f*x_dpmm), (float)(arrow.y), 
					Color.rgb(0xff,0x00,0x00),Color.rgb(0xff,0xff,0xd0),  Shader.TileMode.MIRROR));
	
			path = new Path();
			path.moveTo((int) arrow.x - 2f*x_dpmm, (int) arrow.y);
			path.lineTo((int) arrow.x + 2f*x_dpmm, (int) arrow.y);
			path.lineTo((int) arrow.x, (int) arrow.y - 2f*x_dpmm);
			path.close();
			canvas.drawPath(path, arrowpaint);
			arrowpaint.setShader(new LinearGradient((float)arrow.x, (float)arrow.y, (float)arrow.x, (float)(arrow.y-gradientpixels), 
					Color.rgb(0xff,0x00,0x00),Color.rgb(0xff,0xff,0xd0),  Shader.TileMode.REPEAT));			
			canvas.drawRect((int) arrow.x, (int) arrow.y - fivenm + 1,
					(int) arrow.x + 5, (int) arrow.y - fivenm + 3, arrowpaint);
			canvas.drawRect((int) arrow.x - 9, (int) arrow.y - fivemin + 1,
					(int) arrow.x, (int) arrow.y - fivemin + 3, arrowpaint);
			canvas.drawRect((int) arrow.x - 0.25f*x_dpmm, (int) 0, (int) arrow.x + 0.25f*x_dpmm,
					(int) arrow.y, arrowpaint);
			arrowpaint.setShader(null);
			arrowpaint.setColor(Color.WHITE);
		}

		else {
			if (lastpos != null) {
				Merc pos = Project.latlon2merc(new LatLon(
						lastpos.getLatitude(), lastpos.getLongitude()),
						zoomlevel);
				Merc dest;
				if (lastpos != null && lastpos.hasBearing()) {
					float hdg = lastpos.getBearing();
					Vector d = new Vector(0, -(bottom-top));
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
				arrowpaint.setStrokeWidth(0.8f*x_dpmm);
				Path path = new Path();
				path.moveTo((int) (screenpos.x + 2.5f*x_dpmm * left.x - 0.5f*x_dpmm * forward.x),
						(int) (screenpos.y + 2.5f*x_dpmm * left.y - 0.5f*x_dpmm * forward.y));
				path.lineTo((int) (screenpos.x + 2.5f*x_dpmm * right.x - 0.5f*x_dpmm * forward.x),
						(int) (screenpos.y + 2.5f*x_dpmm * right.y - 0.5f*x_dpmm * forward.y));
				path.lineTo((int) (screenpos.x + 2.5f*x_dpmm * forward.x),
						(int) (screenpos.y + 2.5f*x_dpmm * forward.y));
				path.close();
				canvas.drawPath(path, arrowpaint);

				canvas.drawLine((float) screenpos.x, (float) screenpos.y,
						(float) screenpos2.x, (float) screenpos2.y, arrowpaint);
				float gradientpixels = getGradientPixels(fivenm);
				arrowpaint.setShader(new LinearGradient(
						(float)screenpos.x, (float)screenpos.y, 
						(float)(screenpos.x+1f*x_dpmm*forward.y), (float)(screenpos.y-1f*x_dpmm*forward.x), 
						Color.rgb(0xff,0x00,0x00),Color.rgb(0xff,0xff,0xd0), Shader.TileMode.MIRROR));
				
				path = new Path();
				path.moveTo((int) (screenpos.x + 2f*x_dpmm * left.x),
						(int) (screenpos.y + 2f*x_dpmm * left.y));
				path.lineTo((int) (screenpos.x + 2f*x_dpmm * right.x),
						(int) (screenpos.y + 2f*x_dpmm * right.y));
				path.lineTo((int) (screenpos.x + 2f*x_dpmm * forward.x),
						(int) (screenpos.y + 2f*x_dpmm * forward.y));
				path.close();
				canvas.drawPath(path, arrowpaint);
				arrowpaint.setShader(new LinearGradient(
						(float)screenpos.x, (float)screenpos.y, 
						(float)(screenpos.x+gradientpixels*forward.x), (float)(screenpos.y+gradientpixels*forward.y), 
						Color.rgb(0xff,0x00,0x00),Color.rgb(0xff,0xff,0xd0), Shader.TileMode.REPEAT));

				arrowpaint.setStrokeWidth(0.4f*x_dpmm);
				canvas.drawLine((float) screenpos.x, (float) screenpos.y,
						(float) screenpos2.x, (float) screenpos2.y, arrowpaint);
				arrowpaint.setShader(null);
				arrowpaint.setColor(Color.WHITE);
				arrowpaint.setStrokeWidth(2);

			}

		}
		if (circle!=null)
		{
			linepaint.setStrokeWidth(Math.max(circle.radius/20,x_dpmm));
			linepaint.setColor(Color.RED);
			linepaint.setStyle(Style.STROKE);
			Vector sp = tf.merc2screen(new Merc(circle.x,circle.y));			
			canvas.drawCircle((float)sp.x,(float)sp.y,circle.radius,linepaint);
		}
		
		canvas.restore();
		
		{
			canvas.save();
			canvas.translate(0, bottom);
			canvas.clipRect(left,0,right,lowerinset);
			Merc m1=tf.screen2merc(gui.getArrow());
			Merc m2=tf.screen2merc(new Vector((right-left)*0.5,top));
			float howfar=(float)Project.exacter_distance(Project.merc2latlon(m1, zoomlevel),Project.merc2latlon(m2, zoomlevel)); 
			drawInset(canvas,lastpos,right-left,lowerinset,howfar);
			canvas.restore();
		}
		
		top = screen_extent.top;
		bottom = screen_extent.bottom;
		sizey = bottom-top;
		

		InformationPanel we = panel;
		if (we != null) {
			drawInfoPanel(canvas, gui, tf, extrainfo, extrainfolineoffset,
					zoomlevel, clickables, we);

		} else {
			float y = bottom - (bigtextpaint.getTextSize() * 1.5f + 2);

			final Rect tr3 = drawButton(canvas, right, y, "Wpts", -1, left,
					right, false);
			if (tr3 != null) {
				clickables.add(new GuiSituation.Clickable() {
					@Override
					public Rect getRect() {
						return tr3;
					}

					@Override
					public void onClick() {
						gui.onShowWaypoints();
					}
				});
				int rightedge = tr3.left - 5;
				if (zoom_in_text == null) {
					Rect tr1 = drawButton(canvas, left, y, "Zoom +", 1, left,
							rightedge, true);

					int edge = (tr1 != null) ? tr1.right + 5 : right;
					Rect tr2 = drawButton(canvas, edge, y, "Zoom -", 1, edge,
							rightedge, true);
					if (tr2 == null) {
						tr1 = drawButton(canvas, left, y, "+", 1, left,
								rightedge, true);
						edge = tr1.right + 5;
						tr2 = drawButton(canvas, edge, y, "-", 1, edge,
								rightedge, true);
						if (tr2 != null) {
							zoom_buttons = true;
							zoom_in_text = "+";
							zoom_out_text = "-";
						} else {
							zoom_buttons = false;
							zoom_in_text = "none";
							zoom_out_text = "none";
						}
					} else {
						zoom_buttons = true;
						zoom_in_text = "Zoom +";
						zoom_out_text = "Zoom -";
					}

				}
				if (zoom_buttons) {
					final Rect tr1 = drawButton(canvas, left, y, zoom_in_text,
							1, left, rightedge, false);
					clickables.add(new GuiSituation.Clickable() {
						@Override
						public Rect getRect() {
							return tr1;
						}

						@Override
						public void onClick() {
							gui.changeZoom(+1);
						}
					});
					int edge = tr1.right + 5;

					final Rect tr2 = drawButton(canvas, edge, y, zoom_out_text,
							1, edge, rightedge, false);
					clickables.add(new GuiSituation.Clickable() {
						@Override
						public Rect getRect() {
							return tr2;
						}

						@Override
						public void onClick() {
							gui.changeZoom(-1);
						}
					});
				}
			}
		}

		linepaint.setColor(Color.RED);
		float yledge =bigtextpaint.getTextSize() * 0.85f;
		bigtextpaint.setColor(Color.WHITE);
		RectF r = new RectF(0, 0, right, yheader + 0.7f*y_dpmm);
		RectF rledge=new RectF(r);
		canvas.drawRect(r, blackgroundpaint);
		addTextIfFits(canvas, "hdg:", "223-",r, "hdg:",String.format("%03.0f°", lastpos.getBearing()),
				yheader, smalltextpaint,hugetextpaint);

		addTextIfFits(canvas, "gs:","222", r, "gs:",String.format("%.0f", gs_kt),
				yheader, smalltextpaint,hugetextpaint);
		

		// canvas.drawText(String.format("%03.0f°",lastpos.getBearing()), 40, y,
		// bigtextpaint);
		// canvas.drawText(String.format("%.0fkt",lastpos.getSpeed()*3.6/1.852),100,y,bigtextpaint);
		if (tripstate!=null)
		{
			NextLanding nextlanding=tripstate.getNextLanding();
			if (nextlanding!=null)
			{
				//int td = tripstate.get_time_to_destination();
				// canvas.drawText(fmttime(td),150,y,bigtextpaint);asdf
				addTextIfFits(canvas, "ETA:", "22:22", rledge, "ETA:",formatter2.format(nextlanding.when),  yledge, smalltextpaint,bigtextpaint);
				//Log.i("fplan.delay","nextlanding: "+nextlanding.where+" when: "+nextlanding.planned);
				int delay=(int)((nextlanding.when.getTime()-nextlanding.planned.getTime())/(60*1000l));
				addTextIfFits(canvas, (delay>0) ? "delay:" : "ahead:", "22h:22m", rledge, (delay>0) ? "delay:" : "ahead:" ,fmtdelay(delay),  yledge, smalltextpaint,bigtextpaint);
			}
		}
		
		if (charging)
			bigtextpaint.setColor(Color.rgb(128,255,128));
		else
			bigtextpaint.setColor(Color.rgb(255,128,128));
		
		String curtime_s=formatter2.format(new Date());
		addTextIfFits(canvas, "BAT:", "222%", rledge, (!charging) ? "BAT:" : "bat:" ,(battery<0) ? "--" : ""+battery+"%",  yledge, smalltextpaint,bigtextpaint);
		bigtextpaint.setColor(Color.WHITE);
		addTextIfFits(canvas, "", "22:22", rledge, "" ,curtime_s,  yledge, smalltextpaint,bigtextpaint);
		
		String amsl_txt="--";
		if (amsl>-9999)
			amsl_txt=""+amsl;
		addTextIfFits(canvas, "ETA:", "22:22", r, "msl:",amsl_txt,  yheader, smalltextpaint,bigtextpaint);
		
		if (havefix) // if significantly after 1970-0-01
		{
			addTextIfFits(canvas, "GPS:","200%", 
					r, "GPS:",String.format("%d%%",gps_sat_fix_cnt/2), yheader, smalltextpaint, bigtextpaint,false, false);
		} else {
			bigtextpaint.setColor(Color.RED);
			addTextIfFits(canvas, "GPS:","200%", 
					r, "GPS:",String.format("%d%%",gps_sat_fix_cnt/2), yheader, smalltextpaint, bigtextpaint,true, false);
			bigtextpaint.setColor(Color.WHITE);
		}
		
		if (last_cvr_amp!=0)
		{
			Log.i("fplan.cvr","Last amp: "+last_cvr_amp);
			float dist=((float)last_cvr_amp)/32768f;
			if (dist>1f) dist=1;
			if (dist<0.1f) dist=0.1f;
			float cvrh1=r.bottom;
			float cvrh2=r.bottom-r.bottom*dist;
			if (dist<0.12f)
				linepaint.setColor(Color.BLUE);
			else if (dist<0.9)
				linepaint.setColor(Color.GREEN);
			else
				linepaint.setColor(Color.RED);			
			canvas.drawLine(right, cvrh1, right, cvrh2, linepaint);
			linepaint.setColor(Color.WHITE);
		}
		
		// /canvas.drawText(String.format("Z%d",zoomlevel), 0,y,bigtextpaint);
		if (Config.debugMode())
			addTextIfFits(canvas, "Z13", "", r, String.format("Z%d", zoomlevel),
				"", yheader, bigtextpaint, smalltextpaint);
		
		if (havefix && !isDragging)
		{
			drawBug(tripstate, canvas, lastpos, sizex, sizey, yheader);
			
		}
		

		int airspace_button_space_left = left;
		int airspace_button_space_right = right;
		if (!isDragging || gui.getChartMode())
		{
			float h = smalltextpaint.getTextSize();
			float topbutton_y = yheader + h;

			String text = "View";
			final Rect tr1 = drawButton(canvas, right, topbutton_y, text, -1,
					0, right, false);
			airspace_button_space_right = tr1.left - 4;
			clickables.add(new GuiSituation.Clickable() {
				@Override
				public Rect getRect() {
					return tr1;
				}

				@Override
				public void onClick() {
					gui.toggle_map();
				}
			});
			
			if (chosen_ad_maps!=null && now-chosen_ad_map_when<4500)
			{
				float cury=topbutton_y+y_dpmm*5.0f;
				linepaint.setStrokeWidth(0.5f*x_dpmm);
				linepaint.setColor(Color.WHITE);
				bigtextpaint.setTextSize(2.5f*y_dpmm);
				int start=Math.max(chosen_ad_map_i-10,0);
				//boolean sec2=now-chosen_ad_map_when>2000;
				for(int ii=start;ii<start+Math.min(chosen_ad_maps.length,8);++ii)
				{
					int i=ii%chosen_ad_maps.length;
					
					String chosen_ad_map=chosen_ad_maps[i];
					//Log.i("fplan.chosen","Drawimg chosen ad map:"+chosen_ad_map);
					//smalltextpaint.setTextSize(bigtextsize);
					Rect rect=new Rect();
					bigtextpaint.getTextBounds(chosen_ad_map, 0, chosen_ad_map.length(), rect);
					//float 
					//rect.offset(((right-left)-rect.width())/2, (int) ((cury-rect.top)-rect.height()));
					Rect bgrect=new Rect(rect);
					if (i==chosen_ad_map_i)
					{
						bigtextpaint.setColor(Color.WHITE);
					}
					else
					{
						bigtextpaint.setColor(Color.rgb(200,200,200));
					}
					bgrect.offset(0, (int) ((cury)-rect.top));
					bgrect.left=left;
					bgrect.right=right;
					canvas.drawRect(bgrect, blackgroundpaint);
					canvas.drawText(chosen_ad_map, bgrect.left+2.6f*x_dpmm-rect.left, bgrect.top-rect.top, bigtextpaint);
					if (i==chosen_ad_map_i)
					{
						float mid=cury+rect.height()*0.4f;
						canvas.drawLine(bgrect.left, mid, bgrect.left+x_dpmm*2.3f, mid, linepaint);
						canvas.drawLine(bgrect.left+x_dpmm*1, mid-1*x_dpmm, bgrect.left+x_dpmm*2.3f, mid, linepaint);
						canvas.drawLine(bgrect.left+x_dpmm*1, mid+1*x_dpmm, bgrect.left+x_dpmm*2.3f, mid, linepaint);
					}
					cury+=rect.height();
				}
				bigtextpaint.setTextSize(bigtextsize);
				
			}			 			
		}
		if (isDragging && !gui.getChartMode()) {
			float h = smalltextpaint.getTextSize();
			float topbutton_y = yheader + h;

			String text = "Center";
			final Rect tr1 = drawButton(canvas, right, topbutton_y, text, -1,
					0, right, false);
			airspace_button_space_right = tr1.left - 4;
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

			if (gui != null && (gui.getnorthup() == false || gui.getChartMode())) {
				if (gui.getChartMode())
					text = "Set Chart Up";
				else
					text = "Set North Up";
				final Rect tr2 = drawButton(canvas, 0, topbutton_y, text, 1, 0,
						edge, false);
				if (tr2 != null) {
					airspace_button_space_left = tr2.right + 4;
					clickables.add(new GuiSituation.Clickable() {
						@Override
						public Rect getRect() {
							return tr2;
						}

						@Override
						public void onClick() {
							if (gui.getChartMode() && adloader!=null)
								gui.onChartUp(adloader);
							else
								gui.onNorthUp();
						}
					});
				}
			}

		} else {
			if (download_status != null && !download_status.equals("")) {
				float y2 = (yheader + bigtextpaint.getTextSize() * 1.1f);

				String text = "Load:" + download_status;
				final Rect tr1 = drawButton(canvas, 0, y2, text, 1, 0,
						Integer.MAX_VALUE, false);
				clickables.add(new GuiSituation.Clickable() {
					@Override
					public Rect getRect() {
						return tr1;
					}

					@Override
					public void onClick() {
						Log.i("fplan", "Cancel download");
						gui.cancelMapDownload();
					}
				});

			}
		}
		// Log.i("fplan","prox warning coords "
		// +airspace_button_space_left+".."+airspace_button_space_right);
		if (prox_warning != null
				&& airspace_button_space_right > airspace_button_space_left) {
			final Rect tr1 = drawAirspaceAhead(canvas, (yheader * 3) / 2,
					prox_warning, airspace_button_space_left,
					airspace_button_space_right);
			clickables.add(new GuiSituation.Clickable() {
				@Override
				public Rect getRect() {
					return tr1;
				}

				@Override
				public void onClick() {
					gui.showAirspaces();
				}
			});

		}

		long aft = SystemClock.elapsedRealtime();

		if (cached.size() > 120) {
			int was = cached.size();
			for (Entry<CacheKey, Bitmap> e : cached.entrySet()) {
				if (!used.containsKey(e.getKey())) {
					e.getValue().recycle();
				}
			}
			cached = used;
			// Log.i("fplan.fps","Clear cache with "+was+" items new size: "+cached.size());
		}
		used = new HashMap<MapDrawer.CacheKey, Bitmap>();
		// Log.i("fplan.fps","Time to draw: "+(aft-bef)+"ms");
	}
	ElevationProfile prof=new ElevationProfile();
	private void drawInset(Canvas canvas, Location lastpos,float width,float height,float howfar) {
		Merc merc=Project.latlon2merc(new LatLon(lastpos), 13);
		int steps=60;
		float chunknm=(float)Project.approx_scale(merc, 13, howfar/steps);
		Vector delta=Project.heading2vector(lastpos.getBearing()).mul(chunknm);
		 
		Vector cur=merc.toVector();
		iMerc[] locs=new iMerc[steps];
		for(int i=0;i<steps;++i)
		{
			locs[i]=new iMerc(cur.x,cur.y);
			cur.x+=delta.x;
			cur.y+=delta.y;
		}
		int[] elevs=prof.getProfile(locs);
		float xdelta=width/steps;
		float x=0;
		for(int elev:elevs)
		{
			float y=height*(1.0f-elev/9500.0f);
			blackgroundpaint.setColor(Color.GREEN);
			canvas.drawRect(x,y,x+xdelta,height, blackgroundpaint);
			blackgroundpaint.setColor(Color.BLUE);
			Log.i("fplan.prof","Drawing profile: "+x+","+y+" - "+(x+xdelta)+","+y);
			canvas.drawRect(x,0,x+xdelta,y, blackgroundpaint);
			x+=xdelta;
		}
		blackgroundpaint.setColor(Color.BLACK);
		
	}

	private float getGradientPixels(float fivenm) {
		float gradientpixels=2*fivenm;
		if (gradientpixels>0.15f*(bottom-top))
		{
			gradientpixels=fivenm/5.0f;
			if (gradientpixels>0.15f*(bottom-top))
			{
				gradientpixels=(fivenm/(5.0f*18.52f)); //100m
			}
		}
		return gradientpixels;
	}

	private void drawBitmapMap(Canvas canvas, GetMapBitmap bitmaps,
			ElevBitmapCache elevbmc, boolean terrwarn,
			final AdChartLoader adloader, TransformIf tf, int zoomlevel,
			int sizex, int sizey, boolean isUserPresentlyMovingMap,
			Merc screen_center,boolean elevonly,Rect cliprect) {
		iMerc centertile = new iMerc((int) screen_center.x & (~255),
				(int) screen_center.y & (~255));
		// int diagonal = (int) Math.sqrt((sizex / 2) * (sizey / 2))+1;
		// Log.i("fplan.bitmap","Diagonal: "+diagonal+" sizex:"+sizex+" sizey: "+sizey);
		// int minus = (diagonal + 255) / 256;
		// minus=2;
		// int tot = 2 * minus + 1;

		//int tot;
		int minuspixels;
		{
			float smallres = Math.min(sizex, sizey);
			float bigres = Math.max(sizex, sizey);
			double diag_length = Math.sqrt(smallres * smallres + bigres
					* bigres);
			double diag_angle = Math.atan2(smallres, bigres);
			// double hdiag_length=diag_length/2.0;
			final int tilesize = 256;
			float base = bigres;
			// b= 180 - 90 - diag_angle
			double ba = Math.PI - Math.PI / 2 - diag_angle;
			// maxh/base = sin(b)
			// maxh = sin(b)*base
			// print "diag_length:",diag_length
			// print "max height:",maxh
			minuspixels = ((int) diag_length + 256) / 256;
		}
		// Log.i("fplan.drawmap","Total tiles needed:"+tot);

		
		
		//iMerc topleft = new iMerc(centertile.getX() - (256 * minus),
		//		centertile.getY() - 256 * minus);
		iMerc topleft = new iMerc(centertile.getX() - (256 * minuspixels),
				centertile.getY() - 256 * minuspixels);
		float hdg = (float) (tf.getHdgRad() * (180.0 / Math.PI));
		for (int j = 0; j < 2 * minuspixels; ++j) {

			for (int i = 0; i < 2 * minuspixels; ++i) {

				iMerc cur = new iMerc(topleft.getX() + 256 * i,
						topleft.getY() + 256 * j);
				if (cur.getX() < 0 || cur.getY() < 0) //outside worldmap, west of 180W or north of ~86N.
					continue;
				Vector v = tf.merc2screen(new Merc(cur.getX(), cur.getY()));
				//if (!tileOnScreen((float) v.x, (float) v.y, tf))
				//	continue;
				BitmapRes b = null;
				// Log.i("fplan","Bitmap for "+cur);
				canvas.save();
				canvas.clipRect(cliprect);
				canvas.rotate(-hdg, (float) v.x, (float) v.y);
				RectF trg = new RectF((float) v.x, (float) v.y,
						(float) v.x + 256, (float) v.y + 256);
				if (canvas.quickReject(trg, EdgeType.BW))
				{
					canvas.restore();
					continue;
				}
				if (adloader==null && !elevonly)
				{
					b = bitmaps.getBitmap(cur, zoomlevel);
					if (b != null && b.b != null) {
						// float px=(float)(v.x+i*256);
						// float py=(float)(v.y+j*256);
	
						// Log.i("fplan","Drawing bitmap at "+v.x+","+v.y);
						Rect src = b.rect;
						canvas.drawBitmap(b.b, src, trg, null);
						// Log.i("fplan.terr","Queried "+cur);
						
	
					}
				}
				if (elevonly || (terrwarn && !isUserPresentlyMovingMap)) {
					BMResult elevbm = elevbmc.query2(cur);
					if (elevbm != null && elevbm.bm != null)
						canvas.drawBitmap(elevbm.bm, elevbm.r, trg,
								null);
				}
				canvas.restore();

			}
		}
		// Log.i("fplan.drawmap","Tiles used:"+tilesused);
	}

	private void drawAdMap(Canvas canvas, final AdChartLoader adloader,
			TransformIf tf, int zoomlevel, Merc screen_center) {
		double onenmpixels=Project.approx_scale(screen_center.y, zoomlevel, 1);
		int best_ad_level=adloader.guess_zoomlevel(onenmpixels);
		Log.i("fplan.bad","Best level: "+best_ad_level);
		adloader.set_level(best_ad_level);
		
		adloader.start();
		int w=adloader.get_width();
		int h=adloader.get_height();
		for(int x=0;x<w;x+=256)
		{
			
			for(int y=0;y<=h;y+=256)
			{
				LatLon p1=adloader.pixel2latlon(new Vector(x,y));
				LatLon p2=adloader.pixel2latlon(new Vector(x+256,y));
				LatLon p3=adloader.pixel2latlon(new Vector(x,y+256));
				//LatLon p4=adloader.pixel2latlon(new Vector(x+256,y+256));
				//Log.i("fplan.bml","Figured latlon = "+p1+" "+p2+" "+p3+" "+p4);
				Merc m1=Project.latlon2merc(p1, zoomlevel);
				Merc m2=Project.latlon2merc(p2, zoomlevel);
				Merc m3=Project.latlon2merc(p3, zoomlevel);
				Vector s1=tf.merc2screen(m1);
				Vector s2=tf.merc2screen(m2);
				Vector s3=tf.merc2screen(m3);
				
				
				//Log.i("fplan.bml","adloader got bitmap for "+x+","+y+" drawing near"+s1);
				Vector X=s2.minus(s1).mul(1/256.0);
				Vector Y=s3.minus(s1).mul(1/256.0);
				
				canvas.save();
				Matrix mat=canvas.getMatrix();
				mat.preTranslate((float)s1.x, (float)s1.y);
				Matrix rotscale=new Matrix();
				rotscale.setValues(new float[]{
						(float)X.x,(float)Y.x,0,
						(float)X.y,(float)Y.y,0,
						0,0,1
				});
				mat.preConcat(rotscale);
				canvas.setMatrix(mat);
				RectF trg=new RectF(0,0,256,256);
				boolean qr=canvas.quickReject(trg, EdgeType.BW);
				if (!qr)
				{
					BitmapRes bres=adloader.getBitmap(new iMerc(x,y));
					if (bres!=null && bres.b!=null)
					{
						canvas.drawBitmap(bres.b, bres.rect,trg,null);
					}
				}
				canvas.restore();
				
				
			}
		}
		adloader.end();
	}

	private void drawBaseVectorMap(TripData tripdata, AirspaceLookup lookup,
			Canvas canvas, boolean havefix, TransformIf tf, boolean isDragging,
			boolean chartmode, int zoomlevel, boolean isUserPresentlyMovingMap,
			BoundingBox bb13, BoundingBox smbb13,
			ArrayList<SigPoint> major_airfields) {
		if (true)
		{
		if (onlyWithin(60, isUserPresentlyMovingMap))
			if (zoomlevel >= 8 && lookup != null) {
				ArrayList<AirspaceArea> areas = lookup.areas.get_areas(bb13);
				Collections.sort(areas, new Comparator<AirspaceArea>() {
					@Override
					public int compare(AirspaceArea object1,
							AirspaceArea object2) {
						if ((object1.r & 0xff) > (object2.r & 0xff))
							return -1;
						if ((object1.r & 0xff) < (object2.r & 0xff))
							return 1;
						if ((object1.g & 0xff) < (object2.g & 0xff))
							return 1;
						if ((object1.g & 0xff) > (object2.g & 0xff))
							return -1;
						return 0;
					}
				});
				for (AirspaceArea as : areas) {
					if (as.a==0)
						continue;
					ArrayList<Vector> vs = new ArrayList<Vector>();
					for (LatLon latlon : as.points) {
						Merc m = Project.latlon2merc(latlon, zoomlevel);
						Vector v = tf.merc2screen(m);
						vs.add(v);
					}
					linepaint.setStrokeWidth(0.5f * x_dpmm);
					linepaint.setColor(Color.rgb(as.r, as.g, as.b));
					for (int i = 0; i < vs.size(); ++i) {
						Vector a = vs.get(i);
						Vector b = vs.get((i + 1) % vs.size());


						canvas.drawLine((float) a.getx(), (float) a.gety(),
								(float) b.getx(), (float) b.gety(), linepaint);
					}
				}
			}

		// if (declutter==null)
		DeclutterTree declutter = new DeclutterTree(
				(int) (textpaint.getTextSize() * 1.4f));
		/*
		 * else declutter.clearIfNeeded(); if (last_zoomlevel!=zoomlevel) {
		 * declutter.clear(); last_zoomlevel=zoomlevel; }
		 */
		if (onlyWithin(60, isUserPresentlyMovingMap) && !chartmode)
			if (zoomlevel >= 8 && lookup != null) {
				if (major_airfields == null)
					major_airfields = lookup.majorAirports.findall(bb13);

				for (SigPoint sp : major_airfields) {
					Merc m = Project.latlon2merc(sp.latlon, zoomlevel);
					Vector p = tf.merc2screen(m);
					String text;
					text = sp.name;
					if (zoomlevel <= 8
							&& (sp.extra == null | sp.extra.runways == null || sp.extra.runways.length == 0))
						continue; // only draw really big afs on zoomlevel 8

					textpaint.setColor(Color.GREEN);
					linepaint.setColor(Color.GREEN);
					renderText(canvas, p, text, declutter, Color.GREEN);
				}
			}

		if (onlyWithin(60, isUserPresentlyMovingMap) && !chartmode)
			if (zoomlevel >= 9 && lookup != null) {
				for (SigPoint sp : lookup.minorAirfields.findall(bb13)) {
					// Merc m=Project.merc2merc(sp.pos,13,zoomlevel);
					Merc m = Project.latlon2merc(sp.latlon, zoomlevel);
					Vector p = tf.merc2screen(m);
					String text;
					text = sp.name;
					textpaint.setColor(Color.GREEN);
					linepaint.setColor(Color.GREEN);
					renderText(canvas, p, text, declutter, Color.GREEN);
				}
			}
		if (onlyWithin(60, isUserPresentlyMovingMap) && !chartmode)
			if (lookup != null && zoomlevel>6) {
				for (SigPoint sp : lookup.allCities.findall(bb13)) {
					if (sp.name==null || sp.name.length()==0)
						continue; //this should never happen, but might due to a temporary server bug.
					Merc m = Project.latlon2merc(sp.latlon, zoomlevel);
					Vector p = tf.merc2screen(m);
					textpaint.setARGB(0xff, 0xff, 0xff, 0xb0);
					linepaint.setARGB(0xff, 0xff, 0xff, 0xb0);
					renderText(canvas, p, "[" + sp.name + "]", declutter,
							Color.rgb(0xff, 0xff, 0xb0));
				}
			}
		if (onlyWithin(60, isUserPresentlyMovingMap) && !chartmode)
			if (lookup != null && zoomlevel > 8) {
				for (SigPoint sp : lookup.allTowns.findall(bb13)) {
					Merc m = Project.latlon2merc(sp.latlon, zoomlevel);
					Vector p = tf.merc2screen(m);
					textpaint.setARGB(0xff, 0xff, 0xff, 0xb0);
					linepaint.setARGB(0xff, 0xff, 0xff, 0xb0);
					renderText(canvas, p, "[" + sp.name + "]", declutter,
							Color.rgb(0xff, 0xff, 0xb0));
				}
			}

		if (onlyWithin(60, isUserPresentlyMovingMap) && !chartmode)
			if (zoomlevel >= 10 && lookup != null) {
				for (SigPoint sp : lookup.allOthers.findall(bb13)) {
					// Merc m=Project.merc2merc(sp.pos,13,zoomlevel);
					Merc m = Project.latlon2merc(sp.latlon, zoomlevel);
					Vector p = tf.merc2screen(m);
					// Log.i("fplan",String.format("dxsigp: %s: %f %f",sp.name,px,py));
					// textpaint.setARGB(0, 255,255,255);
					String t;
					{
						textpaint.setARGB(0xff, 0xff, 0xa0, 0xa0);
						linepaint.setARGB(0xff, 0xff, 0xa0, 0xa0);
						t = sp.name;
					}
					renderText(canvas, p, t, declutter,
							Color.rgb(0xff, 0xa0, 0xa0));
				}

				for (SigPoint sp : lookup.allObst.findall(smbb13)) {
					/*
					 */
					// Merc m=Project.merc2merc(sp.pos,13,zoomlevel);
					Merc m = Project.latlon2merc(sp.latlon, zoomlevel);
					Vector p = tf.merc2screen(m);

					// Log.i("fplan",String.format("dxsigp: %s: %f %f",sp.name,px,py));
					// textpaint.setARGB(0, 255,255,255);
					textpaint.setARGB(0xff, 0xff, 0xa0, 0xff);
					linepaint.setARGB(0xff, 0xff, 0xa0, 0xff);
					renderText(canvas, p, String.format("%.0fft", sp.alt),
							null, Color.rgb(0xff, 0xa0, 0xff));
					// canvas.drawText(String.format("%.0fft",sp.alt),
					// (float)(p.x),
					// (float)(p.y), textpaint);
					// canvas.drawPoint((float)p.x,(float)p.y,linepaint);
				}
			}

		
		if (tripdata != null && tripdata.waypoints.size() >= 2) {
			
			
			
			float[] lines = new float[4 * (tripdata.waypoints.size() - 1)];
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

			for (int i = tripdata.waypoints.size() - 2; i >= 0; --i) {
				canvas.drawLine(lines[4 * i + 0], lines[4 * i + 1],
						lines[4 * i + 2], lines[4 * i + 3], widetrippaint);
				canvas.drawLine(lines[4 * i + 0], lines[4 * i + 1],
						lines[4 * i + 2], lines[4 * i + 3], trippaint);
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
				renderText(canvas, p, wp.name, null, Color.WHITE);
			}
		}

		if (!havefix) {

			if (!isDragging) {
				linepaint.setStrokeWidth((int) (5 * x_dpmm));
				linepaint.setARGB(200, 255, 255, 0);
				canvas.drawLine(left, top, right, bottom, linepaint);
				canvas.drawLine(left, bottom, right, top, linepaint);
			}

			if (isDragging) {
				linepaint.setStrokeWidth((int) (2.2 * x_dpmm));
				linepaint.setARGB(100, 255, 0, 0);
			} else {
				linepaint.setStrokeWidth((int) (2.5 * x_dpmm));
				linepaint.setARGB(255, 255, 0, 0);
			}

			canvas.drawLine(left, top, right, bottom, linepaint);
			canvas.drawLine(left, bottom, right, top, linepaint);

			if (!isDragging) {

				bigtextpaint.setTextSize((right - left) * 0.2f);
				bigtextpaint.setTypeface(Typeface.DEFAULT_BOLD);
				String t = "NO GPS";
				bigtextpaint.setARGB(255, 255, 255, 0);
				bigtextpaint.setStyle(Paint.Style.FILL);
				Rect bounds = new Rect();
				bigtextpaint.getTextBounds(t, 0, t.length(), bounds);
				float tx=left + 0.5f * (right - left) - 0.5f * bounds.width() - bounds.left;
				float ty=top + 0.5f * (bottom - top) - bounds.top - 0.5f * bounds.height();
				
				drawBigText(canvas, t, tx, ty,bigtextpaint);
				//public voidgetTextPath (String text, int start, int end, float x, float y, Path path)
				
				
				//canvas.drawText(t,
														
				bigtextpaint.setARGB(255, 255, 0, 0);
				bigtextpaint.setStyle(Paint.Style.STROKE);
				bigtextpaint.setStrokeWidth((right - left) / 75);

				tx=left + 0.5f * (right - left) - 0.5f * bounds.width() - bounds.left;
				ty=top + 0.5f * (bottom - top) - bounds.top - 0.5f * bounds.height();
				
				drawBigText(canvas, t, tx, ty, bigtextpaint);
				bigtextpaint.setStyle(Paint.Style.FILL);

				bigtextpaint.setTextSize(bigtextsize);
				bigtextpaint.setColor(Color.WHITE);
				bigtextpaint.setTypeface(Typeface.DEFAULT);

			}
			linepaint.setStrokeWidth(2.5f * x_dpmm);
			linepaint.setColor(Color.RED);
		}
		}
	}

	private void drawInfoPanel(Canvas canvas, final GuiSituation gui,
			TransformIf tf, boolean extrainfo, int extrainfolineoffset,
			int zoomlevel, ArrayList<GuiSituation.Clickable> clickables,
			InformationPanel we) {
		float tsy = bigtextpaint.getTextSize() + 2;
		final int maxlines = getNumInfoLines(bottom - top);
		Vector point = we.getPoint();
		if (point != null) {
			Merc me = Project.merc2merc(new Merc(point), 13, zoomlevel);
			if (me != null) {
				// float
				// px=(float)rot_x(p.getx()-center.x,p.gety()-center.y)+ox;
				// float
				// py=(float)rot_y(p.getx()-center.x,p.gety()-center.y)+oy;
				Vector p = tf.merc2screen(me);
				thinlinepaint.setARGB(180, 40, 40, 255);
				thinlinepaint.setStrokeWidth(1 * x_dpmm);
				canvas.drawCircle((float) p.x, (float) p.y, 2 * x_dpmm,
						thinlinepaint);
				thinlinepaint.setStrokeWidth(0.7f * x_dpmm);
				canvas.drawLine((float) p.x + 1.7f * x_dpmm, (float) p.y,
						(float) p.x + 2.5f * x_dpmm, (float) p.y,
						thinlinepaint);
				canvas.drawLine((float) p.x - 1.7f * x_dpmm, (float) p.y,
						(float) p.x - 2.5f * x_dpmm, (float) p.y,
						thinlinepaint);
				canvas.drawLine((float) p.x, (float) p.y + 1.7f * x_dpmm,
						(float) p.x, (float) p.y + 2.5f * x_dpmm,
						thinlinepaint);
				canvas.drawLine((float) p.x, (float) p.y - 1.7f * x_dpmm,
						(float) p.x, (float) p.y - 2.5f * x_dpmm,
						thinlinepaint);
				thinlinepaint.setStrokeWidth(1f * x_dpmm);
			}
		}

		thinlinepaint.setColor(Color.WHITE);
		float y = bottom - 3.0f * y_dpmm;//
		bigtextpaint.setColor(Color.WHITE);
		// long when = we.getWhen();
		double dist = we.getDistance();
		boolean skipped = we.getSkipped();
		boolean empty = we.getEmpty();
		String whenstr;
		String whentempl;
		// Log.i("fplan","When: "+when);
		Date passed = we.getPassed();
		Date eta2 = we.getETA2();
		if (eta2 != null) {
			int when = (int) ((eta2.getTime() - new Date().getTime()) / 1000l);
			whenstr = fmttime((int) when);
			whentempl="22:22_";
			// whenstr="T:"+formatter.format(eta2)+"Z";
		} else {
			if (empty) {
				whenstr = "";
				whentempl="";
			} else if (skipped) {
				whenstr = "Skipped.";
				whentempl="Skipped.";
			} else {
				if (passed != null)
					whenstr = "(" + formatter.format(passed) + "Z)";
				else
					whenstr = "--:--";
				whentempl="(22:22Z)";
			}
		}

		/*
		 * #error: Move position into base class of WarningEvent #Then move
		 * calculation of distance also to baseclass. #don't store time and
		 * distance, just store position. #Let moving map calculate time,
		 * based on distance (or even based on position?)
		 */
		boolean extrainfo_available = we.getHasExtraInfo();

		String[] details;
		if (extrainfo)
			details = we.getExtraDetails();
		else
			details = we.getDetails();

		final Place[] extended_available = we.getHasExtendedInfo();

		int actuallines = details.length;
		// Log.i("fplan", "details.length 1 : " + details.length);
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

		float topy1 = y - tsy * actuallines - 4;

		final Rect tr3 = drawButton(canvas, right - 5, topy1 - tsy * 1.4f,
				"Close", -1, left, right, false);
		if (extended_available.length > 0) {
			final Rect tr4 = drawButton(canvas, left + 5, topy1 - tsy
					* 1.4f, "More", 1, left, right, false);

			clickables.add(new GuiSituation.Clickable() {
				@Override
				public Rect getRect() {
					return tr4;
				}

				@Override
				public void onClick() {
					gui.onShowExtended(extended_available);
				}
			});
		}
		clickables.add(new GuiSituation.Clickable() {
			@Override
			public Rect getRect() {
				return tr3;
			}

			@Override
			public void onClick() {
				gui.onCloseInfoPanel();
			}
		});
		
		bigtextpaint.setTextSize(bigtextsize);
		

		RectF r = new RectF(0, topy1, right, bottom);
		canvas.drawLine(0, topy1, right, topy1, thinlinepaint);
		canvas.drawRect(r, backgroundpaint);
		String dir=DescribePosition.roughdirshort((float)((we.getHeading()+180.0)%360.0));
		if (dist >= 0)
		{
			addTextIfFits(canvas, "you:","",
					r, "you:","",  y1, smalltextpaint,bigtextpaint);
			addTextIfFits(canvas, "1222.2","NM",
					r, String.format("%.1f", dist), "NM", y1, bigtextpaint,smalltextpaint,false,true);
			addTextIfFits(canvas, "NWW ", "",
					r, String.format(dir+" ", dist), "", y1, bigtextpaint, smalltextpaint);
		}


		//addTextIfFits(canvas, "(SWW)", "", r, "("+dir+")", "", y1, bigtextpaint, smalltextpaint);
		
		// canvas.drawText(String.format("%.1fnm",we.getDistance()),
		// 2,y1,bigtextpaint);
		addTextIfFits(canvas, whentempl, "", r, whenstr, "", y1, mediumtextpaint, mediumtextpaint);
		
		// canvas.drawText(String.format("T:%s",whenstr),
		// 70,y1,bigtextpaint);		
		addTextIfFits(canvas, null, null, r, we.getPointTitle(), "", y1, mediumtextpaint, mediumtextpaint);
		// canvas.drawText(we.getTitle(), 140,y1,bigtextpaint);
		for (int i = 0; i < actuallines - 1; ++i) {
			/*
			 * Log.i("fplan", "Actuallines:" + actuallines + " offset: " +
			 * extrainfolineoffset + " details length:" + details.length);
			 */
			if (extrainfolineoffset + i < details.length)
				canvas.drawText(details[extrainfolineoffset + i], 2, y1
						+ tsy + i * tsy, bigtextpaint);
		}
		if (we.hasLeft()) {
			canvas.drawLine((int) (0 + 2.8f * x_dpmm),
					(int) (bottom - 0.7f * y_dpmm),
					(int) (0 + 2.0f * x_dpmm),
					(int) (bottom - 1.5f * y_dpmm), thinlinepaint);
			canvas.drawLine((int) (0 + 2.0f * x_dpmm),
					(int) (bottom - 1.5f * y_dpmm),
					(int) (0 + 2.8f * x_dpmm),
					(int) (bottom - 2.3f * y_dpmm), thinlinepaint);

			canvas.drawRect((int) (0.2f * x_dpmm),
					(int) (bottom - 2.8f * y_dpmm), (int) (4.8f * x_dpmm),
					(int) (bottom - 0.2f * y_dpmm), thinlinepaint);
		}
		final Rect leftrect = new Rect(left, (int) topy1,
				(right - left) / 4, bottom);
		clickables.add(new GuiSituation.Clickable() {
			@Override
			public Rect getRect() {
				return leftrect;
			}

			@Override
			public void onClick() {
				gui.onInfoPanelBrowse(-1);
			}
		});
		float off = right - (5.0f * x_dpmm);
		if (we.hasRight()) {
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
		final Rect rightrect = new Rect(right - (right - left) / 4,
				(int) topy1, right, bottom);
		clickables.add(new GuiSituation.Clickable() {
			@Override
			public Rect getRect() {
				return rightrect;
			}

			@Override
			public void onClick() {
				gui.onInfoPanelBrowse(+1);
			}
		});

		{
			canvas.drawRect((int) (5.2f * x_dpmm),
					(int) (bottom - 2.8f * y_dpmm),
					(int) (-0.2f * x_dpmm + off),
					(int) (bottom - 0.2f * y_dpmm), thinlinepaint);
			if (extrainfo_available) {
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
				canvas.drawText(t, (right - left) * 0.5f - tbounds.width()
						/ 2, (int) (bottom - 1.0 * y_dpmm), textpaint);
			}
			int w = (right - left) / 4;
			final Rect midrect = new Rect(left + w, (int) topy1, right - w,
					bottom);
			clickables.add(new GuiSituation.Clickable() {
				@Override
				public Rect getRect() {
					return midrect;
				}

				@Override
				public void onClick() {
					gui.cycleextrainfo();
				}
			});

		}
		// canvas.drawText(String.format("%03.0f°",lastpos.getBearing()),
		// 50, y, bigtextpaint);
		// canvas.drawText(String.format("%.0fkt",lastpos.getSpeed()*3.6/1.852),150,y,textpaint);
	}

	private void drawBug(TripState tripstate, Canvas canvas, Location lastpos,
			int sizex, int sizey, float y) {
		try
		{
			if (tripstate!=null) 
			{
				BugInfo bug=tripstate.getBug();
				if (bug!=null)
				{
					canvas.save();
					float hdgdelta=(float)(bug.hdg-lastpos.getBearing());
					if (hdgdelta<-180) hdgdelta+=360;
					if (hdgdelta>180) hdgdelta-=360;
					double ang=(180.0/Math.PI)*Math.atan2(sizex/2,(3*sizey)/4);
					hdgdelta/=ang;
					arrowpaint.setColor(Color.WHITE);
					Vector bugvec=new Vector(right/2+(right/2)*hdgdelta,y);
					if (hdgdelta>1)
					{
						bugvec=new Vector(right,y);
					}
					else if (hdgdelta<-1)
					{
						bugvec=new Vector(0,y);
					}
					
					canvas.save();
					arrowpaint.setStrokeWidth(0.7f*x_dpmm);
					arrowpaint.setColor(Color.BLACK);
					float ww=4.5f;
					float wy=2.0f;
					canvas.rotate(bug.bank,(float)bugvec.x,(float)bugvec.y+wy*y_dpmm);
					canvas.drawLine((float)bugvec.x-ww*x_dpmm, (float)bugvec.y+wy*y_dpmm, (float)bugvec.x+ww*x_dpmm, (float)bugvec.y+wy*x_dpmm, arrowpaint);
					arrowpaint.setColor(Color.WHITE);
					arrowpaint.setStrokeWidth(0.4f*x_dpmm);
					canvas.drawLine((float)bugvec.x-ww*x_dpmm, (float)bugvec.y+wy*y_dpmm, (float)bugvec.x+ww*x_dpmm, (float)bugvec.y+wy*x_dpmm, arrowpaint);
					arrowpaint.setColor(Color.BLACK);
					canvas.restore();
					
					arrowpaint.setStrokeWidth(0.7f*x_dpmm);
					canvas.drawLine((float)0.5f*right, (float)bugvec.y, 0.5f*right, (float)bugvec.y+2.5f*x_dpmm, arrowpaint);
					arrowpaint.setColor(Color.WHITE);
					arrowpaint.setStrokeWidth(0.4f*x_dpmm);
					canvas.drawLine((float)0.5f*right, (float)bugvec.y, 0.5f*right, (float)bugvec.y+2.5f*x_dpmm, arrowpaint);
					
					int rot=0;
					if (hdgdelta>1)
					{
						bugvec=new Vector(right,y+1.50f*x_dpmm);
						rot=90;
					}
					else if (hdgdelta<-1)
					{
						bugvec=new Vector(0,y+1.5f*x_dpmm);
						rot=270;
					}
					else
					{
						bugvec=new Vector(right/2+(right/2)*hdgdelta,y);
					}
					
					if (rot!=0)
						canvas.rotate(rot,(float)bugvec.x,(float)bugvec.y);
					
					Path path = new Path();
					path.moveTo((int) bugvec.x - 1.4f*x_dpmm, (int) bugvec.y + 3.2f*x_dpmm);
					path.lineTo((int) bugvec.x + 1.4f*x_dpmm, (int) bugvec.y + 3.2f*x_dpmm);
					path.lineTo((int) bugvec.x, (int) bugvec.y);
					path.close();
					
					arrowpaint.setStyle(Style.FILL);
					arrowpaint.setColor(Color.WHITE);
					canvas.drawPath(path, arrowpaint);
					arrowpaint.setColor(Color.BLACK);
					arrowpaint.setStrokeWidth(0.25f*x_dpmm);
					arrowpaint.setStyle(Style.STROKE);
					canvas.drawPath(path, arrowpaint);
											
					canvas.restore();

					arrowpaint.setColor(Color.WHITE);
					canvas.drawLine((float)0.5f*right, (float)bugvec.y, 0.5f*right, (float)bugvec.y+2.5f*x_dpmm, arrowpaint);

					arrowpaint.setStyle(Style.FILL);
					
					
				}
			}
		}catch(Throwable e)
		{
			e.printStackTrace();
		}
	}

	private void drawBigText(Canvas canvas, String t, float tx, float ty,Paint paint) {
		Path path=new Path();
		paint.getTextPath(t, 0, t.length(), tx, ty, path);
		path.close();
		canvas.drawPath(path, paint);
	}

	private Rect drawButton(Canvas canvas, float x, float topbutton_y,
			String text, int layoutdir, int x1lim, int x2lim,
			boolean measureOnly) {
		float h2 = smalltextpaint.getTextSize();
		thinlinepaint.setColor(Color.WHITE);
		final Rect tr1 = new Rect();
		smalltextpaint.getTextBounds(text, 0, text.length(), tr1);
		tr1.bottom = (int) (tr1.top + h2);

		int xpos;
		if (layoutdir < 0)
			xpos = (int) (x - tr1.width() - h2);
		else
			xpos = (int) (x + h2);
		tr1.offsetTo(xpos, (int) topbutton_y);
		MapDrawer.grow(tr1, (int) (0.4f * h2));
		if (tr1.left < x1lim || tr1.right > x2lim)
			return null;
		if (!measureOnly) {
			canvas.drawRect(tr1, backgroundpaint);
			thinlinepaint.setStrokeWidth(0.25f * x_dpmm);
			canvas.drawRect(tr1, thinlinepaint);

			canvas.drawText(text, tr1.left + 0.4f * h2, tr1.bottom - 0.4f * h2,
					smalltextpaint);
		}
		return tr1;
	}

	private Rect drawAirspaceAhead(Canvas canvas, float y, String[] texts2,
			int x1lim, int x2lim) {
		float x = x1lim;
		Rect totrect = new Rect();

		String[] texts = new String[texts2.length + 1];
		texts[0] = "Airspace Ahead";
		for (int i = 0; i < texts2.length; ++i)
			texts[i + 1] = texts2[i];

		int[] textsizes = new int[texts.length];
		Rect[] bounds = new Rect[texts.length];
		int maxx = 0;
		int sy = 0;
		for (int i = 0; i < texts.length; ++i) {
			if (i == 0)
				textsizes[i] = (int) (2.8f * y_dpmm);
			else if (i == 1)
				textsizes[i] = (int) (3.3f * y_dpmm);
			else
				textsizes[i] = (int) (3.5f * y_dpmm);

			if (textsizes[i] < y_dpmm)
				textsizes[i] = (int) y_dpmm;
			bounds[i] = new Rect();
			for (; textsizes[i] > 10; textsizes[i] -= (textsizes[i] / 8 + 1)) {
				ahtextpaint.setTextSize(textsizes[i]);
				ahtextpaint.getTextBounds(texts[i], 0, texts[i].length(),
						bounds[i]);
				if (bounds[i].width() < (x2lim - x1lim))
					break;
			}
			if (bounds[i].width() > maxx)
				maxx = bounds[i].width();
			sy += bounds[i].height() + 4;
		}
		totrect.left = (int) x;
		totrect.top = (int) y;
		totrect.right = (int) x + maxx + 25;
		totrect.bottom = (int) (y + sy);
		totrect.offset(((x2lim - x1lim) - totrect.width()) / 2, 0);
		if (totrect.right > x2lim)
			totrect.right = x2lim;
		if (totrect.left < x1lim)
			totrect.left = x1lim;

		canvas.save();
		canvas.drawRect(totrect, backgroundpaint);
		canvas.drawRect(totrect, thinlinepaint);

		canvas.clipRect(totrect);
		for (int i = 0; i < texts.length; ++i) {
			ahtextpaint.setTextSize(textsizes[i]);
			canvas.drawText(texts[i], totrect.left - bounds[i].left + 1
					+ totrect.width() / 2 - bounds[i].width() / 2, y
					- bounds[i].top + 1, ahtextpaint);
			y += bounds[i].height() + 2;
		}
		canvas.restore();
		return totrect;
	}

	public int getNumInfoLines(int ysize) {
		float tsy = bigtextpaint.getTextSize() + 2;
		final int maxlines = (int) ((0.5f * (ysize)) / tsy) - 1;
		if (maxlines <= 1)
			throw new RuntimeException(
					"The screen is too small for this program");
		// Log.i("fplan", "numinfolines:" + maxlines);
		return maxlines;
	}

	private boolean tileOnScreen(float cx, float cy, TransformIf tf) {
		float maxdiag = 363;
		if (left != 0 || top != 0)
			return false;
		if (cx + maxdiag < 0)
			return false;
		if (cx - maxdiag > right)
			return false;
		if (cy + maxdiag < 0)
			return false;
		if (cy - maxdiag > bottom)
			return false;

		Vector base = new Vector(cx, cy);
		Vector v = new Vector();
		int sidex = 0;
		int sidey = 0;
		Vector[] tilecorners = new Vector[4];
		for (int j = 0; j < 2; ++j) {
			for (int i = 0; i < 2; ++i) {
				v.x = 256 * i;
				v.y = 256 * j;
				Vector r = v.rot(-tf.getHdgRad());
				r.x += base.x;
				r.y += base.y;
				int idx = i;
				if (j == 1)
					idx = 3 - i;
				tilecorners[idx] = r.copy();
				int cursidex = 0;
				int cursidey = 0;
				if (r.x < 0)
					cursidex = -1;
				if (r.x > right)
					cursidex = 1;
				if (r.y < 0)
					cursidey = -1;
				if (r.y > bottom)
					cursidey = 1;
				if (cursidex == 0 && cursidey == 0)
					return true; // corner is actually on screen
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
		// If we get here, the tile is either outside of screen,
		// or one of its lines is intersecting, but no vertex is on screen.
		// this means that if the polygon is on the screen, one of
		// the screen corners has to be in the polygon.
		// (Since the tile/polygon is known to be smaller than screen)
		ConvexPolygon tilepol = new ConvexPolygon(tilecorners);

		if (tilepol.inside(new Vector(0, 0)))
			return true;
		if (tilepol.inside(new Vector(right, 0)))
			return true;
		if (tilepol.inside(new Vector(right, bottom)))
			return true;
		if (tilepol.inside(new Vector(0, bottom)))
			return true;

		return false;

	}
	boolean addTextIfFits(Canvas canvas, String sizetext, String sizetext2,
			RectF r, String realtext, String realtext2, float y, Paint tp, Paint tp2) {
		return addTextIfFits(canvas, sizetext, sizetext2,
				r, realtext, realtext2, y, tp, tp2,false, false); 
	}

	boolean addTextIfFits(Canvas canvas, String sizetext, String sizetext2,
			RectF r, String realtext, String realtext2, float y, Paint tp, Paint tp2,boolean strikethrough, boolean tight) {
		if (sizetext == null) {
			Rect rect = new Rect();
			getTextBounds(realtext, tp, rect);
			canvas.drawText(realtext, r.left, y, tp);
			canvas.drawText(realtext2, (float)(r.left+rect.width()), y, tp2);
			r.left = r.right + 1;
			return true;
		} else {
			Rect rect2 = new Rect();
			getTextBounds(sizetext2, tp2, rect2);
			Rect rect = new Rect();
			getTextBounds(sizetext, tp, rect);
			
			if (r.left + rect.width() + rect2.width() < r.right) {
				canvas.drawText(realtext, r.left, y, tp);
				float x2;
				if (!tight)
				{
					x2=r.left+rect.width()+0.25f*x_dpmm;
				}
				else
				{
					Rect realsize1=new Rect();
					getTextBounds(realtext, tp, realsize1);
					x2=r.left+realsize1.width()+0.25f*x_dpmm;
				}
				canvas.drawText(realtext2, x2, y, tp2);
				if (strikethrough)
				{
					Rect rect3 = new Rect();
					tp2.setStrokeWidth(0.5f*x_dpmm);
					getTextBounds(realtext2, tp2, rect3);
					canvas.drawLine(r.left+rect.width(), y+rect3.bottom-rect3.height()/2, r.left+rect.width()+rect3.width(), y+rect3.bottom-rect3.height()/2, tp2);
				}
				r.left += (rect.width()+rect2.width()) + 1.0f * x_dpmm;
				return true;
			} else {
				// r.left = r.right + 1; // definitely out of space now!
				return false;
			}
		}

	}

	private void getTextBounds(String sizetext, Paint tp, Rect rect) {
		if (sizetext.length()==0)
		{
			rect.left=0;rect.right=0;
			rect.top=0;
			rect.bottom=(int)(tp.getTextSize()/2);
			return;
		}
		tp.getTextBounds(sizetext, 0, sizetext.length(), rect);
	}

	static void grow(Rect r, int howmuch) {
		r.left -= howmuch;
		r.right += howmuch;
		r.top -= howmuch;
		r.bottom += howmuch;
	}

	public static String fmttime(int when) {
		if (when == 0 || when > 3600 * 24 * 10)
			return "--:--";
		return String.format("%d:%02d", when / 60, when % 60);
	}
	public static String fmtdelay(int when) {
		if (when>60*24*7)
			return "--";
		if (when<-60*24*7)
			return "-";
		if (when<0) {
			when=-when;
		}
		if (when<60)
			return ""+when+"m";
		int hour=when/60;
		int minute=when%60;
		return String.format("%dh%02dm", hour,minute);
	}

	public void find_me(GuiSituation gui,Location lastpos,float width,float height) {
		handler.removeCallbacks(animate_circle);
		TransformIf tf = gui.getTransform();
		int zoomlevel=gui.getZoomlevel();
		Merc pos = Project.latlon2merc(new LatLon(
				lastpos.getLatitude(), lastpos.getLongitude()),
				zoomlevel);
		circle=new Circle();
		
		circle.radius=width/2.2f;
		Merc p=tf.screen2merc(new Vector(width/2f,height/2f));
		circle.x=(float)p.x;
		circle.y=(float)p.y;
		circle.dx=(float)pos.x-circle.x;
		circle.dy=(float)pos.y-circle.y;
		int steps_needed=(int)(Math.sqrt(circle.dx*circle.dx+circle.dy*circle.dy)/(width/10.0f)+1);
		if (steps_needed<10)
			steps_needed=10;
		circle.dx/=steps_needed;
		circle.dy/=steps_needed;
		circle.steps_left=steps_needed;
		circle.steps_done=0;
		circle.dradius=(circle.radius-width/10)/steps_needed;
		handler.postDelayed(animate_circle, 400);				
	}
	

}
