package se.flightplanner;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowManager;
import android.view.View.MeasureSpec;

public class CompassRoseView extends View {
	private int box; //desired size
	private float own_heading;
	private float point_heading;
	private Bitmap little_plane;
	public void set(float own_heading,float point_heading)
	{
		this.own_heading=own_heading;
		this.point_heading=point_heading;
		invalidate();
	}
	private Paint textpaint;
	private Paint linepaint;
	private Paint thinlinepaint;
	private Paint thinthinlinepaint;
	private Paint pointpaint;
	public CompassRoseView(Context context)
	{
		super(context);		
		DisplayMetrics metrics = new DisplayMetrics();
		
		((WindowManager)context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getMetrics(metrics);
				
		box=Math.min(metrics.widthPixels,metrics.heightPixels)/2;
		
		
		
		textpaint = new Paint();
		textpaint.setAntiAlias(true);
		textpaint.setColor(Color.WHITE);
		
		textpaint.setTextSize(metrics.scaledDensity*(float)(box/10));
		
		linepaint = new Paint();
		linepaint.setAntiAlias(true);
		linepaint.setColor(Color.WHITE);
		linepaint.setStyle(Style.STROKE);
		linepaint.setStrokeWidth(box/20);
		
		thinlinepaint = new Paint();
		thinlinepaint.setAntiAlias(true);
		thinlinepaint.setColor(Color.WHITE);
		thinlinepaint.setStyle(Style.STROKE);
		thinlinepaint.setStrokeWidth(Math.max(box/45, 2));

		thinthinlinepaint = new Paint();
		thinthinlinepaint.setAntiAlias(true);
		thinthinlinepaint.setColor(Color.WHITE);
		thinthinlinepaint.setStyle(Style.STROKE);
		thinthinlinepaint.setStrokeWidth(Math.max(box/70, 2));

		pointpaint = new Paint();
		pointpaint.setAntiAlias(true);
		pointpaint.setColor(Color.RED);		
	}
	
	protected void onMeasure(int widthMeasureSpec,int heightMeasureSpec)
	{
		
		
		int widthsize=MeasureSpec.getSize(widthMeasureSpec);
		int widthmode=MeasureSpec.getMode(widthMeasureSpec);
		int heightsize=MeasureSpec.getSize(heightMeasureSpec);
		int heightmode=MeasureSpec.getMode(heightMeasureSpec);
		
		int width;
		int height;

		if (widthmode==MeasureSpec.EXACTLY)
			width=widthsize;
		else if (widthmode==MeasureSpec.AT_MOST)
			width=widthsize>box ? box : widthsize;
		else
			width=box;
		
		
			
		if (heightmode==MeasureSpec.EXACTLY)
			height=heightsize;
		else if (heightmode==MeasureSpec.AT_MOST)
			height=heightsize>box ? box: heightsize;
		else
			height=box;
		
		if (height<getSuggestedMinimumHeight())
			height=getSuggestedMinimumHeight();
		if (width<getSuggestedMinimumWidth())
			width=getSuggestedMinimumWidth();
		//Log.i("bridge","Textheight: "+textheight+" height: "+height );
		setMeasuredDimension(width, height);
	}
	@Override
	protected void onDraw(Canvas canvas)
	{		
		//canvas.
		float w=getRight()-getLeft();
		float h=getBottom()-getTop();
		float abox=Math.min(w,h);
		float awid=abox*0.05f;

		String[] letters=new String[]{"N","E","S","W"};
		float sb=abox/6.0f;
		
		canvas.save();
		canvas.rotate(360-own_heading,abox/2,abox/2);
		RectF boxrect=new RectF(sb,sb,abox-sb,abox-sb);
		linepaint.setColor(Color.rgb(0xb0, 0xff, 0xb0));
		thinthinlinepaint.setColor(Color.rgb(0xb0, 0xff, 0xb0));		
		canvas.drawArc(boxrect, 20, 50, false, linepaint);
		canvas.drawArc(boxrect, 110, 50, false, linepaint);
		canvas.drawArc(boxrect, 200, 50, false, linepaint);
		canvas.drawArc(boxrect, 290, 50, false, linepaint);
		
		float radius=0.5f*abox-0.7f*sb;
		for(int deg=0;deg<360;deg+=30)
		{
			float radian=(float)(deg*(Math.PI/180.0f));
			float radius2=radius+0.2f*sb;
			if (deg%90==0)
				radius2+=0.2f*sb;
			float x1=abox*0.5f+(float) ((radius)*Math.sin(radian));
			float y1=abox*0.5f-(float) ((radius)*Math.cos(radian));
			float x2=abox*0.5f+(float) ((radius2)*Math.sin(radian));
			float y2=abox*0.5f-(float) ((radius2)*Math.cos(radian));
			
			canvas.drawLine(x1,y1,x2,y2,thinthinlinepaint);
		}
		
		
		for(int i=0;i<4;++i)
		{
			float rad=(float)(Math.PI*2f)*((float)i)/4.0f;
			float nomx=abox*0.5f+(float) ((abox*0.27f)*Math.sin(rad));
			float nomy=abox*0.5f-(float) ((abox*0.27f)*Math.cos(rad));
			Rect letter=new Rect();
			textpaint.getTextBounds(letters[i], 0, 1, letter);
			float sizex=letter.width();
			float sizey=letter.height();
			float drawx=nomx-0.5f*sizex-letter.left;
			float drawy=nomy-0.5f*sizey-letter.top;
			canvas.save();
			canvas.rotate(180-i*90,nomx,nomy);
			textpaint.setColor(Color.rgb(0xb0, 0xff, 0xb0));
			canvas.drawText(letters[i], drawx,drawy,textpaint);		
			canvas.restore();
		}
		
		
		canvas.restore();
		Rect planerect=new Rect();
		planerect.left=(int) (0.30f*abox);
		planerect.right=(int) (0.70f*abox);
		planerect.top=(int) (0.30f*abox);
		planerect.bottom=(int) (0.70f*abox);
		
		if (little_plane==null)
		{
			//Options opts=new Options();
			//opts.
			little_plane=BitmapFactory.decodeResource(
					getResources(),
					R.drawable.plane);			
		}
		if (little_plane!=null && !little_plane.isRecycled())
		{
			canvas.drawBitmap(little_plane, null, planerect, thinlinepaint);
		}
		else
		{
			Log.i("fplan","Little plane was null or recycled: "+little_plane);
		}
		canvas.drawLine(0.5f*abox,0,0.5f*abox,awid*1.8f,thinlinepaint);
		canvas.drawLine(0.5f*abox,awid*7.0f,0.5f*abox,0.5f*abox,thinlinepaint);

		
		if (point_heading>-500)
		{
			canvas.save();
			canvas.rotate(point_heading-own_heading,abox/2,abox/2);
			thinlinepaint.setColor(Color.rgb(0xd0, 0xd0, 0xff));
			canvas.drawLine(0.5f*abox,0,0.5f*abox-awid,awid*2,thinlinepaint);
			canvas.drawLine(0.5f*abox,0,0.5f*abox+awid,awid*2,thinlinepaint);
			thinlinepaint.setColor(Color.WHITE);;
			
			canvas.restore();
		}
		
	}
	
}
