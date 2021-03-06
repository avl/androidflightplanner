/**
 * 
 */
package se.flightplanner2;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;

import android.util.Log;

import se.flightplanner2.Project.LatLon;
import se.flightplanner2.Project.Merc;
import se.flightplanner2.vector.Polygon;
import se.flightplanner2.vector.Vector;

public class AirspaceArea implements Serializable 
{
	public Polygon poly;
	public String name;
	public ArrayList<LatLon> points;
	public ArrayList<String> freqs;
	public String floor;
	public String ceiling;
	
	public String getIdentity()
	{
		return name+"/"+floor+"/"+ceiling+"/"+(int)poly.get_area();
	}
	public String toString()
	{
		return "AirspaceArea("+name+" size: "+poly.get_area()+" floor: "+floor+" ceiling: "+ceiling+" rbg: "+r+" "+g+" "+b+" "+a+")";
	}
	/**
	 * Dynamic information about this airspace area in relation to the single observer
	 * From a high-level design level, this does not belong here. However, having it here
	 * is good for performance, since diverse parts of the code which all need the
	 * spacestats can have it without any extra lookup (since they already lookup
	 * the airspace area). The disadvantage is that we can't support multiple observers. 
	 */
	volatile public SpaceStats dyninfo; 
	public short r,g,b,a;
	/*!
	 * Set to time when last cleared by ATC in this area, or 0 if never.
	 */
	public long cleared;
	
	public void serialize(DataOutputStream os) throws IOException
	{
		if (name!=null)
			os.writeUTF(name);
		else
			os.writeUTF("");
		os.writeByte(r);
		os.writeByte(g);
		os.writeByte(b);
		os.writeByte(a);
		int numpoints=points.size();
		os.writeInt(numpoints);
		for(int i=0;i<numpoints;++i)
			points.get(i).serialize(os);
		int numfreqs=freqs.size();
		os.writeInt(numfreqs);
		for(int i=0;i<numfreqs;++i)
		{
			String f=freqs.get(i);
			if (f!=null)
				os.writeUTF(f);
			else
				os.writeUTF("");
		}
		if (floor!=null)
			os.writeUTF(floor);
		else
			os.writeUTF("");
		if (ceiling!=null)
			os.writeUTF(ceiling);
		else
			os.writeUTF("");
	}
	public static AirspaceArea deserialize(DataInputStream is, int version) throws IOException
	{
		AirspaceArea a=new AirspaceArea();
		a.name=is.readUTF();		
		if (version>=2)
		{
			a.r=(short)(is.readByte()&0xff);
			a.g=(short)(is.readByte()&0xff);
			a.b=(short)(is.readByte()&0xff);
			a.a=(short)(is.readByte()&0xff);
		}
		int numpoints=is.readInt();
		if (numpoints<0 || numpoints>100)
			throw new RuntimeException("Too many points in area: "+a.name+" : "+numpoints);
		a.points=new ArrayList<LatLon>();
		for(int i=0;i<numpoints;++i)
			a.points.add(LatLon.deserialize(is));
		
		int numfreqs=is.readInt();
		if (numfreqs<0 || numfreqs>50)
			throw new RuntimeException("Too many freqs in area: "+a.name+" : "+numpoints);
		a.freqs=new ArrayList<String>();
		for(int i=0;i<numfreqs;++i)
			a.freqs.add(is.readUTF());
		a.floor=is.readUTF();
		a.ceiling=is.readUTF();
		a.initPoly(a.points);
		return a;		
	}
	public Polygon getPoly(){return poly;}
	
	public void initPoly(ArrayList<LatLon> points2) {
		ArrayList<Vector> pointsvec=new ArrayList<Vector>();				
		for(int j=0;j<points2.size();++j)
		{
			LatLon latlon=points2.get(j);
			Merc merc=Project.latlon2merc(latlon, 13);
			pointsvec.add(new Vector(merc.x,merc.y));				
		}
		this.poly=new Polygon(pointsvec);
	}
	
	public AirspaceArea(){}
	public AirspaceArea(String pname,Polygon ppoly)
	{
		poly=ppoly;
		name=pname;
	}
}