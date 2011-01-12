package se.flightplanner;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import se.flightplanner.BackgroundMapLoader.LoadedBitmap;
import se.flightplanner.MapCache.Key;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;

public class BackgroundMapDownloader extends AsyncTask<Void, String, BackgroundMapDownloader.DownloadedAirspaceData> {
	static public interface BackgroundMapDownloadOwner
	{
		public void onProgress(String prog);
		public void onFinish(Airspace apace,AirspaceLookup lookup);
	}
	BackgroundMapDownloadOwner owner;
	String user;
	String pass;
	public BackgroundMapDownloader(BackgroundMapDownloadOwner owner,String user,String pass)
	{
		this.owner=owner;
		this.user=user;
		this.pass=pass;
	}
	
	@SuppressWarnings("serial")
	static public class BackgroundException extends Exception {
		public String what;

		public BackgroundException(String what) {
			this.what = what;
		}
	}
	@Override
    protected void onProgressUpdate(String... progress) {
		for(String s:progress)
		{
			Log.i("fplan.download","Progress:"+s);
			owner.onProgress(s);
		}
		
	}
	@Override
    protected void onPostExecute(DownloadedAirspaceData result)
	{
		Log.i("fplan.download","onPostExecute:"+result);
		if (result!=null)
			owner.onFinish(result.airspace,result.lookup);
		else
			owner.onFinish(null,null);
			
	}

	boolean waitAvailable() throws InterruptedException {
		for (;;) {
			String state = Environment.getExternalStorageState();
			if (Environment.MEDIA_MOUNTED.equals(state)) {
				// We can read and write the media
				Log.i("fplan.download","Flash is available");
				return true;
			}
			Log.i("fplan.download","Waiting for flash to become available");
			Thread.sleep(2000);
			if (Thread.currentThread().isInterrupted())
				return false;
		}
	}

