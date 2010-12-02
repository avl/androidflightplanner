package se.flightplanner.map3d;


import javax.microedition.khronos.opengles.GL10;


import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.util.Log;

import se.flightplanner.AirspaceArea;
import se.flightplanner.SpaceStats;
import se.flightplanner.map3d.GuiState.DrawOrder;
import se.flightplanner.map3d.GuiState.DrawPos;
import se.flightplanner.map3d.ObserverContext.ObserverState;

public class GuiDrawer {
	FontBitmapCache fc;
	SimpleGeometry simgem;
	int retreat;
	public GuiDrawer()
	{
		fc=new FontBitmapCache();
		retreat=0;
	}
	public void draw(GuiState state,GL10 gl,ObserverState obs,int width,int height)
	{
		simgem=new SimpleGeometry(100);
		DrawOrder draw=state.getDrawOrder();
		if (draw==null)
			return;
		fc.startframe();
		simgem.reset();
		int idx=0;
		if (!draw.retreating)
			retreat=0;
		if (draw.retreating && retreat<width)
			retreat+=10+retreat/4;
		gl.glEnable(gl.GL_SCISSOR_TEST);
		//gl.glDisable(gl.GL_SCISSOR_TEST);
		gl.glDisable(gl.GL_DEPTH_TEST);
		//fc.addString("HEJ");
		//fc.drawString(10,10,32,"HEJ",width,height);
		gl.glScissor(0, height/5, width, (3*height)/5);
		/*idx=0;
		
		for(AirspaceArea area:draw.spaces)
		{
			DrawPos pos=draw.get(idx);//scroll_y;
			int y=pos.y;
			idx+=1;
			if (y>height) continue;
			if (y<-pos.size) continue;
			//preload all needed
			Log.i("fplan","Adding string #"+idx+" : "+area.name);
			fc.addString(area.name);
		}*/
		idx=0;
		long now = SystemClock.uptimeMillis();
		for(AirspaceArea area:draw.spaces)
		{
			DrawPos pos=draw.get(idx);//scroll_y;
			idx+=1;
			int x=pos.x+retreat;
			int y=pos.y;
			if (x>width) break;				
			if (y>height) continue;
			if (y<-pos.size) continue;
			SpaceStats stats=area.dyninfo;
			if (stats==null || now-stats.updated>100)
				area.dyninfo = stats = SpaceStats.getStats(obs.pos, area);
			float relbearing=stats.bearing-obs.heading;
			simgem.putArrow(x,y+pos.size/2,pos.size/2.5f,relbearing/(180.0f/(float)Math.PI),width,height);
			fc.drawString(x+15,y,pos.size, ""+stats.bearing+" "+area.name,width,height);
		}
		simgem.draw(gl,width,height);
		fc.draw(gl,width,height);		
		 
		if (draw.selected!=null)
		{
			//Log.i("fplan","Drawing selected area: "+draw.selected.name);
			AirspaceArea area=draw.selected;
			
			SpaceStats stats = area.dyninfo;
			if (stats==null || now-stats.updated>100)
				area.dyninfo = stats = SpaceStats.getStats(obs.pos, area);
			
			fc.drawString(0, 0, 24,String.format("%s: %s",stats.diststr,area.name),width,height);
			StringBuilder freqs=new StringBuilder();
			for(String freq:area.freqs)
				freqs.append(freq);
			fc.drawString(0,24,24,area.floor+"-"+area.ceiling+" "+freqs.toString(),width,height);			
		}
		
		gl.glScissor(0, 0, width, height); ///see: http://code.google.com/p/android/issues/detail?id=3047
		gl.glScissor(0, 0, width, height/5);
		gl.glDisable(gl.GL_SCISSOR_TEST);
		fc.draw(gl,width,height);		

		gl.glScissor(0, 0, width, height); ///see: http://code.google.com/p/android/issues/detail?id=3047
		gl.glDisable(gl.GL_SCISSOR_TEST);
		gl.glEnable(gl.GL_DEPTH_TEST);
	}
}
