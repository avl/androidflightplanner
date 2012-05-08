package se.flightplanner2;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import se.flightplanner2.MapCache.Key;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;

public class ClearancePersistence {
	private BackgroundSaver loader;
	
	public void load(ArrayList<AirspaceArea> areas)
	{
		
		try
		{
		
			File extpath = Environment.getExternalStorageDirectory();
			File srcpath = new File(extpath,
					Config.path+"clearance.dat");
			FileInputStream fs=new FileInputStream(srcpath);
			DataInputStream ds=new DataInputStream(fs);
			HashMap<String,ArrayList<AirspaceArea>> id2areas=new HashMap<String, ArrayList<AirspaceArea>>();
			for(AirspaceArea a:areas)
			{
				a.cleared=0;
				String id=a.getIdentity();
				ArrayList<AirspaceArea> l=id2areas.get(id);
				if (l==null)
				{
					l=new ArrayList<AirspaceArea>();
					id2areas.put(id,l);
				}
				l.add(a);
			}
			int version=ds.readByte();
			if (version!=1) throw new RuntimeException("Unknown on-disk format");
			int len=ds.readInt();
			for(int i=0;i<len;++i)
			{
				String id=ds.readUTF();
				long when=ds.readLong();
				ArrayList<AirspaceArea> l=id2areas.get(id);
				if (l!=null)
					for(AirspaceArea a:l)
						a.cleared=when;
			}						
		}catch(Throwable e)
		{
			e.printStackTrace();
			Log.i("fplan","Couldn't load clearances");
		}			
		
	}
	
	long last_update=0;
	int count_standstill=0;
	@SuppressWarnings("unchecked")
	void update(Location loc,AirspaceLookupIf lookup)
	{
		final float gs=(float)(loc.getSpeed()*3.6/1.852);
		if (gs<2)
		{
			++count_standstill;
			if (count_standstill>20 && SystemClock.elapsedRealtime()-last_update>1000l*30)
			{				
				last_update=SystemClock.elapsedRealtime();
				count_standstill=0;
				boolean needswork=false;
				for(AirspaceArea area:lookup.getAllAirspace())
				{
					if (area.cleared!=0)
					{
						area.cleared=0;
						needswork=true;
					}
				}
				if (needswork)
					save(lookup);
			}
		}
		else
		{
			count_standstill=0;
		}
		
	}
	public void save(AirspaceLookupIf lookup)
	{
		loader=new BackgroundSaver();
		loader.execute(lookup.getAllAirspace());
		
	}
	private class BackgroundSaver extends AsyncTask<ArrayList<AirspaceArea>, Void, Void>
	{
		@Override
		protected Void doInBackground(ArrayList<AirspaceArea>... params) {
			try
			{
				if (params.length==0) return null;
				ArrayList<AirspaceArea> last=params[params.length-1];
				File extpath = Environment.getExternalStorageDirectory();
				File tmppath = new File(extpath,
						Config.path+"clearance.dat.tmp");
				File dstpath = new File(extpath,
						Config.path+"clearance.dat");
				FileOutputStream fs=new FileOutputStream(tmppath);
				DataOutputStream ds=new DataOutputStream(fs);
				ds.writeByte(1); //Version
				ArrayList<String> cleared=new ArrayList<String>(); 
				ArrayList<Long> clearedwhen=new ArrayList<Long>();
				long now=new Date().getTime();
				for(AirspaceArea a:last)
				{
					long ago=now-a.cleared;
					if (ago>Config.clearance_valid_time)
						a.cleared=0;
					if (a.cleared!=0)
					{
						cleared.add(a.getIdentity());
						clearedwhen.add(a.cleared);
					}
				}
				ds.writeInt(cleared.size());
				for(int i=0;i<cleared.size();++i)
				{
					String id=cleared.get(i);
					Long when=clearedwhen.get(i);
					ds.writeUTF(id);
					ds.writeLong(when);
				}
				tmppath.renameTo(dstpath);
			}catch(Throwable e)
			{
				e.printStackTrace();
				Log.i("fplan","Couldn't persist clearances");
			}			
			return null;
		}
		
	}
	
}
