package se.flightplanner2;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.settingsinfo);
        final SetupInfo setup=this;
        final Button button = (Button) findViewById(R.id.button);
        final EditText edtxt = (EditText) findViewById(R.id.edittext);
        edtxt.setText(getIntent().getExtras().getString("se.flightplanner2.user"));
        final EditText edpwd = (EditText) findViewById(R.id.editpass);
        edpwd.setText(getIntent().getExtras().getString("se.flightplanner2.password"));
        detail=getIntent().getExtras().getInt("se.flightplanner2.mapdetail",0)+1;
        boolean startchecked=getIntent().getExtras().getBoolean("se.flightplanner2.northup",false);
        final SetupInfo outer_this=this;
        final CheckBox northup=(CheckBox)findViewById(R.id.northup_default);
    	northup.setChecked(startchecked);
        
        button.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            	Intent ret=new Intent(Intent.ACTION_DEFAULT);
            	ret.putExtra("se.flightplanner2.login",edtxt.getText().toString());
            	ret.putExtra("se.flightplanner2.password",edpwd.getText().toString());
            	ret.putExtra("se.flightplanner2.mapdetail",outer_this.detail-1);
            	ret.putExtra("se.flightplanner2.northup",northup.isChecked());
            	
            	ret.putExtra("se.flightplanner2.thenopen", getIntent().getExtras().getString("se.flightplanner2.thenopen"));
            	setup.setResult(RESULT_OK,ret);
            	setup.finish();
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
