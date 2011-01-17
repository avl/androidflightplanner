package se.flightplanner;

import java.lang.reflect.Array;
import java.util.Arrays;


public class BitBuf {
	int[] bits;
	int idx;
	int off;
	int capacity;
	int size;
	public BitBuf copy() {
		BitBuf ret=new BitBuf(capacity);
		if (ret.bits.length!=bits.length)
			throw new RuntimeException("Internal error in BitBuf");
		System.arraycopy(bits,0,ret.bits,0,bits.length);
		ret.idx=idx;
		ret.off=off;
		ret.capacity=capacity;
		ret.size=size;
		return ret;
	}
	public BitBuf(int len)	
	{
		
		bits=new int[(len+31)/32];
		capacity=len;
		idx=off=0;
	}
	public void reset()
	{
		idx=0;
		off=0;
	}
	public boolean read(BitSeq seq,int len) {
		int bitlen=idx*32+off;
		if (bitlen+len>size)
			return false;
		
		seq.setSize(len);
		for(int i=0;i<len;++i)
		{
			boolean b=
				(bits[idx]&(1<<off))!=0;
			seq.setBit(i,b);			
			off+=1;
			if (off==32)
			{
				off=0;
				idx+=1;
			}
		}
		return true;
	}
	public boolean readbit() {
		int bitlen=idx*32+off;
		if (bitlen+1>size)
			throw new RuntimeException("Out of bits");
		
		boolean b=
			(bits[idx]&(1<<off))!=0;

		off+=1;
		if (off==32)
		{
			off=0;
			idx+=1;
		}
		return b;
	}
	
	public boolean write(BitSeq seq) {
		int bitlen=idx*32+off;
		if (bitlen+seq.size()>capacity)
			return false;
		int s=seq.size();
		for(int i=0;i<s;++i)
		{
			boolean b=seq.getBit(i);
			if (b)
				bits[idx]|=(1<<off);
			off+=1;
			if (off==32)
			{
				off=0;
				idx+=1;
			}
		}
		int newsize=32*idx+off;
		if (newsize>size)
			size=newsize;
		return true;
	}

}
