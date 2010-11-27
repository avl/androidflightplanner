package se.flightplanner.map3d;

import java.util.ArrayList;
import java.util.HashMap;

import javax.microedition.khronos.opengles.GL10;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.Paint.Style;

public class FontBitmapCache {

	Bitmap b;
	Canvas canvas;
	Paint textpaint;
	Paint erasepaint;
	int tex;
	
	private static class Slot
	{
		String text;
		int idx;
		boolean used;
	}
	int framecount;
	int capacity;
	boolean dirty;
	ArrayList<Slot> slots;
	HashMap<String,Slot> string2slot;
	public FontBitmapCache(int capacity)
	{
		if (capacity!=16 && capacity!=32)
			throw new RuntimeException("Only 16 and 32 are acceptable capacities");
		b=Bitmap.createBitmap(512,16*capacity,Bitmap.Config.RGB_565);
		canvas=new Canvas(b);
		for(int i=0;i<capacity;++i)
		{
			Slot slot=new Slot();
			slot.idx=i;
			slot.used=false;
			slot.text=null;
			slots.add(slot);
		}
		int background=Color.BLACK;
		int foreground=Color.WHITE;
		textpaint = new Paint();
		textpaint.setAntiAlias(true);
		textpaint.setStrokeWidth(5);
		textpaint.setColor(foreground);
		textpaint.setStrokeCap(Paint.Cap.ROUND);
		textpaint.setTextSize(16);
		textpaint.setTypeface(Typeface.create(Typeface.SANS_SERIF,
                                           Typeface.NORMAL));

		erasepaint= new Paint();
		erasepaint.setStyle(Style.FILL);
		erasepaint.setColor(background);
		
	}
	/**
	 * Call at start of each frame. This does not clear the cache,
	 * but marks each item as _clearable_.
	 */
	void startframe()
	{
		framecount=0;
		for(Slot slot:string2slot.values())
			slot.used=false;
	}
	/**
	 * Call this for all strings you will draw in the scene.
	 * Then actually draw all strings in the scene.
	 * Calling this before each draw, will work but be inefficient.
	 * Call startframe at start of scene frame.
	 */
	void addString(String s)
	{
		if (framecount>=capacity) throw new RuntimeException("No capacity left");
		Slot slot=string2slot.get(s);
		if (slot!=null) 
		{
			slot.used=true;
			return; //already present
		}
		boolean found=false;
		for(int i=0;i<capacity;++i)
		{
			slot=slots.get(i);
			if (slot.used) continue;
			slot.used=true;
			if (slot.text!=null)
				string2slot.remove(slot.text);
			slot.text=s;
			string2slot.put(s, slot);
			render(i*16,s);
			dirty=true;
			found=true;
			break;
		}		
		if (!found)
			throw new RuntimeException("Too many strings in scene");
	}
	private void render(int y, String s) {
		canvas.drawRect(0,y,512, y+16,erasepaint);
		canvas.drawText(s, 0, y, textpaint);
	}
	void reloadTexture(GL10 gl)
	{
		if (tex<0)
		{
			tex=TextureHelpers.loadTexture(this.b, gl);
		}
		else
		{
			TextureHelpers.reloadTexture(b,gl,tex);
		}
	}
	void drawString(GL10 gl,String s)
	{
		dirty=false;
		Slot slot=string2slot.get(s);
		if (slot==null) throw new RuntimeException("You must have called addString before calling drawString");
		if (dirty)
			reloadTexture(gl);
	}
	
	
}
