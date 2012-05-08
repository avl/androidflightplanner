package se.flightplanner2;

import java.util.Arrays;

import android.content.Context;
import android.location.Location;
import android.os.SystemClock;
import android.os.Vibrator;
import android.widget.Toast;

public class AirspaceWarner {
	private enum State
	{
		IDLE,
		FIVE_MIN_WARNED,
		FOUR_MIN_WARNED
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
	private String[] report_warning;
	public String[] getWarning()
	{
		return report_warning;
	}
	void run(Location loc,Vibrator vibrator,Context context)
	{
		det.run(loc);
		if (!det.isWarning())
		{
			state=State.IDLE;
			report_warning=null;
			return;
		}
		report_warning=det.getWarning();
		
		if (state!=State.IDLE)
		{
			if (!Arrays.equals(lastareas,det.getAreanames()))
			{
				lastareas=det.getAreanames();
				long sinceblock=SystemClock.elapsedRealtime()-blocktime;
				if (sinceblock>60*1000)
				{
					lastareas=det.getAreanames();
					blocktime=SystemClock.elapsedRealtime();
					state=State.IDLE;				
				}
			}
		}
		else
		{
			lastareas=det.getAreanames();
		}
		
		
		switch(state)
		{
		case IDLE:
		{
			if ((det.getTimeLeft()<4.5 || det.getDistLeft()<0.7)) 
			{				
				if (vibrator!=null && report_warning[0]!=null) 
				{
					vibrator.vibrate(fivemin_pattern, -1);
				}
				Toast.makeText(context, "Airspace Ahead: "+report_warning[0], Toast.LENGTH_SHORT).show();
				state=State.FIVE_MIN_WARNED;
			}
		}
		break;
		case FIVE_MIN_WARNED:
		{
			if ((det.getTimeLeft()<3 || det.getDistLeft()<0.5)) 
			{				
				if (vibrator!=null) 
				{
					vibrator.vibrate(fourmin_pattern, -1);
				}
				Toast.makeText(context, "Airspace Ahead: "+report_warning[0], Toast.LENGTH_SHORT).show();
				state=State.FOUR_MIN_WARNED;
			}
		}
		break;
		}
		
	}

}
