package se.flightplanner2.intercept.junk;

import java.util.ArrayList;
import java.util.LinkedList;

import se.flightplanner2.intercept.FlightPathSegment;
import se.flightplanner2.intercept.StateVector;
import se.flightplanner2.vector.Vector;

public class InterceptPlanner {
	
	static public class HistoryAndState
	{
		ArrayList<FlightPathSegment> history=new ArrayList<FlightPathSegment>();
		StateVector state;
	}
	/*
	@SuppressWarnings("unchecked")
	ArrayList<FlightPathSegment> plan_segments(StateVector initial,int limit)
	{
		if (limit>30) throw new RuntimeException("Too many recursions");
		if (initial.pos.x<0) throw new RuntimeException("Internal error: Intercept planner can only handle right intercepts");		
		float dist=(float)initial.pos.x;
		float join_30_dist=getJoin30Dist();
		if (initial.hdg<330 && initial.hdg>=135)
		{
			ArrayList<FlightPathSegment> res=new ArrayList<FlightPathSegment>();			
			HistoryAndState turned=compositeCurve(initial,330,standard_bank,standard_bank);
			if (turned.state.pos.x<join_30_dist)
			{
				HistoryAndState center=compositeCurveToCenter(initial,standard_bank,standard_bank);
				ArrayList<FlightPathSegment> rest=mirror(plan_segments(mirror(center.state),limit+1));
				center.history.addAll(rest);
				return center.history;
			}
			
			return plan_segments(prev,turned,limit+1);
		}
		
		
		if (dist>2*join_30_dist)
		{
			
		}
					
	}
	static public class Item
	{
		public float time;
		public float hdg;
		public Vector pos;
	}

	static public class ModelBanks
	{
		float speed;
		float standard_bank;
		From level flight up along Y-axis, bank right up to standard_bank   
		ArrayList<Item> peel;
	}
	private ModelBanks calc_model(float speed,float standard_bank)
	{
		ModelBanks banks=new ModelBanks();
		StateVector init=new StateVector(new Vector(0,0),0,speed,0,0);
		FlightPathRollSegment rollman=new FlightPathRollSegment(standard_bank);
		ArrayList<Item> out=new ArrayList<Item>();
		for(;;)
		{
			StateVector output=rollman.execute(init, 0.5f);
			Item it=new Item();
			it.time=output.time;
			it.hdg=output.hdg;
			it.pos=output.pos;
			out.add(it);
			if (Math.abs(output.roll-standard_bank)<1e-3)
				break;
		}
		banks.peel=out;
		banks.speed=speed;
		banks.standard_bank=standard_bank;
		return banks;		
	}

	private StateVector compositeCurve(
			StateVector initial, float targ_hdg,float endbank) {
		
		
		
		return null;
	}
*/
}
