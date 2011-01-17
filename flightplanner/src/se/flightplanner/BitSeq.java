package se.flightplanner;

public class BitSeq {
	private int len;
	private boolean[] bits=new boolean[65];
	public BitSeq()
	{
		
	}
	public void setsingle(boolean v) {
		len=1;
		bits[0]=v;
	}
	public void unarycode(long minbits_) {
		if (minbits_<0 || minbits_>bits.length)
			throw new RuntimeException("Bad unarycode value:"+minbits_);
		int minbits=(int)minbits_;
		len=minbits+1;
		for(int i=0;i<minbits;++i)
			bits[i]=true;
		bits[minbits]=false;
	}
	static public int countbits(long number)
	{
		if (number<0) throw new RuntimeException("Don't know how to count bits of a negatvie number");
		
		for(int bits=0;bits<64;++bits)
		{
			long l=1;
			l<<=bits;
			if (number<l)
				return bits;				
		}
		return 64;
	}
	
	public void binarycode(long number, int numbits) {
		if (countbits(number)!=numbits)
			throw new RuntimeException("Number "+number+" is not ideally represented with "+numbits+" bits");
		if (numbits<=0)
		{
			len=0;
			return;
		}
		len=numbits-1;
		for(int i=0;i<numbits-1;++i)
		{
			boolean b=(number&(1l<<i))!=0;
			bits[i]=b;
		}
	}
	public int size() {
		return len;
	}
	public boolean getBit(int i) {
		return bits[i];
	}
	public void setBit(int i, boolean b) {
		bits[i]=b;
	}
	public void setSize(int len2) {
		len=len2;
	}
	public boolean getsingle() {
		return bits[0];
	}
	public long binarydecode(int plen) {
		plen-=1;
		if (plen>len) 
			throw new RuntimeException("INternal");
		if (plen<0) return 0;
		long number=0;
		for(int i=0;i<plen;++i)
		{
			if (bits[i])
				number|=(1l<<i);
		}		
		number|=(1l<<plen);
		return number;
	}

}
