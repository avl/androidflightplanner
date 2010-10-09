package se.flightplanner.map3d;

import se.flightplanner.Project.iMerc;

public class Vertex {
	private int mercx; //mercx
	private int mercy; //mercy
	
	/**
	 * Get the mercator x-position of this vertex.
	 */
	public int getx() { return mercx;}
	/**
	 * Get the mercator y-position of this vertex.
	 */
	public int gety() { return mercy;}
	
	/**
	 * Calculates the resulting elevation of the vertex, stores
	 * the value to the lastElev field, and returns the same value.
	 */
	public int calcZ()
	{
		lastElev=(100*elev)/elevcontributors;
		return lastElev;
	}
	public int getLastElev()
	{
		return lastElev;
	}
	//last resulting elev, the one written to the graphics card.
	private int lastElev;
	
	//altitude contributions. Divide by elevcontribs to get real elev. 
	private int elev;
	//each contributor adds 1 to this. Divide elev by this to get real elev.
	//weak contributors might add an elev scaled by f. They will add f here instead then.
	private int elevcontributors; //in units of 0.01 (i.e, divide this number by 100.0f to get real number) 
	
	private short bufptr; //pointer to vertex buf for this vertex.
	
	private short usage; //number of Things using this as corner vertex. When this goes to 0, vertex disappears (and *must* be purged from all stitches!)
	
	private byte owninglevel; //Level on which the Things which own this vertex are (have it as corners)
	public String toString()
	{
		return "Vertex("+mercx+","+mercy+")";
	}
	public boolean isUsed()
	{
		return usage>0;
	}
	public void incrementUsage()
	{
		usage+=1;
	}
	public boolean decrementUsage()
	{
		usage-=1;
		if (usage<=0)
		{
			//mercx=-1;
			//mercy=-1;
			//owninglevel=-1;
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
		this.usage=-1;
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
	public boolean valid()
	{
		return usage>0;
	}
	public int dbgUsage() {
		
		return usage;
	}
}
