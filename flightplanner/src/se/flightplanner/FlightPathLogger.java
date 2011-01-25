package se.flightplanner;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;

import android.os.Environment;
import android.util.Log;

import se.flightplanner.Project.Merc;
import se.flightplanner.Project.iMerc;
import se.flightplanner.vector.BoundingBox;

public class FlightPathLogger {

	ArrayList<Chunk> chunks;
	boolean active;
	public FlightPathLogger()
	{
		chunks=new ArrayList<FlightPathLogger.Chunk>();
		active=false;
	}
	
	public void saveCurrent(AirspaceLookup lookup) throws IOException
	{
		if (active && chunks.size()>0)
		{
			Chunk chunk=chunks.get(chunks.size()-1);
			if (chunk.startstamp>24*86400*10)
			{
				chunk.setEndPlace(findPlace(chunk.last17,lookup));
				chunk.saveToDisk();
			}
		}
	}
	/**
	 * @param merc Position to log
	 * @param nomenclature
	 * @throws IOException 
	 */
	public void log(iMerc merc17,long gps_timestamp_ms,int speedHint,AirspaceLookup lookup) throws IOException
	{	
		boolean splitup=false;
		if (active && chunks.size()>0)
		{
			
			Chunk chunk=chunks.get(chunks.size()-1);
			if (chunk.binbuf.size()>128000*8)
				splitup=true;
		}
		
		if (speedHint>20 && !splitup)
		{
			Chunk chunk=null;
			if (active && chunks.size()>0)
			{
				chunk=chunks.get(chunks.size()-1);
			}
			else
			{
				Log.i("fplan.fplog","Starting new chunk");
				chunk=new Chunk(merc17,gps_timestamp_ms);
				chunk.setStartPlace(findPlace(merc17,lookup));
				chunks.add(chunk);
				active=true;
			}
			chunk.log(merc17,gps_timestamp_ms);
			Log.i("fplan.fplog","Merc:"+merc17+" stamp:"+gps_timestamp_ms);
		}
		else
		{
			if (active)
			{
				if (chunks.size()>0)
				{					
					Log.i("fplan.fplog","Finishing chunk");
					Chunk chunk=chunks.get(chunks.size()-1);
					chunk.log(merc17,gps_timestamp_ms);
					chunk.finish(findPlace(chunk.last17,lookup));
					if (chunk.startstamp>24*86400*10)
						chunk.saveToDisk();
				}
				active=false;
			}
		}
	}
	private String findPlace(iMerc merc17,AirspaceLookup lookup) {
		if (lookup==null)
			return "Unknown";
		iMerc merc13=Project.imerc2imerc(merc17,17,13);
		int somedist = (int)(Project.approx_scale(merc13.getY(), 13, 5));
		BoundingBox bb13 = new BoundingBox(merc13.getX(),
				merc13.getY(), merc13.getX(), merc13.getY())
				.expand(somedist);
		iMerc closest13=null;
		SigPoint closest_point=null;
		int closest_dist=Integer.MAX_VALUE;
		ArrayList<SigPoint> possible=new ArrayList<SigPoint>();
		for (SigPoint sp : lookup.majorAirports.findall(bb13)) 
			possible.add(sp);
		for (SigPoint sp : lookup.minorAirfields.findall(bb13)) 
			possible.add(sp);
		for (SigPoint sp : lookup.allCities.findall(bb13)) 
			possible.add(sp);
		for (SigPoint sp : lookup.allTowns.findall(bb13)) 
			possible.add(sp);
		for (SigPoint sp : lookup.allOthers.findall(bb13))
			if (sp.kind=="town")
				possible.add(sp);
		for (SigPoint sp : possible) {
			iMerc m13 = Project.latlon2imerc(sp.latlon, 13);
			int dist=difflen(m13,merc13);
			if (sp.kind=="port" || sp.kind=="field")
				dist-=somedist/4; //prefer to give pos relative to airfield
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
		double lennm=Project.exacter_distance(
				Project.imerc2latlon(mypos, 13),
				Project.imerc2latlon(fixed, 13));
		if (lennm<1.0f)
			return fixedname;
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
	static int calcrate(float delta,int stampdelta)
	{
		return (int)(1000.0f*delta/stampdelta);
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
		private long distance_millinm;
		private boolean finished;
		

		
		
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
			this.finished=false;
			this.distance_millinm=0;
		}
		private Chunk() {
			//Just use for loadFromDisk
		}
		final static private SimpleDateFormat dateformat = new SimpleDateFormat("yyyyMMddkkmmss");
		final static private SimpleDateFormat dateonlyformat = new SimpleDateFormat("yyyy-MM-dd");
		final static private SimpleDateFormat timeonlyformat = new SimpleDateFormat("kk:mm");
		static
		{
			dateformat.setTimeZone(TimeZone.getTimeZone("GMT"));
			dateonlyformat.setTimeZone(TimeZone.getTimeZone("GMT"));
			timeonlyformat.setTimeZone(TimeZone.getTimeZone("GMT"));
		}
		public String getStartDate()
		{
			return dateonlyformat.format(new Date(startstamp));
		}
		public String getStartTime()
		{
			return timeonlyformat.format(new Date(startstamp));
		}
		public String getEndTime()
		{
			return timeonlyformat.format(new Date(laststamp));
		}
		public String getDeparture()
		{
			return start_place;
		}
		public String getDestination()
		{
			return end_place;
		}
		public float getDistance()
		{
			return distance_millinm/1000.0f;
		}
		public String getDuration()
		{
			long delta=laststamp-startstamp;
			long deltasec=delta/1000;
			long deltamin=(deltasec+30)/60;
			int hours=(int)(deltamin/60);
			int minutes=(int)(deltamin%60);
			return String.format("%dh%dm", hours,minutes);
		}
		
		private void saveToDisk() throws IOException {
			Date d=new Date(startstamp);
			String filename=dateformat.format(d);
			File extpath = Environment.getExternalStorageDirectory();
			File tripdirpath = new File(extpath,
				"/Android/data/se.flightplanner/files/triplog/");
			Log.i("fplan.fplog","Writing"+filename);
			serialize(filename, tripdirpath);				
			
		}
		static public Chunk loadFromDisk(String filename,boolean headeronly) throws IOException {
			File extpath = Environment.getExternalStorageDirectory();
			File tripdirpath = new File(extpath,
				"/Android/data/se.flightplanner/files/triplog/");
			Chunk c=new Chunk();
			c.deserialize(filename, tripdirpath, headeronly);
			return c;
		}
		public void deserialize(String filename, File tripdirpath,boolean headeronly)
				throws IOException 
		{
			if (!tripdirpath.exists())
			{
				tripdirpath.mkdirs();
			}
			File path= new File(tripdirpath,filename);
			try
			{
				InputStream ofstream=new BufferedInputStream(
						new FileInputStream(path)
						);
				DataInputStream data=new DataInputStream(ofstream);
				int magic=data.readInt();
				if (magic!=0xfafafa01)
					throw new RuntimeException("Bad magic, got: "+magic);
				int version=data.readInt();
				if (version<1 || version>2)
					throw new RuntimeException("Bad version, got: "+version);
				start_place=data.readUTF();
				end_place=data.readUTF();
				start17=iMerc.deserialize(data);
				startstamp=data.readLong();
				last17=iMerc.deserialize(data);
				lasthdg=data.readInt();
				lastrate=data.readInt();
				lastturn=data.readInt();
				laststamp=data.readLong();
				laststampdelta=data.readInt();
				finished=data.readInt()!=0;
				if (version>1)
					distance_millinm=data.readLong();
				else
					distance_millinm=0;
				if (!headeronly)
					binbuf=BinaryCodeBuf.deserialize(data);				
				
			} catch (IOException e)
			{
				path.delete();
				throw e;
			}
		}
		public void serialize(String filename, File tripdirpath)
				throws IOException {
			if (!tripdirpath.exists())
			{
				tripdirpath.mkdirs();
			}
			File path= new File(tripdirpath,filename);
			try
			{
				OutputStream ofstream=new BufferedOutputStream(
						new FileOutputStream(path)
						);
				DataOutputStream data=new DataOutputStream(ofstream);
				data.writeInt(0xfafafa01); //magic
				data.writeInt(2); //version
				
				if (start_place!=null)
					data.writeUTF(start_place);
				else
					data.writeUTF("");
				if (end_place!=null)
					data.writeUTF(end_place);
				else
					data.writeUTF("");
				
				start17.serialize(data);
				data.writeLong(startstamp);
				last17.serialize(data);
				data.writeInt(lasthdg);
				data.writeInt(lastrate);
				data.writeInt(lastturn);
				data.writeLong(laststamp);
				data.writeInt(laststampdelta);
				data.writeInt(finished ? 1 : 0);
				data.writeLong(distance_millinm);
				binbuf.serialize(data);
			
				data.flush();
				data.close();
				Log.i("fplan.fplog","Wrote "+path);
				
			} catch (IOException e)
			{
				path.delete();
				throw e;
			}
		}
		public void finish(String endPlace) {
			end_place=endPlace;
			finished=true;
			
		}
		public void setStartPlace(String place) {
			start_place=place;
			
		}
		public void setEndPlace(String place) {
			end_place=place;
			
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
			if (code_stampdelta>300*1000) return false;
			//System.out.println("code_stamp: "+code_stampdelta+" secs:"+secs);
			
			
			//Various values
			iMerc delta=diff(merc17,last17);
			float deltalen=(float)Math.sqrt(Math.pow(delta.getX(),2)+Math.pow(delta.getY(),2));
			int rate=calcrate(deltalen,(int)code_stampdelta);
			//Calculate the rate ('speed') to be coded
			int code_rate=rate;
			
			distance_millinm+=(long)(Project.approx_nm(merc17.getY(),17, (int)(1e3*deltalen)));			
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
