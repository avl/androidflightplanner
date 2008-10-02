package se.flightplanner;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class SetupInfo extends Activity {

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.settingsinfo);
        final SetupInfo setup=this;
        final Button button = (Button) findViewById(R.id.button);
        final EditText edtxt = (EditText) findViewById(R.id.edittext);
        edtxt.setText(getIntent().getExtras().getString("se.flightplanner.user"));
        final EditText edpwd = (EditText) findViewById(R.id.editpass);
        edpwd.setText(getIntent().getExtras().getString("se.flightplanner.password"));
        button.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            	Intent ret=new Intent(Intent.ACTION_DEFAULT);
            	ret.putExtra("se.flightplanner.login",edtxt.getText().toString());
            	ret.putExtra("se.flightplanner.password",edpwd.getText().toString());
            	setup.setResult(RESULT_OK,ret);
            	setup.finish();
            }
        });        
    }

}
