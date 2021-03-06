package se.flightplanner2.intercept;

import se.flightplanner2.intercept.Intercept.Result;
import se.flightplanner2.intercept.SecantSolver.Function;
import se.flightplanner2.intercept.TurnPlanner.Res;
import se.flightplanner2.vector.Vector;
import junit.framework.TestCase;

public class TestIntercept extends TestCase {

	public void testTurn() throws Exception
	{
		float radius_nm=Helpers.getTurnRadius(100, Helpers.get_turn_rate_deg(100, 20));
		float radius_m=radius_nm*1852;
		float radius_feet=radius_m/0.3048f;
		assertEquals(2440,radius_feet,15);
		
	}
	public void testSecantSolver()
	{
		{		
		SecantSolver s=new SecantSolver(new Function()
			{
				@Override
				public float calc(float x) {				
					return x-5;
				}
			});
			assertEquals(5.0f,s.solve(-10, 10),0.01f);
		}
		{		
		SecantSolver s=new SecantSolver(new Function()
			{
				@Override
				public float calc(float x) {				
					return (x-3)*(x-3);
				}
			});
			assertEquals(3.0f,s.solve(-10, 10),0.01f);
		}
		
	}
	public void testIntercept1() throws Exception
	{
		//from internet, radius should be 2440 feet
		float radius=2440*0.3048f/1852.0f;
		float angle_of_bank=20;
		float KTAS=100;
	    float turnradius = (float)((KTAS*KTAS) / (11.26 * Math.tan (angle_of_bank/(180.0f/Math.PI))) * 0.3048/1852.0); 
	    assertEquals(radius,turnradius,0.01);
		
		StateVector sv=new StateVector(new Vector(0,0),0,KTAS,angle_of_bank,0);
		
		float halfturndistance=(float)(radius*Math.PI);
		float halfturntime=(halfturndistance/sv.speed)*3600;
		FlightPathTurnSegment fpt=new FlightPathTurnSegment(halfturntime);

		{
			StateVector r=fpt.execute(sv); //should give 180 deg turn
			assertEquals(180,r.hdg,1);
		    assertEquals(0,r.pos.y,0.01);
		    assertEquals((double)radius*2,r.pos.x,0.01);
		}
	}
	public void testIntercept2() throws Exception
	{
		//from internet, radius should be 2440 feet
		//float radius=2440*0.3048f/1852.0f;
		float angle_of_bank=20;
		float KTAS=100;
	    float turnradius = (float)((KTAS*KTAS) / (11.26 * Math.tan (angle_of_bank/(180.0f/Math.PI))) * 0.3048/1852.0); 
	    //assertEquals(radius,turnradius,1);
		
		StateVector sv=new StateVector(new Vector(0,0),0,KTAS,-angle_of_bank,0);
		float halfturndistance=(float)(turnradius*Math.PI);
		float halfturntime=(halfturndistance/sv.speed)*3600;
		FlightPathTurnSegment fpt=new FlightPathTurnSegment(halfturntime/2);
		

		{
			StateVector r=fpt.execute(sv); //should give 90 deg turn
			assertEquals(270,r.hdg,1);
		    assertEquals(-(double)turnradius,r.pos.y,0.01);
		    assertEquals(-(double)turnradius,r.pos.x,0.01);				
			
		}
	}
	
