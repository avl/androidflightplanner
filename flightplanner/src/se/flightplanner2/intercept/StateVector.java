package se.flightplanner2.intercept;

import se.flightplanner2.Project;
import se.flightplanner2.vector.Vector;

public class StateVector {

	Vector pos; //merc 13
	float hdg;  //degrees
	float speed;//kt
	float roll;
	float time; //elapsed, in hours.
	public void integrate_postime(float dt) {
		float traverse_nm=speed*dt*(1.0f/3600.0f);
		//float traverse_merc=(float)Project.approx_scale(pos.y, 13, traverse_nm);
		pos=pos.plus(Project.heading2vector(hdg).mul(traverse_nm));
		time+=dt;
	}
	public StateVector(StateVector p)
	{
		pos=p.pos;
		hdg=p.hdg;
		speed=p.speed;
		roll=p.roll;
		time=p.time;
	}
	public StateVector()
	{
		pos=new Vector(0,0);
	}
	public StateVector(Vector pos, float hdg, float speed, float roll,
			float time)
	{
		this.pos = pos;
		this.hdg = hdg;
		this.speed = speed;
		this.roll = roll;
		this.time = time;
	}
	@Override
	public String toString() {
		return "StateVector [pos=" + pos + ", hdg=" + hdg + ", speed=" + speed
				+ ", roll=" + roll + ", time=" + time + "]";
	}
	
	
	
}
