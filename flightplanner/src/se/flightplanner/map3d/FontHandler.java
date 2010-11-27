package se.flightplanner.map3d;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.opengles.GL10;

import android.graphics.Bitmap;
import android.util.Log;

public class FontHandler {
	int tex;
	Bitmap fontbitmap;
	private FloatBuffer vertexbuf; ///TODO: We could reuse these buffers from the Terrain drawer, and save some memory or gc pressure.
	private FloatBuffer texcoordbuf;
	private ShortBuffer indexbuf;
	private ByteBuffer colorbuf;
	int index=0;
	int vertex=0;
	final int indexcapacity=10000;
	final int vertcapacity=10000;
	public FontHandler(Bitmap fontbitmap)
	{
				

		Log.i("fplan","Loaded font bitmap "+fontbitmap.getWidth()+"x"+fontbitmap.getHeight());
		this.fontbitmap=fontbitmap;
		tex=-1;
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
	
	}
	private void tri(int a,int b,int c)
	{
		if(!(a<32000 && b<32000 && c<32000))
			throw new RuntimeException("Bad indices in tri()");
		
		indexbuf.put((short)a);
		indexbuf.put((short)b);
		indexbuf.put((short)c);
		index+=3;
	}

	private int vput(int x,int y,int u,int v,int width,int height)
	{
		vertexbuf.put(x-(width>>1));
		vertexbuf.put((height>>1)-y);
		vertexbuf.put(1.0f);
		texcoordbuf.put(u/256.0f+1/1024.0f);
		texcoordbuf.put(v/256.0f+1/1024.0f);
		colorbuf.put((byte)-1);
		colorbuf.put((byte)-1);
		colorbuf.put((byte)-1);
		colorbuf.put((byte)-1);
		int ret=vertex;
		vertex+=1;
		return ret;
	}
	public void putchar(int x,int y,char code,int textsize,int width,int height) {
		int idx=(int)code;
		if (vertex>vertcapacity-4)
			return;
		if (index>indexcapacity-6)
			return;
		if (idx<0 || idx>=256) idx=0;
		int dx=(textsize*12)/16;
		int dy=textsize;
		int tx=(idx%16)*16;
		int ty=(idx/16)*16;
		int a=vput(x,y,tx,ty,width,height);
		int b=vput(x,y+dy,tx,ty+16,width,height);
		int c=vput(x+dx,y,tx+12,ty,width,height);
		int d=vput(x+dx,y+dy,tx+12,ty+16,width,height);
		tri(a,b,c);
		tri(b,d,c);
	}
	public void putstring(int x,int y,String what,int textsize,int width,int height) {
		for(char code : what.toCharArray())
		{
			putchar(x,y,code,textsize,width,height);
			x+=(textsize*12)/16;
		}
	}
	@SuppressWarnings("static-access")
	void draw(GL10 gl,int width,int height)
	{
		
		GlHelper.checkGlError(gl);
		if (tex==-1) loadTexture(gl);
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
		
		/*int code=0;
		for(int iy=0;iy<60;++iy)
			for(int i=0;i<10;++i)
			{
				putchar(i*16,iy*16,(char)code);
				++code;
			}
		*/
		//clear();
		//putstring(0,0,"ABC",width,height);
		//putstring(0,20,"ABCÅÄÖ",width,height);
		
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

		//clear();
	}
	public void clear()
	{
		vertexbuf.position(0);
		texcoordbuf.position(0);
		colorbuf.position(0);
		vertex=0;
		indexbuf.position(0);
		index=0;		
	}
	public void loadTexture(GL10 gl)
	{
		if (tex!=-1)
			TextureHelpers.unloadTexture(gl,tex);
		tex=TextureHelpers.loadTexture(fontbitmap,gl);		
	}
	
}
