package se.flightplanner2.descpos;

import java.io.Serializable;

public abstract class RelDec implements Serializable
{
	public abstract String getName();
	public abstract String getDescr(boolean shortdesc,boolean exacter);				
}