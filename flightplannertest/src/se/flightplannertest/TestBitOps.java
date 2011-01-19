package se.flightplannertest;

import static org.junit.Assert.*;

import java.util.Random;

import org.junit.Test;

import se.flightplanner.BinaryCodeBuf;
import se.flightplanner.BitSeq;
import se.flightplanner.FlightPathLogger;
import se.flightplanner.FlightPathLogger.Chunk;
import se.flightplanner.FlightPathLogger.Chunk.PosTime;
import se.flightplanner.Project.iMerc;

public class TestBitOps {
	@Test
	public void testBinaryRep()
	{
		BitSeq bs=new BitSeq();
		bs.binarycode(3,2);
		assertEquals(1,bs.size());
		assertEquals(3,bs.binarydecode(2));
		
	}
	@Test
	public void testBinaryRep2()
	{
		BitSeq bs=new BitSeq();
		bs.binarycode(9,4);
		assertEquals(3,bs.size());
		assertEquals(9,bs.binarydecode(4));
		
	}
	@Test
	public void testZeroGammaCode()
	{
		BinaryCodeBuf b=new BinaryCodeBuf();
		b.gammacode(0);
		BinaryCodeBuf b2=BinaryCodeBuf.backdecode(b);
		assertEquals(b2.gammadecode(),0);
	}
	@Test
	public void testOneGammaCode()
	{
		BinaryCodeBuf b=new BinaryCodeBuf();
		b.gammacode(1);
		BinaryCodeBuf b2=BinaryCodeBuf.backdecode(b);
		assertEquals(b2.gammadecode(),1);
	}
	@Test
	public void testMinusOneGammaCode()
	{
		BinaryCodeBuf b=new BinaryCodeBuf();
		b.gammacode(-1);
		BinaryCodeBuf b2=BinaryCodeBuf.backdecode(b);
		assertEquals(b2.gammadecode(),-1);
	}
	@Test
	public void testTwoGammaCode()
	{
		BinaryCodeBuf b=new BinaryCodeBuf();
		b.gammacode(2);
		BinaryCodeBuf b2=BinaryCodeBuf.backdecode(b);
		assertEquals(2,b2.gammadecode());
	}
	@Test
	public void testThreeGammaCode()
	{
		BinaryCodeBuf b=new BinaryCodeBuf();
		b.gammacode(3);
		BinaryCodeBuf b2=BinaryCodeBuf.backdecode(b);
		assertEquals(b2.gammadecode(),3);
	}
	@Test
	public void testHundredGammaCode()
	{
		BinaryCodeBuf b=new BinaryCodeBuf();
		b.gammacode(100);
		BinaryCodeBuf b2=BinaryCodeBuf.backdecode(b);
		assertEquals(b2.gammadecode(),100);
	}
	@Test
	public void testRandomGammaCode()
	{
		Random r=new Random();
		for(int i=0;i<1000;++i)
		{
			int val=r.nextInt();
			BinaryCodeBuf b=new BinaryCodeBuf();
			b.gammacode(val);
			BinaryCodeBuf b2=BinaryCodeBuf.backdecode(b);
			assertEquals(b2.gammadecode(),val);
		}
	}
	@Test 
	public void testFlightLogger1()
	{
		Chunk chunk=new Chunk(new iMerc(0,0),0);
		chunk.log(new iMerc(0,0),1000);
		chunk.rewind();
		PosTime item=chunk.playback();
		assertEquals(new iMerc(0,0),item.pos);
		assertEquals(1000,item.stamp);
	}
	@Test 
	public void testFlightLogger2()
	{
		Chunk chunk=new Chunk(new iMerc(0,0),0);
		chunk.log(new iMerc(0,0),1000);
		chunk.log(new iMerc(5,0),2000);
		chunk.log(new iMerc(10,0),3000);
		chunk.rewind();
		PosTime item=chunk.playback();
		assertEquals(new iMerc(0,0),item.pos);
		assertEquals(1000,item.stamp);
		item=chunk.playback();
		assertEquals(new iMerc(4,0),item.pos);
		assertEquals(2000,item.stamp);
		item=chunk.playback();
		assertEquals(new iMerc(9,0),item.pos);
		assertEquals(3000,item.stamp);
	}
	@Test
	public void testFlightLoggerLong()
	{		
		Chunk chunk=new Chunk(new iMerc(0,0),0);
		for(int check=0;check<2;++check)
		{
			int x=0,y=0;
			int vx=10,vy=0;
			Random r=new Random(42);
			if (check==1)
			{
				chunk.rewind();
			}
			long stamp=0;
			for(int i=0;i<3600;++i)
			{
				int dx=0;
				int dy=0;
				if (r.nextInt(10)<=2)
				{
					dx=r.nextInt(30)-15;
					dy=r.nextInt(30)-15;
				}
				if (r.nextInt(10)<=5)
				{
					dx=r.nextInt(2)-1;
					dy=r.nextInt(2)-1;
				}
				vx+=dx;
				vy+=dy;
				if (Math.abs(vx)>800) vx=0;
				if (Math.abs(vy)>800) vy=0;
				x+=vx;
				y+=vy;
				stamp+=2000;
				if (check==0)
				{
					assertTrue(chunk.log(new iMerc(x,y),stamp));
				}
				else
				{
					//System.out.println("Size:"+chunk.sizebits()/8192+"kB");
					PosTime it=chunk.playback();
					assertTrue(Math.abs(it.pos.getX()-x)<50);
					assertTrue(Math.abs(it.pos.getY()-y)<50);					
				}
				
			}
			if (check==1)
				System.out.println("Bits:"+chunk.sizebits()+" per second: "+chunk.sizebits()/3600.0f);
		}		
	}
	@Test 
	public void testFlightLogger3()
	{
		int[] dxs=new int[]{
				0,5,-5,15,-15,127,-127	
			};
		int[] dys=new int[]{
				0,5,-5,15,-15,127,-127	
			};
		for(int dx : dxs)
		{
			for(int dy : dys)
			{
				Chunk chunk=new Chunk(new iMerc(0,0),0);
				chunk.log(new iMerc(0,0),1000);
				chunk.log(new iMerc(dx,dy),2000);
				chunk.log(new iMerc(2*dx,2*dy),3000);
				chunk.rewind();
				PosTime item=chunk.playback();
				assertEquals(new iMerc(0,0),item.pos);
				assertEquals(1000,item.stamp);
				item=chunk.playback();
				//System.out.println("pos:"+item.pos+" dx:"+dx+" dy:"+dy);
				assertTrue(Math.abs(item.pos.getX()-dx)<4);
				assertTrue(Math.abs(item.pos.getY()-dy)<4);
				assertEquals(2000,item.stamp);
				item=chunk.playback();
				//System.out.println("pos:"+item.pos+" dx:"+2*dx+" dy:"+2*dy);
				assertTrue(Math.abs(item.pos.getX()-2*dx)<6);
				assertTrue(Math.abs(item.pos.getY()-2*dy)<6);
				assertEquals(3000,item.stamp);
			}
		}
	}
	@Test 
	public void testFlightLogger4()
	{
		String desc=FlightPathLogger.describe_relative(
				new iMerc(1000,1000), //me
				new iMerc(500,1000), //soomething
				"Arlanda");
		assertEquals("0 NM east of Arlanda",desc);
	}
}
