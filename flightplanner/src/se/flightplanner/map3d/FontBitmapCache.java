package se.flightplanner.map3d;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.HashMap;

import javax.microedition.khronos.opengles.GL10;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.Paint.Align;
import android.graphics.Paint.FontMetrics;
import android.graphics.Paint.Style;
import android.util.Log;

public class FontBitmapCache {

	Bitmap b;
	Canvas canvas;
	Paint textpaint;
	Paint erasepaint;
	int tex;
	private FloatBuffer vertexbuf; ///TODO: We could reuse these buffers from the Terrain drawer, and save some memory or gc pressure.
	private FloatBuffer texcoordbuf;
	private ShortBuffer indexbuf;
	private ByteBuffer colorbuf;
	int index=0;
	int vertex=0;
	final int indexcapacity=500;
	final int vertcapacity=500;
	int texsizex,texsizey;
	
	private static class Slot
	{
		String text;
		int idx;
		int xsize;
		boolean used;
	}
	int framecount;
	int capacity;
	boolean dirty;
	final int fontsize=16;
	ArrayList<Slot> slots;
	HashMap<String,Slot> string2slot;
	public FontBitmapCache()
	{
		capacity=32;
		if (capacity!=16 && capacity!=32)
			throw new RuntimeException("Only 16 and 32 are acceptable capacities");
		texsizex=512;
		texsizey=fontsize*capacity;
		b=Bitmap.createBitmap(texsizex,texsizey,Bitmap.Config.ARGB_8888);
		canvas=new Canvas(b);
		string2slot=new HashMap<String, Slot>();
		slots=new ArrayList<Slot>();
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
		textpaint.setTextAlign(Align.LEFT);
		textpaint.setTextSize((fontsize*2)/3);
		textpaint.setTypeface(Typeface.create(Typeface.SANS_SERIF,
                                           Typeface.NORMAL));

		erasepaint= new Paint();
		erasepaint.setStyle(Style.FILL);
		erasepaint.setColor(background);
		
		ByteBuffer bytebuf= ByteBuffer.allocateDirect(vertcapacity*3*4);
		bytebuf.order(ByteOrder.nativeOrder());
		vertexbuf=bytebuf.asFloatBuffer();

		bytebuf= ByteBuffer.allocateDirect(vertcapacity*2*4);
		bytebuf.order(ByteOrder.nativeOrder());
		texcoordbuf=bytebuf.asFloatBuffer();
		
		bytebuf= ByteBuffer.allocateDirect(indexcapacity*2);
		bytebuf.order(ByteOrder.nativeOrder());
		indexbuf=bytebuf.asShortBuffer();
		
		colorbuf=ByteBuffer.allocateDirect(vertcapacity*4);

		vertexbuf.position(0);
		texcoordbuf.position(0);
		colorbuf.position(0);
		vertex=0;
		indexbuf.position(0);
		index=0;
		tex=-1;

	}
	private void putstring(int x,int y,int idx,int textsize,int width,int height) {
		if (vertex>vertcapacity-4)
			return;
		if (index>indexcapacity-6)
			return;
		Slot slot=slots.get(idx);
		int dx=(slot.xsize*textsize)/fontsize;
		int dy=textsize;
		int tx1=0;
		int tx2=slot.xsize;
		int ty1=fontsize*idx;
		int ty2=fontsize*(idx+1);
		int a=vput(x,y,tx1,ty1,width,height);
		int b=vput(x,y+dy,tx1,ty2,width,height);
		int c=vput(x+dx,y,tx2,ty1,width,height);
		int d=vput(x+dx,y+dy,tx2,ty2,width,height);
		tri(a,b,c);
		tri(b,d,c);
	}
	private void tri(int a,int b,int c)
	{
		if(!(a<32000 && b<32000 && c<32000))
			throw new RuntimeException("Bad indices in tri()");
		
		indexbuf.put((short)a);
		indexbuf.put((short)b);
		indexbuf.put((short)c);
		///Log.i("fplan","Tri with end index: #"+index+" "+a+" "+b+" "+c);
		index+=3;
	}
	
	

	private int vput(int x,int y,int u,int v,int width,int height)
	{
		vertexbuf.put(x-(width>>1));
		vertexbuf.put((height>>1)-y);
		vertexbuf.put(1.0f);
		texcoordbuf.put(u/((float)texsizex)+1/((float)4*texsizex));
		texcoordbuf.put(v/((float)texsizey)+1/((float)4*texsizey));
		colorbuf.put((byte)-1);
		colorbuf.put((byte)-1);
		colorbuf.put((byte)-1);
		colorbuf.put((byte)-1);
		///Log.i("fplan","Making vertex #"+vertex+" "+x+","+y+", u="+u+",v="+v);
		int ret=vertex;
		vertex+=1;
		return ret;
	}
	
