package se.flightplanner.map3d;

import se.flightplanner.Project.iMerc;

public class Vertex {
	private int mercx; //mercx
	private int mercy; //mercy
	public int getx() { return mercx;}
	public int gety() { return mercy;}
	
	//last resulting elev, the one written to the graphics card.
	private float lastElev;
	
	//altitude contributions. Divide by elevcontribs to get real elev. 
	private float elev;
	//each contributor adds 1 to this. Divide elev by this to get real elev.
	//weak contributors might add an elev scaled by f. They will add f here instead then.
	private float elevcontributors;
	
	private short bufptr; //pointer to vertex buf for this vertex.
	
	private short usage; //number of Things using this as corner vertex. When this goes to 0, vertex disappears (and *must* be purged from all stitches!)
	
	private byte owninglevel;
	
	public void incrementUsage()
	{
		usage+=1;
	}
	public boolean decrementUsage()
	{
		usage-=1;
		if (usage<=0)
		{
			mercx=-1;
			mercy=-1;
			owninglevel=-1;
			return true;
		}
		return false;
	}
	
	///Level of the Thing which would own a vertex at this level
	public int owningLevel()
	{
		return owninglevel;
	}
	
	public boolean equals(Object oo)
	{
		Vertex o=(Vertex)oo;
		return o.mercx==mercx && o.mercy==mercy;
	}
	/** Must only be called by VertexStore! */
	public void deploy(int mercx,int mercy,byte owninglevel)
	{
		this.usage=1;
		this.mercx=mercx;
		this.mercy=mercy;
		this.owninglevel=owninglevel;
	}
	
	public int hashCode()
	{
		return mercx+mercy*1013;
	}
	public Vertex(short bufptr)
	{
		this.mercx=-1;
		this.mercy=-1;
		this.bufptr=bufptr;
	}
	public short getIndex() {
		return bufptr;
	}
	public iMerc getimerc() {
		return new iMerc(this.mercx,this.mercy);
	}
	public short getPointer() {
		// TODO Auto-generated method stub
		return bufptr;
	}
}