	public void testIntercept3()
	{
		StateVector sv=new StateVector(new Vector(0,0),0,100,0,0);
		FlightPathRollSegment fpt=new FlightPathRollSegment(20);
		
		StateVector one=fpt.execute(sv);
		assertEquals(20,one.roll,0.1);
		assertEquals(4,one.time,0.01);
		assertTrue(one.pos.y<0);
		assertTrue(one.pos.x>0);
		
	}
	public void testPlanTurn()
	{
		TurnPlanner tp=new TurnPlanner();
		assertEquals(-20,tp.getStrategy1(-90, 20, 100).rolldeg,1e-3);
		assertEquals(20,tp.getStrategy1(90, 20, 100).rolldeg,1e-3);
		assertEquals((90.0-7.795718445656886f*2)/Helpers.get_turn_rate_deg(100, 20),tp.getStrategy1(90, 0, 100).keep_turn,1e-3);
		assertEquals((90.0-7.795718445656886f*2)/Helpers.get_turn_rate_deg(100, 20),tp.getStrategy1(-90, 0, 100).keep_turn,1e-3);

		assertEquals((90.0-7.795718445656886f)/Helpers.get_turn_rate_deg(100, 20),tp.getStrategy1(90, 20, 100).keep_turn,1e-3);
		assertEquals((90.0-7.795718445656886f)/Helpers.get_turn_rate_deg(100, 20),tp.getStrategy1(-90, -20, 100).keep_turn,1e-3);
		
		assertEquals((90.0-7.795718445656886f)/Helpers.get_turn_rate_deg(100, 20),tp.getStrategy1(90, -20, 100).keep_turn,1e-3);
		assertEquals((90.0-7.795718445656886f)/Helpers.get_turn_rate_deg(100, 20),tp.getStrategy1(-90, 20, 100).keep_turn,1e-3);
		
		assertEquals(-14.2158,tp.getStrategy1(0, 20, 100).rolldeg,1e-3);
		assertEquals(-20,tp.getStrategy1(-7.795718445656886f, 20, 100).rolldeg,1e-3);
		//assertEquals(null,tp.getStrategy1(7.795718445656886f*2, 0, 100));
		
		assertEquals(20,tp.getStrategy1(7.795718445656886f*2, 0, 100).rolldeg,1e-3);
		assertEquals(20,tp.getStrategy1(7.795718445656886f, 20, 100).rolldeg,1e-3);
		assertEquals(-20,tp.getStrategy1(-7.795718445656886f, -20, 100).rolldeg,1e-3);
		//assertEquals(24.36,tp.getStrategy1(7.795718445656886f*2, 20, 100).rolldeg,1e-1);
		//assertEquals(null,tp.getStrategy1(0, 20, 100));


		
		
	}
		
	/*
	public void testPlanTurn()
	{
		TurnPlanner tp=new TurnPlanner();
		
		Res res=tp.calc(100, 90,0);
		System.out.println("Res: "+res);
	}
	public void testPlanTurn2()
	{
		TurnPlanner tp=new TurnPlanner();
		
		Res res=tp.calc(100, 20f,0);
		
		FlightPathCompositeTurnSegment comp=new FlightPathCompositeTurnSegment(20,100,0);
		StateVector sv=new StateVector();
		sv.speed=100;
		sv.hdg=-20;
		System.out.println("Res: "+res);
		System.out.println("Effect: "+comp.execute(sv));
	}
	public void testPlanTurn3()
	{
		TurnPlanner tp=new TurnPlanner();
		
		Res res=tp.calc(100, 45,60);
		
		FlightPathCompositeTurnSegment comp=new FlightPathCompositeTurnSegment(20,100,0);
		StateVector sv=new StateVector();
		sv.speed=100;
		sv.hdg=-20;
		System.out.println("Res: "+res);
		System.out.println("Effect: "+comp.execute(sv));
	}
	public void testPlanTurn4()
	{
		TurnPlanner tp=new TurnPlanner();
		
		Res res=tp.calc(100, 8,15);
		
		FlightPathCompositeTurnSegment comp=new FlightPathCompositeTurnSegment(12,100,15);
		this doesn't work at all.
		StateVector sv=new StateVector();
		sv.speed=100;
		sv.hdg=0;
		System.out.println("Res: "+res);
		System.out.println("Effect: "+comp.execute(sv));
	}
	public void testIntercept4()
	{
		StateVector sv=new StateVector(new Vector(0,0),0,100,0,0);
		FlightPathStraightSegment fpt=new FlightPathStraightSegment(3600);
		StateVector res=fpt.execute(sv);
		assertEquals(-100,res.pos.y,0.1);
	}
	*/
	public void testCompleteIntercept()
	{
		StateVector sv=new StateVector(
				new Vector(0,0),0.0f,100,-20,0);
		Intercept ic=new Intercept();
		Result r=ic.fast(sv);
		System.out.println("Result final: "+r);
				
		
	}
	
}
