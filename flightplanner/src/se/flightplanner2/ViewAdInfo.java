package se.flightplanner2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import se.flightplanner2.Airspace.ChartInfo;
import se.flightplanner2.Airspace.VariantInfo;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.shapes.ArcShape;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class ViewAdInfo extends Activity {
	String icao;
	SigPoint sp;
	
	private void loadChart(String chartname)
	{
		Intent intent = new Intent(this, AdChartActivity.class);
		
		intent.putExtra("se.flightplanner2.user", getPreferences(MODE_PRIVATE).getString("user","")); 
		intent.putExtra("se.flightplanner2.password", getPreferences(MODE_PRIVATE).getString("password",""));
    	Log.i("fplan.chart","Before calling put Serializable");    	
    	
		intent.putExtra("se.flightplanner2.chartname", chartname);
    	Log.i("fplan.chart","After calling put Serializable");	
		//map.releaseMemory();
		startActivity(intent);		
	}
	private void show_aip(AipText aiptext) {
		Intent intent = new Intent(ViewAdInfo.this, HtmlViewer.class);
		intent.putExtra("se.flightplanner2.htmlpath", aiptext.get_datapath().getAbsolutePath()); 
		startActivity(intent);
	}

	@Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);		
    	StringBuilder sb=new StringBuilder();
    	icao=getIntent().getStringExtra("se.flightplanner2.icao");
    	if (GlobalLookup.lookup==null)
    	{
    		finish();
    		return;
    	}
    	sp=GlobalLookup.lookup.getByIcao(icao);
    	if (sp==null)
    	{
    		finish();
    		return;
    	}
    			
		sb.append("<h1>"+sp.name+"</h1>");
		if (sp.extra!=null)
		{
	    	if (sp.extra.icao!=null)
	    		sb.append("<p>("+sp.extra.icao+")</p>");
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
				GlobalDetailedPlace.detailedplace=new NakedDetailedPlace(sp.name,sp.latlon);
				Log.i("fplan","ViewAdInfo detailedplace:"+GlobalDetailedPlace.detailedplace);
				Intent intent = new Intent(ViewAdInfo.this, DetailedPlaceActivity.class);
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
				final ChartInfo ci=GlobalLookup.lookup.getChartInfo(icao);
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
						String what;
						if (vi.variant.equals(""))
							what="Airport Chart";
						else if (vi.variant.equals(".landing"))
							what="Landing Chart";
						else if (vi.variant.equals(".VAC") || vi.variant.equals(".vac"))
							what="Visual Approach Chart";
						else if (vi.variant.equals(".parking"))
							what="Parking Chart";
						else
							what=vi.variant;
						items.add(what);
					}
			        		        
			    	AlertDialog.Builder builder = new AlertDialog.Builder(ViewAdInfo.this);
			    	builder.setTitle("Choose Trip");
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
				final ArrayList<AipText> aipitems=GlobalLookup.lookup.getAipText(icao);
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
	        		ViewAdInfo.this.show_aip(aipitems.get(0));
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
		        		show_aip(aiptext);
		    	    	
		    	    }

		    	});
		    	AlertDialog diag=builder.create();
		    	diag.show();
				
			}
    	});
		
	}

}
