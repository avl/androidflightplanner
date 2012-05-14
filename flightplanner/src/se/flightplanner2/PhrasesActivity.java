package se.flightplanner2;

import java.util.ArrayList;

import se.flightplanner2.Project.LatLon;
import se.flightplanner2.descpos.RelDec;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.Html;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

public class PhrasesActivity extends Activity {
	private LinearLayout layout;
	private ArrayList<View> payloads=new ArrayList<View>();
	private RelDec curreldec;

	  protected void onActivityResult(int requestCode, int resultCode,
	             Intent data) {
	         if (requestCode == DescribePosition.CHOOSE_POSITION) {
	             if (resultCode == RESULT_OK) {
	                 RelDec rel=(RelDec)data.getSerializableExtra("se.flightplanner2.reldec");
	                 curreldec=rel;
	                 RookieHelper.showmsg(this,rel.getDescr(true));
	                 update();
	             }
	         }
	     }
	
	private TextView addFoldable(String name,String content)
	{
		TextView payload=new TextView(this);
		payload.setText(Html.fromHtml(content));
		payload.setTextSize(20);
		addFoldable(name,payload);
		return payload;
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
    private String getctl()
    {
    	return "XYZ Control";
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
    	if (curreldec!=null)
    		return curreldec.getDescr(true)+" [altitude]";
    	return "[position] [altitude]";
    }
    
    //private void add(Service service,Phase phase,
    //		)
    
    private String get_enroute_text()
    {
    	StringBuilder sb=new StringBuilder();
    	sb.append("<h1>Enroute Phraseology</h1>");
    	sb.append("<h2>Enroute</h2>");
    	sb.append("<p>"+getctl()+" "+getreg()+"(distance) MILES (GNSS or DME) FROM (name of DME station or significant point)</p>");
    	sb.append("<p>"+getctl()+" "+getreg()+" REQUEST CHANGE TO [freq]</p>");
    	sb.append("<p>"+getctl()+" "+getreg()+" REQUEST ? ft (FLIGHT LEVEL ?) (to ask for a new altitude/flight level)</p>");
    	sb.append("<p>"+getctl()+" "+getreg()+" "+getpos()+" REQUEST CROSSING AIRSPACE FORM [position] TO [position] FLIGHT LEVEL [level] (or [altitude] FEET)</p>");
    	sb.append("<p>"+getctl()+" "+getreg()+" REQUEST WEATHER DEVIATION [x MILES] LEFT (or RIGHT) OF ROUTE</p>");
    	sb.append("<p>"+getctl()+" "+getreg()+" REQUEST WEATHER DEVIATION TO [position] VIA [route]</p>");
    	sb.append("</div>");
            	
    	return sb.toString();
    }
    private String get_departure_text()
    {
    	StringBuilder sb=new StringBuilder();
    	sb.append("<h1>Departure Phraseology</h1>");
    	sb.append("<h2>Taxi</h2>");
    	sb.append("<p>"+getgnd()+" "+getreg()+" [location] REQUEST START UP, INFORMATION [ATIS letter]</p>");
    	
    	sb.append("<p>"+getgnd()+" "+getreg()+" "+gettype()+" [location] VFR TO "+getdest()+" REQUEST TAXI</p>");

    	sb.append("<p>"+getgnd()+" "+getreg()+" REQUEST DEPARTURE INFORMATION (when no ATIS)</p>");

    	sb.append("<p>"+getgnd()+" "+getreg()+" REQUEST DETAILED TAXI INSTRUCTIONS</p>");
    	sb.append("<p>"+getgnd()+" "+getreg()+" REQUEST CROSS RUNWAY [number]</p>");
    	sb.append("<p>"+getgnd()+" "+getreg()+" REQUEST CHANGE TO [freq]</p>");
    	

    	sb.append("<h2>Take-off</h2>");
    	sb.append("<p>"+gettwr()+" "+getreg()+" READY (when run-up complete)</p>");

    	sb.append("<p>"+getreg()+" LINING UP (when asked to line up)</p>");
    	sb.append("<p>"+getreg()+"(condition) LINING UP (when asked to line up after a condition has been met, for example: \"AFTER LANDING CESSNA\")</p>");
    	
    	sb.append("<p>"+gettwr()+" "+getreg()+" REQUEST RIGHT (or LEFT) TURN (ask permission to turn right (or left) once airborne)</p>");

    	sb.append("<h2>Departing</h2>");
    	sb.append("<p>"+gettwr()+" "+getreg()+" REQUEST VECTORS (to avoid traffic or for navigation aid)</p>");
    	//sb.append("<p>"+get who()+" "+getreg()+" READY (at holding point, when ready departure)</p>");
		//asdf
		return sb.toString();
    }
    private String get_emergency_text()
    {
    	StringBuilder sb=new StringBuilder();
    	sb.append("<h1>Emergency</h1>");
    	    	    
    	sb.append("<p>"+gettwr()+" "+getreg()+" "+gettype()+" "+getpos()+" INFORMATION [ATIS letter] FOR LANDING</p>");
    	sb.append("<p>"+gettwr()+" "+getreg()+" CANCEL EMERGENCY (when pilot wishes to cancel emergency condition)</p>");
		return sb.toString();
    }
    
    private String get_arrival_text()
    {
    	StringBuilder sb=new StringBuilder();
    	sb.append("<h1>Arrival Phraseology</h1>");
    	
    	
    	sb.append("<h2>Approach</h2>");    	
    	sb.append("<p>"+gettwr()+" "+getreg()+" "+gettype()+" "+getpos()+" INFORMATION [ATIS letter] FOR LANDING</p>");
    	sb.append("<p>"+gettwr()+" "+getreg()+" REQUEST HOLDING INSTRUCTIONS");
    	sb.append("<p>"+gettwr()+" "+getreg()+" REQUEST VECTORS (to avoid traffic or for navigation aid)</p>");    	
    	sb.append("<p>"+gettwr()+" "+getreg()+" REQUEST STRAIGHT-IN APPROACH");
    	sb.append("<p>"+gettwr()+" "+getreg()+" REQUEST VISUAL APPROACH");
    	
    	sb.append("<p>"+getgnd()+" "+getreg()+" FINAL (use if no \"clear-to-land\")</p>");  
    	sb.append("<p>"+getgnd()+" "+getreg()+" LONG FINAL (more than 4NM from touchdown, or 8NM for straight-in approach)</p>");
    	
    	sb.append("<h2>Landing</h2>");    	
    	sb.append("<p>"+getgnd()+" "+getreg()+" GOING AROUND</p>");

    	sb.append("<h2>Taxi-in</h2>");    	
    	sb.append("<p>"+getgnd()+" "+getreg()+" REQUEST BACKTRACK (to ask permission to taxi back along runway)</p>");
    	sb.append("<p>"+getgnd()+" "+getreg()+" ON RUNWAY REQUEST TAXI TO FUEL STATION</p>");
    	sb.append("<p>"+getgnd()+" "+getreg()+" ON RUNWAY REQUEST TAXI TO PARKING</p>");
		sb.append("<p>"+getgnd()+" "+getreg()+" RUNWAY VACATED (entire aircraft is past holding point)");
    	sb.append("<p>"+getgnd()+" "+getreg()+" REQUEST CROSS RUNWAY [number]");
    	sb.append("<p>"+getgnd()+" "+getreg()+" REQUEST CHANGE TO [freq]");
		
		return sb.toString();
    }
	LayoutInflater layinf;
	private View addFoldable(String name,final View payload)
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
		return payload;
	}
	
