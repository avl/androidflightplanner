package se.flightplanner.map3d;

import java.util.ArrayList;

import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import se.flightplanner.AirspaceArea;
import se.flightplanner.map3d.ObserverContext.ObserverState;

public class GuiState {
	private ObserverState environ;
	private long lastUserAction;
	private long lastUpdate;
	private AirspaceArea shown_area;
	private int width, height;
	private int start_scroll_y;
	private int start_scroll_x;
	private int scroll_base;
	private int cur_scroll_y;
	private int cur_scroll_x;
	private int info_scroll_base;
	private int info_cur_scroll_y;

	private int glance;
	public static class DrawPos
	{
		public DrawPos(int x,int y,int size)
		{
			this.x=x;
			this.y=y;
			this.size=size;			
		}
		public int x;
		public int y;
		public int size;
	}
	
	public static class DrawOrder {
		// int itemHeight;
		ArrayList<AirspaceArea> spaces;
		ArrayList<DrawPos> coords;
		/** Null if no area selected */
		AirspaceArea selected;
		///int scroll_x;
		boolean retreating;
		int info_scroll_y;
		int glance;

		// int scroll_y;
		public DrawPos get(int idx) {
			return coords.get(idx);
		}
	}

	private volatile DrawOrder drawOrder;

	public DrawOrder getDrawOrder() {
		return drawOrder;
	}

	@SuppressWarnings("unchecked")
	private void createDrawOrder()
	{
		DrawOrder ret=new DrawOrder();
		if (environ!=null)
			ret.spaces=(ArrayList<AirspaceArea>)environ.spaces.clone();
		else
			ret.spaces=new ArrayList<AirspaceArea>();
		ret.glance=glance;
		int infoscroll=info_scroll_base+info_cur_scroll_y;
		if (shown_area!=null)
		{
			int maxscrolly=32*(2+shown_area.freqs.size())-height/5;
			if (maxscrolly<0) maxscrolly=0;
			if (infoscroll>maxscrolly)
				infoscroll=maxscrolly;
			if (infoscroll<0) infoscroll=0;
		}
		ret.info_scroll_y=infoscroll;
		ret.coords=new ArrayList<DrawPos>();
		///ret.scroll_x=cur_scroll_x;
		int raw_scroll_ammount=scroll_base+cur_scroll_y;
		if (raw_scroll_ammount<0)
			raw_scroll_ammount=0;
		int scroll_ammount;
		int zoomview;
		if (raw_scroll_ammount<150)
		{
			zoomview=raw_scroll_ammount/2;
			scroll_ammount=raw_scroll_ammount;
		}
		else
		{
			zoomview=150/2;
			scroll_ammount=raw_scroll_ammount;
		}
		
		
		//Log.i("fplan","scroll ammount:"+scroll_ammount);
		if (scroll_ammount<0)
			scroll_ammount=0;
		if (scroll_ammount>=32*ret.spaces.size())
			scroll_ammount=32*ret.spaces.size()-1;
		ret.selected=shown_area;
		if (state==State.RETREATING)
			ret.retreating=true;
		
		int logidx=scroll_ammount/32;
		int logrem=scroll_ammount%32;
		int yposoffs=height/5+zoomview;
		for(int i=0;i<logidx;++i)
			ret.coords.add(null);
		float y=-logrem;
		float logy=-logrem;
		int eft=cur_scroll_x-width/2;
		eft=eft*eft*eft*eft;
		eft/=(width/2+1)*(width/2+1)*(width/2+1);
		int effective_cur_scroll_x=width/2+eft;
		
		for(int i=logidx-1;i>=0;--i)
		{
			logy-=32;
			float step=48.0f/(1.0f+0.01f*Math.abs(logy));
			if (step>64) step=64;
			if (step<16) step=16;
			float prevy=y;
			y-=step;			
			int size=((int)prevy)-((int)y);
			ret.coords.set(i,new DrawPos((int)(effective_cur_scroll_x-2*step+32),(int)(y+yposoffs),0));
		}
		y=-logrem;
		logy=-logrem;
		for(int i=logidx;i<ret.spaces.size();++i)
		{
			float step=48.0f/(1.0f+0.01f*Math.abs(logy));
			if (step>64) step=64;
			if (step<16) step=16;
			float nexty=y;
			nexty+=step;			
			int size=((int)nexty)-((int)y);
			
			ret.coords.add(new DrawPos((int)(effective_cur_scroll_x-2*step+32),(int)(y+yposoffs),size));
			y=nexty;
			logy+=32;
		}
		for(int i=0;i<ret.coords.size()-1;++i)
		{
			DrawPos cur=ret.coords.get(i);
			DrawPos next=ret.coords.get(i+1);
			cur.size=next.y-cur.y;
		}
		drawOrder=ret;
	}

	private int scale(int idx, int scrollY) {
		int step = 32 - idx * 2;
		if (step <= 16)
			step = 16;
		return step;
	}

	private static enum State {
		IDLE, SHOW, PRESCROLL, SCROLLING_LIST, SCROLLING_INFO, RETREATING
	};

	private State state;

