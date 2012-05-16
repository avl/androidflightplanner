package se.flightplanner2;

import java.util.ArrayList;

import se.flightplanner2.GuiSituation.Clickable;
import se.flightplanner2.Project.LatLon;
import se.flightplanner2.Project.Merc;
import se.flightplanner2.Project.iMerc;
import se.flightplanner2.Timeout.DoSomething;
import se.flightplanner2.vector.Vector;
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
	private AirspaceLookup lookup;
	private GuiSituation.GuiState state=GuiState.IDLE;
	private Timeout drag_timeout;
	private float dragstartx,dragstarty;	
	private Merc drag_base13;
	private float drag_heading;
	private GuiClientInterface movingMap;
	private ArrayList<GuiSituation.Clickable> clickables;
	private TripState tripstate;
	private InformationPanel currentInfo;
	private boolean defnorthup;
	
	void setnorthup(boolean d)
	{
		defnorthup=d;
	}

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
		void doInvalidate();	
		void cancelMapDownload();
		void doShowExtended(Place[] places);
		void showAirspaces();				
	}
	public interface Clickable
	{
		Rect getRect();
		void onClick();
	}
	private int maxinfolines;
	private Location lastpos;
	private int xsize,ysize;
	static final private LatLon limitA=new LatLon(80,-179);
	static final private LatLon limitB=new LatLon(-80,179);
	private int limitx1,limity1,limitx2,limity2;
	public GuiSituation(MovingMap map,int maxinfolines,Location initpos,
			int xsize,int ysize,TripState tripstate,AirspaceLookup lookup)	
	{
		iMerc tmp=Project.latlon2imerc(limitA,13);
		limitx1=tmp.getX();limity1=tmp.getY();
		tmp=Project.latlon2imerc(limitB,13);
		limitx2=tmp.getX();limity2=tmp.getY();
		this.tripstate=tripstate;
		this.lookup=lookup;
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
			onTouchFingerUp(tf, x,y, x_dpmm, y_dpmm);
		return true;
	}
	public void onClassicalClick(float x, float y,float x_dpmm,float y_dpmm)
	{
		int best_idx=-1;
		float bestdist=
			Math.max((0.9f*x_dpmm)+5f,xsize*0.15f);
		int idx=0;
		for(GuiSituation.Clickable click : clickables)
		{			
			Rect r=click.getRect();
			if (r==null) continue;
			if (x>=r.left && x<=r.right && y>=r.top && y<=r.bottom)
			{
				best_idx=idx;
				bestdist=0;
				break;
			}
			float dist=1e6f;
			if (x>=r.left && x<=r.right)
				dist=Math.min(Math.abs(y-r.top), Math.abs(y-r.bottom));
			else if (y>=r.top && y<=r.bottom)
				dist=Math.min(Math.abs(x-r.left), Math.abs(x-r.right));
			else if (x<=r.left)
				dist=Math.min(dist(x,y,r.left,r.top),dist(x,y,r.left,r.bottom));
			else if (x>=r.right)
				dist=Math.min(dist(x,y,r.right,r.top),dist(x,y,r.right,r.bottom));
			/*
			int cx=(r.left+r.right)/2;
			int cy=(r.bottom+r.top)/2;
			float dist=(cx-x)*(cx-x)+(cy-y)*(cy-y);
			*/
			
			if (dist<=bestdist)
			{
				best_idx=idx;
				bestdist=dist;
			}
			++idx;
		}
		if (best_idx!=-1)
		{
			clickables.get(best_idx).onClick();
			return;
		}
		{
			Transform tf = getTransform();
			Merc m=tf.screen2merc(new Vector(x,y));
			LatLon point=Project.merc2latlon(m,zoomlevel);
			double marker_size_pixels=x_dpmm*7;
			double marker_size_merc13=Math.pow(2, 13-zoomlevel)*marker_size_pixels;
			showInfo(point,(long)(marker_size_merc13));
			movingMap.doInvalidate();
		}
	}
	private float dist(float x, float y, float left, float top) {
		return (float)Math.sqrt((x-left)*(x-left)+(y-top)*(y-top));
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
		InformationPanel we=currentInfo;
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
				if (drag_base13.y<limity1) drag_base13.y=limity1;
				if (drag_base13.y>limity2) drag_base13.y=limity2;
				if (drag_base13.x<limitx1) drag_base13.x=limitx1;
				if (drag_base13.x>limitx2) drag_base13.x=limitx2;
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
			if (drag_center13.x<limitx1) drag_center13.x=limitx1;
			if (drag_center13.y<limity1) drag_center13.y=limity1;
			if (drag_center13.x>limitx2) drag_center13.x=limitx2;
			if (drag_center13.y>limity2) drag_center13.y=limity2;
			resetDragTimeout();
			movingMap.doInvalidate();
			break;
		}
		
	}
	public void onTouchFingerUp(Transform tf, float x, float y,float xdpmm,float ydpmm) {
		switch(state)
		{
		case IDLE:
			break;
		case MAYBE_DRAG:
			onClassicalClick(x, y, xdpmm, ydpmm);
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
				if (defnorthup)
					hdg=0;
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
		if (drag_center13==null && !defnorthup)
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
		if (currentInfo!=null)
			currentInfo.updatemypos(lastpos);
		
	}
	public void cancelMapDownload() {
		movingMap.cancelMapDownload();
	}
	public void onShowWaypoints() {
		currentInfo=tripstate;
		movingMap.doInvalidate();
	}
	public void onCloseInfoPanel()
	{
		currentInfo=null;
		movingMap.doInvalidate();
	}
	public void onInfoPanelBrowse(int i) {
		if (currentInfo!=null)
		{
			if (i==-1)
			{
				if (currentInfo.hasLeft())
					currentInfo.left();
			} else if(i==+1)
			{
				if (currentInfo.hasRight())
					currentInfo.right();				
			}
			movingMap.doInvalidate();
		}
		
	}
	public void updateTripState(TripState tripstate2) {
		if (tripstate!=tripstate2)
			currentInfo=null;
		this.tripstate=tripstate2;
	}
	public void updateLookup(AirspaceLookup lookup)
	{
		this.lookup=lookup;
	}
	private void showInfo(LatLon about,long marker_size13) {
		if (lookup!=null)
			currentInfo=new AirspacePointInfo(about,lookup,marker_size13);
		
	}
	public InformationPanel getCurrentInfo() {
		return currentInfo;
	}
	public boolean getnorthup() {		
		return defnorthup;
	}
	public void onShowExtended(Place[] places) {
		
		movingMap.doShowExtended(places);
	}
	public void sizechange(int width, int height, int numInfoLines) {
		this.xsize=width;
		this.ysize=height;
		this.maxinfolines=numInfoLines;		
	}
	public boolean getFingerDown() {
		return state!=GuiState.IDLE;
	}
	public void showAirspaces() {
		movingMap.showAirspaces();
	}
	
}