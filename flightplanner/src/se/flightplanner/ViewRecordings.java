package se.flightplanner;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import se.flightplanner.FlightPathLogger.Chunk;
import se.flightplanner.FlightPathUploader.HandleUpload;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class ViewRecordings extends Activity implements HandleUpload {
	
	final File extpath = Environment.getExternalStorageDirectory();
	final File tripdirpath = new File(extpath,
			Config.path+"triplog/");
	FlightPathUploader ful;
	static private String toreadable(String filename)
	{
		return 
			filename.substring(0,4)+"-"+
			filename.substring(4,6)+"-"+
			filename.substring(6,8)+" "+
			filename.substring(8,10)+":"+
			filename.substring(10,12)+":"+				
			filename.substring(12,14);				
	}
	static private String tofilename(String readable)
	{
		return 
			readable.substring(0,4)+
			readable.substring(5,7)+
			readable.substring(8,10)+
			readable.substring(11,13)+
			readable.substring(14,16)+				
			readable.substring(17,19);				
	}
	@Override
	public void onDestroy()
	{
		if (ful!=null)
		{
			ful.cancel(true);
		}
		super.onDestroy();
	}
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);

        setContentView(R.layout.viewrecordings);
        
        final Button uploadbutton = (Button) findViewById(R.id.upload);
        final Button backbutton = (Button) findViewById(R.id.back);
        final Button clearbutton = (Button) findViewById(R.id.clearall);
                       
        
        final ListView list = (ListView) findViewById(R.id.listlabel);
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(metrics.widthPixels,metrics.heightPixels/2);
        list.setLayoutParams(params);
        Log.i("fplan.vr","list:"+list+"vr:"+R.layout.viewrecordings+"rowlabel:"+ R.id.rowlabel);
        
        
        ArrayList<String> internalList=new ArrayList<String>();
        final ArrayAdapter<String> adapter=new ArrayAdapter<String>(this,/*R.layout.viewrecordings,*/android.R.layout.simple_list_item_1,
        		internalList);
        final String user=getIntent().getExtras().getString("se.flightplanner.user");
        final String pass=getIntent().getExtras().getString("se.flightplanner.password");

        list.setAdapter(adapter);
		updateFileList(adapter);
		final ViewRecordings outer_this=this;
		final String[] chosenfile=new String[]{null};
        AdapterView.OnItemClickListener i=new AdapterView.OnItemClickListener()
        {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int pos,
					long id) {
				Log.i("fplan.vr","On Item Select!");
				String item=tofilename(adapter.getItem(pos));
				chosenfile[0]=item;
				try {
					Chunk chunk=Chunk.loadFromDisk(item, true);					
					update(R.id.startdate, chunk.getStartDate());
					update(R.id.starttime, chunk.getStartTime());
					update(R.id.endtime, chunk.getEndTime());
					update(R.id.duration, chunk.getDuration());
					update(R.id.distance, String.format("%.0fNM",chunk.getDistance()));
					update(R.id.departure, chunk.getDeparture());
					update(R.id.destination, chunk.getDestination());
					
				} catch (IOException e) {
					e.printStackTrace();
				}							
			}

			private void update(int fieldid, String value) {
				TextView tv=(TextView)findViewById(fieldid);
				tv.setText(value);
				tv.invalidate();
			}        	
        };

        list.setOnItemClickListener(i);
        clearbutton.setOnClickListener(new OnClickListener()
        {
			@Override
			public void onClick(View arg0) {
				
				DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
				    @Override
				    public void onClick(DialogInterface dialog, int which) {
				        switch (which){
				        case DialogInterface.BUTTON_POSITIVE:
							Log.i("fplan.vr","On Clear Button! Count:"+adapter.getCount());
							for(int i=0;i<adapter.getCount();++i)
							{
								String item=tofilename(adapter.getItem(i));
								Log.i("fplan.vr","Deleting "+item);
								File path=new File(tripdirpath,item);
								path.delete();
							}
							updateFileList(adapter);
							list.invalidate();
				            break;

				        case DialogInterface.BUTTON_NEGATIVE:
				            break;
				        }
				    }
				};
				AlertDialog.Builder builder = new AlertDialog.Builder(outer_this);
				builder.setMessage("Really delete all recorded trips?").setPositiveButton("Delete!", dialogClickListener)
				    .setNegativeButton("My goodness, no!", dialogClickListener).show();
				
			}        	
        });
        
        final ViewRecordings outerthis=this;
        backbutton.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				outerthis.finish();
			}        	
        }
        );
        uploadbutton.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				Log.i("fplan.vr","On click upload, choseN:"+chosenfile[0]+" ful:"+ful+" user/pass"+user+"/"+pass);
				if (chosenfile[0]!=null)
				{
					if (user==null || user.equals(""))
					{
						RookieHelper.showmsg(outerthis,"No password. Go to main screen, press menu->Settings, and enter name & password.");
					}
					else
					{
						if (ful!=null)
						{
							RookieHelper.showmsg(outerthis,"Upload in progress");
						}
						else
						{
							ful=new FlightPathUploader(user,pass,outerthis);						
							ful.execute(chosenfile[0]);
						}
					}
				}
				else
				{
					if (adapter.getCount()==0)
						RookieHelper.showmsg(outerthis, "You have no recorded trips! Go out and fly!");
					else
						RookieHelper.showmsg(outerthis, "Select a trip first!");
				}	
			
			}        	
        }
        );
        
	}

	private void updateFileList(ArrayAdapter<String> adapter) {
		adapter.clear();
		if (tripdirpath.exists())
        {
			String[] list=tripdirpath.list();
			Arrays.sort(list,Collections.reverseOrder());
        	for(String fname : list)
        	{
        		String human_readable;
        		try
        		{
        			human_readable=toreadable(fname);
        			Log.i("fplan","Adding file: "+fname+" as "+human_readable);
        		}
        		catch(IndexOutOfBoundsException	e)
        		{
        			continue;
        		}
        		adapter.add(human_readable);
        	}
        }        
	}
	@Override
	public void onFinishUpload(String status) {
		if (ful!=null)
		{
			if (status.equals("success"))
				RookieHelper.showmsg(this, "Upload successful!");
			else
				RookieHelper.showmsg(this, "Upload failed. Check connection, username and password!");
		}
		ful=null;
	}
}