	public void onTouchUpdate(int x, int y) {
		//Log.i("fplan", "onTouchUpdate" + x + "," + y);
		long now = SystemClock.uptimeMillis();
		lastUserAction = now;
		if (state == State.RETREATING && (x > width - width / 2)) {
			state = State.PRESCROLL;
			cur_scroll_x=width/2;
			start_scroll_y = y;
			start_scroll_x = x;
			createDrawOrder();
			return;
		}

		if (state == State.IDLE) {
			if (x > width - width / 2) {
				state = State.SHOW; //Going directly to SHOW makes the finger-up event be swallowed
				cur_scroll_x=width/2;
				start_scroll_y = y;
				start_scroll_x = x;
				createDrawOrder();
			}
			return;
		}
		if (state == State.SHOW) {
			state = State.PRESCROLL;
			start_scroll_y = y;
			start_scroll_x = x;
			createDrawOrder();
			return;
		}
		if (state == State.PRESCROLL) {
			int diff = Math.abs(y - start_scroll_y)+Math.abs(x-start_scroll_x);
			if (diff > width/15+10) {
				if (y<height/5)
					state=State.SCROLLING_INFO;
				else
					state = State.SCROLLING_LIST;
				//Log.d("fplan", "Start scrolling");
				// scroll_start=SystemClock.uptimeMillis();
				start_scroll_y = y;
				start_scroll_x = x;
				cur_scroll_y = 0;
				createDrawOrder();
			}
			return;
		}
		if (state == State.SCROLLING_LIST) {
			cur_scroll_x = x;
			if (cur_scroll_x < width / 2)
				cur_scroll_x = width / 2;
			//Log.d("fplan", "Keep scrolling");
			cur_scroll_y = get_cur_scroll(y);
			createDrawOrder();
			return;
		}
		if (state == State.SCROLLING_INFO) {
			info_cur_scroll_y = get_cur_scroll(y);
			createDrawOrder();
			return;
		}
	}

	private int get_cur_scroll(int y) {
		return 2*(start_scroll_y-y);
	}

	public void onTouchFingerUp(int x, int y) {
		//Log.i("fplan", "onUp" + x + "," + y);
		if (state == State.PRESCROLL) {
			//Log.d("fplan", "Generated click");
			generate_click(y);
			createDrawOrder();
			state=State.SHOW;
			return;
		}
		if (state == State.SCROLLING_INFO)
		{
			info_scroll_base += get_cur_scroll(y);
			if (info_scroll_base < 0)
				info_scroll_base = 0;
			if (shown_area!=null)
			{
				int maxscrolly=32*(2+shown_area.freqs.size())-height/5;
				if (maxscrolly<0) maxscrolly=0;
				if (info_scroll_base>maxscrolly)
					info_scroll_base=maxscrolly;			
			}
			info_cur_scroll_y=0;
			state = State.SHOW;				
			createDrawOrder();
			return;
		}
		if (state == State.SCROLLING_LIST) {
			scroll_base += get_cur_scroll(y);
			if (scroll_base < 0)
				scroll_base = 0;
			if (environ.spaces!=null)
			{
				if (scroll_base>32*environ.spaces.size())
					scroll_base=32*environ.spaces.size();
			}
			else
			{
				scroll_base=0;
			}
			cur_scroll_y = 0;
			if (cur_scroll_x>width/2+width/4)
			{
				state=State.RETREATING;
			}
			else
			{
				state = State.SHOW;				
				cur_scroll_x = width / 2;
			}
			createDrawOrder();
			return;
		}
	}

	private final int ITEM_HEIGHT = 16;

	private void generate_click(int ycoord) {
		// ycoord is relative to this scroll-list
			if (drawOrder!=null)
			{
				int idx=0;
				for(DrawPos pos : drawOrder.coords)
				{
					if (ycoord>=pos.y && ycoord<pos.y+pos.size)
					{
						if (environ.spaces != null && idx<environ.spaces.size() && idx>=0)
						{
							shown_area=environ.spaces.get(idx);
							//Log.i("fplan","Showing area:"+shown_area.name);
							return;
						}
					}
					idx+=1;
				}
			}
	}


	public void onTimer() {
		long now = SystemClock.uptimeMillis();
		if (state == State.SHOW && now - lastUserAction > 30000) {
			state = State.RETREATING;
			createDrawOrder();
		}
		if (now-lastUserAction>30000)
			glance=0;
		timer.postDelayed(new Runnable() {
			
			@Override
			public void run() {
				onTimer();
			}
		}, 2500);
	}

	Handler timer;
	public GuiState(int width, int height) {
		this.state = State.IDLE;
		this.width = width;
		this.height = height;
		this.cur_scroll_x = width;
		timer=new Handler();
		timer.postDelayed(new Runnable() {
			
			@Override
			public void run() {
				onTimer();
			}
		}, 2500);
		/*
		 * Questions: 1: Show something about spaces continuously? Like progress
		 * of R-area approaching 2: How to allow user to determine what the
		 * shown 3D-objects are? Click in list and highlight 3D-obj? 3: Keep
		 * track of clearances?
		 */
	}

	public void maybeDoUpdate(ObserverState obsstate) {
		long now = SystemClock.uptimeMillis();
		if (state == State.IDLE || now > lastUpdate + 60000)
		{
			
			environ = obsstate;
		}
	}

	public void setScreenDim(int width2, int height2) {
		width = width2;
		height = height2;
	}

	public void arrowkeys(int i) {
		if (i<0)
			glance-=45;
		if (i>0)
			glance+=45;
		if(glance<-180) glance+=360;
		if(glance>=180) glance-=360;
		lastUserAction=SystemClock.uptimeMillis();
		createDrawOrder();
	}

}
