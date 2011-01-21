package se.flightplanner;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.message.BasicNameValuePair;

import se.flightplanner.FlightPathLogger.Chunk;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

public class FlightPathUploader extends AsyncTask<String, Void, String> {

	String user,pass;
	HandleUpload cb;
	public FlightPathUploader(String user,String pass,HandleUpload cb)
	{
		this.cb=cb;
		this.user=user;
		this.pass=pass;
	}
	static public interface HandleUpload
	{
		public void onFinishUpload(String status);
	}
	@Override
    protected void onPostExecute(String result) 
    {
		Log.i("fplan.vr","onPostExecute");
		cb.onFinishUpload(result);
    }

	@Override
	protected String doInBackground(String... args)  {
		for(String filename:args)
		{
			File extpath = Environment.getExternalStorageDirectory();
			File tripdirpath = new File(extpath,
				"/Android/data/se.flightplanner/files/triplog/");
			File path= new File(tripdirpath,filename);
			
			ArrayList<NameValuePair> nvps=new ArrayList<NameValuePair>();
			nvps.add(new BasicNameValuePair("filename", filename));
			try {
				Log.i("fplan.vr","httpUpload");
				DataDownloader.httpUpload("/api/uploadtrip",
						user,pass,
						nvps,
						path);
				Log.i("fplan.vr","upload finished");
			} catch (ClientProtocolException e) {
				e.printStackTrace();
				return "failed";
			} catch (IOException e) {
				e.printStackTrace();
				return "failed";
			}
			
		}
		return "success";
	}

}
