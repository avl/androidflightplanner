package se.flightplanner.map3d;

public class Triangle {
	short pointer;
	short[] idx;
	boolean used;
	TerrainTexture texture;
	
	public TerrainTexture getTexture()
	{
		return texture;
	}
	public Triangle(short pointer)
	{
		this.pointer=pointer;
		idx=new short[3];
		this.used=false;
	}
	
	public void reset() {
		used=false;
		for(int i=0;i<3;++i)
			idx[i]=0;
		texture=null;
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
	
	public void assign(Vertex a, Vertex b, Vertex c, TerrainTexture texture) {
		idx[0]=a.getPointer();
		idx[1]=b.getPointer();
		idx[2]=c.getPointer();
		this.texture=texture;
		used=true;
	}
	public short getPointer() {
		return pointer;
	}
	public short getidx(int i) {
		return idx[i];
	}
	public boolean isUsed() {
		return used;
	}
}
