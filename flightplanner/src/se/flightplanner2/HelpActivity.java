package se.flightplanner2;

import android.app.Activity;
import android.os.Bundle;
import android.text.Html;
import android.widget.TextView;

public class HelpActivity extends Activity {
	@Override
    public void onCreate(Bundle savedInstanceState) 
	{	
		super.onCreate(savedInstanceState);
		setContentView(R.layout.help);
		
		TextView tv=(TextView)findViewById(R.id.help_text);
		tv.setText(Html.fromHtml(
				"<h1>SwFlightplanner</h1>"+
				"<h2>Introduction</h2>"+
				"<p>Welcome to the android app for swflightplanner.se. I hope that this app will help you "+
				"while flying VFR in the supported countries (mainly Sweden). The application can probably be "+
				"used without reading this manual, but please check at least the first chapter, with some "+
				"pitfalls and gotchas in this program.</p>"+
				"<h2>Read this First!</h2>"+
				"<p>First of all, <b>use your own judgement</b>, and be aware that this program will contain bugs and faults.</p>"+
				"<p>Second, if you end up in a <b>menu you don't recognize</b>, push the back-button on your device until you "+
				"get back to the main screen.</p>"+
				"<p>If the main screen <b>doesn't show the map</b> you're used to (=the base map), click the upper right corner to cycle between "+
				"available maps for nearby areas, until you come back to the base map.</p>"+
				"<p><b>When clicking in the map</b>, the app will describe YOUR position relative to the clicked point. "+
				"I've found this to be more useful, because you almost never need to describe the position of a point, "+
				"you're almost always interested in your own position relative to the clicked point. So, be aware that "+
				"if you click on an airfield, the text at the bottom of the screen may say \"12.1 NM S\". This means that " +
				"YOU are 12.1 nautical miles south of the airfield, not that the airfield is south of you.</p>" +
				"<h2>Features</h2>"+
				"SwFlightplanner has the following features:<br/>" +
				"* FREE - no cost!<br/>" +
				"* Moving aviation map over Sweden, Finland, Estonia and Latvia.<br/>" +
				"* Create routes at home, on a PC, then easily sync to your phone and use in your airplane. No more sitting in the cockpit and programming the GPS!<br/>" +
				"* One-click sync for NOTAM, METAR and TAF for all airports in the above mentioned countries. Data is compressed, meaning the sync is typically fast (less than one minute) even on slow GSM networks. This means you can bring up NOTAM, METAR and TAF for alternate airports, while airborne, even for airports you never planned to use. Great if for some reason need arises to land on an airport not planned before takeoff.<br/>"+
				"* Visual approach charts, landing charts, aerodrome charts, airport AIP information for most airports - once again great for non-planned alternate airports.<br/>"+
				"* Help for taxing - your GPS position is shown on taxi charts (certain airports only, unfortunately).<br/>"+
				"* Terrain avoidance function - colors the map red where the terrain is above your current GPS altitude.<br/>"+
				"* Airspace clearance function - warns when approaching an airspace. You can tell the program which airspaces you have clearance in, and you will receive no warnings for those airspaces.<br/>"+
				"* Radio phraseology function - phrase-book with common phrases for VFR pilots, complete with station names, altitudes, and positions.<br/>"+
				"* Describe-my-position function - describes your position relative to any point you choose.<br/>"+
				"* Record my trips - automatically records your GPS route whenever the app is active - no start/stop required. Trips can be uploaded to www.swflightplanner.se and analyzed on map there.<br/>"+
				"* Cockpit Voice Recorder - automatically records all sounds picked up by your device when flying. Use a line-level to mic-level converter and connect your phone to the headset out on your aircraft, and thus automatically always record everything you say and/or hear on the radio.<br/>"+
								
				"<h2>Getting Started</h2>" +
				"<p>The first thing to do is to create an account on www.swflightplanner.se. In a pinch, " +
				"it is possible to get this web site to almost work from an Android phone, but it is very much " +
				"recommended to use a real computer (a laptop will do). Many airports have briefing computers " +
				"available for pilots, and the www.swflightplanner.se will typically work on them.</p>" +
				"<p>After having created an account, select menu->Settings, and enter your username and password. Also " +
				"decide how much space on your phone " +
				"you wish to give swflightplanner. The best user experience will be had if you choose at least " +
				"\"Very High\" detail level, consuming a few gigabyte. If you choose a lower level, not all functionality " +
				"will be available. For instance, terrain avoidance, elevation data and airport maps require at least Very High.</p>" +
				"<p>You can also choose a few other configuration options. I recommend enabling the airspace warning, " +
				"and automatic weather sync. If you frequently fly in mountains, the terrain warnings could be useful.</p>"+
				"<p>Finally, select Menu->Sync to download everything to your phone. Subsequent syncs only download information which has changed.</p>"+ 
				"<h2>Basics - How to use this App</h2>" +
				"<p>The way to use this app is to use www.swflightplanner.se to create routes, then download them to " +
				"your android phone/tablet.</p>" +
				"<p>Use www.swflightplanner.se to create a route, then select the Menu->Sync on your phone. Syncing " +
				"downloads all created routes to the phone. It also downloads NOTAM/TAF/METAR/AIP-info for all airports in the " +
				"app.</p>" +
				"<p>After syncing, choose your trip by selecting 'Menu->Select Trip'. Note that if you modify the currently " +
				"active trip at www.swflightplanner.se and then sync, the active trip is not modified until you select " +
				"Menu->Select Trip and reload the trip.</p>" +
				"<p>Now, with the right trip loaded, go out and fly. You can use the buttons in the lower left of the screen " +
				"to zoom in and out on the map. Your planned route is shown as a white line across the map. At the top of " +
				"the screen you will see constantly updated ETA (Estimated Time of Arrival), delay (how many minutes before" +
				"or after the flight plan you currently are), phone battery level, present zulu time (=UTC), your " +
				"hdg (gps heading=true track), gs (ground speed in knots), the GPS altitude above sea level (don't trust this!), "+
				"the GPS signal strength expressed as a not very well-defined percentage (above 100% is good).</p>"+
				"<h2>Using the Map</h2>"+
				"<p>You can pan the map by simply dragging with a finger. When you do this, the map stops moving, so that the " +
				"aircraft might eventually leave the visible part of the map. However, a short while after the user stops panning, the " +
				"map automatically centers on the aircraft again. You can zoom by using the buttons in the lower left, or by pinching.</p>" +
				"<h3>Tap a point in the map</h3>" +
				"<p>You can tap any point on the map to bring up information about this point.</p>" +
				"<p>The following information is shown</p>" +
				"<p><b>Your position:</b> Expressed relative to the point, in nautical miles and with approximate direction. So, if the app says 4.8NM SE, it means you are " +
				"4.8 nautical miles south-east of the tapped point. Good for position reports - just tap something you believe ATC " +
				"will be aware of (like a significan point or larger town), and just read what the app says.</p>"+
				"<p><b>Time to point:</b>: The number of minutes and seconds until you reach this point, if you were to " +
				"approach it directly at your current ground speed. The app only takes distance and your speed into account, not heading. " +
				"So even if you tap a point that you *passed* 2 minutes ago, the app will report 2 minutes.</p>" +
				"<p><b>Elevation:</b> The terrain elevation at the point. At coarse zoom levels, the reported elevation is " +
				"the highest elevation in the vicinity of the point tapped. Note that the terrain database is only accurate to " +
				"approximately 100m elevation precision in level terrain, and is up to 300m off in mountain regions. You must never use this " +
				"app for terrain avoidance under any circumstances. However, if using it for situational awareness, it can be good " +
				"to know that elevations reported for mountains is often several 100 m too low</p>" +
				"<p><b>Airspace:</b> The known airspaces covering the tapped point are shown. Tap this list to expand it to show " +
				"frequencies as well as airspace names. Tap several times to cycle between screens, if information does not fit on " +
				"one page.</p>"+
				"<p><b>More-button: </b>Tap this button to show even more information about the tapped point.</p>" +
				"<h3>Cycle between maps</h3>" +
				"<p>The application downloads airport maps from AIP for many airports. When near an airport, you can " +
				"tap the upper right corner of the map to cycle between available maps. When showing a map other than the " +
				"regular base map, the map is not a 'moving map'. This means you will have to pan around these maps manually. " +
				
				"<h2>Waypoints</h2>" +
				"<p>Tap the button labeled 'Wpts' in the bottom right of the screen to show waypoint information. If this button " +
				"isn't visible because airspace info is being shown, close the airspace info by tapping the 'close' button.</p>" +
				"<p>You can cycle between all waypoints by clicking the little left and right arrows in the bottom left and right " +
				"corners of the screen. A blue circle on the map shows where the waypoint is located. For each waypoint, the following " +
				"information is shown:</p>" +
				"<p><b>Your position:</b> Your position expressed relative to the waypoint.</p>" +
				"<p><b>ETA</b> Your estimated time of arriving at the waypoint. This is calculated based on your current " +
				"position, and the assumption that you will be travelling at the planned ground speed. This is important to " +
				"understand - it will not use your present ground speed. The reason for not using the present ground speed is that it " +
				"leads to wildly varying ETAs based on small changes in winds etc. If you are travelling faster than planned GS, " +
				"the app assumes it is because the wind is different than planned. For a tailwind, the app cannot assume that the " +
				"same tailwind will be present on all legs. Typically different legs have different headings, and this also means " +
				"wind effects differ. To make the app easier to understand for pilots, the app uses planned GS, not actual GS. " +
				"This means that if you have a constant strong headwind, you will see the ETA slowly climbing up, minute by minute.</p>" +
				"<p><b>Waypoint Name: </b>A description of this waypoint. This will be either the name of a waypoint you added at " +
				"www.swflightplanner.se, or such a name prefixed by either TOD, TOC, BOT, BOC. These abbreviations have the following meanings:<br/>"+
				"<b>TOD</b>= Top Of Descent - this is the point where you start a descent to an aerodrome.<br />"+
				"<b>TOC</b>= Top Of Climb - this is the point after a climb where you will level out on a the altitude.<br />"+
				"<b>BOD</b>= Bottom Of Descent - this is the point after a descent where you will level out on a new altitude.<br />"+
				"<b>BOC</b>= Bottom Of Climb - this is the point at which you start a climb to a new alittude.<br />"+
				"<p>In other words, the app inserts extra waypoints for each climb/descent/cruise configuration change.</p>"+
				"<p>You can click the 'More' button to bring up a lot more information about each waypoint. The information available is:</p>"+
				"<h2>Airport Information</h2>"+
				"<p>Tap an airport in the map, and then tap the 'More' button to show METAR, TAF and NOTAM for an airport." +
				"You can also view AIP information about the airport, with local traffic rules, ATC frequencies, etc. For many " +
				"airports there is also maps such as aerodrome charts and visual approach charts.</p>" +
				"<h2>Airspaces Information</h2>"+
				"<p>The Menu->Airspaces shows a schematic view of nearby airspaces. Tap and drag your finger to the left " +
				"tell the app that you are cleared in an airspace. Tap and drag to the right to cancel clearance. The app will " +
				"warn if you appear to be entering an airspace for which you have no clearance. Note that the app does not trust the " +
				"GPS altitude, and will warn for airspaces which you are passing above or below. "
				
				
				
				
						
				
				
				
				));
	}
}
