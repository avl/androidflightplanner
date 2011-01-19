package se.flightplanner;

import java.util.ArrayList;

import se.flightplanner.Project.Merc;
import se.flightplanner.Project.iMerc;
import se.flightplanner.vector.BoundingBox;

public class FlightPathLogger {

	ArrayList<Chunk> chunks;
	boolean active;
	
	
	/**
	 * @param merc Position to log
	 * @param nomenclature
	 */
	public void log(iMerc merc17,long gps_timestamp_ms,int speedHint,AirspaceLookup lookup)
	{			
		boolean activated=false;
		boolean deactivated=false;
		if (speedHint>20)
		{
			Chunk chunk=null;
			if (active && chunks.size()>0)
			{
				chunk=chunks.get(chunks.size()-1);
			}
			else
			{
				chunk=new Chunk(merc17,gps_timestamp_ms);
				activated=true;
				chunks.add(chunk);
				active=true;
			}
			chunk.log(merc17,gps_timestamp_ms);
		}
		else
		{
			if (active)
			{
				deactivated=false;
				active=false;
			}
		}
		if (activated)
		{
			String place=findPlace(merc17,lookup);			
		}
		else
		{
			String place=findPlace(chunks.get(chunks.size()-1).last17,lookup);
		}
	}
	private String findPlace(iMerc merc17,AirspaceLookup lookup) {
		iMerc merc13=Project.imerc2imerc(merc17,17,13);
		int somedist = (int)(Project.approx_scale(merc13.getY(), 13, 5));
		BoundingBox bb13 = new BoundingBox(merc13.getX(),
				merc13.getY(), merc13.getX(), merc13.getY())
				.expand(somedist);
		iMerc closest13=null;
		SigPoint closest_point=null;
		int closest_dist=Integer.MAX_VALUE;
		for (SigPoint sp : lookup.allAirfields.findall(bb13)) {
			iMerc m13 = Project.latlon2imerc(sp.latlon, 13);
			int dist=difflen(m13,merc13);
			if (dist<=closest_dist)
			{
				closest_point=sp;
				closest13=m13;
				closest_dist=dist;
			}
		}
		if (closest_point==null)
			return "Unknown Place";
		return describe_relative(merc13,closest13,closest_point.name);
	}
	public static String describe_relative(iMerc mypos, iMerc fixed, String fixedname) {
		iMerc delta=diff(mypos,fixed);
		int len=difflen(fixed,mypos);		
		double onenm=Project.approx_scale(mypos.getY(),13, 1.0);
		double lennm=len/onenm;
		String dir="?";
		int hdg=calchdg(delta);
		if (hdg>=360-22 || hdg<=0+22)
			dir="north";
		else if (hdg>=45-22 && hdg<=45+22)
			dir="north-east";
		else if (hdg>=90-22 && hdg<=90+22)
			dir="east";
		else if (hdg>=135-22 && hdg<=135+22)
			dir="south-east";
		else if (hdg>=180-22 && hdg<=180+22)
			dir="south";
		else if (hdg>=225-22 && hdg<=225+22)
			dir="south-west";
		else if (hdg>=270-22 && hdg<=270+22)
			dir="west";
		else if (hdg>=315-22 && hdg<=315+22)
			dir="north-west";
		
		return String.format("%.0f NM %s of %s",lennm,dir,fixedname);
	}
	static iMerc diff(iMerc a,iMerc b)
	
	{
		return new iMerc(a.getX()-b.getX(),a.getY()-b.getY());
	}
	static int calchdg(iMerc delta)
	{
		return (int)(Math.atan2(-delta.getX(),delta.getY())*180/3.14159f)+180;
	}
	static int calcrate(iMerc delta,int stampdelta)
	{
		return (int)(1000.0f*Math.sqrt(Math.pow(delta.getX(),2)+Math.pow(delta.getY(),2))/stampdelta);
	}
	static int difflen(iMerc a,iMerc b)
	{
		float dx=a.getX()-b.getX();
		float dy=a.getY()-b.getY();
		return (int)(Math.sqrt(dx*dx+dy*dy));
	}
	static public class Chunk
	{
		private String start_place;
		private String end_place;
		private iMerc start17;
		private long startstamp;
		private iMerc last17;
		private int lasthdg;
		private int lastrate;
		private int lastturn;
		private long laststamp;
		private int laststampdelta;

