package se.flightplanner;

import android.app.Activity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;

public class ViewRecordings extends Activity {
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);

        setContentView(R.layout.viewrecordings);
        final ViewRecordings setup=this;
        final Button uploadbutton = (Button) findViewById(R.id.upload);
        final Button exitbutton = (Button) findViewById(R.id.exit);
        ListView list = (ListView) findViewById(R.id.listlabel);
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(metrics.widthPixels,metrics.heightPixels/2);
        list.setLayoutParams(params);
        Log.i("fplan.vr","list:"+list+"vr:"+R.layout.viewrecordings+"rowlabel:"+ R.id.rowlabel);
        
        
        list.setAdapter(new ArrayAdapter<String>(this,/*R.layout.viewrecordings,*/android.R.layout.simple_list_item_1,new
        		String[]{
        		"banan",
        		"aprikos",
        		"a1",
        		"a2",
        		"a3",
        		"a4",
        		"a5",
        		"a6",
        		"a7",
        		"a8",
        		}));
        
	}
}
