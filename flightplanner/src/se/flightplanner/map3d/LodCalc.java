package se.flightplanner.map3d;

import se.flightplanner.Project.iMerc;

public class LodCalc {
	int screen_height;
	float tolerance; //the number of pixels wrong an outline is allowed to be.
	
/*
	static float log2(float x)
	{
		return (float)(Math.log(x)/Math.log(2));
	}
	
	public static byte coarsestLod(int mx,int my)
	{
		int powx=(int)Math.round(log2((((mx-1)&(~mx))+1)));
		int powy=(int)Math.round(log2((((my-1)&(~my))+1)));
		int levelx=(13-(powx-8));
		int levely=(13-(powy-8));
		if (levelx>levely)
			return (byte)levelx;
		else
			return (byte)levely;
	}
	*/
 
		
	public LodCalc(int screen_height,float tolerance)
	{
		this.screen_height=screen_height;
		this.tolerance=tolerance;
	}
	/**
	 * Return <-1 if no refining is necessary, or likely within some time.
	 * Returns -1..0 if refining might soon be needed. Closer to 0 means more likely.
	 * Returns 0..1 if refining is necessary. 1 means refining must be complete. 0.5 means half way in lod-morph
	 * @return
	 */
	public float needRefining(
			float suppressed_detail, //how many merc-units wrong an entity is
			float distance //how far away the entity is (also merc units)
			)
	{//TODO: Needs testing		
		float gap=(suppressed_detail/distance * screen_height)/tolerance;
		if (gap<=0.5f)
		{
			return 0.0f;
		}
		if (gap<0.75f)
		{
			gap-=0.5f;
			gap*=4.0f;
			return -1.0f+gap;
		}
		gap-=0.75f;
		gap*=4.0f;
		if (gap<=0.0f) return 0.0f;
		if (gap>=1.0f) return 1.0f;
		return gap;		
	}
}