		private BinaryCodeBuf binbuf;
		public Chunk(iMerc start17,long stamp)
		{
			this.start17=start17;
			this.startstamp=stamp;
			this.binbuf=new BinaryCodeBuf();
			this.last17=start17.copy();
			this.lasthdg=0;
			this.lastturn=0;
			this.lastrate=50;
			this.laststamp=startstamp;
			this.laststampdelta=1000;
		}
		public void rewind()
		{
			
			binbuf=BinaryCodeBuf.backdecode(binbuf);
			this.last17=start17.copy();
			this.lasthdg=0;
			this.lastturn=0;
			this.lastrate=50;
			this.laststamp=startstamp;
			this.laststampdelta=1000;
			
		}
		/**
		 * Return false if coding failed because we need to start a new chunk
		 * otherwise true.
		 */
		public boolean log(iMerc merc17,long gps_timestamp_ms)
		{			
			//Calculate the timestamp delta to be coded
			long code_stampdelta=(gps_timestamp_ms-this.laststamp);
			if (code_stampdelta<1000) return true; //can't log events this close in time			
			if (code_stampdelta>5000) return false;
			//System.out.println("code_stamp: "+code_stampdelta+" secs:"+secs);
			
			//Various values
			iMerc delta=diff(merc17,last17);
			int rate=calcrate(delta,(int)code_stampdelta);
			//Calculate the rate ('speed') to be coded
			int code_rate=rate;
			//System.out.println("code_rate:"+code_rate);				
			
			//Calculate the turn to be coded
			int hdg=calchdg(delta);			
			int assumehdg=this.lasthdg;
			int code_turn=hdg-assumehdg;
			//System.out.println("code_turn:"+code_turn);				
			
			//Code timestamp, turn and rate.
			int presize=binbuf.size();
			int helper=code_turn-lastturn;
			//System.out.println("Encoding "+(code_stampdelta-laststampdelta)+","+(code_turn)+","+(code_rate-lastrate));
			if (!binbuf.gammacode(code_stampdelta-laststampdelta) ||
				!binbuf.gammacode(code_turn) ||
				!binbuf.gammacode(code_rate-lastrate)
				)
			{
				binbuf.rewind2size(presize);
				return false;
			}
						
			this.lastturn=code_turn;
			this.lastrate=code_rate;
			this.laststampdelta=(int)code_stampdelta;
			this.lasthdg+=code_turn;
			//System.out.println("Encoded hdg:"+lasthdg);
			this.last17=travel(this.last17,this.lastrate,this.lasthdg,this.laststampdelta);
			this.laststamp=gps_timestamp_ms;
			return true;
		}
		static public class PosTime
		{
			public iMerc pos;
			public long stamp;
		}
		public PosTime playback()
		{
			if (binbuf.offset()==binbuf.size()) return null;
			long raw_stampdelta=binbuf.gammadecode();
			long raw_turn=binbuf.gammadecode();
			long raw_rate=binbuf.gammadecode();
			//System.out.println("Decoded "+(pre-binbuf.available())+" bits");
			//System.out.println("Decoding "+(raw_stampdelta)+","+(raw_turn)+","+(raw_rate));
			//int lastsecs=laststampdelta/1000;
			laststampdelta+=raw_stampdelta;
			//int secs=laststampdelta/1000;
			//long code_turn2=lastturn+raw_helper;
			long code_turn=raw_turn;
			long code_rate=lastrate+raw_rate;
			
			
			lastrate=(int)code_rate;
			lastturn=(int)(code_turn);
			lasthdg+=code_turn;
			//System.out.println("Decoded hdg:"+lasthdg);
			laststamp+=laststampdelta;
			last17=travel(this.last17,this.lastrate,this.lasthdg,this.laststampdelta);			
			PosTime item=new PosTime();
			item.pos=last17.copy();
			item.stamp=laststamp;
			return item;
		}
		private iMerc travel(iMerc pos, int rate, int hdg,
				int delta) {
			float rad=(float)(hdg/(180.0f/Math.PI));
			int dx=(int)(delta*rate*(Math.sin(rad))/1000.0f);
			int dy=(int)(delta*rate*(-Math.cos(rad))/1000.0f);
			
			return new iMerc(pos.getX()+dx,pos.getY()+dy);
		}
		public int sizebits() {
			return binbuf.size();
		}
		
		
	}
}
