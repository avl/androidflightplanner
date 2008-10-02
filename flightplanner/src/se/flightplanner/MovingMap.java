package se.flightplanner;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.Paint.Style;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.location.Location;
import android.location.LocationManager;
import android.view.View;

public class MovingMap extends View {
	private TripData tripdata;
	private Location mylocation;
	private Location lastpos;
	private int background;
	private int foreground;
	private Paint textpaint;
	public MovingMap(Context context)
	{
		super(context);
		background=Color.BLACK;
		foreground=Color.WHITE;
		textpaint = new Paint();
		textpaint.setAntiAlias(true);
		textpaint.setStrokeWidth(5);
		textpaint.setColor(foreground);
		textpaint.setStrokeCap(Paint.Cap.ROUND);
		textpaint.setTextSize(16);
		textpaint.setTypeface(Typeface.create(Typeface.SANS_SERIF,
                                           Typeface.NORMAL));
		//textpaint.set
		mylocation=null;
		lastpos=null;
		tripdata=null;		
	}
	
	public void update_tripdata(TripData ptripdata)
	{
		tripdata=ptripdata;
		invalidate();
	}
	protected void onDraw(Canvas canvas) {
        canvas.drawColor(background);
		if (tripdata==null)
		{
			canvas.drawText("No trip loaded.", this.getLeft(), this.getTop()+textpaint.getTextSize(), textpaint);
			return;
		}
		else if (mylocation==null)
		{
			canvas.drawText("No GPS-fix.", this.getLeft(), this.getTop()+textpaint.getTextSize(), textpaint);			
		}
		canvas.drawText("Top:"+this.getTop(), this.getLeft(), this.getTop()+textpaint.getTextSize()+80, textpaint);			
		if (lastpos!=null)
		{
			canvas.translate(this.getLeft(),this.getTop());
			draw_actual_map(canvas,
					this.getRight()-this.getLeft(),
					this.getBottom()-this.getTop(),
					lastpos);
		}
        /*
		int x = 10;
		int y = 10;
		int width = 300;
		int height = 50;
		ShapeDrawable mDrawable = new ShapeDrawable(new OvalShape());
		mDrawable.getPaint().setColor(0xff74AC23);
		mDrawable.setBounds(x, y, x + width, y + height);
		mDrawable.draw(canvas);
        */
		
		//canvas.drawText("ÅÄÖ", 10, this.getTop()+textpaint.getTextSize(), textpaint);
		//canvas.drawText("TRIP", 10, this.getBottom(), textpaint);
		//canvas.drawText("TRIP", 10, 100, textpaint);
	}
	private void draw_actual_map(Canvas canvas, int sizex, int sizey, Location lastpos2) {
		// TODO Auto-generated method stub
		
	}

	public void gps_update(Location loc)
	{
		mylocation=loc;
		lastpos=mylocation;
		invalidate();
	}
	public void gps_disabled() {
		mylocation=null;
		invalidate();		
	}
}
