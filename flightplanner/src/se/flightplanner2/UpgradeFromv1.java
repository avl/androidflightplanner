package se.flightplanner2;

import java.io.File;

import android.os.Environment;
import android.util.Log;

public class UpgradeFromv1 {

	public static void upgradeIfNeeded() {
		//File v2path=new File("/Android/data/se.flightplanner2/files/");
		File extpath = Environment.getExternalStorageDirectory();
		File v1path=new File(extpath,"/Android/data/se.flightplanner/files/");
		File v2basepath=new File(extpath,"/Android/data/se.flightplanner2/");
		File v1basepath=new File(extpath,"/Android/data/se.flightplanner/");
		Log.i("fplan.upgrade","v1 path exists:"+v1path.exists());
		Log.i("fplan.upgrade","v1 basepath exists:"+v1basepath.exists());
		Log.i("fplan.upgrade","v2 basepath exists:"+v2basepath.exists());
		if (!v2basepath.exists() && v1path.exists())
		{			
			v1basepath.renameTo(v2basepath);
		}		
	}

}
