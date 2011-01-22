package se.flightplanner;

import java.util.ArrayList;

import se.flightplanner.GuiSituation.Clickable;
import se.flightplanner.Project.LatLon;
import se.flightplanner.Project.Merc;
import se.flightplanner.Timeout.DoSomething;
import se.flightplanner.vector.Vector;
import android.graphics.Rect;
import android.location.Location;
import android.util.Log;
import android.view.MotionEvent;

public class GuiSituation
{
	private int zoomlevel;
	private boolean extrainfo;
	private int extrainfolineoffset;
	private Merc drag_center13;

	private GuiSituation.GuiState state=GuiState.IDLE;
	private Timeout drag_timeout;
	private float dragstartx,dragstarty;	
	private Merc drag_base13;
	private float drag_heading;
	private GuiClientInterface movingMap;
	private ArrayList<GuiSituation.Clickable> clickables;
	
	
	public class DragTimeout implements DoSomething
	{
		@Override
		public void run() {
			doCenterDragging();
		}	
	}
	
	enum GuiState
	{
		IDLE, //Normal
		MAYBE_DRAG, //about to start dragging
		DRAGGING
	}
	public interface GuiClientInterface
	{

		void sideways(int i);

		/**
		 * 
		 * @param point Point to show info about.
		 */
		void showInfo(LatLon point);

		void doInvalidate();

		InformationPanel getCurrentWarning();
		
