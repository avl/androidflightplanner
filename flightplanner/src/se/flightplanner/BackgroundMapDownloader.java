package se.flightplanner;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import se.flightplanner.Airspace.AirspaceProgress;
import se.flightplanner.BackgroundMapLoader.LoadedBitmap;
import se.flightplanner.MapCache.Key;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.StatFs;
import android.os.SystemClock;
import android.util.Log;

public class BackgroundMapDownloader extends AsyncTask<Airspace, String, BackgroundMapDownloader.DownloadedAirspaceData> {
	static public interface BackgroundMapDownloadOwner
	{
		public void onProgress(String prog);
		public void onFinish(Airspace apace,AirspaceLookup lookup, String error);
	}
	BackgroundMapDownloadOwner owner;
	String user;
	String pass;
	int mapdetail;
	public BackgroundMapDownloader(BackgroundMapDownloadOwner owner,String user,String pass, int mapdetail)
	{
		this.owner=owner;
		this.user=user;
		this.pass=pass;
		this.mapdetail=mapdetail;
	}
	
	@SuppressWarnings("serial")
	static public class BackgroundException extends Exception {
		public String what;

		public BackgroundException(String what) {
			this.what = what;
		}
	}
	@SuppressWarnings("serial")
	static public class FatalBackgroundException extends Exception {
		public String what;

		public FatalBackgroundException(String what) {
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
		//Log.i("fplan.download","onPostExecute:"+result);
		if (result!=null)
			owner.onFinish(result.airspace,result.lookup,result.error);
		else
			owner.onFinish(null,null,"error");
			
	}
	@Override
    protected void onCancelled()
	{
		owner.onFinish(null,null,"Cancelled");		
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
			publishProgress("Flash unavailable");
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
		public String error;
	}
	private DownloadedAirspaceData downloadAirspace(Airspace previous)
	{
    	try {
    		publishProgress("Searching Updates");
    		///RookieHelper.showmsg(this,"Airspace data for Sweden will now be downloaded. This can take several minutes, and your phone may become unresponsive. Turn on internet access, click ok, and have patience!");
    		DownloadedAirspaceData ad=new DownloadedAirspaceData();
    		
    		ad.airspace=Airspace.download(previous,new AirspaceProgress(){
				@Override
				public void report(int percent) {
					publishProgress("Airspace "+percent+"%");					
				}
    		},user,DataDownloader.hashpass(pass));
    		ad.airspace.serialize_to_file("airspace.bin");
			
			Log.i("fplan","Building BSP-trees");
			ad.lookup=new AirspaceLookup(ad.airspace);
	    	//areaTree=new AirspaceAreaTree(airspace.getSpaces());
	    	//sigPointTree=new AirspaceSigPointsTree(airspace.getPoints());
			Log.i("fplan","BSP-trees finished");
			publishProgress("Airspace 100%");
	        return ad;
		} catch (Exception e) {
			e.printStackTrace();
			publishProgress(e.toString());
			return null;
		}
		
	}
	private long checkspace(long atleast) throws FatalBackgroundException
	{
		StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
		long bytesAvailable = (long)stat.getBlockSize() *(long)stat.getBlockCount();
		long megs=bytesAvailable/(1000000);
		if (bytesAvailable<atleast)
			throw new FatalBackgroundException("Error: Disk full.");
		return bytesAvailable;
	}
	@Override
	protected DownloadedAirspaceData doInBackground(Airspace... asp) {
		try
		{
			if (asp.length>1) throw new RuntimeException("Provide 1 or 0 preivous spaces");
			Airspace previous=asp[0];
			DownloadedAirspaceData res=null;
			publishProgress("Starting");
			checkspace(5000000);
			try {
				waitAvailable();
				res=downloadAirspace(previous);			
			} catch (InterruptedException e2) {
				DownloadedAirspaceData  ret=new DownloadedAirspaceData();
				ret.error="Cancelled";
				return ret;
			}
			if (res==null)
			{
				DownloadedAirspaceData  ret=new DownloadedAirspaceData();
				ret.error="Failed";
				return ret;
			}
			
			try {
				waitAvailable();
				downloadAdCharts(res);
			} catch (InterruptedException e2) {			
				res.error="Cancelled";
				return res;
			} catch (Exception e) {
				e.printStackTrace();
				res.error="Failed";
				return res;
			}
			
			
			
			
			int failcount=0;
			for (;;) {
				try {
					waitAvailable();
					if (Thread.currentThread().isInterrupted())
					{
						DownloadedAirspaceData  ret=new DownloadedAirspaceData();
						ret.error="Cancelled";
						return ret;					
					}
					long totprog = 0;
					int maxlevel=MapDetailLevels.getMaxLevelFromDetail(mapdetail);
					for (int level = 0; level <= maxlevel; ++level) {
						Log.i("fplan.download","About to download level "+level);
						
						totprog = downloadLevel(totprog, level, maxlevel);
					}
					return res;
				} catch (InterruptedException e) {
					DownloadedAirspaceData  ret=new DownloadedAirspaceData();
					ret.error="Cancelled";
					return ret;					
				} catch (BackgroundException e) {
					publishProgress(e.what);
					try {
						Thread.sleep(5000);
					} catch (InterruptedException e1) {
						return null;
					}
					++failcount;
					if (failcount>25)
					{
						DownloadedAirspaceData  ret=new DownloadedAirspaceData();
						ret.error="Too many failures";
						return ret;					
					}
				}
	
			}
			
			
		} 
		catch (FatalBackgroundException e) 
		{
			DownloadedAirspaceData  ret=new DownloadedAirspaceData();
			ret.error=e.what;
			e.printStackTrace();
			Log.i("fplan","Fatal background error:"+e.what);
			return ret;			
		}
		
	}

