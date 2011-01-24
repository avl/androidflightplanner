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

public class SigPoint implements Serializable
{
	private static final long serialVersionUID = 1939452363561911490L;
	public Merc pos;
	public String name;
	public String kind; //interned
	public double alt;
	public LatLon latlon;
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
	}
	public static SigPoint deserialize(DataInputStream is) throws IOException {
		SigPoint p=new SigPoint();
		p.name=is.readUTF();
		p.kind=is.readUTF().intern();
		if (p.kind=="airport")
		{
			if (p.name.endsWith("*"))
			{
				p.name=p.name.substring(0,p.name.length()-1);
				p.kind="airport";
			}
			else
			{
				p.kind="airfield";
			}
		}
		p.alt=is.readFloat();		
		p.latlon=LatLon.deserialize(is);
		p.calcMerc();
		return p;
	}
	public void calcMerc() {
		// TODO Auto-generated method stub
		Merc merc=Project.latlon2merc(latlon, 13);
		pos=merc;		
	}
}