	private TextView payload_enroute; 
	private TextView payload_arrival; 
	private TextView payload_departure; 
	private TextView payload_emergency; 
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.phrases);
		layinf=(LayoutInflater)getSystemService(LAYOUT_INFLATER_SERVICE);
		
		layout=(LinearLayout)findViewById(R.id.main);
		
		Button but_pos=(Button)findViewById(R.id.but_position);

		but_pos.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(PhrasesActivity.this, DescribePosition.class);
				intent.putExtra("se.flightplanner2.selectpos", true);
				startActivityForResult(intent,DescribePosition.CHOOSE_POSITION);								
			}			
		});
		Spinner spn_chn=(Spinner)findViewById(R.id.spn_channel);
		
		fill_spinner(spn_chn, new String[]{"Hejsan","Svejsan"});

		
		//addFoldable("Departure",
		//		get_departure_texts()				
		//		);
		payload_enroute=addFoldable("En-Route",""
				);
		payload_arrival=addFoldable("Arrival",""				
				);
		payload_departure=addFoldable("Departure",""				
				);		
		payload_emergency=addFoldable("Emergency","");
		
        update();
	}
	private void update()
	{
		payload_enroute.setText(Html.fromHtml(get_enroute_text())); 
		payload_arrival.setText(Html.fromHtml(get_arrival_text())); 
		payload_departure.setText(Html.fromHtml(get_departure_text())); 
		payload_emergency.setText(Html.fromHtml(get_emergency_text())); 
		/*private TextView payload_arrival; 
		private TextView payload_departure; 
		private TextView payload_emergency;*/
	}

	private void fill_spinner(Spinner spn_pos, String[] contents) {
		ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(this,
				android.R.layout.simple_spinner_item,contents);
	    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	    spn_pos.setAdapter(adapter);
	}
}
