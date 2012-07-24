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
	//private int[] roll2turn; //per 0.5 seconds	
	//private float turnrate; //in steady turn
	//private float speed;
	
	static double bank2time(float bank,float bankrate)
	{
		double S=bankrate;
		return bank/S;
	}
	static double time2bank(float time,float bankrate)
	{
		double S=bankrate;
		return time*S;
	}
	static Float getTime(float turn,float bankrate_deg_sec,float speed_kt)
	{
		double S=bankrate_deg_sec*(Math.PI/180.0);
		float V=speed_kt;
		double T=turn*(Math.PI/180.0);
		double K=19.088568019211976;
		double sig=Math.signum(S*T);
		T=Math.abs(T);
		S=Math.abs(S);
		double tmp=Math.exp(S*V*(-T)/K);
		if (tmp<-1) return null;
		if (tmp>+1) return null;
		return (float)(sig*Math.acos(tmp)/S);
	}
	static Float getTurn(float time,float bankrate_deg_sec,float speed_kt)
	{
		double S=bankrate_deg_sec*(Math.PI/180.0);
		double sig=Math.signum(time*S);
		time=Math.abs(time);
		S=Math.abs(S);
		float V=speed_kt;
		double K=19.088568019211976;
		double tmp=Math.cos(time*S);
		if (tmp<0) return null;
		return (float)(180.0/Math.PI)*(float)(-sig*K * Math.log(tmp)/(S*V));
	}
	static Res getStrategy1(float hdgdelta_deg,float bank,float speed_kt)
	{		
		float rate=5;		
		
		float hdgdelta=hdgdelta_deg;
		float starttime=(float)bank2time(bank,5);
		Float starthdg=getTurn(starttime,rate,speed_kt);		
		if (starthdg==null) return null;
		int strategy=1;
		if (starthdg>hdgdelta)
			strategy=2;		
		float desthdg=hdgdelta;
		
		Res res=new Res();
		if (strategy==1)
		{
			Float time=getTime((desthdg+starthdg)/2,rate,speed_kt);
			if (time==null) return null;
			res.keep_turn=0;
			res.rolldeg=(float)(time2bank(time,rate));
		}
		else
		{
			Float time=getTime((desthdg-starthdg)/2,-rate,speed_kt);
			if (time==null) return null;
			
			res.keep_turn=0;
			res.rolldeg=(float)(time2bank(time,-rate));
			
			
			
		}
		
		if (Math.abs(res.rolldeg)>Helpers.nominal_bank)			
		{
			if (res.rolldeg>Helpers.nominal_bank)
				res.rolldeg=Helpers.nominal_bank;
			else
				res.rolldeg=-Helpers.nominal_bank;

			float fullturn=(float)getTurn(Helpers.nominal_bank/rate,rate,speed_kt);
			
			float bonus_turn_need;
			if ((bank<0) != (hdgdelta<0))
			{
				bonus_turn_need=Math.abs(starthdg);
			}
			else
			{
				bonus_turn_need=0;
				if (Math.abs(bank)>20)
				{
					//bonus_turn_need=-(Math.abs(starthdg)-fullturn);
					//bonus_turn_need-=(Math.abs(starthdg)-fullturn);
					bonus_turn_need+=-Math.abs(starthdg)+fullturn;
				}
				bonus_turn_need+=Math.abs(starthdg);
				
			}
			
			
			
			float turn_need=Math.abs(hdgdelta_deg)-2*fullturn+bonus_turn_need;
			res.keep_turn=turn_need/Helpers.get_turn_rate_deg(speed_kt, Helpers.nominal_bank);
		}
		
		
		return res;			
		
	}
	/*
	Res getStrategy2(float hdgdelta_deg,float bank,float speed_kt)
	{
		float hdgdelta=hdgdelta_deg;
		float rate=5;
		float starttime=(float)bank2time(bank,5);
		Float starthdg=getTurn(starttime,rate,speed_kt);		
		if (starthdg==null) return null;
		if (starthdg<hdgdelta)
			return null;
		float desthdg=hdgdelta;		
		Float time=getTime((desthdg-starthdg)/2,-rate,speed_kt);
		if (time==null) return null;
		
		Res res=new Res();
		res.keep_turn=0;
		res.rolldeg=(float)(time2bank(time,-rate));
		return res;
	}
	
		

	*/
	
	static public class Res
	{
		public float rolldeg;
		public float keep_turn;
		@Override
		public String toString() {
			return "Res [rolldeg=" + rolldeg + ", keep_turn=" + keep_turn + "]";
		}
		
	}
	/*
	public Res calc(float speed,float hdgdelta,float initial_roll)
	{
		init(speed);
		

		/*
		float init_turn;
		{
			int init_dir=1;
			if (initial_roll<0) 
				init_dir=-1;
			
			float reused_init_turn=initial_roll;
			if (reused_init_turn>Helpers.nominal_bank)
				reused_init_turn=Helpers.nominal_bank;
		
			init_turn=0.001f*init_dir*roll2turn[(int)(Math.abs(initial_roll)*10)];
			reused_init_turn=0.001f*init_dir*roll2turn[(int)(Math.abs(reused_init_turn)*10)];
			
			
		}
		
		
		
		
		
		float bankonly=getTurn(initial_roll,Helpers.nominal_bank)+
					getTurn(Helpers.nominal_bank,0);				
		
		if (hdgdelta<bankonly)
		{
			float bonusturn=getTurn(initial_roll,0);
			float remain_delta=hdgdelta-bonusturn;

			int dir=1;
			if (remain_delta<0)
			{
				remain_delta=-remain_delta;
				dir=-dir;
			}
			
			int target_delta=(int)(1000.0f*remain_delta/2);
			int y=Arrays.binarySearch(roll2turn, target_delta);
			//Y == (-(i) - 1)
			//Y+1 = -i
			//i=-(Y+1)
			if (y<0)
				y=-(y+1);
			float rolldeg=dir*(y/10.0f);
			int init_dir=(initial_roll<0) ? -1 : 1;
			if (init_dir==dir)
			{
				float counted_twice=bonusturn;
				remain_delta+=counted_twice;
				target_delta=(int)(1000.0f*remain_delta/2);
				y=Arrays.binarySearch(roll2turn, target_delta);
				if (y<0)
					y=-(y+1);
				rolldeg=dir*(y/10.0f);
			}			
			
			Res res=new Res();
			res.rolldeg=rolldeg;
			res.keep_turn=0;								
			return res;
		}
		else
		{
			Res res=new Res();
			int dir=1;
			if (hdgdelta<0)
			{
				hdgdelta=-hdgdelta;
				dir=-dir;
			}
			
			float remain=hdgdelta-bankonly;
			float keep=remain/turnrate;
			
			
			res.rolldeg=dir*Helpers.nominal_bank;
			res.keep_turn=keep;								
			return res;
			
		}
	}

	private float get0Turn(float roll)
	{
		if (roll<0)
			return 0.001f*(roll2turn[(int)(-roll*10)]);
		return 0.001f*(roll2turn[(int)(roll*10)]);
	}
	private float getTurn(float roll1,float roll2) {
		float v1=get0Turn(roll2);
		float v2=get0Turn(roll1);
		return Math.max(v1,v2)-Math.min(v1,v2);
	}
	
	void init(float speed)
	{
		if (this.speed==speed)
			return;
		this.speed=speed;
		int maxbank=10*(int)(60+1);
		roll2turn=new int[maxbank+1];
		for(int x=1;x<=maxbank;++x)
		{
			FlightPathRollSegment roll=new FlightPathRollSegment(x/10.0f);
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
	*/
}
