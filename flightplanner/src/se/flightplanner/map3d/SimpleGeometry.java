package se.flightplanner.map3d;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.opengles.GL10;

public class SimpleGeometry {
	private FloatBuffer vertexbuf; ///TODO: We could reuse these buffers from the Terrain drawer, and save some memory or gc pressure.
	private ShortBuffer indexbuf;
	private ByteBuffer colorbuf;
	int vertex,index;
	int vertcapacity;
	int indexcapacity;
	public SimpleGeometry(int capacity)
	{
		vertcapacity=3*capacity;
		indexcapacity=3*capacity;
		ByteBuffer bytebuf= ByteBuffer.allocateDirect(vertcapacity*3*4);
		bytebuf.order(ByteOrder.nativeOrder());
		vertexbuf=bytebuf.asFloatBuffer();

		bytebuf= ByteBuffer.allocateDirect(vertcapacity*2*4);
		bytebuf.order(ByteOrder.nativeOrder());
		
		bytebuf= ByteBuffer.allocateDirect(indexcapacity*2);
		bytebuf.order(ByteOrder.nativeOrder());
		indexbuf=bytebuf.asShortBuffer();
		
		colorbuf=ByteBuffer.allocateDirect(vertcapacity*4);

		vertexbuf.position(0);
		colorbuf.position(0);
		vertex=0;
		indexbuf.position(0);
		index=0;		
	}
	private void tri2d(int a,int b,int c)
	{
		if(!(a<32000 && b<32000 && c<32000))
			throw new RuntimeException("Bad indices in tri()");
		
		indexbuf.put((short)a);
		indexbuf.put((short)b);
		indexbuf.put((short)c);
		///Log.i("fplan","Tri with end index: #"+index+" "+a+" "+b+" "+c);
		index+=3;
	}
	private int vput2d(int x,int y,int width,int height,byte r,byte g,byte b)
	{
		vertexbuf.put(x-(width>>1));
		vertexbuf.put((height>>1)-y);
		vertexbuf.put(1.0f);
		colorbuf.put(r);
		colorbuf.put(g);
		colorbuf.put(b);
		colorbuf.put((byte)-1);
		///Log.i("fplan","Making vertex #"+vertex+" "+x+","+y+", u="+u+",v="+v);
		int ret=vertex;
		vertex+=1;
		return ret;
	}
	public void putArrow(int x,int y,float size,float angle,int width,int height)
	{
		int apex_x=(int)(size*(float)Math.sin(angle));	
		int apex_y=-(int)(size*(float)Math.cos(angle));	
		int l_x=(int)(size*(float)Math.sin(angle-2.5f));	
		int l_y=-(int)(size*(float)Math.cos(angle-2.5f));	
		int r_x=(int)(size*(float)Math.sin(angle+2.5f));	
		int r_y=-(int)(size*(float)Math.cos(angle+2.5f));
		int sapex_x=(int)(size*0.5f*(float)Math.sin(angle));	
		int sapex_y=-(int)(size*0.5f*(float)Math.cos(angle));	
		int sl_x=(int)(size*0.85f*(float)Math.sin(angle-2.8f));	
		int sl_y=-(int)(size*0.85f*(float)Math.cos(angle-2.8f));	
		int sr_x=(int)(size*0.85f*(float)Math.sin(angle+2.8f));	
		int sr_y=-(int)(size*0.85f*(float)Math.cos(angle+2.8f));
		putTriangle2D(
				x+apex_x,y+apex_y,
				x+l_x,y+l_y,
				x+r_x,y+r_y,width,height,(byte)-1,(byte)-1,(byte)-1);
		putTriangle2D(
				x+sapex_x,y+sapex_y,
				x+sl_x,y+sl_y,
				x+sr_x,y+sr_y,width,height,(byte)0,(byte)0,(byte)0);
				
		
	}
	public void putTriangle2D(int x1,int y1,int x2,int y2,int x3,int y3,int width,int height,byte r,byte g,byte b)
	{
		if (vertex+3>=vertcapacity)
			throw new RuntimeException("Out of vertices");
		if (index+3>=indexcapacity)
			throw new RuntimeException("Out of indices");
		int i1=vput2d(x1,y1,width,height,r,g,b);
		int i2=vput2d(x2,y2,width,height,r,g,b);
		int i3=vput2d(x3,y3,width,height,r,g,b);
		tri2d(i1,i2,i3);
	}
	public void reset()
	{
		index=0;
		vertex=0;
		vertexbuf.position(0);
		colorbuf.position(0);
		indexbuf.position(0);
	}
	@SuppressWarnings("static-access")
	public void draw(GL10 gl,int width,int height)
	{		
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
		gl.glDisable(GL10.GL_TEXTURE_2D);
		GlHelper.checkGlError(gl);
		indexbuf.position(0);
		vertexbuf.position(0);
		colorbuf.position(0);
		gl.glVertexPointer(3, gl.GL_FLOAT, 0, vertexbuf);
		gl.glColorPointer(4, gl.GL_UNSIGNED_BYTE,0, colorbuf);
		gl.glDrawElements(gl.GL_TRIANGLES, index, gl.GL_UNSIGNED_SHORT,indexbuf);		
		GlHelper.checkGlError(gl);
		reset();
	}
	
}
