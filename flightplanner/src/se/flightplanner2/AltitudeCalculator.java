package se.flightplanner2;

public class AltitudeCalculator {
	static public double getAltitude(double p0,double p)
	{
		double g=9.80665;
		double T0=288.15;
		double M=0.0289644;
		double R= 	8.31447;
		double L=0.0065;
		//Math.exp(-(g*M*h)/(R*T0));
	    double X=g*M/(R*L);
	    	    
	    return T0*(1.0-Math.pow(p/p0,(1.0/X)))/L;
	}
}
