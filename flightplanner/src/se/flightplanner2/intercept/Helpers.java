package se.flightplanner2.intercept;

public class Helpers {
	static final float nominal_bank=20;
	static float get_turn_rate_deg(float speed_kt,float bank_deg)
	{
		float turnrate=(float)(562.645*Math.tan(bank_deg/(180.0/Math.PI))/(speed_kt*1.852/3.6));
		return turnrate;
	}

	static float get_bank_deg(float speed_kt,float turn_rate_deg)
	{
		
		return (float)(Math.atan(0.00177732*speed_kt*3.6/1.852*turn_rate_deg)*(180/Math.PI));
	}

	static public float getTurnRadius(StateVector state,float rate) {
		float period=360.0f/Math.abs(rate);
		float circum=period*state.speed/3600.0f;
		float radius=circum/(float)(Math.PI*2);
		return radius;
	}
	static public float getTurnRadius(float speed,float rate) {
		float period=360.0f/Math.abs(rate);
		float circum=period*speed/3600.0f;
		float radius=circum/(float)(Math.PI*2);
		return radius;
	}

}
