package se.flightplanner2;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;

public class SetupInfo extends Activity {

	int detail;
	@Override
	public void onBackPressed() {
	  super.onBackPressed();
	  overridePendingTransition(0, 0);
	}
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //getWindow().setWindowAnimations(android.R.anim.);

        
        setContentView(R.layout.settingsinfo);
        final SetupInfo setup=this;
        final Button button = (Button) findViewById(R.id.button);
        final EditText edtxt = (EditText) findViewById(R.id.edittext);
        edtxt.setText(getIntent().getExtras().getString("se.flightplanner2.user"));
        final EditText edpwd = (EditText) findViewById(R.id.editpass);
        edpwd.setText(getIntent().getExtras().getString("se.flightplanner2.password"));
        final EditText storage = (EditText) findViewById(R.id.editstorage);
        storage.setText(getIntent().getExtras().getString("se.flightplanner2.storage"));
        detail=getIntent().getExtras().getInt("se.flightplanner2.mapdetail",0)+1;
        boolean startchecked=getIntent().getExtras().getBoolean("se.flightplanner2.northup",false);
        boolean startcheckedvibrate=getIntent().getExtras().getBoolean("se.flightplanner2.vibrate",false);
        boolean startcheckedterrwarn=getIntent().getExtras().getBoolean("se.flightplanner2.terrwarn",false);
        boolean startcheckedautosync=getIntent().getExtras().getBoolean("se.flightplanner2.autosync",false);
        boolean startcheckedcvr=getIntent().getExtras().getBoolean("se.flightplanner2.cvr",false);
        boolean startcheckedsideview=getIntent().getExtras().getBoolean("se.flightplanner2.sideview",false);
        boolean startcheckednmea_udp=getIntent().getExtras().getBoolean("se.flightplanner2.nmea_udp",false);
        final SetupInfo outer_this=this;
        final CheckBox northup=(CheckBox)findViewById(R.id.northup_default);
    	northup.setChecked(startchecked);

    	final CheckBox vibrate=(CheckBox)findViewById(R.id.vibrate_default);
    	vibrate.setChecked(startcheckedvibrate);

    	final CheckBox terrwarn=(CheckBox)findViewById(R.id.terrain_warning_default);
    	terrwarn.setChecked(startcheckedterrwarn);
        
    	final CheckBox autosync=(CheckBox)findViewById(R.id.autosync_default);
    	autosync.setChecked(startcheckedautosync);

    	final CheckBox cvr=(CheckBox)findViewById(R.id.cvr_default);
    	cvr.setChecked(startcheckedcvr);

    	final CheckBox sideview=(CheckBox)findViewById(R.id.sideview_default);
    	sideview.setChecked(startcheckedsideview);

    	final CheckBox nmea_udp=(CheckBox)findViewById(R.id.nmea_udp);
    	nmea_udp.setChecked(startcheckednmea_udp);

        button.setOnClickListener(new OnClickListener() {
        	void onOk()
        	{
        		
            	Intent ret=new Intent(Intent.ACTION_DEFAULT);
    			ret.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
    			String username2=edtxt.getText().toString();    			
            	ret.putExtra("se.flightplanner2.login",username2);
            	ret.putExtra("se.flightplanner2.password",edpwd.getText().toString());
            	ret.putExtra("se.flightplanner2.storage",storage.getText().toString());
            	ret.putExtra("se.flightplanner2.mapdetail",outer_this.detail-1);
            	ret.putExtra("se.flightplanner2.northup",northup.isChecked());
            	ret.putExtra("se.flightplanner2.vibrate",vibrate.isChecked());
            	ret.putExtra("se.flightplanner2.terrwarn",terrwarn.isChecked());
            	ret.putExtra("se.flightplanner2.autosync",autosync.isChecked());
            	ret.putExtra("se.flightplanner2.cvr",cvr.isChecked());
            	ret.putExtra("se.flightplanner2.sideview",sideview.isChecked()); 
            	ret.putExtra("se.flightplanner2.nmea_udp",nmea_udp.isChecked()); 
            	
            	ret.putExtra("se.flightplanner2.thenopen", getIntent().getExtras().getString("se.flightplanner2.thenopen"));
            	setup.setResult(RESULT_OK,ret);
            	setup.finish();
            	setup.overridePendingTransition(0, 0);            	        		
        	}
            public void onClick(View v) {
    			
    			String username=edtxt.getText().toString();
    			if (username.length()==0)
    			{
    				RookieHelper.showmsg(SetupInfo.this, "You have entered an empty user name!");
    				return;
    			}
    			if (Character.isWhitespace(username.charAt(0)) ||
    					Character.isWhitespace(username.charAt(username.length()-1)))
    			{
    				AlertDialog.Builder builder = new AlertDialog.Builder(SetupInfo.this);
    				builder.setMessage(
    						"You have entered a user name that begins or ends with a space! Surely this is by mistake?")
    						.setCancelable(true)
    						.setPositiveButton("Oops, let me fix it!",
    								new DialogInterface.OnClickListener() {
    									public void onClick(DialogInterface dialog,
    											int id) {
    										dialog.dismiss();
    									}
    								})
    						.setNegativeButton("No, my name really is like that (promise)!",
    								new DialogInterface.OnClickListener() {
    									public void onClick(DialogInterface dialog,
    											int id) {
    										dialog.dismiss();
    						    			onOk();
    									}
    								});
    				AlertDialog diag = builder.create();
    				diag.show();    				
    				return;    				
    			}
    			onOk();
            }
        });
        
        
        Spinner spinner = (Spinner) findViewById(R.id.spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.detail_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        AdapterView.OnItemSelectedListener i=new AdapterView.OnItemSelectedListener()
        {

			@Override
			public void onItemSelected(AdapterView<?> parent, View arg1,
					int pos, long id) {
				outer_this.detail=pos;
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {				
			}
        	
        };
        spinner.setOnItemSelectedListener(i);
        spinner.setSelection(detail);
        
    }
}