	/**
	 * Call at start of each frame. This does not clear the cache,
	 * but marks each item as _clearable_.
	 */
	public void startframe()
	{
		framecount=0;
		for(Slot slot:string2slot.values())
			slot.used=false;
		resetdraw();
	}
	private void resetdraw()
	{
		index=0;
		vertex=0;
		vertexbuf.position(0);
		texcoordbuf.position(0);
		colorbuf.position(0);
		indexbuf.position(0);
	}
	/**
	 * Call this for all strings you will draw in the scene.
	 * Then actually draw all strings in the scene.
	 * Calling this before each draw, will work but be inefficient.
	 * Call startframe at start of scene frame.
	 */
	private Slot addString(String s)
	{
		if (framecount>=capacity) throw new RuntimeException("No capacity left");
		Slot slot=string2slot.get(s);
		if (slot!=null) 
		{
			slot.used=true;
			return slot; //already present
		}
		for(int i=0;i<capacity;++i)
		{
			slot=slots.get(i);
			if (slot.used) continue;
			slot.used=true;
			if (slot.text!=null)
				string2slot.remove(slot.text);
			slot.text=s;
			string2slot.put(s, slot);
			render(slot,i*fontsize,s);
			return slot;
		}		
		throw new RuntimeException("Too many strings in scene");
	}
	private void render(Slot slot,int y, String s) {
		canvas.drawRect(0,y,texsizex, y+fontsize,erasepaint);
		Rect bounds=new Rect();
		textpaint.getTextBounds(s, 0, s.length(), bounds);
		FontMetrics fm=textpaint.getFontMetrics();
		//float pitch=Math.abs(fm.top)+Math.abs(fm.bottom);
		canvas.drawText(s, 0, y+Math.abs(fm.top), textpaint);
		slot.xsize=bounds.right;
		Log.i("fplan","Rendered string "+s+" to bitmap");
		dirty=true;
		
	}
	public void reloadTexture(GL10 gl)
	{
		if (tex<0)
		{
			tex=TextureHelpers.loadTexture(this.b, gl);
			Log.i("fplan","Loading texture from bitmap: "+tex);
		}
		else
		{
			TextureHelpers.reloadTexture(b,gl,tex);
			Log.i("fplan","reloading texture from bitmap: "+tex);
		}
	}
	
	public void drawString(int x,int y,int textsize,String s,int width,int height)
	{
		Slot slot=addString(s);
		if (slot==null) throw new RuntimeException("You must have called addString before calling drawString");
		putstring(x,y,slot.idx,textsize,width,height);
	}
	
	int cnt=0;
	@SuppressWarnings("static-access")
	void draw(GL10 gl,int width,int height)
	{		
		if (dirty)
		{
			cnt++;
			//reloading the existing texture seems to crash things...
			reloadTexture(gl);
			dirty=false;
		}
		GlHelper.checkGlError(gl);
		gl.glMatrixMode(GL10.GL_PROJECTION);
		gl.glLoadIdentity();
		float[] matrix=new float[]{
				2.0f/width,0,0,0,
				0,2.0f/height,0,0,
				0,0,1,0,
				0,0,0,1,
		};
		gl.glMultMatrixf(matrix,0);
		gl.glMatrixMode(GL10.GL_MODELVIEW);
		gl.glLoadIdentity();
				
		GlHelper.checkGlError(gl);
		gl.glEnable(GL10.GL_TEXTURE_2D);
		gl.glBindTexture(GL10.GL_TEXTURE_2D, tex);
		GlHelper.checkGlError(gl);
		indexbuf.position(0);
		texcoordbuf.position(0);
		vertexbuf.position(0);
		colorbuf.position(0);
		gl.glVertexPointer(3, gl.GL_FLOAT, 0, vertexbuf);
		gl.glColorPointer(4, gl.GL_UNSIGNED_BYTE,0, colorbuf);
		gl.glTexCoordPointer(2,gl.GL_FLOAT,0,texcoordbuf);
		gl.glDrawElements(gl.GL_TRIANGLES, index, gl.GL_UNSIGNED_SHORT,indexbuf);		
		GlHelper.checkGlError(gl);
		resetdraw();
	}

}
