package se.flightplanner.map3d;

import se.flightplanner.Project.iMerc;

public class Vertex {
	private int mercx; //mercx
	private int mercy; //mercy
	private short loElev;
	private short hiElev;
	private short usage; //refcounter, 0 if unused.
	private short bufptr; //pointer to vertex buf for this vertex.
	public boolean equals(Object oo)
	{
		Vertex o=(Vertex)oo;
		return o.mercx==mercx && o.mercy==mercy;
	}
	/** Must only be called by VertexStore! */
	public void increase()
	{
		usage+=1;
	}
	/** Must only be called by VertexStore! */
	public void deploy(int mercx,int mercy)
	{
		this.mercx=mercx;
		this.mercy=mercy;
		assert usage==0;
		this.loElev=-32768; //to be initialized later
		this.hiElev=-32768; //to be initialized later
		this.usage=1;
	}
	
	public int hashCode()
	{
		return mercx+mercy*1013;
	}
	public Vertex(int mercx,int mercy,short usage,short bufptr)
	{
		this.mercx=mercx;
		this.mercy=mercy;
		this.loElev=-32768; //to be initialized later
		this.hiElev=-32768; //to be initialized later
		this.usage=usage;
		this.bufptr=bufptr;
	}
	public short getIndex() {
		return bufptr;
	}
	public void updateElev(short loElev, short hiElev) {
		this.loElev=loElev;
		this.hiElev=hiElev;		
	}
	public iMerc getimerc() {
		return new iMerc(this.mercx,this.mercy);
	}
	public boolean decrease() {
		if (this.usage<=1)
		{
			this.mercx=-1;
			this.mercy=-1;
			this.loElev=-32768; //to be initialized later
			this.hiElev=-32768; //to be initialized later
			this.usage=-1;
			this.bufptr=-1;
			return true;
		}
		this.usage-=1;
		return false;
	}
	public short getPointer() {
		// TODO Auto-generated method stub
		return bufptr;
	}
	public boolean hasElev() {
		return this.hiElev!=-32768;
	}
}
