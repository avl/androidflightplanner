package se.flightplanner2.descpos;

import java.io.Serializable;

public abstract class RelDec implements Serializable
{
	public String name;
	public abstract String getDescr(boolean shortdesc);				
}