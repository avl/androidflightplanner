package se.flightplanner2;

import java.util.Arrays;

import android.location.Location;
import android.os.SystemClock;
import android.os.Vibrator;

public class AirspaceWarner {
	private enum State
	{
		IDLE,
		FIVE_MIN_WARNED,
		FOUR_MIN_WARNED,
		IMMINENT_WARNED
	}
	private State state=State.IDLE;
	private AirspaceProximityDetector det;
	public AirspaceWarner(AirspaceProximityDetector det)
	{
		this.det=det;
	}
	private long[] fivemin_pattern=new long[]{0,100,200,100,200,100,200,100,200,100};
	private long[] fourmin_pattern=new long[]{0,100,200,100,200,100,200,100,200,100};
	private long[] imminent_pattern=new long[]{0,100,100,100,100,100,100,100,100,100,100,200,200,500,200,1000,200,1500};
	private long blocktime;
	private String[] lastareas;
	public String getWarning()
	{
		return det.getWarning();
	}
	void run(Location loc,Vibrator vibrator)
	{
		det.run(loc);
		if (!det.isWarning())
		{
			long sinceblock=SystemClock.elapsedRealtime()-blocktime;
			if (sinceblock<60*1000)
				return;
			blocktime=SystemClock.elapsedRealtime();
			state=State.IDLE;
			return;
		}
		
		if (!Arrays.equals(lastareas,det.getAreanames())){
			long sinceblock=SystemClock.elapsedRealtime()-blocktime;
			if (sinceblock>60*1000)
			{
				lastareas=det.getAreanames();
				blocktime=SystemClock.elapsedRealtime();
				state=State.IDLE;				
			}			
		}
		
		
		switch(state)
		{
		case IDLE:
		{
			if ((det.getTimeLeft()<5 || det.getDistLeft()<1)) 
			{				
				if (vibrator!=null) vibrator.vibrate(fivemin_pattern, -1);
				state=State.FIVE_MIN_WARNED;
			}
		}
		break;
		case FIVE_MIN_WARNED:
		{
			if ((det.getTimeLeft()<4 || det.getDistLeft()<0.25)) 
			{				
				if (vibrator!=null) vibrator.vibrate(fourmin_pattern, -1);
				state=State.FOUR_MIN_WARNED;
			}
		}
		break;
		case FOUR_MIN_WARNED:
		{
			if ((det.getTimeLeft()<1 || det.getDistLeft()<0.1)) 
			{				
				if (vibrator!=null) vibrator.vibrate(imminent_pattern, -1);
				state=State.IMMINENT_WARNED;
			}
		}
		break;
		case IMMINENT_WARNED:
		{
			
		}
		}
		
	}

}
