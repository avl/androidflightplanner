package se.flightplanner;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Arrays;


public class BitBuf {
	int[] bits;
	int idx;
	int off;
	int capacity;
	int size;
	public BitBuf copy() {
		BitBuf ret=new BitBuf();
		ret.bits=new int[(capacity+31)/32];
		if (ret.bits.length!=bits.length)
			throw new RuntimeException("Internal error in BitBuf");
		System.arraycopy(bits,0,ret.bits,0,bits.length);
		ret.idx=idx;
		ret.off=off;
		ret.capacity=capacity;
		ret.size=size;
		return ret;
	}
	private void assure(int newcap) {
		if (capacity>newcap)
			return;
		newcap*=3;
		newcap>>=1;
		int[] temp=new int[(newcap+31)/32];
		System.arraycopy(bits,0,temp,0,bits.length);
		bits=temp;
		capacity=newcap;		
	}
	public BitBuf()	
	{
		int startcap=10;
		bits=new int[(startcap+31)/32];
		capacity=startcap;
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
		if (bitlen>=size)
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
		assure(bitlen+seq.size());
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
		//System.out.println("In write: "+idx+" off: "+off);
		int newsize=32*idx+off;
		if (newsize>size)
			size=newsize;
		return true;
	}
	public int size() {
		return size;
	}
	
	public void rewind2size(int size) {
		if (size<0) throw new RuntimeException("Bad size");
		this.size=size;
		idx=size/32;
		off=size%32;
	}
	public int offset() {
		int newsize=32*idx+off;
		return newsize;
	}
	public void serialize(DataOutputStream data) throws IOException {
		int numints=(size+31)/32;
		data.writeInt(numints);
		for(int i=0;i<numints;++i)
		{
			data.writeInt(this.bits[i]);
		}
		data.writeInt(0xfeed42);
	}
	public static BitBuf deserialize(DataInputStream data) throws IOException {
		BitBuf ret=new BitBuf();
		ret.capacity=ret.size=data.readInt();
		for(int i=0;i<ret.size;++i)
			ret.bits[i]=data.readInt();
		int magic=data.readInt();
		if (magic!=0xfeed42)
			throw new RuntimeException("Bad magic in BitBuf");
		return ret;
	}

}
