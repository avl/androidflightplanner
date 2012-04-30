package se.flightplanner.simpler;

import java.util.ArrayList;

import se.flightplanner.AirspaceArea;
import se.flightplanner.AirspaceLookupIf;
import se.flightplanner.Project;
import se.flightplanner.Project.LatLon;
import se.flightplanner.Project.Merc;
import se.flightplanner.simpler.AirspaceLayout.Cell;
import se.flightplanner.simpler.AirspaceLayout.Measurer;
import se.flightplanner.simpler.AirspaceLayout.Row;
import se.flightplanner.simpler.AirspaceLayout.Rows;
import se.flightplanner.simpler.Common;
import se.flightplanner.simpler.Common.Compartment;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class SimplerView extends View {

	static public interface ViewOwner
	{
		public void touched();
	}
	ViewOwner owner;
	private AirspaceLookupIf lookup;
	private LatLon pos;
	private float hdg;
	private float gs;
	private Paint txtpaint;
	private Paint smalltxtpaint;
	private Paint boxpaint;
	private Paint boxframepaint;
	static final private int border=3;
	static final private int margins=10;
	private boolean needlayout=true;
	static private Common.Rect rect2rect(Rect r)
	{
		return new Common.Rect(r.left,r.top,r.right,r.bottom);
	}
	
	public SimplerView(Context context,AirspaceLookupIf lookup,LatLon initial_pos,float initial_heading,float init_gs,ViewOwner owner) {
		super(context);
		this.owner=owner;
		this.lookup=lookup;
		this.pos=initial_pos;
		this.hdg=initial_heading;
		this.gs=init_gs;
		txtpaint=new Paint();
		txtpaint.setTextSize(22);
		txtpaint.setAntiAlias(true);
		smalltxtpaint=new Paint();
		smalltxtpaint.setTextSize(17);
		smalltxtpaint.setAntiAlias(true);
		boxpaint=new Paint();
		boxpaint.setStyle(Style.FILL);
		
		boxframepaint=new Paint();
		boxframepaint.setStyle(Style.STROKE);
		doupdate();
	}
	private AirspaceLayout layout;
	void doupdate()
	{
		FindNearby nb=new FindNearby(lookup,pos,hdg);
		
		layout=new AirspaceLayout(new Measurer()		{
			@Override
			public Common.Rect measure(AirspaceArea area) {
				Rect r1=new Rect();
				
				txtpaint.getTextBounds(area.name, 0, area.name.length(), r1);
				r1.offset(-r1.left, -r1.top);
				r1.left-=margins;
				r1.right+=margins;
				
				Common.Rect ret=rect2rect(r1);				
				String alts=getalts(area);
				txtpaint.getTextBounds(alts, 0, alts.length(), r1);
				r1.offset(-r1.left, -r1.top);
				r1.left-=margins;
				r1.right+=margins;
				if (r1.width()>ret.width())
					ret.right=r1.right;
				ret.bottom+=r1.bottom;
				return ret;
			}
		},nb);

		needlayout=true;
	}
	
	private static class CompartmentData
	{
		public Compartment comp;
		public int x;
		public int y;
		public int rot;
		public int xsize;
		public int ysize;
		public String human;
		public CompartmentData(Compartment comp,int x,int y,int rot,int xsize,int ysize,String human)
		{
			this.comp=comp;
			this.x=x;
			this.y=y;
			this.rot=rot;
			this.xsize=xsize;
			this.ysize=ysize;
			this.human=human;
		}
	}
	private String getalts(AirspaceArea a)
	{
		return ""+a.floor+"-"+a.ceiling;
	}
	
	private enum State
	{
		IDLE,
		FINGER_DOWN,		
	}
	private State state=State.IDLE;
	private float downx,downy;
	
	@Override
	public boolean onTouchEvent(MotionEvent ev)
	{
		
		float x=ev.getX();
		float y=ev.getY();
		
		if (ev.getAction()==MotionEvent.ACTION_DOWN ||
				ev.getAction()==MotionEvent.ACTION_MOVE)
		{
			owner.touched();
			if (state==State.IDLE)
			{
				downx=x;downy=y;
				state=State.FINGER_DOWN;
			}
		}
		else if (ev.getAction()==MotionEvent.ACTION_UP)
		{
			owner.touched();
			if (state==State.FINGER_DOWN)
			{
				if (x-downx>20 || y-downy<-20)
				{
					clear(downx,downy,false);
				}
				else if (x-downx<-20 || y-downy>20)
				{
					clear(downx,downy,true);
				}
			}
			state=State.IDLE;
		}
		return true;
	}

	private void clear(float x, float y, boolean cleared) {
		AirspaceArea area=findSpace(x,y);
		if (area!=null)
		{
			if (area.cleared!=cleared)
				invalidate();
			area.cleared=cleared;
		}
		
	}
	private AirspaceArea findSpace(float x, float y) {
		float closest_dist=1e10f;
		AirspaceArea closest=null;
		for(Position p:positions)
		{
			//Log.i("fplan","Checking if "+p.area.name+" at "+p.rect+" is hit by "+x+","+y);
			
			if (p.rect.contains((int)x, (int)y))
			{
				return p.area;
			}
			float dist=0;
			if (y<p.rect.top) dist+=p.rect.top-y;
			if (x<p.rect.left) dist+=p.rect.left-x;
			if (y>p.rect.bottom) dist+=y-p.rect.bottom;
			if (x>p.rect.right) dist+=x-p.rect.right;
			if (dist<closest_dist)
			{
				closest_dist=dist;
				closest=p.area;
			}
		}
		if (closest_dist<100)
			return closest;
		else
			return null;
	}
	
	static private class Position
	{
		AirspaceArea area;
		Rect rect;
		
	}
	private ArrayList<Position> positions=new ArrayList<Position>();
	private int lastheight,lastwidth;
	@Override
	protected void onDraw(Canvas canvas) {
		//Log.i("fplan.al","Hdg:"+hdg);
		int width=getRight()-getLeft();
		int height=getBottom()-getTop();
		
		if (width!=lastwidth || height!=lastheight) 
			needlayout=true;
		lastwidth=width;
		lastheight=height;
		
		float characteristic_distance=6;
		if (gs>1)
		{
			characteristic_distance=gs/12.0f;
		}
		if (characteristic_distance<6)
			characteristic_distance=6;
		Merc mercpos=Project.latlon2merc(pos, 13);
		
		
		int h2=height/2;
		int w3=width/3;
		int ahead_xsize=width;
		int left_xsize=h2;
		int right_xsize=h2;
		int present_xsize=w3;
		float scalefactor=(float)Project.approx_scale(mercpos.y, 13, 1);
		
		if (needlayout)
		{
			layout.update(left_xsize, ahead_xsize, right_xsize, present_xsize);
			positions.clear();
		}
		
		
		for(CompartmentData comp:new CompartmentData[]{
				new CompartmentData(Compartment.AHEAD,0,0,0, width,h2,"Ahead"),
				new CompartmentData(Compartment.LEFT,0,height,270, h2,w3,"Left"),
				new CompartmentData(Compartment.RIGHT,width,h2,90, h2,w3,"Right"),
				new CompartmentData(Compartment.PRESENT,w3,h2,0, w3,h2,"Inside Airspace")})
		{
			Rows rows=layout.getRows(comp.comp);
			Matrix mat=new Matrix();
			canvas.save();
			mat.preRotate(comp.rot, comp.x, comp.y);
			canvas.rotate(comp.rot,comp.x,comp.y);
			mat.preTranslate(comp.x,comp.y);
			canvas.translate(comp.x, comp.y);
			int cury=comp.ysize;
			int nexty=Integer.MAX_VALUE;
			
			
			
			float last_distrow_distance=-1;
			
			
			for(int i=0;i<rows.rows.size();++i)
			{
				Row row=rows.rows.get(i);
				float closest_dist=1e20f;
				float closest_uncleared_dist=1e20f;
				for(Cell cell:row.cells)
				{
					float nm=(float)cell.area.distance/scalefactor;
					if (nm<closest_dist)
						closest_dist=nm;
					if (!cell.area.area.cleared && nm<closest_uncleared_dist)
						closest_uncleared_dist=nm;
					
				}			
				boolean has_range_box=false;
				if (needDistanceRow(closest_dist,last_distrow_distance))
				{
					float orangeness=0;
					last_distrow_distance=closest_dist;
					Common.Rect rect=new Common.Rect(0,0,comp.xsize,15);
					String desc;
					if (comp.comp==Compartment.PRESENT)
					{
						desc=comp.human;
					}
					else
					{
						orangeness=1.0f-(closest_uncleared_dist-1.0f)/3.0f;
						desc=String.format("%.1fNM+ "+comp.human,closest_dist);
						if (gs>1)
						{
							float time=(60.0f*closest_dist/gs);							
							desc=desc+String.format(" (%d min)",(int)time);
							
							float time2=(60.0f*closest_uncleared_dist/gs);							
							orangeness=1.0f-(time2-1.5f)/3.0f;
						}
					}
					if (orangeness>1) orangeness=1;
					if (orangeness<0) orangeness=0;
					float r=orangeness*(255)+(1.0f-orangeness)*255;
					float g=orangeness*(90)+(1.0f-orangeness)*255;
					float b=orangeness*(0)+(1.0f-orangeness)*255;
					
					
					cury=drawBox(canvas,cury,rect,"^ "+desc+" ^",(String)null,(int)r,(int)g,(int)b,smalltxtpaint);
					has_range_box=true;
				}
								
				for(Cell cell:row.cells)
				{
					AirspaceArea area=cell.area.area;
					float nm=(float)cell.area.distance/scalefactor;
					String alts=getalts(area);
					
					int off;
					if (has_range_box)
					{
						if (Math.abs(nm-closest_dist)<0.75)
							off=0;
						else
							off=5;
					}
					else
						off=0;
					int r=area.r;
					int g=area.g;
					int b=area.b;
					if (area.cleared)
					{
						r=g=b=0xc0;
					}
					int boxy2=drawBox(canvas,cury-off,cell.rect,alts,area.name,r,g,b,txtpaint);
					nexty=Math.min(nexty,boxy2);
					
					if (needlayout)
					{
						RectF rect=new RectF(cell.rect.left,boxy2,cell.rect.right,cury);
						//Log.i("fplan","Storing position untransformed:"+cell.area.area.name+": "+rect);
						mat.mapRect(rect);
						//Log.i("fplan","    - transformed:"+rect);
						storePosition(area,rect);						
					}
					
				}
				if (nexty!=Integer.MAX_VALUE)
					cury=nexty;
			}
			
			canvas.restore();
		}
		
		
		needlayout=false;
		
		
		
	}
	private void storePosition(AirspaceArea area, RectF rect) {
		Position p=new Position();
		p.rect=new Rect();
		p.rect.left=(int)rect.left;
		p.rect.right=(int)rect.right;
		p.rect.top=(int)rect.top;
		p.rect.bottom=(int)rect.bottom;
		p.area=area;
		positions.add(p);
		
	}
	private boolean needDistanceRow(float distance,float last_distrow_distance) {
		if (last_distrow_distance<0) return true;
		return distance>last_distrow_distance*2;
	}
	private int drawBox(Canvas canvas, int cury, Common.Rect cellrect,String label,String label2,int r,int g,int b,Paint usetxtpaint) {
		Rect re=new Rect();
		re.left=cellrect.left+border;
		re.right=cellrect.right-border;
		re.bottom=cury-border;
		
		Rect drawrect1=new Rect();
		usetxtpaint.getTextBounds(label, 0, label.length(), drawrect1);		
		Rect drawrect2=new Rect();
		int h;
		if (label2!=null)
		{
			usetxtpaint.getTextBounds(label2, 0, label2.length(), drawrect2);
			h=drawrect1.height()+drawrect2.height()+10;
		}
		else
		{
			h=drawrect1.height();
		}		
		re.top=cury-h-border;
		//AirspaceArea area=cell.area.area;
		boxpaint.setColor(rgb(r,g,b,-140));
		canvas.drawRect(re, boxpaint);
		boxframepaint.setColor(rgb(r,g,b,0));
		boxframepaint.setStrokeWidth(border);
		canvas.drawRect(re, boxframepaint);
		usetxtpaint.setColor(Color.WHITE);
		canvas.save();
		canvas.clipRect(re);
		for(int i=0;i<2;++i)
		{
			Rect drawrect;
			///Rect re2;
			int rew=re.width();
			int reh;
			int yoff;
			String text;
			if (i==0)
			{
				drawrect=drawrect1;
				
				if (label2==null)
					reh=re.height();
				else
					reh=re.height()/2;
				yoff=0;
				text=label;
			}
			else
			{
				if (label2==null) break;
				drawrect=drawrect2;
				reh=re.height()/2;
				yoff=reh;
				text=label2;
			}
			int drw=drawrect.width();
			int drh=drawrect.height();
			canvas.drawText(text, cellrect.left-drawrect.left+(rew-drw)/2, cury-drawrect.bottom-(reh-drh)/2-yoff-border, usetxtpaint);
		}
		canvas.restore();
		return cury-h-border*2;
	}
	private int rgb(int r, int g, int b,int lighten) {
		r+=lighten;
		g+=lighten;
		b+=lighten;
		if (r>255) r=255;
		if (g>255) g=255;
		if (b>255) b=255;
		if (r<0) r=0;
		if (g<0) g=0;
		if (b<0) b=0;
		return Color.rgb(r,g,b);
	}
	public void update(LatLon pos2, double hdg2, double gs2,boolean quick) {
		pos=pos2;
		hdg=(float)hdg2;
		gs=(float)gs2;
		if (!quick)
			doupdate();
		invalidate();
	}


}
