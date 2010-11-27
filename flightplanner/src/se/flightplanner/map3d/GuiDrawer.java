package se.flightplanner.map3d;

import javax.microedition.khronos.opengles.GL10;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.Log;

import se.flightplanner.AirspaceArea;
import se.flightplanner.Project;
import se.flightplanner.map3d.GuiState.DrawOrder;
import se.flightplanner.map3d.GuiState.DrawPos;
import se.flightplanner.map3d.ObserverContext.ObserverState;
import se.flightplanner.vector.Vector;
import se.flightplanner.vector.Polygon.InsideResult;

public class GuiDrawer {
	FontHandler fh;
	int retreat;
	public GuiDrawer(FontHandler fh)
	{
		this.fh=fh;
		retreat=0;
	}
	public void draw(GuiState state,GL10 gl,ObserverState obs,int width,int height)
	{
		DrawOrder draw=state.getDrawOrder();
		if (draw==null)
			return;
		fh.clear();
		int idx=0;
		if (!draw.retreating)
			retreat=0;
		if (draw.retreating && retreat<width)
			retreat+=10+retreat/4;
		//gl.glViewport(0,height/5,width,(3*height)/5);
		gl.glEnable(gl.GL_SCISSOR_TEST);
		gl.glDisable(gl.GL_DEPTH_TEST);
		
		gl.glScissor(0, height/5, width, (3*height)/5);
		idx=0;
		for(AirspaceArea area:draw.spaces)
		{
			DrawPos pos=draw.get(idx);//scroll_y;
			int y=pos.y;
			idx+=1;
			if (y>height) continue;
			if (y<-pos.size) continue;
			//preload all needed
			inittextureoffset(area.name);
		}
		idx=0;
		for(AirspaceArea area:draw.spaces)
		{
			DrawPos pos=draw.get(idx);//scroll_y;
			idx+=1;
			int x=pos.x+retreat;
			int y=pos.y;
			if (x>width) break;				
			if (y>height) continue;
			if (y<-pos.size) continue;
			int tex=obtaintextureoffset(area.name);
			fh.specialputstring(x,y,tex,pos.size,width,height);
			//fh.putstring(x, y, area.name,pos.size,width,height);
			//fh.putstring(50, 50, "Hejsan",width,height);
		}
		fh.draw(gl,width,height);		
		 
		if (draw.selected!=null)
		{
			fh.clear();
			Log.i("fplan","Drawing selected area: "+draw.selected.name);
			AirspaceArea area=draw.selected;
			Vector pos=obs.pos.toVector();
			InsideResult res=area.poly.inside(pos);
			String diststr;
			if (!res.isinside)
			{
				Vector close=area.poly.closest(pos);
				double nmdist=close.minus(pos).length()/Project.approx_scale(pos.y, 13, 1.0);
				diststr=String.format("%.0fnm", nmdist);
			}
			else
			{
				diststr="inside";
			}
			
			
			gl.glScissor(0, 0, width, height); ///see: http://code.google.com/p/android/issues/detail?id=3047
			//gl.glScissor(0, 0, width, height/5);
			fh.putstring(0, 0, String.format("%s: %s",diststr,area.name),16,width,height);
			StringBuilder freqs=new StringBuilder();
			for(String freq:area.freqs)
				freqs.append(freq);
			fh.putstring(0,16,area.floor+"-"+area.ceiling+" "+freqs.toString(),16,width,height);
			
			fh.draw(gl,width,height);		
		}
		

		gl.glScissor(0, 0, width, height); ///see: http://code.google.com/p/android/issues/detail?id=3047
		gl.glDisable(gl.GL_SCISSOR_TEST);
		gl.glEnable(gl.GL_DEPTH_TEST);
	}
	private int obtaintextureoffset(String name) {
		// TODO Auto-generated method stub
		return 0;
	}
}
