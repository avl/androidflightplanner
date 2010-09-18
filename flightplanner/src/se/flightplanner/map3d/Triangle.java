package se.flightplanner.map3d;

public class Triangle {
	short pointer;
	short[] idx;
	
	public Triangle(short pointer)
	{
		this.pointer=pointer;
		idx=new short[3];
	}
	public void reset() {
		for(int i=0;i<3;++i)
			idx[i]=0;
	}
	@Override
	public int hashCode()
	{
		return pointer;
	}
	@Override
	public boolean equals(Object o)
	{
		Triangle t=(Triangle)o;
		return t.pointer==pointer;
	}
	
	public void assign(Vertex a, Vertex b, Vertex c) {
		idx[0]=a.getIndex();
		idx[1]=b.getIndex();
		idx[2]=c.getIndex();
	}
	public short getPointer() {
		return pointer;
	}
	public short getidx(int i) {
		return idx[i];
	}
}
