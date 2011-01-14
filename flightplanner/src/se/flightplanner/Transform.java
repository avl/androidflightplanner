package se.flightplanner;

import se.flightplanner.Project.Merc;
import se.flightplanner.vector.Vector;

class Transform
{
	public Transform(
			Merc mypos,
			Vector arrow,
			float hdg,
			int zoomlevel)
	{
		this.mypos=mypos;
		this.hsizex=arrow.x;
		this.hsizey=arrow.y;
		this.hdg=(float)hdg;
		this.hdgrad=(float)(hdg*(Math.PI/180.0));
		this.zoomlevel=zoomlevel;
	}
	public double hsizex; //x position of observer in screen coordinates
	public double hsizey; //y position of observer in screen coordinates
	Merc mypos; //Position of user
	public float hdgrad; //heading of user, in radians
	public float hdg;
	int zoomlevel;
	/// Convert from merc to screen coordinates with 
	/// north up on map
	public Merc getPos()
	{
		return mypos;
	}
	public float getHdg()
	{
		return hdg;
	}
	public float getHdgRad()
	{
		return hdgrad;
	}
	public Vector merc2northscreen(Merc m)
	{
		return new Vector(m.x-mypos.x+hsizex,m.y-mypos.y+hsizey);
	}
	/// Convert from screen coordinates with 
	/// north up on map to merc.
	public Merc northscreen2merc(Vector n)
	{
		/*
		s.x=m.x-mypos.x+hsizex
		s.y=-m.y+mypos.y+hsizey
		m.x=s.x+mypos.x-hsizex
		m.y=mypos.y+hsizey-s.y
		*/
		return new Merc(n.getx()+mypos.x-hsizex,n.gety()+mypos.y-hsizey);
	}
	private Vector northscreen2screen(Vector n)
	{
		Vector c=new Vector(n.getx()-hsizex,n.gety()-hsizey);
		Vector r=c.unrot(hdgrad);
		return new Vector(r.getx()+hsizex,r.gety()+hsizey);
	}
	private Vector screen2northscreen(Vector s)
	{
		Vector c=new Vector(s.getx()-hsizex,s.gety()-hsizey);
		Vector r=c.rot(hdgrad);
		return new Vector(r.getx()+hsizex,r.gety()+hsizey);
	}
	public Merc screen2merc(Vector s)
	{
		return northscreen2merc(screen2northscreen(s));
	}
	public Vector merc2screen(Merc m)
	{
		return northscreen2screen(merc2northscreen(m));
	}
}