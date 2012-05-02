package se.flightplanner2;

import android.os.Handler;
import android.util.Log;

public class Timeout {
	private Handler handler;
	private DoSomething todo;
	private Runnable runner;
	private boolean outstanding;
	
	public Timeout()
	{
		outstanding=false;
		handler=new Handler();
		runner=new Runnable()
		{
			@Override
			public void run() {
				if (todo!=null)
					todo.run();
				todo=null;
				outstanding=false;
			}
		};
	}
	static public interface DoSomething
	{
		public void run();
	}
	
	void timeout(DoSomething dosome,long timeout)
	{
		if (outstanding)
			handler.removeCallbacks(runner);
		todo=dosome;
		outstanding=true;
		handler.postDelayed(runner, timeout);		
	}
}
