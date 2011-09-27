package se.flightplanner;

import java.io.IOException;

import android.app.Activity;
import android.os.Bundle;

public class AdChartActivity extends Activity {

	AdChartView view;
    public void onCreate(Bundle savedInstanceState) 
    {
    	super.onCreate(savedInstanceState);
    	try {
			view=new AdChartView(this,"ESSA");
			setContentView(view);	
		} catch (IOException e) {
			RookieHelper.showmsg(this,"Problem:"+e.toString());
		}
	}
    
}
