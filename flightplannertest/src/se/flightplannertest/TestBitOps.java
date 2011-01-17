package se.flightplannertest;

import static org.junit.Assert.*;

import java.util.Random;

import org.junit.Test;

import se.flightplanner.BinaryCodeBuf;
import se.flightplanner.BitSeq;

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
		BinaryCodeBuf b=new BinaryCodeBuf(128);
		b.gammacode(0);
		BinaryCodeBuf b2=BinaryCodeBuf.backdecode(b);
		assertEquals(b2.gammadecode(),0);
	}
	@Test
	public void testOneGammaCode()
	{
		BinaryCodeBuf b=new BinaryCodeBuf(128);
		b.gammacode(1);
		BinaryCodeBuf b2=BinaryCodeBuf.backdecode(b);
		assertEquals(b2.gammadecode(),1);
	}
	@Test
	public void testMinusOneGammaCode()
	{
		BinaryCodeBuf b=new BinaryCodeBuf(128);
		b.gammacode(-1);
		BinaryCodeBuf b2=BinaryCodeBuf.backdecode(b);
		assertEquals(b2.gammadecode(),-1);
	}
	@Test
	public void testTwoGammaCode()
	{
		BinaryCodeBuf b=new BinaryCodeBuf(128);
		b.gammacode(2);
		BinaryCodeBuf b2=BinaryCodeBuf.backdecode(b);
		assertEquals(2,b2.gammadecode());
	}
	@Test
	public void testThreeGammaCode()
	{
		BinaryCodeBuf b=new BinaryCodeBuf(128);
		b.gammacode(3);
		BinaryCodeBuf b2=BinaryCodeBuf.backdecode(b);
		assertEquals(b2.gammadecode(),3);
	}
	@Test
	public void testHundredGammaCode()
	{
		BinaryCodeBuf b=new BinaryCodeBuf(128);
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
			BinaryCodeBuf b=new BinaryCodeBuf(512);
			b.gammacode(val);
			BinaryCodeBuf b2=BinaryCodeBuf.backdecode(b);
			assertEquals(b2.gammadecode(),val);
		}
	}

}
