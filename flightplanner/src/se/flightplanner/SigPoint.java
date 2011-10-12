/**
 * 
 */
package se.flightplanner;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;

import se.flightplanner.Project.LatLon;
import se.flightplanner.Project.Merc;

public class SigPoint implements Serializable,Comparable<SigPoint>
{
	private static final long serialVersionUID = 1939452363561911490L;
	
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
	}
	
	public Merc pos;
	public String name;
	public String kind; //interned
	public double alt;
	public LatLon latlon;
	/*!
	 * Some points have charts associated with them.
	 * So far, only airports though.
	 */
	public Chart chart; 
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
		os.writeFloat((float) alt);
		latlon.serialize(os);
		
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
		
	}
	public static SigPoint deserialize(DataInputStream is,int version) throws IOException {
		SigPoint p=new SigPoint();
		p.name=is.readUTF();
		p.kind=is.readUTF().intern();
		if (p.kind=="airport")
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
		p.alt=is.readFloat();		
		p.latlon=LatLon.deserialize(is);
		if (version>=3)
		{
			byte havechart=is.readByte();
			if (havechart!=0 && havechart!=1)
				throw new RuntimeException("corrupt stream - havechart not 0 or 1.");
			if (havechart==1)
			{
				Chart c=new Chart();
				c.width=is.readInt();
				c.height=is.readInt();
				c.name=is.readUTF();
				c.checksum=is.readUTF();
				c.url=is.readUTF();
				double [] matrix=new double[6];
				for(int i=0;i<6;++i)
				{
					if (version>=4)
						matrix[i]=is.readDouble();
					else
						matrix[i]=is.readFloat();
				}
				c.A=new double[][]{new double[2],new double[2]};
				c.A[0][0]=matrix[0];
				c.A[1][0]=matrix[1];
				c.A[0][1]=matrix[2];
				c.A[1][1]=matrix[3];
				c.T=new double[]{matrix[4],matrix[5]};	
				p.chart=c;
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
}