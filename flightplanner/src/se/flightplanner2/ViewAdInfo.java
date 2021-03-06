package se.flightplanner2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import se.flightplanner2.Airspace.ChartInfo;
import se.flightplanner2.Airspace.VariantInfo;
import se.flightplanner2.Project.LatLon;
import se.flightplanner2.Project.Merc;
import se.flightplanner2.vector.BoundingBox;
import se.flightplanner2.vector.Vector;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.shapes.ArcShape;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class ViewAdInfo extends Activity {
	String icao;
	SigPoint sp;
	LatLon latlon;
	String name="Unknown";
	public ViewAdInfo()
	{
	}
	
	private void loadChart(String chartname)
	{
		AirspaceLookup lookup=GlobalLookup.lookup;
		if (lookup!=null)
		{
			if (lookup.haveproj(chartname))
			{
		    	Intent ret=new Intent(Intent.ACTION_DEFAULT);
				ret.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
		    	ret.putExtra("se.flightplanner2.adchart",chartname);
		    	//Log.i("fplan.adchart","Returning with "+chartname);
		    	setResult(RESULT_OK,ret);
		    	
		    	finish();
		    	return;				
			}
		}

		Intent intent = new Intent(this, AdChartActivity.class);
		
		intent.putExtra("se.flightplanner2.user", getPreferences(MODE_PRIVATE).getString("user","")); 
		intent.putExtra("se.flightplanner2.password", getPreferences(MODE_PRIVATE).getString("password",""));
    	Log.i("fplan.chart","Before calling put Serializable");    	
    	
		intent.putExtra("se.flightplanner2.chartname", chartname);
    	Log.i("fplan.chart","After calling put Serializable");	
		//map.releaseMemory();
		startActivity(intent);
	}
	private void show_aip(AipText aiptext,String storage) {
		//Intent intent = new Intent();
		try
		{
			Intent intent = new Intent("android.intent.action.VIEW", Uri.parse("file:/"+aiptext.get_datapath(storage).getAbsolutePath()));
			//Uri uri = Uri.parse("content://se.flightplanner2/"+aiptext.get_datapath().getAbsolutePath());//Uri.parse("file://"+aiptext.get_datapath().getAbsolutePath());
			//intent.setData(uri);
			intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
			intent.setClassName("com.android.browser", "com.android.browser.BrowserActivity");			
			startActivity(intent);
		}
		catch(Throwable e)
		{
			Intent intent = new Intent(ViewAdInfo.this, HtmlViewer.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
			intent.putExtra("se.flightplanner2.htmlpath", aiptext.get_datapath(storage).getAbsolutePath()); 
			startActivity(intent);			
		}
		
		//Intent intent = new Intent(ViewAdInfo.this, HtmlViewer.class);
		
		
	    //Intent intent = new Intent("android.intent.action.VIEW", Uri.parse("content://se.flightplanner2/"+aiptext.get_datapath().getAbsolutePath()));  
		//Intent intent = new Intent("android.intent.action.VIEW", Uri.parse("http://www.google.com"));
		//RookieHelper.showmsg(this,"Path: "+aiptext.get_datapath().getAbsolutePath());
		
		
		//Intent intent = new Intent("android.intent.action.VIEW", Uri.parse("file://"+aiptext.get_datapath().getAbsolutePath()));

		//Intent intent = new Intent("android.intent.action.VIEW", 
		//		Uri.parse("content://com.android.htmlfileprovider"+aiptext.get_datapath().getAbsolutePath()));
			
		//intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
		//
	    //startActivity(intent);  
	    
		/*intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
		intent.putExtra("se.flightplanner2.htmlpath", aiptext.get_datapath().getAbsolutePath()); 
		startActivity(intent);*/
	}

	@Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);		
    	StringBuilder sb=new StringBuilder();
    	icao=getIntent().getStringExtra("se.flightplanner2.icao");
    	final AirspaceLookup lookup=GlobalLookup.lookup;
    	if (lookup==null)
    	{
    		finish();
    		overridePendingTransition(0, 0);
    		return;
    	}
    	if (icao!=null)
    	{
	    	sp=lookup.getByIcao(icao);
	    	if (sp==null)
	    	{
	    		finish();
	    		overridePendingTransition(0, 0);
	    		return;
	    	}
	    	latlon=sp.latlon;
    	}
    	else
    	{
    		latlon=(LatLon)getIntent().getSerializableExtra("se.flightplanner2.latlon");
    	}
    	if (latlon==null)
    	{
    		finish();
    		overridePendingTransition(0, 0);
    		return;
    		
    	}
    	

    	
    	
    	if (sp!=null)
    	{
			sb.append("<h1>"+sp.name+"</h1>");
			if (sp.extra!=null)
			{
		    	if (sp.extra.icao!=null)
		    		sb.append("<p>("+sp.extra.icao+")</p>");
		    	if (sp.extra.remark!=null && sp.extra.remark.length()>0)
		    	{		    		
		    		sb.append("<h2>Remarks</h2><p> "+TextUtils.htmlEncode(sp.extra.remark)+"</p>");
		    	}
		    	if (sp.extra.metar!=null)
		    	{
		    		sb.append("<h2>METAR:</h2><p> "+sp.extra.metar+"</p>");
		    	}
		    	if (sp.extra.taf!=null)
		    	{
		    		sb.append("<h2>TAF:</h2><p> "+sp.extra.taf+"</p>");
		    	}
		    	if (sp.extra.notams.length>0)
		    	{
			    	sb.append("<h2>NOTAMs:</h2>");
		    		for(String notam:sp.extra.notams)
		    		{
		    	    	sb.append("<p><pre>"+notam+"</pre></p>");
		    		}	    		
		    	}
			}
    	}
    	
    	sb.append("<h2>Airspaces</h2>");
    	Vector p=Project.latlon2mercvec(latlon, 13);
    	for(AirspaceArea area:lookup.areas.get_areas(BoundingBox.nearby(latlon, 0.01f)))
		{
    		if (!area.poly.is_inside(p))
    			continue;
    		sb.append("<p>");
    		sb.append("<b>"+area.name+"</b>:<br/>");
    		sb.append(area.floor+" - "+area.ceiling+"<br/>");
    		for(String freq:area.freqs)
    		{
    			sb.append(freq+"<br/>");
    		}
    		sb.append("</p>");
		}
    	
    	sb.append("<h2>Elevation</h2>");
    	if (GlobalGetElev.get_elev!=null)
    	{
    		int elv=GlobalGetElev.get_elev.get_elev_ft(latlon, 13, 1);
    		String elvs;
    		if (elv>=-10000 && elv<Short.MAX_VALUE)
    			elvs=""+elv+" feet";
    		else
    			elvs="?";
    		sb.append("<p>Terrain elevation, MSL: "+elvs+"</p>");
    	}
    	
    	    	
        setContentView(R.layout.adinfo);
    	
    	TextView main=(TextView)findViewById(R.id.main_text);
    	
    	main.setText(Html.fromHtml(sb.toString(),null,null));
    	
    	Button back=(Button)findViewById(R.id.back_button);
    	Button charts=(Button)findViewById(R.id.chart_button);
    	Button aip=(Button)findViewById(R.id.aip_button);
    	Button place=(Button)findViewById(R.id.location_button);
    	
    	place.setOnClickListener(new OnClickListener()
    	{
			@Override
			public void onClick(View v) {
				GlobalDetailedPlace.detailedplace=new NakedDetailedPlace(name,latlon);
				Log.i("fplan","ViewAdInfo detailedplace:"+GlobalDetailedPlace.detailedplace);
				Intent intent = new Intent(ViewAdInfo.this, DetailedPlaceActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
				startActivity(intent);				
				
			}    		
    	});
    	back.setOnClickListener(new OnClickListener()
    	{
			@Override
			public void onClick(View v) {
				ViewAdInfo.this.finish();			
			}    		
    	});
    	
    	charts.setOnClickListener(new OnClickListener()
    	{
			@Override
			public void onClick(View v) {
				if (icao==null)
				{
					RookieHelper.showmsg(ViewAdInfo.this, "Click on an airfield on the main map, then select this button to get a list of charts for that field (including VAC etc).");
					return;					
				}
				
				final ChartInfo ci=lookup.getChartInfo(icao);
				ArrayList<String> items=new ArrayList<String>();
				ArrayList<VariantInfo> vars=null;
				if (ci!=null) 
				{
					vars=new ArrayList<VariantInfo>(ci.getVariants());
					Collections.sort(vars,new Comparator<VariantInfo>(){
						@Override
						public int compare(VariantInfo object1, VariantInfo object2) {
							return object1.variant.compareTo(object2.variant);
						}
					});
				}
				if (vars==null || vars.size()==0)
				{
					RookieHelper.showmsg(ViewAdInfo.this, "No charts for this airfield");
				}
				else if (vars.size()==1)
				{
	    	    	VariantInfo variant=vars.iterator().next();
	    	    	ViewAdInfo.this.loadChart(variant.chartname);					
				}
				else
					{
					for(VariantInfo vi:vars)
					{
						String what = Airspace.getHumanReadableVariant(vi);
						items.add(what);
					}
			        		        
			    	AlertDialog.Builder builder = new AlertDialog.Builder(ViewAdInfo.this);
			    	builder.setTitle("Choose Chart");
			    	final ArrayList<VariantInfo> fvars=vars;
			    	builder.setItems(items.toArray(new String[]{}), new DialogInterface.OnClickListener() {
			    	    public void onClick(DialogInterface dialog, int item) {
			    	    	VariantInfo variant=fvars.get(item);
			    	    	ViewAdInfo.this.loadChart(variant.chartname);
			    	    }
			    	});
			    	AlertDialog diag=builder.create();
			    	diag.show();
				}
							
							
			}    		
    	});
    	
    	aip.setOnClickListener(new OnClickListener()
    	{
			@Override
			public void onClick(View v) {
				if (icao==null)
				{
					RookieHelper.showmsg(ViewAdInfo.this, "Click on an airfield on the main map, then select this button.");
					return;					
				}
				final String storage=Storage.getStoragePath(ViewAdInfo.this);
				
				final ArrayList<AipText> aipitems=lookup.getAipText(icao);
				if (aipitems==null)
				{
					RookieHelper.showmsg(ViewAdInfo.this, "No AIP documents for this airfield");
					return;
				}
				String[] items=new String[aipitems.size()];
				for(int i=0;i<items.length;++i)
				{
					items[i]=aipitems.get(i).category;
				}
				if (aipitems.size()==1)
				{
	        		ViewAdInfo.this.show_aip(aipitems.get(0),storage);
	        		return;
				}
		    	AlertDialog.Builder builder = new AlertDialog.Builder(ViewAdInfo.this);
		    	builder.setTitle("Available Documents");
		    	builder.setItems(items, new DialogInterface.OnClickListener() {
		    	    public void onClick(DialogInterface dialog, int item) {
		    	    	AipText aiptext=aipitems.get(item);
		    	    	
		    	    	/*
		    	    	Intent viewIntent = new Intent(Intent.ACTION_VIEW,
		    	                Uri.fromFile(aiptext.get_datapath()));
		    	    	ViewAdInfo.this.startActivity(viewIntent);*/		    	    	
		        		show_aip(aiptext,storage);
		    	    	
		    	    }

		    	});
		    	AlertDialog diag=builder.create();
		    	diag.show();
				
			}
    	});
		
	}

}