		void cancelMapDownload();
		
	}
	public interface Clickable
	{
		Rect getRect();
		void onClick();
	}
	private int maxinfolines;
	Location lastpos;
	int xsize,ysize;
	public GuiSituation(MovingMap map,int maxinfolines,Location initpos,
			int xsize,int ysize)	
	{
		this.xsize=xsize;
		this.ysize=ysize;
		this.lastpos=initpos;
		this.maxinfolines=maxinfolines;
		movingMap=map;
		drag_timeout=new Timeout();
		zoomlevel=9;
		clickables=new ArrayList<GuiSituation.Clickable>();

	}
	boolean handleOnTouch(MotionEvent ev,float x_dpmm,float y_dpmm) {
		float x=ev.getX();
		Transform tf = getTransform();
		float y=ev.getY();
		if (ev.getAction()==MotionEvent.ACTION_DOWN ||
			ev.getAction()==MotionEvent.ACTION_MOVE)	
			onTouchFingerDown(tf, x,y,x_dpmm,y_dpmm);	
		else
		if (ev.getAction()==MotionEvent.ACTION_UP)
			onTouchFingerUp(tf, x,y);
		return true;
	}
	public void onClassicalClick(float x, float y)
	{
		float b=ysize;
		float r=xsize;
		float w=xsize;//movingMap.getRight()-movingMap.getLeft();
		float h=ysize;//movingMap.getBottom()-movingMap.getTop();
		Rect fat_finger=new Rect((int)x,(int)y,(int)x,(int)y);
		MapDrawer.grow(fat_finger,(int)(w*0.075f));
		for(GuiSituation.Clickable click : clickables)
		{			
			if (Rect.intersects(fat_finger,click.getRect()))
			{
				click.onClick();
				
				return;
			}
		}
		
		if (y>b-0.2*h)
		{
			if (x<0.33*w)
				movingMap.sideways(-1);
			else
			if (x>r-0.33*w)
				movingMap.sideways(+1);
			else
			{
				cycleextrainfo();
			}
		}
		else
		{
			Transform tf = getTransform();
			Merc m=tf.screen2merc(new Vector(x,y));
			LatLon point=Project.merc2latlon(m,zoomlevel);
			movingMap.showInfo(point);
			movingMap.doInvalidate();
		}
	}
	public void onTouchAbort()
	{ 
		//this is called when gui.zoomlevel is changed, which can happen
		//while dragging, in principle.
		state=GuiState.IDLE;
	}
	public void onNorthUp()
	{
		drag_heading=0;
		movingMap.doInvalidate();
	}
	public void doCenterDragging()
	{
		state=GuiState.IDLE;
		drag_center13=null;
		drag_base13=null;
		movingMap.doInvalidate();
	}
	void cycleextrainfo() {
		InformationPanel we=movingMap.getCurrentWarning();
		if (we!=null)
		{
			String[] details;
			int maxlines=maxinfolines;
			if (extrainfo)
				details=we.getExtraDetails();
			else
				details=we.getDetails();
	
			Log.i("fplan","Cycling!");
			int lines=details.length;
			if (lines-extrainfolineoffset<=maxlines)
			{ //the set previously visible included the last ones, so let's move on.
				extrainfo=!extrainfo;
				extrainfolineoffset=0;
				if (extrainfo)
					details=we.getExtraDetails();
				else
					details=we.getDetails();
			}
			else
			{
				extrainfolineoffset+=maxlines;
			}
			movingMap.doInvalidate();
		}
	}
	void resetDragTimeout() {
		drag_timeout.timeout(new DragTimeout(), 30000);
	}
	public void onTouchFingerDown(Transform tf, float x, float y,float x_dpmm,float y_dpmm) {
		switch(state)
		{
		case IDLE:
			dragstartx=x;
			dragstarty=y;
			state=GuiState.MAYBE_DRAG;
			break;
		case MAYBE_DRAG:
		{
			float dist=(dragstartx-x)*(dragstartx-x)+(dragstarty-y)*(dragstarty-y);
			float thresh=4.0f*x_dpmm;
			if (dist>thresh*thresh)
			{
				dragstartx=x;
				dragstarty=y;
				float h=0.25f*(ysize);
				float hdgrad=tf.getHdgRad();
				Merc t=new Merc(tf.getPos().x,tf.getPos().y);
				
				if (drag_center13==null)
				{
					float deltax=(float)(Math.sin(hdgrad)*h);
					float deltay=-(float)(Math.cos(hdgrad)*h);
					t.x+=deltax;
					t.y+=deltay;
				}
				drag_base13=Project.merc2merc(t, zoomlevel, 13);
				drag_heading=tf.getHdg();
				state=GuiState.DRAGGING;
				resetDragTimeout();
			}
		}
			break;
		case DRAGGING:
			float deltax1=(x-dragstartx)*(1<<(13-zoomlevel));
			float deltay1=(y-dragstarty)*(1<<(13-zoomlevel));
			float hdgrad=tf.getHdgRad();
			float deltax=(float)(Math.cos(hdgrad)*deltax1-Math.sin(hdgrad)*deltay1);
			float deltay=(float)(Math.sin(hdgrad)*deltax1+Math.cos(hdgrad)*deltay1);
			drag_center13=new Merc(drag_base13.x-deltax,drag_base13.y-deltay);
			resetDragTimeout();
			movingMap.doInvalidate();
			break;
		}
		
	}
	public void onTouchFingerUp(Transform tf, float x, float y) {
		switch(state)
		{
		case IDLE:
			break;
		case MAYBE_DRAG:
			onClassicalClick(x, y);
			state=GuiState.IDLE;
			break;
		case DRAGGING:
			state=GuiState.IDLE;
			break;
		}
	}
	void changeZoom(int zd) {
		onTouchAbort();
		state=GuiSituation.GuiState.IDLE;
		
		zoomlevel+=zd;
		if (zoomlevel<4)
			zoomlevel=4;
		else
		if (zoomlevel>13)
			zoomlevel=13;		
		movingMap.doInvalidate();
	}
	Transform getTransform() {
		if (lastpos!=null)
		{
			Merc mypos;
			float hdg=0;
			if (drag_center13!=null)
			{
				Merc drag_center=Project.merc2merc(drag_center13,13,zoomlevel);
				mypos=new Merc(drag_center.x,drag_center.y);
				hdg=drag_heading;
			}
			else
			{
				mypos=Project.latlon2merc(new LatLon(lastpos.getLatitude(),lastpos.getLongitude()),zoomlevel);
				if (lastpos.hasBearing())
				{
					hdg=lastpos.getBearing();
				}				
			}
			return new Transform(mypos,getArrow(),(float)hdg,zoomlevel);
		}
		else
		{
			return new Transform(new Merc(128<<zoomlevel,128<<zoomlevel),getArrow(),0,zoomlevel);			
		}
	}
	public Vector getArrow()
	{
		Vector v=new Vector(xsize/2,ysize/2);
		if (drag_center13==null)
			v.y+=ysize/4; //not dragging
		return v;		
	}

	public int getZoomlevel() {
		return zoomlevel;
	}
	public int getExtrainfolineoffset() {
		return extrainfolineoffset;
	}
	public Merc getDrag_center13() {
		return drag_center13;
	}
	public boolean getExtraInfo() {
		return extrainfo;
	}
	public void clearClickables() {
		clickables.clear();
	}
	public ArrayList<GuiSituation.Clickable> getClickables() {
		return clickables;
	}
	public void updatePos(Location pos) {
		this.lastpos=pos;
	}
	public void cancelMapDownload() {
		movingMap.cancelMapDownload();
	}
	
}