	static public class DownloadedAirspaceData
	{
		Airspace airspace;
		AirspaceLookup lookup;
		String terrain;
	}
	private DownloadedAirspaceData downloadAirspace()
	{
    	try {
    		///RookieHelper.showmsg(this,"Airspace data for Sweden will now be downloaded. This can take several minutes, and your phone may become unresponsive. Turn on internet access, click ok, and have patience!");
    		DownloadedAirspaceData ad=new DownloadedAirspaceData();
    		ad.airspace=Airspace.download();
    		ad.airspace.serialize_to_file("airspace.bin");
			
			Log.i("fplan","Building BSP-trees");
			ad.lookup=new AirspaceLookup(ad.airspace);
	    	//areaTree=new AirspaceAreaTree(airspace.getSpaces());
	    	//sigPointTree=new AirspaceSigPointsTree(airspace.getPoints());
			Log.i("fplan","BSP-trees finished");
	        return ad;
		} catch (Exception e) {
			publishProgress(e.toString());
			return null;
		}
		
	}
	@Override
	protected DownloadedAirspaceData doInBackground(Void... dummy) {
		

		DownloadedAirspaceData res=null;
		try {
			waitAvailable();
			res=downloadAirspace();
		} catch (InterruptedException e2) {
			publishProgress("Cancelled");
			return null;
		}

		boolean alreadyerased=false;
		for (;;) {
			try {

				waitAvailable();
				
				alreadyerased=true; //TODO: Remove
				
				if(!alreadyerased)
				{
					try
					{
						for(int i=0;i<=10;++i)
						{
							File extpath = Environment.getExternalStorageDirectory();
							File path = new File(extpath,
									"/Android/data/se.flightplanner/files/level" + i);
							if (path.exists())
							{
								
								if (!path.delete())
								{
									publishProgress("Could not delete "+path);
									return null;
								}
							}
						}
					}
					catch(Throwable e)
					{
						publishProgress("Problem deleting existing files"+e);
						return null;
					}
					alreadyerased=true;
				}
				
				if (Thread.currentThread().isInterrupted())
					return null;
				long totprog = 0;
				for (int level = 0; level <= 10; ++level) {
					Log.i("fplan.download","About to download level "+level);
					totprog = downloadLevel(totprog, level);
				}
				return res;
			} catch (InterruptedException e) {
				return null;
			} catch (BackgroundException e) {
				publishProgress("Problem:"+e.what);
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e1) {
					return null;
				}
			}

		}
	}

	private long downloadLevel(long totprog, int level)
			throws InterruptedException, BackgroundException {
		long startversion = -1;
		for (;;) {
			File extpath = Environment.getExternalStorageDirectory();
			File dirpath = new File(extpath,"/Android/data/se.flightplanner/files/");
			if (!dirpath.exists())
				dirpath.mkdirs();
			File path = new File(extpath,
					"/Android/data/se.flightplanner/files/level" + level);
			long filelength = 0;
			if (path.exists())
				filelength = path.length();
			if (!dirpath.exists())
				throw new RuntimeException("Couldn't create directory:"+dirpath);
			
			if (path.exists() && !path.canWrite()) {
				this.publishProgress("Could not write to " + path);
				Thread.sleep(5000);
				continue;
			}

			ArrayList<NameValuePair> nvps = new ArrayList<NameValuePair>();
			nvps.add(new BasicNameValuePair("version", "1"));
			nvps.add(new BasicNameValuePair("level", "" + level));
			nvps.add(new BasicNameValuePair("offset", "" + filelength));
			nvps.add(new BasicNameValuePair("maxlen", "" + 1000000));

			InputStream inp=null;
			try {
				
				Log.i("fplan.download","About to fetch data at offset "+filelength+" level "+level);
				inp = DataDownloader.postRaw("/api/getmap", user,pass, nvps,
						false);

				DataInputStream inp2 = new DataInputStream(inp);
				int magic=inp2.readInt();
				if (magic!=0xf00df00d)
					throw new RuntimeException("Server error, got magic: "+magic);
				int version = inp2.readInt();
				
				
				
				if (version != 1)
					throw new RuntimeException("Unsupported version");
				int error= inp2.readInt();
				if (error==1)
					throw new BackgroundException("Bad password");
				if (error==2)
					throw new BackgroundException("Server is out of bandwidth");
				if (error!=0)
					throw new BackgroundException("Server error");
				long dataversion = inp2.readLong();
				if (startversion == -1)
					startversion = dataversion;
				if (startversion != dataversion)
				{
					if (path.delete()==false)
						throw new RuntimeException("Couldn't delete existing file "+path);
					throw new BackgroundException(
							"Dataversion changed mid-download. Restarting.");
				}
				long curlevelsize = inp2.readLong();
				long totalsize = inp2.readLong();
				long sizeleft = inp2.readLong();
				
				float perc=(float)100.0f*(totprog+filelength)/totalsize;
				publishProgress(String.format("%.3f%%",perc));
				
				if (sizeleft == 0) {
					Log.i("fplan.download","Finished downloading level "+level);
					totprog += filelength;
					return totprog;
				}
				Log.i("fplan.download","stamp: "+dataversion+" Cur: "+curlevelsize+" total: "+totalsize+" sizeleft: "+sizeleft);
				magic=inp2.readInt();
				if (magic!=0xa51c2)
					throw new RuntimeException("Server error2:"+magic);				
				
				RandomAccessFile raf=new RandomAccessFile(path,"rw");
				raf.seek(filelength);
				try
				{
					
					byte[] buffer=new byte[1024];
					int cnt=0;
					for(;;)
					{
						int readlen=inp2.read(buffer);
						if (readlen==-1)
						{
							Log.i("fplan.download","Finished writing chunk "+filelength+" level "+level);	
							break;
						}
						if (readlen==0)
							Thread.sleep(50);
						else
							raf.write(buffer,0,readlen);
						Log.i("fplan.download","Writing chunk "+filelength+" level "+level+" byte "+cnt);	
						cnt+=readlen;
						perc=(float)100.0f*(totprog+cnt+filelength)/totalsize;
						publishProgress(String.format("%.3f%%",perc));
					}
				}
				finally
				{
					raf.close();					
				}

			}

			catch (BackgroundException e) {
				throw e;
			} catch (Exception e) {
				Log.e("fplan", "" + e);
				e.printStackTrace();
				throw new BackgroundException(e.toString());
			}
			finally
			{
				if (inp!=null)
					try {
						inp.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
			}

		}
	}

}
