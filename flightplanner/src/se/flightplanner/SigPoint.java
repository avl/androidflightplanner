/**
 * 
 */
package se.flightplanner;

import java.io.Serializable;

import se.flightplanner.Project.Merc;

public class SigPoint implements Serializable
{
	private static final long serialVersionUID = 1939452363561911490L;
	Merc pos;
	String name;
	String kind; //interned
	double alt;
}