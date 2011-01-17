package se.flightplanner;

import se.flightplanner.Project.iMerc;

public class FlightPathLogger {

	/**
	 * @param merc Position to log
	 * @param nomenclature
	 */
	public void log(iMerc merc17,int speed,int hdg,long gps_timestamp_ms,String nomenclature)
	{			
	}
	static private class Chunk
	{
		private iMerc last17;
		private int lasthdg;
		private int lastrate;
		private int lastturn;
		private long laststamp;
		private long laststampdelta;

		private BinaryCodeBuf binbuf;
		static iMerc diff(iMerc a,iMerc b)
		{
			return new iMerc(a.getX()-b.getX(),a.getY()-b.getY());
		}
		static int calchdg(iMerc delta)
		{
			return (int)(Math.atan2(-delta.getX(),-delta.getY())*180/3.14159f)+180;
		}
		static int calcrate(iMerc delta,long timedelta)
		{
			return (int)(timedelta*Math.sqrt(Math.pow(delta.getX(),2)+Math.pow(delta.getY(),2)));
		}
		public Chunk(iMerc start17,long stamp)
		{
			this.binbuf=new BinaryCodeBuf(32768*8);
			this.last17=start17;
			this.lasthdg=0;
			this.lastturn=0;
			this.lastrate=50;
			this.laststamp=stamp;
			this.laststampdelta=1000;
		}
		public void log(iMerc merc17,long gps_timestamp_ms,String nomenclature)
		{			
			iMerc delta=diff(last17,merc17);
			long code_stampdelta=gps_timestamp_ms-this.laststamp;
			if (code_stampdelta<1000) return; //can't log events this close in time			
			int secs=(int)(code_stampdelta/1000);
			int hdg=calchdg(delta);
			int rate=calcrate(delta,code_stampdelta);
			
			int assumehdg=this.lasthdg+this.lastturn*secs;
			
			int code_rate=rate;
			if (code_rate<lastrate-1)
				code_rate=code_rate+1;
			else if (code_rate>lastrate+1)
				code_rate=code_rate-1;
			else
				code_rate=lastrate;
			
			int code_turn=hdg-assumehdg;
			if (code_turn<lastturn*secs-1)
				code_turn=code_turn+1;
			else if (code_turn>lastturn*secs+1)
				code_turn=code_turn-1;
			else
				code_turn=lastturn*secs;			
			
			binbuf.gammacode(code_stampdelta-laststampdelta);
			binbuf.gammacode(code_turn-lastturn*secs);
			binbuf.gammacode(code_rate-lastrate);
			
			this.lastturn=code_turn/secs;
			this.lastrate=code_rate;
			this.laststampdelta=code_stampdelta;									
		}
		
		
	}
}
