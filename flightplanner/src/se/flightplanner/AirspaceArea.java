/**
 * 
 */
package se.flightplanner;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;

import se.flightplanner.Project.LatLon;
import se.flightplanner.Project.Merc;
import se.flightplanner.vector.Polygon;
import se.flightplanner.vector.Vector;

public class AirspaceArea implements Serializable 
{
	public Polygon poly;
	public String name;
	public ArrayList<LatLon> points;
	public ArrayList<String> freqs;
	public String floor;
	public String ceiling;
	public void serialize(DataOutputStream os) throws IOException
	{
		if (name!=null)
			os.writeUTF(name);
		else
			os.writeUTF("");
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
	public static AirspaceArea deserialize(DataInputStream is) throws IOException
	{
		AirspaceArea a=new AirspaceArea();
		a.name=is.readUTF();		
		int numpoints=is.readInt();
		if (numpoints>100)
			throw new RuntimeException("Too many points in area: "+a.name+" : "+numpoints);
		a.points=new ArrayList<LatLon>();
		for(int i=0;i<numpoints;++i)
			a.points.add(LatLon.deserialize(is));
		int numfreqs=is.readInt();
		if (numfreqs>50)
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