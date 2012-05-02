package se.flightplanner2;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class HtmlViewer extends Activity {

	@Override
    public void onCreate(Bundle savedInstanceState) 
	{	
		super.onCreate(savedInstanceState);
		String abspath=getIntent().getStringExtra("se.flightplanner2.htmlpath");
				
		WebView view=new WebView(this);
		//File fpath=new File(abspath);
		//FileInputStream fi=new FileInputStream(fpath);
		//InputStreamReader fir=new InputStreamReader(new BufferedInputStream(fi,16000));
		//fir.rea
		view.loadUrl("content://se.flightplanner2/"+abspath);
		view.getSettings().setBuiltInZoomControls(true);
		
		view.setWebViewClient(new WebViewClient(){
			   // you tell the webclient you want to catch when a url is about to load
		    @Override
		    public boolean shouldOverrideUrlLoading(WebView  view, String  url){
		        return true;
		    }
		    // here you execute an action when the URL you want is about to load
		    @Override
		    public void onLoadResource(WebView  view, String  url){
		    }			
			
		});
		setContentView(view);
	}
	
}
