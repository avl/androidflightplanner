package se.flightplanner.map3d;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import se.flightplanner.Project.iMerc;
import se.flightplanner.map3d.ElevationStore.Elev;
import se.flightplanner.map3d.ElevationStore.ElevTile;

public class ElevationReader {

	
	//Vertices waiting to be fetched
	//Owned by background thread
	private HashSet<Vertex> order;
	private boolean quit;
	
	static private class ExecutedOrder
	{
		public ExecutedOrder(Vertex v2, Elev e) {
			this.v=v2;
			this.e=e;
		}
		public Vertex v;
		public Elev e;
	}
	
	//Vertices whose elevations have been fetched
	//Owned by background thread
	private ArrayList<ExecutedOrder> executed;
	
	private ElevationStore elevStore;
	
	public ElevationReader(ElevationStore elevStore)
	{
		this.elevStore=elevStore;
		order=new HashSet<Vertex>();
		executed=new ArrayList<ExecutedOrder>();
	}
	public void queue(Vertex v)
	{
		inbox.add(v);
	}
	private synchronized boolean background_iter() throws InterruptedException
	{
		if (quit) return true;
		while(true)
		{
			Iterator<Vertex> i=order.iterator();
			if (!i.hasNext())
				break;
			Vertex v1=i.next();
			ElevTile et=elevStore.get(v1.getimerc());
			for(Vertex v2:order)
			{
				Elev e=et.get(v2.getimerc());
				if (v1==v2 && e==null)
					throw new RuntimeException("elevstore returned elevtile which didn't contain the asked for merc coord!");
				if (e!=null)
				{
					executed.add(new ExecutedOrder(v2,e));
				}
			}
			
			
		}
		Thread.sleep(100);
		return false;
	}
	private void background() throws InterruptedException
	{
		while(true)
		{
			if (background_iter())
			{
				return;
			}
		}
	}

	//Queued up vertices (added by foreground queue)
	//Not synchronized, only accessed by foreground thread.
	//Exchanged by 'commit'.
	private HashSet<Vertex> inbox;
		
	///Run by foreground thread to apply
	///results to foreground-owned vertex objects,
	///while background thread stands still
	synchronized public void commit(boolean quit)
	{
		this.quit=quit;
		for(Vertex v: inbox)
		{
			order.add(v);
		}
		inbox.clear();
		for(ExecutedOrder e:executed)
		{
			e.v.updateElev(e.e.loElev,e.e.hiElev);
		}
		executed.clear();
	}
	
	
}
