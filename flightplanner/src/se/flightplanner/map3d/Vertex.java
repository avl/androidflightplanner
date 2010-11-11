package se.flightplanner.map3d;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import android.util.Log;

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
		if (elevcontributors==0)
		{
			throw new RuntimeException("No elev for vertex: "+this+" lastelev: "+lastElev);
		}
		lastElev=(1000*elev)/elevcontributors;
		//Log.i("fplan","CalcZ(...)="+lastElev);
		return lastElev;
	}
	public void contribElev(short hiElev,short strength) {
		elev+=hiElev;
		elevcontributors+=strength;		
		if (elevcontributors<=0 || strength<=0) throw new RuntimeException("Bad elevcontributors value:"+elevcontributors+" resulting from add of "+strength);
	}
	public void resetElev()
	{
		elev=0;
		elevcontributors=0;
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
	
	private String what; //Debug description of what Vertex is used for.
	
	private float u; //texture coords
	private float v; //texture coords
	
	public String toString()
	{
		return "Vertex("+mercx+","+mercy+",what="+what+",lastElev="+lastElev+",use="+usage+")";
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
			if (usage<0) throw new RuntimeException("Bad decrementUsage - usage is now negative:"+"Vertex:"+this+" usage: "+usage);
			//mercx=-1;
			//mercy=-1;
			//owninglevel=-1;
			return true;
		}
		return false;
	}
	
	///Level of the Thing which would own a vertex at this level
	/* 
	 * We'd like to get rid of the owninglevel variable if possible. 
	 * public int owningLevel()
	{
		return owninglevel;
	}*/
	
	public boolean equals(Object oo)
	{
		Vertex o=(Vertex)oo;
		return o.mercx==mercx && o.mercy==mercy;
	}
	
	/** Must only be called by VertexStore! */
	public void deploy(int mercx,int mercy,byte owninglevel, String what, float u, float v)
	{
		this.usage=1;
		this.mercx=mercx;
		this.mercy=mercy;
		this.owninglevel=owninglevel;
		this.what=what;
		this.u=u;
		this.v=v;
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
	/*
	public short getIndex() {
		return bufptr;
	}*/
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
	public void debugDump(Writer f) throws IOException {
		f.write("{\n");
		f.write("\"nr\" : "+bufptr+" ,\n");
		f.write("\"used\" : \""+isUsed()+"\",\n");
		f.write("\"posx\" : "+mercx+" ,\n");
		f.write("\"posy\" : "+mercy+" ,\n");
		f.write("\"what\" : \""+what+"\" ,\n");
		f.write("\"lastElev\" : "+lastElev+" \n");
		f.write("}\n");		
	}
	public String getWhat() {
		return what;
	}
	public void setWhat(String w) {
		what=w;
	}
	public boolean dbgHasElev() {
		return elevcontributors>0;
	}
	public float getu() {
		return u;
	}
	public float getv() {
		return u;
	}
}
