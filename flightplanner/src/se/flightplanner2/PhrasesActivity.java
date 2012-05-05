package se.flightplanner2;

import java.util.ArrayList;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.Html;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class PhrasesActivity extends Activity {
	LinearLayout layout;
	ArrayList<View> payloads=new ArrayList<View>();
	private void addFoldable(String name,String content)
	{
		TextView payload=new TextView(this);
		payload.setText(Html.fromHtml(content));
		addFoldable(name,payload);
	}
	
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
    	super.onConfigurationChanged(newConfig);
    }
    
    enum Service
    {
    	GROUND,
    	TOWER,
    	CONTROL
    }
    enum Phase
    {
    	GROUND,
    	FLIGHT
    }
    
    
    
    
	
    private String getgnd()
    {
    	return "XYZ Ground";
    }
    private String gettwr()
    {
    	return "XYZ Tower";
    }
    private String getreg()
    {
    	return "SE-ABC";
    }
    private String gettype()
    {
    	return "Cessna 172";
    }
    private String getdest()
    {
    	return "[destination]";
    }
    private String getpos()
    {
    	return "[position] [altitude]";
    }
    
    //private void add(Service service,Phase phase,
    //		)
    
    private String get_departure_text()
    {
    	StringBuilder sb=new StringBuilder();
    	sb.append("<h1>Departure Phraseology</h1>");
    	sb.append("<h2>Taxi</h2>");
    	sb.append("<p>"+getgnd()+" "+getreg()+" [location] REQUEST START UP, INFORMATION [ATIS letter]</p>");
    	
    	sb.append("<p>"+getgnd()+" "+getreg()+" "+gettype()+" [location] VFR TO "+getdest()+" REQUEST TAXI</p>");

    	sb.append("<p>"+getgnd()+" "+getreg()+" REQUEST DETAILED TAXI INSTRUCTIONS</p>");

    	sb.append("<h2>Takeoff</h2>");
    	//sb.append("<p>"+get who()+" "+getreg()+" READY (at holding point, when ready departure)</p>");
		//asdf
		return sb.toString();
    }
    private String get_arrival_text()
    {
    	StringBuilder sb=new StringBuilder();
    	sb.append("<h1>Arrival Phraseology</h1>");
    	sb.append("<h2>Approach</h2>");
    	sb.append("<p>"+gettwr()+" "+getreg()+" "+gettype()+" "+getpos()+" INFORMATION [ATIS letter] FOR LANDING</p>");
    	
    	sb.append("<p>"+getgnd()+" "+getreg()+" FINAL (use if no \"clear-to-land\")</p>");  
    	sb.append("<p>"+getgnd()+" "+getreg()+" LONG FINAL (more than 4NM from touchdown, or 8NM for straight-in approach)</p>");


    	sb.append("<p>"+getgnd()+" "+getreg()+" GOING AROUND</p>");
		
		return sb.toString();
    }
	LayoutInflater layinf;
	private void addFoldable(String name,final View payload)
	{
		Button but=(Button)layinf.inflate(R.layout.phrase_button, null,false);
		// v = vi.inflate(R.layout.game_list_item, null,false);				
		but.setOnClickListener(new View.OnClickListener() {		
			@Override
			public void onClick(View v) {
				for(View pay:payloads)
					pay.setVisibility(View.GONE);
				payload.setVisibility(View.VISIBLE);
			}
		});
		but.setText(name);
		layout.addView(but);
		layout.addView(payload);
		payloads.add(payload);
		payload.setVisibility(View.GONE);		
	}
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.phrases);
		layinf=(LayoutInflater)getSystemService(LAYOUT_INFLATER_SERVICE);
		
		layout=(LinearLayout)findViewById(R.id.main);
		
		//addFoldable("Departure",
		//		get_departure_texts()				
		//		);
		addFoldable("En-Route","Test text2"
				 +"(distance) MILES (GNSS or DME) FROM (name of DME"
				 +"station) (or significant point);"
				
				);
		addFoldable("Arrival","Test text3"
				+"Request Holding instructions ("
				);
		
		addFoldable("Other",""
				//+"NEGATIVE EIGHT POINT THREE THREE (radio without 8.33kHz capability)" //no 8.33kHz capability
				+getgnd()+" "+getreg()+" [location] REQUEST TAXI TO [destination on airport]"
				+getgnd()+" "+getreg()+" REQUEST CROSS RUNWAY [number]"
				+getgnd()+" "+getreg()+" REQUEST CHANGE TO [freq]"
				+getgnd()+" "+getreg()+" RUNWAY VACATED (entire aircraft is past holding point)"
				);
		addFoldable("Emergency","Test text4");
		
	}
}
