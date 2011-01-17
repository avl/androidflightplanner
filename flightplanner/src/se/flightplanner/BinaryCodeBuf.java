package se.flightplanner;

public class BinaryCodeBuf {

	private BitBuf bitbuf;
	private BitSeq seq1;
	private BitSeq seq2;
	private BitSeq seq3;
	private BitSeq seq4;
	private BitSeq seq5;
	public static BinaryCodeBuf backdecode(BinaryCodeBuf buf)
	{
		return new BinaryCodeBuf(buf.bitbuf);
	}
	public BinaryCodeBuf(int len)
	{
		bitbuf=new BitBuf(len);
		seq1=new BitSeq();
		seq2=new BitSeq();
		seq3=new BitSeq();
		seq4=new BitSeq();
		seq5=new BitSeq();
	}
	private BinaryCodeBuf(BitBuf buf)
	{
		bitbuf=buf.copy();
		bitbuf.reset();
		seq1=new BitSeq();
		seq2=new BitSeq();
		seq3=new BitSeq();
		seq4=new BitSeq();
		seq5=new BitSeq();
	}
	public void gammacode(long x)
	{
		if (x==0)
		{
			seq1.setsingle(false);
			bitbuf.write(seq1);
			return;
		}
		seq1.setsingle(true);		
		if (x<0)
		{
			x=-x;
			seq2.setsingle(true);
		}
		else
		{
			seq2.setsingle(false);
		}
		--x;
		int bits=BitSeq.countbits(x);
		int bitsbits=BitSeq.countbits(bits);
		seq3.unarycode(bitsbits);
		seq4.binarycode(bits,bitsbits);
		seq5.binarycode(x,bits);	
		bitbuf.write(seq1);
		bitbuf.write(seq2);
		bitbuf.write(seq3);
		bitbuf.write(seq4);
		bitbuf.write(seq5);
	}
	public long gammadecode()
	{
		if (!bitbuf.readbit())
			return 0;		
		boolean negative=false;
		if (bitbuf.readbit())
			negative=true;
		int bitsbits=0;
		while (bitbuf.readbit()==true)
			++bitsbits;		
		bitbuf.read(seq4, bitsbits-1);
		int bits=(int)seq4.binarydecode(bitsbits);
		bitbuf.read(seq5, bits-1);
		long x=seq5.binarydecode(bits);
		++x;
		if (negative)
			x=-x;
		return x;
	}
	
}
