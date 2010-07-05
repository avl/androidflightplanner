package se.flightplanner;

import java.security.MessageDigest;
import java.util.ArrayList;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;

public class DataDownloader {

	static JSONObject post(String path,String user, String pass,
			ArrayList<NameValuePair> nvps) throws Exception {
		
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
		if (user!=null)
		{
			nvps.add(new BasicNameValuePair("user",user));
		}
		HttpPost req=new HttpPost("http://10.0.2.2:5000"+path);
		UrlEncodedFormEntity postparams=new UrlEncodedFormEntity(nvps,"UTF8");
		req.setEntity(postparams);
	
		DefaultHttpClient cli=new DefaultHttpClient();
		BasicResponseHandler rh=new BasicResponseHandler();
		String str=cli.execute(req,rh);
		JSONObject obj=new JSONObject(str);
		if (obj.has("error"))
		{
			throw new RuntimeException("Error:"+obj.getString("error"));
		}
		return obj;
		/*
		JSONObject obj;
		UrlEncodedFormEntity postparams=new UrlEncodedFormEntity(nvps,"UTF8");
		req.setEntity(postparams);
		
		HttpResponse resp = cli.execute(req);
		Log.i("get",resp.getStatusLine().toString());
		HttpEntity ent=resp.getEntity();
		if (ent==null)
		{
			throw new RuntimeException("No request body");
		}
		InputStream str=ent.getContent();
		InputStreamReader rd=new InputStreamReader(str,"UTF-8");
		char[] buf=new char[1024];
		for(;;)
		{
			int r=rd.read(buf,0,1024);
			if (r>0)
				bd.append(buf,0,r);
			if (r<=0)
				break;
		}
		Log.i("ent",bd.toString());
		
		obj=new JSONObject(bd.toString());
		if (obj.has("error"))
		{
			throw new RuntimeException("Error:"+obj.getString("error"));
		}
		return obj;
		*/
	}

}
