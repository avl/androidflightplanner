package se.flightplanner2;

import java.io.File;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

class Storage
{
	static public String getStoragePath(Context ctx)
	{
		return ctx.getSharedPreferences("se.flightplanner2.prefs",Context.MODE_PRIVATE).getString("storage", "").intern();
	}
	static public File getStorage(String path) {		
		if (path==null || !new File(path).exists())
		{
			Log.w("fplan", "User-supplied storage location doesn't exist.");
			return Environment.getExternalStorageDirectory();
		}
		return new File(path);
	}
	
}