package se.flightplannertest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Test;

import se.flightplanner.Airspace;
import se.flightplanner.AirspaceArea;
import se.flightplanner.SigPoint;
import se.flightplanner.Project.LatLon;

public class TestAirspaceSerialization {

	@Test public void testDataStream() throws IOException
	{
		ByteArrayOutputStream bao=new ByteArrayOutputStream();
		DataOutputStream os=new DataOutputStream(bao);
		os.writeFloat((float) 1.0);
		//data
		os.writeInt(0x01020304);
		//Ints are in big-endian
		os.writeUTF("hello");
		byte[] arr=bao.toByteArray();
		for(int i=0;i<arr.length;++i)
			System.out.println("Element:"+i+" = "+(int)(arr[i]));
		//So, the writeUTF function appears to write the length first in big-endian format.
		
	}
	
	AirspaceArea getDummyArea()
	{
		AirspaceArea area=new AirspaceArea();
		area.floor="Floor";
		area.name="Name of some area";
		area.ceiling="Ceiling";
		area.freqs=new ArrayList<String>();
		area.freqs.add("SomeString");
		area.points=new ArrayList<LatLon>();
		area.points.add(new LatLon(59,18));
		return area;
	}
	@Test
	public void testAirspaceSerialize() throws IOException
	{
		Airspace airspace=new Airspace();
		airspace.points=new ArrayList<SigPoint>();
		airspace.spaces=new ArrayList<AirspaceArea>();
		
		SigPoint p=new SigPoint();
		p.alt=4500;
		p.kind="obstacle";
		p.latlon=new LatLon(59,18);
		p.name="SomeObst";
		p.calcMerc();
		airspace.points.add(p);
		airspace.spaces.add(getDummyArea());
		
		ByteArrayOutputStream bao=new ByteArrayOutputStream();
		DataOutputStream os=new DataOutputStream(bao);
		airspace.serialize(os);
		ByteArrayInputStream bai=new ByteArrayInputStream(bao.toByteArray());
		DataInputStream is=new DataInputStream(bai);
		Airspace airspace2=Airspace.deserialize(is,null,null);		
		
		Assert.assertEquals(airspace.points.size(),airspace2.points.size());
		for(int i=0;i<airspace.points.size();++i)
		{
			SigPoint a=airspace.points.get(i);
			SigPoint b=airspace2.points.get(i);
			Assert.assertEquals(a.name,b.name);
			Assert.assertEquals(a.kind,b.kind);
			Assert.assertEquals(a.latlon.lat,b.latlon.lat,1e-6);
			Assert.assertEquals(a.latlon.lon,b.latlon.lon,1e-6);
			Assert.assertEquals(a.pos.x,b.pos.x,1e-6);
			Assert.assertEquals(a.pos.y,b.pos.y,1e-6);
		}		
		Assert.assertEquals(airspace.spaces.size(),airspace2.spaces.size());
		for(int i=0;i<airspace.spaces.size();++i)
		{
			AirspaceArea a=airspace.spaces.get(i);
			AirspaceArea b=airspace2.spaces.get(i);
			Assert.assertEquals(a.name,b.name);
			Assert.assertEquals(a.freqs,b.freqs);
			Assert.assertEquals(a.ceiling,b.ceiling);
			Assert.assertEquals(a.floor,b.floor);
		}
	}
	
	@Test
	public void testAirspaceAreaSerialize() throws IOException
	{
		AirspaceArea area=getDummyArea();
		ByteArrayOutputStream bao=new ByteArrayOutputStream();
		DataOutputStream os=new DataOutputStream(bao);
		area.serialize(os);
		ByteArrayInputStream bai=new ByteArrayInputStream(bao.toByteArray());
		DataInputStream is=new DataInputStream(bai);
		AirspaceArea area2=AirspaceArea.deserialize(is,0);
		
		Assert.assertEquals(area.floor,area2.floor);
		Assert.assertEquals(area.ceiling,area2.ceiling);
		Assert.assertEquals(area.freqs,area2.freqs);
		
	}
}