	private void downloadAdCharts(DownloadedAirspaceData res) throws Exception {
		
		
		File extpath = Environment.getExternalStorageDirectory();
		File metapath = new File(extpath,
				"/Android/data/se.flightplanner/files/chartmeta.dat");
		File newstyle = new File(extpath,
				"/Android/data/se.flightplanner/files/newstyle.dat");
		File chartlistpath = new File(extpath,
				"/Android/data/se.flightplanner/files/chartlist.dat");
		long stamp=0;
		long lateststamp=0;
		if (metapath.exists())
		{
			DataInputStream ds=new DataInputStream(
					new FileInputStream(metapath));
			stamp=ds.readLong();
			ds.close();
			
			Log.i("fplan.download","adchart metadata stamp:"+stamp);
		}
		if (!newstyle.exists())
		{
			stamp=0;			
		}
		//stamp=0;
		
		final class AD
		{
			String chartname;
			String humanreadable;
			String icao;
			String cksum;
			String variant;
		}
		try{
			res.airspace.load_chart_list(chartlistpath);
		}catch(Throwable e) {
			e.printStackTrace();			
		}
		
		ArrayList<AD> newcharts=new ArrayList<AD>();
		{
			ArrayList<NameValuePair> nvps = new ArrayList<NameValuePair>();
			nvps.add(new BasicNameValuePair("version", "3"));
			nvps.add(new BasicNameValuePair("stamp", "" + stamp));
			InputStream inp = DataDownloader.postRaw("/api/getnewadchart", user,pass, nvps,false);
			DataInputStream inp2 = new DataInputStream(inp);
			if (inp2.readInt()!=0xf00d1011)
				throw new RuntimeException("Bad magic");
			int version=inp2.readInt();
			if (version!=1 && version!=2 && version!=3) throw new RuntimeException("Bad version number");
			lateststamp=inp2.readLong();
			int numcharts=inp2.readInt();
			publishProgress("Listing Aerodromes, charts:"+numcharts);
			for(int i=0;i<numcharts;++i)
			{
				AD ad=new AD();
				ad.chartname=inp2.readUTF();
				ad.humanreadable=inp2.readUTF();
				ad.icao=inp2.readUTF();
				ad.cksum=inp2.readUTF();
				ad.variant=inp2.readUTF();
				Log.i("fplan.download","Queing chart icao:"+ad.icao+"/chartname:"+ad.chartname+" human:"+ad.humanreadable+" variant: "+ad.variant);
				newcharts.add(ad);
			}
			if (inp2.readInt()!=0xaabbccda) throw new FatalBackgroundException("Bad magic 5");

		}
		
		
		for(AD chart:newcharts)
		{
			checkspace(2000000);
			File chartprojpath = new File(extpath,
					"/Android/data/se.flightplanner/files/"+chart.chartname+".proj");
			publishProgress(chart.humanreadable);
			
			ArrayList<NameValuePair> nvps = new ArrayList<NameValuePair>();
			nvps.add(new BasicNameValuePair("version", "2"));
			nvps.add(new BasicNameValuePair("chart", chart.chartname));
			nvps.add(new BasicNameValuePair("cksum", chart.cksum));
			InputStream inp = DataDownloader.postRaw("/api/getadchart", user,pass, nvps,false);
			DataInputStream inp2=new DataInputStream(inp);
			if (inp2.readInt()!=0xaabb1234)
				throw new FatalBackgroundException("Bad magic:");
			int status=inp2.readInt();
			if (status==3)
				continue; //Skip this chart, we don't have a projection for it. This may happen from time to time.
			if (status==1)
				throw new FatalBackgroundException("Bad password");
			if (status!=0)
				throw new FatalBackgroundException("Failed getting chart:"+status);	

			int version=inp2.readInt();
			int numlevels=inp2.readInt();
			if (version!=1 && version!=2) throw new RuntimeException("Bad version number");

			{			
				DataOutputStream ds=new DataOutputStream(
						new FileOutputStream(chartprojpath));
				for(int i=0;i<6;++i)
					ds.writeDouble(inp2.readDouble());
				int w=inp2.readInt();
				int h=inp2.readInt();
				
				ds.writeInt(w); //width
				ds.writeInt(h); //height
				Log.i("fplan.download","Created chart proj file, width: "+w+" height: "+h);
				ds.close();
			}			
			
			if (inp2.readInt()!=0xaabbccde)
				throw new FatalBackgroundException("Bad magic 2");
			
			byte[] buf=new byte[4096];
			String master_cksum=null;
			for(int i=0;i<numlevels;++i)
			{
				File chartblobpath= new File(extpath,
						"/Android/data/se.flightplanner/files/"+chart.chartname+"-"+i+".bin");
				String cksum=inp2.readUTF();
				if (master_cksum==null)
					master_cksum=cksum;
				else
					if (!master_cksum.equals(cksum))
					{
						Log.i("fplan.download","Chart:"+chart.chartname+" master ck:"+master_cksum+" ck:"+cksum);
						throw new BackgroundException("Chart changed mid download. Please retry.");
					}
				int blobsize=inp2.readInt();
				
				checkspace(5000000);
				
				DataOutputStream ds=new DataOutputStream(
						new FileOutputStream(chartblobpath));
				if (blobsize>40000000)
					throw new RuntimeException("AD Chart is way too large");
				Log.i("fplan.download","Reading "+blobsize+" byte blob.");
				while(blobsize>0)
				{
					int len=(int)blobsize;
					if (len>4096)
						len=4096;
					inp2.readFully(buf,0,len);
					ds.write(buf,0,len);
					blobsize-=len;
				}
				
				if (inp2.readInt()!=0xaabbccdf) throw new FatalBackgroundException("Bad magic 3");
				
			}		
			if (inp2.readInt()!=0xf111) throw new FatalBackgroundException("Bad magic 4");
			
			res.airspace.report_new_chart(chart.humanreadable,chart.chartname,chart.icao,chart.variant);			
			
		}
		
		res.airspace.save_chart_list(chartlistpath);
		{
			DataOutputStream ds=new DataOutputStream(
					new FileOutputStream(metapath));
			ds.writeLong(lateststamp);
			Log.i("fplan.download","Wrote chart metadata file, stamp:"+lateststamp);
			ds.close();
		}
		{
			DataOutputStream ds=new DataOutputStream(
					new FileOutputStream(newstyle));
			ds.writeInt(1);
			Log.i("fplan.download","Wrote newstyle signalling file, stamp.");
			ds.close();
		}
		
		
	}
	private long downloadLevel(long totprog, int level, int maxlevel)
			throws InterruptedException, BackgroundException, FatalBackgroundException {
		long startversion = -1;
		boolean first=true;
		for (;;) {
			File extpath = Environment.getExternalStorageDirectory();
			File metapath = new File(extpath,
			"/Android/data/se.flightplanner/files/meta.dat");
	
			if (!metapath.exists())
			{
				Log.i("fplan.download","Metadata missing, deleting all present map tile files");
				deleteStoredFiles();
			}

			
			File dirpath = new File(extpath,"/Android/data/se.flightplanner/files/");
			if (!dirpath.exists())
				dirpath.mkdirs();
			if (!dirpath.exists())
				throw new RuntimeException("Couldn't create directory:"+dirpath);

			File path = new File(extpath,
					"/Android/data/se.flightplanner/files/level" + level);
			
			
			long filelength = 0;
			if (path.exists())
				filelength = path.length();
			if (path.exists() && !path.canWrite()) {
				throw new BackgroundException("Could not write to " + path);
			}

			ArrayList<NameValuePair> nvps = new ArrayList<NameValuePair>();
			nvps.add(new BasicNameValuePair("version", "1"));
			nvps.add(new BasicNameValuePair("level", "" + level));
			nvps.add(new BasicNameValuePair("offset", "" + filelength));
			nvps.add(new BasicNameValuePair("maxlen", "" + 1000000));
			nvps.add(new BasicNameValuePair("maxlevel", "" + maxlevel));

			InputStream inp=null;
			try {
				
				Log.i("fplan.download","About to fetch data at offset "+filelength+" level "+level);
				inp = DataDownloader.postRaw("/api/getmap", user,pass, nvps,
						false);

				DataInputStream inp2 = new DataInputStream(inp);
				int magic=inp2.readInt();
				if (magic!=0xf00df00d)
				{
					Log.i("fplan","Bad magic: "+magic);
					throw new FatalBackgroundException("Server error");
				}
				int version = inp2.readInt();
				
				
				
				if (version != 1)
					throw new FatalBackgroundException("Must upgrade app from Market first!");
				int error= inp2.readInt();
				if (error==1)
					throw new FatalBackgroundException("Bad password");
				if (error==2)
					throw new BackgroundException("Server is out of bandwidth");
				if (error!=0)
					throw new BackgroundException("Server error");
				long dataversion = inp2.readLong();
				Log.i("fplan","Server dataversion:"+dataversion+" interrupted:"+Thread.currentThread().isInterrupted());
				if (Thread.currentThread().isInterrupted())
					throw new FatalBackgroundException("Cancelled");
				if (startversion == -1)
				{
					if (metapath.exists())
					{
						DataInputStream ds=new DataInputStream(
								new FileInputStream(metapath));
						long metaversion=ds.readLong();
						startversion=metaversion;
						ds.close();
						Log.i("fplan.download","Read metadata file, dataversion:"+startversion);
					}
					else
					{
						startversion = dataversion;
						DataOutputStream ds=new DataOutputStream(
								new FileOutputStream(metapath));
						ds.writeLong(startversion);
						Log.i("fplan.download","Created metadata file, dataversion:"+startversion);
						ds.close();	
					}
				}
				if (startversion != dataversion)
				{
					Log.i("fplan.download","Map dataversion changed. Was:"+startversion+" now: "+dataversion);

					deleteStoredFiles();
					throw new BackgroundException(
							"Dataversion changed. Restarting download.");
				}
				long curlevelsize = inp2.readLong();
				long totalsize = inp2.readLong();
				long sizeleft = inp2.readLong(); //of current level
				
				if (first)
				{
					first=false;
					checkspace(sizeleft);
				}
				
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
				long last=SystemClock.uptimeMillis();
				try
				{
					
					byte[] buffer=new byte[1024];
					int cnt=0;
					for(;;)
					{
						if (Thread.currentThread().isInterrupted())
							throw new FatalBackgroundException("Cancelled");
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
						//Log.i("fplan.download","Writing chunk "+filelength+" level "+level+" byte "+cnt+" interrupt:"+Thread.currentThread().isInterrupted());
						cnt+=readlen;
						perc=(float)100.0f*(totprog+cnt+filelength)/totalsize;
						long now=SystemClock.uptimeMillis();
						if (now-last>2000)
							publishProgress(String.format("%.3f%%",perc));
						last=now;
					}
				}
				finally
				{
					raf.close();					
				}

			}

			catch (BackgroundException e) {
				throw e;
			} catch (FatalBackgroundException e) {
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
	private void deleteStoredFiles() throws FatalBackgroundException {
		
		File extpath2 = Environment.getExternalStorageDirectory();
		File metapath = new File(extpath2,
		"/Android/data/se.flightplanner/files/meta.dat");
		if (metapath.exists())
		{
			if (metapath.delete()==false)
				throw new FatalBackgroundException("Couldn't delete "+metapath.getName());
		}
		for(int dellevel=0;dellevel<=13;++dellevel)
		{						
			File levpath = new File(extpath2,
					"/Android/data/se.flightplanner/files/level" + dellevel);
			if (levpath.exists())
				if (levpath.delete()==false)
					throw new FatalBackgroundException("Couldn't delete existing file "+levpath.getName());
		}
	}

}
