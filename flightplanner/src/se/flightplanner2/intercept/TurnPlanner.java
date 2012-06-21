package se.flightplanner2.intercept;

import java.util.Arrays;

public class TurnPlanner {

	/*!
	 * The number of degrees turned (in horizontal plane),
	 * while rolling from wings-level to a specified roll.
	 * 
	 * x = specified roll degrees (20 for 20 deg, for example)
	 * y = hdg change in milli-degrees, ammount of horizontal plane turn (yaw)
	 * roll2turn[x] = y
	 *  
	 */
	private int[] roll2turn; //per 0.5 seconds	
	private float turnrate; //in steady turn
	private float speed;
	
	static public class Res
	{
		public float rolldeg;
		public float keep_turn;
		@Override
		public String toString() {
			return "Res [rolldeg=" + rolldeg + ", keep_turn=" + keep_turn + "]";
		}
		
	}
	
	public Res calc(float speed,float hdgdelta)
	{
		init(speed);
		int dir=1;
		if (hdgdelta<0)
		{
			hdgdelta=-hdgdelta;
			dir=-dir;
		}
		float bankonly=2f*0.001f*roll2turn[(int)(Helpers.nominal_bank)];
		if (hdgdelta<bankonly)
		{
			int target_delta=(int)(hdgdelta/2);
			int y=Arrays.binarySearch(roll2turn, target_delta);
			//Y == (-(i) - 1)
			//Y+1 = -i
			//i=-(Y+1)
			if (y<0)
				y=-(y+1);
			Res res=new Res();
			res.rolldeg=dir*y;
			res.keep_turn=0;								
			return res;
		}
		else
		{
			Res res=new Res();
			float remain=hdgdelta-bankonly;
			float keep=remain/turnrate;
			
			res.rolldeg=dir*Helpers.nominal_bank;
			res.keep_turn=keep;								
			return res;
			
		}
	}
	
	void init(float speed)
	{
		if (this.speed==speed)
			return;
		this.speed=speed;
		int maxbank=(int)(Helpers.nominal_bank+1);
		roll2turn=new int[maxbank+1];
		for(int x=1;x<=maxbank;++x)
		{
			FlightPathRollSegment roll=new FlightPathRollSegment(x);
			StateVector basic=new StateVector();
			basic.speed=speed;
			StateVector res=roll.execute(basic);			
			roll2turn[x]=(int)(1000.0f*res.hdg);
		}					
		
		FlightPathTurnSegment turn=new FlightPathTurnSegment(0.1f);
		StateVector basic=new StateVector();
		basic.speed=speed;
		basic.roll=Helpers.nominal_bank;
		StateVector res=turn.execute(basic);
		turnrate=res.hdg/0.1f;
		//System.out.println("Turn rate: "+turnrate);
		

	}
	
}
