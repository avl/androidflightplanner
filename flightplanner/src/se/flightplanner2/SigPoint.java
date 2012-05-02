/**
 * 
 */
package se.flightplanner2;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import se.flightplanner2.Project.LatLon;
import se.flightplanner2.Project.Merc;

public class SigPoint implements Serializable,Comparable<SigPoint>
{
	private static final long serialVersionUID = 1939452363561911490L;
	
	/*
	
	public static class Chart implements Serializable
	{
		private static final long serialVersionUID = 6324069623768703289L;
		public int width;
		public int height;
		public String name;
		public String checksum;
		public String url;
		public double[][] A; //2x2 matrix with airport chart projection scale/rotation latlon -> image pixels
		public double[] T; //2 vector with airport chart projection translation
	}*/
	
	public Merc pos; //merc 13
	public String name;
	public String kind; //interned
	public double alt;

	public LatLon latlon;
	
	static public class End
	{
		public String name;
		public LatLon pos;
	}
	static public class Runway
	{
		public End[] ends;
	}
	static public class ExtraData
	{
		public String[] notams;
		public String metar;
		public String icao;
		public String taf;
		public Runway[] runways;
	}
	public ExtraData extra;
	
	/*!
	 * Some points have charts associated with them.
	 * So far, only airports though.
	 */
	//public Chart chart; 
	@Override
	public int compareTo(SigPoint o) {
		if (pos.x<o.pos.x) return -1;
		if (pos.x>o.pos.x) return +1;
		if (pos.y<o.pos.y) return -1;
		if (pos.y>o.pos.y) return +1;		
		return name.compareTo(o.name);
	}
	public void serialize(DataOutputStream os) throws IOException
	{
		if (name!=null)
			os.writeUTF(name);
		else
			os.writeUTF("");
		if (kind!=null)
			os.writeUTF(kind);
		else
			os.writeUTF("");
		
		if (extra==null)
		{
			 //don't forget to update below (extra!=null case)
			os.writeInt(0); //0 notams
			os.writeByte(0); //no icao
			os.writeByte(0); //0 taf
			os.writeByte(0); //0 metar
			os.writeByte(0); //0 runways
		}
		else
		{
			//Don't forget to update above
			os.writeInt(extra.notams.length); //0 notams
			for(int i=0;i<extra.notams.length;++i)
				os.writeUTF(extra.notams[i]);
			if (extra.icao!=null)
			{
				os.writeByte(1);
				os.writeUTF(extra.icao);
			}
			else
			{
				os.writeByte(0);
			}
			if (extra.taf!=null)
			{
				os.writeByte(1);
				os.writeUTF(extra.taf);
			}
			else
			{
				os.writeByte(0); //0 taf
			}
			if (extra.metar!=null)
			{		
				os.writeByte(1); 
				os.writeUTF(extra.metar);
			}
			else
			{
				os.writeByte(0);
			}
			if (extra.runways!=null)
			{
				os.writeByte(extra.runways.length);
				for(Runway runway:extra.runways)
				{
					for(int i=0;i<2;++i)
					{
						End end=runway.ends[i];
						os.writeUTF(end.name);
						end.pos.serialize(os);
					}
				}
			}
		}

		
		
		os.writeFloat((float) alt);
		latlon.serialize(os);
		/*
		if (chart!=null)
		{
			os.writeByte(1); //havechart=1 (true)
			os.writeInt(chart.width);
			os.writeInt(chart.height);
			os.writeUTF(chart.name);
			os.writeUTF(chart.checksum);
			os.writeUTF(chart.url);
			os.writeDouble(chart.A[0][0]);
			os.writeDouble(chart.A[1][0]);
			os.writeDouble(chart.A[0][1]);
			os.writeDouble(chart.A[1][1]);
			os.writeDouble(chart.T[0]);
			os.writeDouble(chart.T[1]);
		}
		else
		{
			os.writeByte(0);			
		}
		*/
		
	}
	public static SigPoint deserialize(DataInputStream is,int version) throws IOException {
		SigPoint p=new SigPoint();
		p.name=is.readUTF();
		p.kind=is.readUTF().intern();
		if (p.kind=="airport") //this won't happen with any new data.
		{
			if (p.name.endsWith("*"))
			{
				p.name=p.name.substring(0,p.name.length()-1);
				p.kind="port";
			}
			else
			{
				p.kind="field";
			}
			
		}
		String metar=null;
		String taf=null;
		String icao=null;
		String[] notams=null;
		Runway[] runways=null;
		if (version>=5)
		{
			int num_notams=is.readInt();
			notams=new String[num_notams];
			for(int i=0;i<num_notams;++i)
				notams[i]=is.readUTF();
			
			if (is.readByte()!=0)
				icao=is.readUTF();//icao
			if (is.readByte()!=0)
				taf=is.readUTF();//TAF
			if (is.readByte()!=0)
				metar=is.readUTF();//Metar
			
			if (version>=7)
			{
				int nrunways=is.readByte();
				runways=new Runway[nrunways];
				for(int j=0;j<nrunways;++j)
				{
					Runway runway=new Runway();
					End[] ends=new End[]{new End(),new End()};
					for(int i=0;i<2;++i)
					{
						ends[i].name=is.readUTF();
						ends[i].pos=LatLon.deserialize(is);
					}
					runway.ends=ends;
					runways[j]=runway;
				}
			}
			
		}
		if (metar!=null || taf!=null || icao!=null || (notams!=null && notams.length>0) || runways!=null)
		{				
			p.extra=new ExtraData();
			p.extra.metar=metar;
			p.extra.taf=taf;
			p.extra.icao=icao;
			p.extra.notams=notams;
			p.extra.runways=runways;
		}
		
		p.alt=is.readFloat();		
		p.latlon=LatLon.deserialize(is);
		
		
		if (version>=3 && version<=5)
		{
			byte havechart=is.readByte();
			if (havechart!=0 && havechart!=1)
				throw new RuntimeException("corrupt stream - havechart not 0 or 1.");
			if (havechart==1)
			{
				//Chart c=new Chart();
				is.readInt();
				is.readInt();
				is.readUTF();
				is.readUTF();
				is.readUTF();
				double [] matrix=new double[6];
				for(int i=0;i<6;++i)
				{
					if (version>=4)
						is.readDouble();
					else
						is.readFloat();
				}				
			}
			
		}
		p.calcMerc();
		return p;
	}
	public void calcMerc() {
		// TODO Auto-generated method stub
		Merc merc=Project.latlon2merc(latlon, 13);
		pos=merc;		
	}
	public static void sort_nearest(ArrayList<SigPoint> airfs, final LatLon mypos) {
		Collections.sort(airfs,new Comparator<SigPoint>()
				{
					@Override
					public int compare(SigPoint o1, SigPoint o2) {
						double d1=Project.exacter_distance(o1.latlon, mypos);
						double d2=Project.exacter_distance(o2.latlon, mypos);
						if (d1<d2)return -1; 
						if (d1>d2)return 1; 
						return 0;
					}			
				});		
	}
}