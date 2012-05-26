package se.flightplanner2;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Log;

public class CVR {

	public boolean recording;
	private MediaRecorder mRecorder;
	
	public void stop()
	{
		if (mRecorder!=null)
		{
			Log.i("fplan.cvr","Stop recording");
			mRecorder.stop();		
			mRecorder.release();
			mRecorder=null;
		}
	}
	public void start()
	{
		try
		{
			if (mRecorder!=null)
			{
				stop();
			}
	        Log.i("fplan.cvr","Prepare recording");
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_d_HHmm");
			formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
			Date now=new Date();
			File extpath = Environment.getExternalStorageDirectory();
			File cvrpath= new File(extpath,"CVR");
			cvrpath.mkdirs();;
			String filename="CVR_"+formatter.format(now)+".3gp";
			File cvrfile=new File(cvrpath,filename);
			
			
			mRecorder = new MediaRecorder();
	        mRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
	        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
	        mRecorder.setOutputFile(cvrfile.getAbsolutePath());
	        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_WB);
	
	        mRecorder.prepare();
	
	        mRecorder.start();
	        Log.i("fplan.cvr","Start recording");
	        recording=true;
		}catch(Throwable e)
		{
        	Log.i("fplan.cvr","CVR failed to start: "+e.getMessage());
        	e.printStackTrace();
			
		}				
		
	}
}
