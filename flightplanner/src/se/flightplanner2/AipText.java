package se.flightplanner2;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

import android.os.Environment;
import android.util.Log;

public class AipText {

	public String icao;
	public String name;
	public String category;	
	public String checksum;
	public Date date;
	public boolean got_data;
	
	public File get_datapath()
	{
		File extpath = Environment.getExternalStorageDirectory();
		if (icao.length()>100) throw new RuntimeException("Totally unexpected value of icao.");
		File aiptextpath = new File(extpath,
				Config.path+icao+"/");
		Log.i("fplan","Path:"+aiptextpath.getAbsolutePath());
		aiptextpath.mkdirs();
		File path=new File(aiptextpath,category+".html");
		return path;
	}
	
	public static AipText deserialize(DataInputStream is,int version) throws IOException {
		AipText p=new AipText();
		p.icao=is.readUTF();
		//Log.i("fplan","Read:"+p.icao);
		p.name=is.readUTF();
		//Log.i("fplan","Read:"+p.name);
		p.category=is.readUTF();
		//Log.i("fplan","Read:"+p.category);
		p.checksum=is.readUTF();
		//Log.i("fplan","Read:"+p.checksum);
		p.date=new Date(is.readLong()*1000);
		p.got_data=false;
		if (is.readByte()==1)
		{ //Blob is contained
			int bloblen=is.readInt();
			Log.i("fplan","Reading "+bloblen+" byte blob.");
			File extpath = Environment.getExternalStorageDirectory();
			File aiptextpath = new File(extpath,
					Config.path+p.icao+"/");
			aiptextpath.mkdirs();
			File path=new File(aiptextpath,p.category+".html");
			File tmppath=new File(aiptextpath,p.category+".html.part");
			byte[] buf=new byte[4096];
			FileOutputStream os=new FileOutputStream(tmppath);
			for(;;)
			{
				int chunk=bloblen;
				if (chunk>buf.length) chunk=buf.length;
				int got=is.read(buf,0,chunk);
				os.write(buf,0,got);
				if (got<0 || (chunk>0 && got==0)) {
					Log.i("fplan","Failed reading "+p.icao+" - "+p.category+" chunk: "+chunk+" bloblen: "+bloblen+" got: "+got);
					break;
				}
				bloblen-=got;
				if (bloblen==0)
				{
					if (tmppath.renameTo(path))
					{
						p.got_data=true;
					}
					else
					{
						Log.i("fplan","renameTo failed "+p.icao+" - "+p.category);
					}					
					break;
				}
			}
			os.close();
		}
		else
		{
			File fp=p.get_datapath();
			if (fp.exists())
				p.got_data=true;
		}
				
		
		return p;
	
	}
	
	public AipText serialize(DataOutputStream os) throws IOException {
		AipText p=new AipText();
		os.writeUTF(icao);
		os.writeUTF(name);
		os.writeUTF(category);
		os.writeUTF(checksum);
		os.writeLong(date.getTime()/1000);		
		os.writeByte(0); //no Blob serialized						
		return p;
	
	}
		
}
