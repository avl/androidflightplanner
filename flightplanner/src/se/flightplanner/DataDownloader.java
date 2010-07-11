package se.flightplanner;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.zip.InflaterInputStream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;

import android.util.Log;

public class DataDownloader {

	static JSONObject post(String path,String user, String pass,
			ArrayList<NameValuePair> nvps,boolean zip) throws Exception {
		
		if (pass!=null)
		{
			byte[] md5pass=MessageDigest.getInstance("MD5").
			digest(pass.getBytes());
			StringBuilder hd=new StringBuilder();
			for(int i=0;i<md5pass.length;++i)
			{
				byte b=md5pass[i];
				hd.append(Character.forDigit((b>>4)&15,16));
				hd.append(Character.forDigit(b&15,16));
			}
			nvps.add(new BasicNameValuePair("password",hd.toString()));
		}
		if (zip)
		{
			nvps.add(new BasicNameValuePair("zip","1"));
		}
		if (user!=null)
		{
			nvps.add(new BasicNameValuePair("user",user));
		}
		String addr="http://10.0.2.2:5000";
		//String addr="http://192.168.1.104:5000";
		//String addr="http://79.99.0.86:5000";
		HttpPost req=new HttpPost(addr+path);
		UrlEncodedFormEntity postparams=new UrlEncodedFormEntity(nvps,"UTF8");
		req.setEntity(postparams);
	
		DefaultHttpClient cli=new DefaultHttpClient();
		String strres;

		if (zip)
		{
			HttpResponse resp = cli.execute(req);
			HttpEntity ent=resp.getEntity();
			if (ent==null)
			{
				throw new RuntimeException("No request body");
			}
			InputStream str2=ent.getContent();
			InputStream str=new InflaterInputStream(str2);
			InputStreamReader rd=new InputStreamReader(str,"UTF-8");
			char[] buf=new char[1024];
			StringBuilder bd=new StringBuilder();
			for(;;)
			{
				int r=rd.read(buf,0,1024);
				if (r>0)
					bd.append(buf,0,r);
				if (r<=0)
					break;
			}
			strres=bd.toString();
		}
		else
		{
			BasicResponseHandler rh=new BasicResponseHandler();
			//Log.i("fplan","About to execute response handler");
			strres=cli.execute(req,rh);
			/*HttpResponse resp = cli.execute(req);
			Log.i("fplan","Log after execute");
			HttpEntity ent=resp.getEntity();
			if (ent==null)
			{
				throw new RuntimeException("No request body");
			}
			InputStream str=ent.getContent();
			InputStreamReader rd=new InputStreamReader(str,"UTF-8");
			char[] buf=new char[1024];
			StringBuilder bd=new StringBuilder();
			for(;;)
			{
				int r=rd.read(buf,0,1024);
				if (r>0)
					bd.append(buf,0,r);
				if (r<=0)
					break;
			}
			strres=bd.toString();
			 */
		}
		Log.i("fplan","Start JSON parse:"+strres.substring(0,Math.min(100,strres.length())));
		JSONObject obj=new JSONObject(strres);
		Log.i("fplan","JSON Parse complete");

		if (obj.has("error"))
		{
			throw new RuntimeException("Error:"+obj.getString("error"));
		}
		return obj;
	}

}
